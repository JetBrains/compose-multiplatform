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
 * Test for [CompositionLocalNamingDetector].
 */
class CompositionLocalNamingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CompositionLocalNamingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CompositionLocalNamingDetector.CompositionLocalNaming)

    // Simplified CompositionLocal.kt stubs
    private val compositionLocalStub = compiledStub(
        filename = "CompositionLocal.kt",
        filepath = "androidx/compose/runtime",
        """
            package androidx.compose.runtime

            sealed class CompositionLocal<T> constructor(defaultFactory: (() -> T)? = null)

            abstract class ProvidableCompositionLocal<T> internal constructor(
                defaultFactory: (() -> T)?
            ) : CompositionLocal<T>(defaultFactory)

            internal class DynamicProvidableCompositionLocal<T> constructor(
                defaultFactory: (() -> T)?
            ) : ProvidableCompositionLocal<T>(defaultFactory)

            internal class StaticProvidableCompositionLocal<T>(
                defaultFactory: (() -> T)?
            ) : ProvidableCompositionLocal<T>(defaultFactory)

            fun <T> compositionLocalOf(
                defaultFactory: (() -> T)? = null
            ): ProvidableCompositionLocal<T> = DynamicProvidableCompositionLocal(defaultFactory)

            fun <T> staticCompositionLocalOf(
                defaultFactory: (() -> T)? = null
            ): ProvidableCompositionLocal<T> = StaticProvidableCompositionLocal(defaultFactory)
        """,
"""
        androidx/compose/runtime/CompositionLocalKt.class:
        H4sIAAAAAAAAAJ1UW08TQRg9s72yFChFoRQvCFW5KFtuGmklMRhiQ7lEGozh
        abq7kOlll+xuG3gx/A3/hW8aTUyf/VHGb7YlarHUdJOd+WbmzDlnv/lmf/z8
        +h3AGp4zLHLLcGxhnGu6XTuzXVNz6pYnaqa25Y+FJ2yrYOu8uuNFwBjiZd7g
        WpVbp9p+qWzqNBtgSOgd6P0ThndzhYrtVYWllRs17aRu6XLZ1bbbUSY7X+gq
        f+DYDWHwUtXsNJJlOM8VNwqdRrKbvfRyi8VidrM/1Zy/lWG2YDunWtn0Sg4X
        RM4ty/Z4S2jP9vbqVWkwfROKIFKBYBtdjby+sHhN6N39RKAyhHPCEt4mueqd
        6qMYYhhSMYhhhtU+MhBBnGHYME94veptc92znQuG6V7CDKnrxZFu0zCUezrP
        Xz/p/uomhhDCKhSMMyRdeRx6J0aW7Yuu3If+npsyNKkiJfM73Y3+6sNjSLa8
        3GUYvcrArulxg3uccqbUGgG6o0w2IQZWkYFC8+dCRhmKjGUGo3kZV5uXqpJU
        VCWq+H3zMpWOUxNNBBPKGyXDZoJRgikr8agSD6TU1nSSZYKEC/0HTGqtMKz3
        eVlZkf4PV5/4Z12MdYKXKlQQwS3bMBlGCsIy9+q1kukUJbHkkJgj7gg5bk8O
        HIpTi3t1h+Kpty07eashXEHLr37fOrqSnasH3OE10zOdv2BDdMh6ZZeftQXU
        Q7vu6Oa2kIPJNsfRNX4s01kGIZ8gJmWhUb9Eo2cIgFEf+4bB9wufMdLE6Cd5
        ltCoDftrt5CRiBYOCYxRv+xjIlhpo6LUr9IboYRiAIgP0LbbFLdEFMiyGJoK
        fvhI9bKz8AUTLZU1aslB1Jcb9lEJIhwjwgQZTf7LaEoanfqH0WR/Ru/caPRe
        V6PjRDhBhOO0vO6DnhID8JLY7lOOp48RyONBHjPUYjaPNB7m8QiPj8FczGH+
        GGEXIRcLLhZdJFw8cZH8BUMfUaX/BgAA
        """,
        """
        androidx/compose/runtime/ProvidableCompositionLocal.class:
        H4sIAAAAAAAAAI1S308TQRD+9lracmIpRaHgLxAkAolXURNjK0YxDTUFURpe
        eNr2Ftz2umf29hp869/if+CTiQ+m8dE/yjjblogkBO5hZvabb76Z29nff378
        BPAUKwxPuPJ1KP0Trxl2PoeR8HSsjOwIb0+HXenzRiC2BhlpZKhqYZMHaTCG
        Srn+otbiXe4FXB177xst0TSlzdqFeudVyvV6abPEsHrlijSSDKmyVNJsMiw9
        rLVDE0jltbod7yhWTUuMvMooKpZWD0j9MlZ5fTCH5S7XQn3stYRpaC6Jw5UK
        DR/yd+MgsHdBA3+4tPHZvFRGaMUD76044nFgtohqdNw0od7hui00tZ5ACq6L
        MVxjSJpPMmJ4dvFFXrwYGi7rD9tUuO3whWHhsmEZpk4pO8JwnxtOmNPpJuiN
        MGvGGFiboBNpT0WK/McM7/q9vOsUHLff+99lVgr93loy0+/l2EYmn8w726zo
        vJklIJ/NJeZdCz3v9wqsmPz1NeXkxqziBjWpM6xf/QHRlPnTyc/+zvR54qO2
        oXvdCn3BMFmTSuzGnYbQdXuFVsNyDriW9jwCx/flseIm1hQvfxwOUFVdGUlK
        73HNO4LW+vrfA2Fw98NYN0VF2vq5Uc3BsOIMMbkIhzZtP/pjWnwaCdyj0yvy
        Dvn02jr7jolvFDpYIOsO4AxRU1ikaGZIw3VkBzJpTCJHUvcHFRksWcxqj1OQ
        GMEJLA/8XTwg/5KyUzRF/hCJKqaruEEWN62ZqWIWhUOwCHOYP0QqQjbCrQi3
        I0xGuBMh/Rejxn6KQAQAAA==
        """,
        """
        androidx/compose/runtime/CompositionLocal.class:
        H4sIAAAAAAAAAJVTXU8TQRQ9s91+sPKxFMWCqCCo/VC2EjUohKgYkiatKDR9
        4cEMuwsObXfJ7rTBF9P4L3z1H/hE4oNpePRHGe9sixLUVPbhzr13zj33zNzZ
        7z++fgPwEIsMOe45gS+cI8v2m4d+6FpBy5Oi6VrrUSyk8L2yb/NGEowhu1p9
        Wj7gbW41uLdvbe4euLZcWfszxWCezyWhMyRWhSfkGsN8tlz3ZUN41kG7ae21
        PFt1Cq2NvldcydVI3iDUaqFaXVmLsLFsrjaMBIYMxGEw6PKdCBkK5f8+Iqke
        ddw93mrIDW5LP3jPMDtIJsPWwKOUzgKEJ93A4w3rZa/VOmFl0FL9Kjyou0F0
        mjcDSS/Kqe5m3ICGtPKSyrvMMLFg/8a+bUZghsWLkTOMnxZUXMkdLjnltGY7
        Ru+MKRNnYHVKHQkVFclzHjBsdjuThpbRjG7H0Ewyau2HGS31ONPt5PVUt2Oy
        Ja2ovZhK6WkzpZmxaSOtp7XlbifDivrJ54Rmxk8+MpZQtEvUqcqoLdKnms4O
        69G/n8PrwG8Lh+823L88jInzucW6pDe27jsuw1hZeO6rVnPXDaqqXvVWmBoP
        hIr7yaFtse9x2QrIH9mW3K5X+GF/z9j2W4HtbggVTG31JNVEKGj3uef5kkfz
        1+dobHGoL0YezZHsXYqe0IE1ddH5Y1z6Qo6GLNkEpQEdObKTPQCGMRIRxDGK
        MdrPR+iUSX8nTIoVXZHodVrHZvQPn2h65XyBHWOiR1yIurPU2Q5R9Xi/eplA
        as6JfOEYV1QRixTM9rK/FCT6CpQ3iatRVU9NDPei9Q7u0/qMMBnSO7WDWAnT
        JVwjixllrpdwAzd3wEJin9tBMsRIiFsh5kOMhlgIVeb2T+jXEMfzBAAA
        """,
        """
        androidx/compose/runtime/StaticProvidableCompositionLocal.class:
        H4sIAAAAAAAAAJ1SXW8SQRQ9s3x2bSmlVmn9qhYT2yYurZqoIIk2aUpCKxHC
        C0/D7hQHll2zO0vqG7/Ff+CTiQ+G+OiPMt4BGqsJIek+3I8z55699878+v39
        B4DneMzwintO4EvnwrL9wSc/FFYQeUoOhNVQXEm7HvhD6fCOK44m51JJ36v5
        NndTYAz1cvN1rceH3HK517Xed3rCVqVKba7qfL1ys1mqlBieXaM2hThDsiw9
        qSoMO09qfV+50rN6w4F1Hnm2JobW8SwqlnZbDLuLWOX9SUeaW6j5QdfqCdUJ
        uCQO9zxfr0fzzyLX1T2VlpFEykQCJkNcfZQhQ2n+IhatlzaRccQ5j1x1zG3l
        B58ZthfNxbB2STkVijtcccKMwTBG9820STCwPkEXUmdFipwDhpPxKGcaecMc
        j/516fEoPx7txcln2WE6F88ZJ6xovNvIZbKxLVPnL4nCivGfX5JGNqH1DukX
        TYYX13kE1G3ucoKrY63/T3zaV7TlI98RDKs16YmzaNARQVOLag3NafFA6nwG
        LjVk1+MqCigufJi2UvWGMpR0XOcBHwglgrd/r5bBbPhRYItjqes3ZzWtacUV
        Ig5g0L3rj2bXzwAx3KesQrhBPrm3/w03vlJk4AFZc4JmqSaDbYpuTVlYxspE
        JUn4Kik9nFSk8Yh8SksvURCbwTHsTPw9FMi/oVMtuNZGrIpcFetkcVObjSr9
        4HYbLEQem20kQ6yE2ApxJ0QmxN0QqT/sQJiNEwQAAA==
        """,
        """
        androidx/compose/runtime/DynamicProvidableCompositionLocal.class:
        H4sIAAAAAAAAAJ1SXU8TQRQ9s/1khVKKaMEvlJoIJG5BTQytTRRDaFKwkYYX
        nqa7A07ZnTW7sw289bf4D3wy8cE0PvqjjHfaEtGEkLAP9+PsuWfuvTO/fn//
        AeAlnjJsceVFofTOHDcMPoexcKJEaRkI5/254oF021HYlx7v+mJ7RJBahqoV
        utzPgTG0652tVo/3ueNzdeJ86PaEq2uN1pWyV+vVO51ao8bw4ga1OaQZsnWp
        pG4wrDxrnYbal8rp9QPnOFGuIcbOziSq1lYPGVavY9XXRx0ZbqUVRidOT+hu
        xCVxuFKh5mP+fuL7pqfaNLLI2cjAZkjrTzJmqF+9iGv3S6soeOKYJ77e4a4O
        o3OG5esGY5i7oOwJzT2uOWFW0E/RjTNjMgzslKAzabIqRd4Gw+5wULKtsmUP
        B/+6/HBQHg7W0uSLbDNfSpesXVa13i2UCsXUkm3y10Rh1fTPL1mrmDF6m3RE
        h+HVTV4BdVu6mODyWPP/E5+falrzdugJhtmWVGI/Cboi6hhRo2E4hzySJp+A
        UwfyRHGdRBRXPo5baaq+jCX9bvOIB0KL6O3fu2WwD8IkcsWONPWLk5rDccUl
        IjZg0cWbj2Y37wApPKSsQbhFPru2/g23vlJk4RFZe4ROm8eCZYrujFmEzIxU
        sihglpQejyryeEI+Z6SnKEhN4BRWRv4BKuTf0N8iCc4dIdVEqYl5srhtzEKT
        Drh7BBajjMUjZGPMxFiKcS9GIcb9GLk/6rXJ3BUEAAA=
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcclkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKiTkDBbILMnMz/PJT07M8S7hUuOSwaVeLy0/X4gtJLW4xLtEiUGL
        AQBypVQ1cAAAAA==
        """
    )

    @Test
    fun noLocalPrefix() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val FooCompositionLocal = compositionLocalOf { 5 }

                object Test {
                    val BarCompositionLocal: CompositionLocal<String?> = staticCompositionLocalOf {
                        null
                    }
                }

                class Test2 {
                    companion object {
                        val BazCompositionLocal: ProvidableCompositionLocal<Int> =
                            compositionLocalOf()
                    }
                }
            """
            ),
            compositionLocalStub
        )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/Test.kt:6: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                val FooCompositionLocal = compositionLocalOf { 5 }
                    ~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:9: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                    val BarCompositionLocal: CompositionLocal<String?> = staticCompositionLocalOf {
                        ~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:16: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                        val BazCompositionLocal: ProvidableCompositionLocal<Int> =
                            ~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
            """
            )
    }

    @Test
    fun prefixedWithLocal() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val LocalFoo = compositionLocalOf { 5 }

                object Test {
                    val LocalBar: CompositionLocal<String?> = staticCompositionLocalOf { null }
                }

                class Test2 {
                    companion object {
                        val LocalBaz: ProvidableCompositionLocal<Int> = compositionLocalOf()
                    }
                }
            """
            ),
            compositionLocalStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
