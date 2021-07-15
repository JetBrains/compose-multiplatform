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
import androidx.compose.ui.node.RootForTest

internal expect fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher

/**
 * Dispatcher to inject any kind of input. An [InputDispatcher] is created at the
 * beginning of [performMultiModalInput] or the single modality alternatives, and disposed at the
 * end of that method. The state of all input modalities is persisted and restored on the next
 * invocation of [performMultiModalInput] (or an alternative).
 *
 * Dispatching input happens in two stages. In the first stage, all events are generated
 * (enqueued), using the `enqueue*` methods, and in the second stage all events are injected.
 * Clients of [InputDispatcher] should only call methods for the first stage listed below, the
 * second stage is handled by [performMultiModalInput] and friends.
 *
 * Touch input:
 * * [getCurrentTouchPosition]
 * * [enqueueTouchDown]
 * * [enqueueTouchMove]
 * * [updateTouchPointer]
 * * [enqueueTouchUp]
 * * [enqueueTouchCancel]
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
     * The state of the current touch gesture. If `null`, no touch gesture is in progress.
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
     * Increases the current event time by [durationMillis].
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
     * Generates a down touch event at [position] for the pointer with the given [pointerId].
     * Starts a new touch gesture if no other [pointerId]s are down. Only possible if the
     * [pointerId] is not currently being used, although pointer ids may be reused during a touch
     * gesture.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     *
     * @see enqueueTouchMove
     * @see updateTouchPointer
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
     * Generates a move touch event without moving any of the pointers. Use this to commit all
     * changes in pointer location made with [updateTouchPointer]. The generated event will contain
     * the current position of all pointers.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun enqueueTouchMove() {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }
        gesture.enqueueMove()
        gesture.hasPointerUpdates = false
    }

    /**
     * Updates the position of the touch pointer with the given [pointerId] to the given
     * [position], but does not generate a move touch event. Use this to move multiple pointers
     * simultaneously. To generate the next move touch event, which will contain the current
     * position of _all_ pointers (not just the moved ones), call [enqueueTouchMove]. If you move
     * one or more pointers and then call [enqueueTouchDown], without calling [enqueueTouchMove]
     * first, a move event will be generated right before that down event.
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
     * pointer.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [enqueueTouchDown]
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchCancel
     */
    fun enqueueTouchUp(pointerId: Int) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot send UP event, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }

        // First send the UP event
        gesture.enqueueUp(pointerId)

        // Then remove the pointer, and end the gesture if no pointers are left
        gesture.lastPositions.remove(pointerId)
        if (gesture.lastPositions.isEmpty()) {
            partialGesture = null
        }
    }

    /**
     * Generates a cancel touch event for the current touch gesture.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     */
    fun enqueueTouchCancel() {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send CANCEL event, no gesture is in progress"
        }
        gesture.enqueueCancel()
        partialGesture = null
    }

    /**
     * Generates a move event with all pointer locations, if any of the pointers has been moved by
     * [updateTouchPointer] since the last move event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            enqueueTouchMove()
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
     * Called when this [InputDispatcher] is about to be discarded, from
     * [MultiModalInjectionScope.dispose].
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
