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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun GraphicsLayerSettings() {
    var scaleX by remember { mutableStateOf(1f) }
    var scaleY by remember { mutableStateOf(1f) }
    var translationX by remember { mutableStateOf(0f) }
    var translationY by remember { mutableStateOf(0f) }
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var rotationZ by remember { mutableStateOf(0f) }
    var cameraDistance by remember { mutableStateOf(DefaultCameraDistance) }
    var originX by remember { mutableStateOf(0.5f) }
    var originY by remember { mutableStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Box(
            Modifier
                .graphicsLayer(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    translationX = translationX,
                    translationY = translationY,
                    rotationX = rotationX,
                    rotationY = rotationY,
                    rotationZ = rotationZ,
                    cameraDistance = cameraDistance,
                    transformOrigin = TransformOrigin(originX, originY),
                )
                .align(CenterHorizontally)
                .size(200.dp)
                .background(MaterialTheme.colors.secondary)
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Box(
                modifier = Modifier
                    .offset(10.dp, 10.dp)
                    .size(80.dp)
                    .background(MaterialTheme.colors.primary)
                    .pointerHoverIcon(PointerIcon.Crosshair)
            )
        }

        Spacer(Modifier.height(20.dp))
        SliderSetting("ScaleX", scaleX, 0.5f..2f) { scaleX = it }
        SliderSetting("ScaleY", scaleY, 0.5f..2f) { scaleY = it }
        SliderSetting("TranslationX", translationX, -250f..250f) { translationX = it }
        SliderSetting("TranslationY", translationY, -250f..250f) { translationY = it }
        SliderSetting("RotateX", rotationX, -180f..180f) { rotationX = it }
        SliderSetting("RotateY", rotationY, -180f..180f) { rotationY = it }
        SliderSetting("RotateZ", rotationZ, -180f..180f) { rotationZ = it }
        SliderSetting("OriginX", originX, 0f..1f) { originX = it }
        SliderSetting("OriginY", originY, 0f..1f) { originY = it }
        SliderSetting("CameraDistance", cameraDistance, 3f..30f) { cameraDistance = it }
    }
}

@Composable
fun SliderSetting(text: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(30.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = "${round(value * 10f) / 10f}",
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.width(50.dp)
        )
        var steps = ((range.endInclusive - range.start) / 0.1f).roundToInt()
        if (steps > 100) steps /= 10
        if (steps > 100) steps /= 10
        Slider(
            value = value,
            onValueChange,
            valueRange = range,
            steps = steps - 1
        )
    }
}
