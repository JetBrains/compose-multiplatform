/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ScaffoldPaddingDetector].
 */
class ScaffoldPaddingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ScaffoldPaddingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ScaffoldPaddingDetector.UnusedMaterialScaffoldPaddingParameter)

    // Simplified Scaffold.kt stubs
    private val ScaffoldStub = compiledStub(
        filename = "Scaffold.kt",
        filepath = "androidx/compose/material",
        checksum = 0x2dde3750,
        """
            package androidx.compose.material

            import androidx.compose.foundation.layout.PaddingValues
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun Scaffold(
                modifier: Modifier = Modifier,
                topBar: @Composable () -> Unit = {},
                bottomBar: @Composable () -> Unit = {},
                content: @Composable (PaddingValues) -> Unit
            ) {}

        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGI2BijgUueSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHiCk5OTEvLz0nxLuHi5WJOy88XYgtJLS7xLlFi0GIAACJwI+tQ
        AAAA
        """,
        """
        androidx/compose/material/ScaffoldKt＄Scaffold＄1.class:
        H4sIAAAAAAAAAKVU604TQRT+Zlt6xxZEuYj3qi0o2+LdNiSEQNxQMLHYxPBr
        2t3C0N1Z091t8B8P4RP4BKKJJJoY4k8fynhmaQ2GiBo32bMn53zfmXOb/fb9
        0xcA9/CEQefS7LrC3NVbrvPK9Szd4b7VFdzW6y3ebru2uernB2q+HAdjWK11
        XN8WUt/pObqQhJeEr3GnafLKcV87kC1fuNLTV/paqTrwv5DCryxUGKZ+HyyO
        KMOl0wPGEWOIVQWFW2CIFIoNhmjBKDYySCCVwhDSZPC3hcdQrv1juZReTMie
        27EYxgrF2g7vcd3mckt/1tyxWn4lgyySKWgYYUgfqyyOswwJY72+sbi+tMww
        /EvZGZzD+STGME6gassOk1f5hqGmlPtMkrRphpEBcc3yucl9TilpTi9CA2RK
        xJQAA+soJULOXaG0EmlmmWH6cC+ROtxLaTmNPrnDvSmtxJ6mvr6NaQlNYeYp
        8SqXrnztuIFHLaRg+b9pUxy3GXI/e2VabR7YPsObwskuB0Jfc03RFlb3Twvy
        n/5yxTg5JbUMc9Cp0kG6cx3KNLrkmjTY0Zrb4naDU31N29pQgiFbE9JaD5ym
        1e1bMoaUVnfJ5p5n0S5ll2XLdj0ht2gy267JkKyLLcn9oEvgVN0Nui1rRSjm
        5PNA+sKxGsITFGpRStfnYdoo0ZSHqON0rzCpxk6ji9JLq0CWedLyhGBq0DOR
        A2T2w2HfJZk5smI45IyoPewzZkMMvQqs0T1XMGVIHyOyI2JukYi5PnFe7ZE6
        fOYjRt9j4t0p/ET/4ASlPTh4nNDqSX+G9vIAFz7g4n5oGMJ9kimCHQEm8CCs
        8w7V/zA8JIJH4beMx+Gvia49sS5vImLgioGrBq7hukHNuGHgJm5tgnkooEh+
        DzMeZj1kfwB7t1DT1wQAAA==
        """,
        """
        androidx/compose/material/ScaffoldKt＄Scaffold＄2.class:
        H4sIAAAAAAAAAKVU604TQRT+Zlt6WYotiHIR71VbULatd9uQECJxQ8HEYhPD
        r2l3C0O3s6a72+A/HsIn8AlEE0k0McSfPpTxzNIqhogaN9mzJ+d835lzm/36
        7eNnAHfwiMHg0uq6wtoxmm7npevZRof7dldwx6g1eavlOtaKnx2o2VIcjGGl
        2nZ9R0hju9cxhCS8JHyVdxoWLx/1tQLZ9IUrPWO5rxUqA/9zKfzyQplh+vfB
        4ogyXDg5YBwxhlhFULgFhkguX2eI5sx8PYUEdB1DGCaDvyU8hmL1H8ul9GJC
        9ty2zTCey1e3eY8bDpebxtPGtt30yymkkdShYZRh+EhlcZxmSJhrtfXFtaXH
        DCO/lJ3CGZxNYhwTBKo0nTB5lW8Yalq5TyVJm2EYHRBXbZ9b3OeUktbpRWiA
        TImYEmBgbaVEyLkjlFYgzSoyzBzsJvSDXV3LaPTJHOxOawX2RP/yJqYlNIUp
        UeIVLl35quMGHrWQgmX/pk1x3GTI/OiVZbd44PgMr3PHuxwIY9W1REvY3T8t
        yH/6i2Xz+JTUMszDoEoH6c63KdPokmvRYMeqbpM7dU71NRx7XQmGdFVIey3o
        NOxu35IypbS7Sw73PJt2Kf1YNh3XE3KTJrPlWgzJmtiU3A+6BNZrbtBt2stC
        MaeeBdIXHbsuPEGhFqV0fR6mjQJNeYg6TvcKU2rsNLoovbQKZCmRliUEU4Oe
        jewjtRcO+zbJ1KEVIyFnVO1hnzEXYuhVYI3uuYKxkPKTyA6JmUUiZvrEktoj
        dfjsB4y9w+TbE/iJ/sEJSntw8ASh1TP8CdqLfZx7j/N7oWEId0nqBDsETOJe
        WOctqv9+eEgED8JvEQ/DXxNde2Jd3EDExCUTl01cwVWTmnHNxHXc2ADzkEOe
        /B5mPcx5SH8Hm6I3JNcEAAA=
        """,
        """
        androidx/compose/material/ScaffoldKt.class:
        H4sIAAAAAAAAAMVVS3PbVBT+rl+SFSd1lThN3BJK49I0jyo25elQSE3Titim
        g9tssrqWZaNYusrokSkbJgx/gQ1b/gGsOiwYD0v+BX+E6ZFshzTupGTKDAvd
        e173nO+ce+7Rn3//9juAu2gwlLjoeK7VeaYZrnPo+qbm8MD0LG5rLYN3u67d
        2Q0kMIb8AT/ims1FT/uqfWAaJE0yyGMrhu9X6hPOQktruB2ra5letd53A9sS
        2sGRo3VDYQSWK3xtZ0RtvqG+XL29x/DXm2HYGuufCiuo3vtvzctbG5Pgum4o
        OjxSU2m/dcNAe8w7HUv09rgdmn71TIQox5uTXrxQBJZjarWY523brDIs112v
        px2YQdvjFuHgQrgBH2JqukEztG2ykp1RbWQoDEunMrAENYKgRtBF4JEDy/Al
        5BgKxjem0R95eMw97phkyHBrpX62RaqnJK3ISY8yyGEGlxRMI8+QCdzD+5xi
        qwzZthsErhOzcwyS4RIAEciYJ1znXyvD9dd1z2tNymSSH3dzqWN2eWgHDD/+
        z12tTxY1aoJr54GS8BaVM2oGLsgJw/k5lE4sqzm8jetZLOEdBu3fjIbSScXK
        EpapnfRm68l2s/aAoTwZ9HwHFP0m3s2ihFsvd+Ir6ibh9oURViSsXRxWJYa1
        kcU67uSQRkZBApsMl8c31zADTk+YU/8knKMkTVYWLZloAQPrR0SClM+siKKj
        iU6Z4YfB8Q1lcKwk8ol4WzjZ4k9OjOni0/zguJjYZBVZJmOikpVZolLFaTWl
        knwz/cfPmYSciaXShPRKXi7OxjJlpMkONY+kCEqFRSjVcTanX8yEMHojlYsP
        MYapcTnv9OlNpWpux2S4VLeE2Qydtuk9iWZWFNA1uL3H6TKIHwmzLasneBB6
        RF/9ejjpdHFk+Rapt/8ZavQzO6s9mU4vmU23Am70G/xwFCCnC2F6NZv7vklq
        peWGnmHuWJFuceRybyIcytQHqeiOaV+MGoO4GnF8JF9cVaee4/KaOkvrulqg
        dUO9Qusv8ZEvoh6hyi/QOHxA9OrwEBSSIKZU+lhMzdGXiKl5FJHETuxBwsOR
        D5n2R5E+RUw2brszaz6Lq7hGdITQoVAZ2iuFVOq7n6D8ihsDLO0WUukhtzLA
        ar2QkoacRlxjdW194znKQ+g6rWkkZ6an4yyWCAkoiETYZ2gvYIpCZbGMHGWV
        Jbxfkl6lg6U4swV6SsN9N3Z3H3Xa6wSuQm7f20dSx10d7+v4AB/q+Agf6/gE
        1X0wH1v4dB9TPtI+7vlQfCz4UH185kP2Medj3sfnPrZfADvMWafjCAAA
        """
    )

    @Test
    fun unreferencedParameters() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.material.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.*

                @Composable
                fun Test() {
                    Scaffold { /**/ }
                    Scaffold(Modifier) { /**/ }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { /**/ }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}, content = { /**/ })
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { _ -> /**/ }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding -> /**/ }
                }
            """
            ),
            ScaffoldStub,
            Stubs.Modifier,
            Stubs.PaddingValues,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/foo/test.kt:10: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold { /**/ }
                             ~~~~~~~~
src/foo/test.kt:11: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier) { /**/ }
                                       ~~~~~~~~
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { /**/ }
                                                                    ~~~~~~~~
src/foo/test.kt:13: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}, content = { /**/ })
                                                                              ~~~~~~~~
src/foo/test.kt:14: Error: Content padding parameter _ is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { _ -> /**/ }
                                                                      ~
src/foo/test.kt:15: Error: Content padding parameter innerPadding is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding -> /**/ }
                                                                      ~~~~~~~~~~~~
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

                import androidx.compose.material.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.*

                val foo = false

                @Composable
                fun Test() {
                    Scaffold {
                        foo.let {
                            // These `it`s refer to the `let`, not the `Scaffold`, so we
                            // should still report an error
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding ->
                        foo.let { innerPadding ->
                            // These `innerPadding`s refer to the `let`, not the `Scaffold`, so we
                            // should still report an error
                            innerPadding.let {
                                if (innerPadding) { /**/ } else { /**/ }
                            }
                        }
                    }
                }
            """
            ),
            ScaffoldStub,
            Stubs.Modifier,
            Stubs.PaddingValues,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold {
                             ^
src/foo/test.kt:21: Error: Content padding parameter innerPadding is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding ->
                                                                      ~~~~~~~~~~~~
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

                import androidx.compose.material.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.*

                @Composable
                fun Test() {
                    Scaffold {
                        it
                    }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding ->
                        innerPadding
                    }
                }
        """
            ),
            ScaffoldStub,
            Stubs.Modifier,
            Stubs.PaddingValues,
            Stubs.Composable
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
