package org.example.gui;

/**
 * 변환 진행률 정보
 */
public class ProgressInfo {
    private final int fileIndex;
    private final int totalFiles;
    private final long currentRow;
    private final String fileName;
    private final String status;

    public ProgressInfo(int fileIndex, int totalFiles, long currentRow, String fileName, String status) {
        this.fileIndex = fileIndex;
        this.totalFiles = totalFiles;
        this.currentRow = currentRow;
        this.fileName = fileName;
        this.status = status;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public long getCurrentRow() {
        return currentRow;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }
}
