/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.animation.core.lint

import androidx.compose.lint.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

// TODO: add tests for methods defined in class files when we update Lint to support bytecode()
//  test files

/**
 * Test for [TransitionDetector].
 */
class TransitionDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TransitionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(TransitionDetector.UnusedTransitionTargetStateParameter)

    // Simplified Transition.kt stubs
    private val TransitionStub = kotlin(
        """
            package androidx.compose.animation.core

            import androidx.compose.runtime.Composable

            class Transition<S> {
                class Segment<S>
            }

            @Composable
            inline fun <S> Transition<S>.animateFloat(
                noinline transitionSpec: @Composable Transition.Segment<S>.() -> Unit = {},
                label: String = "FloatAnimation",
                targetValueByState: @Composable (state: S) -> Float
            ): Float = 5f
        """
    )

    @Test
    fun unreferencedParameters() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*

                val transition = Transition<Boolean>()

                var foo = false

                @Composable
                fun Test() {
                    transition.animateFloat { if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { if (foo) 1f else 0f })
                    transition.animateFloat { param -> if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { param -> if (foo) 1f else 0f })
                    transition.animateFloat { _ -> if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { _ -> if (foo) 1f else 0f })
                }
            """
            ),
            TransitionStub,
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { if (foo) 1f else 0f }
                                            ~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:14: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { if (foo) 1f else 0f })
                                                                 ~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:15: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { param -> if (foo) 1f else 0f }
                                              ~~~~~
src/foo/test.kt:16: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { param -> if (foo) 1f else 0f })
                                                                   ~~~~~
src/foo/test.kt:17: Error: Target state parameter _ is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { _ -> if (foo) 1f else 0f }
                                              ~
src/foo/test.kt:18: Error: Target state parameter _ is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { _ -> if (foo) 1f else 0f })
                                                                   ~
6 errors, 0 warnings
            """
            )
    }

    @Test
    fun unreferencedParameter_shadowedNames() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*

                val transition = Transition<Boolean>()

                var foo = false

                @Composable
                fun Test() {
                    transition.animateFloat {
                        foo.let {
                            // These `it`s refer to the `let`, not the `animateFloat`, so we 
                            // should still report an error
                            it.let {
                                if (it) 1f else 0f
                            }
                        }
                    }
                    transition.animateFloat { param ->
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `animateFloat`, so we 
                            // should still report an error
                            if (param) 1f else 0f
                        }
                    }
                }
            """
            ),
            TransitionStub,
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat {
                                            ^
src/foo/test.kt:22: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { param ->
                                              ~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
            package foo

            import androidx.compose.animation.core.*
            import androidx.compose.runtime.*

            val transition = Transition<Boolean>()

            var foo = false

            @Composable
            fun Test() {
                transition.animateFloat { if (it) 1f else 0f }
                transition.animateFloat(targetValueByState = { if (it) 1f else 0f })
                transition.animateFloat { param -> if (param) 1f else 0f }
                transition.animateFloat(targetValueByState = { param -> if (param) 1f else 0f })
                transition.animateFloat { param ->
                    foo.let {
                        it.let {
                            if (param && it) 1f else 0f
                        }
                    }
                }
                transition.animateFloat {
                    foo.let { param ->
                        param.let { param ->
                            if (param && it) 1f else 0f
                        }
                    }
                }

                transition.animateFloat {
                    foo.run {
                        run {
                            if (this && it) 1f else 0f
                        }
                    }
                }

                fun multipleParameterLambda(lambda: (Boolean, Boolean) -> Float): Float 
                    = lambda(true, true)

                transition.animateFloat {
                    multipleParameterLambda { _, _ ->
                        multipleParameterLambda { param1, _ ->
                            if (param1 && it) 1f else 0f
                        }
                    }
                }
            }
        """
            ),
            TransitionStub,
            kotlin(Stubs.Composable)
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
