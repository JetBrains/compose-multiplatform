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

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ComposableStateFlowValueDetector].
 */
class ComposableStateFlowValueDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableStateFlowValueDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableStateFlowValueDetector.StateFlowValueCalledInComposition)

    /**
     * Combined stub of StateFlow / supertypes
     */
    private val stateFlowStub: TestFile = compiledStub(
        filename = "StateFlow.kt",
        filepath = "kotlinx/coroutines/flow",
        checksum = 0x5f478927,
        """
        package kotlinx.coroutines.flow

        interface Flow<out T>

        interface SharedFlow<out T> : Flow<T>

        interface StateFlow<out T> : SharedFlow<T> {
            val value: T
        }
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdosSgxQAAhojekkAAAAA=
        """,
        """
        kotlinx/coroutines/flow/Flow.class:
        H4sIAAAAAAAAAH1QPU8CQRSct8jXiXr4iYkx2hkKD4mVGhMbEhKMiRAbqgUW
        snDsJtweUt7vsjBX+6OM76DTxC1m9s3Oy+x7X98fnwBucUI4m1kXarMKhnZh
        Y6eNioJxaN+DFkMRRLh66N11pnIpg1CaSfAymKqhu3/8KxH831oRW4RqZ5MR
        PCsnR9JJdor5MsdfoAzyBJqxtNJZ1eDb6IZwmSaeJ2rCYxZ+mpTGtTSpF0pp
        4tMFNUVDZMYm4bzz3wicRT3iJFS6TjqVadczRyh39cRIFy8UwevaeDFULR1y
        cfoaG6fn6k1HehCqJ2MsN2progInIo/NyeGQUTAfrfkAx+udEgrsKfaRa6PU
        RpkRXgbbbVSw0wdF2MUev0fwI1Qj7P8AoQI1/5ABAAA=
        """,
        """
        kotlinx/coroutines/flow/SharedFlow.class:
        H4sIAAAAAAAAAH1RTU/CQBB9U74rKuAXJMYYYzx4sEg8CSHxQiRiTGzjhdMC
        Cy6UNmm3yJHf5cFw9kcZp8TERAN7eG9mduZN3u7n1/sHgBtUCGcTX7vKm1t9
        P/AjrTwZWkPXf7PsVxHIQYvDDIjw0HBuO2MxE5YrvJH11BvLvq43/5c66wRj
        qYbj1Jt1QuHvWAZJwvGm0QzShOKPuvUotRgILVjLmM4S7IZiSBFowqW5irMq
        R4NrwsVyYZpG2TCZmbLD8nJxmc4uFwU6pVq2lCwZ91Q14u4a4Xytg98n4bXk
        EE42muWmvK2FlnFyNdGEnK1GntBRIAmm7UdBX7aUy0nlOfK0msoXFaqeK+88
        z+dB5XshmzaQAiGD+CRwyGgwH634AOXVTxKy3JXrItGG2cYWI/IxbLexg90u
        KEQBRb4PUQqxF2L/G+H7nf0GAgAA
        """,
        """
        kotlinx/coroutines/flow/StateFlow.class:
        H4sIAAAAAAAAAI1QTU/bQBB9s3YcJ6WpcWkbApXoxwE4YIp6qBqKxAU1EghB
        IoSU05Is6RJjS9l1ytG/pYf+iB4qi2N/VNUxpT00QuUyM29235uZ9+Pnt+8A
        3uI54cU4tbFOrqJBOkkzqxNlovM4/Rx1rbRqj6sqiHC03Xu/fyGnMoplMooO
        zy7UwLZ3Zlv7d+p9khM1LAW3e732TpsQ/EuuwiW8/L9AFR7BHyl7IuNMERZW
        12YXIVRW13gSYf52p+hAWTmUVnJPXE4dtoDKUCHQmFtXukSbXA3fEHaLvFEX
        TVEv8pskfMc/bxb5uucXeUArtOWHbig+0qY4DgOnJd4V+en1V/f6i+e1XN8N
        KqXQFuHV3Zb8sZgXoh7h9T3MK++a/j577i9/Y2wJta4eJdJmE36qd9NsMlB7
        OmaweJwlVl+qE230Wax2kyRlok4TwzYKVEColl6w+T5qjFqMBOpwbisHSzd5
        EcucP/CPB8ya68Pp4GEHDY54VIagg3mEfZDBYyz04Rk8MXhq8MygaeAb1H4B
        5Dh0XXwCAAA=
        """
    )

    @Test
    fun errors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.flow.*

                val stateFlow: StateFlow<Boolean> = object : StateFlow<Boolean> {
                    override val value = true
                }

                class TestFlow : StateFlow<Boolean> {
                    override val value = true
                }

                val testFlow = TestFlow()

                @Composable
                fun Test() {
                    stateFlow.value
                    testFlow.value
                }

                val lambda = @Composable {
                    stateFlow.value
                    testFlow.value
                }

                val lambda2: @Composable () -> Unit = {
                    stateFlow.value
                    testFlow.value
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        stateFlow.value
                        testFlow.value
                    })
                    LambdaParameter {
                        stateFlow.value
                        testFlow.value
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        stateFlow.value
                        testFlow.value
                    }

                    val localLambda2: @Composable () -> Unit = {
                        stateFlow.value
                        testFlow.value
                    }
                }
            """
            ),
            Stubs.Composable,
            stateFlowStub
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/androidx/compose/runtime/foo/TestFlow.kt:19: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    stateFlow.value
                              ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:20: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    testFlow.value
                             ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:24: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    stateFlow.value
                              ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:25: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    testFlow.value
                             ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:29: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    stateFlow.value
                              ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:30: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    testFlow.value
                             ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:39: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:40: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:43: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:44: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:50: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:51: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:55: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:56: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
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
                import kotlinx.coroutines.flow.*

                val stateFlow: StateFlow<Boolean> = object : StateFlow<Boolean> {
                    override val value = true
                }

                class TestFlow : StateFlow<Boolean> {
                    override val value = true
                }

                val testFlow = TestFlow()

                fun test() {
                    stateFlow.value
                    testFlow.value
                }

                val lambda = {
                    stateFlow.value
                    testFlow.value
                }

                val lambda2: () -> Unit = {
                    stateFlow.value
                    testFlow.value
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        stateFlow.value
                        testFlow.value
                    })
                    lambdaParameter {
                        stateFlow.value
                        testFlow.value
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        stateFlow.value
                        testFlow.value
                    }

                    val localLambda2: () -> Unit = {
                        stateFlow.value
                        testFlow.value
                    }
                }
            """
            ),
            Stubs.Composable,
            stateFlowStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
