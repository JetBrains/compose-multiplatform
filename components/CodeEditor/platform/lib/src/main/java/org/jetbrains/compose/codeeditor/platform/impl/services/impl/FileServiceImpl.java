package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.util.PathUtil;
import com.intellij.util.io.Ksuid;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.intellij.openapi.application.PathManager.PROPERTY_CONFIG_PATH;
import static com.intellij.openapi.application.PathManager.PROPERTY_HOME_PATH;
import static com.intellij.openapi.application.PathManager.PROPERTY_SYSTEM_PATH;

public class FileServiceImpl extends IpwService implements FileService {
    private static final String TMP_ROOT_FOLDER_NAME = "ipwRoot";
    private static final String HOME_FILES_PATH_IN_RESOURCES = "home";

    private Path tmpRootPath;

    @Override
    protected void doInit() {
        try {
            createTmpDirStructure();
            copyHomeResources();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        FileUtilRt.delete(tmpRootPath.toFile());
    }

    @Override
    public Path getTempRoot() {
        return tmpRootPath;
    }

    @Override
    public Path generateTempPath(String fileName) {
        return generateTempPath(fileName, tmpRootPath);
    }

    @Override
    public Path generateTempPath(String fileName, Path root) {
        Path path = root.resolve(generateName(fileName));
        if (Files.exists(path)) {
            throw new IllegalStateException("Path " + path + " must be unique but already exists");
        }
        return path;
    }

    @Override
    public VirtualFile createDirInMemory(String dirName) {
        var root = Objects.requireNonNull(VirtualFileManager.getInstance().findFileByUrl("temp:///"));
        root.refresh(false, false);
        VirtualFile dir;
        try {
            dir = root.createChildDirectory(this, dirName);
            clearDirInMemory(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dir;
    }

    @Override
    public void clearDirInMemory(VirtualFile dir) throws IOException {
        var tempFs = (TempFileSystem)dir.getFileSystem();
        for (var child : dir.getChildren()) {
            if (!tempFs.exists(child)) {
                tempFs.createChildFile(this, dir, child.getName());
            }
            child.delete(this);
        }
    }

    @Override
    public void clearDir(VirtualFile dir) throws IOException {
        for (var child : dir.getChildren()) {
            child.delete(this);
        }
    }

    @Override
    public VirtualFile getFile(VirtualFile root, String path) {
        VirtualFile result = root.findFileByRelativePath(path);
        if (result == null) {
            root.refresh(false, true);
            result = root.findFileByRelativePath(path);
        }
        return result;
    }

    @Override
    public VirtualFile createFile(VirtualFile root, String path) {
        try {
            return WriteAction.computeAndWait(() -> doCreateFile(root, path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void copyDir(VirtualFile src, VirtualFile dst) {
        WriteAction.runAndWait(() -> {
            try {
                refreshRecursively(src);
                VfsUtil.copyDirectory(this, src, dst, VirtualFileFilter.ALL);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void refreshRecursively(VirtualFile file) {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                file.getChildren();
                return true;
            }
        });
        file.refresh(false, true);
    }

    private VirtualFile doCreateFile(VirtualFile root, String targetPath) throws IOException {
        String dirPath = PathUtil.getParentPath(targetPath);
        String fileName = PathUtil.getFileName(targetPath);
        VirtualFile targetDir = findOrCreateDir(root, dirPath);
        return targetDir.createChildData(this, fileName);
    }

    private VirtualFile findOrCreateDir(VirtualFile root, String relativePath) throws IOException {
        if (relativePath.isEmpty()) return root;

        List<String> dirs = StringUtil.split(StringUtil.trimStart(relativePath, "/"), "/");
        for (String dirName : dirs) {
            if (".".equals(dirName)) continue;

            if ("..".equals(dirName)) {
                root = root.getParent();
                if (root == null) throw new IllegalArgumentException("Invalid path: " + relativePath);
                continue;
            }

            VirtualFile dir = root.findChild(dirName);
            if (dir != null) {
                root = dir;
            } else {
                root = root.createChildDirectory(this, dirName);
            }
        }

        return root;
    }

    @Override
    public VirtualFile synchronizeDirVfs(Path dir) {
        var virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
            FileUtil.toSystemIndependentName(dir.toString())
        );
        if (virtualFile != null) {
            virtualFile.getChildren();
            virtualFile.refresh(false, true);
        }
        return virtualFile;
    }

    @Override
    public void synchronizeDirVfs(VirtualFile dir) {
        dir.getChildren();
        dir.refresh(false, true);
    }

    private void createTmpDirStructure() throws IOException {
        File tmpRoot = FileUtil.createTempDirectory(TMP_ROOT_FOLDER_NAME, "", true);
        String ideaSystem = FileUtil.createTempDirectory(tmpRoot, "idea-system", "", false).getPath();
        String ideaConfig = FileUtil.createTempDirectory(tmpRoot, "idea-config", "", false).getPath();
        tmpRootPath = tmpRoot.toPath();

        System.setProperty(PROPERTY_HOME_PATH, tmpRootPath.toString());
        System.setProperty(PROPERTY_SYSTEM_PATH, ideaSystem);
        System.setProperty(PROPERTY_CONFIG_PATH, ideaConfig);
    }

    private void copyHomeResources() throws IOException {
        URL sourceUrl = getClass().getResource('/' + HOME_FILES_PATH_IN_RESOURCES);
        if (sourceUrl != null) {
            if (URLUtil.JAR_PROTOCOL.equals(sourceUrl.getProtocol())) {
                copyHomeResourcesFromJar(sourceUrl);
            } else {
                copyHomeResourcesFromFile(sourceUrl);
            }
        }
    }

    private void copyHomeResourcesFromJar(URL sourceUrl) throws IOException {
        String pathToJarFile = Objects.requireNonNull(URLUtil.splitJarUrl(sourceUrl.toString())).getFirst();

        try (JarFile jarFile = new JarFile(pathToJarFile)) {
            int prefixLength = HOME_FILES_PATH_IN_RESOURCES.length() + 1;
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String fileName = entry.getName();

                if (fileName.startsWith(HOME_FILES_PATH_IN_RESOURCES)) {
                    if (!entry.isDirectory()) {
                        fileName = fileName.substring(prefixLength);
                        Path dest = tmpRootPath.resolve(fileName);
                        Files.createDirectories(dest.getParent());
                        try (var sourceIs = jarFile.getInputStream(entry)) {
                            Files.copy(sourceIs, dest);
                        }
                    }
                }
            }
        }
    }

    private void copyHomeResourcesFromFile(URL sourceUrl) throws IOException {
        File sourceDir = URLUtil.urlToFile(sourceUrl);
        File destDir = tmpRootPath.toFile();
        FileUtil.copyDir(sourceDir, destDir);
    }

    private String generateName(String fileName) {
        var nameBuilder = new StringBuilder(fileName.length() + 1 + Ksuid.MAX_ENCODED_LENGTH);
        int extIndex = fileName.lastIndexOf('.');
        if (!fileName.isEmpty() && extIndex != 0) {
            if (extIndex == -1) {
                nameBuilder.append(fileName);
            } else {
                nameBuilder.append(fileName, 0, extIndex);
            }
            nameBuilder.append('_');
        }
        nameBuilder.append(Ksuid.generate());
        if (extIndex != -1) {
            nameBuilder.append(fileName, extIndex, fileName.length());
        }
        return nameBuilder.toString();
    }
}
