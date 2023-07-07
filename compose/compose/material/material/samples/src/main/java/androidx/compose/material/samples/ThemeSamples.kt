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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun MaterialThemeSample() {
    val lightColors = lightColors(
        primary = Color(0xFF1EB980)
    )

    val darkColors = darkColors(
        primary = Color(0xFF66ffc7)
    )

    val colors = if (isSystemInDarkTheme()) darkColors else lightColors

    val typography = Typography(
        h1 = TextStyle(
            fontWeight = FontWeight.W100,
            fontSize = 96.sp
        ),
        button = TextStyle(
            fontWeight = FontWeight.W600,
            fontSize = 14.sp
        )
    )

    MaterialTheme(colors = colors, typography = typography) {
        val currentTheme = if (MaterialTheme.colors.isLight) "light" else "dark"
        ExtendedFloatingActionButton(
            text = { Text("FAB with text style and color from $currentTheme theme") },
            onClick = {}
        )
    }
}

@Sampled
@Composable
fun ThemeColorSample() {
    val colors = MaterialTheme.colors
    Box(Modifier.aspectRatio(1f).fillMaxSize().background(color = colors.primary))
}

@Sampled
@Composable
fun ThemeTextStyleSample() {
    val typography = MaterialTheme.typography
    Text(text = "H4 styled text", style = typography.h4)
}
