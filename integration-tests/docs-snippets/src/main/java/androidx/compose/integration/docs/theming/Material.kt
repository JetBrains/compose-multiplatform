// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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

// Ignore lint warnings in documentation snippets
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "RemoveEmptyParenthesesFromLambdaCall"
)

package androidx.compose.integration.docs.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalElevationOverlay
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
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/themes/material
 *
 * No action required if it's modified.
 */

private object MaterialSnippet1 {
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
    fun MaterialTheming() {
        MaterialTheme(
            colors = MaterialTheme.colors,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) { }
    }
}

private object MaterialSnippet2 {
    val Red = Color(0xffff0000)
    val Blue = Color(red = 0f, green = 0f, blue = 1f)
}

private object MaterialSnippet3 {
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

private object MaterialSnippet4 {
    @Composable
    fun MaterialTheming() {
        MaterialTheme(
            colors = if (darkTheme) DarkColors else LightColors
        ) {
            // app content
        }
    }
}

private object MaterialSnippet5 {
    @Composable
    fun MaterialTheming() {
        Text(
            text = "Hello theming",
            color = MaterialTheme.colors.primary
        )
    }
}

private object MaterialSnippet6 {
    @Composable
    fun MaterialTheming() {
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
}

private object MaterialSnippet7 {
    @Composable
    fun MaterialTheming() {
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
    @Composable private fun Icon() { }
    @Composable private fun Text() { }
}

private object MaterialSnippet8 {
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

private object MaterialSnippet9 {
    @Composable
    fun MaterialTheming() {
        val isLightTheme = MaterialTheme.colors.isLight
        Icon(
            painterResource(
                id = if (isLightTheme) {
                    R.drawable.ic_sun_24dp
                } else {
                    R.drawable.ic_moon_24dp
                }
            ),
            contentDescription = "Theme"
        )
    }
}

private object MaterialSnippet10 {
    @Composable
    fun MaterialTheming() {
        Surface(
            elevation = 2.dp,
            color = MaterialTheme.colors.surface, // color will be adjusted for elevation
            /*...*/
        ) { /*...*/ }
    }
}

private object MaterialSnippet11 {
    @Composable
    fun MaterialTheming() {
        // Elevation overlays
        // Implemented in Surface (and any components that use it)
        val color = MaterialTheme.colors.surface
        val elevation = 4.dp
        val overlaidColor = LocalElevationOverlay.current?.apply(
            color, elevation
        )
    }
}

private object MaterialSnippet12 {
    @Composable
    fun MaterialTheming() {
        MyTheme {
            CompositionLocalProvider(LocalElevationOverlay provides null) {
                // Content without elevation overlays
            }
        }
    }
}

private object MaterialSnippet13 {
    @Composable
    fun MaterialTheming() {
        Surface(
            // Switches between primary in light theme and surface in dark theme
            color = MaterialTheme.colors.primarySurface,
            /*...*/
        ) { /*...*/ }
    }
}

private object MaterialSnippet14 {
    @Composable
    fun MaterialTheming() {
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
    @Composable private fun MaterialTheme(typography: Typography) { }
}

private object MaterialSnippet15 {
    @Composable
    fun MaterialTheming() {
        val typography = Typography(defaultFontFamily = Rubik)
        MaterialTheme(typography = typography, /*...*/)
    }
    @Composable private fun MaterialTheme(typography: Typography) { }
}

private object MaterialSnippet16 {
    @Composable
    fun MaterialTheming() {
        Text(
            text = "Subtitle2 styled",
            style = MaterialTheme.typography.subtitle2
        )
    }
}

private object MaterialSnippet17 {
    @Composable
    fun MaterialTheming() {
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
    @Composable private fun MaterialTheme(shapes: Shapes) { }
}

private object MaterialSnippet18 {
    @Composable
    fun MaterialTheming() {
        Surface(
            shape = MaterialTheme.shapes.medium, /*...*/
        ) {
            /*...*/
        }
    }
}

private object MaterialSnippet19 {
    @Composable
    fun MyButton(
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

private object MaterialSnippet20 {
    @Composable
    fun DetailsScreen(/* ... */) {
        PinkTheme {
            // other content
            RelatedSection()
        }
    }

    @Composable
    fun RelatedSection(/* ... */) {
        BlueTheme {
            // content
        }
    }
}

private object MaterialSnippet21 {
    @Composable
    fun MaterialTheming() {
        Button(
            onClick = { /* ... */ },
            enabled = true,
            // Custom colors for different states
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                disabledBackgroundColor = MaterialTheme.colors.onBackground
                    .copy(alpha = 0.2f)
                    .compositeOver(MaterialTheme.colors.background)
                // Also contentColor and disabledContentColor
            ),
            // Custom elevation for different states
            elevation = ButtonDefaults.elevation(
                defaultElevation = 8.dp,
                disabledElevation = 2.dp,
                // Also pressedElevation
            )
        ) { /* ... */ }
    }
}

private object MaterialSnippet22 {
    @Composable
    fun MyApp() {
        MaterialTheme {
            CompositionLocalProvider(
                LocalRippleTheme provides SecondaryRippleTheme
            ) {
                // App content
            }
        }
    }

    @Immutable
    private object SecondaryRippleTheme : RippleTheme {
        @Composable
        override fun defaultColor() = RippleTheme.defaultRippleColor(
            contentColor = MaterialTheme.colors.secondary,
            lightTheme = MaterialTheme.colors.isLight
        )

        @Composable
        override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
            contentColor = MaterialTheme.colors.secondary,
            lightTheme = MaterialTheme.colors.isLight
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private val Yellow500 = Color(0xffffeb46)
private val Yellow400 = Color(0xffffeb46)
private val Blue700 = Color(0xffffeb46)

private const val darkTheme = true
private val DarkColors = darkColors()
private val LightColors = lightColors()

@Composable private fun MyTheme(content: @Composable () -> Unit) {}
@Composable private fun PinkTheme(content: @Composable () -> Unit) {}
@Composable private fun BlueTheme(content: @Composable () -> Unit) {}

@Suppress("ClassName")
private object R {
    object drawable {
        const val ic_sun_24dp = 1
        const val ic_moon_24dp = 1
    }
    object font {
        const val rubik_regular = 1
        const val rubik_medium = 1
        const val rubik_bold = 1
    }
}

private val Rubik = FontFamily()
