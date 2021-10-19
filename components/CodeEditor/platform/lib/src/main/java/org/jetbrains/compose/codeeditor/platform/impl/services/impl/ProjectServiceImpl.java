package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ModuleService;

import java.nio.file.Path;

public class ProjectServiceImpl extends AbstractProjectService {

    private final Path myProjectFolder;

    public ProjectServiceImpl(ApplicationService applicationService, FileService fileService,
                              String projectName, Path projectFolder) {
        super(applicationService, fileService, projectName);
        myProjectFolder = projectFolder;
    }

    @Override
    protected Path getProjectFolder() {
        return myProjectFolder;
    }

    @Override
    protected ModuleService createModuleService() {
        return new ModuleServiceImpl(myApplicationService, myFileService);
    }

}
