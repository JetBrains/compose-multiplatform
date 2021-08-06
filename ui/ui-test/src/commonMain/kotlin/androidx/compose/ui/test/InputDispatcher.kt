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
package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.node.RootForTest
import kotlin.math.max
import kotlin.math.roundToInt

internal expect fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher

/**
 * Dispatcher to inject full and partial gestures. An [InputDispatcher] is created at the
 * beginning of [performGesture], and disposed at the end of that method. If there is still a
 * [gesture going on][isGestureInProgress] when the dispatcher is disposed, the state of the
 * current gesture will be persisted and restored on the next invocation of [performGesture].
 *
 * Dispatching input happens in two stages. In the first stage, all events are generated
 * (enqueued), using the `enqueue*` methods, and in the second stage all events are injected.
 * Clients of [InputDispatcher] should only call methods for the first stage listed below, the
 * second stage is handled by [performGesture].
 *
 * Full gestures:
 * * [enqueueClick]
 * * [enqueueSwipe]
 * * [enqueueSwipes]
 *
 * Partial gestures:
 * * [enqueueTouchDown]
 * * [enqueueTouchMove]
 * * [enqueueTouchUp]
 * * [enqueueTouchCancel]
 * * [updateTouchPointer]
 * * [getCurrentTouchPosition]
 *
 * Chaining methods:
 * * [advanceEventTime]
 */
