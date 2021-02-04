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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.gesture

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.customevents.LongPressFiredEvent
import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Deprecated("Use Modifier.pointerInput {detectDragGesturesAfterLongPress(...)} instead")
interface LongPressDragObserver {

    /**
     * Override to be notified when a long press has occurred and thus dragging can start.
     *
     * Note that when this is called, dragging hasn't actually started, but rather, dragging can start.  When dragging
     * has actually started, [onDragStart] will be called.  It is possible for [onDragStart] to be called immediately
     * after this synchronously in the same event stream.
     *
     * This won't be called again until after [onStop] has been called.
     *
     * @see onDragStart
     * @see onDrag
     * @see onStop
     */
    fun onLongPress(pxPosition: Offset) {}

    /**
     * Override to be notified when dragging has actually begun.
     *
     * Dragging has begun when both [onLongPress] has been called, and the average pointer distance change is not 0.
     *
     * This will not be called until after [onLongPress] has been called, and may be called synchronously,
     * immediately afterward [onLongPress], as a result of the same pointer input event.
     *
     * This will not be called again until [onStop] has been called.
     *
     * @see onLongPress
     * @see onDrag
     * @see onStop
     */
    fun onDragStart() {}

    /**
     * Override to be notified when a distance has been dragged.
     *
     * When overridden, return the amount of the [dragDistance] that has been consumed.
     *
     * Called after [onDragStart] and for every subsequent pointer movement, as long as the movement
     * was enough to constitute a drag (the average movement on the x or y axis is not equal to
     * 0).  This may be called synchronously, immediately afterward [onDragStart], as a result of
     * the same pointer input event.
     *
     * Note: This will be called multiple times for a single pointer input event and the values
     * provided in each call should be considered unique.
     *
     * @param dragDistance The distance that has been dragged.  Reflects the average drag distance
     * of all pointers.
     */
    fun onDrag(dragDistance: Offset) = Offset.Zero

    /**
     * Override to be notified when a drag has stopped.
     *
     * This is called once all pointers have stopped interacting with this DragGestureDetector and
     * [onLongPress] was previously called.
     */
    fun onStop(velocity: Offset) {}

    /**
     * Override to be notified when the drag has been cancelled.
     *
     * This is called if [onLongPress] has ben called and then a cancellation event has occurs
     * (for example, due to the gesture detector being removed from the tree) before [onStop] is
     * called.
     */
    fun onCancel() {}
}

/**
 * This gesture detector detects dragging in any direction, but only after a long press has first
 * occurred.
 *
 * Dragging begins once a long press has occurred and then dragging occurs.  When long press occurs,
 * [LongPressDragObserver.onLongPress] is called. Once dragging has occurred,
 * [LongPressDragObserver.onDragStart] will be called. [LongPressDragObserver.onDrag] is then
 * continuously called whenever pointer movement results in a drag. The gesture will end
 * with either a call to [LongPressDragObserver.onStop] or [LongPressDragObserver.onCancel]. Either
 * will be called after [LongPressDragObserver.onLongPress] is called.
 * [LongPressDragObserver.onStop] is called when the the gesture ends due to all of the pointers
 * no longer interacting with the LongPressDragGestureDetector (for example, the last finger has
 * been lifted off of the LongPressDragGestureDetector). [LongPressDragObserver.onCancel] is
 * called in response to a system cancellation event.
 *
 * When multiple pointers are touching the detector, the drag distance is taken as the average of
 * all of the pointers.
 *
 * @param longPressDragObserver The callback interface to report all events.
 * @see LongPressDragObserver
 */
@Deprecated("Use Modifier.pointerInput { detectDragGesturesAfterLongPress(...)} instead.")
fun Modifier.longPressDragGestureFilter(
    longPressDragObserver: LongPressDragObserver
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "longPressDragGestureFilter"
        properties["longPressDragObserver"] = longPressDragObserver
    }
) {
    val glue = remember { LongPressDragGestureDetectorGlue() }
    glue.longPressDragObserver = longPressDragObserver

    rawDragGestureFilter(glue.dragObserver, glue::dragEnabled)
        .then(PointerInputModifierImpl(glue))
        .longPressGestureFilter(glue.onLongPress)
}

private class LongPressDragGestureDetectorGlue : PointerInputFilter() {
    lateinit var longPressDragObserver: LongPressDragObserver
    private var dragStarted: Boolean = false
    var dragEnabled: Boolean = false

    val dragObserver: DragObserver =

        object : DragObserver {

