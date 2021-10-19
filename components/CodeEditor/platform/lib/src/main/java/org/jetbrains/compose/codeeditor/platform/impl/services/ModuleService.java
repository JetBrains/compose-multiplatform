package org.jetbrains.compose.codeeditor.platform.impl.services;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.util.List;

public interface ModuleService extends Service {

    void createModule(Project project, Path projectFolder);

    void closeModule();

    Module getModule();

    void addLibraries(List<String> paths);

    VirtualFile getSourceRoot();

    void synchronizeSourceRoot();

    VirtualFile refreshFile(String path);

}
