package org.jetbrains.compose.codeeditor.platform.impl.services;

import org.jetbrains.compose.codeeditor.platform.impl.GTDData;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;

import java.util.List;

public interface ProjectService extends Service {

    void createProject();

    void closeProject();

    Project getProject();

    void addLibraries(List<String> paths);

    void synchronizeProjectDir();

    List<LookupElement> getCodeCompletion(String path, int caretOffset);

    GTDData gotoDeclaration(String path, int caretOffset);

}
