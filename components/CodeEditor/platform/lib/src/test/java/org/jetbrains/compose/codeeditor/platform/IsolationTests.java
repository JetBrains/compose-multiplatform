package org.jetbrains.compose.codeeditor.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertCodeCompletion;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.platform;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.tempDir;

@ExtendWith(WrapperExtension.class)
class IsolationTests {

    @Test
    void twoFilesWithImplementationsOfTheSameInterfaceTest() {
        var file1 = tempDir + "/files/IsolationTest1.java";
        var file2 = tempDir + "/files/IsolationTest2.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openFile(file1);
            var project2 = platform.openFile(file2);
            assertCodeCompletion(project1, file1, List.of("IsolationTest1"), List.of("IsolationTest2"));
            assertCodeCompletion(project2, file2, List.of("IsolationTest2"), List.of("IsolationTest1"));
            project1.closeProject();
            project2.closeProject();
        });
    }

    @Test
    void twoProjectsWithImplementationsOfTheSameInterfaceTest() {
        var projectDir1 = tempDir + "/projects/isolationProject1";
        var projectDir2 = tempDir + "/projects/isolationProject2";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openProject(projectDir1);
            var project2 = platform.openProject(projectDir2);
            assertCodeCompletion(project1, file, projectDir1, List.of("IsoClass1"), List.of("IsoClass2"));
            assertCodeCompletion(project2, file, projectDir2, List.of("IsoClass2"), List.of("IsoClass1"));
            project1.closeProject();
            project2.closeProject();
        });
    }

    @Test
    void twoProjectsWithImplementationsOfTheSameLibTest() {
        var projectDir1 = tempDir + "/projects/isolationLibProject1";
        var projectDir2 = tempDir + "/projects/isolationLibProject2";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openProject(projectDir1);
            var project2 = platform.openProject(projectDir2);
            project1.addLibraries(tempDir + "/lib/api.jar");
            project2.addLibraries(tempDir + "/lib/api.jar");
            assertCodeCompletion(project1, file, projectDir1, List.of("Impl1"), List.of("Impl2"));
            assertCodeCompletion(project2, file, projectDir2, List.of("Impl2"), List.of("Impl1"));
            project1.closeProject();
            project2.closeProject();
        });
    }

}
