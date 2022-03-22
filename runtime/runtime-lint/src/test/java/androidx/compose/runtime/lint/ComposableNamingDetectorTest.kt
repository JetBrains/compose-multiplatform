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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ComposableNamingDetector].
 */
class ComposableNamingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableNamingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableNamingDetector.ComposableNaming)

    @Test
    fun returnsUnit_lowerCaseName_fails() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun button() {}
            """
            ),
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:7: Warning: Composable functions that return Unit should start with an uppercase letter [ComposableNaming]
                fun button() {}
                    ~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/runtime/foo/test.kt line 7: Change to Button:
@@ -7 +7
-                 fun button() {}
+                 fun Button() {}
                """
            )
    }

    @Test
    fun returnsUnit_upperCaseName_passes() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button() {}
            """
            ),
            Stubs.Composable
        )
            .run()
            .expectClean()
    }

    @Test
    fun returnsValue_lowerCaseName_passes() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun getInt(): Int { return 5 }
            """
            ),
            Stubs.Composable
        )
            .run()
            .expectClean()
    }

    @Test
    fun returnsValue_upperCaseName_fails() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun GetInt(): Int { return 5 }
            """
            ),
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:7: Warning: Composable functions with a return type should start with a lowercase letter [ComposableNaming]
                fun GetInt(): Int { return 5 }
                    ~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/runtime/foo/test.kt line 7: Change to getInt:
@@ -7 +7
-                 fun GetInt(): Int { return 5 }
+                 fun getInt(): Int { return 5 }
                """
            )
    }

    @Test
    fun ignoreNonComposableFunctions() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                fun button() {}
                fun Button() {}
                fun GetInt(): Int { return 5 }
                fun getInt(): Int { return 5 }
            """
            )
        )
            .run()
            .expectClean()
    }

    @Test
    fun ignoreOperatorComposableFunctions() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable

                object Test {
                    @Composable
                    operator fun invoke() {}

                    @Composable
                    operator fun unaryPlus() {}
                }
            """
            ),
            Stubs.Composable
        )
            .run()
            .expectClean()
    }

    @Test
    fun ignoreOverriddenOperatorComposableFunctions() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable

                interface Test {
                    @Composable
                    operator fun invoke()

                    @Composable
                    operator fun unaryPlus()
                }

                object TestImpl : Test {
                    @Composable
                    override fun invoke() {}

                    @Composable
                    override fun unaryPlus() {}
                }
            """
            ),
            Stubs.Composable
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
