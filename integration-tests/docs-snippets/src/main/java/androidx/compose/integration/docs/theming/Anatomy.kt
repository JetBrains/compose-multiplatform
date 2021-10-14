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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/themes/anatomy
 *
 * No action required if it's modified.
 */

private object Anatomy {
    // Start snippet 1
    @Immutable
    data class ColorSystem(
        val color: Color,
        val gradient: List<Color>
        /* ... */
    )

    @Immutable
    data class TypographySystem(
        val fontFamily: FontFamily,
        val textStyle: TextStyle
    )
    /* ... */

    @Immutable
    data class CustomSystem(
        val value1: Int,
        val value2: String
        /* ... */
    )

    /* ... */
    // End snippet 1

    // Start snippet 2
    val LocalColorSystem = staticCompositionLocalOf {
        ColorSystem(
            color = Color.Unspecified,
            gradient = emptyList()
        )
    }

    val LocalTypographySystem = staticCompositionLocalOf {
        TypographySystem(
            fontFamily = FontFamily.Default,
            textStyle = TextStyle.Default
        )
    }

    val LocalCustomSystem = staticCompositionLocalOf {
        CustomSystem(
            value1 = 0,
            value2 = ""
        )
    }

    /* ... */
    // End snippet 2

    // Start snippet 3
    @Composable
    fun Theme(
        /* ... */
        content: @Composable () -> Unit
    ) {
        val colorSystem = ColorSystem(
            color = Color(0xFF3DDC84),
            gradient = listOf(Color.White, Color(0xFFD7EFFF))
        )
        val typographySystem = TypographySystem(
            fontFamily = FontFamily.Monospace,
            textStyle = TextStyle(fontSize = 18.sp)
        )
        val customSystem = CustomSystem(
            value1 = 1000,
            value2 = "Custom system"
        )
        /* ... */
        CompositionLocalProvider(
            LocalColorSystem provides colorSystem,
            LocalTypographySystem provides typographySystem,
            LocalCustomSystem provides customSystem,
            /* ... */
            content = content
        )
    }
    // End snippet 3

    // Start snippet 4
    // Use with eg. Theme.colorSystem.color
    object Theme {
        val colorSystem: ColorSystem
            @Composable
            get() = LocalColorSystem.current
        val typographySystem: TypographySystem
            @Composable
            get() = LocalTypographySystem.current
        val customSystem: CustomSystem
            @Composable
            get() = LocalCustomSystem.current
        /* ... */
    }
    // End snippet 4
}
