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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.input.pointer.HandlePointerInputScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangedIgnoreConsumed
import androidx.compose.ui.util.fastAny
import kotlin.math.abs
import kotlin.math.sign

/**
 * Waits for pointer down and [ViewConfiguration.touchSlop] motion in the any direction before
 * calling [onTouchSlopReached] with the change that brings the motion over touch slop and
 * the [Offset] beyond touchSlop that has passed.
 *
 * [onTouchSlopReached] should consume the position change if it accepts the motion. If it does,
 * then the method returns. If not, touch slop detection will start fresh. This allows a
 * drag detection for a specific direction. For example, the developer may not want dragging
 * beyond a scroll position may to trigger touch slop, but dragging the other direction should
 * trigger it.
 *
 * This method returns the [PointerId] of the pointer that is driving the drag after
 * touch slop has been crossed and [onTouchSlopReached] has consumed the position change.
 *
 * @see awaitHorizontalTouchSlop
 * @see awaitVerticalTouchSlop
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.awaitTouchSlop(
    onTouchSlopReached: (change: PointerInputChange, overSlop: Offset) -> Unit
): PointerId {
    var offset = Offset.Zero
    val touchSlop = viewConfiguration.touchSlop
    return awaitTouchSlop(
        onDown = { offset = Offset.Zero },
        onDragEvent = {
            offset += it.positionChange()
            val distance = offset.getDistance()
            var acceptedDrag = false
            if (distance >= touchSlop) {
                val touchSlopOffset = offset / distance * touchSlop
                onTouchSlopReached(it, offset - touchSlopOffset)
                if (it.anyPositionChangeConsumed()) {
                    acceptedDrag = true
                } else {
                    // The position change wasn't consumed, so we continue to look for touch slop.
                    offset = Offset.Zero
                }
            }
            acceptedDrag
        }
    )
}

/**
 * Reads position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if
 * none exist, the method returns. This does not wait for touch slop.
 *
 * @return `true` if the drag completed normally or `false` if the drag motion was
 * canceled by another gesture detector consuming position change events.
 *
 * @see awaitTouchSlop
 * @see horizontalDrag
 * @see verticalDrag
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean {
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer)

        if (change.anyPositionChangeConsumed()) {
            return false
        }

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Reads pointer input events until a drag is detected or all pointers are up. When the  final
 * pointer is raised, the up event is returned. When a drag event is detected, the
 * drag change will be returned. Note that if [pointerId] has been raised, another pointer
 * that is down will be used, if available, so the returned [PointerInputChange.id] may
 * differ from [pointerId].
 *
 * @see awaitVerticalDragOrCancel
 * @see awaitHorizontalDragOrCancel
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.awaitDragOrUp(
    pointerId: PointerId,
): PointerInputChange = awaitDragOrUp(pointerId) { it.positionChangedIgnoreConsumed() }

/**
 * Gesture detector that waits for pointer down and touch slop in any direction and then
 * calls [onDrag] for each drag event. It follows the touch slop detection of
 * [awaitTouchSlop], so [onDrag] must consume the position change
 * if it wants to accept the drag motion. [onDragEnd] is called after all pointers are up
 * and [onDragCancel] is called if another gesture has consumed pointer input, canceling
 * this gesture.
 *
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 */
@ExperimentalPointerInput
suspend fun PointerInputScope.detectDragGestures(
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    forEachGesture {
        handlePointerInput {
            val pointer = awaitTouchSlop(onDrag)
            if (
                !drag(pointer) {
                    onDrag(it, it.positionChange())
                }
            ) {
                onDragCancel()
            } else {
                onDragEnd()
            }
        }
    }
}

