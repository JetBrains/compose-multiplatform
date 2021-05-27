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

import android.graphics.Matrix
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

val CoroutineGestureDemos = listOf(
    ComposableDemo("Tap/Double-Tap/Long Press") { CoroutineTapDemo() },
    ComposableDemo("Drag Horizontal and Vertical") { TouchSlopDragGestures() },
    ComposableDemo("Drag with orientation locking") { OrientationLockDragGestures() },
    ComposableDemo("Drag 2D") { Drag2DGestures() },
    ComposableDemo("Rotation/Pan/Zoom") { MultitouchGestureDetector() },
    ComposableDemo("Rotation/Pan/Zoom with Lock") { MultitouchLockGestureDetector() },
    ComposableDemo("Pointer type input") { PointerTypeInput() },
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
        Spacer(Modifier.requiredSize(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .pointerInput(Unit) {
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
        Spacer(Modifier.requiredSize(5.dp))
        Row {
            Box(
                Modifier
                    .size(50.dp)
                    .background(hueToColor(tapHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on tap", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.requiredSize(5.dp))
        Row {
            Box(
                Modifier
                    .size(50.dp)
                    .clipToBounds()
                    .background(hueToColor(doubleTapHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on double-tap", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.requiredSize(5.dp))
        Row {
            Box(
                Modifier
                    .size(50.dp)
                    .clipToBounds()
                    .background(hueToColor(longPressHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on long press", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.requiredSize(5.dp))
        Row {
            Box(
                Modifier
                    .size(50.dp)
                    .clipToBounds()
                    .background(hueToColor(pressHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on press", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.requiredSize(5.dp))
        Row {
            Box(
                Modifier
                    .size(50.dp)
                    .clipToBounds()
                    .background(hueToColor(releaseHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on release", Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.requiredSize(5.dp))
        Row {
            Box(
                Modifier
                    .size(50.dp)
                    .clipToBounds()
                    .background(hueToColor(cancelHue))
                    .border(BorderStroke(2.dp, Color.Black))
            )
            Text("Changes color on cancel", Modifier.align(Alignment.CenterVertically))
        }
    }
}

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
                    .requiredSize(50.dp)
                    .background(Color.Blue)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragDistance ->
                            val offsetPx = offset.toPx()
                            val newOffset =
                                (offsetPx + dragDistance).coerceIn(0f, width - 50.dp.toPx())
                            val consumed = newOffset - offsetPx
                            if (consumed != 0f) {
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
                        .requiredSize(50.dp)
                        .background(Color.Red)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragDistance ->
                                val offsetPx = offset.toPx()
                                val newOffset = (offsetPx + dragDistance)
                                    .coerceIn(0f, height - 50.dp.toPx())
                                val consumed = newOffset - offsetPx
                                if (consumed != 0f) {
                                    offset = newOffset.toDp()
                                }
                            }
                        }
                )
            }
            Box(
                Modifier.requiredHeight(50.dp)
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

@Composable
fun OrientationLockDragGestures() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    Box(
        Modifier.onSizeChanged {
            size = it
        }.pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
                offsetY = (offsetY.toPx() + dragAmount)
                    .coerceIn(0f, size.height.toFloat() - 50.dp.toPx()).toDp()
            }
        }

    ) {
        Box(
            Modifier.offset(offsetX, 0.dp)
                .background(Color.Blue.copy(alpha = 0.5f))
                .requiredWidth(50.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        offsetX = (offsetX.toPx() + dragAmount)
                            .coerceIn(0f, size.width.toFloat() - 50.dp.toPx()).toDp()
                    }
                }
        )
        Box(
            Modifier.offset(0.dp, offsetY)
                .background(Color.Red.copy(alpha = 0.5f))
                .requiredHeight(50.dp)
                .fillMaxWidth()
        )

        Text(
            "Drag the columns around. They should lock to vertical or horizontal.",
            Modifier.align(Alignment.Center)
        )
    }
}

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
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .background(Color.Blue)
                .requiredSize(50.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
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

@Composable
fun MultitouchArea(
    text: String,
    gestureDetector: suspend PointerInputScope.(
        (centroid: Offset, pan: Offset, zoom: Float, angle: Float) -> Unit,
    ) -> Unit
) {
    val matrix by remember { mutableStateOf(Matrix()) }
    var angle by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            gestureDetector { centroid, pan, gestureZoom, gestureAngle ->
                val anchorX = centroid.x - size.width / 2f
                val anchorY = centroid.y - size.height / 2f
                matrix.postRotate(gestureAngle, anchorX, anchorY)
                matrix.postScale(gestureZoom, gestureZoom, anchorX, anchorY)
                matrix.postTranslate(pan.x, pan.y)

                val v = FloatArray(9)
                matrix.getValues(v)
                offsetX = v[Matrix.MTRANS_X]
                offsetY = v[Matrix.MTRANS_Y]
                val scaleX = v[Matrix.MSCALE_X]
                val skewY = v[Matrix.MSKEW_Y]
                zoom = sqrt(scaleX * scaleX + skewY * skewY)
                angle = atan2(v[Matrix.MSKEW_X], v[Matrix.MSCALE_X]) * (-180 / Math.PI.toFloat())
            }
        }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
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
@Composable
fun MultitouchGestureDetector() {
    MultitouchArea(
        "Zoom, Pan, and Rotate"
    ) {
        detectTransformGestures(
            panZoomLock = false,
            onGesture = it
        )
    }
}

/**
 * This is a multi-touch gesture detector, including pan, zoom, and rotation.
 * It is common to want to lean toward zoom over rotation, so this gesture detector will
 * lock into zoom if the first unless the rotation passes touch slop first.
 */
@Composable
fun MultitouchLockGestureDetector() {
    MultitouchArea(
        "Zoom, Pan, and Rotate Locking to Zoom"
    ) {
        detectTransformGestures(
            panZoomLock = true,
            onGesture = it
        )
    }
}

@Composable
fun PointerTypeInput() {
    var pointerType by remember { mutableStateOf<PointerType?>(null) }
    Box(
        Modifier.pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    val pointer = awaitPointerEvent().changes.first()
                    pointerType = pointer.type
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.first().pressed)
                    pointerType = null
                }
            }
        }
    ) {
        Text("Touch or click the area to see what type of input it is.")
        Text("PointerType: ${pointerType ?: ""}", Modifier.align(Alignment.BottomStart))
    }
}