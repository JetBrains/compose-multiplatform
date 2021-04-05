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
 * Test for [UnrememberedAnimatableDetector].
 */
class UnrememberedAnimatableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = UnrememberedAnimatableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnrememberedAnimatableDetector.UnrememberedAnimatable)

    // Simplified Animatable.kt stubs
    private val AnimatableStub = kotlin(
        """
            package androidx.compose.animation.core

            class Animatable<T, V>(
                initialValue: T,
                val typeConverter: V? = null
            )

            fun Animatable(initialValue: Float): Animatable<Float, Any> = Animatable(initialValue)
        """
    )

    // Simplified Animatable Color function stub, from androidx.compose.animation
    private val AnimatableColorStub = kotlin(
        """
            package androidx.compose.animation

            import androidx.compose.animation.core.Animatable
            import androidx.compose.ui.graphics.Color

            fun Animatable(initialValue: Color): Animatable<Color, Any> = Animatable(initialValue)
        """
    )

    @Test
    fun notRemembered() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.animation.*
                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.graphics.*

                @Composable
                fun Test() {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda = @Composable {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda2: @Composable () -> Unit = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    })
                    LambdaParameter {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }

                    val localLambda2: @Composable () -> Unit = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }
            """
            ),
            AnimatableStub,
            AnimatableColorStub,
            kotlin(Stubs.Color),
            kotlin(Stubs.Composable),
            kotlin(Stubs.Remember)
        )
            .run()
            .expect(
                """
src/test/{.kt:12: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable = Animatable<Boolean, Any>(false)
                                     ~~~~~~~~~~
src/test/{.kt:13: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable2 = Animatable(0f)
                                      ~~~~~~~~~~
src/test/{.kt:15: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable3 = Animatable(Color.Red)
                                      ~~~~~~~~~~
src/test/{.kt:20: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable = Animatable<Boolean, Any>(false)
                                     ~~~~~~~~~~
src/test/{.kt:21: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable2 = Animatable(0f)
                                      ~~~~~~~~~~
src/test/{.kt:23: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable3 = Animatable(Color.Red)
                                      ~~~~~~~~~~
src/test/{.kt:28: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable = Animatable<Boolean, Any>(false)
                                     ~~~~~~~~~~
src/test/{.kt:29: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable2 = Animatable(0f)
                                      ~~~~~~~~~~
src/test/{.kt:31: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable3 = Animatable(Color.Red)
                                      ~~~~~~~~~~
src/test/{.kt:41: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:42: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:44: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:48: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:49: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:51: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:58: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:59: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:61: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:66: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:67: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:69: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:77: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:78: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:80: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
24 errors, 0 warnings
            """
            )
    }

    @Test
    fun rememberedInsideComposableBody() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.animation.*
                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.graphics.*

                @Composable
                fun Test() {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = remember { Animatable(0f) }
                    val animatable2 = remember { Animatable() }
                    // Color function from androidx.compose.animation
                    val animatable3 = remember { Animatable(Color.Red) }
                }

                val lambda = @Composable {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = remember { Animatable(0f) }
                    val animatable2 = remember { Animatable() }
                    // Color function from androidx.compose.animation
                    val animatable3 = remember { Animatable(Color.Red) }
                }

                val lambda2: @Composable () -> Unit = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = remember { Animatable(0f) }
                    val animatable2 = remember { Animatable() }
                    // Color function from androidx.compose.animation
                    val animatable3 = remember { Animatable(Color.Red) }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable() }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    })
                    LambdaParameter {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable() }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable() }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable() }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    }
                }
            """
            ),
            AnimatableStub,
            AnimatableColorStub,
            kotlin(Stubs.Color),
            kotlin(Stubs.Composable),
            kotlin(Stubs.Remember)
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.animation.*
                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.graphics.*

                fun test() {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda2: () -> Unit = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                fun LambdaParameter(content: () -> Unit) {}

                fun test2() {
                    LambdaParameter(content = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    })
                    LambdaParameter {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }

                    val localLambda2: () -> Unit = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                fun test3() {
                    class Foo {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }

                    val localObject = object {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                @Composable
                fun Test4() {
                    class Foo {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }
            """
            ),
            AnimatableStub,
            AnimatableColorStub,
            kotlin(Stubs.Color),
            kotlin(Stubs.Composable),
            kotlin(Stubs.Remember)
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
