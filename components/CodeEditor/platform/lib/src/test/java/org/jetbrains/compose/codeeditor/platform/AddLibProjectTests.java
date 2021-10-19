package org.jetbrains.compose.codeeditor.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertCodeCompletion;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.platform;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.tempDir;

@ExtendWith(WrapperExtension.class)
class AddLibProjectTests {

    @Test
    void severalCallsTest() {
        var projectDir = tempDir + "/projects/libJavaProject";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            project.addLibraries(tempDir + "/lib/api.jar");
            project.addLibraries(tempDir + "/lib/implA.jar");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassA", "ClassD"), List.of("ClassB", "ClassC"));
            project.closeProject();
        });
    }

    @Test
    void oneFileAndOneDirInOneCallTest() {
        var projectDir = tempDir + "/projects/libJavaProject";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            project.addLibraries(tempDir + "/lib/api.jar", tempDir + "/lib/sub");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassB", "ClassC", "ClassD"), List.of("ClassA"));
            project.closeProject();
        });
    }

    @Test
    void rootDirTest() {
        var projectDir = tempDir + "/projects/libJavaProject";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            project.addLibraries(tempDir + "/lib");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassA", "ClassB", "ClassC", "ClassD"));
            project.closeProject();
        });
    }

    @Test
    void onlyApiTest() {
        var projectDir = tempDir + "/projects/libJavaProject";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            project.addLibraries(tempDir + "/lib/api.jar");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassD"), List.of("ClassA", "ClassB", "ClassC"));
            project.closeProject();
        });
    }

    @Test
    void addLibsBetweenCodeCompletionCallsTest() {
        var projectDir = tempDir + "/projects/libJavaProject";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);

            project.addLibraries(tempDir + "/lib/api.jar");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassD"), List.of("ClassA", "ClassB", "ClassC"));

            project.addLibraries(tempDir + "/lib/implA.jar");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassA", "ClassD"), List.of("ClassB", "ClassC"));

            project.addLibraries(tempDir + "/lib");
            assertCodeCompletion(project, file, projectDir,
                List.of("ClassA", "ClassB", "ClassC", "ClassD"));

            project.closeProject();
        });
    }

}
