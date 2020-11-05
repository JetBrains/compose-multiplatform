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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.debugInspectorInfo

// TODO(b/146133703): Likely rename to PanGestureDetector as per b/146133703
/**
 * This gesture detector detects dragging in any direction.
 *
 * Dragging normally begins when the touch slop distance (currently defined by [TouchSlop]) is
 * surpassed in a supported direction (see [DragObserver.onDrag]).  When dragging begins in this
 * manner, [DragObserver.onStart] is called, followed immediately by a call to
 * [DragObserver.onDrag]. [DragObserver.onDrag] is then continuously called whenever pointers
 * have moved. The gesture ends with either a call to [DragObserver.onStop] or
 * [DragObserver.onCancel], only after [DragObserver.onStart] is called. [DragObserver.onStop] is
 * called when the dragging ends due to all of the pointers no longer interacting with the
 * DragGestureDetector (for example, the last pointer has been lifted off of the
 * DragGestureDetector). [DragObserver.onCancel] is called when the dragging ends due to a system
 * cancellation event.
 *
 * If [startDragImmediately] is set to true, dragging will begin as soon as soon as a pointer comes
 * in contact with it, effectively ignoring touch slop and blocking any descendants from reacting
 * the "down" change.  When dragging begins in this manner, [DragObserver.onStart] is called
 * immediately and is followed by [DragObserver.onDrag] when some drag distance has occurred.
 *
 * When multiple pointers are touching the detector, the drag distance is taken as the average of
 * all of the pointers.
 *
 * @param dragObserver The callback interface to report all events related to dragging.
 * @param canDrag Set to limit the directions under which touch slop can be exceeded. Return true
 * if you want a drag to be started due to the touch slop being surpassed in the given [Direction].
 * If [canDrag] is not provided, touch slop will be able to be exceeded in all directions.
 * @param startDragImmediately Set to true to have dragging begin immediately when a pointer is
 * "down", preventing children from responding to the "down" change.  Generally, this parameter
 * should be set to true when the child of the GestureDetector is animating, such that when a finger
 * touches it, dragging is immediately started so the animation stops and dragging can occur.
 */
fun Modifier.dragGestureFilter(
    dragObserver: DragObserver,
    canDrag: ((Direction) -> Boolean)? = null,
    startDragImmediately: Boolean = false
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dragGestureFilter"
        properties["dragObserver"] = dragObserver
        properties["canDrag"] = canDrag
        properties["startDragImmediately"] = startDragImmediately
    }
) {
    val glue = remember { TouchSlopDragGestureDetectorGlue() }
    glue.touchSlopDragObserver = dragObserver

    // TODO(b/146427920): There is a gap here where RawPressStartGestureDetector can cause a call to
    //  DragObserver.onStart but if the pointer doesn't move and releases, (or if cancel is called)
    //  The appropriate callbacks to DragObserver will not be called.
    rawDragGestureFilter(glue.rawDragObserver, glue::enabledOrStarted)
        .dragSlopExceededGestureFilter(glue::enableDrag, canDrag)
        .rawPressStartGestureFilter(
            glue::startDrag,
            startDragImmediately,
            PointerEventPass.Initial
        )
}

/**
 * Glues together the logic of RawDragGestureDetector, TouchSlopExceededGestureDetector, and
 * InterruptFlingGestureDetector.
 */
private class TouchSlopDragGestureDetectorGlue {

    lateinit var touchSlopDragObserver: DragObserver
    var started = false
    var enabled = false
    val enabledOrStarted
        get() = started || enabled

    fun enableDrag() {
        enabled = true
    }

    fun startDrag(downPosition: Offset) {
        started = true
        touchSlopDragObserver.onStart(downPosition)
    }

    val rawDragObserver: DragObserver =
        object : DragObserver {
            override fun onStart(downPosition: Offset) {
                if (!started) {
                    touchSlopDragObserver.onStart(downPosition)
                }
            }

            override fun onDrag(dragDistance: Offset): Offset {
                return touchSlopDragObserver.onDrag(dragDistance)
            }

            override fun onStop(velocity: Offset) {
                started = false
                enabled = false
                touchSlopDragObserver.onStop(velocity)
            }

            override fun onCancel() {
                started = false
                enabled = false
                touchSlopDragObserver.onCancel()
            }
        }
}