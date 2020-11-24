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

package androidx.compose.foundation.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectMultitouchGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random

val CoroutineGestureDemos = listOf(
    ComposableDemo("Tap/Double-Tap/Long Press") { CoroutineTapDemo() },
    ComposableDemo("Drag Horizontal and Vertical") { TouchSlopDragGestures() },
    ComposableDemo("Drag with orientation locking") { OrientationLockDragGestures() },
    ComposableDemo("Drag 2D") { Drag2DGestures() },
    ComposableDemo("Rotation/Pan/Zoom") { MultitouchGestureDetector() },
    ComposableDemo("Rotation/Pan/Zoom with Lock") { MultitouchLockGestureDetector() },
)

fun hueToColor(hue: Float): Color {
    val huePrime = hue / 60
    val hueRange = huePrime.toInt()
    val hueRemainder = huePrime - hueRange
    return when (hueRange) {
        0 -> Color(1f, hueRemainder, 0f)
        1 -> Color(1f - hueRemainder, 1f, 0f)
        2 -> Color(0f, 1f, hueRemainder)
        3 -> Color(0f, 1f - hueRemainder, 1f)
        4 -> Color(hueRemainder, 0f, 1f)
        else -> Color(1f, 0f, 1f - hueRemainder)
    }
}

fun randomHue() = Random.nextFloat() * 360

fun anotherRandomHue(hue: Float): Float {
    val newHue: Float = Random.nextFloat() * 260f

    // we don't want the hue to be close, so we ensure that it isn't with 50 of the current hue
    return if (newHue > hue - 50f) {
        newHue + 100f
    } else {
        newHue
    }
}

/**
 * Gesture detector for tap, double-tap, and long-press.
 */
