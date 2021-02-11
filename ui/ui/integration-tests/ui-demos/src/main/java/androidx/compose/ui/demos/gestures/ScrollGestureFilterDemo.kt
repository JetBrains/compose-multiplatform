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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple demo that shows off ScrollGestureFilter.
 */
@Composable
fun ScrollGestureFilterDemo() {
    Column {
        Text("Demonstrates scroll orientation locking")
        Text(
            "The inner box is composed inside of the outer.  If you start dragging the inner box" +
                "vertically, it will drag vertically , the same pointer will only ever allow " +
                "the box to be dragged vertically.  If drag the inner box horizontally, the " +
                "container will start being dragged horizontally and that pointer will only " +
                "ever drag horizontally."
        )
        ScrollableBox(240.dp, Orientation.Horizontal, Green, Yellow) {
            ScrollableBox(144.dp, Orientation.Vertical, Red, Blue)
        }
    }
}

@Composable
fun ScrollableBox(
    size: Dp,
    orientation: Orientation,
    activeColor: Color,
    idleColor: Color,
    content: @Composable () -> Unit = {}
) {

    val interactionSource = remember { MutableInteractionSource() }
    val color = if (interactionSource.collectIsDraggedAsState().value) activeColor else idleColor
    val offsetPx = remember { mutableStateOf(0f) }

    val offsetDp = with(LocalDensity.current) {
        offsetPx.value.toDp()
    }
    val (offsetX, offsetY) = when (orientation) {
        Orientation.Horizontal -> offsetDp to Dp.Hairline
        Orientation.Vertical -> Dp.Hairline to offsetDp
    }

    Box(
        Modifier.offset(offsetX, offsetY)
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .scrollable(
                interactionSource = interactionSource,
                orientation = orientation,
                state = rememberScrollableState { scrollDistance ->
                    offsetPx.value += scrollDistance
                    scrollDistance
                }
            )
            .size(size)
            .background(color)
    ) {
        content()
    }
}
