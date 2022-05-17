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

package androidx.compose.foundation.demos.text

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TextStyle parameters color, textDecoration and shadow currently have special treatment that
 * they are sent to TextPainter and set on the paint. Previously we did this for performance
 * reasons.
 *
 * Some of the text params definitely change the text metrics and require re-layout of the text.
 * However something like background do not change text metrics and ideally could have not required
 * text relayout.
 *
 * While updating the TextStyle.canReuseLayout code, I realized that background is in the list of
 * attributes that requires relayout. This demo contains 2 attributes that does not require layout
 * (color and shadow) and 1 that does require relayout (background); all three changing colors.
 *
 * The goal was to see animations do work on those attributes.
 */
@Preview
@Composable
fun TextReuseLayoutDemo() {
    val colorAnimationSpec = remember {
        infiniteRepeatable(tween<Color>(3000), repeatMode = RepeatMode.Reverse)
    }
    val color by rememberInfiniteTransition()
        .animateColor(Color.Red, Color.Green, colorAnimationSpec)

    val shadowDistanceAnimationSpec = remember {
        infiniteRepeatable(tween<Float>(3000), repeatMode = RepeatMode.Reverse)
    }
    val shadowDistance by rememberInfiniteTransition()
        .animateFloat(5f, 20f, shadowDistanceAnimationSpec)

    val style = TextStyle(fontSize = 64.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
    val shadow = Shadow(
        color = color,
        blurRadius = Float.MIN_VALUE,
        offset = Offset(shadowDistance, shadowDistance)
    )
    val text = "ABC"

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = text, style = style.copy(color = color))

        Spacer(Modifier.padding(16.dp))

        Text(text = text, style = style.copy(background = color))

        Spacer(Modifier.padding(16.dp))

        Text(text = text, style = style.copy(shadow = shadow))
    }
}
