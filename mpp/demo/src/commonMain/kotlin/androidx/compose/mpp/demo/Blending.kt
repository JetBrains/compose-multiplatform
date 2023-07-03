/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt


@Composable
fun Blending() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        var blendMode by remember { mutableStateOf(DefaultBlendMode) }
        var red by remember { mutableStateOf(1f) }
        var green by remember { mutableStateOf(0f) }
        var blue by remember { mutableStateOf(0f) }
        var alpha by remember { mutableStateOf(0.5f) }
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(200.dp)
        ) {
            val radius = size.minDimension / 2
            drawCircle(
                Color.Red,
                center = Offset(center.x - radius / 2, center.y),
                radius = radius,
                alpha = 0.5f,
            )

            drawCircle(
                color = Color(red, green, blue),
                center = Offset(center.x + radius / 2, center.y),
                radius = radius,
                alpha = alpha,
                blendMode = blendMode
            )
        }
        SliderSetting("Red", red) { red = it }
        SliderSetting("Green", green) { green = it }
        SliderSetting("Blue", blue) { blue = it }
        SliderSetting("Alpha", alpha) { alpha = it }
        BlendModeSetting(blendMode) { blendMode = it }
    }
}

@Composable
fun BlendModeSetting(
    value: BlendMode,
    onOptionSelected: (BlendMode) -> Unit
) {
    val blendModes = listOf(
        BlendMode.Clear,
        BlendMode.Src,
        BlendMode.Dst,
        BlendMode.SrcOver,
        BlendMode.DstOver,
        BlendMode.SrcIn,
        BlendMode.DstIn,
        BlendMode.SrcOut,
        BlendMode.DstOut,
        BlendMode.SrcAtop,
        BlendMode.DstAtop,
        BlendMode.Xor,
        BlendMode.Plus,
        BlendMode.Modulate,
        BlendMode.Screen,
        BlendMode.Overlay,
        BlendMode.Darken,
        BlendMode.Lighten,
        BlendMode.ColorDodge,
        BlendMode.ColorBurn,
        BlendMode.Hardlight,
        BlendMode.Softlight,
        BlendMode.Difference,
        BlendMode.Exclusion,
        BlendMode.Multiply,
        BlendMode.Hue,
        BlendMode.Saturation,
        BlendMode.Color,
        BlendMode.Luminosity,
    )
    LazyColumn(modifier = Modifier.selectableGroup()) {
        items(blendModes) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .selectable(
                        selected = it == value,
                        onClick = { onOptionSelected(it) },
                        role = Role.RadioButton
                    )
            ) {
                RadioButton(
                    selected = it == value,
                    onClick = null
                )
                Text(
                    text = it.toString(),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
