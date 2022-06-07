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
 * Test for [ComposableCoroutineCreationDetector].
 */
class ComposableCoroutineCreationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableCoroutineCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableCoroutineCreationDetector.CoroutineCreationDuringComposition)

    private val coroutineBuildersStub: TestFile = compiledStub(
        filename = "Builders.common.kt",
        filepath = "kotlinx/coroutines",
        checksum = 0xdb1ff08e,
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
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdwqXOJZSdX5KTmQdSVpRfWpKZl1osJOhUmpmT
        klpUHA/Um5uf512ixKDFAADN8kOtaQAAAA==
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
        """
    )

    private val flowStub: TestFile = compiledStub(
        filename = "Flow.kt",
        filepath = "kotlinx/coroutines/flow",
        checksum = 0x3416a857,
        """
        package kotlinx.coroutines.flow

        class Flow<out T>
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdwqXOJZSdX5KTmQdSVpRfWpKZl1osJOhUmpmT
        klpUHA/Um5ufB1RowCWOqVAvLSe/XIgLptq7RIjTOT8nJzUZaLQSgxYDAKm0
        uUqbAAAA
        """,
        """
        kotlinx/coroutines/flow/Flow.class:
        H4sIAAAAAAAAAH1Qz08TQRh932y7C0uVBQWLInjEHlggJCZCSNCEpEnVRJpe
        epq2Kw7dziQ7s8Bx/xb/A04mHsyGI3+U8ZvKSRPn8L7vvXnz/Zj7Xz9+AjjE
        NmFzalyu9E06NoUpndKZTb/k5jo9Y4hAhJ3j/tvepbySaS71RfppdJmN3dHJ
        vxIh+VuL0CCEx0ord0IIdl4PWggRxWhigdBwX5UlbPX+NwOXXXkwpB8yJyfS
        SdbE7CrgHchDk0BTlm6UZ3ucTfYJnbpqxaIt4rqKRcJQV+266oQLdZXQKzoQ
        e+Jd8+5bKJLAvzjgIn3iioh8292p4wHfm0lGWO7xSB/L2Sgr+nKUs7LaM2OZ
        D2ShPH8QF8/VhZauLDiPz01ZjLMz5S82PpfaqVk2UFax81Rr46RTRlvsQ/Bf
        +MPt/dcwbjBL55xX63zH4i0nAs8Zw7nYwAvG1h8DYixxDLA5dwV4OY9tbHF8
        w54Wex4NEXTxuItlRiQeVrpYxZMhyOIp1oZoWCxZrFs8s4h+Az4tHOgiAgAA
        """
    )

    private val flowBuildersStub: TestFile = compiledStub(
        filename = "Builders.kt",
        filepath = "kotlinx/coroutines/flow",
        checksum = 0xb581dd7,
        """
        package kotlinx.coroutines.flow

        fun <T> flowOf(
            value: T
        ): Flow<T> = Flow()
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdwqXOJZSdX5KTmQdSVpRfWpKZl1osJOhUmpmT
        klpUHA/Um5ufB1RowCWOqVAvLSe/XIgLptq7RIjTOT8nJzUZaLQSgxYDAKm0
        uUqbAAAA
        """,
        """
        kotlinx/coroutines/flow/BuildersKt.class:
        H4sIAAAAAAAAAIVR32/SUBT+TguFdSgd/trY3JS5hb3Yjfi0ERI1WSQiS1xD
        Yni6QEculN6kvcU98rf4F/hmookhPvpHGU8RYyLRNe13zvn6ndN7vn7/8fkr
        gGc4JFTGSgcyvHb7KlKJlqEfu1eBeu++SGQw8KP4tc6BCM5ITIUbiHDoXvRG
        fp9Zk2Cl0osrQq3a+ltwdtT61+xzhjNCo+6drrY1qp53Q2+dFQ0esN9S0dAd
        +boXCRnGrghDpYWWivO20u0kCFi1879ROazxGnUZSt0gmNWjTgHrKNiwcYuQ
        nYog8Qml1WMSNpZndN/4WgyEFswZk6nJ1lIKWQKN08Rg/lqm2TFngxO2az6z
        bX6MTcM28mZ5z5nPyvlSpmS8Mo6pksnPZ45RsxyzzMS3D5bhZNLOGmH3JlPJ
        I6z//nlPx5qQeakGvEOxxdp2Mun5kSd6wWIr1RdBR0QyrZfk2qUchkInEefb
        b5NQy4nfDKcylvz6+R+DCfalSqK+fy7Ttq2ltLMixAkMZJBeLEMWFkzscVVj
        njjmv8B+9wm3P6ZW4RGjteAtPGYs/NKgCIdjZaHJYX+pyi/qJwvcxQHHU2Y3
        +CulLswm7jRxlxH3mriPB01sYqsLilHGdhfZOL13YjyMUYxh/QQRN9W+GAMA
        AA==
        """
    )

    private val flowCollectStub: TestFile = compiledStub(
        filename = "Collect.kt",
        filepath = "kotlinx/coroutines/flow",
        checksum = 0x8685bc57,
        """
        package kotlinx.coroutines.flow

        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch

        fun <T> Flow<T>.launchIn(
            scope: CoroutineScope
        ) = scope.launch {}
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlk5iXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbqpeWny/EFpJaXOJdwqXOJZSdX5KTmQdSVpRfWpKZl1osJOhUmpmT
        klpUHA/Um5ufB1RowCWOqVAvLSe/XIgLptq7RIjTOT8nJzUZaLQSgxYDAKm0
        uUqbAAAA
        """,
        """
        kotlinx/coroutines/flow/CollectKt＄launchIn＄1.class:
        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
        """,
        """
        kotlinx/coroutines/flow/CollectKt.class:
        H4sIAAAAAAAAAI1UW08TQRT+ZntlLdBWQECsAlWusm29U2yimMbGisQ2JIYH
        nW6Xsu121uyl8ti/5JNEE9Nnf5Tx7LYVQQT7cM6Zb+Z855s5Z/vj59fvAO5j
        k2G+ZTqGLo4U1bRM19GFZisHhvlJ2TYNQ1OdV04EjCHe5B2uGFw0lDe1JuER
        BBiiBneFelgSDC+Wy/9iKpLJn7e7PQwrqvlRy6/sMXzYqm6Wz9bKFy4m31qr
        VvOF/y2xWDathtLUnJrFdWErXAjT4Y5uUrxjOjuuYeQZwlvOoW4XohhhSPWJ
        lWanrejC0SzBDaUkHIvSddWO4ArDpHqoqa1B/i63eFujgwxLy39f5w+k4pE0
        SFcMoxiTEcM4Q8j2xEaRYFi/tD3pYQ/S2QiuesJ1oTsFuufw0U6/h6DA9a/r
        V53ElIwJXDt9ywOi7L9IcRDlIphhSJ8j57mrG3XNst+rZrttCm9grpOMviyG
        4rm9O9OX8sW1faU3kJIxh5sMY2mvN+mT4UtdPHtezy/XwJAYynitObzOHU6Y
        1O4E6FthngkxsJYXSIQf6V6UoaieZdjsdZNyrytLccl300M3m4r3urNShi0E
        o71uXFqNJoNJ6aWUCeTC8SBthDyGHKMSYFUGedDYjZbDENw26xrDeJlE7rjt
        mmZVec0gJFk2VW7scUv31gNwpKI3BHdci+L0W5ca3dZKoqPbOm3/HslnJ+PO
        ECsJoVnbBrdtjZZyxXQtVSvqHt3MgGKvT/BHHrKQEIT3o2MIIYwAcrTaJVQi
        P7ealI8RX0smPfsNE+/YF0z3MHuMW5+998M9smHKjmGE/oeAqX4e5rHg884h
        gUXaf+CfjuAh+VGJgKhf1LMBPCIr02rCT5nGY/9wFk/IPyU8TdJu7yNQwp0S
        lshiuYQVrJawhvV9MBt3sbGPsE1fHBQbGRsLNhI2Fn8B0lRZliIFAAA=
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
                import kotlinx.coroutines.flow.*

                @Composable
                fun Test() {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda = @Composable {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda2: @Composable () -> Unit = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    })
                    LambdaParameter {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }

                    val localLambda2: @Composable () -> Unit = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }
            """
            ),
            Stubs.Composable,
            coroutineBuildersStub,
            flowStub,
            flowBuildersStub,
            flowCollectStub,
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:10: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:11: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:12: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    flowOf(Unit).launchIn(CoroutineScope)
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:17: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:18: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    flowOf(Unit).launchIn(CoroutineScope)
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:22: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:23: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:24: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    flowOf(Unit).launchIn(CoroutineScope)
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:33: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:34: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:35: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:38: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:39: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:40: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:46: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:47: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:48: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:52: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:53: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:54: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
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
                import kotlinx.coroutines.*
                import kotlinx.coroutines.flow.*

                fun test() {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda2: () -> Unit = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    })
                    lambdaParameter {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }

                    val localLambda2: () -> Unit = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }
            """
            ),
            Stubs.Composable,
            coroutineBuildersStub,
            flowStub,
            flowBuildersStub,
            flowCollectStub,
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
