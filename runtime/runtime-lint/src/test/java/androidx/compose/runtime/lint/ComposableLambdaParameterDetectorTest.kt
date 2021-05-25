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

import androidx.compose.lint.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ComposableLambdaParameterDetector].
 */
class ComposableLambdaParameterDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableLambdaParameterDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(
        ComposableLambdaParameterDetector.ComposableLambdaParameterNaming,
        ComposableLambdaParameterDetector.ComposableLambdaParameterPosition
    )

    @Test
    fun incorrectNaming() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button(foo: Int, text: @Composable () -> Unit) {

                }
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:7: Warning: Composable lambda parameter should be named content [ComposableLambdaParameterNaming]
                fun Button(foo: Int, text: @Composable () -> Unit) {
                                     ~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/test.kt line 7: Rename text to content:
@@ -7 +7
-                 fun Button(foo: Int, text: @Composable () -> Unit) {
+                 fun Button(foo: Int, content: @Composable () -> Unit) {
            """
            )
    }

    @Test
    fun notTrailing() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button(content: @Composable () -> Unit, foo: Int) {

                }
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:7: Warning: Composable lambda parameter should be the last parameter so it can be used as a trailing lambda [ComposableLambdaParameterPosition]
                fun Button(content: @Composable () -> Unit, foo: Int) {
                           ~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun incorrectNamingAndNotTrailing() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button(text: @Composable () -> Unit, foo: Int) {

                }
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:7: Warning: Composable lambda parameter should be named content [ComposableLambdaParameterNaming]
                fun Button(text: @Composable () -> Unit, foo: Int) {
                           ~~~~
src/androidx/compose/ui/foo/test.kt:7: Warning: Composable lambda parameter should be the last parameter so it can be used as a trailing lambda [ComposableLambdaParameterPosition]
                fun Button(text: @Composable () -> Unit, foo: Int) {
                           ~~~~
0 errors, 2 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/test.kt line 7: Rename text to content:
@@ -7 +7
-                 fun Button(text: @Composable () -> Unit, foo: Int) {
+                 fun Button(content: @Composable () -> Unit, foo: Int) {
            """
            )
    }

    @Test
    fun lambdaParameterWithReceiver() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                class Foo

                @Composable
                fun Button(text: @Composable Foo.() -> Unit) {

                }
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/Foo.kt:9: Warning: Composable lambda parameter should be named content [ComposableLambdaParameterNaming]
                fun Button(text: @Composable Foo.() -> Unit) {
                           ~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/Foo.kt line 9: Rename text to content:
@@ -9 +9
-                 fun Button(text: @Composable Foo.() -> Unit) {
+                 fun Button(content: @Composable Foo.() -> Unit) {
            """
            )
    }

    @Test
    fun multipleParameters() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button(text: @Composable () -> Unit, icon: @Composable () -> Unit, foo: Int) {

                }
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expectClean()
    }

    @Test
    fun nullableComposableLambdas() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button(
                    text: @Composable (() -> Unit)?,
                    foo: Int
                ) {}

                @Composable
                fun Button2(
                    text: (@Composable () -> Unit)?,
                    foo: Int
                ) {}
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:8: Warning: Composable lambda parameter should be named content [ComposableLambdaParameterNaming]
                    text: @Composable (() -> Unit)?,
                    ~~~~
src/androidx/compose/ui/foo/test.kt:14: Warning: Composable lambda parameter should be named content [ComposableLambdaParameterNaming]
                    text: (@Composable () -> Unit)?,
                    ~~~~
src/androidx/compose/ui/foo/test.kt:8: Warning: Composable lambda parameter should be the last parameter so it can be used as a trailing lambda [ComposableLambdaParameterPosition]
                    text: @Composable (() -> Unit)?,
                    ~~~~
src/androidx/compose/ui/foo/test.kt:14: Warning: Composable lambda parameter should be the last parameter so it can be used as a trailing lambda [ComposableLambdaParameterPosition]
                    text: (@Composable () -> Unit)?,
                    ~~~~
0 errors, 4 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/test.kt line 8: Rename text to content:
@@ -8 +8
-                     text: @Composable (() -> Unit)?,
+                     content: @Composable (() -> Unit)?,
Fix for src/androidx/compose/ui/foo/test.kt line 14: Rename text to content:
@@ -14 +14
-                     text: (@Composable () -> Unit)?,
+                     content: (@Composable () -> Unit)?,
            """
            )
    }

    @Test
    fun receiverScopedComposableFunction() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                object FooScope

                @Composable
                fun FooScope.Button(foo: Int) {}
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expectClean()
    }

    @Test
    fun composableLambdaWithParameter() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                object FooScope

                @Composable
                fun Button(foo: @Composable (Int, Boolean) -> Unit) {}
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonComposableLambda() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable

                @Composable
                fun Button(text: () -> Unit, foo: Int) {

                }
            """
            ),
            kotlin(Stubs.Composable)
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
