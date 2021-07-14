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
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ComposableFlowOperatorDetector].
 */
class ComposableFlowOperatorDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableFlowOperatorDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableFlowOperatorDetector.FlowOperatorInvokedInComposition)

    /**
     * Combined stub of some Flow APIs
     */
    private val flowStub: TestFile = compiledStub(
        filename = "Flow.kt",
        filepath = "kotlinx/coroutines/flow",
        """
        package kotlinx.coroutines.flow

        interface Flow<out T>

        inline fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> {
            return object : Flow<R> {}
        }

        fun <T> Flow<T>.drop(count: Int): Flow<T> = this
        """,
"""
        kotlinx/coroutines/flow/Flow.class:
        H4sIAAAAAAAAAH1QPU8CQRSctwgH59fhJybGaGcsPCRWakxsSEgwJkJsqBZY
        yMKxm3B7SHm/y8Jc7Y8yvoNOE7eY2ZmdZPa9r++PTwC3OCacTq2LtFmGAzu3
        idNGxeEosu9hk8EDES4funftiVzIMJJmHL70J2rg7h//WoTgt+dhg1BtrzvC
        Z+XkUDrJSTFbFPgLlEORQFO2ljpXdb4NbwgXWer7oiZ8ZhFkaXlUy9KrUjlL
        AzqnhqiLPNggnLX/G4G7qEvcBC+X11NHqHT02EiXzBXB79hkPlBNHbE4eU2M
        0zP1pmPdj9STMdZJp62JS1yGItangANGwXy44n0crdZJKHHG66HQQrmFCiP8
        HDZb2MJ2DxRjB7v8HiOIUY2x9wNCSdkgiwEAAA==
        """,
        """
        kotlinx/coroutines/flow/FlowKt＄map＄1.class:
        H4sIAAAAAAAAAI1SXW8SQRQ9s1A+RpQWq4LWihYrReN2iSZGmiZNhYSUqikN
        LyQmA2zpwDJr2Fnkkb/kk4kPhmd/lPHuFl582LibnPsx5+49c+/+/vPzF4A3
        KDOUxq52pJqbfXfq+loq2zOvHPeb2SA406WJ+FqykmAMb1sjMROmI9TQ/NQb
        2X1da0UVH11e1I5rDJv/liURZ9iJKk0iwZA4kkrqY4ZY+aCTQQppjg1whri+
        lh7DfmT3tXQSsLUimue2FgOhBeWMySxGI2ABbDCwMaXmMogOyRtYDLvLRZov
        F9zIGxW2XKR4frmopnLxnPFuuThkAatKrEgR1Gg3WmQSBbogKWX4Uo7+1voW
        o9nEvPJVX0tXeWZj5VVrB9HlGTzCDkMyCF6PNY3x1B3YDNkWET/6k549vRQ9
        hzK5ltsXTkdMZRCvkpmmUvb01BGeZ9Pws3XVd1xPqiEN9dodMKTbcqiE9qdE
        5m3Xn/bthgwqt2+CD3bPH9bn2lYeyWUoXPhKy4ndkZ6kDidKuVqEd4IFgzZN
        WoMN0UurJ3xCkRnGtLLKD9z6To6BImEiTCbxlDBzQyB7Ozx5RsgpZyB4Ctgj
        jOMhHqMU1sfwPLS72Ccr2ucnn/lqRvwsHCivtItrr8FfFq3i+vx/fkBeaXFr
        z3plWe/Jr3O8oDYWCbpDIrNdxJrYbGKLELkA7jaxjXtdMA/38aALwwvc/F8g
        yNK6tAMAAA==
        """,
        """
        kotlinx/coroutines/flow/FlowKt.class:
        H4sIAAAAAAAAAI1TbW/bVBR+ruM4jpu2jteVJhulbIalLZ3TbrwtWcaoVDWi
        FNRGBVQJcZO6nVvHRr5O2MeID/wQfgHfmEBCUfmC+Mr/QRw7TsjWkmDJ97zc
        55zznHvP/fPvX34D8BAfMyxf+KHreM+tlh/4ndDxbGGduv531g4tn4QZMAb9
        nHe55XLvzPqseW63yJtiSLX5twxfl/YmZagku9Z5t22ddrxW6PiesHYSbauy
        Ojmc4a9q49HeqwQqB9f4apOpVNcbjUptGqHqBqE2hqCxPNu+R0qHRygCHVCq
        9Ws4TGmoGgcy3N3zgzPr3A6bAXeoPvc8P+QDLvt+uN9xXUIp1fCZI2oqsqOL
        ink7XmgHHnetuhcGFO60RAYzDDdbz+zWRRL/OQ942yYgw73SVaZjnsMoyVll
        9SiHWcxpyGGeIRsG3BOnftBWkWcwJ8+JScNgbmZwIyLteE5YowEpRRlvYlHD
        Al5juD0pRQYFBtV0zFMznitWJwpm1P7AXp42JyvTJo1BPgl8SvVkysjWp87k
        N9fO5P+bvynZqzGIQRv0PmCcbvkdL2TID3v81A75CQ85AaV2N0WPmUVLms7t
        IlIk8j93Iq1M2skmwx/9Xknr9zRpSdIklX5dJZka2ENfJHUC9XtFk2RRNWRD
        2pXK7I6s9nu6tDZybCl6qiiV5WJTTycwZQQjwXRlDPxAlfVMcUNlxg0jvytd
        /qjk1Kyhqpohq2ppxtCMYYqcoRjyEivPltXdyx/U31+wfo/gkj53+b0kE8tC
        1M8W9dlg1DTYAYMxPJXxu14cOkfPYJ9ENAQeSTrSLnc79tjY/MdTp4hMdC/3
        L+j85W3/hGLm9wi232k37aDBm64dUfBb3D3igRPZiTN76Jx5POwEpN86oAt0
        2nbd6zrCoe2n/z54elyv7o5IvwTL1T3PDrZdLoRNpnbod4KWveNExQpJiqMr
        6bEJCTKiT0IBaShkfUjWF2RHI1JYM7QX0NcNg9bU41+x8NXPWOqj+FMc8ohW
        hY56DhlUSF+hoDnM4BZu0y6F43Usx+kLyOMNQlbjuAweJ5EqyRr9s1JiDNYC
        pXoz4VKlgCiZOuCy9nJpBVpcenGAwd2koAqTSAwL3rlSMDssCOL8hFaN7IWE
        60dx0Ad4SvJL8r9FR/P2MVJ13KujRCtW61jDeh3vYOMYTOA+rGPMC6wILAuU
        BUw6XIG0wJZAXuCBwEOBdwXeE3g/3lL+AdkMm1BrBwAA
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdwqXIJZ6dX5KTmQdSVpRfWpKZl1qsl5aTXy7E
        5gYkvUuUGLQYABYJSb1jAAAA
        """
    )

    @Ignore // b/193270279
    @Test
    fun errors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.flow.*

                val emptyFlow: Flow<Unit> = object : Flow<Unit> {}

                fun <T> Flow<T>.customOperator(param: Boolean): Flow<T> = this

                @Composable
                fun Test() {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda = @Composable {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda2: @Composable () -> Unit = {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    })
                    LambdaParameter {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }

                    val localLambda2: @Composable () -> Unit = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }
            """
            ),
            Stubs.Composable,
            flowStub
        )
            .run()
            .expect(
                """
                    src/androidx/compose/runtime/foo/test.kt:14: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .map { true }
                         ~~~
