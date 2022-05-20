/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.animation.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
internal fun AlphaBoxDemo(animatedFloat: Float) {
    Rect(color = androidGreen.copy(alpha = animatedFloat.coerceIn(0f, 1f)))
}

@Composable
internal fun ScalingBoxDemo(animatedFloat: Float) {
    Rect(modifier = Modifier.scale(animatedFloat))
}

@Composable
internal fun RotatingBoxDemo(animatedFloat: Float) {
    Rect(modifier = Modifier.rotate(animatedFloat * 360f))
}

@Composable
internal fun TranslationBoxDemo(animatedFloat: Float) {
    Box(modifier = Modifier.height(100.dp).width(50.dp)) {
        OutlinedSquare(modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            val sizePx = 16.dp.toPx()
            val strokeThickness = 1.dp.toPx()
            val size = Size(sizePx, sizePx)
            drawRect(
                androidGreen,
                topLeft = Offset(
                    this.size.width / 2f - sizePx / 2f,
                    animatedFloat * (this.size.height - sizePx - strokeThickness)
                ),
                size = size
            )
        })
    }
}

@Composable
internal fun ColorBoxDemo(animatedFloat: Float) {
    Rect(color = lerp(androidGreen, AndroidBlue, animatedFloat))
}

@Composable
internal fun Rect(
    modifier: Modifier = Modifier,
    color: Color = androidGreen
) {
    Box {
        OutlinedSquare(boxSize)
        Canvas(modifier = modifier.then(boxSize), onDraw = {
            val sizePx = 16.dp.toPx()
            val size = Size(sizePx, sizePx)
            drawRect(
                color,
                topLeft = Offset(
                    this.size.width / 2f - sizePx / 2f,
                    this.size.height / 2f - sizePx / 2f
                ),
                size = size
            )
        })
    }
}

@Composable
internal fun OutlinedSquare(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(AndroidNavy, style = Stroke(1.dp.toPx()))
    }
}

private val boxSize = Modifier.size(50.dp)
