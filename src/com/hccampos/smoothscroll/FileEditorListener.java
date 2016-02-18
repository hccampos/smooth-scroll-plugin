package com.hccampos.smoothscroll;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.editor.Editor;
import java.util.*;

public class FileEditorListener implements FileEditorManagerListener {
    private Map<FileEditor, SmoothScrollMouseWheelListener> _listeners =
            new HashMap<FileEditor, SmoothScrollMouseWheelListener>();

    public void fileOpened(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
        FileEditor[] editors = fileEditorManager.getAllEditors();

        // Add a wheel listener to all editors.
        for(FileEditor fileEditor: editors) {
            if (fileEditor instanceof TextEditor) {
                SmoothScrollMouseWheelListener listener = new SmoothScrollMouseWheelListener(fileEditor);
                _listeners.put(fileEditor, listener);

                listener.startAnimating();

                Editor editor = ((TextEditor) fileEditor).getEditor();
                editor.getContentComponent().addMouseWheelListener(listener);
            }
        }
    }

    public void fileClosed(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
        Set<FileEditor> destroyedEditors = new HashSet<FileEditor>(_listeners.keySet());

        // Remove all the editors that are still active from the set of destroyed editors.
        for(FileEditor editor: fileEditorManager.getAllEditors()) {
            if (destroyedEditors.contains(editor)) {
                destroyedEditors.remove(editor);
            }
        }

        // Remove the wheel listener from all the destroyed editors.
        for (FileEditor fileEditor: destroyedEditors) {
            SmoothScrollMouseWheelListener listener = _listeners.get(fileEditor);

            if (listener != null) {
                listener.stopAnimating();

                Editor editor = ((TextEditor) fileEditor).getEditor();
                editor.getContentComponent().removeMouseWheelListener(listener);
            }
        }
    }

    public void selectionChanged(FileEditorManagerEvent fileEditorManagerEvent) { }
}