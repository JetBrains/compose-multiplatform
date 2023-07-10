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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MissingJvmDefaultWithCompatibilityDetectorTest : AbstractLintDetectorTest(
    useDetector = MissingJvmDefaultWithCompatibilityDetector(),
    useIssues = listOf(MissingJvmDefaultWithCompatibilityDetector.ISSUE),

) {
    @Test
    fun `Test lint for interface with stable default method`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                interface InterfaceWithDefaultMethod {
                    fun methodWithoutDefaultImplementation(foo: Int): String
                    fun methodWithDefaultImplementation(): Int = 3
                }
            """)
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/InterfaceWithDefaultMethod.kt:4: Error: This interface must be annotated with @JvmDefaultWithCompatibility because it has a stable method with a default implementation [MissingJvmDefaultWithCompatibility]
                interface InterfaceWithDefaultMethod {
                ^
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Autofix for src/java/androidx/InterfaceWithDefaultMethod.kt line 4: Annotate with @JvmDefaultWithCompatibility:
@@ -4 +4
+                 @JvmDefaultWithCompatibility
        """
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test lint for interface with stable method with default parameter`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                interface InterfaceWithMethodWithDefaultParameterValue {
                    fun methodWithDefaultParameterValue(foo: Int = 3): Int
                }
            """)
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/InterfaceWithMethodWithDefaultParameterValue.kt:4: Error: This interface must be annotated with @JvmDefaultWithCompatibility because it has a stable method with a parameter with a default value [MissingJvmDefaultWithCompatibility]
                interface InterfaceWithMethodWithDefaultParameterValue {
                ^
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Autofix for src/java/androidx/InterfaceWithMethodWithDefaultParameterValue.kt line 4: Annotate with @JvmDefaultWithCompatibility:
@@ -4 +4
+                 @JvmDefaultWithCompatibility
        """
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test lint for interface implementing @JvmDefaultWithCompatibility interface`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                import kotlin.jvm.JvmDefaultWithCompatibility

                @JvmDefaultWithCompatibility
                interface InterfaceWithAnnotation {
                    fun foo(bar: Int = 3): Int
                }
            """),
            kotlin("""
                package java.androidx

                interface InterfaceWithoutAnnotation: InterfaceWithAnnotation {
                    fun baz(): Int
                }
            """),
            Stubs.JvmDefaultWithCompatibility
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/InterfaceWithoutAnnotation.kt:4: Error: This interface must be annotated with @JvmDefaultWithCompatibility because it implements an interface which uses this annotation [MissingJvmDefaultWithCompatibility]
                interface InterfaceWithoutAnnotation: InterfaceWithAnnotation {
                ^
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Autofix for src/java/androidx/InterfaceWithoutAnnotation.kt line 4: Annotate with @JvmDefaultWithCompatibility:
@@ -4 +4
+                 @JvmDefaultWithCompatibility
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test lint does not apply to interface implementing @JvmDefaultWithCompatibility`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                import kotlin.jvm.JvmDefaultWithCompatibility

                @JvmDefaultWithCompatibility
                interface InterfaceWithAnnotation {
                    fun foo(bar: Int = 3): Int = 4
                }
            """),
            Stubs.JvmDefaultWithCompatibility
        )

        check(*input)
            .expectClean()
    }

    @Test
    fun `Test lint does not apply to unstable interface`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                @RequiresOptIn
                interface UnstableInterface {
                    fun foo(bar: Int = 3): Int = 4
                }
            """),
            Stubs.OptIn
        )

        check(*input)
            .expectClean()
    }

    @Test
    fun `Test lint does not apply to interface with no stable methods`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                interface InterfaceWithoutStableMethods {
                    @RequiresOptIn
                    fun unstableMethod(foo: Int = 3): Int = 4
                }
            """),
            Stubs.OptIn
        )

        check(*input)
            .expectClean()
    }

    @Test
    fun `Test lint does apply to interface with one unstable method and one stable method`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                interface InterfaceWithStableAndUnstableMethods {
                    @RequiresOptIn
                    fun unstableMethod(foo: Int = 3): Int = 4
                    fun stableMethod(foo: Int = 3): Int = 4
                }
            """),
            Stubs.OptIn
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/InterfaceWithStableAndUnstableMethods.kt:4: Error: This interface must be annotated with @JvmDefaultWithCompatibility because it has a stable method with a default implementation [MissingJvmDefaultWithCompatibility]
                interface InterfaceWithStableAndUnstableMethods {
                ^
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Autofix for src/java/androidx/InterfaceWithStableAndUnstableMethods.kt line 4: Annotate with @JvmDefaultWithCompatibility:
@@ -4 +4
+                 @JvmDefaultWithCompatibility
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test lint does not apply to interface with no default methods`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                interface InterfaceWithoutDefaults {
                    fun methodWithoutDefaults(foo: Int): Int
                }
            """)
        )

        check(*input)
            .expectClean()
    }

    @Test
    fun `Test lint does not apply to Java file`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                interface JavaInterface {
                    static int staticMethodInInterface() {
                        return 3;
                    }
                }
            """)
        )

        check(*input)
            .expectClean()
    }
}