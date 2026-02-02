package org.example.service.reader;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.example.model.Profile;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * XLSX 파일 리더 (SAX 기반 스트리밍)
 */
public class XlsxReader implements DataReader {

    @Override
    public long read(File file,
                     Profile profile,
                     Consumer<List<String>> headerCallback,
                     Consumer<Map<String, String>> rowCallback,
                     Consumer<Long> progressCallback) throws Exception {

        try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStringsTable sst = (SharedStringsTable) reader.getSharedStringsTable();
            StylesTable styles = reader.getStylesTable();

            Iterator<InputStream> sheets = reader.getSheetsData();
            if (sheets.hasNext()) {
                try (InputStream sheetData = sheets.next()) {
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();

                    XlsxSheetHandler handler = new XlsxSheetHandler(
                        sst, styles, profile, headerCallback, rowCallback, progressCallback);
                    parser.parse(new InputSource(sheetData), handler);

                    return handler.getProcessedRowCount();
                }
            }
        }
        return 0;
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".xlsx"};
    }

    /**
     * SAX 핸들러
     */
    private static class XlsxSheetHandler extends DefaultHandler {
        private final SharedStringsTable sst;
        private final StylesTable styles;
        private final Profile profile;
        private final Consumer<List<String>> headerCallback;
        private final Consumer<Map<String, String>> rowCallback;
        private final Consumer<Long> progressCallback;

        private int currentRow = 0;
        private int currentCol = 0;
        private StringBuilder cellValue = new StringBuilder();
        private String cellType;
        private String cellStyle;
        private String cellRef;
        private boolean inValue = false;

        private List<String> currentRowData = new ArrayList<>();
        private List<String> headerRow = new ArrayList<>();
        private long processedRowCount = 0;
        private boolean headerProcessed = false;

        public XlsxSheetHandler(SharedStringsTable sst,
                                StylesTable styles,
                                Profile profile,
                                Consumer<List<String>> headerCallback,
                                Consumer<Map<String, String>> rowCallback,
                                Consumer<Long> progressCallback) {
            this.sst = sst;
            this.styles = styles;
            this.profile = profile;
            this.headerCallback = headerCallback;
            this.rowCallback = rowCallback;
            this.progressCallback = progressCallback;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("row".equals(qName)) {
                currentRow = Integer.parseInt(attributes.getValue("r"));
                currentRowData.clear();
                currentCol = 0;
            } else if ("c".equals(qName)) {
                cellRef = attributes.getValue("r");
                cellType = attributes.getValue("t");
                cellStyle = attributes.getValue("s");
                currentCol = getColumnIndex(cellRef);
            } else if ("v".equals(qName) || "t".equals(qName)) {
                inValue = true;
                cellValue.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inValue) {
                cellValue.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("v".equals(qName) || "t".equals(qName)) {
                inValue = false;
            } else if ("c".equals(qName)) {
                String value = getCellValue();

                if (profile.getOptions().isTrimWhitespace() && value != null) {
                    value = value.trim();
                }

                while (currentRowData.size() < currentCol) {
                    currentRowData.add("");
                }
                currentRowData.add(value);
            } else if ("row".equals(qName)) {
                processRow();
            }
        }

        private String getCellValue() {
            String value = cellValue.toString();

            if ("s".equals(cellType)) {
                int idx = Integer.parseInt(value);
                return sst.getItemAt(idx).getString();
            } else if ("str".equals(cellType) || "inlineStr".equals(cellType)) {
                return value;
            } else if ("b".equals(cellType)) {
                return "1".equals(value) ? "true" : "false";
            } else if (value.isEmpty()) {
                return "";
            } else {
                try {
                    double numValue = Double.parseDouble(value);

                    if (cellStyle != null) {
                        int styleIdx = Integer.parseInt(cellStyle);
                        if (styles != null && DateUtil.isADateFormat(styleIdx,
                            styles.getStyleAt(styleIdx).getDataFormatString())) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            return sdf.format(DateUtil.getJavaDate(numValue));
                        }
                    }

                    if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                        return String.valueOf((long) numValue);
                    }
                    return value;
                } catch (Exception e) {
                    return value;
                }
            }
        }

        private int getColumnIndex(String cellRef) {
            StringBuilder col = new StringBuilder();
            for (char c : cellRef.toCharArray()) {
                if (Character.isLetter(c)) {
                    col.append(c);
                } else {
                    break;
                }
            }

            int index = 0;
            for (int i = 0; i < col.length(); i++) {
                index = index * 26 + (col.charAt(i) - 'A' + 1);
            }
            return index - 1;
        }

        private void processRow() {
            if (currentRow == 1) {
                headerRow = new ArrayList<>(currentRowData);
                headerCallback.accept(headerRow);
                headerProcessed = true;
            } else if (headerProcessed) {
                // 빈 행 건너뛰기
                if (profile.getOptions().isSkipEmptyRows()) {
                    boolean allEmpty = currentRowData.stream()
                        .allMatch(s -> s == null || s.trim().isEmpty());
                    if (allEmpty) return;
                }

                // 행 데이터를 Map으로 변환
                Map<String, String> rowMap = new HashMap<>();
                for (int i = 0; i < headerRow.size(); i++) {
                    String colName = headerRow.get(i).trim();
                    String value = i < currentRowData.size() ? currentRowData.get(i) : "";
                    rowMap.put(colName, value);
                }

                rowCallback.accept(rowMap);
                processedRowCount++;

                if (progressCallback != null && processedRowCount % 10000 == 0) {
                    progressCallback.accept(processedRowCount);
                }
            }
        }

        public long getProcessedRowCount() {
            return processedRowCount;
        }
    }
}
