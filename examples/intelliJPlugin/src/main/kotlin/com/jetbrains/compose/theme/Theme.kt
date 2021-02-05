package com.jetbrains.compose.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jetbrains.compose.theme.intellij.SwingColor

private val DarkGreenColorPalette = darkColors(
    primary = green200,
    primaryVariant = green700,
    secondary = teal200,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    error = Color.Red,
)

private val LightGreenColorPalette = lightColors(
    primary = green500,
    primaryVariant = green700,
    secondary = teal200,
    onPrimary = Color.White,
    onSurface = Color.Black
)

@Composable
fun WidgetTheme(
    darkTheme: Boolean = false,
    content: @Composable() () -> Unit,
) {
    val colors = if (darkTheme) DarkGreenColorPalette else LightGreenColorPalette

    MaterialTheme(
        colors = colors.copy(
            background = SwingColor.background,
            onBackground = SwingColor.onBackground,
            surface = SwingColor.background,
            onSurface = SwingColor.onBackground,
        ),
        typography = typography,
        shapes = shapes,
        content = content
    )
}