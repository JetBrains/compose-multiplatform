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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "LocalVariableName")

package androidx.compose.integration.docs.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.Typography
import androidx.compose.material.contentColorFor
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/themes
 *
 * No action required if it's modified.
 */

private object ThemingSnippet1 {
    /* Can't be compiled. See snippet below for changes.
    MaterialTheme(
    colors = ...,
    typography = ...,
    shapes = ...
    ) {
        // app content
    }
     */
    @Composable
    fun MaterialThemeSignature() {
        MaterialTheme(
            colors = MaterialTheme.colors,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) { }
    }
}

private object ThemingSnippet2 {
    val Red = Color(0xffff0000)
    val Blue = Color(red = 0f, green = 0f, blue = 1f)
}

private object ThemingSnippet3 {
    private val Yellow200 = Color(0xffffeb46)
    private val Blue200 = Color(0xff91a4fc)
    // ...

    private val DarkColors = darkColors(
        primary = Yellow200,
        secondary = Blue200,
        // ...
    )
    private val LightColors = lightColors(
        primary = Yellow500,
        primaryVariant = Yellow400,
        secondary = Blue700,
        // ...
    )
}

@Composable private fun ThemingSnippet4() {

    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors
    ) {
        // app content
    }
}

@Composable private fun ThemingSnippet5() {
    Text(
        text = "Hello theming",
        color = MaterialTheme.colors.primary
    )
}

@Composable private fun ThemingSnippet6() {
    /* This snippet comes from the API. It needs to be updated if the snippet below is modified:
Surface(
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    ...

TopAppBar(
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    ...
     */
    Column {
        Surface(
            color = MaterialTheme.colors.surface,
            contentColor = contentColorFor(MaterialTheme.colors.surface)
        ) {}
        TopAppBar(
            backgroundColor = MaterialTheme.colors.primarySurface,
            contentColor = contentColorFor(MaterialTheme.colors.primarySurface)
        ) {}
    }
}

@Composable private fun ThemingSnippet7() {
    // By default, both Icon & Text use the combination of LocalContentColor &
    // LocalContentAlpha. De-emphasize content by setting content alpha
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(/*...*/)
    }
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
        Icon(/*...*/)
        Text(/*...*/)
    }
}

private object ThemingSnippet8 {
    @Composable
    fun MyTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        MaterialTheme(
            colors = if (darkTheme) DarkColors else LightColors,
            /*...*/
            content = content
        )
    }
}

@Composable private fun ThemingSnippet9() {
    val isLightTheme = MaterialTheme.colors.isLight
}

@Composable private fun ThemingSnippet10() {
    Surface(
        elevation = 2.dp,
        color = MaterialTheme.colors.surface, // color will be adjusted for elevation
        /*...*/
    ) { /*...*/ }
}

private object ThemingSnippet11 {
    val Colors.snackbarAction: Color
        @Composable get() = if (isLight) Red300 else Red700
}

@Composable private fun ThemingSnippet12() {
    val Rubik = FontFamily(
        Font(R.font.rubik_regular),
        Font(R.font.rubik_medium, FontWeight.W500),
        Font(R.font.rubik_bold, FontWeight.Bold)
    )

    val MyTypography = Typography(
        h1 = TextStyle(
            fontFamily = Rubik,
            fontWeight = FontWeight.W300,
            fontSize = 96.sp
        ),
        body1 = TextStyle(
            fontFamily = Rubik,
            fontWeight = FontWeight.W600,
            fontSize = 16.sp
        )
        /*...*/
    )
    MaterialTheme(typography = MyTypography, /*...*/)
}

@Composable private fun ThemingSnippet13() {
    val typography = Typography(defaultFontFamily = Rubik)
    MaterialTheme(typography = typography, /*...*/)
}

@Composable private fun ThemingSnippet14() {
    Text(
        text = "Subtitle2 styled",
        style = MaterialTheme.typography.subtitle2
    )
}

@Composable private fun ThemingSnippet15() {
    val Shapes = Shapes(
        small = RoundedCornerShape(percent = 50),
        medium = RoundedCornerShape(0f),
        large = CutCornerShape(
            topStart = 16.dp,
            topEnd = 0.dp,
            bottomEnd = 0.dp,
            bottomStart = 16.dp
        )
    )

    MaterialTheme(shapes = Shapes, /*...*/)
}

@Composable private fun ThemingSnippet16() {
    Surface(
        shape = MaterialTheme.shapes.medium, /*...*/
    ) {
        /*...*/
    }
}

/* ktlint-disable indent */
private object ThemingSnippet17 {
    @Composable
    fun LoginButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary
            ),
            onClick = onClick,
            modifier = modifier,
            content = content
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private val Yellow500 = Color(0xffffeb46)
private val Yellow400 = Color(0xffffeb46)
private val Blue700 = Color(0xffffeb46)

private val darkTheme = true
private val DarkColors = darkColors()
private val LightColors = lightColors()

@Composable private fun Icon() { }
@Composable private fun Text() { }

private val Red300 = Color(0xffffeb46)
private val Red700 = Color(0xffffeb46)

@Suppress("ClassName")
private object R {
    object font {
        const val rubik_regular = 1
        const val rubik_medium = 1
        const val rubik_bold = 1
    }
}

private val Rubik = FontFamily()
private fun MaterialTheme(typography: Typography) { }
private fun MaterialTheme(shapes: Shapes) { }
