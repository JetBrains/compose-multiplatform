package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import javax.swing.JComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class IpwDataProvider implements DataProvider, DataContext {
    private final Project myProject;

    IpwDataProvider(Project project) {
        myProject = project;
    }

    @Override
    @Nullable
    public Object getData(@NotNull @NonNls String dataId) {
        if (myProject.isDisposed()) {
            throw new RuntimeException("IpwDataProvider is already disposed for " + myProject);
        }

        if (CommonDataKeys.PROJECT.is(dataId)) {
            return myProject;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(myProject);
        if (manager == null) return null;

        if (CommonDataKeys.EDITOR.is(dataId) || OpenFileDescriptor.NAVIGATE_IN_EDITOR.is(dataId)) {
            return manager instanceof FileEditorManagerImpl
                ? ((FileEditorManagerImpl)manager).getSelectedTextEditor(true)
                : manager.getSelectedTextEditor();
        } else if (PlatformDataKeys.FILE_EDITOR.is(dataId)) {
            Editor editor = manager.getSelectedTextEditor();
            return editor == null ? null : TextEditorProvider.getInstance().getTextEditor(editor);
        } else {
            Editor editor = getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                Object managerData = manager.getData(dataId, editor, editor.getCaretModel().getCurrentCaret());
                if (managerData != null) {
                    return managerData;
                }
                JComponent component = editor.getContentComponent();
                if (component instanceof EditorComponentImpl) {
                    return ((EditorComponentImpl)component).getData(dataId);
                }
            }
        }

        return null;
    }
}
