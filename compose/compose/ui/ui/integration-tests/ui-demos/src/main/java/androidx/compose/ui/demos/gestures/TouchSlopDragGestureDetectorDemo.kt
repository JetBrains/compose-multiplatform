/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Simple [detectVerticalDragGestures] and [detectHorizontalDragGestures] demo.
 */
@Composable
fun DragGestureFilterDemo() {

    val verticalColor = Color(0xfff44336)
    val horizontalColor = Color(0xff2196f3)

    val offset = remember { mutableStateOf(Offset.Zero) }
    val canStartVertically = remember { mutableStateOf(true) }

    val color =
        if (canStartVertically.value) {
            verticalColor
        } else {
            horizontalColor
        }

    val (offsetX, offsetY) =
        with(LocalDensity.current) { offset.value.x.toDp() to offset.value.y.toDp() }

    Column {
        Text(
            "Demonstrates standard dragging (when a slop has to be exceeded before dragging can " +
                "start) and customization of the direction in which dragging can occur."
        )
        Text(
            "When the box is blue, it can only be dragged horizontally.  When the box is red, it" +
                " can only be dragged vertically."
        )
        Box(
            Modifier.offset(offsetX, offsetY)
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(192.dp)
                .pointerInput(Unit) {
                    if (canStartVertically.value) {
                        detectVerticalDragGestures(
                            onDragEnd = { canStartVertically.value = !canStartVertically.value }
                        ) { _, dragDistance ->
                            offset.value =
                                Offset(x = offset.value.x, y = offset.value.y + dragDistance)
                        }
                    } else {
                        detectHorizontalDragGestures(
                            onDragEnd = { canStartVertically.value = !canStartVertically.value }
                        ) { _, dragDistance ->
                            offset.value =
                                Offset(x = offset.value.x + dragDistance, y = offset.value.y)
                        }
                    }
                }
                .background(color)
        )
    }
}
