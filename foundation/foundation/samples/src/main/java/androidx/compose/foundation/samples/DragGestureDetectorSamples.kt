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
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

@Composable
@Sampled
fun AwaitHorizontalDragOrCancellationSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var width by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { width = it.width.toFloat() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxHeight()
                .width(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            var change =
                                awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                    val originalX = offsetX.value
                                    val newValue =
                                        (originalX + over).coerceIn(0f, width - 50.dp.toPx())
                                    change.consume()
                                    offsetX.value = newValue
                                }
                            while (change != null && change.pressed) {
                                change = awaitHorizontalDragOrCancellation(change.id)
                                if (change != null && change.pressed) {
                                    val originalX = offsetX.value
                                    val newValue = (originalX + change.positionChange().x)
                                        .coerceIn(0f, width - 50.dp.toPx())
                                    change.consume()
                                    offsetX.value = newValue
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
@Sampled
fun HorizontalDragSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var width by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { width = it.width.toFloat() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxHeight()
                .width(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            val change =
                                awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                    val originalX = offsetX.value
                                    val newValue =
                                        (originalX + over).coerceIn(0f, width - 50.dp.toPx())
                                    change.consume()
                                    offsetX.value = newValue
                                }
                            if (change != null) {
                                horizontalDrag(change.id) {
                                    val originalX = offsetX.value
                                    val newValue = (originalX + it.positionChange().x)
                                        .coerceIn(0f, width - 50.dp.toPx())
                                    it.consume()
                                    offsetX.value = newValue
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
@Sampled
fun DetectHorizontalDragGesturesSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var width by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { width = it.width.toFloat() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxHeight()
                .width(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val originalX = offsetX.value
                        val newValue = (originalX + dragAmount).coerceIn(0f, width - 50.dp.toPx())
                        offsetX.value = newValue
                    }
                }
        )
    }
}

@Composable
@Sampled
fun AwaitVerticalDragOrCancellationSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { height = it.height.toFloat() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            var change =
                                awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                                    val originalY = offsetY.value
                                    val newValue = (originalY + over)
                                        .coerceIn(0f, height - 50.dp.toPx())
                                    change.consume()
                                    offsetY.value = newValue
                                }
                            while (change != null && change.pressed) {
                                change = awaitVerticalDragOrCancellation(change.id)
                                if (change != null && change.pressed) {
                                    val originalY = offsetY.value
                                    val newValue = (originalY + change.positionChange().y)
                                        .coerceIn(0f, height - 50.dp.toPx())
                                    change.consume()
                                    offsetY.value = newValue
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
@Sampled
fun VerticalDragSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { height = it.height.toFloat() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            val change =
                                awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                                    val originalY = offsetY.value
                                    val newValue = (originalY + over)
                                        .coerceIn(0f, height - 50.dp.toPx())
                                    change.consume()
                                    offsetY.value = newValue
                                }
                            if (change != null) {
                                verticalDrag(change.id) {
                                    val originalY = offsetY.value
                                    val newValue = (originalY + it.positionChange().y)
                                        .coerceIn(0f, height - 50.dp.toPx())
                                    it.consume()
                                    offsetY.value = newValue
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
@Sampled
fun DetectVerticalDragGesturesSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { height = it.height.toFloat() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val originalY = offsetY.value
                        val newValue = (originalY + dragAmount).coerceIn(0f, height - 50.dp.toPx())
                        offsetY.value = newValue
                    }
                }
        )
    }
}

@Composable
@Sampled
fun AwaitDragOrCancellationSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(Size.Zero) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { size = it.toSize() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            var change = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                val original = Offset(offsetX.value, offsetY.value)
                                val summed = original + over
                                val newValue = Offset(
                                    x = summed.x.coerceIn(0f, size.width - 50.dp.toPx()),
                                    y = summed.y.coerceIn(0f, size.height - 50.dp.toPx())
                                )
                                change.consume()
                                offsetX.value = newValue.x
                                offsetY.value = newValue.y
                            }
                            while (change != null && change.pressed) {
                                change = awaitDragOrCancellation(change.id)
                                if (change != null && change.pressed) {
                                    val original = Offset(offsetX.value, offsetY.value)
                                    val summed = original + change.positionChange()
                                    val newValue = Offset(
                                        x = summed.x.coerceIn(0f, size.width - 50.dp.toPx()),
                                        y = summed.y.coerceIn(0f, size.height - 50.dp.toPx())
                                    )
                                    change.consume()
                                    offsetX.value = newValue.x
                                    offsetY.value = newValue.y
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
@Sampled
fun DragSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(Size.Zero) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { size = it.toSize() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            val change = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                val original = Offset(offsetX.value, offsetY.value)
                                val summed = original + over
                                val newValue = Offset(
                                    x = summed.x.coerceIn(0f, size.width - 50.dp.toPx()),
                                    y = summed.y.coerceIn(0f, size.height - 50.dp.toPx())
                                )
                                change.consume()
                                offsetX.value = newValue.x
                                offsetY.value = newValue.y
                            }
                            if (change != null) {
                                drag(change.id) {
                                    val original = Offset(offsetX.value, offsetY.value)
                                    val summed = original + it.positionChange()
                                    val newValue = Offset(
                                        x = summed.x.coerceIn(0f, size.width - 50.dp.toPx()),
                                        y = summed.y.coerceIn(0f, size.height - 50.dp.toPx())
                                    )
                                    it.consume()
                                    offsetX.value = newValue.x
                                    offsetY.value = newValue.y
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
@Sampled
fun DetectDragGesturesSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(Size.Zero) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { size = it.toSize() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val original = Offset(offsetX.value, offsetY.value)
                        val summed = original + dragAmount
                        val newValue = Offset(
                            x = summed.x.coerceIn(0f, size.width - 50.dp.toPx()),
                            y = summed.y.coerceIn(0f, size.height - 50.dp.toPx())
                        )
                        offsetX.value = newValue.x
                        offsetY.value = newValue.y
                    }
                }
        )
    }
}

@Composable
@Sampled
fun DetectDragWithLongPressGesturesSample() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(Size.Zero) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { size = it.toSize() }
    ) {
        Box(
            Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(50.dp)
                .background(Color.Blue)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress { _, dragAmount ->
                        val original = Offset(offsetX.value, offsetY.value)
                        val summed = original + dragAmount
                        val newValue = Offset(
                            x = summed.x.coerceIn(0f, size.width - 50.dp.toPx()),
                            y = summed.y.coerceIn(0f, size.height - 50.dp.toPx())
                        )
                        offsetX.value = newValue.x
                        offsetY.value = newValue.y
                    }
                }
        )
    }
}
