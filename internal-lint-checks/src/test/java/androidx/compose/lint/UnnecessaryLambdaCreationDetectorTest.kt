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

import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import androidx.compose.lint.UnnecessaryLambdaCreationDetector.Companion.ISSUE
import org.intellij.lang.annotations.Language

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class UnnecessaryLambdaCreationDetectorTest {

    private val composableStub = kt(
        """
        package androidx.compose.runtime

        @MustBeDocumented
        @Retention(AnnotationRetention.BINARY)
        @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
            AnnotationTarget.PROPERTY
        )
        annotation class Composable
    """
    )

    private val stub = kt(
        """
        package test

        import androidx.compose.runtime.Composable

        val lambda = @Composable { }
        val anonymousFunction = @Composable fun() {}
        val lambdaWithReceiver = @Composable { number: Int -> }
        val anonymousFunctionWithReceiver = @Composable fun(number: Int) {}
        fun function() {}

        @Composable
        fun ComposableFunction(content: @Composable () -> Unit) {
            content()
        }
    """
    ).to("test/stub.kt")

    private fun check(@Language("kotlin") code: String): TestLintResult {
        return TestLintTask.lint()
            .files(kt(code.trimIndent()), stub, composableStub)
            .allowMissingSdk(true)
            .issues(ISSUE)
            .run()
    }

    @Test
    fun warnsForSingleExpressions() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
                ComposableFunction {
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
src/test/test.kt:8: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:12: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        anonymousFunction()
        ~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
        """
        )
    }

    @Test
    fun warnsForMultipleLambdas() {
        check(
            """
            package test

            import androidx.compose.runtime.Composable

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
src/test/test.kt:13: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
    MultipleChildComposableFunction( { lambda() }) {
                                       ~~~~~~
src/test/test.kt:14: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
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
        ).expect(
            """
src/test/test.kt:23: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        parameterizedLambda(child)
        ~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """
        )
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
