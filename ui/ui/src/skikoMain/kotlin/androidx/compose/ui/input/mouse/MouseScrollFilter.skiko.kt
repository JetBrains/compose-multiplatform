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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.input.mouse

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import java.awt.event.MouseWheelEvent

/**
 * Indicates distance by which we should scroll some container.
 */
@ExperimentalComposeUiApi
sealed class MouseScrollUnit {
    /**
     * Indicates that scrolling should be performed by [value] lines.
     *
     * On different platforms one tick of wheel rotation may cause different [value].
     *
     * Scrolling by one line usually means that we should scroll by some fixed offset, or by offset
     * dependent on the container's bounds (in which scroll event occurs),
     * or by one real text line in some document.
     */
    @ExperimentalComposeUiApi
    data class Line(val value: Float) : MouseScrollUnit()

    /**
     * Indicates that scrolling should be performed by [value] pages.
     *
     * Some platforms don't emit scrolling events by Page units.
     *
     * Scrolling by one page usually means that we should scroll by one container's height
     * (in which scroll event occurs), or by one real page in some document.
     */
    @ExperimentalComposeUiApi
    data class Page(val value: Float) : MouseScrollUnit()
}

/**
 * Mouse wheel or touchpad event.
 */
@ExperimentalComposeUiApi
class MouseScrollEvent(
    /**
     * Change of mouse scroll.
     *
     * Positive if scrolling down, negative if scrolling up.
     */
    val delta: MouseScrollUnit,

    /**
     * Orientation in which scrolling event occurs.
     *
     * Up/down wheel scrolling causes events in vertical orientation.
     * Left/right wheel scrolling causes events in horizontal orientation.
     */
    val orientation: MouseScrollOrientation
)

// TODO(demin): how easy-to-use scroll API should look like?
//  maybe something like Modifier.pointerScroll { delta: Offset -> } ?
//  or Modifier.pointerInput(Unit) { scroll { delta: Offset ->  } }
//  ?
/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept scroll events from mouse wheel and touchpad.
 *
 * @param onMouseScroll This callback is invoked when the user interacts with the mouse wheel or
 * touchpad.
 * While implementing this callback, return true to stop propagation of this event. If you return
 * false, the scroll event will be sent to this [mouseScrollFilter]'s parent.
 */
@ExperimentalComposeUiApi
fun Modifier.mouseScrollFilter(
    onMouseScroll: (
        /**
         * Mouse wheel or touchpad event.
         */
        event: MouseScrollEvent,

        /**
         * Bounds of the container in which scroll event occurs.
         */
        bounds: IntSize
    ) -> Boolean
): Modifier = pointerInput(onMouseScroll) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val mouseEvent = (event.mouseEvent as? MouseWheelEvent) ?: continue
            val mouseChange = event.changes.find { it.type == PointerType.Mouse }
            val isScroll = event.type == PointerEventType.Scroll
            if (isScroll && mouseChange != null && !mouseChange.isConsumed) {
                val legacyEvent = mouseEvent.toLegacyEvent(mouseChange.scrollDelta)
                if (onMouseScroll(legacyEvent, size)) {
                    mouseChange.consume()
                }
            }
        }
    }
}

private fun MouseWheelEvent.toLegacyEvent(scrollDelta: Offset): MouseScrollEvent {
    val value = if (scrollDelta.x != 0f) scrollDelta.x else scrollDelta.y
    return MouseScrollEvent(
        delta = if (scrollType == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            MouseScrollUnit.Page(value * scrollAmount)
        } else {
            MouseScrollUnit.Line(value * scrollAmount)
        },

        // There are no other way to detect horizontal scrolling in AWT
        orientation = if (isShiftDown || scrollDelta.x != 0f) {
            MouseScrollOrientation.Horizontal
        } else {
            MouseScrollOrientation.Vertical
        }
    )
}

@ExperimentalComposeUiApi
enum class MouseScrollOrientation {
    Vertical, Horizontal
}