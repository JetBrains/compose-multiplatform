/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material.studies.rally

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val rallyGreen = Color(0xFF1EB980)

@Composable
fun RallyTheme(content: @Composable () -> Unit) {
    val colors = darkColors(
        primary = Color.White,
        surface = Color(0xFF26282F),
        onSurface = Color.White,
        background = Color(0xFF26282F),
        onBackground = Color.White
    )
    // TODO: Bundle Roboto Condensed and Eczar font files.
    val typography = Typography(
        defaultFontFamily = FontFamily.Default,
        // Unused
        h1 = TextStyle(
            fontWeight = FontWeight.W100,
            fontSize = 96.sp
        ),
        h2 = TextStyle(
            fontWeight = FontWeight.W600,
            fontSize = 44.sp
        ),
        h3 = TextStyle(
            fontWeight = FontWeight.W400,
            fontSize = 14.sp
        ),
        // Unused
        h4 = TextStyle(
            fontWeight = FontWeight.W700,
            fontSize = 34.sp
        ),
        // Unused
        h5 = TextStyle(
            fontWeight = FontWeight.W700,
            fontSize = 24.sp
        ),
        // Eczar
        h6 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp
        ),
        subtitle1 = TextStyle(
            fontWeight = FontWeight.W300,
            fontSize = 14.sp
        ),
        subtitle2 = TextStyle(
            fontWeight = FontWeight.W400,
            fontSize = 14.sp
        ),
        body1 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        body2 = TextStyle(
            fontWeight = FontWeight.W200,
            fontSize = 14.sp
        ),
        button = TextStyle(
            fontWeight = FontWeight.W700,
            fontSize = 14.sp
        ),
        // Unused
        caption = TextStyle(
            fontWeight = FontWeight.W500,
            fontSize = 12.sp
        ),
        // Unused
        overline = TextStyle(
            fontWeight = FontWeight.W500,
            fontSize = 10.sp
        )
    )
    MaterialTheme(colors = colors, typography = typography, content = content)
}

@Composable
fun RallyDialogThemeOverlay(content: @Composable () -> Unit) {
    val dialogColors = darkColors(
        primary = Color.White,
        surface = Color(0xFF1E1E1E),
        onSurface = Color.White
    )
    val currentTypography = MaterialTheme.typography
    val dialogTypography = currentTypography.copy(
        body1 = currentTypography.body1.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp
        ),
        button = currentTypography.button.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.em
        )
    )
    MaterialTheme(colors = dialogColors, typography = dialogTypography, content = content)
}
