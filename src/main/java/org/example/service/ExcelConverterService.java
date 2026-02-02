package org.example.service;

import org.apache.poi.util.IOUtils;
import org.example.model.*;
import org.example.service.reader.CsvReader;
import org.example.service.reader.DataReader;
import org.example.service.reader.XlsxReader;
import org.example.service.writer.CsvDataWriter;
import org.example.service.writer.DataWriter;
import org.example.service.writer.XlsxDataWriter;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 파일 변환 서비스
 * xlsx, csv 입출력 지원
 */
public class ExcelConverterService {

    private final CalculationEngine calculationEngine;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final List<DataReader> readers = new ArrayList<>();

    public ExcelConverterService() {
        this.calculationEngine = new CalculationEngine();

        // 리더 등록
        readers.add(new XlsxReader());
        readers.add(new CsvReader());
    }

    /**
     * 진행률 리스너 인터페이스
     */
    public interface ProgressListener {
        void onFileStart(int fileIndex, int totalFiles, File file);
        void onProgress(int fileIndex, int totalFiles, long currentRow, String fileName);
        void onFileComplete(int fileIndex, int totalFiles, ConversionResult result);
        void onError(String message, Exception e);
        void onAllComplete(List<ConversionResult> results);

        /**
         * 누락된 컬럼 발견 시 호출
         * @param fileName 파일명
         * @param missingColumns 누락된 컬럼 목록
         * @return true면 계속 진행, false면 중단
         */
        default boolean onMissingColumns(String fileName, List<String> missingColumns) {
            return true; // 기본값: 계속 진행
        }
    }

    /**
     * 다중 파일 변환
     */
    public List<ConversionResult> convert(Profile profile,
                                          List<File> inputFiles,
                                          File outputDir,
                                          ProgressListener listener) {
        return convert(profile, inputFiles, outputDir, listener, false);
    }

    /**
     * 다중 파일 변환 (병합 옵션 포함)
     */
    public List<ConversionResult> convert(Profile profile,
                                          List<File> inputFiles,
                                          File outputDir,
                                          ProgressListener listener,
                                          boolean mergeFiles) {

        cancelled.set(false);
        List<ConversionResult> results = new ArrayList<>();

        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

        // 병합 모드
        if (mergeFiles && inputFiles.size() > 1) {
            ConversionResult result = convertMerged(profile, inputFiles, outputDir, listener);
            results.add(result);

            if (listener != null) {
                listener.onAllComplete(results);
            }
            return results;
        }

        // 개별 파일 모드
        for (int i = 0; i < inputFiles.size(); i++) {
            if (cancelled.get()) {
                break;
            }

            final int fileIndex = i;
            File inputFile = inputFiles.get(i);

            if (listener != null) {
                listener.onFileStart(fileIndex, inputFiles.size(), inputFile);
            }

            ConversionResult result = convertSingle(profile, inputFile, outputDir,
                (currentRow) -> {
                    if (listener != null) {
                        listener.onProgress(fileIndex, inputFiles.size(), currentRow, inputFile.getName());
                    }
                },
                listener);

            results.add(result);

            if (listener != null) {
                listener.onFileComplete(fileIndex, inputFiles.size(), result);
            }
        }

        if (listener != null) {
            listener.onAllComplete(results);
        }

        return results;
    }

