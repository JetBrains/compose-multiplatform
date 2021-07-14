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

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.kotlinAndCompiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ColorsDetector].
 *
 * Tests for when Colors.kt is available as source (during global / IDE analysis), and for when
 * it is available as bytecode (during partial / CLI analysis). Since we cannot resolve default
 * values when it is only available as bytecode, is it expected that we throw less errors in that
 * mode.
 */
class ColorsDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ColorsDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(ColorsDetector.ConflictingOnColor)

    // Simplified Colors.kt stubs
    private val ColorsStub = kotlinAndCompiledStub(
        filename = "Colors.kt",
        filepath = "androidx/compose/material",
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
        """,
        """
        androidx/compose/material/Colors.class:
        H4sIAAAAAAAAAJVUTW8TSRB9Pf6YeGzHdownNmGWrwBJgExA3LJCgrArBZnd
        1QblQA6oMx6Sju3uqHscwS3aH7BnzvwDTlntYRXBAYkfhahpTwYQhxUzo3qv
        uqq763W1/fHTv/8BuIe7DJe4HGglBi/DSI0PlYnDMU9iLfgo3FAjpY0LxtA8
        4Ec8HHG5F/6+exBHiYsCQ/lnIUVyn8JLj796ni1vMxSWlrdrKMH1UMQMQzHZ
        F4bhSv//9ltncA+1GHP9ioE9ZpjNvG1OWTJhqJg4UnJgE5o5z8PeLo+Ge1pN
        5ICWMhP9gkcxQynWWmmareQfZ8tXldz6slZNyYdfTaXErbPJrpK/TKe7wvTF
        3j7tw54x3P9WeH+okpGQ4cHROBSSZEmS9Sh+wSejZENJk+hJlCj9hOthrNen
        B1T24OACQ3sx+pLxfGxTGFZ/bEmG1tmEJ3HCBzzhNOaMjwrUb5aaEhU+pKGX
        IvXWiA3uMLw5Pe56TtfxnObpsUef5TM18qvkdk+P7zpr7OHhXLnpnHfWCoTF
        DEsZljN0M5zJsJKhl2E1w1qG9QxnM2wQNt//w06P370pO83Wu7+cIpXSSyul
        +wqGxe/v0ESEe5of7ovITK8Rqa5M79PqkHpV3FADamOjL2T822S8G+unfHdE
        I3N9FfGRvTvkZ4PelproKP5VpE7vz4lMxDjeFkZQ9IGUKuGJoKPHHepcEenj
        0EutpOJuk/eW8Hx61isnqLy14VWyaTi1Idm/MZsmwEPVLlBCDXUbL1GkDtsq
        NIiVLGsScy1rEatYNkesalmbWN2yc4QNyzrEWpb5xNqWzRPrWNYlNm9Zj0rt
        Nen3gYWs/A80skAYrFy87Jd816/4Vb/uN/yW3/Y7/nynd4IglcWslNd2wyCX
        EuRSglxKkEsJcilBLiXIpQS5lCCXEuRSglxKkEsJcinBVIplP+EicQdr9vBv
        UacATjVdohou76CwiSubuEoWi6m5tonruLEDZrCE5R3MGVQNVgxqBjcNZg0a
        hv5q0DJpqG1wzqBj4BvMG3QNejZ54TP/Je0LWAUAAA==
        """,
        """
        androidx/compose/material/ColorsKt.class:
        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
        RMpEg4lGE70mgibGTAT+BZIgXWjiCwAA
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcolmZiXUpSfmVKhl5yfW5BfnKqX
        m1iSWpSZmCPE4Zyfk19U7F3Cpc4li1OZXlp+vhBbSGpxCVihDIbC0ky99KLE
        gozM5GIhdrCR3iVKDFoMAMec7K6RAAAA
        """
    )

    @Test
    fun constructorErrors_source() {
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
            Stubs.Color,
            ColorsStub.kotlin
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
    fun lightColorsErrors_source() {
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
            Stubs.Color,
            ColorsStub.kotlin
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
    fun darkColorsErrors_source() {
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
            Stubs.Color,
            ColorsStub.kotlin
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
    fun trackVariableAssignment_source() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val testColor1 = Color.Black

                fun test() {
                    val colors = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor1,
                        onBackground = Color.Black,
                    )

                    val testColor2 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor2,
                        onBackground = Color.Black,
                    )

                    var testColor3 = Color.Green
                    testColor3 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor3,
                        onBackground = Color.Black,
                    )
                }
            """
            ),
            Stubs.Color,
            ColorsStub.kotlin
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors_source() {
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
            Stubs.Color,
            ColorsStub.kotlin
        )
            .run()
            .expectClean()
    }

    @Test
    fun constructorErrors_compiled() {
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
            Stubs.Color,
            ColorsStub.compiled
        )
            .run()
            // TODO: b/184856104 currently the constructor call to Colors cannot be resolved when
            // it is available as bytecode, so we don't see any errors.
            .expectClean()
    }

    @Ignore // b/193270279
    @Test
    fun lightColorsErrors_compiled() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors = lightColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
            ),
            Stubs.Color,
            ColorsStub.compiled
        )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:14: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Ignore // b/193270279
    @Test
    fun darkColorsErrors_compiled() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors = darkColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
            ),
            Stubs.Color,
            ColorsStub.compiled
        )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:14: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun trackVariableAssignment_compiled() {
        lint().files(
            kotlin(
                """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val testColor1 = Color.Black

                fun test() {
                    val colors = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor1,
                        onBackground = Color.Black,
                    )

                    val testColor2 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor2,
                        onBackground = Color.Black,
                    )

                    var testColor3 = Color.Green
                    testColor3 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor3,
                        onBackground = Color.Black,
                    )
                }
            """
            ),
            Stubs.Color,
            ColorsStub.compiled
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors_compiled() {
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
            Stubs.Color,
            ColorsStub.compiled
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