internal abstract class InputDispatcher(
    private val testContext: TestContext,
    private val root: RootForTest?
) {
    companion object {
        /**
         * The default time between two successively injected events, 10 milliseconds.
         * Ideally, the value should reflect a realistic pointer input sample rate, but that
         * depends on too many factors. Instead, the value is chosen comfortably below the
         * targeted frame rate (60 fps, equating to a 16ms period).
         */
        var eventPeriodMillis = 10L
            internal set
    }

    /**
     * The eventTime of the next event.
     */
    protected var currentTime = testContext.currentTime

    /**
     * The state of the current touch gesture. If `null`, no touch gesture is in progress. This
     * state contains the current position of all pointer ids and whether or not pointers have
     * moved without having enqueued the corresponding move event.
     */
    protected var partialGesture: PartialGesture? = null

    /**
     * Indicates if a gesture is in progress or not. A gesture is in progress if at least one
     * finger is (still) touching the screen.
     */
    val isTouchInProgress: Boolean
        get() = partialGesture != null

    init {
        val state = testContext.states.remove(root)
        if (state != null) {
            partialGesture = state.partialGesture
        }
    }

    protected open fun saveState(root: RootForTest?) {
        if (root != null) {
            testContext.states[root] =
                InputDispatcherState(
                    partialGesture
                )
        }
    }

    @OptIn(InternalTestApi::class)
    private val TestContext.currentTime get() = testOwner.mainClock.currentTime

    /**
     * Increases the current event time by [durationMillis]. Note that [enqueueTouchMove] and
     * [enqueueTouchCancel] also increase the current time by 10ms.
     *
     * @param durationMillis The duration of the delay. Must be positive
     */
    fun advanceEventTime(durationMillis: Long = eventPeriodMillis) {
        require(durationMillis >= 0) {
            "duration of a delay can only be positive, not $durationMillis"
        }
        currentTime += durationMillis
    }

    /**
     * During a touch gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no touch gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     * pointer is not currently in use
     */
    fun getCurrentTouchPosition(pointerId: Int): Offset? {
        return partialGesture?.lastPositions?.get(pointerId)
    }

    /**
     * Generates a click event at [position]. There will be 10ms in between the down and the up
     * event. The generated events are enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param position The coordinate of the click
     */
    fun enqueueClick(position: Offset) {
        enqueueTouchDown(0, position)
        enqueueTouchMove()
        enqueueTouchUp(0)
    }

    /**
     * Generates a swipe gesture from [start] to [end] with the given [durationMillis]. The
     * generated events are enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param start The start position of the gesture
     * @param end The end position of the gesture
     * @param durationMillis The duration of the gesture
     */
    fun enqueueSwipe(start: Offset, end: Offset, durationMillis: Long) {
        val durationFloat = durationMillis.toFloat()
        enqueueSwipe(
            curve = { lerp(start, end, it / durationFloat) },
            durationMillis = durationMillis
        )
    }

    /**
     * Generates a swipe gesture from [curve]&#40;0) to [curve]&#40;[durationMillis]), following the
     * route defined by [curve]. Will force sampling of an event at all times defined in
     * [keyTimes]. The number of events sampled between the key times is implementation
     * dependent. The generated events are enqueued in this [InputDispatcher] and will be sent
     * when [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param curve The function that defines the position of the gesture over time
     * @param durationMillis The duration of the gesture
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun enqueueSwipe(
        curve: (Long) -> Offset,
        durationMillis: Long,
        keyTimes: List<Long> = emptyList()
    ) {
        enqueueSwipes(listOf(curve), durationMillis, keyTimes)
    }

    /**
     * Generates [curves].size simultaneous swipe gestures, each swipe going from
     * [curves]&#91;i&#93;(0) to [curves]&#91;i&#93;([durationMillis]), following the route defined
     * by [curves]&#91;i&#93;. Will force sampling of an event at all times defined in [keyTimes].
     * The number of events sampled between the key times is implementation dependent. The
     * generated events are enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param curves The functions that define the position of the gesture over time
     * @param durationMillis The duration of the gestures
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun enqueueSwipes(
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
            enqueueTouchDown(i, curve(startTime))
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
            sendPartialSwipes(curves, currTime, tNext)
            currTime = tNext
        }

        // And end with up events
        repeat(curves.size) {
            enqueueTouchUp(it)
        }
    }

    /**
     * Generates move events between `f([t0])` and `f([tN])` during the time window `(downTime +
     * t0, downTime + tN]`, using [fs] to sample the coordinate of each event. The number of
     * events sent (#numEvents) is such that the time between each event is as close to
     * [InputDispatcher.eventPeriodMillis] as possible, but at least 1. The first event is sent at
     * time `downTime + (tN - t0) / #numEvents`, the last event is sent at time tN.
     *
     * @param fs The functions that define the coordinates of the respective gestures over time
     * @param t0 The start time of this segment of the swipe, in milliseconds relative to downTime
     * @param tN The end time of this segment of the swipe, in milliseconds relative to downTime
     */
    private fun sendPartialSwipes(
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
            val t = androidx.compose.ui.util.lerp(t0, tN, progress)
            fs.forEachIndexed { i, f ->
                updateTouchPointer(i, f(t))
            }
            enqueueTouchMove(t - tPrev)
            tPrev = t
        }
    }

    /**
     * Generates a down touch event at [position] for the pointer with the given [pointerId],
     * starting a new partial gesture. A partial gesture can only be started if none was currently
     * ongoing for that pointer. Pointer ids may be reused during the same gesture. The generated
     * touch event is enqueued in this [InputDispatcher] and will be sent when [sendAllSynchronous]
     * is called at the end of [performGesture].
     *
     * It is possible to mix partial gestures with full gestures (e.g. generate a
     * [click][enqueueClick] during a partial gesture), as long as you make sure that the default
     * pointer id (id=0) is free to be used by the full gesture.
     *
     * A full gesture starts with a down event at some position (with this method) that indicates
     * a finger has started touching the screen, followed by zero or more [down][enqueueTouchDown],
     * [move][enqueueTouchMove] and [up][enqueueTouchUp] events that respectively indicate that
     * another finger started touching the screen, a finger moved around or a finger was lifted up
     * from the screen. A gesture is finished when [up][enqueueTouchUp] lifts the last remaining
     * finger from the screen, or when a single [cancel][enqueueTouchCancel] event is generated.
     *
     * Partial gestures don't have to be defined all in the same [performGesture] block, but
     * keep in mind that while the gesture is not complete, all code you execute in between
     * blocks that progress the gesture, will be executed while imaginary fingers are actively
     * touching the screen. All events generated during a single [performGesture] block are sent
     * together at the end of that block.
     *
     * In the context of testing, it is not necessary to complete a gesture with an up or cancel
     * event, if the test ends before it expects the finger to be lifted from the screen.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     *
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun enqueueTouchDown(pointerId: Int, position: Offset) {
        var gesture = partialGesture

        // Check if this pointer is not already down
        require(gesture == null || !gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send DOWN event, a gesture is already in progress for pointer $pointerId"
        }

        // Send a MOVE event if pointers have changed since the last event
        gesture?.flushPointerUpdates()

        // Start a new gesture, or add the pointerId to the existing gesture
        if (gesture == null) {
            gesture = PartialGesture(currentTime, position, pointerId)
            partialGesture = gesture
        } else {
            gesture.lastPositions[pointerId] = position
        }

        // Send the DOWN event
        gesture.enqueueDown(pointerId)
    }

    /**
     * Generates a move touch event [delay] milliseconds after the previous injected event of this
     * gesture, without moving any of the pointers. The default [delay] is [10 milliseconds]
     * [eventPeriodMillis]. Use this to commit all changes in pointer location made
     * with [updateTouchPointer]. The generated event will contain the current position of all
     * pointers. It is enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture]. See [enqueueTouchDown] for
     * documentation on creating touch gestures.
     *
     * @param delay The time in milliseconds between the previously injected event and the move
     * event. [10 milliseconds][eventPeriodMillis] by default.
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun enqueueTouchMove(delay: Long = eventPeriodMillis) {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }
        require(delay >= 0) {
            "Cannot send MOVE event with a delay of $delay ms"
        }

        advanceEventTime(delay)
        gesture.enqueueMove()
        gesture.hasPointerUpdates = false
    }

    /**
     * Updates the position of the pointer with the given [pointerId] to the given [position],
     * but does not generate a move touch event. Use this to move multiple pointers
     * simultaneously. To generate the next move touch event, which will contain the current
     * position of _all_ pointers (not just the moved ones), call [enqueueTouchMove] without
     * arguments. If you move one or more pointers and then call [enqueueTouchDown] or
     * [enqueueTouchUp], without calling [enqueueTouchMove] first, a move event will be generated
     * right before that down or up event. See [enqueueTouchDown] for documentation on creating
     * touch gestures.
     *
     * @param pointerId The id of the pointer to move, as supplied in [enqueueTouchDown]
     * @param position The position to move the pointer to
     *
     * @see enqueueTouchDown
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun updateTouchPointer(pointerId: Int, position: Offset) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot move pointers, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot move pointer $pointerId, it is not active in the current gesture"
        }

        gesture.lastPositions[pointerId] = position
        gesture.hasPointerUpdates = true
    }

    /**
     * Generates an up touch event for the given [pointerId] at the current position of that
     * pointer, [delay] milliseconds after the previous injected event of this gesture. The default
     * [delay] is 0 milliseconds. The generated touch event is enqueued in this [InputDispatcher]
     * and will be sent when [sendAllSynchronous] is called at the end of [performGesture]. See
     * [enqueueTouchDown] for documentation on creating touch gestures.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [enqueueTouchDown]
     * @param delay The time in milliseconds between the previously injected event and the move
     * event. 0 milliseconds by default.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchCancel
     */
    fun enqueueTouchUp(pointerId: Int, delay: Long = 0) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot send UP event, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }
        require(delay >= 0) {
            "Cannot send UP event with a delay of $delay ms"
        }

        gesture.flushPointerUpdates()
        advanceEventTime(delay)

        // First send the UP event
        gesture.enqueueUp(pointerId)

        // Then remove the pointer, and end the gesture if no pointers are left
        gesture.lastPositions.remove(pointerId)
        if (gesture.lastPositions.isEmpty()) {
            partialGesture = null
        }
    }

    /**
     * Generates a cancel touch event [delay] milliseconds after the previous injected event of
     * this gesture. The default [delay] is [10 milliseconds][InputDispatcher.eventPeriodMillis].
     * The generated event is enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture]. See [enqueueTouchDown] for
     * documentation on creating touch gestures.
     *
     * @param delay The time in milliseconds between the previously injected event and the cancel
     * event. [10 milliseconds][InputDispatcher.eventPeriodMillis] by default.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     */
    fun enqueueTouchCancel(delay: Long = eventPeriodMillis) {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send CANCEL event, no gesture is in progress"
        }
        require(delay >= 0) {
            "Cannot send CANCEL event with a delay of $delay ms"
        }

        advanceEventTime(delay)
        gesture.enqueueCancel()
        partialGesture = null
    }

    /**
     * Generates a move event with all pointer locations, if any of the pointers has been moved by
     * [updateTouchPointer] since the last move event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            enqueueTouchMove(eventPeriodMillis)
        }
    }

    /**
     * Sends all enqueued events and blocks while they are dispatched. If an exception is
     * thrown during the process, all events that haven't yet been dispatched will be dropped.
     */
    abstract fun sendAllSynchronous()

    protected abstract fun PartialGesture.enqueueDown(pointerId: Int)

    protected abstract fun PartialGesture.enqueueMove()

    protected abstract fun PartialGesture.enqueueUp(pointerId: Int)

    protected abstract fun PartialGesture.enqueueCancel()

    /**
     * Called when this [InputDispatcher] is about to be discarded, from [GestureScope.dispose].
     */
    fun dispose() {
        saveState(root)
        onDispose()
    }

    /**
     * Override this method to take platform specific action when this dispatcher is disposed.
     * E.g. to recycle event objects that the dispatcher still holds on to.
     */
    protected open fun onDispose() {}
}

/**
 * The state of the current gesture. Contains the current position of all pointers and the
 * down time (start time) of the gesture. For the current time, see [InputDispatcher.currentTime].
 *
 * @param downTime The time of the first down event of this gesture
 * @param startPosition The position of the first down event of this gesture
 * @param pointerId The pointer id of the first down event of this gesture
 */
internal class PartialGesture(val downTime: Long, startPosition: Offset, pointerId: Int) {
    val lastPositions = mutableMapOf(Pair(pointerId, startPosition))
    var hasPointerUpdates: Boolean = false
}

/**
 * The state of an [InputDispatcher], saved when the [GestureScope] is disposed and restored
 * when the [GestureScope] is recreated.
 *
 * @param partialGesture The state of an incomplete gesture. If no gesture was in progress
 * when the state of the [InputDispatcher] was saved, this will be `null`.
 */
internal data class InputDispatcherState(
    val partialGesture: PartialGesture?
)
