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

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/themes/material#material3
 *
 * No action required if it's modified.
 */

private object Material3Snippet1 {
    /* Can't be compiled. See snippet below for changes.
      MaterialTheme(
        colorScheme = …,
        typography = …
        // Updates to shapes coming soon
      ) {
        // M3 app content
      }
    */
    @Composable
    fun MaterialTheming() {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = MaterialTheme.typography
        ) { }
    }
}

private object Material3Snippet2 {
    private val Blue40 = Color(0xff1e40ff)
    private val DarkBlue40 = Color(0xff3e41f4)
    private val Yellow40 = Color(0xff7d5700)
    // Remaining colors from tonal palettes

    private val LightColorScheme = lightColorScheme(
        primary = Blue40,
        secondary = DarkBlue40,
        tertiary = Yellow40,
        // error, primaryContainer, onSecondary, etc.
    )
    private val DarkColorScheme = darkColorScheme(
        primary = Blue80,
        secondary = DarkBlue80,
        tertiary = Yellow80,
        // error, primaryContainer, onSecondary, etc.
    )
}

private object Material3Snippet3 {
    @Composable
    fun Material3Theming() {
        val darkTheme = isSystemInDarkTheme()
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
        ) {
            // M3 app content
        }
    }
}

private object Material3Snippet4 {
    @SuppressLint("NewApi")
    @Composable
    fun Material3Theming() {
        // Dynamic color is available on Android 12+
        val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val colorScheme = when {
            dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
            dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }
}

private object Material3Snippet5 {
    @Composable
    fun Material3Theming() {
        Text(
            text = "Hello M3 theming",
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

private object Material3Snippet6 {
    val KarlaFontFamily = FontFamily(
        Font(R.font.karla_regular),
        Font(R.font.karla_bold, FontWeight.Bold)
    )

    val AppTypography = Typography(
        bodyLarge = TextStyle(
            fontFamily = KarlaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        // titleMedium, labelSmall, etc.
    )
}

private object Material3Snippet7 {
    @Composable
    fun Material3Theming() {
        MaterialTheme(
            typography = AppTypography
        ) {
            // M3 app content
        }
    }
}

private object Material3Snippet8 {
    @Composable
    fun Material3Theming() {
        Text(
            text = "Hello M3 theming",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private object Material3Snippet9 {
    @Composable
    fun Material3Theming() {
        Surface(
            tonalElevation = 16.dp,
            shadowElevation = 16.dp
        ) {
            // Surface content
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private val Blue80 = Color(0xff1e40ff)
private val DarkBlue80 = Color(0xff3e41f4)
private val Yellow80 = Color(0xff7d5700)

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

private const val darkTheme = true

private val AppTypography = Typography()