    /**
     * 여러 파일을 하나로 병합 변환
     */
    private ConversionResult convertMerged(Profile profile,
                                           List<File> inputFiles,
                                           File outputDir,
                                           ProgressListener listener) {

        ConversionResult result = new ConversionResult(inputFiles.get(0));

        // 출력 형식에 따른 라이터 생성
        DataWriter writer = createWriter(profile);
        String outputExt = writer.getExtension();

        // 출력 파일명 결정 (merged_로 시작)
        String outputFileName = "merged_" + System.currentTimeMillis() + outputExt;
        if (profile.getOutputFileName() != null && !profile.getOutputFileName().isEmpty()) {
            String profileOutput = profile.getOutputFileName();
            if (!profileOutput.contains("{filename}")) {
                outputFileName = profileOutput;
                if (!outputFileName.toLowerCase().endsWith(outputExt.toLowerCase())) {
                    int dotIdx = outputFileName.lastIndexOf('.');
                    if (dotIdx > 0) {
                        outputFileName = outputFileName.substring(0, dotIdx) + outputExt;
                    } else {
                        outputFileName = outputFileName + outputExt;
                    }
                }
            }
        }

        File outputFile = new File(outputDir, outputFileName);
        result.setOutputFile(outputFile);

        try {
            // 중복 제거용 Set
            Set<String> seenKeys = new HashSet<>();
            List<Integer> uniqueKeyIndices = new ArrayList<>();

            // 출력 헤더 및 컬럼 인덱스
            List<String> outputHeaders = new ArrayList<>();
            List<String> selectedSourceColumns = new ArrayList<>();

            // 통계 카운터
            long[] totalInputRows = {0};
            long[] totalOutputRows = {0};
            long[] totalDuplicateRows = {0};
            long[] totalEmptyRows = {0};

            writer.open(outputFile, profile);
            boolean headerWritten = false;

            for (int fileIdx = 0; fileIdx < inputFiles.size(); fileIdx++) {
                if (cancelled.get()) {
                    break;
                }

                File inputFile = inputFiles.get(fileIdx);
                final int currentFileIdx = fileIdx;

                if (listener != null) {
                    listener.onFileStart(fileIdx, inputFiles.size(), inputFile);
                }

                DataReader reader = findReader(inputFile);
                if (reader == null) {
                    System.err.println("Unsupported file format: " + inputFile.getName());
                    continue;
                }

                Map<String, Integer> sourceColumnIndex = new HashMap<>();
                final boolean isFirstFile = !headerWritten;

                reader.read(inputFile, profile,
                    // 헤더 콜백
                    headerRow -> {
                        try {
                            // 원본 컬럼명 → 인덱스 매핑
                            sourceColumnIndex.clear();
                            for (int i = 0; i < headerRow.size(); i++) {
                                sourceColumnIndex.put(headerRow.get(i).trim(), i);
                            }

                            // 첫 번째 파일에서만 누락 컬럼 체크 및 헤더 작성
                            if (isFirstFile) {
                                // 누락된 컬럼 수집
                                List<String> missingColumns = new ArrayList<>();
                                for (ColumnMapping col : profile.getColumns()) {
                                    if (!sourceColumnIndex.containsKey(col.getSource())) {
                                        missingColumns.add(col.getSource());
                                    }
                                }

                                // 누락된 컬럼이 있으면 리스너에게 확인
                                if (!missingColumns.isEmpty() && listener != null) {
                                    boolean shouldContinue = listener.onMissingColumns(inputFile.getName(), missingColumns);
                                    if (!shouldContinue) {
                                        throw new RuntimeException("Cancelled due to missing columns");
                                    }
                                }

                                // 프로필에서 선택할 컬럼 찾기
                                int idx = 0;
                                for (ColumnMapping col : profile.getColumns()) {
                                    Integer colIdx = sourceColumnIndex.get(col.getSource());
                                    if (colIdx != null) {
                                        selectedSourceColumns.add(col.getSource());
                                        outputHeaders.add(col.getOutputName());

                                        if (col.isUniqueKey()) {
                                            uniqueKeyIndices.add(idx);
                                        }
                                        idx++;
                                    }
                                }

                                // 계산 컬럼 헤더 추가
                                for (Calculation calc : profile.getCalculations()) {
                                    if (calc.getInsertAfter() != null) {
                                        int insertIdx = outputHeaders.indexOf(calc.getInsertAfter());
                                        if (insertIdx >= 0) {
                                            outputHeaders.add(insertIdx + 1, calc.getNewColumn());
                                        } else {
                                            outputHeaders.add(calc.getNewColumn());
                                        }
                                    } else {
                                        outputHeaders.add(calc.getNewColumn());
                                    }
                                }

                                writer.writeHeader(outputHeaders);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Header processing error", e);
                        }
                    },
                    // 행 콜백
                    rowData -> {
                        try {
                            totalInputRows[0]++;

                            // 중복 체크
                            if (!uniqueKeyIndices.isEmpty()) {
                                StringBuilder keyBuilder = new StringBuilder();
                                for (int keyIdx : uniqueKeyIndices) {
                                    if (keyIdx < selectedSourceColumns.size()) {
                                        String colName = selectedSourceColumns.get(keyIdx);
                                        String value = rowData.getOrDefault(colName, "");
                                        keyBuilder.append(value).append("\u0000");
                                    }
                                }
                                String key = keyBuilder.toString();

                                if (seenKeys.contains(key)) {
                                    totalDuplicateRows[0]++;
                                    return;
                                }
                                seenKeys.add(key);
                            }

                            // 선택된 컬럼 값 추출
                            List<String> values = new ArrayList<>();
                            for (String colName : selectedSourceColumns) {
                                values.add(rowData.getOrDefault(colName, ""));
                            }

                            // 계산 컬럼 추가
                            for (Calculation calc : profile.getCalculations()) {
                                String calcValue = calculationEngine.evaluate(calc, rowData);

                                if (calc.getInsertAfter() != null) {
                                    int insertIdx = outputHeaders.indexOf(calc.getNewColumn());
                                    if (insertIdx >= 0 && insertIdx <= values.size()) {
                                        values.add(insertIdx, calcValue);
                                    } else {
                                        values.add(calcValue);
                                    }
                                } else {
                                    values.add(calcValue);
                                }

                                rowData.put(calc.getNewColumn(), calcValue);
                            }

                            writer.writeRow(values);
                            totalOutputRows[0]++;

                        } catch (Exception e) {
                            throw new RuntimeException("Row processing error", e);
                        }
                    },
                    // 진행률 콜백
                    rowNum -> {
                        if (listener != null) {
                            listener.onProgress(currentFileIdx, inputFiles.size(), rowNum, inputFile.getName());
                        }
                    }
                );

                headerWritten = true;

                if (listener != null) {
                    ConversionResult fileResult = new ConversionResult(inputFile);
                    fileResult.markSuccess(0);
                    listener.onFileComplete(fileIdx, inputFiles.size(), fileResult);
                }
            }

            writer.close();
            result.markSuccess(totalInputRows[0], totalOutputRows[0], totalDuplicateRows[0], totalEmptyRows[0]);

        } catch (Exception e) {
            result.markFailed(e.getMessage(), e);
            System.err.println("Merge conversion error: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 단일 파일 변환
     */
    public ConversionResult convertSingle(Profile profile,
                                          File inputFile,
                                          File outputDir,
                                          RowProgressCallback rowCallback) {
        return convertSingle(profile, inputFile, outputDir, rowCallback, null);
    }

    /**
     * 단일 파일 변환 (리스너 포함)
     */
    public ConversionResult convertSingle(Profile profile,
                                          File inputFile,
                                          File outputDir,
                                          RowProgressCallback rowCallback,
                                          ProgressListener listener) {

        ConversionResult result = new ConversionResult(inputFile);

        // 적절한 리더 찾기
        DataReader reader = findReader(inputFile);
        if (reader == null) {
            result.markFailed("지원하지 않는 파일 형식입니다: " + inputFile.getName(), null);
            return result;
        }

        // 출력 형식에 따른 라이터 생성
        DataWriter writer = createWriter(profile);
        String outputExt = writer.getExtension();

        // 출력 파일 경로 결정
        String outputFileName = determineOutputFileName(profile, inputFile, outputExt);
        File outputFile = new File(outputDir, outputFileName);
        result.setOutputFile(outputFile);

        try {
            // 중복 제거용 Set
            Set<String> seenKeys = new HashSet<>();
            List<Integer> uniqueKeyIndices = new ArrayList<>();

            // 출력 헤더 및 컬럼 인덱스
            List<String> outputHeaders = new ArrayList<>();
            List<String> selectedSourceColumns = new ArrayList<>();
            Map<String, Integer> sourceColumnIndex = new HashMap<>();

            // 통계 카운터
            long[] inputRows = {0};      // 입력 행 수
            long[] outputRows = {0};     // 출력 행 수
            long[] duplicateRows = {0};  // 중복 건너뛴 행 수
            long[] emptyRows = {0};      // 빈 행 건너뛴 행 수

            // 누락 컬럼으로 인한 중단 플래그
            boolean[] abortDueToMissingColumns = {false};

            writer.open(outputFile, profile);

            reader.read(inputFile, profile,
                // 헤더 콜백
                headerRow -> {
                    try {
                        // 원본 컬럼명 → 인덱스 매핑
                        for (int i = 0; i < headerRow.size(); i++) {
                            sourceColumnIndex.put(headerRow.get(i).trim(), i);
                        }

                        // 누락된 컬럼 수집
                        List<String> missingColumns = new ArrayList<>();
                        for (ColumnMapping col : profile.getColumns()) {
                            if (!sourceColumnIndex.containsKey(col.getSource())) {
                                missingColumns.add(col.getSource());
                            }
                        }

                        // 누락된 컬럼이 있으면 리스너에게 확인
                        if (!missingColumns.isEmpty() && listener != null) {
                            boolean shouldContinue = listener.onMissingColumns(inputFile.getName(), missingColumns);
                            if (!shouldContinue) {
                                abortDueToMissingColumns[0] = true;
                                return; // 헤더 처리 중단
                            }
                        }

                        // 프로필에서 선택할 컬럼 찾기
                        int idx = 0;
                        for (ColumnMapping col : profile.getColumns()) {
                            Integer colIdx = sourceColumnIndex.get(col.getSource());
                            if (colIdx != null) {
                                selectedSourceColumns.add(col.getSource());
                                outputHeaders.add(col.getOutputName());

                                if (col.isUniqueKey()) {
                                    uniqueKeyIndices.add(idx);
                                }
                                idx++;
                            } else {
                                System.out.println("Warning: Column not found - " + col.getSource());
                            }
                        }

                        // 계산 컬럼 헤더 추가
                        for (Calculation calc : profile.getCalculations()) {
                            if (calc.getInsertAfter() != null) {
                                int insertIdx = outputHeaders.indexOf(calc.getInsertAfter());
                                if (insertIdx >= 0) {
                                    outputHeaders.add(insertIdx + 1, calc.getNewColumn());
                                } else {
                                    outputHeaders.add(calc.getNewColumn());
                                }
                            } else {
                                outputHeaders.add(calc.getNewColumn());
                            }
                        }

                        writer.writeHeader(outputHeaders);
                    } catch (Exception e) {
                        throw new RuntimeException("헤더 처리 오류", e);
                    }
                },
                // 행 콜백
                rowData -> {
                    try {
                        inputRows[0]++;  // 입력 행 카운트

                        // 중복 체크
                        if (!uniqueKeyIndices.isEmpty()) {
                            StringBuilder keyBuilder = new StringBuilder();
                            for (int keyIdx : uniqueKeyIndices) {
                                if (keyIdx < selectedSourceColumns.size()) {
                                    String colName = selectedSourceColumns.get(keyIdx);
                                    String value = rowData.getOrDefault(colName, "");
                                    keyBuilder.append(value).append("\u0000");
                                }
                            }
                            String key = keyBuilder.toString();

                            if (seenKeys.contains(key)) {
                                duplicateRows[0]++;  // 중복 카운트
                                return;
                            }
                            seenKeys.add(key);
                        }

                        // 선택된 컬럼 값 추출
                        List<String> values = new ArrayList<>();
                        for (String colName : selectedSourceColumns) {
                            values.add(rowData.getOrDefault(colName, ""));
                        }

                        // 계산 컬럼 추가
                        for (Calculation calc : profile.getCalculations()) {
                            String calcValue = calculationEngine.evaluate(calc, rowData);

                            if (calc.getInsertAfter() != null) {
                                int insertIdx = outputHeaders.indexOf(calc.getNewColumn());
                                if (insertIdx >= 0 && insertIdx <= values.size()) {
                                    values.add(insertIdx, calcValue);
                                } else {
                                    values.add(calcValue);
                                }
                            } else {
                                values.add(calcValue);
                            }

                            rowData.put(calc.getNewColumn(), calcValue);
                        }

                        writer.writeRow(values);
                        outputRows[0]++;  // 출력 행 카운트

                        if (rowCallback != null && outputRows[0] % 10000 == 0) {
                            rowCallback.onRow(outputRows[0]);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Row processing error", e);
                    }
                },
                // 진행률 콜백
                rowNum -> {
                    if (rowCallback != null) {
                        rowCallback.onRow(rowNum);
                    }
                }
            );

            writer.close();

            // 누락된 컬럼으로 인해 중단된 경우
            if (abortDueToMissingColumns[0]) {
                result.markFailed("Cancelled due to missing columns", null);
                // 빈 출력 파일 삭제
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return result;
            }

            result.markSuccess(inputRows[0], outputRows[0], duplicateRows[0], emptyRows[0]);

        } catch (Exception e) {
            result.markFailed(e.getMessage(), e);
            System.err.println("Conversion error [" + inputFile.getName() + "]: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 파일에 맞는 리더 찾기
     */
    private DataReader findReader(File file) {
        for (DataReader reader : readers) {
            if (reader.canRead(file)) {
                return reader;
            }
        }
        return null;
    }

    /**
     * 출력 형식에 따른 라이터 생성
     */
    private DataWriter createWriter(Profile profile) {
        String format = profile.getOptions().getOutputFormat();
        if ("xlsx".equalsIgnoreCase(format)) {
            return new XlsxDataWriter();
        }
        return new CsvDataWriter();
    }

    /**
     * 출력 파일명 결정
     */
    private String determineOutputFileName(Profile profile, File inputFile, String outputExt) {
        String baseName = inputFile.getName();
        // 기존 확장자 제거
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) {
            baseName = baseName.substring(0, dotIdx);
        }

        if (profile.getOutputFileName() != null && !profile.getOutputFileName().isEmpty()) {
            String profileOutput = profile.getOutputFileName();

            if (profileOutput.contains("{filename}")) {
                String result = profileOutput.replace("{filename}", baseName);
                // 확장자가 이미 있으면 교체, 없으면 추가
                if (result.contains(".")) {
                    int extIdx = result.lastIndexOf('.');
                    result = result.substring(0, extIdx) + outputExt;
                } else {
                    result = result + outputExt;
                }
                return result;
            }

            return baseName + "_" + profile.getProfileName() + outputExt;
        }

        return baseName + outputExt;
    }

    /**
     * 변환 취소
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 취소 상태 확인
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 행 처리 콜백
     */
    @FunctionalInterface
    public interface RowProgressCallback {
        void onRow(long rowNumber);
    }
}
