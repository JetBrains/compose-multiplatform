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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */

/**
 * Test for [ComposedModifierDetector].
 */
@RunWith(JUnit4::class)
class ComposedModifierDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposedModifierDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposedModifierDetector.UnnecessaryComposedModifier)

    /**
     * Simplified Modifier.composed stub
     */
    private val composedStub = compiledStub(
        filename = "ComposedModifier.kt",
        filepath = "androidx/compose/ui",
        checksum = 0xad91cb77,
        """
            package androidx.compose.ui

            import androidx.compose.runtime.Composable

            fun Modifier.composed(
                inspectorInfo: () -> Unit = {},
                factory: @Composable Modifier.() -> Modifier
            ): Modifier = this
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXJJYyhrjRTSMgZwk7xzU/JTMsEK+XjYilJ
        LS4RYgsBkt4lSgxaDACMRj6sewAAAA==
        """,
        """
        androidx/compose/ui/ComposedModifierKt＄composed＄1.class:
        H4sIAAAAAAAAAJVUWU8TURT+7nQfipRFWdy1YgvKtOCaNiSEQJxQMBFsYni6
        7Qxw6fSOmaXBN/6C/0Q0kUQTw7M/ynjutDW4gU4y956c831nn/n67dMXAA/w
        lKHMpeW5wjowmm77tevbRiiM5a5orbuW2BG2txbke1YrX06BMazVWm7gCGns
        d9qGkIHtSe4YNd5uWLxy2rYTymYgXOkbqz2pVO3bX0oRVBYrDFN/d5ZCnOHa
        2Q5TSDIkq4LcLTLECsU6Q7xgFutZpKHrSGCAFMGe8BkWav9dMCWYFLLjtmyG
        sUKxts873HC43DWeN/btZlDJYggZHRqGGQZO1ZbCKEPa3NjcWtpYXmEY/Knw
        LC7iUgZjGCdQtelE6auMI1dTynwhQ9IVhuE+cd0OuMUDTilp7U6MhsjUkWBg
        LSXESH8glFQiySozjJ4cJvWTQ13LabmTwymtxJ7pyjRPqVa5dOWbthv61DYw
        TP9ba1KYZcj96I9l7/DQCRjeFv7Y2z7xvLU4x16umL93vnh2xCzuY4568GsN
        cy1KN77sWjTRkZrb5E6de4I3HHtLHQxDNSHtjbDdsL2eJmtKaXvLDvd9m9Zo
        aEU2HdcXcpdGsudaDJlNsSt5EHoE1jfd0Gvaq0IxJ1+EMhBtuy58Qa6WpHQD
        HtWGEo03QY2nTwqTat40uDi9tAOkKZOUJwTNBsmZ2DGyR2rgmKcz29ViMOIM
        qwXsMWYjDL0KrGEhgilF6hSRdYm5JSLmesR5upUtPfMRI+8x8e4MfroXOE1p
        9wOPE1o9A5+hvTrG5Q+4ehQpEvSnAXSCdQETeBjVeQ8GHkVBYngc3SU8obtM
        yGvEur6NmIkbJm7SiVsmbiNv4g6mt8F83EVhG5qPoo+Z7xp9fu/RBAAA
        """,
        """
        androidx/compose/ui/ComposedModifierKt.class:
        H4sIAAAAAAAAALVU308bRxD+9vzr7BpibJISh1DSOAkYkrNJ2qY1IY1QkE4x
        bhVTXnhazmuy+LyH7s6IvET8C33sa/+Cqk9RHyrUx0r9l6rOns+BAMKVqtq6
        uZmdmW++nZ29P//+7XcAT/Atw32uOr4nO0eW4/UPvEBYA2mtD9XOpteRXSn8
        V2EGjKGwzw+55XK1Z323uy8cWk0wmHFih+HdQvMyuBFMo9nzQlcqa/+wb3UH
        ygmlpwJrI9ZqY/z1xuLV8Ax//TcCqyP/D0qGjbVxfFYfXl1t6Wr32vj93G16
        /p61L8Jdn0sqzZXyQj6k0fLC1sB1KSq9Gr6RwZqJLMPcGcpShcJX3LVsFfqU
        Lp0gg08YrjtvhNOL87/nPu8LCmR4sNA8f8KNMyttDbLXWNzOYwKTOeRxjWGC
        cA8o0PNt1fVMTDFkulzbb02UGCYrmlvldEbmxu15ftyUjA2pU0hhVLHSEV0+
        cEOGH//n6bQvdm/sAdf/3fX70L9KPYNbdOfsVnvrRWv9JcPjS0tcCdHI4zbm
        spjFZx8PzCW7zuBOHimkczBwl2Fq1IRNEfIODzntwegfJuhzwrRIMbCeVgxa
        P5Jaq5HWqTPwk+O53MlxzpgxRi+jcKoOn/LTwslx2aixKj0rkyZFlM1ismjU
        krXEymwhVZ6JLDaUtfQfP6cNMxNJUxdaYSiOOJ4dGVyyrufk3sX2+QMVyr6I
        e8h3XdHQYxsnvzwKBV0lT41Qtt4e6IDS+ZY/6tHMJde9jmC41pRKtAb9XeFv
        aUBNxnO4u819qe14MduWe4qHA5/0W6+HNGx1KANJ7hend5+hct774Rp/FDbR
        DrnT2+QHcYG8rZTw110eBILcubY38B2xIbXvZgy5faEc6nT4SeifgZt6Gsj6
        kqzXZOsjnq4Wc+9RWCoWSS4Xp0lWf4mivyKZ1r1HFk9Jnx/G4zpuRHjTmMKn
        5NdaCTOU8XWUl8E3caZJ7wY9pURsnJGFLNEpk67JPCPolAa6nXz3E3K/Yv4E
        nzerS8vvURmSWSVJKBMRq8mISZr+Gfqcpcl6RnaOwGYjZjNYi5K+wHN6b9D6
        PYK/v4OEjQc2Fkhi0UYVSzaW8XAHLMAjWDvIBkgFuBFgKkCNehegFGAlwOMA
        T/4BiHoaCXoHAAA=
        """
    )

    @Test
    fun noComposableCalls() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed
                import androidx.compose.runtime.Composable

                fun Modifier.test(): Modifier = composed {
                    this@test
                }

                fun Modifier.test(): Modifier {
                    return composed {
                        this@test
                    }
                }

                fun Modifier.test3(): Modifier = composed(factory = {
                    this@test3
                })

                fun Modifier.test4(): Modifier = composed({}, { this@test4})
            """
            ),
            composedStub,
            Stubs.Composable,
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test(): Modifier = composed {
                                                ~~~~~~~~
src/test/test.kt:13: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                    return composed {
                           ~~~~~~~~
src/test/test.kt:18: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test3(): Modifier = composed(factory = {
                                                 ~~~~~~~~
src/test/test.kt:22: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test4(): Modifier = composed({}, { this@test4})
                                                 ~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun composableCalls() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed
                import androidx.compose.runtime.*

                inline fun <T> scopingFunction(lambda: () -> T): T {
                    return lambda()
                }

                fun Modifier.test1(): Modifier = composed {
                    val foo = remember { true }
                    this@test1
                }

                @Composable
                fun composableFunction() {}

                fun Modifier.test2(): Modifier = composed {
                    composableFunction()
                    this@test2
                }

                fun Modifier.test3(): Modifier = composed {
                    scopingFunction {
                        val foo = remember { true }
                        this@test3
                    }
                }

                @Composable
                fun <T> composableScopingFunction(lambda: @Composable () -> T): T {
                    return lambda()
                }

                fun Modifier.test4(): Modifier = composed {
                    composableScopingFunction {
                        this@test4
                    }
                }

                val composableProperty: Boolean
                    @Composable
                    get() = true

                fun Modifier.test5(): Modifier = composed {
                    composableProperty
                    this@test5
                }

                // Test for https://youtrack.jetbrains.com/issue/KT-46795
                fun Modifier.test6(): Modifier = composed {
                    val nullable: Boolean? = null
                    val foo = nullable ?: false
                    val bar = remember { true }
                    this@test6
                }
            """
            ),
            composedStub,
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Remember
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
