package org.jetbrains.compose.codeeditor.platform.impl;

import org.jetbrains.compose.codeeditor.platform.api.CodeCompletionElement;
import org.jetbrains.compose.codeeditor.platform.api.GotoDeclarationData;
import org.jetbrains.compose.codeeditor.platform.api.Project;
import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ProjectService;
import org.jetbrains.compose.codeeditor.platform.impl.services.impl.ProjectServiceImpl;
import org.jetbrains.compose.codeeditor.platform.impl.services.impl.TempProjectServiceImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class IpwProject implements Project, Disposable {

    private ProjectService projectService;
    private boolean isDisposed;

    IpwProject(Path path, Disposable rootDisposable, ApplicationService applicationService, FileService fileService) {
        if (Files.isDirectory(path)) {
            String projectName = path.getFileName().toString();
            projectService = new ProjectServiceImpl(applicationService, fileService, projectName, path);
        } else {
            String projectName = FileUtil.getNameWithoutExtension(path.getFileName().toString());
            projectService = new TempProjectServiceImpl(applicationService, fileService, projectName);
        }
        projectService.init(this);
        projectService.createProject();
        Disposer.register(rootDisposable, this);
    }

    @Override
    public void dispose() {
        isDisposed = true;
        projectService = null;
    }

    @Override
    public void addLibraries(List<String> paths) {
        projectService.addLibraries(paths);
    }

    @Override
    public void synchronizeProjectDirectory() {
        projectService.synchronizeProjectDir();
    }

    @Override
    public List<CodeCompletionElement> getCodeCompletion(String path, int caretOffset) {
        return mapLookupElements(projectService.getCodeCompletion(path, caretOffset));
    }

    @Override
    public GotoDeclarationData gotoDeclaration(String path, int caretOffset) {
        return projectService.gotoDeclaration(path, caretOffset);
    }

    private List<CodeCompletionElement> mapLookupElements(List<LookupElement> elements) {
        return ReadAction.compute(() -> elements.stream()
                                                .map(CcElement::new)
                                                .collect(Collectors.toList()));
    }

    @Override
    public void closeProject() {
        if (isDisposed) {
            throw new IllegalStateException("The project is already closed");
        }
        Disposer.dispose(this);
    }
}
