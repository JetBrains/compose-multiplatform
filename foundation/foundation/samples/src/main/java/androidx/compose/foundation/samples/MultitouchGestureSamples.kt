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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectMultitouchGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalPointerInput::class)
@Composable
@Sampled
fun DetectMultitouchGestures() {
    var angle by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Box(
        Modifier.offset({ offsetX }, { offsetY })
            .graphicsLayer(
                scaleX = zoom,
                scaleY = zoom,
                rotationZ = angle
            )
            .background(Color.Blue)
            .pointerInput {
                detectMultitouchGestures(
                    onGesture = { _, pan, gestureZoom, gestureRotate ->
                        angle += gestureRotate
                        zoom *= gestureZoom
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                )
            }
            .fillMaxSize()
    )
}

@OptIn(ExperimentalPointerInput::class)
@Composable
@Sampled
fun CalculateRotation() {
    var angle by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .graphicsLayer(rotationZ = angle)
            .background(Color.Blue)
            .pointerInput {
                forEachGesture {
                    handlePointerInput {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            val rotation = event.calculateRotation()
                            angle += rotation
                        } while (event.changes.any { it.current.down })
                    }
                }
            }
            .fillMaxSize()
    )
}

@OptIn(ExperimentalPointerInput::class)
@Composable
@Sampled
fun CalculateZoom() {
    var zoom by remember { mutableStateOf(1f) }
    Box(
        Modifier
            .graphicsLayer(scaleX = zoom, scaleY = zoom)
            .background(Color.Blue)
            .pointerInput {
                forEachGesture {
                    handlePointerInput {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            zoom *= event.calculateZoom()
                        } while (event.changes.any { it.current.down })
                    }
                }
            }
            .fillMaxSize()
    )
}

@OptIn(ExperimentalPointerInput::class)
@Composable
@Sampled
fun CalculatePan() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    Box(
        Modifier
            .offset({ offsetX.value }, { offsetY.value })
            .graphicsLayer()
            .background(Color.Blue)
            .pointerInput {
                forEachGesture {
                    handlePointerInput {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            val offset = event.calculatePan()
                            offsetX.value += offset.x
                            offsetY.value += offset.y
                        } while (event.changes.any { it.current.down })
                    }
                }
            }
            .fillMaxSize()
    )
}

@OptIn(ExperimentalPointerInput::class)
@Composable
@Sampled
fun CalculateCentroidSize() {
    var centroidSize by remember { mutableStateOf(0f) }
    var position by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .drawBehind {
                // Draw a circle where the gesture is
                drawCircle(Color.Blue, centroidSize, center = position)
            }
            .pointerInput {
                forEachGesture {
                    handlePointerInput {
                        awaitFirstDown().also {
                            position = it.current.position
                        }
                        do {
                            val event = awaitPointerEvent()
                            val size = event.calculateCentroidSize()
                            if (size != 0f) {
                                centroidSize = event.calculateCentroidSize()
                            }
                            val centroid = event.calculateCentroid()
                            if (centroid != Offset.Unspecified) {
                                position = centroid
                            }
                        } while (event.changes.any { it.current.down })
                    }
                }
            }
            .fillMaxSize()
    )
}