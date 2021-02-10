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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.window

import androidx.compose.desktop.AppFrame
import androidx.compose.desktop.AppManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import java.awt.MouseInfo

@Composable
fun WindowDraggableArea(
    modifier: Modifier = Modifier,
    content: @Composable() () -> Unit = {}
) {
    Box(
        modifier = modifier.dragGestureFilter(
            dragObserver = remember { DragHandler() },
            startDragImmediately = true
        )
    ) {
        content()
    }
}

private class DragHandler : DragObserver {

    private var location = Offset.Zero
    private var cursor = Offset.Zero
    private lateinit var window: AppFrame

    override fun onStart(downPosition: Offset) {
        if (!this::window.isInitialized) {
            window = AppManager.focusedWindow!!
        }
        location = Offset(
            window.x.toFloat(),
            window.y.toFloat()
        )
        val point = MouseInfo.getPointerInfo().getLocation()
        cursor = Offset(
            point.x.toFloat(),
            point.y.toFloat()
        )
    }

    override fun onStop(velocity: Offset) {
        location = Offset.Zero
    }

    override fun onCancel() {
        location = Offset.Zero
    }

    override fun onDrag(dragDistance: Offset): Offset {
        val point = MouseInfo.getPointerInfo().getLocation()

        window.setLocation(
            (location.x - (cursor.x - point.x)).toInt(),
            (location.y - (cursor.y - point.y)).toInt()
        )

        return dragDistance
    }
}
