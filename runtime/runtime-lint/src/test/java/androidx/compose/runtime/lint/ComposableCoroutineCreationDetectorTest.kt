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

package androidx.compose.runtime.lint

import androidx.compose.lint.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
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
 * Test for [ComposableCoroutineCreationDetector].
 */
class ComposableCoroutineCreationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableCoroutineCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableCoroutineCreationDetector.CoroutineCreationDuringComposition)

    private val coroutineBuildersStub: TestFile = kotlin(
        """
        package kotlinx.coroutines

        object CoroutineScope

        fun CoroutineScope.async(
            block: suspend CoroutineScope.() -> Unit
        ) {}

        fun CoroutineScope.launch(
            block: suspend CoroutineScope.() -> Unit
        ) {}
    """
    )

    @Test
    fun errors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.*

                @Composable
                fun Test() {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                }

                val lambda = @Composable {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                }

                val lambda2: @Composable () -> Unit = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    })
                    LambdaParameter {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    }

                    val localLambda2: @Composable () -> Unit = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    }
                }
            """
            ),
            kotlin(Stubs.Composable),
            coroutineBuildersStub
        )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:9: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:10: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:14: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:15: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:19: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:20: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:29: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:30: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:33: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:34: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:40: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:41: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:45: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:46: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
14 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.*

                fun test() {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                }

                val lambda = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                }

                val lambda2: () -> Unit = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    })
                    lambdaParameter {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    }

                    val localLambda2: () -> Unit = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                    }
                }
            """
            ),
            kotlin(Stubs.Composable),
            coroutineBuildersStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
