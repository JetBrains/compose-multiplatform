/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExperimentalPropertyAnnotationDetectorTest : AbstractLintDetectorTest(
    useDetector = ExperimentalPropertyAnnotationDetector(),
    useIssues = listOf(ExperimentalPropertyAnnotationDetector.ISSUE),
    stubs = arrayOf(kotlin("""
        package java.androidx

        @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
        @Retention(AnnotationRetention.BINARY)
        annotation class ExperimentalKotlinAnnotation
    """))
) {
    @Test
    fun `Test correctly annotated var properties`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @get:ExperimentalKotlinAnnotation
                    @set:ExperimentalKotlinAnnotation
                    @ExperimentalKotlinAnnotation
                    var correctlyAnnotatedWithDefault: Int = 3

                    @get:ExperimentalKotlinAnnotation
                    @set:ExperimentalKotlinAnnotation
                    @property:ExperimentalKotlinAnnotation
                    var correctlyAnnotatedWithDefault: Int = 3
                }
            """
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test var properties annotated with one target`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @get:ExperimentalKotlinAnnotation
                    var annotatedWithGet = 3

                    @set:ExperimentalKotlinAnnotation
                    var annotatedWithSet = 3

                    @property:ExperimentalKotlinAnnotation
                    var annotatedWithProperty = 3

                    @ExperimentalKotlinAnnotation
                    var annotatedWithDefault = 3
                }
            """
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:5: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @get:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:8: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @set:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:11: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @property:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:14: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 5: Add missing annotations:
@@ -5 +5
+                     @set:ExperimentalKotlinAnnotation
+                     @property:ExperimentalKotlinAnnotation
Fix for src/java/androidx/AnnotatedProperty.kt line 8: Add missing annotations:
@@ -8 +8
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
+                     @property:ExperimentalKotlinAnnotation
Fix for src/java/androidx/AnnotatedProperty.kt line 11: Add missing annotations:
@@ -11 +11
+                     @set:ExperimentalKotlinAnnotation
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
Fix for src/java/androidx/AnnotatedProperty.kt line 14: Add missing annotations:
@@ -14 +14
+                     @set:ExperimentalKotlinAnnotation
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test var property annotated with two targets`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @get:ExperimentalKotlinAnnotation
                    @ExperimentalKotlinAnnotation
                    var annotatedWithGetAndDefault = 3

                    @ExperimentalKotlinAnnotation
                    @set:ExperimentalKotlinAnnotation
                    var annotatedWithSetAndDefault = 3

                    @get:ExperimentalKotlinAnnotation
                    @set:ExperimentalKotlinAnnotation
                    var annotatedWithGetAndSet = 3

                    @property:ExperimentalKotlinAnnotation
                    @get:ExperimentalKotlinAnnotation
                    var annotatedWithGetAndProperty = 3

                    @set:ExperimentalKotlinAnnotation
                    @property:ExperimentalKotlinAnnotation
                    var annotatedWithSetAndProperty = 3
                }
            """
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:5: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @get:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:9: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:13: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @get:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:17: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @property:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:21: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @set:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 5: Add missing annotations:
@@ -5 +5
+                     @set:ExperimentalKotlinAnnotation
Fix for src/java/androidx/AnnotatedProperty.kt line 9: Add missing annotations:
@@ -9 +9
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
Fix for src/java/androidx/AnnotatedProperty.kt line 13: Add missing annotations:
@@ -13 +13
+                     @property:ExperimentalKotlinAnnotation
Fix for src/java/androidx/AnnotatedProperty.kt line 17: Add missing annotations:
@@ -17 +17
+                     @set:ExperimentalKotlinAnnotation
Fix for src/java/androidx/AnnotatedProperty.kt line 21: Add missing annotations:
@@ -21 +21
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test correctly annotated val property`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @get:ExperimentalKotlinAnnotation
                    @ExperimentalKotlinAnnotation
                    val correctlyAnnotatedWithDefault: Int = 3

                    @get:ExperimentalKotlinAnnotation
                    @property:ExperimentalKotlinAnnotation
                    val correctlyAnnotatedWithProperty: Int = 3
                }
            """
            )
        )

        check(*input)
            .expectClean()
    }

    @Test
    fun `Test val properties annotated with one target`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @get:ExperimentalKotlinAnnotation
                    val annotatedWithGet = 3

                    @property:ExperimentalKotlinAnnotation
                    val annotatedWithProperty = 3

                    @ExperimentalKotlinAnnotation
                    val annotatedWithDefault = 3
                }
            """
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:5: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @get:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:8: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @property:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:11: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 5: Add missing annotations:
@@ -5 +5
+                     @property:ExperimentalKotlinAnnotation
Fix for src/java/androidx/AnnotatedProperty.kt line 8: Add missing annotations:
@@ -8 +8
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
Fix for src/java/androidx/AnnotatedProperty.kt line 11: Add missing annotations:
@@ -11 +11
+                     @get:ExperimentalKotlinAnnotation
+                     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test property annotated with non-experimental annotation`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @NonExperimentalAnnotation
                    var correctlyAnnotated: Int = 3
                }
            """
            ),
            kotlin(
                """
                package java.androidx

                annotation class NonExperimentalAnnotation
            """
            )
        )

        check(*input)
            .expectClean()
    }

    @Test
    fun `Test property using Java defined annotation`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                import java.androidx.ExperimentalJavaAnnotation

                class AnnotatedProperty {
                    @get:ExperimentalJavaAnnotation
                    var annotatedWithGet = 3
                }
        """
            ),
            java("""
                package java.androidx;

                import static androidx.annotation.RequiresOptIn.Level.ERROR;

                import androidx.annotation.RequiresOptIn;

                @RequiresOptIn(level = ERROR)
                public @interface ExperimentalJavaAnnotation {}
            """.trimIndent()),
            kotlin(
                """
            package androidx.annotation

            import kotlin.annotation.Retention
            import kotlin.annotation.Target

            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.ANNOTATION_CLASS)
            annotation class RequiresOptIn(
                val level: Level = Level.ERROR
            ) {
                enum class Level {
                    WARNING,
                    ERROR
                }
            }
            """.trimIndent()
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:7: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @get:ExperimentalJavaAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 7: Add missing annotations:
@@ -7 +7
+                     @set:ExperimentalJavaAnnotation
+                     @property:ExperimentalJavaAnnotation
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test property defined at top-level`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                @get:ExperimentalKotlinAnnotation
                var annotatedWithGet = 3
            """
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/test.kt:4: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                @get:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/test.kt line 4: Add missing annotations:
@@ -4 +4
+                 @set:ExperimentalKotlinAnnotation
+                 @property:ExperimentalKotlinAnnotation
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test property defined in companion object`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    companion object {
                        @get:ExperimentalKotlinAnnotation
                        var annotatedWithGet = 3
                    }
                }
            """
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:6: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                        @get:ExperimentalKotlinAnnotation
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 6: Add missing annotations:
@@ -6 +6
+                         @set:ExperimentalKotlinAnnotation
+                         @property:ExperimentalKotlinAnnotation
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test property defined in interface`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                interface AnnotatedProperty {
                    @get:ExperimentalKotlinAnnotation
                    val annotatedWithGet: Int
                }
            """
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:5: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
                    @get:ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 5: Add missing annotations:
@@ -5 +5
+                     @property:ExperimentalKotlinAnnotation
        """
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                *input
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257294309
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test experimental annotations on non-properties don't trigger lint`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                @file:ExperimentalKotlinAnnotation

                @ExperimentalKotlinAnnotation
                class ExperimentalClass {
                    @ExperimentalKotlinAnnotation
                    fun experimentalFunction() {}
                }
            """
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test property annotated with JvmField doesn't trigger lint`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedWithJvmField {
                    @JvmField
                    @ExperimentalKotlinAnnotation
                    var experimentalProperty = 3
                }
            """
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test const property doesn't trigger lint`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                @ExperimentalKotlinAnnotation
                const val EXPERIMENTAL_CONST = 3
            """
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test property with delegate doesn't trigger lint`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                @ExperimentalKotlinAnnotation
                var experimentalProperty by mutableStateOf(0L)
                """
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test property within function doesn't trigger lint`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                fun functionWithProperty() {
                    @ExperimentalKotlinAnnotation
                    val experimentalProperty = 3
                }
                """.trimIndent()
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test private property doesn't trigger lint but other non-public properties do`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                class AnnotatedProperty {
                    @ExperimentalKotlinAnnotation
                    private var privateProperty = 3

                    @ExperimentalKotlinAnnotation
                    protected var protectedProperty = 3

                    @ExperimentalKotlinAnnotation
                    internal var internalProperty = 3
                }
                """.trimIndent()
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/AnnotatedProperty.kt:7: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
    @ExperimentalKotlinAnnotation
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/AnnotatedProperty.kt:10: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
    @ExperimentalKotlinAnnotation
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/AnnotatedProperty.kt line 7: Add missing annotations:
@@ -7 +7
+     @set:ExperimentalKotlinAnnotation
+     @get:ExperimentalKotlinAnnotation
+     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
Fix for src/java/androidx/AnnotatedProperty.kt line 10: Add missing annotations:
@@ -10 +10
+     @set:ExperimentalKotlinAnnotation
+     @get:ExperimentalKotlinAnnotation
+     @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        """
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test property in private class doesn't trigger lint`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                private class AnnotatedProperty {
                    @ExperimentalKotlinAnnotation
                    var experimentalProperty = 3
                }
                """.trimIndent()
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test property with private setter only has get annotation added`() {
        val input = arrayOf(
            kotlin(
                """
                package java.androidx

                @ExperimentalKotlinAnnotation
                var experimentalProperty = 3
                    private set
                """.trimIndent()
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/test.kt:3: Error: This property does not have all required annotations to correctly mark it as experimental. [ExperimentalPropertyAnnotation]
@ExperimentalKotlinAnnotation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/test.kt line 3: Add missing annotations:
@@ -3 +3
+ @get:ExperimentalKotlinAnnotation
+ @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        """
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }
}