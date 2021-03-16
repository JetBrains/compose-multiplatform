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

package androidx.compose.material.lint

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
 * Test for [ColorsDetector].
 */
class ColorsDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ColorsDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(ColorsDetector.ConflictingOnColor)

    // Simplified Color.kt stubs
    private val ColorStub = kotlin(Stubs.Color)

    // Simplified Colors.kt stubs
    private val ColorsStub = kotlin(
        """
            package androidx.compose.material

            import androidx.compose.ui.graphics.Color

            class Colors(
                primary: Color,
                primaryVariant: Color,
                secondary: Color,
                secondaryVariant: Color,
                background: Color,
                surface: Color,
                error: Color,
                onPrimary: Color,
                onSecondary: Color,
                onBackground: Color,
                onSurface: Color,
                onError: Color,
                isLight: Boolean
            )

            fun lightColors(
                primary: Color = Color(0xFF6200EE),
                primaryVariant: Color = Color(0xFF3700B3),
                secondary: Color = Color(0xFF03DAC6),
                secondaryVariant: Color = Color(0xFF018786),
                background: Color = Color.White,
                surface: Color = Color.White,
                error: Color = Color(0xFFB00020),
                onPrimary: Color = Color.White,
                onSecondary: Color = Color.Black,
                onBackground: Color = Color.Black,
                onSurface: Color = Color.Black,
                onError: Color = Color.White
            ): Colors = Colors(
                primary,
                primaryVariant,
                secondary,
                secondaryVariant,
                background,
                surface,
                error,
                onPrimary,
                onSecondary,
                onBackground,
                onSurface,
                onError,
                true
            )

            fun darkColors(
                primary: Color = Color(0xFFBB86FC),
                primaryVariant: Color = Color(0xFF3700B3),
                secondary: Color = Color(0xFF03DAC6),
                secondaryVariant: Color = secondary,
                background: Color = Color(0xFF121212),
                surface: Color = Color(0xFF121212),
                error: Color = Color(0xFFCF6679),
                onPrimary: Color = Color.Black,
                onSecondary: Color = Color.Black,
                onBackground: Color = Color.White,
                onSurface: Color = Color.White,
                onError: Color = Color.Black
            ): Colors = Colors(
                primary,
                primaryVariant,
                secondary,
                secondaryVariant,
                background,
                surface,
                error,
                onPrimary,
                onSecondary,
                onBackground,
                onSurface,
                onError,
                false
            )
        """
    )

    @Test
    fun constructorErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = Colors(
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.Red,
                    false
                )

                val colors2 = Colors(
                    primary = Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    background = Color.Blue,
                    Color.White,
                    Color.Green,
                    Color.White,
                    Color.Blue,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onError = Color.Red,
                    isLight = false
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors3 = Colors(
                    yellow200,
                    yellow400,
                    yellow200,
                    secondaryVariant = yellow200,
                    Color.White,
                    surface = Color.Blue,
                    Color.White,
                    Color.White,
                    yellow400,
                    Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                    yellow500,
                    false
                )
            """
            ),
            ColorStub,
            ColorsStub
        )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:16: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:17: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:18: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:19: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Red,
                    ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:31: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:32: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Blue,
                    ~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:34: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onSurface = Color.White,
                                ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:51: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:52: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    yellow400,
                    ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:53: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Blue,
                    ~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:55: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    yellow500,
                    ~~~~~~~~~
12 errors, 0 warnings
            """
            )
    }

    @Test
    fun lightColorsErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = lightColors(
                    // Color.White is used by default for some colors, so onPrimary should conflict
                    primary = Color.White,
                    onPrimary = Color.Red,
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors2 = lightColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
            ),
            ColorStub,
            ColorsStub
        )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:10: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = Color.Red,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:20: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:21: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
3 errors, 0 warnings
            """
            )
    }

    @Test
    fun darkColorsErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = darkColors(
                    // Color(0xFF121212) is used by default for some colors, so onPrimary should
                    // conflict
                    primary = Color(0xFF121212),
                    onPrimary = Color.Red,
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors2 = darkColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
            ),
            ColorStub,
            ColorsStub
        )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:11: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = Color.Red,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:21: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:22: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
3 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = lightColors()
                val colors2 = darkColors()
                val colors3 = Colors(
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    false
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors4 = Colors(
                    yellow200,
                    yellow400,
                    Color.White,
                    secondaryVariant = yellow500,
                    Color.White,
                    surface = Color.Blue,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    onSurface = Color(0xFFFFBBCC),
                    Color.White,
                    false
                )

                val colors5 = lightColors(
                    yellow200,
                    yellow400,
                    Color.White,
                    surface = Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                )

                val colors6 = darkColors(
                    yellow200,
                    yellow400,
                    Color.White,
                    surface = Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                )

            """
            ),
            ColorStub,
            ColorsStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
