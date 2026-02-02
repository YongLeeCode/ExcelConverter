package org.example.service.writer;

import com.opencsv.CSVWriter;
import org.example.model.Profile;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

/**
 * CSV 파일 라이터
 */
public class CsvDataWriter implements DataWriter {

    private CSVWriter csvWriter;
    private FileOutputStream fos;
    private OutputStreamWriter osw;

    @Override
    public void open(File file, Profile profile) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());

        String encoding = profile.getOptions().getOutputEncoding();

        // BOM 추가
        if ("UTF-8-BOM".equalsIgnoreCase(encoding)) {
            try (FileOutputStream bomWriter = new FileOutputStream(file)) {
                bomWriter.write(0xEF);
                bomWriter.write(0xBB);
                bomWriter.write(0xBF);
            }
            fos = new FileOutputStream(file, true);
            osw = new OutputStreamWriter(fos, "UTF-8");
        } else {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, encoding.replace("-BOM", ""));
        }

        char delimiter = profile.getOptions().getDelimiter().charAt(0);
        csvWriter = new CSVWriter(osw, delimiter,
            CSVWriter.DEFAULT_QUOTE_CHARACTER,
            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
            CSVWriter.DEFAULT_LINE_END);
    }

    @Override
    public void writeHeader(List<String> headers) throws Exception {
        csvWriter.writeNext(headers.toArray(new String[0]));
    }

    @Override
    public void writeRow(List<String> values) throws Exception {
        csvWriter.writeNext(values.toArray(new String[0]));
    }

    @Override
    public void close() throws Exception {
        if (csvWriter != null) {
            csvWriter.close();
        }
    }

    @Override
    public String getExtension() {
        return ".csv";
    }

    @Override
    public String getFormatName() {
        return "CSV";
    }
}
