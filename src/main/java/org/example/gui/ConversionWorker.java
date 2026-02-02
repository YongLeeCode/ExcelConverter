package org.example.gui;

import org.example.model.ConversionResult;
import org.example.model.Profile;
import org.example.service.ExcelConverterService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 백그라운드 변환 작업
 */
public class ConversionWorker extends SwingWorker<List<ConversionResult>, ProgressInfo> {

    private final Profile profile;
    private final List<File> files;
    private final File outputDir;
    private final ExcelConverterService converterService;
    private final ConversionCallback callback;
    private final Component parentComponent;
    private final boolean mergeFiles;

    /**
     * 변환 완료 콜백 인터페이스
     */
    public interface ConversionCallback {
        void onProgress(ProgressInfo info);
        void onComplete(List<ConversionResult> results);
        void onError(Exception e);
        void onCancelled();
    }

    public ConversionWorker(Profile profile,
                            List<File> files,
                            File outputDir,
                            ExcelConverterService converterService,
                            ConversionCallback callback,
                            Component parentComponent,
                            boolean mergeFiles) {
        this.profile = profile;
        this.files = files;
        this.outputDir = outputDir;
        this.converterService = converterService;
        this.callback = callback;
        this.parentComponent = parentComponent;
        this.mergeFiles = mergeFiles;
    }

    @Override
    protected List<ConversionResult> doInBackground() {
        return converterService.convert(profile, files, outputDir,
            new ExcelConverterService.ProgressListener() {
                @Override
                public void onFileStart(int fileIndex, int totalFiles, File file) {
                    publish(new ProgressInfo(fileIndex, totalFiles, 0, file.getName(), "시작"));
                }

                @Override
                public void onProgress(int fileIndex, int totalFiles, long currentRow, String fileName) {
                    publish(new ProgressInfo(fileIndex, totalFiles, currentRow, fileName, "처리 중"));
                }

                @Override
                public void onFileComplete(int fileIndex, int totalFiles, ConversionResult result) {
                    publish(new ProgressInfo(fileIndex + 1, totalFiles,
                        result.getProcessedRows(), result.getInputFile().getName(),
                        result.isSuccess() ? "완료" : "오류"));
                }

                @Override
                public void onError(String message, Exception e) {
                    publish(new ProgressInfo(-1, -1, 0, message, "오류"));
                }

                @Override
                public void onAllComplete(List<ConversionResult> results) {
                    // done()에서 처리
                }

                @Override
                public boolean onMissingColumns(String fileName, List<String> missingColumns) {
                    AtomicBoolean result = new AtomicBoolean(false);
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            StringBuilder message = new StringBuilder();
                            message.append("Missing columns in file: ").append(fileName).append("\n\n");
                            message.append("The following columns were not found:\n");
                            for (String col : missingColumns) {
                                message.append("  • ").append(col).append("\n");
                            }
                            message.append("\nDo you want to continue without these columns?");

                            int choice = JOptionPane.showConfirmDialog(
                                parentComponent,
                                message.toString(),
                                "Missing Columns",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            );
                            result.set(choice == JOptionPane.YES_OPTION);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        result.set(false);
                    }
                    return result.get();
                }
            }, mergeFiles);
    }

    @Override
    protected void process(List<ProgressInfo> chunks) {
        ProgressInfo info = chunks.get(chunks.size() - 1);
        callback.onProgress(info);
    }

    @Override
    protected void done() {
        try {
            if (isCancelled()) {
                callback.onCancelled();
            } else {
                List<ConversionResult> results = get();
                callback.onComplete(results);
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
