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

package androidx.compose.ui.window

import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.LocalAppWindow
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.MouseInfo
import java.awt.Point

/**
 * WindowDraggableArea is a component that allows you to drag the window using the mouse.
 *
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun WindowDraggableArea(
    modifier: Modifier = Modifier,
    content: @Composable() () -> Unit = {}
) {
    val window = LocalAppWindow.current
    val handler = remember { DragHandler(window) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    awaitFirstDown()
                    handler.register()
                }
            }
        }
    ) {
        content()
    }
}

private class DragHandler(private val window: AppWindow) {
    private var location = window.window.location.toComposeOffset()
    private var pointStart = MouseInfo.getPointerInfo().location.toComposeOffset()

    private val dragListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) = drag()
    }
    private val removeListener = object : MouseAdapter() {
        override fun mouseReleased(event: MouseEvent) {
            window.removeMouseMotionListener(dragListener)
            window.removeMouseListener(this)
        }
    }

    fun register() {
        location = window.window.location.toComposeOffset()
        pointStart = MouseInfo.getPointerInfo().location.toComposeOffset()
        window.addMouseListener(removeListener)
        window.addMouseMotionListener(dragListener)
    }

    private fun drag() {
        val point = MouseInfo.getPointerInfo().location.toComposeOffset()
        val location = location + (point - pointStart)
        window.setLocation(location.x, location.y)
    }

    private fun Point.toComposeOffset() = IntOffset(x, y)
}
