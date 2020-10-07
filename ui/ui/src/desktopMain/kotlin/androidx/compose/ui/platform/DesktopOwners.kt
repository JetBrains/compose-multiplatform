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

import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.KeyEventDesktop
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputData
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.unit.Uptime
import org.jetbrains.skija.Canvas
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent

val DesktopOwnersAmbient = staticAmbientOf<DesktopOwners>()

@OptIn(InternalCoreApi::class)
class DesktopOwners(
    component: DesktopComponent = DummyDesktopComponent,
    val invalidate: () -> Unit
) {
    val list = LinkedHashSet<DesktopOwner>()
    @ExperimentalKeyInput
    var keyboard: Keyboard? = null

    private var pointerId = 0L
    private var isMousePressed = false

    internal val animationClock = DesktopAnimationClock(invalidate)
    internal val platformInputService: DesktopPlatformInput = DesktopPlatformInput(component)

    fun register(desktopOwner: DesktopOwner) {
        list.add(desktopOwner)
        invalidate()
    }

    fun unregister(desktopOwner: DesktopOwner) {
        list.remove(desktopOwner)
        invalidate()
    }

    fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        animationClock.onFrame(nanoTime)
        for (owner in list) {
            owner.setSize(width, height)
            owner.draw(canvas)
        }
    }

    val lastOwner: DesktopOwner?
        get() = list.lastOrNull()

    fun onMousePressed(x: Int, y: Int) {
        isMousePressed = true
        lastOwner?.processPointerInput(pointerInputEvent(x, y, isMousePressed))
    }

    fun onMouseReleased(x: Int, y: Int) {
        isMousePressed = false
        lastOwner?.processPointerInput(pointerInputEvent(x, y, isMousePressed))
        pointerId += 1
    }

    fun onMouseDragged(x: Int, y: Int) {
        lastOwner?.processPointerInput(pointerInputEvent(x, y, isMousePressed))
    }

    fun onMouseScroll(x: Int, y: Int, event: MouseScrollEvent) {
        val position = Offset(x.toFloat(), y.toFloat())
        lastOwner?.onMouseScroll(position, event)
    }

    fun onMouseMoved(x: Int, y: Int) {
        val position = Offset(x.toFloat(), y.toFloat())
        lastOwner?.onPointerMove(position)
    }

    private fun consumeKeyEventOr(event: KeyEvent, or: () -> Unit) {
        val consumed = list.lastOrNull()?.sendKeyEvent(KeyEventDesktop(event)) ?: false
        if (!consumed) {
            or()
        }
    }

    fun onKeyPressed(event: KeyEvent) = consumeKeyEventOr(event) {
        platformInputService.onKeyPressed(event.keyCode, event.keyChar)
    }

    fun onKeyReleased(event: KeyEvent) = consumeKeyEventOr(event) {
        platformInputService.onKeyReleased(event.keyCode, event.keyChar)
    }

    fun onKeyTyped(event: KeyEvent) = consumeKeyEventOr(event) {
        platformInputService.onKeyTyped(event.keyChar)
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
                    PointerId(pointerId),
                    PointerInputData(
                        time,
                        Offset(x.toFloat(), y.toFloat()),
                        down
                    )
                )
            )
        )
    }
}
