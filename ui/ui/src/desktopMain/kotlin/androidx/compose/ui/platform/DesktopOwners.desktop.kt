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

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.skija.Canvas
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal val LocalDesktopOwners = staticCompositionLocalOf<DesktopOwners> {
    error("CompositionLocal DesktopOwnersAmbient not provided")
}

internal class DesktopOwners(
    private val coroutineScope: CoroutineScope,
    component: DesktopComponent = DummyDesktopComponent,
    private val invalidate: () -> Unit = {},
) {
    private var isInvalidationDisabled = false

    @Volatile
    private var hasPendingDraws = true
    private inline fun disableInvalidation(block: () -> Unit) {
        isInvalidationDisabled = true
        try {
            block()
        } finally {
            isInvalidationDisabled = false
        }
    }

    private fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters || list.any(DesktopOwner::needsRender)
        if (hasPendingDraws && !isInvalidationDisabled) {
            invalidate()
        }
    }

    val list = LinkedHashSet<DesktopOwner>()
    private val listCopy = mutableListOf<DesktopOwner>()

    var keyboard: Keyboard? = null

    private var pointerId = 0L
    private var isMousePressed = false

    private val dispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)
    private val coroutineContext = coroutineScope.coroutineContext + dispatcher + frameClock

    internal val recomposer = Recomposer(coroutineContext)
    internal val platformInputService: DesktopPlatformInput = DesktopPlatformInput(component)

    init {
        coroutineScope.launch(coroutineContext, start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    private fun dispatchCommand(command: () -> Unit) {
        coroutineScope.launch(coroutineContext) {
            command()
        }
    }

    /**
     * Returns true if there are pending recompositions, draws or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations() = hasPendingDraws ||
        recomposer.hasPendingWork ||
        dispatcher.hasTasks()

    fun register(desktopOwner: DesktopOwner) {
        list.add(desktopOwner)
        desktopOwner.onNeedsRender = ::invalidateIfNeeded
        desktopOwner.onDispatchCommand = ::dispatchCommand
        invalidateIfNeeded()
    }

    fun unregister(desktopOwner: DesktopOwner) {
        list.remove(desktopOwner)
        desktopOwner.onDispatchCommand = null
        desktopOwner.onNeedsRender = null
        invalidateIfNeeded()
    }

    fun onFrame(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        disableInvalidation {
            // We must see the actual state before we will render the frame
            Snapshot.sendApplyNotifications()
            dispatcher.flush()
            frameClock.sendFrame(nanoTime)

            listCopy.addAll(list)
            for (owner in listCopy) {
                owner.render(canvas, width, height)
            }
            listCopy.clear()
        }

        invalidateIfNeeded()
    }

    internal var focusedOwner: DesktopOwner? = null
    private val hoveredOwner: DesktopOwner?
        get() {
            listCopy.addAll(list)
            for (i in (listCopy.size - 1) downTo 0) {
                val owner = listCopy[i]
                if (owner.isHovered(pointLocation)) {
                    listCopy.clear()
                    return owner
                }
            }
            listCopy.clear()
            return list.lastOrNull()
        }

    fun onMousePressed(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        isMousePressed = true
        val currentOwner = hoveredOwner
        if (currentOwner != null) {
            if (currentOwner.isFocusable && focusedOwner != currentOwner) {
                focusedOwner?.onDismissRequest?.invoke()
                focusedOwner = currentOwner
            } else {
                currentOwner.processPointerInput(
                    pointerInputEvent(nativeEvent, x, y, isMousePressed)
                )
                return
            }
        }
        focusedOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
    }

    fun onMouseReleased(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        isMousePressed = false
        val currentOwner = hoveredOwner
        if (currentOwner != null) {
            if (currentOwner.isFocusable) {
                focusedOwner = currentOwner
            } else {
                currentOwner.processPointerInput(
                    pointerInputEvent(nativeEvent, x, y, isMousePressed)
                )
                return
            }
        }
        focusedOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
        pointerId += 1
    }

    private var pointLocation = IntOffset.Zero

    fun onMouseMoved(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        pointLocation = IntOffset(x, y)
        val event = pointerInputEvent(nativeEvent, x, y, isMousePressed)
        val result = hoveredOwner?.processPointerInput(event)
        if (result?.anyMovementConsumed != true) {
            val position = Offset(x.toFloat(), y.toFloat())
            hoveredOwner?.onPointerMove(position)
        }
    }

    fun onMouseScroll(x: Int, y: Int, event: MouseScrollEvent) {
        val position = Offset(x.toFloat(), y.toFloat())
        hoveredOwner?.onMouseScroll(position, event)
    }

    fun onMouseEntered(x: Int, y: Int) {
        val position = Offset(x.toFloat(), y.toFloat())
        hoveredOwner?.onPointerEnter(position)
    }

    fun onMouseExited() {
        hoveredOwner?.onPointerExit()
    }

    private fun consumeKeyEvent(event: KeyEvent) {
        focusedOwner?.sendKeyEvent(ComposeKeyEvent(event))
    }

    fun onKeyPressed(event: KeyEvent) = consumeKeyEvent(event)

    fun onKeyReleased(event: KeyEvent) = consumeKeyEvent(event)

    fun onKeyTyped(event: KeyEvent) = consumeKeyEvent(event)

    fun onInputMethodEvent(event: InputMethodEvent) {
        if (!event.isConsumed()) {
            when (event.id) {
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED -> {
                    platformInputService.replaceInputMethodText(event)
                    event.consume()
                }
                InputMethodEvent.CARET_POSITION_CHANGED -> {
                    platformInputService.inputMethodCaretPositionChanged(event)
                    event.consume()
                }
            }
        }
    }

    private fun pointerInputEvent(
        nativeEvent: MouseEvent?,
        x: Int,
        y: Int,
        down: Boolean
    ): PointerInputEvent {
        val time = System.nanoTime() / 1_000_000L
        val position = Offset(x.toFloat(), y.toFloat())
        return PointerInputEvent(
            time,
            listOf(
                PointerInputEventData(
                    PointerId(pointerId),
                    time,
                    position,
                    position,
                    down,
                    PointerType.Mouse
                )
            ),
            nativeEvent
        )
    }
}
