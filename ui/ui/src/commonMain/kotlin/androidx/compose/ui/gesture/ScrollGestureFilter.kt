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
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.gesture.scrollorientationlocking.ScrollOrientationLocker
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Defines the callbacks associated with scrolling.
 */
interface ScrollCallback {

    /**
     * Override to be notified when a scroll has started.
     *
     * This will be called as soon as the average distance of all pointers surpasses the touch slop
     * in the relevant orientation.
     *
     * Only called if the last called if the most recent call among [onStart], [onStop], and
     * [onCancel] was [onStop] or [onCancel].
     *
     * @param downPosition The pointer input position of the down event.
     */
    fun onStart(downPosition: Offset) {}

    /**
     * Override to be notified when a distance has been scrolled.
     *
     * When overridden, return the amount of the [scrollDistance] that has been consumed.
     *
     * Called immediately after [onStart] and for every subsequent pointer movement, as long as the
     * movement was enough to constitute a scroll (the average movement on in the relevant
     * orientation  is not equal to 0).
     *
     * Note: This may be called multiple times in a single pass and the values should be accumulated
     * for each call.
     *
     * @param scrollDistance The distance that has been scrolled.  Reflects the average scroll
     * distance of all pointers.
     */
    fun onScroll(scrollDistance: Float) = 0f

    /**
     * Override to be notified when a scroll has stopped.
     *
     * This is called once all pointers have released the associated PointerInputFilter.
     *
     * Only called if the last called if the most recent call among [onStart], [onStop], and
     * [onCancel] was [onStart].
     *
     * @param velocity The velocity of the scroll in the relevant orientation at the point in time
     * when all pointers have released the relevant PointerInputFilter. In pixels per second.
     */
    fun onStop(velocity: Float) {}

    /**
     * Override to be notified when the scroll has been cancelled.
     *
     * This is called in response to a cancellation event such as the associated
     * PointerInputFilter having been removed from the hierarchy.
     *
     * Only called if the last called if the most recent call among [onStart], [onStop], and
     * [onCancel] was [onStart].
     */
    fun onCancel() {}
}

/**
 * Like [Modifier.dragGestureFilter], this gesture filter will detect dragging, but will only do
 * so along the given [orientation].
 *
 * This gesture filter also disambiguates amongst other scrollGestureFilters such that for all
 * pointers that this gesture filter uses to scroll in the given [orientation], other
 * scrollGestureFilters (or other clients of [ScrollOrientationLocker]) will not use those same
 * pointers to drag in the other [orientation].  Likewise, this scrollGestureFilter will not use
 * pointers to drag if they are already being used to drag in a different orientation.
 *
 * Note: [canDrag] will only be queried in directions that exist within the given [orientation].
 *
 * Note: Changing the value of [orientation] will reset the gesture filter such that it will not
 * respond to input until new pointers are detected.
 *
 * @param scrollCallback: The set of callbacks for scrolling.
 * @param orientation: The orientation this gesture filter uses.
 * @param canDrag Set to limit the types of directions under which touch slop can be exceeded.
 * Return true if you want a drag to be started due to the touch slop being surpassed in the
 * given [Direction]. If [canDrag] is not provided, touch slop will be able to be exceeded in all
 * directions that are in the provided [orientation].
 * @param startDragImmediately Set to true to have dragging begin immediately when a pointer is
 * "down", preventing children from responding to the "down" change.  Generally, this parameter
 * should be set to true when the child of the GestureDetector is animating, such that when a finger
 * touches it, dragging is immediately started so the animation stops and dragging can occur.
 */
// TODO(shepshapard): Consider breaking up ScrollCallback such that the onScroll lambda can be
//  the final parameter.
fun Modifier.scrollGestureFilter(
    scrollCallback: ScrollCallback,
    orientation: Orientation,
    canDrag: ((Direction) -> Boolean)? = null,
    startDragImmediately: Boolean = false
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollGestureFilter"
        properties["scrollCallback"] = scrollCallback
        properties["orientation"] = orientation
        properties["canDrag"] = canDrag
        properties["startDragImmediately"] = startDragImmediately
    }
) {
    val coordinator = remember { ScrollGestureFilterCoordinator() }
    coordinator.scrollCallback = scrollCallback
    coordinator.orientation = orientation

    // TODO(b/146427920): There is a gap here where RawPressStartGestureDetector can cause a call to
    //  DragObserver.onStart but if the pointer doesn't move and releases, (or if cancel is called)
    //  The appropriate callbacks to DragObserver will not be called.
    rawDragGestureFilter(
        coordinator.rawDragObserver,
        coordinator::enabledOrStarted,
        orientation
    )
        .dragSlopExceededGestureFilter(coordinator::enableDrag, canDrag, orientation)
        .rawPressStartGestureFilter(
            coordinator::startDrag,
            startDragImmediately,
            PointerEventPass.Initial
        )
}

/**
 * Coordinates the logic of rawDragGestureFilter, dragSlopExceededGestureFilter, and
 * rawPressStartGestureFilter.
 *
 * Also maps the output of rawDragGestureFilter to the output of scrollGestureFilter.
 */
private class ScrollGestureFilterCoordinator {
    private var started = false
    private var enabled = false

    lateinit var scrollCallback: ScrollCallback
    lateinit var orientation: Orientation

    val enabledOrStarted
        get() = started || enabled

    fun enableDrag() {
        enabled = true
    }

    fun startDrag(downPosition: Offset) {
        started = true
        scrollCallback.onStart(downPosition)
    }

    val rawDragObserver: DragObserver =
        object : DragObserver {
            override fun onStart(downPosition: Offset) {
                if (!started) {
                    scrollCallback.onStart(downPosition)
                }
            }

            override fun onDrag(dragDistance: Offset): Offset {
                return when (orientation) {
                    Orientation.Horizontal -> Offset(scrollCallback.onScroll(dragDistance.x), 0f)
                    Orientation.Vertical -> Offset(0f, scrollCallback.onScroll(dragDistance.y))
                }
            }

            override fun onStop(velocity: Offset) {
                started = false
                enabled = false
                return when (orientation) {
                    Orientation.Horizontal -> scrollCallback.onStop(velocity.x)
                    Orientation.Vertical -> scrollCallback.onStop(velocity.y)
                }
            }

            override fun onCancel() {
                started = false
                enabled = false
                scrollCallback.onCancel()
            }
        }
}