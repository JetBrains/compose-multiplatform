/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.kotlinAndCompiledStub
import com.android.tools.lint.checks.infrastructure.TestLintResult
import org.junit.Test
import org.junit.runner.RunWith
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.intellij.lang.annotations.Language
import org.junit.runners.Parameterized

/* ktlint-disable max-line-length */
@RunWith(Parameterized::class)
class UnnecessaryLambdaCreationDetectorTest(
    @Suppress("unused")
    private val parameterizedDebugString: String,
    private val stub: TestFile
) : LintDetectorTest() {
    companion object {
        private val stub = kotlinAndCompiledStub(
            filename = "Stub.kt",
            filepath = "test",
            checksum = 0xdbff73f0,
            source = """
                package test

                import androidx.compose.runtime.Composable

                fun function() {}

                @Composable
                fun ComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                @Composable
                inline fun InlineComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                @Composable
                inline fun <reified T> ReifiedComposableFunction(content: @Composable () -> Unit) {
                    content()
                }
            """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcbFUpJaXCLEFlxSmuQNpEOAPO8S
            JQYtBgBDd0xtMAAAAA==
            """,
            """
            test/StubKt.class:
            H4sIAAAAAAAAAJ1UW08TQRT+ZnvZpRRZKigtCqhVCl621GuEmBgTQmNFI4gP
            PE23Cw5tZ83utOGR+OJv8MnEf+Cb+mAIvvmjjGe2FLlUUZvsnJlzvu9858yl
            3398+QrgFu4x9CsvVM6yalUfKxOMwd7kbe40uNxwnlY3PZe8MQZrvSVdJXzJ
            ECtMrzJkHvnN137Iqw1vYT90qVCp+6ohpLPZbjpdSuh0EcU5TS2dhJrvxl9I
            oeYeRKTLFS5rgS9qW44bKXtO0JJKND3nVyVzVELFDzacTU9VAy4oKZfSV7wj
            sOSrpVajQSjT9aXypLKQZhg/UI0gdyB5wylLFRBfuKGJUwwj7ivPre8leMYD
            3vQIyDBVqBzdr7kDnmWdZIMaSMPGUAqDyBzW69G9iWGGpJBtv+4xDBemjyuk
            cQZn+zGCUYbJk7acYbQsCeD1OrHzeZFfz/8+zsoM2eeeWBderVd8cX7l/vH6
            HvzPCY9HpfxBa6hLeuIpXuOKU2tGsx2jq8z0kKBy63pikH9L6FmRZrVZhvc7
            28Opne2UYRuRGTU6nxXPjdk72zmjyEpDtpEbyMQzNC/Gdj8kKbho5ibsxG+j
            u2+tb58Y0afspAZdTFo727Y5egI6aVsavfvGMFMJa/ddqch0mSVGfSDTbfLg
            AbIVurL6jd6oK4b4I79GN2OwQme21GpWvWBFb5Tm+i5vrPJA6PWes29ZbEiu
            WgHNx553nkxZtkUoKPzw1+tgyB+N7t/zQ7DUst8KXG9B6OzZPc7qsXyYhYE4
            9C+OLBJIUndFWmXR+bGPeiAYopAeszBhEVzD5omuvadmMgOfcHrmM7IMLzXH
            iDgpskkaB9CPEq3THTTZHNmbEa6P/uM62VNkb9Nn0mYSQVcxti+1SFCDrN2R
            is33FLPo9Q7SM9ZiZ6K1hXM4H8nafylrZzFOtNg/yQ6T7Mgh2cmeshOHZA3c
            iUYHd8kukPcCHcLFNcTKuFRGnkZcLuMKpsooYHoNLMQMrq6hL4QZ4lqIdIjr
            IVIhxkKMh7gRIvETuPI3KzoGAAA=
            """
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Array<Any> = arrayOf(
            arrayOf("Source stubs", stub.kotlin),
            arrayOf("Compiled stubs", stub.compiled)
        )
    }

    override fun getDetector(): Detector = UnnecessaryLambdaCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnnecessaryLambdaCreationDetector.ISSUE)

    private fun check(@Language("kotlin") code: String): TestLintResult {
        return lint()
            .files(kotlin(code.trimIndent()), stub, Stubs.Composable)
            .run()
    }

    @Test
    fun warnsForSingleExpressions() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }
            val anonymousFunction = @Composable fun() {}
            val lambdaWithReceiver = @Composable { number: Int -> }
            val anonymousFunctionWithReceiver = @Composable fun(number: Int) {}

            @Composable
            fun Test() {
                ComposableFunction {
                    lambda()
                }

                InlineComposableFunction {
                    lambda()
                }

                ReifiedComposableFunction<Any> {
                    lambda()
                }

                ComposableFunction {
                    anonymousFunction()
                }

                ComposableFunction {
                    lambdaWithReceiver(10)
                }

                ComposableFunction {
                    anonymousFunctionWithReceiver(10)
                }

                ComposableFunction {
                    function()
                }
            }
        """
        ).expect(
            """
src/test/test.kt:13: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:17: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:21: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:25: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        anonymousFunction()
        ~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """
        )
    }

    @Test
    fun warnsForMultipleLambdas() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            @Composable
            fun MultipleChildComposableFunction(
                firstChild: @Composable () -> Unit,
                secondChild: @Composable () -> Unit
            ) {}

            @Composable
            fun Test() {
                MultipleChildComposableFunction( { lambda() }) {
                    lambda()
                }
            }
        """
        ).expect(
            """
src/test/test.kt:15: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
    MultipleChildComposableFunction( { lambda() }) {
                                       ~~~~~~
src/test/test.kt:16: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
2 errors, 0 warnings
        """
        )
    }

    @Test
    fun ignoresMultipleExpressions() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            @Composable
            fun Test() {
                ComposableFunction {
                    lambda()
                    lambda()
                }
            }
        """
        ).expectClean()
    }

    @Test
    fun ignoresPropertyAssignment() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            val property: @Composable () -> Unit = {
                lambda()
            }
        """
        ).expectClean()
    }

    @Test
    fun ignoresDifferentFunctionalTypes_parameters() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            @Composable
            fun ComposableFunctionWithParams(
                child: @Composable (child: @Composable () -> Unit) -> Unit
            ) {}

            @Composable
            fun Test() {
                ComposableFunctionWithParams { child ->
                    lambda()
                }
            }

            val parameterizedLambda: (@Composable () -> Unit) -> Unit = { it() }
            val differentlyParameterizedLambda: (Int) -> Unit = { }

            @Composable
            fun Test1() {
                ComposableFunctionWithParams { child ->
                    parameterizedLambda(child)
                }
            }

            @Composable
            fun Test2() {
                ComposableFunctionWithParams { child ->
                    differentlyParameterizedLambda(5)
                }
            }
        """
        ).expectClean()
    }

    @Test
    fun ignoresDifferentFunctionalTypes_receiverScopes() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            class SomeScope
            class OtherScope

            @Composable
            fun ScopedComposableFunction(content: @Composable SomeScope.() -> Unit) {
                SomeScope().content()
            }

            @Composable
            fun Test() {
                val unscopedLambda: () -> Unit = {}
                val scopedLambda: @Composable SomeScope.() -> Unit = {}
                val differentlyScopedLambda: @Composable OtherScope.() -> Unit = {}

                ScopedComposableFunction {
                    unscopedLambda()
                }

                ScopedComposableFunction {
                    scopedLambda()
                }

                ScopedComposableFunction {
                    OtherScope().differentlyScopedLambda()
                }
            }
        """
        ).expect(
            """
src/test/SomeScope.kt:24: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        scopedLambda()
        ~~~~~~~~~~~~
1 errors, 0 warnings
        """
        )
    }

    @Test
    fun ignoresMismatchedComposability() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            fun uncomposableLambdaFunction(child: () -> Unit) {}

            val lambda = @Composable { }
            val uncomposableLambda = {}

            @Composable
            fun Test() {
                uncomposableLambdaFunction {
                    lambda()
                }

                ComposableFunction {
                    uncomposableLambda()
                }
            }
        """
        ).expectClean()
    }
}
/* ktlint-enable max-line-length */
