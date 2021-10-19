package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class TempModuleServiceImpl extends AbstractModuleService {

    private Path moduleFile;

    TempModuleServiceImpl(ApplicationService applicationService, FileService fileService) {
        super(applicationService, fileService);
    }

    @Override
    protected Path getModuleFilePath() {
        if (moduleFile == null) {
            moduleFile = Paths.get(myProjectFolder.toString(), myModuleName + ".iml");
        }
        return moduleFile;
    }

    @Override
    protected void deleteModuleFiles() {
        if (myModule != null && mySourceRoot != null) {
            WriteCommandAction.runWriteCommandAction(myModule.getProject(), () -> {
                try {
                    myFileService.clearDir(mySourceRoot);
                } catch (IOException e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public VirtualFile refreshFile(String path) {
        var vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (vFile == null) {
            throw new IllegalArgumentException("Path: " + path + " not found");
        }
        vFile.refresh(false, true);
        return copyFileToSource(vFile);
    }

    private VirtualFile copyFileToSource(VirtualFile file) {
        String fileName = file.getName();
        var targetFile = myFileService.getFile(mySourceRoot, fileName);
        if (targetFile == null) {
            targetFile = myFileService.createFile(mySourceRoot, fileName);
        }
        copyContent(file, targetFile);
        return targetFile;
    }

    private void copyContent(VirtualFile file, VirtualFile targetFile) {
        try {
            WriteAction.runAndWait(() -> {
                targetFile.setBinaryContent(file.contentsToByteArray());
                FileDocumentManager.getInstance().reloadFiles(targetFile);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected VirtualFile defineSourceRoot() {
        return myFileService.createDirInMemory(myModuleName);
    }

}