            override fun onStart(downPosition: Offset) {
                longPressDragObserver.onDragStart()
                dragStarted = true
            }

            override fun onDrag(dragDistance: Offset): Offset {
                return longPressDragObserver.onDrag(dragDistance)
            }

            override fun onStop(velocity: Offset) {
                dragEnabled = false
                dragStarted = false
                longPressDragObserver.onStop(velocity)
            }

            override fun onCancel() {
                dragEnabled = false
                dragStarted = false
                longPressDragObserver.onCancel()
            }
        }

    // This handler ensures that onStop will be called after long press happened, but before
    // dragging actually has begun.
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Main &&
            dragEnabled &&
            !dragStarted &&
            pointerEvent.changes.all { it.changedToUpIgnoreConsumed() }
        ) {
            dragEnabled = false
            longPressDragObserver.onStop(Offset.Zero)
        }
    }

    // This handler ensures that onCancel is called if onLongPress was previously called but
    // dragging has not yet started.
    override fun onCancel() {
        if (dragEnabled && !dragStarted) {
            dragEnabled = false
            longPressDragObserver.onCancel()
        }
    }

    val onLongPress = { pxPosition: Offset ->
        dragEnabled = true
        longPressDragObserver.onLongPress(pxPosition)
    }
}

internal fun Modifier.longPressGestureFilter(
    onLongPress: (Offset) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "longPressGestureFilter"
        properties["onLongPress"] = onLongPress
    }
) {
    @Suppress("DEPRECATION")
    val scope = rememberCoroutineScope()
    val filter = remember { LongPressGestureFilter(scope) }
    filter.onLongPress = onLongPress
    PointerInputModifierImpl(filter)
}

internal class LongPressGestureFilter(
    private val coroutineScope: CoroutineScope
) : PointerInputFilter() {
    lateinit var onLongPress: (Offset) -> Unit

    /*@VisibleForTesting*/
    internal var longPressTimeout = LongPressTimeoutMillis

    private enum class State {
        Idle, Primed, Fired
    }

    private var state = State.Idle
    private val pointerPositions = linkedMapOf<PointerId, Offset>()
    private var job: Job? = null
    private lateinit var customEventDispatcher: CustomEventDispatcher

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        this.customEventDispatcher = customEventDispatcher
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Initial) {
            if (state == State.Fired) {
                // If we fired and have not reset, we should prevent other pointer input nodes from
                // responding to up, so consume it early on.
                changes.fastForEach {
                    if (it.changedToUp()) {
                        it.consumeDownChange()
                    }
                }
            }
            return
        }

        if (pass == PointerEventPass.Main) {
            if (state == State.Idle && changes.all { it.changedToDown() }) {
                // If we are idle and all of the changes changed to down, we are prime to fire
                // the event.
                primeToFire()
            } else if (state != State.Idle && changes.all { it.changedToUpIgnoreConsumed() }) {
                // If we have started and all of the changes changed to up, reset to idle.
                resetToIdle()
            } else if (!changes.anyPointersInBounds(bounds)) {
                // If all pointers have gone out of bounds, reset to idle.
                resetToIdle()
            }

            if (state == State.Primed) {
                // If we are primed, keep track of all down pointer positions so we can pass
                // pointer position information to the event we will fire.
                changes.forEach {
                    if (it.pressed) {
                        pointerPositions[it.id] = it.position
                    } else {
                        pointerPositions.remove(it.id)
                    }
                }
            }
        }

        if (
            pass == PointerEventPass.Final &&
            state != State.Idle &&
            changes.fastAny { it.anyPositionChangeConsumed() }
        ) {
            // If we are anything but Idle and something consumed movement, reset.
            resetToIdle()
        }
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        if (
            state == State.Primed &&
            customEvent is LongPressFiredEvent &&
            pass == PointerEventPass.Initial
        ) {
            // If we are primed but something else fired long press, we should reset.
            // Doesn't matter what pass we are on, just choosing one so we only reset once.
            resetToIdle()
        }
    }

    override fun onCancel() {
        resetToIdle()
    }

    private fun fireLongPress() {
        state = State.Fired
        onLongPress.invoke(pointerPositions.asIterable().first().value)
        customEventDispatcher.dispatchCustomEvent(LongPressFiredEvent)
    }

    private fun primeToFire() {
        state = State.Primed
        job = coroutineScope.launch {
            delay(longPressTimeout)
            fireLongPress()
        }
    }

    private fun resetToIdle() {
        state = State.Idle
        job?.cancel()
        pointerPositions.clear()
    }
}