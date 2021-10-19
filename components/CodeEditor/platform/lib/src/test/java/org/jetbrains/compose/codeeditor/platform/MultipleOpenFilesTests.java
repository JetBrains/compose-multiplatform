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
class MultipleOpenFilesTests {

    /**
     * Tests that a project can be opened when another project is open and that after closing one project while another
     * is open, the other project is successfully closed
     */
    @Nested
    class OpenCloseTests {

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinClose_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinClose_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
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
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        // tests that the code completion continues to work
        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
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
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_KotlinClose_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        // tests that the code completion continues to work
        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
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
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
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
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_JavaCC_KotlinClose_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                kotlinProject.closeProject();
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
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
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinCC_JavaCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

    }

    @Nested
    class MultipleCallsTests {

        @Test
        void javaKotlin_JavaOpen_KotlinOpen_JavaCC_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_JavaOpen_JavaCC_KotlinCC_JavaCC_KotlinCC_JavaClose_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_JavaOpen_JavaCC_KotlinOpen_JavaCC_KotlinCC_JavaClose_KotlinCC_KotlinClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                javaProject.closeProject();
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
            });
        }

        @Test
        void javaKotlin_KotlinOpen_KotlinCC_JavaOpen_JavaCC_KotlinCC_KotlinClose_JavaCC_JavaClose_Test() {
            var javaFile = tempDir + "/files/TestAr.java";
            var kotlinFile = tempDir + "/files/test.kt";
            Assertions.assertDoesNotThrow(() -> {
                var kotlinProject = platform.openFile(kotlinFile);
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                var javaProject = platform.openFile(javaFile);
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
                kotlinProject.closeProject();
                assertCodeCompletion(javaProject, javaFile, List.of("ArrayList"));
                javaProject.closeProject();
            });
        }

    }

    @Nested
    class TwoJavaFilesTests {

        @Test
        void java_JavaOpen_JavaOpen_Java1CC_Java2CC_JavaClose_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_Java2CC_JavaClose_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_JavaOpen_Java1CC_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_Java1CC_Java2CC_JavaClose_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_Java1CC_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_Java2CC_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_JavaOpen_Java1CC_Java2CC_JavaClose_Java1CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                javaProject1.closeProject();
            });
        }

        @Test
        void java_JavaOpen_JavaOpen_Java1CC_Java2CC_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_Java1CC_Java2CC_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_Java1CC_JavaOpen_Java2CC_Java1CC_Java2CC_JavaClose_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_JavaOpen_Java1CC_Java2CC_Java1CC_Java2CC_JavaClose_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject1.closeProject();
                javaProject2.closeProject();
            });
        }

        @Test
        void java_JavaOpen_JavaOpen_Java1CC_Java2CC_Java1CC_JavaClose_Java2CC_JavaClose_Test() {
            var javaFile1 = tempDir + "/files/TestAr.java";
            var javaFile2 = tempDir + "/files/TestSo.java";
            Assertions.assertDoesNotThrow(() -> {
                var javaProject1 = platform.openFile(javaFile1);
                var javaProject2 = platform.openFile(javaFile2);
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
                javaProject1.closeProject();
                assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
                javaProject2.closeProject();
            });
        }

    }

    @Test
    void threeFilesTest() {
        var javaFile1 = tempDir + "/files/TestAr.java";
        var javaFile2 = tempDir + "/files/TestSo.java";
        var kotlinFile = tempDir + "/files/test.kt";
        Assertions.assertDoesNotThrow(() -> {
            var javaProject1 = platform.openFile(javaFile1);
            var javaProject2 = platform.openFile(javaFile2);
            var kotlinProject = platform.openFile(kotlinFile);
            assertCodeCompletion(javaProject1, javaFile1, List.of("ArrayList"));
            assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
            assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
            javaProject1.closeProject();
            assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
            assertCodeCompletion(javaProject2, javaFile2, List.of("SortedMap"));
            javaProject2.closeProject();
            assertCodeCompletion(kotlinProject, kotlinFile, List.of("SortedMap"));
            kotlinProject.closeProject();
        });
    }

    @Test
    void twoFilesWithLibsTest() {
        var file1 = tempDir + "/files/LibTest.java";
        var file2 = tempDir + "/files/LibTest2.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openFile(file1);
            var project2 = platform.openFile(file2);
            project1.addLibraries(tempDir + "/lib/api.jar", tempDir + "/lib/implA.jar");
            project2.addLibraries(tempDir + "/lib");
            assertCodeCompletion(project1, file1, List.of("ClassA"), List.of("ClassB", "ClassC"));
            assertCodeCompletion(project2, file2, List.of("ClassA", "ClassB", "ClassC"));
            project1.closeProject();
            project2.closeProject();
        });
    }

    @Test
    void addLibsBetweenCodeCompletionCallsTest() {
        var file1 = tempDir + "/files/LibTest.java";
        var file2 = tempDir + "/files/LibTest2.java";
        Assertions.assertDoesNotThrow(() -> {
            var project1 = platform.openFile(file1);
            var project2 = platform.openFile(file2);

            project1.addLibraries(tempDir + "/lib/api.jar");
            project2.addLibraries(tempDir + "/lib/api.jar");
            assertCodeCompletion(project1, file1, List.of(), List.of("ClassA", "ClassB", "ClassC"));
            assertCodeCompletion(project2, file2, List.of(), List.of("ClassA", "ClassB", "ClassC"));

            project1.addLibraries(tempDir + "/lib/implA.jar");
            project2.addLibraries(tempDir + "/lib/sub");
            assertCodeCompletion(project1, file1, List.of("ClassA"), List.of("ClassB", "ClassC"));
            assertCodeCompletion(project2, file2, List.of("ClassB", "ClassC"), List.of("ClassA"));

            project1.addLibraries(tempDir + "/lib");
            project2.addLibraries(tempDir + "/lib");
            assertCodeCompletion(project1, file1, List.of("ClassA", "ClassB", "ClassC"));
            assertCodeCompletion(project2, file2, List.of("ClassA", "ClassB", "ClassC"));

            project1.closeProject();
            project2.closeProject();
        });
    }

}
