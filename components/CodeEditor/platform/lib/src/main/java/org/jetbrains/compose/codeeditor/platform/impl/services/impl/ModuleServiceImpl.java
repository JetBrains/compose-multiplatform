package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.PathKt;

import java.nio.file.Path;

public class ModuleServiceImpl extends AbstractModuleService {

    private Path myModuleFile;

    ModuleServiceImpl(ApplicationService applicationService, FileService fileService) {
        super(applicationService, fileService);
    }

    @Override
    protected Path getModuleFilePath() {
        if (myModuleFile == null) {
            myModuleFile = myFileService.generateTempPath(myModuleName + ".iml");
        }
        return myModuleFile;
    }

    @Override
    protected void deleteModuleFiles() {
        if (myModuleFile != null) {
            PathKt.delete(myModuleFile);
        }
    }

    @Override
    public VirtualFile refreshFile(String path) {
        path = FileUtil.toSystemIndependentName(path);
        if (path.startsWith(mySourceRoot.getPath())) {
            path = path.substring(mySourceRoot.getPath().length());
        }
        var vFile = findFileInSource(path);
        if (vFile == null) {
            throw new IllegalArgumentException("Path: " + path + " not found in project folder");
        }
        vFile.refresh(false, false);
        return vFile;
    }

    @Override
    protected VirtualFile defineSourceRoot() {
        return myFileService.synchronizeDirVfs(myProjectFolder);
    }

}
