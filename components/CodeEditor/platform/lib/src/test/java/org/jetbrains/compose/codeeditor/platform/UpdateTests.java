package org.jetbrains.compose.codeeditor.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertCodeCompletion;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.platform;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.replaceInFile;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.tempDir;

@ExtendWith(WrapperExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class UpdateTests {

    @Test
    @Order(1)
    void javaProjectImportedFileUpdateTest() {
        var projectDir = tempDir + "/projects/updateJavaProject";
        var file = projectDir + "/org/example/Main.java";
        var importedFile = projectDir + "/org/example/a/ClassA.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertCodeCompletion(project, file, List.of("getJ"), List.of("getI"));
            replaceInFile(importedFile,
                "getJ() {\n        return j;",
                "getI() {\n        return i;");
            project.synchronizeProjectDirectory();
            assertCodeCompletion(project, file, List.of("getI"), List.of("getJ"));
            project.closeProject();
        });
    }

    @Test
    @Order(2)
    void javaProjectUpdateTest() {
        var projectDir = tempDir + "/projects/updateJavaProject";
        var file = projectDir + "/org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openProject(projectDir);
            assertCodeCompletion(project, file, List.of("getI"), List.of("setI"));
            replaceInFile(file, "classA.g", "classA.s");
            assertCodeCompletion(project, file, List.of("setI"), List.of("getI"));
            project.closeProject();
        });
    }

    @Test
    @Order(3)
    void javaOneFileUpdateTest() {
        var file = tempDir + "/files/UpdateTest.java";
        Assertions.assertDoesNotThrow(() -> {
            var project = platform.openFile(file);
            assertCodeCompletion(project, file, List.of("SortedMap"), List.of("ArrayList"));
            replaceInFile(file, "new So", "new Ar");
            assertCodeCompletion(project, file, List.of("ArrayList"), List.of("SortedMap"));
            project.closeProject();
        });
    }

}
