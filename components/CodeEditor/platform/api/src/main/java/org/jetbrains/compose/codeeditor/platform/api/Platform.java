package org.jetbrains.compose.codeeditor.platform.api;

/**
 * Only one initialized instance of this interface can exist at runtime.
 * The platform cannot be re-initialized after stopping.
 */
public interface Platform {

    /**
     * Initializes the platform. Must be called first.
     */
    void init();

    /**
     * Stops the platform, closes projects, and all open resources.
     */
    void stop();

    /**
     * Creates a new project.
     * @param rootFolder the root folder of the project, which contains all the source files.
     */
    Project openProject(String rootFolder);

    /**
     * Creates a temporary project for only one file.
     * Allows to use code completion for this file.
     * @param filePath absolute path to the file
     */
    Project openFile(String filePath);
}
