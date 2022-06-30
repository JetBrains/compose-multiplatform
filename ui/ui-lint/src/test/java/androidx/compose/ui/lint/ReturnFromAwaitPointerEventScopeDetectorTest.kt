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

@RunWith(JUnit4::class)
class ReturnFromAwaitPointerEventScopeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ReturnFromAwaitPointerEventScopeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ReturnFromAwaitPointerEventScopeDetector.ExitAwaitPointerEventScope)

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
        ForEachGestureStub,
        UiStubs.Alignment,
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
    fun awaitPointerEventScope_insideForEach_shouldNotWarn() {
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
    fun awaitPointerEventScope_otherMethodName_shouldNotWarn() {
        expectClean(
            """
                package test

                fun <T> awaitPointerEventScope(block: () -> T): T {
                    return block()
                }
                fun TestComposable() {
                   val result = awaitPointerEventScope {
                        "Result"
                    }
                    println(result)
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_assignedToVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        while (true) {
                            val assigned = awaitPointerEventScope {

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
                            val assigned = awaitPointerEventScope {
                                           ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_returnedFromMethod_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(): Any {
                    return awaitPointerEventScope {

                    }
                }
            """
            ),
            *stubs,
        )
            .run()
            .expect(
                """
src/test/test.kt:7: $WarningMessage
                    return awaitPointerEventScope {
                           ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_assignedFromLambdaMethod_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(): Boolean {
                    val result = run {
                        awaitPointerEventScope {
                            true
                        }
                    }

                    return result
                }

                private suspend fun PointerInputScope.doSomethingInInputScope(nullable: String?): Boolean {
                    val result = nullable?.let {
                        awaitPointerEventScope {
                            true
                        }
                    } ?: false

                    return result
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
src/test/test.kt:18: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_returnedFromLambdaMethod_shouldWarn() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(): Boolean {
                    return run {
                        awaitPointerEventScope {
                            true
                        }
                    }
                }

                private suspend fun PointerInputScope.doSomethingInInputScope(nullable: String?): Boolean {
                    return nullable?.let {
                        awaitPointerEventScope {
                            true
                        }
                    } ?: false
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
src/test/test.kt:16: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_notAssignedToVariable_shouldNotWarn() {
        expectClean(
            """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        while (true) {
                            awaitPointerEventScope {

                            }
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

    private val WarningMessage: String =
        "Warning: ${ReturnFromAwaitPointerEventScopeDetector.ErrorMessage} " +
            "[${ReturnFromAwaitPointerEventScopeDetector.IssueId}]"
}