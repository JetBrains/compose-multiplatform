package org.jetbrains.compose.codeeditor.platform.api;

import java.util.Arrays;
import java.util.List;

public interface Project {

    /**
     * Loads libraries for the project.
     *
     * @param paths list of paths to libraries.
     * Each item can refer to a jar library file or to a directory.
     * If a directory is specified, all libraries from it and all its subdirectories will be loaded.
     */
    default void addLibraries(String... paths) {
        addLibraries(Arrays.asList(paths));
    }

    void addLibraries(List<String> paths);

    /**
     * Refreshes information about the project directory structure.
     * Must be called after any manipulation in the project directory.
     */
    void synchronizeProjectDirectory();

    /**
     * Calls the code completion for the file at the offset.
     *
     * @param path path to a source file.
     * @param caretOffset - caret position offset
     */
    List<CodeCompletionElement> getCodeCompletion(String path, int caretOffset);

    /**
     * Returns the coordinates of the element declaration.
     *
     * @param path path to a source file.
     * @param caretOffset caret position offset
     */
    GotoDeclarationData gotoDeclaration(String path, int caretOffset);

    /**
     * Closes the project, clears resources.
     */
    void closeProject();
}
