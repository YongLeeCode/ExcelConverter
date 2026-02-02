package org.example.service.reader;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import org.example.model.Profile;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

/**
 * CSV 파일 리더
 */
public class CsvReader implements DataReader {

    @Override
    public long read(File file,
                     Profile profile,
                     Consumer<List<String>> headerCallback,
                     Consumer<Map<String, String>> rowCallback,
                     Consumer<Long> progressCallback) throws Exception {

        // 인코딩 감지 (BOM 체크)
        String encoding = detectEncoding(file);

        char delimiter = profile.getOptions().getDelimiter().charAt(0);
        CSVParser parser = new CSVParserBuilder()
            .withSeparator(delimiter)
            .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding);
             com.opencsv.CSVReader csvReader = new CSVReaderBuilder(reader)
                 .withCSVParser(parser)
                 .build()) {

            String[] header = csvReader.readNext();
            if (header == null) {
                return 0;
            }

            // BOM 제거
            if (header.length > 0 && header[0].startsWith("\uFEFF")) {
                header[0] = header[0].substring(1);
            }

            List<String> headerList = Arrays.asList(header);
            headerCallback.accept(headerList);

            long processedRowCount = 0;
            String[] row;

            while ((row = csvReader.readNext()) != null) {
                // 빈 행 건너뛰기
                if (profile.getOptions().isSkipEmptyRows()) {
                    boolean allEmpty = Arrays.stream(row)
                        .allMatch(s -> s == null || s.trim().isEmpty());
                    if (allEmpty) continue;
                }

                // 행 데이터를 Map으로 변환
                Map<String, String> rowMap = new HashMap<>();
                for (int i = 0; i < headerList.size(); i++) {
                    String colName = headerList.get(i).trim();
                    String value = i < row.length ? row[i] : "";

                    if (profile.getOptions().isTrimWhitespace() && value != null) {
                        value = value.trim();
                    }

                    rowMap.put(colName, value);
                }

                rowCallback.accept(rowMap);
                processedRowCount++;

                if (progressCallback != null && processedRowCount % 10000 == 0) {
                    progressCallback.accept(processedRowCount);
                }
            }

            return processedRowCount;
        }
    }

    /**
     * BOM으로 인코딩 감지
     */
    private String detectEncoding(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bom = new byte[3];
            int read = fis.read(bom);

            if (read >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                return "UTF-8";
            } else if (read >= 2 && bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
                return "UTF-16LE";
            } else if (read >= 2 && bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }
        }

        // 기본값: EUC-KR 시도 후 UTF-8
        return Charset.forName("EUC-KR").name();
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".csv"};
    }
}
