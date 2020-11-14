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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.rawDragGestureFilter
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.unit.dp

/**
 * Simple [rawDragGestureFilter] demo.
 */
@Composable
fun RawDragGestureFilterDemo() {
    val offset = remember { mutableStateOf(Offset.Zero) }

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: Offset): Offset {
            offset.value += dragDistance
            return dragDistance
        }
    }

    val (offsetX, offsetY) =
        with(AmbientDensity.current) { offset.value.x.toDp() to offset.value.y.toDp() }

    Column {
        Text("Demonstrates dragging that starts immediately (no slop or anything else).")
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .offset(offsetX, offsetY)
                .preferredSize(192.dp)
                .rawDragGestureFilter(dragObserver)
                .background(Grey)
        )
    }
}
