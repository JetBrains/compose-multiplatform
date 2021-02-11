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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.jetbrains.skija.Canvas
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal val DesktopOwnersAmbient = staticCompositionLocalOf<DesktopOwners> {
    error("CompositionLocal DesktopOwnersAmbient not provided")
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class DesktopOwners(
    coroutineScope: CoroutineScope,
    component: DesktopComponent = DummyDesktopComponent,
    invalidate: () -> Unit = {},
) {
    private val _invalidate = invalidate
    @Volatile
    private var hasPendingDraws = false

    private var invalidateScheduled = false
    private var willRenderInThisFrame = false

    fun invalidate() {
        if (!willRenderInThisFrame) {
            invalidateScheduled = true
            hasPendingDraws = true
            _invalidate()
        }
    }

    val list = LinkedHashSet<DesktopOwner>()
    var keyboard: Keyboard? = null

    private var pointerId = 0L
    private var isMousePressed = false

    private val dispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val frameClock = BroadcastFrameClock(::invalidate)
    private val coroutineContext = dispatcher + frameClock

    internal val recomposer = Recomposer(coroutineContext)
    internal val platformInputService: DesktopPlatformInput = DesktopPlatformInput(component)

    init {
        // TODO(demin): Experimental API (CoroutineStart.UNDISPATCHED).
        //  Decide what to do before release (copy paste or use different approach).
        coroutineScope.launch(coroutineContext, start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
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
        invalidate()
    }

    fun unregister(desktopOwner: DesktopOwner) {
        list.remove(desktopOwner)
        invalidate()
    }

    fun onFrame(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        invalidateScheduled = false
        willRenderInThisFrame = true

        try {
            // We must see the actual state before we will render the frame
            Snapshot.sendApplyNotifications()
            dispatcher.flush()
            frameClock.sendFrame(nanoTime)

            for (owner in list) {
                owner.setSize(width, height)
                owner.measureAndLayout()
            }
        } finally {
            willRenderInThisFrame = false
        }

        for (owner in list) {
            owner.draw(canvas)
        }

        if (frameClock.hasAwaiters) {
            _invalidate()
        }

        if (!invalidateScheduled) {
            hasPendingDraws = false
        }
    }

    val lastOwner: DesktopOwner?
        get() = list.lastOrNull()

    fun onMousePressed(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        isMousePressed = true
        lastOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
    }

    fun onMouseReleased(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        isMousePressed = false
        lastOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
        pointerId += 1
    }

    fun onMouseDragged(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        lastOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
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
        val consumed = list.lastOrNull()?.sendKeyEvent(ComposeKeyEvent(event)) ?: false
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
