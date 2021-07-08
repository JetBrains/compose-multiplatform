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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [UnrememberedMutableStateDetector].
 */
class UnrememberedMutableStateDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = UnrememberedMutableStateDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnrememberedMutableStateDetector.UnrememberedMutableState)

    @Ignore // b/193270279
    @Test
    fun notRemembered() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                }

                val lambda = @Composable {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    })
                    LambdaParameter {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }
            """
            ),
            Stubs.Composable,
            Stubs.MutableState,
            Stubs.Remember
        )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:8: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = mutableStateOf(0)
                              ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:9: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val bar = mutableStateListOf<Int>()
                              ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:10: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val baz = mutableStateMapOf<Int, Float>()
                              ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:14: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = mutableStateOf(0)
                              ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:15: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val bar = mutableStateListOf<Int>()
                              ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:16: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val baz = mutableStateMapOf<Int, Float>()
                              ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:20: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = mutableStateOf(0)
                              ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:21: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val bar = mutableStateListOf<Int>()
                              ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:22: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val baz = mutableStateMapOf<Int, Float>()
                              ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:31: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:32: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:33: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:36: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:37: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:38: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:44: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:45: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:46: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:50: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:51: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:52: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:59: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:60: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:61: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
24 errors, 0 warnings
            """
            )
    }

    @Ignore // b/193270279
    @Test
    fun rememberedInsideComposableBody() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    val foo = remember { mutableStateOf(0) }
                    val bar = remember { mutableStateListOf<Int>() }
                    val baz = remember {
                        val test = "test"
                        mutableStateMapOf<Int, Float>()
                    }
                }

                val lambda = @Composable {
                    val foo = remember { mutableStateOf(0) }
                    val bar = remember { mutableStateListOf<Int>() }
                    val baz = remember {
                        val test = "test"
                        mutableStateMapOf<Int, Float>()
                    }
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = remember { mutableStateOf(0) }
                    val bar = remember { mutableStateListOf<Int>() }
                    val baz = remember {
                        val test = "test"
                        mutableStateMapOf<Int, Float>()
                    }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                    })
                    LambdaParameter {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                    }
                }
            """
            ),
            Stubs.Composable,
            Stubs.MutableState,
            Stubs.Remember
        )
            .run()
            .expectClean()
    }

    @Ignore // b/193270279
    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                fun test() {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                }

                val lambda = {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                }

                val lambda2: () -> Unit = {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                }

                fun LambdaParameter(content: () -> Unit) {}

                fun test2() {
                    LambdaParameter(content = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    })
                    LambdaParameter {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }

                    val localLambda2: () -> Unit = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }

                fun test3() {
                    class Foo {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }

                    val localObject = object {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }

                @Composable
                fun Test4() {
                    class Foo {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                    }
                }
            """
            ),
            Stubs.Composable,
            Stubs.MutableState,
            Stubs.Remember
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
