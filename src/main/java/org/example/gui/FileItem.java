package org.example.gui;

import java.io.File;

/**
 * 파일 목록 아이템
 */
public class FileItem {
    private final File file;

    public FileItem(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        long sizeKB = file.length() / 1024;
        String sizeStr = sizeKB > 1024 ?
            String.format("%.1f MB", sizeKB / 1024.0) :
            String.format("%d KB", sizeKB);
        return file.getName() + "  (" + sizeStr + ")";
    }
}
