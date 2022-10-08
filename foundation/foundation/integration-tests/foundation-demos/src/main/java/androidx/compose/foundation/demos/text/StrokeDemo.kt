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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
@Composable
fun TextStrokeDemo() {
    var dashInterval by remember { mutableStateOf(2f) }
    var stroke by remember {
        mutableStateOf(
            Stroke(
                width = 4f, pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(2f, 2f)
                )
            )
        )
    }
    var fontSize by remember { mutableStateOf(20.sp) }

    val finalStroke by remember {
        derivedStateOf {
            stroke.copy(
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(
                        dashInterval,
                        dashInterval
                    ), phase = 0f
                )
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Font Size")
        Slider(
            value = fontSize.value,
            onValueChange = {
                fontSize = it.sp
            },
            valueRange = 8f..144f
        )
        Text("Width")
        Slider(
            value = stroke.width,
            onValueChange = {
                stroke = stroke.copy(width = it)
            },
            valueRange = 0f..16f,
            steps = 16
        )
        Text("Miter")
        Slider(
            value = stroke.miter,
            onValueChange = {
                stroke = stroke.copy(miter = it)
            },
            valueRange = 0f..16f,
            steps = 16
        )

        Text("Dash on/off intervals")
        Slider(
            value = dashInterval,
            onValueChange = { dashInterval = it },
            valueRange = 0f..16f,
            steps = 16
        )

        Text("Cap")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RadioButton(
                selected = stroke.cap == StrokeCap.Butt,
                onClick = { stroke = stroke.copy(cap = StrokeCap.Butt) })
            Text(text = "Butt", style = MaterialTheme.typography.body2)

            RadioButton(
                selected = stroke.cap == StrokeCap.Round,
                onClick = { stroke = stroke.copy(cap = StrokeCap.Round) })
            Text(text = "Round", style = MaterialTheme.typography.body2)

            RadioButton(
                selected = stroke.cap == StrokeCap.Square,
                onClick = { stroke = stroke.copy(cap = StrokeCap.Square) })
            Text(text = "Square", style = MaterialTheme.typography.body2)
        }

        Text("Join")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RadioButton(
                selected = stroke.join == StrokeJoin.Round,
                onClick = { stroke = stroke.copy(join = StrokeJoin.Round) })
            Text(text = "Round", style = MaterialTheme.typography.body2)

            RadioButton(
                selected = stroke.join == StrokeJoin.Miter,
                onClick = { stroke = stroke.copy(join = StrokeJoin.Miter) })
            Text(text = "Miter", style = MaterialTheme.typography.body2)

            RadioButton(
                selected = stroke.join == StrokeJoin.Bevel,
                onClick = { stroke = stroke.copy(join = StrokeJoin.Bevel) })
            Text(text = "Bevel", style = MaterialTheme.typography.body2)
        }

        Text(
            text = "This text is drawn using stroke! 🎉",
            style = LocalTextStyle.current.merge(
                TextStyle(
                    fontSize = fontSize,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Red,
                            Color.Green,
                            Color.Blue
                        )
                    ), drawStyle = finalStroke
                )
            )
        )
    }
}

fun Stroke.copy(
    width: Float = this.width,
    miter: Float = this.miter,
    cap: StrokeCap = this.cap,
    join: StrokeJoin = this.join,
    pathEffect: PathEffect? = this.pathEffect
): Stroke {
    return Stroke(width, miter, cap, join, pathEffect)
}