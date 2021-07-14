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
 * Test for [ComposableCoroutineCreationDetector].
 */
class ComposableCoroutineCreationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableCoroutineCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableCoroutineCreationDetector.CoroutineCreationDuringComposition)

    private val coroutineBuildersStub: TestFile = compiledStub(
        filename = "Builders.common.kt",
        filepath = "kotlinx/coroutines",
        """
        package kotlinx.coroutines

        object CoroutineScope

        fun CoroutineScope.async(
            block: suspend CoroutineScope.() -> Unit
        ) {}

        fun CoroutineScope.launch(
            block: suspend CoroutineScope.() -> Unit
        ) {}
        """,
"""
        kotlinx/coroutines/Builders_commonKt.class:
        H4sIAAAAAAAAAK1TXU8TQRQ9M/1aliJlBWyrYpUqX8IW4lsJCRJJGhGNRV54
        MNPtWrbdzpr9aOCN+FP8BfJGfDAE3/xRxrvbLoKagNF9uHvu3XPPnJm5++37
        5y8AnkBnKHcc37bkgW44rhP4ljQ9/Wlg2U3T9d4aTrfryOd+Bowh1xY9odtC
        tvSXjbZpUDXBkBLeoTQYNme3/qC0EcO64bw3qwOK3u519XeBNHzLkZ6+OUAr
        1bldhk//QWh18RoaMecyRRIIRKhyLqK/kZZfXasubP16AlQMLU9vOW5Lb5t+
        wxUW+RBSOr7oe9p2/O3AtqsM6VV/3/LWFAwxTF3wb0nfdKWw9Zr0XWq3DC+D
        YYYJY980OoP+V8IVXZOIDDOzv/u4UKmHIi3ylcUIbqjIYpRuqWE7RkfBGMNw
        ObRRHlzb9DUOiqF01cXR7mxB2T5Dtq8fp2Nx6wvTF03hC+Lybi9B88fCkGJg
        nRBwqh9YIaoQai4zHJ8eldTTI5XnuMrzPIL5PuS5OFF4sUpJkVfYPK/wlZlc
        ojitMC2pUaapmhIhVklpaS2ZZ5V0JXn2Mc2VzNcTdnoUwpxCCkP/JHD2gSfJ
        SiE0vsJoZ9DibV88o9IVE0eUqZjy7MA3aRYcGQvsHEZXocU/51L/51zq+AzJ
        DadpMoxukeR20G2Y7o5o2GZowzGEvStcK8wHxaG61ZLCD1zC5dcBrd81a7Jn
        eRZ9Ph+09Z9DzKDWncA1zE0r7C8Menb7HReIWAZHEuHDUUAKaSQwS9k65Zze
        I/OaeoLcgqZRPI5ocxTTdGJZKJgnPNkn4ibGI6ERjGGCvi9E7AwehzVOBSUc
        oSgWqOmvVspeWunW9VfiWIziDJboXaNqnnZZ2EOihmINtyniTg13MVXDPZT2
        wDzcx4M9qB5SHqY9jHsY81D28DBKH3lIe5j8ASY6o3uSBQAA
        """,
        """
        kotlinx/coroutines/CoroutineScope.class:
        H4sIAAAAAAAAAIWSTW/TQBCG390kjusGmpavlPJV2gNwqNuKGxVSG4FkKRiJ
        VJGqnjbOqmxi7yJ7HfWYEz+Ef1BxqAQSiuDGj0LMmgAHDtjSzLyzs493Zv39
        x6cvAJ5ii2FzYmyq9HmYmNyUVmlZhN3fYT8x72QTjKE9FlMRpkKfha+HY5nY
        JmoM3oHSyj5nqD16PGihAS9AHU2Gun2rCoat3n/pzxj8gyStOAG42+xHcf/4
        MO6+aOEKgiVKXnUok5+FY2mHuVC6CIXWxgqrDMWxsXGZpoRaXXwwfCWtGAkr
        KMezaY26Zc40GNiEUufKqV2KRnsM2/NZEPAOD3ibovnM//aed+azfb7Ljpo+
        //rB423uavcZcbB2VKp0JPNiJzFZZvTOxDJsvCm1VZmM9FQVapjKw78HpHl0
        zUgyrPSo7bjMhjI/FlRDrJ5JRDoQuXJ6kQz6pswT+VI5sb4AD/7BYo9GU3et
        Yd1NivxdUh75NnlOb6NS90iF5JmbwJNL+BfV8v1FMQjygGzrVwGWCAX4WP6z
        +RZVu2f5M/jJJVofsXJRJTg2K3sHD6sfim6AAGunqEW4FuE6Wdxw5mZEkM4p
        WEFnvU3rBYICGwW8n1uFkiGNAgAA
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdwqXOJZSdX5KTmQdSVpRfWpKZl1osJOhUmpmT
        klpUHA/Um5uf512ixKDFAADN8kOtaQAAAA==
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
            Stubs.Composable,
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

    @Ignore // b/193270279
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
            Stubs.Composable,
            coroutineBuildersStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
