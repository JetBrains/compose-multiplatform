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

package androidx.compose.ui.graphics.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun StampedPathEffectSample() {
    val size = 20f
    val square = Path().apply {
        lineTo(size, 0f)
        lineTo(size, size)
        lineTo(0f, size)
        close()
    }
    Column(modifier = Modifier.fillMaxHeight().wrapContentSize(Alignment.Center)) {
        val canvasModifier = Modifier.requiredSize(80.dp).align(Alignment.CenterHorizontally)

        // StampedPathEffectStyle.Morph will modify the lines of the square to be curved to fit
        // the curvature of the circle itself. Each stamped square will be rendered as an arc
        // that is fully contained by the bounds of the circle itself
        Canvas(modifier = canvasModifier) {
            drawCircle(color = Color.Blue)
            drawCircle(
                color = Color.Red,
                style = Stroke(
                    pathEffect = PathEffect.stampedPathEffect(
                        shape = square,
                        style = StampedPathEffectStyle.Morph,
                        phase = 0f,
                        advance = 30f
                    )
                )
            )
        }

        Spacer(modifier = Modifier.requiredSize(10.dp))

        // StampedPathEffectStyle.Rotate will draw the square repeatedly around the circle
        // such that each stamped square is centered on the circumference of the circle and is
        // rotated along the curvature of the circle itself
        Canvas(modifier = canvasModifier) {
            drawCircle(color = Color.Blue)
            drawCircle(
                color = Color.Red,
                style = Stroke(
                    pathEffect = PathEffect.stampedPathEffect(
                        shape = square,
                        style = StampedPathEffectStyle.Rotate,
                        phase = 0f,
                        advance = 30f
                    )
                )
            )
        }

        Spacer(modifier = Modifier.requiredSize(10.dp))

        // StampedPathEffectStyle.Translate will draw the square repeatedly around the circle
        // with the top left of each stamped square on the circumference of the circle
        Canvas(modifier = canvasModifier) {
            drawCircle(color = Color.Blue)
            drawCircle(
                color = Color.Red,
                style = Stroke(
                    pathEffect = PathEffect.stampedPathEffect(
                        shape = square,
                        style = StampedPathEffectStyle.Translate,
                        phase = 0f,
                        advance = 30f
                    )
                )
            )
        }
    }
}