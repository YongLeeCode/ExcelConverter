package org.example.model;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 변환 결과 정보
 */
public class ConversionResult {

    public enum Status {
        SUCCESS,    // 성공
        FAILED,     // 실패
        CANCELLED,  // 취소됨
        SKIPPED     // 건너뜀 (이미 존재 등)
    }

    private File inputFile;       // 입력 파일
    private File outputFile;      // 출력 파일
    private long inputRows;       // 입력 행 수 (헤더 제외)
    private long outputRows;      // 출력 행 수
    private long duplicateRows;   // 중복으로 건너뛴 행 수
    private long emptyRows;       // 빈 행으로 건너뛴 행 수
    private long totalRows;       // 전체 행 수 (deprecated, use inputRows)
    private long processedRows;   // 처리된 행 수 (deprecated, use outputRows)
    private long skippedRows;     // 건너뛴 행 수 (빈 행 등)
    private Status status;        // 결과 상태
    private String errorMessage;  // 에러 메시지
    private Exception exception;  // 예외 객체
    private LocalDateTime startTime;   // 시작 시간
    private LocalDateTime endTime;     // 종료 시간

    public ConversionResult() {
        this.startTime = LocalDateTime.now();
    }

    public ConversionResult(File inputFile) {
        this();
        this.inputFile = inputFile;
    }

    // === Getters & Setters ===

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public long getProcessedRows() {
        return processedRows;
    }

    public void setProcessedRows(long processedRows) {
        this.processedRows = processedRows;
    }

    public long getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(long skippedRows) {
        this.skippedRows = skippedRows;
    }

    public long getInputRows() {
        return inputRows;
    }

    public void setInputRows(long inputRows) {
        this.inputRows = inputRows;
    }

    public long getOutputRows() {
        return outputRows;
    }

    public void setOutputRows(long outputRows) {
        this.outputRows = outputRows;
    }

    public long getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(long duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public long getEmptyRows() {
        return emptyRows;
    }

    public void setEmptyRows(long emptyRows) {
        this.emptyRows = emptyRows;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    // === 유틸리티 메서드 ===

    /**
     * 성공으로 완료 처리
     */
    public void markSuccess(long processedRows) {
        this.status = Status.SUCCESS;
        this.processedRows = processedRows;
        this.outputRows = processedRows;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 상세 정보와 함께 성공으로 완료 처리
     */
    public void markSuccess(long inputRows, long outputRows, long duplicateRows, long emptyRows) {
        this.status = Status.SUCCESS;
        this.inputRows = inputRows;
        this.outputRows = outputRows;
        this.duplicateRows = duplicateRows;
        this.emptyRows = emptyRows;
        this.processedRows = outputRows;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 실패로 완료 처리
     */
    public void markFailed(String errorMessage, Exception exception) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 취소로 완료 처리
     */
    public void markCancelled() {
        this.status = Status.CANCELLED;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 처리 시간 반환
     */
    public Duration getDuration() {
        if (startTime == null || endTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, endTime);
    }

    /**
     * 처리 시간을 보기 좋은 형식으로 반환
     */
    public String getDurationText() {
        Duration d = getDuration();
        long seconds = d.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    /**
     * 성공 여부
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    @Override
    public String toString() {
        String fileName = inputFile != null ? inputFile.getName() : "unknown";
        return String.format("%s: %s (%d 행, %s)",
            fileName, status, processedRows, getDurationText());
    }
}
