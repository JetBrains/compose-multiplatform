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

package androidx.compose.ui.gesture

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize

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
@Suppress("DEPRECATION")
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

/**
 * Glues together the logic of [rawDragGestureFilter], [longPressGestureFilter],
 * and a custom [PointerInputFilter] to make LongPressDragGestureDetector work.
 */
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