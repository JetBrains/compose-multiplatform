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
            test/StubKt.class:
            H4sIAAAAAAAAAJVUW08TQRT+ZnvZpRRYKigtCohVCl621NsDxMSYEBsrGqr4
            wNN0u+DQdtbsThsfiS/+Bp9M/Ae+oQ+G4Js/ynhmS7kjuknPmT3n+8535uxM
            f/3+/gPAPTxk6FdeqJyqateeKROMwd7kHe40udxwXtQ2PZeiMQZrvS1dJXzJ
            ECvMrjJknvitd37Ia01vaT91rVBp+KoppLPZaTk9Suj0EMUFTS2dh1rs5V9L
            oRYeRaTrFS7rgS/q7x03UvacoC2VaHnOQScL1ELFDzacTU/VAi6oKJfSV7wr
            sOyr5XazSSjT9aXypLKQZpg41I2gcCB50ylLFRBfuKGJQYZR963nNvYKvOQB
            b3kEZJgpVI7Pa+FQpKqLbNAG0rAxnMIQMkf1Ttm9iRGGpJAdv+ExjBRmTyqk
            cRGX+jGKMYap80bOMFaWBPBO+2JX8iK/nj87z8oM2RVPrAuvflp+IuL/BTDc
            a++5p3idK079GK1OjM4f0yZBGg29MCj+XuhVkVb1eYbPO1sjqZ2tlGEbkRsz
            uj8rnhu3d7ZyRpGVhm0jN5CJZ2hdjO1+SVLyqZmbtBNnZnc/Wj+3GdFn7KQG
            TSetnS3bHDsHnbQtjd79YJiphLX7qVRkus0So30g09vk4amzV3TO9MW601AM
            8Sd+nT7nUIUGvdxu1bzglR6U5voub67yQOj3vWBfVWxIrtoBrcdXuue8LDsi
            FJR+fHCkGfLHs/uH8wgsVfXbgestCV09u8dZPVEP8zAQh37iyCKBJO3Oobcs
            ug/7qg2KZHVK2yxMWATXsEWi6+jgXGZgGxfmviHL8EZzjIiTIp8kO4B+kgLS
            XTT5HPlShOvD3b3qqehPCjBpmETQXYzvSz0lqEHe7krFFk8Vs+jKDdHd02IX
            o3cLl3ElkrX/UdbOYoJoxn/JjpDs6BHZyWOyh+UM3I/sHTwgv0TRKRr+1TXE
            ypgu4xpZ5Mu4jhtlzKCwBhZiFnNr6AthhrgZIh3iVohUiPEQEyFuh0j8ATwh
            ftTnBQAA
            """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcbFUpJaXCLEFlxSmuQNpEOAPO8S
            JQYtBgBDd0xtMAAAAA==
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
