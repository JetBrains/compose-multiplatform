package org.jetbrains.compose.codeeditor.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.assertCodeCompletion;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.platform;
import static org.jetbrains.compose.codeeditor.platform.WrapperExtension.tempDir;

@ExtendWith(WrapperExtension.class)
class MultipleProjectsTests {

    /**
     * Tests that a project can be opened when another project is open and that after closing one project while another
     * is open, the other project is successfully closed
     */
    @Nested
    class OpenCloseTests {

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinClose_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinClose_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

    }

    /**
     * Tests the effect of the kotlin project on the code completion in the java project
     */
    @Nested
    class EffectKotlinOnJavaTests {

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        // tests that the code completion continues to work
        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

    }

    /**
     * Tests the effect of the java project on the code completion in the kotlin project
     */
    @Nested
    class EffectJavaOnKotlinTests {

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_KotlinClose_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        // tests that the code completion continues to work
        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

    }

    /**
     * Tests that the code completion call works in both projects
     */
    @Nested
    class OneCallPerProjectTests {

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

    }

    /**
     * Tests that calls to the code completion continue to work with different calls to another project
     */
    @Nested
    class JavaKeepsWorkingTests {

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_JavaCC_KotlinClose_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

    }

    /**
     * Tests that calls to the code completion continue to work with different calls to another project
     */
    @Nested
    class KotlinKeepsWorkingTests {

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinCC_JavaCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

    }

    @Nested
    class MultipleCallsTests {

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaProjectDir = tempDir + "/projects/simpleJavaProject";
            var javaFile = "Test.java";
            var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
            var kotlinFile = "test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openProject(kotlinProjectDir);
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                var javaProject = platform.openProject(javaProjectDir);
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

    }

    @Test
    void reopenKotlinTest() {
        var javaProjectDir = tempDir + "/projects/simpleJavaProject";
        var javaFile = "Test.java";
        var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
        var kotlinFile = "test.kt";
        Assertions.assertDoesNotThrow(() -> {
            var kotlinProject = platform.openProject(kotlinProjectDir);
            var javaProject = platform.openProject(javaProjectDir);
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
            kotlinProject.closeProject();
            assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
            kotlinProject = platform.openProject(kotlinProjectDir);
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
            javaProject.closeProject();
            kotlinProject.closeProject();
        });
    }

    @Test
    void reopenJavaTest() {
        var javaProjectDir = tempDir + "/projects/simpleJavaProject";
        var javaFile = "Test.java";
        var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
        var kotlinFile = "test.kt";
        Assertions.assertDoesNotThrow(() -> {
            var kotlinProject = platform.openProject(kotlinProjectDir);
            var javaProject = platform.openProject(javaProjectDir);
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
            javaProject.closeProject();
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            javaProject = platform.openProject(javaProjectDir);
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            assertCodeCompletion(javaProject, javaFile, javaProjectDir, List.of("ArrayList"));
            kotlinProject.closeProject();
            javaProject.closeProject();
        });
    }

    @Test
    void threeProjectsTest() {
        var javaProjectDir1 = tempDir + "/projects/simpleJavaProject";
        var javaFile1 = "Test.java";
        var javaProjectDir2 = tempDir + "/projects/javaProject";
        var javaFile2 = "org/example/Main.java";
        var kotlinProjectDir = tempDir + "/projects/simpleKotlinProject";
        var kotlinFile = "test.kt";
        Assertions.assertDoesNotThrow(() -> {
            var javaProject1 = platform.openProject(javaProjectDir1);
            var javaProject2 = platform.openProject(javaProjectDir2);
            var kotlinProject = platform.openProject(kotlinProjectDir);
            assertCodeCompletion(javaProject1, javaFile1, javaProjectDir1, List.of("ArrayList"));
            assertCodeCompletion(javaProject2, javaFile2, javaProjectDir2, List.of("getI"));
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            javaProject1.closeProject();
            assertCodeCompletion(javaProject2, javaFile2, javaProjectDir2, List.of("getI"));
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            javaProject2.closeProject();
            assertCodeCompletion(kotlinProject, kotlinFile, kotlinProjectDir, List.of("SortedMap"));
            kotlinProject.closeProject();
        });
    }

    @Test
    void twoProjectsWithLibsTest() {
        var projectDir1 = tempDir + "/projects/libJavaProject";
        var projectDir2 = tempDir + "/projects/libJavaProject2";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openProject(projectDir1);
            var project2 = platform.openProject(projectDir2);
            project1.addLibraries(tempDir + "/lib/api.jar");
            project1.addLibraries(tempDir + "/lib/implA.jar");
            project2.addLibraries(tempDir + "/lib");
            assertCodeCompletion(project1, file, projectDir1,
                List.of("ClassA", "ClassD"), List.of("ClassB", "ClassC"));
            assertCodeCompletion(project2, file, projectDir2,
                List.of("ClassA", "ClassB", "ClassC", "ClassD"));
            project1.closeProject();
            project2.closeProject();
        });
    }

    @Test
    void addLibsBetweenCodeCompletionCallsTest() {
        var projectDir1 = tempDir + "/projects/libJavaProject";
        var projectDir2 = tempDir + "/projects/libJavaProject2";
        var file = "org/example/Main.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openProject(projectDir1);
            var project2 = platform.openProject(projectDir2);

            project1.addLibraries(tempDir + "/lib/api.jar");
            project2.addLibraries(tempDir + "/lib/api.jar");
            assertCodeCompletion(project1, file, projectDir1,
                List.of("ClassD"), List.of("ClassA", "ClassB", "ClassC"));
            assertCodeCompletion(project2, file, projectDir2,
                List.of("ClassD"), List.of("ClassA", "ClassB", "ClassC"));

            project1.addLibraries(tempDir + "/lib/implA.jar");
            project2.addLibraries(tempDir + "/lib/sub");
            assertCodeCompletion(project1, file, projectDir1,
                List.of("ClassA", "ClassD"), List.of("ClassB", "ClassC"));
            assertCodeCompletion(project2, file, projectDir2,
                List.of("ClassB", "ClassC", "ClassD"), List.of("ClassA"));

            project1.addLibraries(tempDir + "/lib");
            project2.addLibraries(tempDir + "/lib");
            assertCodeCompletion(project1, file, projectDir1,
                List.of("ClassA", "ClassB", "ClassC", "ClassD"));
            assertCodeCompletion(project2, file, projectDir2,
                List.of("ClassA", "ClassB", "ClassC", "ClassD"));

            project1.closeProject();
            project2.closeProject();
        });
    }
}
