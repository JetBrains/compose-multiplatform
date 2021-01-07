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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Uptime

// TODO(shepshapard): Document.

internal fun down(
    id: Long,
    duration: Duration = Duration.Zero,
    x: Float = 0f,
    y: Float = 0f
): PointerInputChange =
    PointerInputChange(
        PointerId(id),
        Uptime.Boot + duration,
        Offset(x, y),
        true,
        Uptime.Boot + duration,
        Offset(x, y),
        false,
        ConsumedData(Offset.Zero, false)
    )

internal fun PointerInputChange.moveTo(duration: Duration, x: Float = 0f, y: Float = 0f) =
    copy(
        previousTime = time,
        previousPressed = pressed,
        previousPosition = position,
        currentTime = Uptime.Boot + duration,
        currentPressed = true,
        currentPosition = Offset(x, y),
        consumed = ConsumedData()
    )

internal fun PointerInputChange.moveBy(duration: Duration, dx: Float = 0f, dy: Float = 0f) =
    copy(
        previousTime = time,
        previousPressed = pressed,
        previousPosition = position,
        currentTime = time + duration,
        currentPressed = true,
        currentPosition = Offset(position.x + dx, position.y + dy),
        consumed = ConsumedData()
    )

internal fun PointerInputChange.up(duration: Duration) =
    copy(
        previousTime = time,
        previousPressed = pressed,
        previousPosition = position,
        currentTime = Uptime.Boot + duration,
        currentPressed = false,
        currentPosition = position,
        consumed = ConsumedData()
    )

/**
 * A function used to react to and modify [PointerInputChange]s.
 */
internal typealias PointerInputHandler = (PointerEvent, PointerEventPass, IntSize) -> Unit

/**
 * Accepts:
 * 1. Single PointerEvent
 */
internal fun PointerInputHandler.invokeOverAllPasses(
    pointerEvent: PointerEvent,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) = invokeOverPasses(
    pointerEvent,
    listOf(
        PointerEventPass.Initial,
        PointerEventPass.Main,
        PointerEventPass.Final
    ),
    size = size
)

// TODO(shepshapard): Rename to invokeOverPass
/**
 * Accepts:
 * 1. Single PointerEvent
 * 2. Single PointerEventPass
 */
internal fun PointerInputHandler.invokeOverPass(
    pointerEvent: PointerEvent,
    pointerEventPass: PointerEventPass,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) = invokeOverPasses(pointerEvent, listOf(pointerEventPass), size)

/**
 * Accepts:
 * 1. Single PointerEvent
 * 2. vararg of PointerEventPass
 */
internal fun PointerInputHandler.invokeOverPasses(
    pointerEvent: PointerEvent,
    vararg pointerEventPasses: PointerEventPass,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) = invokeOverPasses(pointerEvent, pointerEventPasses.toList(), size)

/**
 * Accepts:
 * 1. Single PointerEvent
 * 2. List of PointerEventPass
 */
internal fun PointerInputHandler.invokeOverPasses(
    pointerEvent: PointerEvent,
    pointerEventPasses: List<PointerEventPass>,
    size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
) {
    require(pointerEvent.changes.isNotEmpty())
    require(pointerEventPasses.isNotEmpty())
    pointerEventPasses.forEach {
        this.invoke(pointerEvent, it, size)
    }
}

/**
 * Simulates the dispatching of [event] to [this] on all [PointerEventPass]es in their standard
 * order.
 *
 * @param event The event to dispatch.
 */
internal fun ((CustomEvent, PointerEventPass) -> Unit).invokeOverAllPasses(
    event: CustomEvent
) {
    invokeOverPasses(
        event,
        listOf(
            PointerEventPass.Initial,
            PointerEventPass.Main,
            PointerEventPass.Final
        )
    )
}

/**
 * Simulates the dispatching of [event] to [this] on all [PointerEventPass]es in their standard
 * order.
 *
 * @param event The event to dispatch.
 * @param pointerEventPasses The [PointerEventPass]es to pass to each call to [this].
 */
internal fun ((CustomEvent, PointerEventPass) -> Unit).invokeOverPasses(
    event: CustomEvent,
    pointerEventPasses: List<PointerEventPass>
) {
    pointerEventPasses.forEach { pass ->
        this.invoke(event, pass)
    }
}