@OptIn(ExperimentalPointerInput::class)
@Composable
fun CoroutineTapDemo() {
    var tapHue by remember { mutableStateOf(randomHue()) }
    var longPressHue by remember { mutableStateOf(randomHue()) }
    var doubleTapHue by remember { mutableStateOf(randomHue()) }
    var pressHue by remember { mutableStateOf(randomHue()) }
    var releaseHue by remember { mutableStateOf(randomHue()) }
    var cancelHue by remember { mutableStateOf(randomHue()) }

    Column {
        Text("The boxes change color when you tap the white box.")
        Spacer(Modifier.size(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .preferredHeight(50.dp)
                .pointerInput {
                    detectTapGestures(
                        onTap = { tapHue = anotherRandomHue(tapHue) },
                        onDoubleTap = { doubleTapHue = anotherRandomHue(doubleTapHue) },
                        onLongPress = { longPressHue = anotherRandomHue(longPressHue) },
                        onPress = {
                            pressHue = anotherRandomHue(pressHue)
                            if (tryAwaitRelease()) {
                                releaseHue = anotherRandomHue(releaseHue)
                            } else {
                                cancelHue = anotherRandomHue(cancelHue)
                            }
                        }
                    )
                }
                .background(Color.White)
                .border(BorderStroke(2.dp, Color.Black))
        ) {
            Text("Tap, double-tap, or long-press", Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .background(hueToColor(tapHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on tap", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(doubleTapHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on double-tap", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(longPressHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on long press", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(pressHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on press", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(releaseHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on release", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.size(5.dp))
        Row {
            Box(
                Modifier
                    .preferredSize(50.dp)
                    .clipToBounds()
                    .background(hueToColor(cancelHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on cancel", Modifier.align(Alignment.CenterVertically))
        }
    }
}

@OptIn(ExperimentalPointerInput::class)
@Composable
fun TouchSlopDragGestures() {
    Column {
        var width by remember { mutableStateOf(0f) }
        Box(
            Modifier.fillMaxWidth()
                .background(Color.Cyan)
                .onSizeChanged { width = it.width.toFloat() }
        ) {
            var offset by remember { mutableStateOf(0.dp) }
            Box(
                Modifier.offset(offset, 0.dp)
                    .size(50.dp)
                    .background(Color.Blue)
                    .pointerInput {
                        detectHorizontalDragGestures { change, dragDistance ->
                            val offsetPx = offset.toPx()
                            val newOffset =
                                (offsetPx + dragDistance).coerceIn(0f, width - 50.dp.toPx())
                            val consumed = newOffset - offsetPx
                            if (consumed != 0f) {
                                change.consumePositionChange(change.positionChange().x, 0f)
                                offset = newOffset.toDp()
                            }
                        }
                    }
            )
            Text("Drag blue box within here", Modifier.align(Alignment.Center))
        }

        Box(Modifier.weight(1f)) {
            var height by remember { mutableStateOf(0f) }
            Box(
                Modifier.fillMaxHeight()
                    .background(Color.Yellow)
                    .onSizeChanged { height = it.height.toFloat() }
            ) {
                var offset by remember { mutableStateOf(0.dp) }
                Box(
                    Modifier.offset(0.dp, offset)
                        .size(50.dp)
                        .background(Color.Red)
                        .pointerInput {
                            detectVerticalDragGestures { change, dragDistance ->
                                val offsetPx = offset.toPx()
                                val newOffset = (offsetPx + dragDistance)
                                    .coerceIn(0f, height - 50.dp.toPx())
                                val consumed = newOffset - offsetPx
                                if (consumed != 0f) {
                                    change.consumePositionChange(
                                        0f,
                                        change.positionChange().y
                                    )
                                    offset = newOffset.toDp()
                                }
                            }
                        }
                )
            }
            Box(
                Modifier.height(50.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .graphicsLayer(
                        rotationZ = 90f,
                        transformOrigin = TransformOrigin(0f, 1f)
                    )
            ) {
                Text(
                    "Drag red box within here",
                    Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalPointerInput::class)
@Composable
fun OrientationLockDragGestures() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    Box(
        Modifier.onSizeChanged {
            size = it
        }.pointerInput {
            detectVerticalDragGestures { change, dragAmount ->
                change.consumePositionChange(0f, change.positionChange().y)
                offsetY = (offsetY.toPx() + dragAmount)
                    .coerceIn(0f, size.height.toFloat() - 50.dp.toPx()).toDp()
            }
        }

    ) {
        Box(
            Modifier.offset(offsetX, 0.dp)
                .background(Color.Blue.copy(alpha = 0.5f))
                .width(50.dp)
                .fillMaxHeight()
                .pointerInput {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consumePositionChange(change.positionChange().x, 0f)
                        offsetX = (offsetX.toPx() + dragAmount)
                            .coerceIn(0f, size.width.toFloat() - 50.dp.toPx()).toDp()
                    }
                }
        )
        Box(
            Modifier.offset(0.dp, offsetY)
                .background(Color.Red.copy(alpha = 0.5f))
                .height(50.dp)
                .fillMaxWidth()
        )

        Text(
            "Drag the columns around. They should lock to vertical or horizontal.",
            Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalPointerInput::class)
@Composable
fun Drag2DGestures() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    Box(
        Modifier.onSizeChanged {
            size = it
        }.fillMaxSize()
    ) {
        Box(
            Modifier.offset({ offsetX.value }, { offsetY.value })
                .background(Color.Blue)
                .size(50.dp)
                .pointerInput {
                    detectDragGestures { change, dragAmount ->
                        change.consumeAllChanges()
                        offsetX.value = (offsetX.value + dragAmount.x)
                            .coerceIn(0f, size.width.toFloat() - 50.dp.toPx())

                        offsetY.value = (offsetY.value + dragAmount.y)
                            .coerceIn(0f, size.height.toFloat() - 50.dp.toPx())
                    }
                }
        )
        Text("Drag the box around", Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalPointerInput::class)
@Composable
fun MultitouchArea(
    text: String,
    gestureDetector: suspend PointerInputScope.(
        (angle: Float) -> Unit,
        (zoom: Float) -> Unit,
        (pan: Offset) -> Unit
    ) -> Unit
) {
    var angle by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset({ offsetX.value }, { offsetY.value })
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    rotationZ = angle
                ).drawBehind {
                    val approximateRectangleSize = 10.dp.toPx()
                    val numRectanglesHorizontal =
                        (size.width / approximateRectangleSize).roundToInt()
                    val numRectanglesVertical =
                        (size.height / approximateRectangleSize).roundToInt()
                    val rectangleWidth = size.width / numRectanglesHorizontal
                    val rectangleHeight = size.height / numRectanglesVertical
                    var hue = 0f
                    val rectangleSize = Size(rectangleWidth, rectangleHeight)
                    for (x in 0 until numRectanglesHorizontal) {
                        for (y in 0 until numRectanglesVertical) {
                            hue += 30
                            if (hue >= 360f) {
                                hue = 0f
                            }
                            val color = hueToColor(hue)
                            val topLeft = Offset(
                                x = x * size.width / numRectanglesHorizontal,
                                y = y * size.height / numRectanglesVertical
                            )
                            drawRect(color = color, topLeft = topLeft, size = rectangleSize)
                        }
                    }
                }.pointerInput {
                    gestureDetector(
                        { angle += it },
                        { zoom *= it },
                        {
                            offsetX.value += it.x
                            offsetY.value += it.y
                        }
                    )
                }
                .fillMaxSize()
        )
        Text(text)
    }
}

/**
 * This is a multi-touch gesture detector, including pan, zoom, and rotation.
 * The user can pan, zoom, and rotate once touch slop has been reached.
 */
@OptIn(ExperimentalPointerInput::class)
@Composable
fun MultitouchGestureDetector() {
    MultitouchArea(
        "Zoom, Pan, and Rotate"
    ) { onRotate, onZoom, onPan ->
        detectMultitouchGestures(
            panZoomLock = false,
            onRotate = onRotate,
            onZoom = onZoom,
            onPan = onPan
        )
    }
}

/**
 * This is a multi-touch gesture detector, including pan, zoom, and rotation.
 * It is common to want to lean toward zoom over rotation, so this gesture detector will
 * lock into zoom if the first unless the rotation passes touch slop first.
 */
@OptIn(ExperimentalPointerInput::class)
@Composable
fun MultitouchLockGestureDetector() {
    MultitouchArea(
        "Zoom, Pan, and Rotate Locking to Zoom"
    ) { onRotate, onZoom, onPan ->
        detectMultitouchGestures(
            panZoomLock = true,
            onRotate = onRotate,
            onZoom = onZoom,
            onPan = onPan
        )
    }
}
