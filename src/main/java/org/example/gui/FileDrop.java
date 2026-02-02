package org.example.gui;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

/**
 * 드래그 앤 드롭 지원 유틸리티
 */
public class FileDrop {

    public interface Listener {
        void filesDropped(File[] files);
    }

    public FileDrop(JComponent component, Listener listener) {
        new DropTarget(component, DnDConstants.ACTION_COPY, new DropTargetListener() {

            @Override
            public void dragEnter(DropTargetDragEvent event) {
                if (isDragOk(event)) {
                    event.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    event.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent event) {
                // 드래그 중
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent event) {
                if (isDragOk(event)) {
                    event.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    event.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent event) {
                // 드래그 영역 벗어남
            }

            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    Transferable transferable = event.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        event.acceptDrop(DnDConstants.ACTION_COPY);

                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable
                            .getTransferData(DataFlavor.javaFileListFlavor);

                        if (listener != null) {
                            listener.filesDropped(files.toArray(new File[0]));
                        }

                        event.dropComplete(true);
                    } else {
                        event.rejectDrop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    event.rejectDrop();
                }
            }

            private boolean isDragOk(DropTargetDragEvent event) {
                return event.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
        });
    }
}
