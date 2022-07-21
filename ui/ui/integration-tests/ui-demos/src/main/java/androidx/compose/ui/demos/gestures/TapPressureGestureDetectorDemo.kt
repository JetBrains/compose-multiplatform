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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple demonstration of subscribing to pressure changes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DetectTapPressureGesturesDemo() {
    val pressureBoxTextSize = 28.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textAlign = TextAlign.Center,
            text = "Each box displays pressure (with and without gestures).\n" +
                "Use a stylus or finger to see pressure values.\n" +
                "For some pen supported devices, a finger touch pressure will equal 1.0."
        )

        var gestureOffsetX by remember { mutableStateOf(0f) }
        var gestureOffsetY by remember { mutableStateOf(0f) }
        var gesturePressure by remember { mutableStateOf(0f) }

        // Gestures (detectDragGestures) with pressure.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .background(Color.Blue)
                .wrapContentSize(Alignment.Center)
                .clipToBounds()
                .border(BorderStroke(2.dp, BorderColor))
                // Resets x & y when a new press is detected (not necessarily needed for pressure).
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            gestureOffsetX = 0f
                            gestureOffsetY = 0f
                        }
                    )
                }
                // Retrieves pressure from [PointerInputChange].
                .pointerInput(Unit) {
                    detectDragGestures { change: PointerInputChange, dragAmount: Offset ->
                        change.consume()
                        gestureOffsetX += dragAmount.x
                        gestureOffsetY += dragAmount.y
                        gesturePressure = change.pressure
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                modifier = Modifier.fillMaxSize(),
                fontSize = pressureBoxTextSize,
                textAlign = TextAlign.Center,
                color = Color.White,
                text = "detectDragGestures + pressure:\n" +
                    "x: $gestureOffsetX, y: $gestureOffsetY,\n" +
                    "pressure: $gesturePressure"
            )
        }

        var awaitPointerEventScopePressureMessage by remember {
            mutableStateOf("Press for value")
        }

        // awaitPointerEventScope with pressure.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .background(Color.Green)
                .wrapContentSize(Alignment.Center)
                .clipToBounds()
                .border(BorderStroke(2.dp, BorderColor))
                // Retrieves pressure from [PointerInputChange].
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach {
                                awaitPointerEventScopePressureMessage = "${it.pressure}"
                                it.consume()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                modifier = Modifier.fillMaxSize(),
                fontSize = pressureBoxTextSize,
                textAlign = TextAlign.Center,
                text = "awaitPointerEventScope + pressure:\n$awaitPointerEventScopePressureMessage"
            )
        }
    }
}
