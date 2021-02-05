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

package androidx.compose.ui.input.mouse

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.unit.IntSize

/**
 * Indicates distance by which we should scroll some container.
 */
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
    data class Line(val value: Float) : MouseScrollUnit()

    /**
     * Indicates that scrolling should be performed by [value] pages.
     *
     * Some platforms don't emit scrolling events by Page units.
     *
     * Scrolling by one page usually means that we should scroll by one container's height
     * (in which scroll event occurs), or by one real page in some document.
     */
    data class Page(val value: Float) : MouseScrollUnit()
}

/**
 * Mouse wheel or touchpad event.
 */
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
    val orientation: Orientation
)

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept scroll events from mouse wheel and touchpad.
 *
 * @param onMouseScroll This callback is invoked when the user interacts with the mouse wheel or
 * touchpad.
 * While implementing this callback, return true to stop propagation of this event. If you return
 * false, the scroll event will be sent to this [mouseScrollFilter]'s parent.
 */
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
): Modifier = composed {
    val filter = remember(::MouseScrollEventFilter)
    filter.onMouseScroll = onMouseScroll
    MousePointerInputModifierImpl(filter)
}

internal class MouseScrollEventFilter : PointerInputFilter() {
    lateinit var onMouseScroll: (MouseScrollEvent, IntSize) -> Boolean

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) = Unit

    override fun onCancel() = Unit

    fun onMouseScroll(event: MouseScrollEvent): Boolean {
        return isAttached && onMouseScroll(event, size)
    }
}

private data class MousePointerInputModifierImpl(
    override val pointerInputFilter: PointerInputFilter
) : PointerInputModifier