src/androidx/compose/runtime/foo/test.kt:15: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .customOperator(true)
                         ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .drop(0)
                         ~~~~
src/androidx/compose/runtime/foo/test.kt:21: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .map { true }
                         ~~~
src/androidx/compose/runtime/foo/test.kt:22: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .customOperator(true)
                         ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:23: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .drop(0)
                         ~~~~
src/androidx/compose/runtime/foo/test.kt:28: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .map { true }
                         ~~~
src/androidx/compose/runtime/foo/test.kt:29: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .customOperator(true)
                         ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:30: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .drop(0)
                         ~~~~
src/androidx/compose/runtime/foo/test.kt:40: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:41: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:42: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
src/androidx/compose/runtime/foo/test.kt:46: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:47: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:48: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
src/androidx/compose/runtime/foo/test.kt:55: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:56: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:57: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
src/androidx/compose/runtime/foo/test.kt:62: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:63: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:64: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
21 errors, 0 warnings
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

                val emptyFlow: Flow<Unit> = object : Flow<Unit> {}

                fun <T> Flow<T>.customOperator(param: Boolean): Flow<T> = this

                fun test() {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda = {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda2: () -> Unit = {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    })
                    lambdaParameter {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }

                    val localLambda2: () -> Unit = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }
            """
            ),
            Stubs.Composable,
            flowStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
