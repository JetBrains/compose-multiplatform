/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.lerp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.ui.test.internal.JvmDefaultWithCompatibility

/**
 * The receiver scope of the touch input injection lambda from [performTouchInput].
 *
 * The functions in [TouchInjectionScope] can roughly be divided into two groups: full gestures
 * and individual touch events. The individual touch events are: [down], [move] and friends, [up],
 * [cancel] and [advanceEventTime]. Full gestures are all the other functions, like
 * [click][TouchInjectionScope.click], [doubleClick][TouchInjectionScope.doubleClick],
 * [swipe][TouchInjectionScope.swipe], etc. These are built on top of the individual events and
 * serve as a good example on how you can build your own full gesture functions.
 *
 * A touch gesture is started with a [down] event, followed by a sequence of [move] events and
 * finally an [up] event, optionally combined with more sets of [down] and [up] events for
 * multi-touch gestures. Most methods accept a pointerId to specify which pointer (finger) the
 * event applies to. Movement can be expressed absolutely with [moveTo] and [updatePointerTo], or
 * relative to the current pointer position with [moveBy] and [updatePointerBy]. The `moveTo/By`
 * methods enqueue an event immediately, while the `updatePointerTo/By` methods don't. This
 * allows you to update the position of multiple pointers in a single [move] event for
 * multi-touch gestures. Touch gestures can be cancelled with [cancel]. All events, regardless
 * the method used, will always contain the current position of _all_ pointers.
 *
 * The entire event injection state is shared between all `perform.*Input` methods, meaning you
 * can continue an unfinished touch gesture in a subsequent invocation of [performTouchInput] or
 * [performMultiModalInput]. Note however that while the pointer positions are retained across
 * invocation of `perform.*Input` methods, they are always manipulated in the current node's
 * local coordinate system. That means that two subsequent invocations of [performTouchInput] on
 * different nodes will report a different [currentPosition], even though it is actually the same
 * position on the screen.
 *
 * All events sent by these methods are batched together and sent as a whole after
 * [performTouchInput] has executed its code block. Because gestures don't have to be defined all
 * in the same [performTouchInput] block, keep in mind that while the gesture is not complete,
 * all code you execute in between these blocks will be executed while imaginary fingers are
 * actively touching the screen. The events sent as part of the same batch will not be interrupted
 * by recomposition, however, if a gesture spans multiple [performTouchInput] blocks it is
 * important to remember that recomposition, layout and drawing could take place during the
 * gesture, which may lead to events being injected into a moving target. As pointer positions are
 * manipulated in the current node's local coordinate system, this could lead to issues caused by
 * the fact that part of the gesture will take effect before the rest of the events have been
 * enqueued.
 *
 * Example of performing a click:
 * @sample androidx.compose.ui.test.samples.touchInputClick
 *
 * Example of performing a swipe up:
 * @sample androidx.compose.ui.test.samples.touchInputSwipeUp
 *
 * Example of performing an L-shaped gesture:
 * @sample androidx.compose.ui.test.samples.touchInputLShapedGesture
 *
 * @see InjectionScope
 */
@JvmDefaultWithCompatibility
interface TouchInjectionScope : InjectionScope {
    /**
     * Returns the current position of the given [pointerId]. The default [pointerId] is 0. The
     * position is returned in the local coordinate system of the node with which we're
     * interacting. (0, 0) is the top left corner of the node.
     */
    fun currentPosition(pointerId: Int = 0): Offset?

    /**
     * Sends a down event for the pointer with the given [pointerId] at [position] on the
     * associated node. The [position] is in the node's local coordinate system, where (0, 0) is
     * the top left corner of the node.
     *
     * If no pointers are down yet, this will start a new touch gesture. If a gesture is already
     * in progress, this event is sent at the same timestamp as the last event. If the given
     * pointer is already down, an [IllegalArgumentException] will be thrown.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The position of the down event, in the node's local coordinate system
     */
    fun down(pointerId: Int, position: Offset)

