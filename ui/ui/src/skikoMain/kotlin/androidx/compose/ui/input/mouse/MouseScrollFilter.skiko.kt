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
@file:Suppress("DEPRECATION") // https://github.com/JetBrains/compose-jb/issues/1514

package androidx.compose.ui.input.mouse

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import java.awt.event.MouseWheelEvent

/**
 * Indicates distance by which we should scroll some container.
 */
@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll." +
        "See the comment to mouseScrollFilter"
)
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
    @Deprecated(
        "Use Modifier.pointerInput + PointerEventType.Scroll." +
            "See the comment to mouseScrollFilter"
    )
    data class Line(val value: Float) : MouseScrollUnit()

    /**
     * Indicates that scrolling should be performed by [value] pages.
     *
     * Some platforms don't emit scrolling events by Page units.
     *
     * Scrolling by one page usually means that we should scroll by one container's height
     * (in which scroll event occurs), or by one real page in some document.
     */
    @Deprecated(
        "Use Modifier.pointerInput + PointerEventType.Scroll." +
            "See the comment to mouseScrollFilter"
    )
    data class Page(val value: Float) : MouseScrollUnit()
}

/**
 * Mouse wheel or touchpad event.
 */
@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll." +
    "See the comment to mouseScrollFilter"
)
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
@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll",
    replaceWith = ReplaceWith(
        "pointerInput(Unit) { \n" +
        "     awaitPointerEventScope {\n" +
        "         while (true) {\n" +
        "             val event = awaitPointerEvent()\n" +
        "             if (event.type == PointerEventType.Scroll) {\n" +
        "                 val scrollDelta = event.changes.first().scrollDelta\n" +
        "                 val bounds = this.size\n" +
        "                 if (onMouseScroll(scrollDelta, bounds)) {\n" +
        "                      event.changes.first().consume()\n" +
        "                 }\n" +
        "             }\n" +
        "         }\n" +
        "     }\n" +
        "}",
        "androidx.compose.ui.input.pointer.pointerInput"
    )
)
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
    // we don't wrap entire loop into awaitPointerEventScope, because we want to skip
    // scroll events, which were send after the first scroll event in the current frame
    // (so there will be no more than one scroll event per frame)

    // TODO(https://github.com/JetBrains/compose-jb/issues/1345):
    //  the more proper behaviour would be to batch multiple scroll events into the single one
    while (true) {
        val event = awaitScrollEvent()
        val mouseEvent = event.mouseEvent as? MouseWheelEvent
        val mouseChange = event.changes.find { it.type == PointerType.Mouse }
        if (mouseChange != null && !mouseChange.isConsumed) {
            val legacyEvent = mouseEvent.toLegacyEvent(mouseChange.scrollDelta)
            if (onMouseScroll(legacyEvent, size)) {
                mouseChange.consume()
            }
        }
    }
}

private suspend fun PointerInputScope.awaitScrollEvent() = awaitPointerEventScope {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (event.type != PointerEventType.Scroll)
    event
}

private fun MouseWheelEvent?.toLegacyEvent(scrollDelta: Offset): MouseScrollEvent {
    val value = if (scrollDelta.x != 0f) scrollDelta.x else scrollDelta.y
    val scrollType = this?.scrollType ?: MouseWheelEvent.WHEEL_UNIT_SCROLL
    val scrollAmount = this?.scrollAmount ?: 1
    return MouseScrollEvent(
        delta = if (scrollType == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            MouseScrollUnit.Page(value * scrollAmount)
        } else {
            MouseScrollUnit.Line(value * scrollAmount)
        },
        orientation = if (scrollDelta.x != 0f) {
            MouseScrollOrientation.Horizontal
        } else {
            MouseScrollOrientation.Vertical
        }
    )
}

@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll." +
        "See the comment to mouseScrollFilter"
)
enum class MouseScrollOrientation {
    Vertical, Horizontal
}
