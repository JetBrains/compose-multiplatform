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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Detector for checking if we have multiple awaitPointerEventScope calls within the same block,
 * which is generally discouraged if we want to guarantee not losing touch events.
 *
 * For each awaitPointerEventScope we'll move to the closest boundary block (method call) and
 * search for the repeated calls inside that block.
 */
@RunWith(JUnit4::class)
class MultipleAwaitPointerEventScopesDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = MultipleAwaitPointerEventScopesDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(MultipleAwaitPointerEventScopesDetector.MultipleAwaitPointerEventScopes)

    private val ForEachGestureStub: TestFile = compiledStub(
        filename = "ForEachGesture.kt",
        filepath = "androidx/compose/foundation/gestures",
        checksum = 0xf41a4b04,
        """
            package androidx.compose.foundation.gestures
            import androidx.compose.ui.input.pointer.PointerInputScope

            suspend fun PointerInputScope.forEachGesture(block: suspend PointerInputScope.() -> Unit) {
                block()
            }
            """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAH2NSwrCMBCGR0SErCToVrC4UsghtFakG0EvEJqxBmomJBOo
                tzfS7gQXw//6YABgCgCTfPNRQaDYamcCWdOrhl6eIqoHJWc0W3KqxcgpYJSr
                Muj2PKQSGRumULNcVBROunmOS26Wd+1/OLEXxb83nX5TYjk7UJ/ho9j8wMkq
                63xi5ck6xiDXtxQ9OmNdex2qy3evbJdtzQXs4AM20YY08QAAAA==
                """,
        """
                androidx/compose/foundation/gestures/ForEachGestureKt.class:
                H4sIAAAAAAAAALVVz08bRxT+Zm1ss0BiNpACaRwS3IYfIWtoadM6Qo2ISVZ1
                DIohVcWhGq8XZ7C9Y+3OWuSGcqjUf6OHnqOe0h4qRG/9o6q+sQ0BTIIUqZY8
                870337z55s2b2X/+/fMvAF/iW4YV7lcDKar7tiubLRl69q6M/CpXQvp2zQtV
                FHihvS6DAndfPuna36skGEN6j7e53eB+zd6o7HkueWMMV3bPcBl+mS32rREJ
                W/itSNktKXzlBfZmt3e0s+zKlpcv1qVqCN/eazft3ch3tSAS0kPLJ+OuDGSk
                hE8q16RPIOpoz88Vz8vLMzb+v2h5uPhRURcv2cLDE8K2L1R+Nb/Qv6XVy/LQ
                F+SivCBblEHN3vNUJeCCtsZ9Xyre3WYpajR4peERbeZDNKk0k1iZD2crCZMh
                Ify2rFN1PJjt19PvuUD0MIYxMoQhXGGY688BJTsgicINbecE6sJNM1yveWpt
                4/nG9pZTKvxU3i5vFkqPC48ZxmYvXMjCNROjGGMYOpXLJK4zpJxSeetRaa3A
                MHIm0cOYwOQgPsEUhc2qlyLMnr8YKx9VNQwDlYZ06wzTl10R0pvVkRuetk9N
                eN+dYRg9pjzzFKdXgJPPaLZj9Fww3SQZWF0Dg/z7QqMcoeoSQ3h4kDEPD0wj
                bZjGhNGBE11opHvG4cHUKuEpI8fmjZyxfDcdm5pJMStukWWZVqqDWG7ASljx
                CZZL5OJHvyaMVPLp0c/f/f2WHR5oM506em3ETSM1qZdeZqQN1rHw09vPHDsL
                +8qjCpD+8ejWq04qR88+bPfriiG+Jqt0OiNlxd36M97a0sXPcLVI+SpFzYoX
                9DxWUbq88YIHQts952BZ1HzePeAbzyNKbtNz/LYIBQ0/endf6M6dH93kAW96
                dNpnaGZZRoHrrQsdfbI350VfPCzBQBz6RzQMIEHWElkuYtDHNL4wf+8PXI3h
                x7cY/w3xNz/8jhtvaCCGZWoTYJtJmvIFYZNCac8YkvSRAKa7AfApbnYWGEcG
                t2gZjaZxm9gruiKI/VU3ElLUf03/azEyBjua3rUGHnTaHL6hfp28d0jvzA5i
                DrIOPqMWnzu4i1kHc5jfAQuxgHs7GAwxEGIxxM0QmRD3Q9zumHaIxH8TzBqW
                1AYAAA==
                """

    )

    private val stubs = arrayOf(
        Stubs.Composable,
        Stubs.Modifier,
        UiStubs.Density,
        UiStubs.PointerInputScope,
        UiStubs.PointerEvent,
        UiStubs.Alignment,
        ForEachGestureStub
    )

    @Test
    fun awaitPointerEventScope_standalone_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {

                        }
                    }
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScopeOtherMethodName_shouldNotWarn() {
        expectClean(
            """
                package test

                import androidx.compose.foundation.gestures.forEachGesture
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.AwaitPointerEventScope
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    fun awaitPointerEventScope(block: AwaitPointerEventScope.() -> Unit) {}
                    Modifier.pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_insideForEachGesture_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.foundation.gestures.forEachGesture
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
            """
        )
    }

    @Test
    fun singleAwaitPointerEventScopeFromMethod_shouldNotWarn() {
        expectClean(
            """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                suspend fun PointerInputScope.TestComposable() {
                    awaitPointerEventScope {

                    }
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_withConditionalCalls_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable(condition: Boolean) {
                    Modifier.pointerInput(Unit) {
                        if (condition) {
                            awaitPointerEventScope {

                            }
                        } else {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:12: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """.trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_fromExtensionMethodAndConditionalCalls_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(
                    condition: Boolean
                ) {
                    if (condition) {
                        awaitPointerEventScope {

                        }
                    } else {
                        awaitPointerEventScope {

                        }
                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:10: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:14: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """.trimIndent()
            )
    }

    @Test
    fun multipleAwaitPointerEventScope_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test
                import androidx.compose.foundation.gestures.forEachGesture
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {

                            }

                            awaitPointerEventScope {

                            }
                        }
                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:12: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """.trimIndent()
            )
    }

    @Test
    fun multipleAwaitPointerEventScope_withLambdaBlocks_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test
                import androidx.compose.foundation.gestures.forEachGesture
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        forEachGesture {
                            run { awaitPointerEventScope {

                            }}

                            run { awaitPointerEventScope {

                            }}
                        }
                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:12: $WarningMessage
                            run { awaitPointerEventScope {
                                  ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: $WarningMessage
                            run { awaitPointerEventScope {
                                  ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """.trimIndent()
            )
    }

    @Test
    fun multipleAwaitPointerEventScope_insideExtensionMethod_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope() {

                    awaitPointerEventScope {

                    }

                    awaitPointerEventScope {

                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:8: $WarningMessage
                    awaitPointerEventScope {
                    ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:12: $WarningMessage
                    awaitPointerEventScope {
                    ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_multipleConditionalAndCalls_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable(condition: Boolean) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {

                        }

                        if (condition) {
                            awaitPointerEventScope {

                            }
                        } else {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:10: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:15: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:19: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
            """.trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_multipleConditionalAndCallsInsideCondition_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable(condition: Boolean) {

                    Modifier.pointerInput(Unit) {
                        if (condition) {
                            awaitPointerEventScope {

                            }

                            awaitPointerEventScope {

                            }
                        } else {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:13: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:17: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:21: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
            """.trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_repetitionWithinCustomModifier_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.PointerInputScope
                import androidx.compose.ui.input.pointer.pointerInput

                fun Modifier.myCustomPointerInput(block: suspend PointerInputScope.() -> Unit) =
                    pointerInput(Unit, block)

                @Composable
                fun TestComposable() {
                    Modifier
                        .myCustomPointerInput {
                            awaitPointerEventScope {

                            }
                            awaitPointerEventScope {

                            }
                        }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:19: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """.trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_nestedBlocks_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.PointerInputScope
                import androidx.compose.ui.input.pointer.pointerInput

                fun Modifier.myCustomPointerInput(block: suspend PointerInputScope.() -> Unit) =
                    pointerInput(Unit, block)

                var condition = false

                enum class Options { A, B, C }

                val options = Options.A

                @Composable
                fun TestComposable() {
                    Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {

                            }

                            if (condition) {
                                try {
                                    when (options) {
                                        Options.A -> {
                                            // do something
                                        }
                                        Options.B -> {
                                            awaitPointerEventScope {

                                            }
                                        }
                                        Options.C -> {
                                            // do something
                                        }
                                    }
                                } catch (e: Exception) {
                                    // do something
                                }
                            }
                        }
                }
                 """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/Options.kt:22: $WarningMessage
                            awaitPointerEventScope {
                            ~~~~~~~~~~~~~~~~~~~~~~
src/test/Options.kt:33: $WarningMessage
                                            awaitPointerEventScope {
                                            ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """.trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_repetitionAcrossCustomModifiers_shouldNotWarn() {
        expectClean(
            """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.PointerInputScope
                import androidx.compose.ui.input.pointer.pointerInput

                fun Modifier.myCustomPointerInput(block: suspend PointerInputScope.() -> Unit) =
                    pointerInput(Unit, block)

                @Composable
                fun TestComposable() {
                    Modifier
                        .myCustomPointerInput {
                            awaitPointerEventScope {

                            }
                        }
                        .myCustomPointerInput {
                            awaitPointerEventScope {

                            }
                        }
                }
                 """
        )
    }

    @Test
    fun awaitPointerEventScope_repetitionAcrossPointerInputModifiers_shouldNotWarn() {
        expectClean(
            """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.PointerInputScope
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {

                            }
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {

                            }
                        }
                }
                 """
        )
    }

    private fun expectClean(source: String) {
        lint()
            .files(kotlin(source), *stubs)
            .run()
            .expectClean()
    }

    private val WarningMessage
        get() = "Warning: ${MultipleAwaitPointerEventScopesDetector.ErrorMessage} " +
            "[${MultipleAwaitPointerEventScopesDetector.IssueId}]"
}