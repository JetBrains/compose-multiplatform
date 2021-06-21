/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */

/**
 * Test for [ModifierParameterDetector].
 */
@RunWith(JUnit4::class)
class ModifierParameterDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierParameterDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            ModifierParameterDetector.ModifierParameter
        )

    @Test
    fun modifierParameterNaming() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.runtime.Composable

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    buttonModifier: Modifier = Modifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:10: Warning: Modifier parameter should be named modifier [ModifierParameter]
                    buttonModifier: Modifier = Modifier,
                    ~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/test.kt line 10: Change name to modifier:
@@ -10 +10
-                     buttonModifier: Modifier = Modifier,
+                     modifier: Modifier = Modifier,
            """
            )
    }

    @Test
    fun modifierParameterType() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.runtime.Composable

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    modifier: Modifier.Element,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:10: Warning: Modifier parameter should have a type of Modifier [ModifierParameter]
                    modifier: Modifier.Element,
                    ~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/test.kt line 10: Change type to Modifier:
@@ -10 +10
-                     modifier: Modifier.Element,
+                     modifier: Modifier,
            """
            )
    }

    @Test
    fun modifierParameterDefaultValue() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.runtime.Composable

                object TestModifier : Modifier.Element

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    modifier: Modifier = TestModifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:12: Warning: Optional Modifier parameter should have a default value of Modifier [ModifierParameter]
                    modifier: Modifier = TestModifier,
                    ~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 12: Change default value to Modifier:
@@ -12 +12
-                     modifier: Modifier = TestModifier,
+                     modifier: Modifier = Modifier,
            """
            )
    }

    @Test
    fun modifierParameterOrdering() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.runtime.Composable

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    modifier: Modifier = Modifier,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:11: Warning: Modifier parameter should be the first optional parameter [ModifierParameter]
                    modifier: Modifier = Modifier,
                    ~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun multipleErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.runtime.Composable

                object TestModifier : Modifier.Element

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    buttonModifier: Modifier.Element = TestModifier,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:13: Warning: Modifier parameter should be named modifier [ModifierParameter]
                    buttonModifier: Modifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:13: Warning: Modifier parameter should be the first optional parameter [ModifierParameter]
                    buttonModifier: Modifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:13: Warning: Modifier parameter should have a type of Modifier [ModifierParameter]
                    buttonModifier: Modifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:13: Warning: Optional Modifier parameter should have a default value of Modifier [ModifierParameter]
                    buttonModifier: Modifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 13: Change name to modifier:
@@ -13 +13
-                     buttonModifier: Modifier.Element = TestModifier,
+                     modifier: Modifier.Element = TestModifier,
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 13: Change type to Modifier:
@@ -13 +13
-                     buttonModifier: Modifier.Element = TestModifier,
+                     buttonModifier: Modifier = TestModifier,
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 13: Change default value to Modifier:
@@ -13 +13
-                     buttonModifier: Modifier.Element = TestModifier,
+                     buttonModifier: Modifier.Element = Modifier,
            """
            )
    }

    @Test
    fun ignoreNonComposableFunctions() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    buttonModifier: TestModifier = TestModifier,
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expectClean()
    }

    /**
     * At some point we may want to have a lint rule / API lint rule to enforce that all required
     * parameters come before optional ones, but that is out of the scope of this lint check.
     */
    @Test
    fun ignoreOrderingIfNoDefaultValue() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    modifier: Modifier,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.runtime.Composable

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    modifier: Modifier = Modifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}

                @Composable
                fun Button2(
                    onClick: () -> Unit,
                    modifier: Modifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
            ),
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
