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
package androidx.compose.ui.platform

import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.dispatch.DesktopUiDispatcher
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputData
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.unit.Uptime
import org.jetbrains.skija.Canvas
import java.awt.Component
import java.awt.event.InputMethodEvent
import java.awt.im.InputMethodRequests

val DesktopOwnersAmbient = staticAmbientOf<DesktopOwners>()

@OptIn(InternalCoreApi::class)
class DesktopOwners(
    component: Component,
    private val redraw: () -> Unit
) {
    private val list = LinkedHashSet<DesktopOwner>()

    // Optimization: we don't need more than one redrawing per tick
    private var redrawingScheduled = false

    private var isMousePressed = false

    internal val platformInputService: DesktopPlatformInput = DesktopPlatformInput(component)

    fun register(desktopOwner: DesktopOwner) {
        list.add(desktopOwner)
        invalidate()
    }

    fun unregister(desktopOwner: DesktopOwner) {
        list.remove(desktopOwner)
        invalidate()
    }

    fun onRender(canvas: Canvas, width: Int, height: Int) {
        for (owner in list) {
            owner.setSize(width, height)
            owner.draw(canvas)
        }
    }

    fun onMousePressed(x: Int, y: Int) {
        isMousePressed = true
        list.lastOrNull()?.processPointerInput(pointerInputEvent(x, y, isMousePressed))
    }

    fun onMouseReleased(x: Int, y: Int) {
        isMousePressed = false
        list.lastOrNull()?.processPointerInput(pointerInputEvent(x, y, isMousePressed))
    }

    fun onMouseDragged(x: Int, y: Int) {
        list.lastOrNull()?.processPointerInput(pointerInputEvent(x, y, isMousePressed))
    }

    fun onKeyPressed(code: Int, char: Char) {
        platformInputService.onKeyPressed(code, char)
    }

    fun onKeyReleased(code: Int, char: Char) {
        platformInputService.onKeyReleased(code, char)
    }

    fun onKeyTyped(char: Char) {
        platformInputService.onKeyTyped(char)
    }

    fun getInputMethodRequests(): InputMethodRequests? {
        return platformInputService.getInputMethodRequests()
    }

    fun onInputMethodTextChanged(event: InputMethodEvent) {
        platformInputService.onInputMethodTextChanged(event)
    }

    private fun pointerInputEvent(x: Int, y: Int, down: Boolean): PointerInputEvent {
        val time = Uptime(System.nanoTime())
        return PointerInputEvent(
            time,
            listOf(
                PointerInputEventData(
                    PointerId(0),
                    PointerInputData(
                        time,
                        Offset(x.toFloat(), y.toFloat()),
                        down
                    )
                )
            )
        )
    }

    fun invalidate() {
        if (!redrawingScheduled) {
            DesktopUiDispatcher.Dispatcher.scheduleAfterCallback {
                redrawingScheduled = false
                if (Recomposer.current().hasPendingChanges()) {
                    invalidate()
                } else {
                    redraw()
                }
            }
            redrawingScheduled = true
        }
    }
}