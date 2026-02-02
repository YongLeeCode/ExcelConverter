package org.example.service.writer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.example.model.Profile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * XLSX 파일 라이터 (SXSSF 스트리밍 기반)
 */
public class XlsxDataWriter implements DataWriter {

    private SXSSFWorkbook workbook;
    private SXSSFSheet sheet;
    private FileOutputStream fos;
    private int currentRowNum = 0;
    private CellStyle headerStyle;

    @Override
    public void open(File file, Profile profile) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());

        // SXSSF: 메모리에 100행만 유지 (대용량 처리)
        workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);

        sheet = workbook.createSheet("Sheet1");

        // 헤더 스타일
        headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        fos = new FileOutputStream(file);
    }

    @Override
    public void writeHeader(List<String> headers) throws Exception {
        Row row = sheet.createRow(currentRowNum++);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    @Override
    public void writeRow(List<String> values) throws Exception {
        Row row = sheet.createRow(currentRowNum++);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            String value = values.get(i);

            // 숫자인 경우 숫자로 저장
            if (value != null && !value.isEmpty()) {
                try {
                    double numValue = Double.parseDouble(value);
                    cell.setCellValue(numValue);
                } catch (NumberFormatException e) {
                    cell.setCellValue(value);
                }
            } else {
                cell.setCellValue(value != null ? value : "");
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (workbook != null && fos != null) {
            workbook.write(fos);
            fos.close();
            workbook.dispose();  // 임시 파일 정리
            workbook.close();
        }
    }

    @Override
    public String getExtension() {
        return ".xlsx";
    }

    @Override
    public String getFormatName() {
        return "Excel";
    }
}
