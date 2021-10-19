package org.jetbrains.compose.codeeditor.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertGotoDeclaration;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertLocalGotoDeclaration;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.platform;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.tempDir;

@ExtendWith(WrapperExtension.class)
class GTDTests {

    @Test
    void localJavaTest() {
        var file = tempDir + "/files/GTDTest.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openFile(file);
            assertLocalGotoDeclaration(project, file);
            project.closeProject();
        });
    }

    @Test
    void javaTest() {
        var projectDir = tempDir + "/projects/GTDJavaProject";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertGotoDeclaration(project, file, projectDir);
            project.closeProject();
        });
    }

    @Test
    void localKotlinTest() {
        var file = tempDir + "/files/GTDTest.kt";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openFile(file);
            assertLocalGotoDeclaration(project, file);
            project.closeProject();
        });
    }

    @Test
    void kotlinTest() {
        var projectDir = tempDir + "/projects/GTDKotlinProject";
        var file = "org/example/main.kt";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertGotoDeclaration(project, file, projectDir);
            project.closeProject();
        });
    }

}
