package org.jetbrains.compose.codeeditor.platform.impl.services;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileService extends Service {

    Path getTempRoot();

    Path generateTempPath(String fileName);

    Path generateTempPath(String fileName, Path root);

    /**
     * Creates in-memory directory temp:///some/path.
     * This method should be only called within write-action.
     */
    VirtualFile createDirInMemory(String dirName);

    void clearDirInMemory(VirtualFile dir) throws IOException;

    void clearDir(VirtualFile dir) throws IOException;

    VirtualFile getFile(VirtualFile root, String path);

    VirtualFile createFile(VirtualFile root, String path);

    void copyDir(VirtualFile src, VirtualFile dst);

    VirtualFile synchronizeDirVfs(Path dir);

    void synchronizeDirVfs(VirtualFile dir);

}