/**
 * Waits for pointer down and [ViewConfiguration.touchSlop] motion in the vertical direction before
 * calling [onTouchSlopReached] with the change that brings the motion over touch slop and
 * the number of pixels beyond touchSlop that have passed.
 *
 * [onTouchSlopReached] should consume the position change if it accepts the motion. If it does,
 * then the method returns. If not, touch slop detection will start fresh. This allows a
 * drag detection for a specific direction. For example, the developer may not want dragging
 * beyond a scroll position may to trigger touch slop, but dragging the other direction should
 * trigger it.
 *
 * If [orientationLock] is `true`, this works with [awaitHorizontalTouchSlop] that also has
 * [orientationLock] as `true` to ensure that only horizontal or vertical dragging is done, but
 * not both. The default is `false`, so both horizontal and vertical drag gestures can operate
 * simultaneously.
 *
 * This method returns the [PointerId] of the pointer that is driving the drag after
 * touch slop has been crossed and [onTouchSlopReached] has consumed the position change.
 *
 * @see awaitHorizontalTouchSlop
 * @see detectVerticalDragGestures
 * @see awaitTouchSlop
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.awaitVerticalTouchSlop(
    orientationLock: Boolean = false,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit
) = awaitTouchSlop(
    orientationLock = orientationLock,
    onTouchSlopReached = onTouchSlopReached,
    motionFromChange = { it.positionChange().y },
    consumeMotion = { change, consumed ->
        change.consumePositionChange(0f, consumed)
    },
    crossMotionFromChange = { it.positionChange().x }
)

/**
 * Reads vertical position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if
 * none exist, the method returns. This does not wait for touch slop
 *
 * @return `true` if the vertical drag completed normally or `false` if the drag motion was
 * canceled by another gesture detector consuming position change events.
 *
 * @see awaitVerticalTouchSlop
 * @see detectVerticalDragGestures
 * @see horizontalDrag
 * @see drag
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.verticalDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean = drag(
    pointerId = pointerId,
    onDrag = onDrag,
    motionFromChange = { it.positionChangeIgnoreConsumed().y },
    motionConsumed = { it.consumed.positionChange.y != 0f }
)

/**
 * Reads pointer input events until a vertical drag is detected, the drag gesture is canceled,
 * or all pointers are up. When the final pointer is raised, the up event is returned.
 * If the drag was canceled, `null` is returned. When a vertical drag event is detected, the
 * drag change will be returned. Note that if [pointerId] has been raised, another pointer
 * that is down will be used, if available, so the returned [PointerInputChange.id] may
 * differ from [pointerId].
 *
 * @see awaitHorizontalDragOrCancel
 * @see verticalDrag
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.awaitVerticalDragOrCancel(
    pointerId: PointerId,
): PointerInputChange? {
    val change = awaitDragOrUp(pointerId) { it.positionChangeIgnoreConsumed().y != 0f }
    return if (change.consumed.positionChange.y != 0f) null else change
}

/**
 * Gesture detector that waits for pointer down and touch slop in the vertical direction and then
 * calls [onVerticalDrag] for each vertical drag event. It follows the touch slop detection of
 * [awaitVerticalTouchSlop], so [onVerticalDrag] must consume the position change
 * if it wants to accept the drag motion. [onDragEnd] is called after all pointers are up
 * and [onDragCancel] is called if another gesture has consumed pointer input, canceling
 * this gesture.
 *
 * When [orientationLock] is `true`, this gesture detector will coordinate with
 * [detectHorizontalDragGestures] and [awaitHorizontalTouchSlop] (with [orientationLock] `true`) to
 * ensure only vertical or horizontal dragging is locked, but not both.
 *
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 */
@ExperimentalPointerInput
suspend fun PointerInputScope.detectVerticalDragGestures(
    orientationLock: Boolean = false,
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) {
    forEachGesture {
        handlePointerInput {
            val pointer = awaitVerticalTouchSlop(orientationLock, onVerticalDrag)
            if (
                verticalDrag(pointer) {
                    onVerticalDrag(it, it.positionChange().y)
                }
            ) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/**
 * Waits for pointer down and [ViewConfiguration.touchSlop] motion in the horizontal direction
 * before calling [onTouchSlopReached] with the change that brings the motion over touch slop and
 * the number of pixels beyond touchSlop that have passed.
 *
 * [onTouchSlopReached] should consume the position change if it accepts the motion. If it does,
 * then the method returns. If not, touch slop detection will start fresh. This allows a
 * drag detection for a specific direction. For example, the developer may not want dragging
 * beyond a scroll position may to trigger touch slop, but dragging the other direction should
 * trigger it.
 *
 * If [orientationLock] is `true`, this works with [awaitVerticalTouchSlop] that also has
 * [orientationLock] as `true` to ensure that only horizontal or vertical dragging is done, but
 * not both. The default is `false`, so both horizontal and vertical drag gestures can operate
 * simultaneously.
 *
 * This method returns the [PointerId] of the pointer that is driving the drag after
 * touch slop has been crossed and [onTouchSlopReached] has consumed the position change.
 *
 * @see awaitVerticalTouchSlop
 * @see detectHorizontalDragGestures
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.awaitHorizontalTouchSlop(
    orientationLock: Boolean = false,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit
) = awaitTouchSlop(
    orientationLock = orientationLock,
    onTouchSlopReached = onTouchSlopReached,
    motionFromChange = { it.positionChange().x },
    consumeMotion = { change, consumed ->
        change.consumePositionChange(consumed, 0f)
    },
    crossMotionFromChange = { it.positionChange().y }
)

/**
 * Reads horizontal position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if
 * none exist, the method returns. This does not wait for touch slop.
 *
 * @see awaitHorizontalTouchSlop
 * @see detectHorizontalDragGestures
 * @see verticalDrag
 * @see drag
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.horizontalDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean = drag(
    pointerId = pointerId,
    onDrag = onDrag,
    motionFromChange = { it.positionChangeIgnoreConsumed().x },
    motionConsumed = { it.consumed.positionChange.x != 0f }
)

/**
 * Reads pointer input events until a horizontal drag is detected, the drag gesture is canceled,
 * or all pointers are up. When the final pointer is raised, the up event is returned.
 * If the drag was canceled, `null` is returned. When a horizontal drag event is detected, the
 * drag change will be returned. Note that if [pointerId] has been raised, another pointer
 * that is down will be used, if available, so the returned [PointerInputChange.id] may
 * differ from [pointerId].
 *
 * @see horizontalDrag
 * @see awaitHorizontalDragOrCancel
 */
@ExperimentalPointerInput
suspend fun HandlePointerInputScope.awaitHorizontalDragOrCancel(
    pointerId: PointerId,
): PointerInputChange? {
    val change = awaitDragOrUp(pointerId) { it.positionChangeIgnoreConsumed().x != 0f }
    return if (change.consumed.positionChange.x != 0f) null else change
}

/**
 * Gesture detector that waits for pointer down and touch slop in the horizontal direction and
 * then calls [onHorizontalDrag] for each horizontal drag event. It follows the touch slop
 * detection of [awaitHorizontalTouchSlop], so [onHorizontalDrag] must consume the position change
 * if it wants to accept the drag motion. [onDragEnd] is called after all pointers are up
 * and [onDragCancel] is called if another gesture has consumed pointer input, canceling
 * this gesture.
 *
 * When [orientationLock] is `true`, this gesture detector will coordinate with
 * [detectVerticalDragGestures] and [awaitVerticalTouchSlop] (with [orientationLock] `true`) to
 * ensure only vertical or horizontal dragging is locked, but not both.
 *
 * @see detectVerticalDragGestures
 */
@ExperimentalPointerInput
suspend fun PointerInputScope.detectHorizontalDragGestures(
    orientationLock: Boolean = false,
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) {
    forEachGesture {
        handlePointerInput {
            val pointer = awaitHorizontalTouchSlop(orientationLock, onHorizontalDrag)
            if (
                horizontalDrag(pointer) {
                    onHorizontalDrag(it, it.positionChange().x)
                }
            ) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/**
 * Continues to read drag events until all pointers are up or the drag event is canceled.
 * The initial pointer to use for driving the drag is [pointerId]. [motionFromChange]
 * converts the [PointerInputChange] to the pixel change in the direction that this
 * drag should detect. [onDrag] is called whenever the pointer moves and [motionFromChange]
 * returns non-zero.
 *
 * @return `true` when the gesture ended with all pointers up and `false` when the gesture
 * was canceled.
 */
@ExperimentalPointerInput
private suspend inline fun HandlePointerInputScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
    motionFromChange: (PointerInputChange) -> Float,
    motionConsumed: (PointerInputChange) -> Boolean
): Boolean {
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) { motionFromChange(it) != 0f }

        if (motionConsumed(change)) {
            return false
        }

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Waits for a single drag in one axis, final pointer up, or all pointers are up.
 * When [pointerId] has lifted, another pointer that is down is chosen to be the finger
 * governing the drag. When the final pointer is lifted, that [PointerInputChange] is
 * returned. When a drag is detected, that [PointerInputChange] is returned. A drag is
 * only detected when [hasDragged] returns `true`.
 */
@ExperimentalPointerInput
private suspend inline fun HandlePointerInputScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean
): PointerInputChange {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.firstOrNull { it.id == pointer }!!
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.firstOrNull { it.current.down }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}

/**
 * Waits for a down event, followed by motion beyond the [ViewConfiguration.touchSlop].
 * When detected, [onTouchSlopReached] is called with the change and the distance beyond
 * the touch slop. [motionFromChange] should return the position change in the direction that
 * matters. If [onTouchSlopReached] does not consume the position change, the touch slop
 * detection will reset and try again.
 *
 * If [orientationLock] is `true`, this works with [awaitTouchSlop] for the other axis that also has
 * [orientationLock] as `true` to ensure that only horizontal or vertical dragging is done, but
 * not both.
 *
 * @return The [PointerId] of the pointer that governs the drag.
 */
@ExperimentalPointerInput
private suspend inline fun HandlePointerInputScope.awaitTouchSlop(
    orientationLock: Boolean,
    onTouchSlopReached: (PointerInputChange, Float) -> Unit,
    motionFromChange: (PointerInputChange) -> Float,
    consumeMotion: (PointerInputChange, Float) -> Unit,
    crossMotionFromChange: (PointerInputChange) -> Float
): PointerId {
    val touchSlop = viewConfiguration.touchSlop
    var totalPositionChange: Float
    var totalCrossPositionChange: Float
    var pointer: PointerId

    do {
        val firstDown = waitForFirstDown()
        totalPositionChange = 0f
        totalCrossPositionChange = 0f

        pointer = firstDown.id
        var dragCanceled: Boolean
        var dragLocked = false
        do {
            val event = awaitPointerEvent()
            dragCanceled = false
            val dragEvent = event.changes.firstOrNull { it.id == pointer }!!
            if (dragEvent.changedToUpIgnoreConsumed()) {
                val otherDown = event.changes.firstOrNull { it.current.down }
                if (otherDown == null) {
                    // This is the last "up"
                    dragCanceled = true
                } else {
                    pointer = otherDown.id
                }
            } else {
                val positionChange = motionFromChange(dragEvent)
                totalPositionChange += positionChange
                totalCrossPositionChange += crossMotionFromChange(dragEvent)

                val inDirection = abs(totalPositionChange)
                val crossDirection = abs(totalCrossPositionChange)
                if (inDirection < touchSlop) {
                    val checkEvent = awaitPointerEvent(PointerEventPass.Final)
                    dragCanceled = anyPositionConsumed(checkEvent)
                } else {
                    if (orientationLock && crossDirection > inDirection) {
                        // Consume the position change in the direction that we care about
                        consumeMotion(dragEvent, positionChange)

                        // give the other direction a chance to consume
                        val checkEvent = awaitPointerEvent(PointerEventPass.Final)

                        // Unconsume the position change
                        consumeMotion(dragEvent, -positionChange)

                        if (anyPositionConsumed(checkEvent)) {
                            dragCanceled = true
                        }
                    }
                    if (!dragCanceled) {
                        onTouchSlopReached(
                            dragEvent,
                            totalPositionChange - (sign(totalPositionChange) * touchSlop)
                        )
                        if (dragEvent.anyPositionChangeConsumed()) {
                            dragLocked = true
                        }
                    }
                }
            }
        } while (!dragCanceled && abs(totalPositionChange) < touchSlop)
    } while (dragCanceled || !dragLocked)
    return pointer
}

/**
 * Waits for a down event, followed by some drag motion. [onDown] is called when the down
 * is detected and [onDragEvent] is called on each drag motion. If it return `true` the
 * touch slop is considered reached and the current tracking pointer is returned. If another
 * gesture has consumed the position change, the gesture is canceled and it waits for a
 * new gesture.
 */
@ExperimentalPointerInput
private suspend inline fun HandlePointerInputScope.awaitTouchSlop(
    onDown: () -> Unit,
    onDragEvent: (PointerInputChange) -> Boolean,
): PointerId {
    var pointer: PointerId

    do {
        val firstDown = waitForFirstDown()
        onDown()

        pointer = firstDown.id
        var dragCanceled: Boolean
        do {
            val event = awaitPointerEvent()
            dragCanceled = anyPositionConsumed(event)
            if (!dragCanceled) {
                val dragEvent = event.changes.firstOrNull { it.id == pointer }!!
                if (dragEvent.changedToUpIgnoreConsumed()) {
                    val otherDown = event.changes.firstOrNull { it.current.down }
                    if (otherDown == null) {
                        // This is the last "up"
                        dragCanceled = true
                    } else {
                        pointer = otherDown.id
                    }
                } else if (onDragEvent(dragEvent)) {
                    return pointer
                } else {
                    val checkConsumed = awaitPointerEvent(PointerEventPass.Final)
                    if (anyPositionConsumed(checkConsumed)) {
                        dragCanceled = true
                    }
                }
            }
        } while (!dragCanceled)
    } while (true)
}

/**
 * Return `true` when [event] has any position consumed.
 */
private fun anyPositionConsumed(event: PointerEvent): Boolean {
    return event.changes.fastAny { it.anyPositionChangeConsumed() }
}
