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

package androidx.compose.foundation.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Preview(showBackground = true)
@Composable
fun ScrollableFocusedChildDemo() {
    val resizableState = remember { ResizableState() }

    Column {
        Text(
            "Click on the blue boxes to give them focus. Drag the handles around the black box " +
                "to change its size. Try adjusting size while the box inside the resizable area " +
                "is focused, and while it's not focused."
        )

        Row {
            Button(
                onClick = {
                    resizableState.cutInHalf()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cut in half")
            }
            Button(
                onClick = {
                    resizableState.resetToMaxSize()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Fill max size")
            }
        }

        FocusGrabber(Modifier.fillMaxWidth())

        Resizable(
            resizableState,
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                Modifier
                    .border(2.dp, Color.Black)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Box(
                    Modifier
                        .size(300.dp)
                        .background(Color.LightGray)
                        .wrapContentSize(align = Alignment.BottomEnd)
                ) {
                    FocusGrabber()
                }
            }
        }
    }
}

@Composable
fun FocusGrabber(modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    Text(
        text = if (hasFocus) "Focused" else "Click to focus",
        color = if (hasFocus) Color.White else Color.Black,
        modifier = modifier
            .clickable { focusRequester.requestFocus() }
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusRequester(focusRequester)
            .focusable()
            .border(3.dp, Color.Blue)
            .then(if (hasFocus) Modifier.background(Color.Blue) else Modifier)
            .padding(8.dp)
    )
}

private class ResizableState {
    var widthOverride by mutableStateOf(-1)
    var heightOverride by mutableStateOf(-1)

    fun resetToMaxSize() {
        widthOverride = -1
        heightOverride = -1
    }

    fun cutInHalf() {
        widthOverride /= 2
        heightOverride /= 2
    }
}

@Suppress("NAME_SHADOWING")
@Composable
private fun Resizable(
    state: ResizableState,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val handleThickness = 48.dp

    Layout(
        modifier = modifier,
        content = {
            ResizeHandle(
                orientation = Horizontal,
                onDrag = { state.heightOverride += it.roundToInt() }
            )
            ResizeHandle(
                orientation = Vertical,
                onDrag = { state.widthOverride += it.roundToInt() }
            )
            Box(propagateMinConstraints = true, content = content)
        }
    ) { measurables, constraints ->
        with(state) {
            val (horizontalHandleMeasurable, verticalHandleMeasurable, contentMeasurable) =
                measurables
            val handleThickness = handleThickness.roundToPx()
            widthOverride = if (widthOverride < 0) {
                constraints.maxWidth
            } else {
                widthOverride.coerceIn(handleThickness, constraints.maxWidth)
            }
            heightOverride = if (heightOverride < 0) {
                constraints.maxHeight
            } else {
                heightOverride.coerceIn(handleThickness, constraints.maxHeight)
            }
            val contentConstraints = Constraints.fixed(
                width = widthOverride - handleThickness,
                height = heightOverride - handleThickness
            )

            val contentPlaceable = contentMeasurable.measure(contentConstraints)
            val horizontalHandlePlaceable = horizontalHandleMeasurable.measure(
                Constraints.fixed(width = widthOverride, height = handleThickness)
            )
            val verticalHandlePlaceable = verticalHandleMeasurable.measure(
                Constraints.fixed(width = handleThickness, height = heightOverride)
            )

            layout(constraints.maxWidth, constraints.maxHeight) {
                contentPlaceable.place(IntOffset.Zero)
                horizontalHandlePlaceable.place(
                    x = 0,
                    y = contentPlaceable.height
                )
                verticalHandlePlaceable.place(
                    x = contentPlaceable.width,
                    y = 0
                )
            }
        }
    }
}

@Suppress("NAME_SHADOWING")
@Composable
private fun ResizeHandle(orientation: Orientation, onDrag: (Float) -> Unit) {
    val dragState = rememberDraggableState(onDrag)
    val lineWidth = 24.dp
    val lineSpacing = 4.dp
    val lineWeight = 1.dp

    Canvas(
        Modifier
            .fillMaxSize()
            .draggable(dragState, if (orientation == Horizontal) Vertical else Horizontal)
    ) {
        val lineWidth = lineWidth.toPx()
        val lineSpacing = lineSpacing.toPx()
        val lineWeight = lineWeight.toPx()

        if (orientation == Horizontal) {
            val startX = center.x - lineWidth / 2
            val endX = center.x + lineWidth / 2
            val y1 = center.y - lineSpacing / 2
            val y2 = center.y + lineSpacing / 2
            drawLine(Color.Black, Offset(startX, y1), Offset(endX, y1), lineWeight)
            drawLine(Color.Black, Offset(startX, y2), Offset(endX, y2), lineWeight)
        } else {
            val startY = center.y - lineWidth / 2
            val endY = center.y + lineWidth / 2
            val x1 = center.x - lineSpacing / 2
            val x2 = center.x + lineSpacing / 2
            drawLine(Color.Black, Offset(x1, startY), Offset(x1, endY), lineWeight)
            drawLine(Color.Black, Offset(x2, startY), Offset(x2, endY), lineWeight)
        }
    }
}