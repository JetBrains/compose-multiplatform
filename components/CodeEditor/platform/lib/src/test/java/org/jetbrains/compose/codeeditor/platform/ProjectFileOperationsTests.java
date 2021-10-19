package org.jetbrains.compose.codeeditor.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertCodeCompletion;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.platform;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.replaceInFile;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.tempDir;

@ExtendWith(WrapperExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class ProjectFileOperationsTests {

    @Test
    @Order(1)
    void newFileTest() {
        var projectDir = tempDir + "/projects/fileOperationsProject";
        var file = projectDir + "/org/example/Main.java";
        var newFile = projectDir + "/org/example/Test.java";
        var sourceFile = tempDir + "/projects/simpleJavaProject/Test.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertCodeCompletion(project, file, List.of("ImplA"));

            var code = Files.readString(Path.of(sourceFile));
            Files.writeString(Path.of(newFile), code, StandardOpenOption.CREATE_NEW);
            assertCodeCompletion(project, newFile, List.of("ArrayList"));

            project.closeProject();
        });
    }



    @Test
    @Order(2)
    void addFileTest() {
        var projectDir = tempDir + "/projects/fileOperationsProject";
        var file = projectDir + "/org/example/Main.java";
        var implA = projectDir + "/org/example/a/ImplA.java";
        var implB = projectDir + "/org/example/a/ImplB.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertCodeCompletion(project, file, List.of("ImplA"), List.of("ImplB"));

            Files.copy(Path.of(implA), Path.of(implB));
            replaceInFile(implB, "ImplA", "ImplB");
            project.synchronizeProjectDirectory();
            assertCodeCompletion(project, file, List.of("ImplA", "ImplB"));

            project.closeProject();
        });
    }

    @Test
    @Order(3)
    void deleteFileTest() {
        var projectDir = tempDir + "/projects/fileOperationsProject";
        var file = projectDir + "/org/example/Main.java";
        var implB = projectDir + "/org/example/a/ImplB.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertCodeCompletion(project, file, List.of("ImplA", "ImplB"));

            Files.delete(Path.of(implB));
            project.synchronizeProjectDirectory();
            assertCodeCompletion(project, file, List.of("ImplA"), List.of("ImplB"));

            project.closeProject();
        });
    }

}
