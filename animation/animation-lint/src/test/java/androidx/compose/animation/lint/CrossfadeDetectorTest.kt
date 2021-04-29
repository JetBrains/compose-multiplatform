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

package androidx.compose.animation.lint

import androidx.compose.lint.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

// TODO: add tests for methods defined in class files when we update Lint to support bytecode()
//  test files

/**
 * Test for [CrossfadeDetector].
 */
class CrossfadeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CrossfadeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CrossfadeDetector.UnusedCrossfadeTargetStateParameter)

    // Simplified Transition.kt stubs
    private val CrossfadeStub = kotlin(
        """
            package androidx.compose.animation

            import androidx.compose.runtime.Composable

            @Composable
            fun <T> Crossfade(
                targetState: T,
                content: @Composable (T) -> Unit
            ) {}
        """
    )

    @Test
    fun unreferencedParameters() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    Crossfade(foo) { if (foo) { /**/ } else { /**/ } }
                    Crossfade(foo, content = { if (foo) { /**/ } else { /**/ } })
                    Crossfade(foo) { param -> if (foo) { /**/ } else { /**/ } }
                    Crossfade(foo, content = { param -> if (foo) { /**/ } else { /**/ } })
                    Crossfade(foo) { _ -> if (foo) { /**/ } else { /**/ } }
                    Crossfade(foo, content = { _ -> if (foo) { /**/ } else { /**/ } })
                }
            """
            ),
            CrossfadeStub,
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/foo/test.kt:11: Error: Target state parameter it is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { if (foo) { /**/ } else { /**/ } }
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:12: Error: Target state parameter it is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo, content = { if (foo) { /**/ } else { /**/ } })
                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:13: Error: Target state parameter param is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { param -> if (foo) { /**/ } else { /**/ } }
                                     ~~~~~
src/foo/test.kt:14: Error: Target state parameter param is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo, content = { param -> if (foo) { /**/ } else { /**/ } })
                                               ~~~~~
src/foo/test.kt:15: Error: Target state parameter _ is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { _ -> if (foo) { /**/ } else { /**/ } }
                                     ~
src/foo/test.kt:16: Error: Target state parameter _ is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo, content = { _ -> if (foo) { /**/ } else { /**/ } })
                                               ~
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

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    Crossfade(foo) {
                        foo.let {
                            // These `it`s refer to the `let`, not the `Crossfade`, so we
                            // should still report an error
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }
                    Crossfade(foo) { param ->
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `Crossfade`, so we
                            // should still report an error
                            if (param) { /**/ } else { /**/ }
                        }
                    }
                }
            """
            ),
            CrossfadeStub,
            kotlin(Stubs.Composable)
        )
            .run()
            .expect(
                """
src/foo/test.kt:11: Error: Target state parameter it is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) {
                                   ^
src/foo/test.kt:20: Error: Target state parameter param is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { param ->
                                     ~~~~~
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

            import androidx.compose.animation.*
            import androidx.compose.runtime.*

            val foo = false

            @Composable
            fun Test() {
                Crossfade(foo) { if (it) { /**/ } else { /**/ } }
                Crossfade(foo, content = { if (it) { /**/ } else { /**/ } })
                Crossfade(foo) { param -> if (param) { /**/ } else { /**/ } }
                Crossfade(foo, content = { param -> if (param) { /**/ } else { /**/ } })

                val content : @Composable (Boolean) -> Unit = {}
                Crossfade(foo, content = content)

                Crossfade(foo) { param ->
                    foo.let {
                        it.let {
                            if (param && it) { /**/ } else { /**/ }
                        }
                    }
                }

                Crossfade(foo) {
                    foo.let { param ->
                        it.let { param ->
                            if (param && it) { /**/ } else { /**/ }
                        }
                    }
                }

                Crossfade(foo) {
                    foo.run {
                        run {
                            if (this && it) { /**/ } else { /**/ }
                        }
                    }
                }

                fun multipleParameterLambda(lambda: (Boolean, Boolean) -> Unit) {}

                Crossfade(foo) {
                    multipleParameterLambda { _, _ ->
                        multipleParameterLambda { param1, _ ->
                            if (param1 && it) { /**/ } else { /**/ }
                        }
                    }
                }
            }
        """
            ),
            CrossfadeStub,
            kotlin(Stubs.Composable)
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
