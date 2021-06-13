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
 * Test for [CrossfadeDetector].
 */
class CrossfadeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CrossfadeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CrossfadeDetector.UnusedCrossfadeTargetStateParameter)

    // Simplified Transition.kt stubs
    private val CrossfadeStub = compiledStub(
        filename = "Transition.kt",
        filepath = "androidx/compose/animation",
        """
            package androidx.compose.animation

            import androidx.compose.runtime.Composable

            @Composable
            fun <T> Crossfade(
                targetState: T,
                content: @Composable (T) -> Unit
            ) {}
        """,
"""
        androidx/compose/animation/TransitionKt.class:
        H4sIAAAAAAAAAIVSW08TQRT+ZnvZdgFZyr0oIhcBUbcQfLHExJAQGisYW3nh
        abpd6vQya3amDY/9Lf4D34wPpvHRH2U8s1RBasIme86Zb77znTNn5uevb98B
        7OMpwyaX9SgU9UvPDzufQhV4XIoO1yKUXjXiUgkTvtE2GIPb5D3utblseKe1
        ZuATmmDIHkahUhe8HjC82Crf5hTLrVC3hfSavY530ZW+EVTe0TDaLW6fMZwe
        VF+OZr7aqlbvSj94doPzQQrKihU3yiMni7pSi07gHcZrXmsHRYa1chg1vGag
        axEXJMylDDW/KnIS6pNuu00s2w+lDqTOwGFYvtGRIDiSvO2VpI4oX/jKxjjD
        rP8x8FtDgXc84p2AiDTv/w3oGqkYkQYdYBz3MOlgAi7DmOZRI9AVaotGnBsV
        YFi5a8gMU38obwPN61xzwqxOL0EvgRmTYmAtE1iEXwoTFSiq7zJUBv0ZZ9B3
        LNdyrEzCsRas+B/08/suGavAVpOZQd+19tJuIm8dW3vzbjI/nUvmKDa2wAqp
        H5/TViZ9bBvv2kZ6j1FtsKo51LC5mx1PXD/A5y3NkDwMzRubLAsZnHQ7tSCq
        mks02aHP22c8EmY9BLMV0ZBcdyOKl95fXX1J9oQStP36+pYZ1m/v/r2vf2hO
        JexGfnAkjPriMOdsRA+7sJCE+YiGFNK0WqdVkXCLvL2TG/uKqS9m1NggmyZi
        Gg4eUzx3RUEO07GETfgM7W/GbBtbQ36G/Db9WSuuk423n8R2DTvkDwmdpepz
        50iUMF/CAlkslpDHUgn38eAcTGEZD8+RUUgprCg8UsgpOAqryoDp38bsatcp
        BAAA
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcUllZiXUpSfmVKhl5yfW5BfnKqX
        mJeZm1iSmZ8nxBNSlJhXnAlie5dw8XIxp+XnC7GFpBaXeJcoMWgxAACekN3e
        UwAAAA==
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
            Stubs.Composable
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
            Stubs.Composable
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
            Stubs.Composable
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
