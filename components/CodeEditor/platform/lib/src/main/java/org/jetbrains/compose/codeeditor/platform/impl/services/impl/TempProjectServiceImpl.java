package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ModuleService;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.PathKt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempProjectServiceImpl extends AbstractProjectService {

    private Path projectFolder;

    public TempProjectServiceImpl(ApplicationService applicationService,
                                  FileService fileService, String projectName) {
        super(applicationService, fileService, projectName);
    }

    @Override
    protected Path getProjectFolder() {
        if (projectFolder == null) {
            var pf = myFileService.generateTempPath(myProjectName);
            try {
                Files.createDirectories(pf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            projectFolder = pf;
        }
        return projectFolder;
    }

    @Override
    protected ModuleService createModuleService() {
        return new TempModuleServiceImpl(myApplicationService, myFileService);
    }

    @Override
    protected void deleteProjectFiles(Project project) {
        super.deleteProjectFiles(project);
        PathKt.delete(projectFolder);
        projectFolder = null;
    }

}
