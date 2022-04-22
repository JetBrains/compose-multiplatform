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

package androidx.compose.material3.lint

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
        mutableListOf(ScaffoldPaddingDetector.UnusedMaterial3ScaffoldPaddingParameter)

    // Simplified Scaffold.kt stubs
    private val ScaffoldStub = compiledStub(
        filename = "Scaffold.kt",
        filepath = "androidx/compose/material3",
        checksum = 0xfee46355,
        """
            package androidx.compose.material3

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
        H4sIAAAAAAAAAGNgYGBmYGBgBGI2Bijg0uCSSsxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzDEW4gpOTkxLy89J8S7h4uViTsvPF2ILSS0u8S5RYtBiAACpks1u
        UQAAAA==
        """,
        """
        androidx/compose/material3/ScaffoldKt＄Scaffold＄1.class:
        H4sIAAAAAAAAAKVU604TQRT+Zlt62RZbEOUi3kFbULYt3ktICIG4oWBisYnh
        17S7haG7s6a7bfAfD+ET+ASiiSSaGOJPH8p4ZmkNhogaN9mzJ+d835lzm/32
        /dMXAPfwhKHApdX2hLVnNDz3lefbhssDuy24M29UG7zZ9BxrLZjqq1PFOBjD
        WqXlBY6Qxm7XNYQkguSOUeFu3eLlk75mRzYC4UnfWO1phYW+/4UUQXmxzDDx
        +2BxRBmunB0wjhhDbEFQuEWGSC5fY4jmzHwtjQR0HQNIkSHYET5DqfKv9VJ+
        MSG7XstmGMnlK7u8yw2Hy23jWX3XbgTlNDJI6tAwxJA6UVoc5xkS5kZ1c2lj
        eYVh8Je607iAi0mMYJRACw0nzF4lHIaaUO5zSdImGYb6xHU74BYPOKWkud0I
        jZApEVMCDKyllAg594TSCqRZRYbJo/2EfrSva1mNPtmj/QmtwJ7qX9/GtISm
        MCVKfIFLT752vY5PPaRg03/VpzjuMGR/Nsuym7zjBAxvcqf73BHGumeJprDb
        f1qR//QXy+bpMal1mINBpfbTnWtRptFlz6LJDle8BndqnAqsO/amEgyZipD2
        Rset2+2eJW1KabeXHe77Nm1TZkU2HM8XcptGs+NZDMmq2JY86LQJrFe9Trth
        rwrFHH/ekYFw7ZrwBYVaktILeJg2CjTmAWo53SyMq7nT7KL00i6QpUTaFCGY
        mvRM5BDpg3Da8yTTx1YMhpwhtYg9xmyIoVeBNbrqCqYMqRNEdkzMLhEx2yOW
        1CKpw2c+Yvg9xt6dwU/0Dk5Q2v2DRwmtntRnaC8PcekDLh+EhgHcJ6kT7Bgw
        hgdhnXep/ofhIRE8Cr9FPA7/TnTxiXV1CxET10xcN3EDN01qxrSJW7i9BeYj
        hzz5fcz4mPWR+QHL6C/y2gQAAA==
        """,
        """
        androidx/compose/material3/ScaffoldKt＄Scaffold＄2.class:
        H4sIAAAAAAAAAKVUbU/TUBR+bjf2xnADUV7Ed9ANlI7h+wgJIRAbBiYOlxg+
        3bUdXNbemrVd8Bs/wl/gLxBNNNHEED/6o4znlk0xRNTYpKcn5zzPueft9uu3
        j58B3MEjhhKXVtsT1p5ueu4Lz7d1lwd2W3BnXq+ZvNn0HGstmOypk+UkGMNa
        teUFjpD6bsfVhSSC5I5e5W7D4pXjvmYozUB40tdXu1ppoed/JkVQWawwjP8+
        WBJxhkunB0wiwZBYEBRukSFWKNYZ4gWjWM8ihUwGfegnQ7AjfIZy9V/rpfwS
        Qna8ls0wXChWd3mH6w6X2/qTxq5tBpUsckhnoGGQof9YaUmcZUgZG7XNpY3l
        FYaBX+rO4hzOpzGMEQItmE6UvUo4CjWu3GfSpE0wDPaI63bALR5wSklzOzEa
        IVMioQQYWEspMXLuCaWVSLPmGCYO91OZw/2Mltfokz/cH9dK7HHmy+uEltIU
        pkyJL3DpyZeuF/rUQwo29Vd9SuIWQ/5Hsyy7yUMnYHhVONnnUOjrniWawm7/
        aUX+0z9XMU6OSa3DLHQqtZfubIsyjS97Fk12qOqZ3KlzKrDh2JtKMOSqQtob
        oduw211L1pDSbi873Pdt2qbcijQdzxdym0az41kM6ZrYljwI2wTO1Lywbdqr
        QjHHnoYyEK5dF76gUEtSegGP0kaJxtxHLaebhTE1d5pdnF7aBbKUSZskBFOT
        no59QPYgmvY8yeyRFQMRZ1AtYpcxE2HoVWCNrrqCsYjyk8iOiPklIua7xLJa
        JHX49HsMvcXom1P4qe7BKUq7d/AIodXT/wna8w+48A4XDyJDH+6SzBDsCDCK
        e1Gdt6n++9EhMTyIvnN4GP2d6OIT6/IWYgauGLhq4BquG9SMKQM3cHMLzEcB
        RfL7mPYx4yP3HfQNQiHaBAAA
        """,
        """
        androidx/compose/material3/ScaffoldKt.class:
        H4sIAAAAAAAAAMVUS3PbVBT+ru1YkmOnrhKniVtCaRya5lHZbnk6FFLTtCK2
        6eA2m6yuZdkolq8yemTKhgnDX2DDln8Aqw4LxsOSf8EfYXok2yGNOwm0zLDQ
        Pc97zneOzj1//PXrbwDuos6wwkXbdaz2M81w+oeOZ2p97puuxe07WtPgnY5j
        t3d9CYwhe8CPuGZz0dW+bB2YBmnjDPLYi+G71dpEtMDS6k7b6limW6n1HN+2
        hHZw1Nc6gTB8yxGetjPiim9oL1Vu7TH8+WYYtsb2p8LyK/f+W/fS1uYkuI4T
        iDYPzdTab5zA1x7zdtsS3T1uB6ZXOZMhrHFlMoobCN/qm1o1knnLNisMyzXH
        7WoHpt9yuUU4uBCOz4eYGo7fCGybvOT+qDcyUgxLpyqwBE2C4LamC9+lAJbh
        SUgz5IyvTaM3ivCYu7xvkiPDzdXa2RGpnNI0wyBdqiCNGVxKIYMsQ9J3Du9z
        yq0yKC3H951+JM4xSIZDAIQvY55wnf9bGa5fND0XupTIJTue5kLb7PDA9hl+
        +J+nWp9sajgE184DJeEtamc4DFxQEIbzayiceFbSeBvXFSzhHYbiP9oNhZOW
        lSQs0zzpjeaT7Ub1AUN5MusFESj/Ct5VUMDNl2fxFZ2TcOvfYyxLWH8NYOUI
        2KaCDdxOYwrJFGIoMlwe/7266XN6xpxmKNY/itN6ZeGRDA8wsF7IxMj4zAo5
        uhprlxi+HxzfSA2OU7FsLCILJyT65NiYzz/NDo7zsSIryzI5ExcvzxKXyGfU
        hEr64tTvPyVjcjLSShPaK1k5PxvpUiOLMrQ8kkIoZRaiVMfVnH41E8rwnbyi
        gRctMobpcTtv9+hdJapO22S4VLOE2Qj6LdN9Eu6tMKFjcHuP098geaRUmlZX
        cD9wib/61XDb6eLI8iwyb/+92BgKZ60nG+olt0zT50avzg9HCdK6EKZbtbnn
        mWRONZ3ANcwdK7QtjkLuTaRDieYgEf5joovhYJBUJYmP9Itr6vRzXF5XZ+nc
        UHN0bqpX6Pw5uvJ5OCPU+QVaiQ+IXxteQoo0iDiVPhZxc/TFIm4eecSxE0WQ
        8HAUQyb6KLQnSFCisTtzZhVcxTXiQ4R9SpUkWs4lEt/+iNQvuDHA0m4uMTWU
        VgdYq+US0lDSSKqvrW9sPkdpCF2ncwrxmUwmqmKJkICSSIR9hmgO05RKwTLS
        VJVCeL8gu0oXC1FlC/SUhnQ3CncfNaI1AlemsHf2EddxV8d7Ot7HBzo+xEc6
        PkZlH8zDFj7Zx7SHKQ/3PKQ8LHhQPXzqQfYw52Hew2cetl8ABZTb8egIAAA=
        """
    )

    @Test
    fun unreferencedParameters() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.material3.*
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
src/foo/test.kt:10: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold { /**/ }
                             ~~~~~~~~
src/foo/test.kt:11: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier) { /**/ }
                                       ~~~~~~~~
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { /**/ }
                                                                    ~~~~~~~~
src/foo/test.kt:13: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}, content = { /**/ })
                                                                              ~~~~~~~~
src/foo/test.kt:14: Error: Content padding parameter _ is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { _ -> /**/ }
                                                                      ~
src/foo/test.kt:15: Error: Content padding parameter innerPadding is not used [UnusedMaterial3ScaffoldPaddingParameter]
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

                import androidx.compose.material3.*
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
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold {
                             ^
src/foo/test.kt:21: Error: Content padding parameter innerPadding is not used [UnusedMaterial3ScaffoldPaddingParameter]
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

                import androidx.compose.material3.*
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