    /**
     * Sends a down event for the default pointer at [position] on the associated node. The
     * [position] is in the node's local coordinate system, where (0, 0) is the top left corner
     * of the node. The default pointer has `pointerId = 0`.
     *
     * If no pointers are down yet, this will start a new touch gesture. If a gesture is already
     * in progress, this event is sent at the same timestamp as the last event. If the default
     * pointer is already down, an [IllegalArgumentException] will be thrown.
     *
     * @param position The position of the down event, in the node's local coordinate system
     */
    fun down(position: Offset) {
        down(0, position)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with
     * the position of the pointer with the given [pointerId] updated to [position]. The
     * [position] is in the node's local coordinate system, where (0, 0) is the top left corner
     * of the node.
     *
     * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param position The new position of the pointer, in the node's local coordinate system
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun moveTo(pointerId: Int, position: Offset, delayMillis: Long = eventPeriodMillis) {
        updatePointerTo(pointerId, position)
        move(delayMillis)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with
     * the position of the default pointer updated to [position]. The [position] is in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node. The default
     * pointer has `pointerId = 0`.
     *
     * If the default pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param position The new position of the pointer, in the node's local coordinate system
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun moveTo(position: Offset, delayMillis: Long = eventPeriodMillis) {
        moveTo(0, position, delayMillis)
    }

    /**
     * Updates the position of the pointer with the given [pointerId] to the given [position], but
     * does not send a move event. The move event can be sent with [move]. The [position] is in
     * the node's local coordinate system, where (0.px, 0.px) is the top left corner of the
     * node.
     *
     * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param position The new position of the pointer, in the node's local coordinate system
     */
    fun updatePointerTo(pointerId: Int, position: Offset)

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with
     * the position of the pointer with the given [pointerId] moved by the given [delta].
     *
     * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param delta The position for this move event, relative to the current position of the
     * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     * x-position, and subtract 10.px from the pointer's y-position.
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun moveBy(pointerId: Int, delta: Offset, delayMillis: Long = eventPeriodMillis) {
        updatePointerBy(pointerId, delta)
        move(delayMillis)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with
     * the position of the default pointer moved by the given [delta]. The default pointer has
     * `pointerId = 0`.
     *
     * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param delta The position for this move event, relative to the current position of the
     * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     * x-position, and subtract 10.px from the pointer's y-position.
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun moveBy(delta: Offset, delayMillis: Long = eventPeriodMillis) {
        moveBy(0, delta, delayMillis)
    }

    /**
     * Updates the position of the pointer with the given [pointerId] by the given [delta], but
     * does not send a move event. The move event can be sent with [move].
     *
     * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param delta The position for this move event, relative to the last sent position of the
     * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     * x-position, and subtract 10.px from the pointer's y-position.
     */
    fun updatePointerBy(pointerId: Int, delta: Offset) {
        // Ignore currentPosition of null here, let updatePointerTo generate the error
        val position = (currentPosition(pointerId) ?: Offset.Zero) + delta
        updatePointerTo(pointerId, position)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event without updating any of the
     * pointer positions. This can be useful when batching movement of multiple pointers
     * together, which can be done with [updatePointerTo] and [updatePointerBy].
     *
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun move(delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a move event [delayMillis] after the last sent event without updating any of the
     * pointer positions.
     *
     * This overload supports gestures with multiple pointers.
     *
     * @param relativeHistoricalTimes Time of each historical event, as a millisecond relative to
     * the time the actual event is sent. For example, -10L means 10ms earlier.
     * @param historicalCoordinates Coordinates of each historical event, in the same coordinate
     * space as [moveTo]. The outer list must have the same size as the number of pointers in the
     * event, and each inner list must have the same size as [relativeHistoricalTimes].
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    @ExperimentalTestApi
    fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long = eventPeriodMillis
    )

    /**
     * Sends a move event [delayMillis] after the last sent event without updating any of the
     * pointer positions.
     *
     * This overload is a convenience method for the common case where the gesture only has one
     * pointer.
     *
     * @param relativeHistoricalTimes Time of each historical event, as a millisecond relative to
     * the time the actual event is sent. For example, -10L means 10ms earlier.
     * @param historicalCoordinates Coordinates of each historical event, in the same coordinate
     * space as [moveTo]. The list must have the same size as [relativeHistoricalTimes].
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    @ExperimentalTestApi
    fun moveWithHistory(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<Offset>,
        delayMillis: Long = eventPeriodMillis
    ) = moveWithHistoryMultiPointer(
        relativeHistoricalTimes,
        listOf(historicalCoordinates),
        delayMillis
    )

    /**
     * Sends an up event for the pointer with the given [pointerId], or the default pointer if
     * [pointerId] is omitted, on the associated node.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [down]
     */
    fun up(pointerId: Int = 0)

    /**
     * Sends a cancel event [delayMillis] after the last sent event to cancel the current
     * gesture. The cancel event contains the current position of all active pointers.
     *
     * @param delayMillis The time between the last sent event and this event.
     * [eventPeriodMillis] by default.
     */
    fun cancel(delayMillis: Long = eventPeriodMillis)
}

internal class TouchInjectionScopeImpl(
    private val baseScope: MultiModalInjectionScopeImpl
) : TouchInjectionScope, InjectionScope by baseScope {
    private val inputDispatcher get() = baseScope.inputDispatcher
    private fun localToRoot(position: Offset) = baseScope.localToRoot(position)

    override fun currentPosition(pointerId: Int): Offset? {
        val positionInRoot = inputDispatcher.getCurrentTouchPosition(pointerId) ?: return null
        return baseScope.rootToLocal(positionInRoot)
    }

    override fun down(pointerId: Int, position: Offset) {
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueTouchDown(pointerId, positionInRoot)
    }

    override fun updatePointerTo(pointerId: Int, position: Offset) {
        val positionInRoot = localToRoot(position)
        inputDispatcher.updateTouchPointer(pointerId, positionInRoot)
    }

    override fun move(delayMillis: Long) {
        advanceEventTime(delayMillis)
        inputDispatcher.enqueueTouchMove()
    }

    @ExperimentalTestApi
    override fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long
    ) {
        repeat(relativeHistoricalTimes.size) {
            check(relativeHistoricalTimes[it] < 0) {
                "Relative historical times should be negative, in order to be in the past" +
                    "(offset $it was: ${relativeHistoricalTimes[it]})"
            }
            check(relativeHistoricalTimes[it] >= -delayMillis) {
                "Relative historical times should not be earlier than the previous event " +
                    "(offset $it was: ${relativeHistoricalTimes[it]}, ${-delayMillis})"
            }
        }

        advanceEventTime(delayMillis)
        inputDispatcher.enqueueTouchMoves(relativeHistoricalTimes, historicalCoordinates)
    }

    override fun up(pointerId: Int) {
        inputDispatcher.enqueueTouchUp(pointerId)
    }

    override fun cancel(delayMillis: Long) {
        advanceEventTime(delayMillis)
        inputDispatcher.enqueueTouchCancel()
    }
}

/**
 * Performs a click gesture (aka a tap) on the associated node.
 *
 * The click is done at the given [position], or in the [center] if the [position] is omitted.
 * The [position] is in the node's local coordinate system, where (0, 0) is the top left corner
 * of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used.
 */
fun TouchInjectionScope.click(position: Offset = center) {
    down(position)
    move()
    up()
}

/**
 * Performs a long click gesture (aka a long press) on the associated node.
 *
 * The long click is done at the given [position], or in the [center] if the [position] is
 * omitted. By default, the [durationMillis] of the press is 100ms longer than the minimum
 * required duration for a long press. The [position] is in the node's local coordinate system,
 * where (0, 0) is the top left corner of the node.
 *
 * @param position The position of the long click, in the node's local coordinate system. If
 * omitted, the [center] of the node will be used.
 * @param durationMillis The time between the down and the up event
 */
fun TouchInjectionScope.longClick(
    position: Offset = center,
    durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
) {
    require(durationMillis >= viewConfiguration.longPressTimeoutMillis) {
        "Long click must have a duration of at least ${viewConfiguration.longPressTimeoutMillis}ms"
    }
    swipe(position, position, durationMillis)
}

// The average of min and max is a safe default
private val ViewConfiguration.defaultDoubleTapDelayMillis: Long
    get() = (doubleTapMinTimeMillis + doubleTapTimeoutMillis) / 2

/**
 * Performs a double click gesture (aka a double tap) on the associated node.
 *
 * The double click is done at the given [position] or in the [center] if the [position] is
 * omitted. By default, the [delayMillis] between the first and the second click is half way in
 * between the minimum and maximum required delay for a double click. The [position] is in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 *
 * @param position The position of the double click, in the node's local coordinate system.
 * If omitted, the [center] position will be used.
 * @param delayMillis The time between the up event of the first click and the down event of the
 * second click
 */
fun TouchInjectionScope.doubleClick(
    position: Offset = center,
    delayMillis: Long = viewConfiguration.defaultDoubleTapDelayMillis
) {
    require(delayMillis >= viewConfiguration.doubleTapMinTimeMillis) {
        "Time between clicks in double click must be at least " +
            "${viewConfiguration.doubleTapMinTimeMillis}ms"
    }
    require(delayMillis < viewConfiguration.doubleTapTimeoutMillis) {
        "Time between clicks in double click must be smaller than " +
            "${viewConfiguration.doubleTapTimeoutMillis}ms"
    }
    click(position)
    advanceEventTime(delayMillis)
    click(position)
}

/**
 * Performs a swipe gesture on the associated node.
 *
 * The motion events are linearly interpolated between [start] and [end]. The coordinates are in
 * the node's local coordinate system, where (0, 0) is the top left corner of the node. The
 * default duration is 200 milliseconds.
 *
 * @param start The start position of the gesture, in the node's local coordinate system
 * @param end The end position of the gesture, in the node's local coordinate system
 * @param durationMillis The duration of the gesture
 */
fun TouchInjectionScope.swipe(
    start: Offset,
    end: Offset,
    durationMillis: Long = 200
) {
    val durationFloat = durationMillis.toFloat()
    swipe(
        curve = { lerp(start, end, it / durationFloat) },
        durationMillis = durationMillis
    )
}

/**
 * Performs a swipe gesture on the associated node.
 *
 * The swipe follows the [curve] from 0 till [durationMillis]. Will force sampling of an event at
 * all times defined in [keyTimes]. The time between events is kept as close to
 * [eventPeriodMillis][InjectionScope.eventPeriodMillis] as possible, given the constraints. The
 * coordinates are in the node's local coordinate system, where (0, 0) is the top left corner of
 * the node. The default duration is 200 milliseconds.
 *
 * @param curve The function that defines the position of the gesture over time
 * @param durationMillis The duration of the gesture
 * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
 * be sampled
 */
fun TouchInjectionScope.swipe(
    curve: (Long) -> Offset,
    durationMillis: Long,
    keyTimes: List<Long> = emptyList()
) {
    @OptIn(ExperimentalTestApi::class)
    multiTouchSwipe(listOf(curve), durationMillis, keyTimes)
}

/**
 * Performs a multi touch swipe gesture on the associated node.
 *
 * Each pointer follows [curves]&#91;i] from 0 till [durationMillis]. Sampling of an event is
 * forced at all times defined in [keyTimes]. The time between events is kept as close to
 * [eventPeriodMillis][InjectionScope.eventPeriodMillis] as possible, given the constraints. The
 * coordinates are in the node's local coordinate system, where (0, 0) is the top left corner of
 * the node. The default duration is 200 milliseconds.
 *
 * Will stay experimental until support has been added to start and end each pointer at
 * different times.
 *
 * @param curves The functions that define the position of the gesture over time
 * @param durationMillis The duration of the gesture
 * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
 * be sampled
 */
@ExperimentalTestApi
fun TouchInjectionScope.multiTouchSwipe(
    curves: List<(Long) -> Offset>,
    durationMillis: Long,
    keyTimes: List<Long> = emptyList()
) {
    val startTime = 0L
    val endTime = durationMillis

    // Validate input
    require(durationMillis >= 1) {
        "duration must be at least 1 millisecond, not $durationMillis"
    }
    val validRange = startTime..endTime
    require(keyTimes.all { it in validRange }) {
        "keyTimes contains timestamps out of range [$startTime..$endTime]: $keyTimes"
    }
    require(keyTimes.asSequence().zipWithNext { a, b -> a <= b }.all { it }) {
        "keyTimes must be sorted: $keyTimes"
    }

    // Send down events
    curves.forEachIndexed { i, curve ->
        down(i, curve(startTime))
    }

    // Send move events between each consecutive pair in [t0, ..keyTimes, tN]
    var currTime = startTime
    var key = 0
    while (currTime < endTime) {
        // advance key
        while (key < keyTimes.size && keyTimes[key] <= currTime) {
            key++
        }
        // send events between t and next keyTime
        val tNext = if (key < keyTimes.size) keyTimes[key] else endTime
        sendMultiTouchSwipeSegment(curves, currTime, tNext)
        currTime = tNext
    }

    // And end with up events
    repeat(curves.size) {
        up(it)
    }
}

/**
 * Generates move events between `f([t0])` and `f([tN])` during the time window `(t0, tN]`, for
 * each `f` in [fs], following the curves defined by each `f`. The number of events sent
 * (#numEvents) is such that the time between each event is as close to
 * [eventPeriodMillis][InputDispatcher.eventPeriodMillis] as possible, but at least 1. The first
 * event is sent at time `downTime + (tN - t0) / #numEvents`, the last event is sent at time tN.
 *
 * @param fs The functions that define the coordinates of the respective gestures over time
 * @param t0 The start time of this segment of the swipe, in milliseconds relative to downTime
 * @param tN The end time of this segment of the swipe, in milliseconds relative to downTime
 */
private fun TouchInjectionScope.sendMultiTouchSwipeSegment(
    fs: List<(Long) -> Offset>,
    t0: Long,
    tN: Long
) {
    var step = 0
    // How many steps will we take between t0 and tN? At least 1, and a number that will
    // bring as as close to eventPeriod as possible
    val steps = max(1, ((tN - t0) / eventPeriodMillis.toFloat()).roundToInt())

    var tPrev = t0
    while (step++ < steps) {
        val progress = step / steps.toFloat()
        val t = lerp(t0, tN, progress)
        fs.forEachIndexed { i, f ->
            updatePointerTo(i, f(t))
        }
        move(t - tPrev)
        tPrev = t
    }
}

/**
 * Performs a pinch gesture on the associated node.
 *
 * For each pair of start and end [Offset]s, the motion events are linearly interpolated. The
 * coordinates are in the node's local coordinate system where (0, 0) is the top left corner of
 * the node. The default duration is 400 milliseconds.
 *
 * @param start0 The start position of the first gesture in the node's local coordinate system
 * @param end0 The end position of the first gesture in the node's local coordinate system
 * @param start1 The start position of the second gesture in the node's local coordinate system
 * @param end1 The end position of the second gesture in the node's local coordinate system
 * @param durationMillis the duration of the gesture
 */
fun TouchInjectionScope.pinch(
    start0: Offset,
    end0: Offset,
    start1: Offset,
    end1: Offset,
    durationMillis: Long = 400
) {
    val durationFloat = durationMillis.toFloat()
    @OptIn(ExperimentalTestApi::class)
    multiTouchSwipe(
        listOf(
            { lerp(start0, end0, it / durationFloat) },
            { lerp(start1, end1, it / durationFloat) }
        ),
        durationMillis
    )
}

/**
 * Performs a swipe gesture on the associated node such that it ends with the given [endVelocity].
 *
 * The swipe will go through [start] at t=0 and through [end] at t=[durationMillis]. In between,
 * the swipe will go monotonically from [start] and [end], but not strictly. Due to imprecision,
 * no guarantees can be made for the actual velocity at the end of the gesture, but generally it
 * is within 0.1 of the desired velocity.
 *
 * When a swipe cannot be created that results in the desired velocity (because the input is too
 * restrictive), an exception will be thrown with suggestions to fix the input.
 *
 * The coordinates are in the node's local coordinate system, where (0, 0) is the top left corner
 * of the node. The default duration is calculated such that a feasible swipe can be created that
 * ends in the given velocity.
 *
 * @param start The start position of the gesture, in the node's local coordinate system
 * @param end The end position of the gesture, in the node's local coordinate system
 * @param endVelocity The velocity of the gesture at the moment it ends in px/second. Must be
 * positive.
 * @param durationMillis The duration of the gesture in milliseconds. Must be long enough that at
 * least 3 input events are generated, which happens with a duration of 40ms or more. If omitted,
 * a duration is calculated such that a valid swipe with velocity can be created.
 *
 * @throws IllegalArgumentException When no swipe can be generated that will result in the desired
 * velocity. The error message will suggest changes to the input parameters such that a swipe
 * will become feasible.
 */
fun TouchInjectionScope.swipeWithVelocity(
    start: Offset,
    end: Offset,
    /*@FloatRange(from = 0.0)*/
    endVelocity: Float,
    durationMillis: Long = VelocityPathFinder.calculateDefaultDuration(start, end, endVelocity)
) {
    require(endVelocity >= 0f) {
        "Velocity cannot be $endVelocity, it must be positive"
    }
    require(eventPeriodMillis < 40) {
        "InputDispatcher.eventPeriod must be smaller than 40ms in order to generate velocities"
    }
    val minimumDuration = ceil(2.5f * eventPeriodMillis).roundToLong()
    require(durationMillis >= minimumDuration) {
        "Duration must be at least ${minimumDuration}ms because " +
            "velocity requires at least 3 input events"
    }

    val pathFinder = VelocityPathFinder(start, end, endVelocity, durationMillis)
    swipe(pathFinder.generateFunction(), durationMillis)
}

/**
 * Performs a swipe up gesture along `x = [centerX]` of the associated node, from [startY] till
 * [endY], taking [durationMillis] milliseconds.
 *
 * @param startY The y-coordinate of the start of the swipe. Must be greater than or equal to the
 * [endY]. By default the [bottom] of the node.
 * @param endY The y-coordinate of the end of the swipe. Must be less than or equal to the
 * [startY]. By default the [top] of the node.
 * @param durationMillis The duration of the swipe. By default 200 milliseconds.
 */
fun TouchInjectionScope.swipeUp(
    startY: Float = bottom,
    endY: Float = top,
    durationMillis: Long = 200
) {
    require(startY >= endY) {
        "startY=$startY needs to be greater than or equal to endY=$endY"
    }
    val start = Offset(centerX, startY)
    val end = Offset(centerX, endY)
    swipe(start, end, durationMillis)
}

/**
 * Performs a swipe down gesture along `x = [centerX]` of the associated node, from [startY] till
 * [endY], taking [durationMillis] milliseconds.
 *
 * @param startY The y-coordinate of the start of the swipe. Must be less than or equal to the
 * [endY]. By default the [top] of the node.
 * @param endY The y-coordinate of the end of the swipe. Must be greater than or equal to the
 * [startY]. By default the [bottom] of the node.
 * @param durationMillis The duration of the swipe. By default 200 milliseconds.
 */
fun TouchInjectionScope.swipeDown(
    startY: Float = top,
    endY: Float = bottom,
    durationMillis: Long = 200
) {
    require(startY <= endY) {
        "startY=$startY needs to be less than or equal to endY=$endY"
    }
    val start = Offset(centerX, startY)
    val end = Offset(centerX, endY)
    swipe(start, end, durationMillis)
}

/**
 * Performs a swipe left gesture along `y = [centerY]` of the associated node, from [startX] till
 * [endX], taking [durationMillis] milliseconds.
 *
 * @param startX The x-coordinate of the start of the swipe. Must be greater than or equal to the
 * [endX]. By default the [right] of the node.
 * @param endX The x-coordinate of the end of the swipe. Must be less than or equal to the
 * [startX]. By default the [left] of the node.
 * @param durationMillis The duration of the swipe. By default 200 milliseconds.
 */
fun TouchInjectionScope.swipeLeft(
    startX: Float = right,
    endX: Float = left,
    durationMillis: Long = 200
) {
    require(startX >= endX) {
        "startX=$startX needs to be greater than or equal to endX=$endX"
    }
    val start = Offset(startX, centerY)
    val end = Offset(endX, centerY)
    swipe(start, end, durationMillis)
}

/**
 * Performs a swipe right gesture along `y = [centerY]` of the associated node, from [startX]
 * till [endX], taking [durationMillis] milliseconds.
 *
 * @param startX The x-coordinate of the start of the swipe. Must be less than or equal to the
 * [endX]. By default the [left] of the node.
 * @param endX The x-coordinate of the end of the swipe. Must be greater than or equal to the
 * [startX]. By default the [right] of the node.
 * @param durationMillis The duration of the swipe. By default 200 milliseconds.
 */
fun TouchInjectionScope.swipeRight(
    startX: Float = left,
    endX: Float = right,
    durationMillis: Long = 200
) {
    require(startX <= endX) {
        "startX=$startX needs to be less than or equal to endX=$endX"
    }
    val start = Offset(startX, centerY)
    val end = Offset(endX, centerY)
    swipe(start, end, durationMillis)
}
