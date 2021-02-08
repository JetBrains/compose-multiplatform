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

@file: Suppress("DEPRECATION")

package androidx.compose.ui.window

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal enum class Direction {
    LEFT, UP, RIGHT, DOWN
}

/**
 * Defines the callbacks associated with dragging.
 */
@Deprecated("Use Modifier.pointerInput { detectDragGestures(...) }")
internal interface DragObserver {

    /**
     * Override to be notified when a drag has started.
     *
     * This will be called as soon as the DragGestureDetector is allowed to start (canStartDragging
     * is null or returns true) and the average distance the pointers have moved are not 0 on
     * both the x and y axes.
     *
     * Only called if the last called if the most recent call among [onStart], [onStop], and
     * [onCancel] was [onStop] or [onCancel].
     *
     * @param downPosition The average position of all pointer positions when they first touched
     * down.
     */
    fun onStart(downPosition: Offset) {}

    /**
     * Override to be notified when a distance has been dragged.
     *
     * When overridden, return the amount of the [dragDistance] that has been consumed.
     *
     * Called immediately after [onStart] and for every subsequent pointer movement, as long as the
     * movement was enough to constitute a drag (the average movement on the x or y axis is not
     * equal to 0).
     *
     * Note: This may be called multiple times in a single pass and the values should be accumulated
     * for each call.
     *
     * @param dragDistance The distance that has been dragged.  Reflects the average drag distance
     * of all pointers.
     */
    fun onDrag(dragDistance: Offset) = Offset.Zero

    /**
     * Override to be notified when a drag has stopped.
     *
     * This is called once all pointers have stopped interacting with this DragGestureDetector.
     *
     * Only called if the last called if the most recent call among [onStart], [onStop], and
     * [onCancel] was [onStart].
     *
     * @param velocity The velocity of the drag in both orientations at the point in time when all
     * pointers have released the relevant PointerInputFilter. In pixels per second.
     */
    fun onStop(velocity: Offset) {}

    /**
     * Override to be notified when the drag has been cancelled.
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
 * Modeled after Android's ViewConfiguration:
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/ViewConfiguration.java
 */

/** The time before a long press gesture attempts to win. */
internal const val LongPressTimeoutMillis: Long = 500L

/**
 * The maximum time from the start of the first tap to the start of the second
 * tap in a double-tap gesture.
 */
// TODO(shepshapard): In Android, this is actually the time from the first's up event
// to the second's down event, according to the ViewConfiguration docs.
internal const val DoubleTapTimeoutMillis: Long = 300L

/**
 * The distance a touch has to travel for the framework to be confident that
 * the gesture is a scroll gesture, or, inversely, the maximum distance that a
 * touch can travel before the framework becomes confident that it is not a
 * tap.
 */
// This value was empirically derived. We started at 8.0 and increased it to
// 18.0 after getting complaints that it was too difficult to hit targets.
internal val TouchSlop = 18.dp

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
 * @param startDragImmediately Set to true to have dragging begin immediately when a pointer is
 * "down", preventing children from responding to the "down" change.  Generally, this parameter
 * should be set to true when the child of the GestureDetector is animating, such that when a finger
 * touches it, dragging is immediately started so the animation stops and dragging can occur.
 */
@Deprecated("Use Modifier.pointerInput{ detectDragGestures(... )} instead")
internal fun Modifier.dragGestureFilter(
    dragObserver: DragObserver,
    startDragImmediately: Boolean = false
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dragGestureFilter"
        properties["dragObserver"] = dragObserver
        properties["startDragImmediately"] = startDragImmediately
    }
) {
    val glue = remember { TouchSlopDragGestureDetectorGlue() }
    glue.touchSlopDragObserver = dragObserver

    // TODO(b/146427920): There is a gap here where RawPressStartGestureDetector can cause a call to
    //  DragObserver.onStart but if the pointer doesn't move and releases, (or if cancel is called)
    //  The appropriate callbacks to DragObserver will not be called.
    rawDragGestureFilter(glue.rawDragObserver, glue::enabledOrStarted)
        .dragSlopExceededGestureFilter(glue::enableDrag)
        .rawPressStartGestureFilter(
            glue::startDrag,
            startDragImmediately,
            PointerEventPass.Initial
        )
}

// TODO(shepshapard): Convert to functional component with effects once effects are ready.
// TODO(shepshapard): Should this calculate the drag distance as the average of all fingers
//  (Shep thinks this is better), or should it only track the most recent finger to have
//  touched the screen over the detector (this is how Android currently does it)?
// TODO(b/139020678): Probably has shared functionality with other movement based detectors.
/**
 * This gesture detector detects dragging in any direction.
 *
 * Note: By default, this gesture detector only waits for a single pointer to have moved to start
 * dragging.  It is extremely likely that you don't want to use this gesture detector directly, but
 * instead use a drag gesture detector that does wait for some other condition to have occurred
 * (such as [dragGestureFilter] which waits for a single pointer to have passed touch
 * slop before dragging starts).
 *
 * Dragging begins when the a single pointer has moved and either [canStartDragging] is null or
 * returns true.  When dragging begins, [DragObserver.onStart] is called.  [DragObserver.onDrag] is
 * then continuously called whenever the average movement of all pointers has movement along the x
 * or y axis.  The gesture ends with either a call to [DragObserver.onStop] or
 * [DragObserver.onCancel], only after [DragObserver.onStart] is called. [DragObserver.onStop] is
 * called when the dragging ends due to all of the pointers no longer interacting with the
 * DragGestureDetector (for example, the last pointer has been lifted off of the
 * DragGestureDetector). [DragObserver.onCancel] is called when the dragging ends due to a system
 * cancellation event.
 *
 * When multiple pointers are touching the detector, the drag distance is taken as the average of
 * all of the pointers.
 *
 * @param dragObserver The callback interface to report all events related to dragging.
 * @param canStartDragging If set, Before dragging is started ([DragObserver.onStart] is called),
 *                         canStartDragging is called to check to see if it is allowed to start.
 */

// TODO(b/129784010): Consider also allowing onStart, onDrag, and onStop to be set individually
//  (instead of all being set via DragObserver).
@Deprecated("use Modifier.pointerInput { } with awaitFirstDown() and drag() functions")
internal fun Modifier.rawDragGestureFilter(
    dragObserver: DragObserver,
    canStartDragging: (() -> Boolean)? = null,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "rawDragGestureFilter"
        properties["dragObserver"] = dragObserver
        properties["canStartDragging"] = canStartDragging
    }
) {
    val filter = remember { RawDragGestureFilter() }
    filter.dragObserver = dragObserver
    filter.canStartDragging = canStartDragging
    DragPointerInputModifierImpl(filter)
}

internal class RawDragGestureFilter : PointerInputFilter() {
    private val velocityTrackers: MutableMap<PointerId, VelocityTracker> = mutableMapOf()
    private val downPositions: MutableMap<PointerId, Offset> = mutableMapOf()

    internal lateinit var dragObserver: DragObserver
    internal var canStartDragging: (() -> Boolean)? = null

    private var started = false

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Initial) {
            if (started) {
                // If we are have started we want to prevent any descendants from reacting to
                // any down change.
                changes.fastForEach {
                    if (it.changedToDown()) {
                        it.consumeDownChange()
                    }
                }
            }
        }

        if (pass == PointerEventPass.Main) {

            // Get the changes for pointers that are relevant to us due to orientation locking.

            // Handle up changes, which includes removing individual pointer VelocityTrackers
            // and potentially calling onStop().
            if (changes.fastAny { it.changedToUpIgnoreConsumed() }) {

                // TODO(b/162269614): Should be update to only have one velocity tracker that
                //  tracks the average change overtime, instead of one for each finger.

                var velocityTracker: VelocityTracker? = null

                changes.fastForEach {
                    // This pointer is up (consumed or not), so we should stop tracking
                    // information about it.  If the pointer is not locked out of our
                    // orientation, get the velocity tracker because this might be a fling.
                    if (it.changedToUp() && changes.contains(it)) {
                        velocityTracker = velocityTrackers.remove(it.id)
                    } else if (it.changedToUpIgnoreConsumed()) {
                        velocityTrackers.remove(it.id)
                    }
                    // removing stored down position for the pointer.
                    if (it.changedToUp()) {
                        downPositions.remove(it.id)
                    }
                }

                if (changes.all { it.changedToUpIgnoreConsumed() }) {
                    // All of the pointers are up, so reset and call onStop.  If we have a
                    // velocityTracker at this point, that means at least one of the up events
                    // was not consumed so we should send velocity for flinging.
                    if (started) {
                        val velocity: Offset? =
                            if (velocityTracker != null) {
                                changes.fastForEach {
                                    it.consumeDownChange()
                                }
                                val velocity = velocityTracker!!.calculateVelocity()
                                Offset(velocity.x, velocity.y)
                            } else {
                                null
                            }
                        started = false
                        dragObserver.onStop(velocity ?: Offset.Zero)
                        reset()
                    }
                }
            }

            // Handle down changes: for each new pointer that has been added, start tracking
            // information about it.
            if (changes.fastAny { it.changedToDownIgnoreConsumed() }) {
                changes.fastForEach {
                    // If a pointer has changed to down, we should start tracking information
                    // about it.
                    if (it.changedToDownIgnoreConsumed()) {
                        velocityTrackers[it.id] = VelocityTracker()
                            .apply {
                                addPosition(
                                    it.uptimeMillis,
                                    it.position
                                )
                            }
                        downPositions[it.id] = it.position
                    }
                }
            }

            // Handle moved changes.

            val movedChanges = changes.filter {
                it.pressed && !it.changedToDownIgnoreConsumed()
            }

            movedChanges.fastForEach {
                // TODO(shepshapard): handle the case that the pointerTrackingData is null,
                //  either with an exception or a logged error, or something else.
                // TODO(shepshapard): VelocityTracker needs to be updated to not accept
                //  position information, but rather vector information about movement.
                // TODO(b/162269614): Should be update to only have one velocity tracker that
                //  tracks the average change overtime, instead of one for each finger.
                velocityTrackers[it.id]?.addPosition(
                    it.uptimeMillis,
                    it.position
                )
            }

            // Check to see if we are already started so we don't have to call canStartDragging
            // again.
            val canStart = !started && canStartDragging?.invoke() ?: true

            // At this point, check to see if we have started, and if we have, we may
            // be calling onDrag and updating change information on the PointerInputChanges.
            if (started || canStart) {

                var totalDx = 0f
                var totalDy = 0f

                movedChanges.fastForEach {
                    if (movedChanges.contains(it)) {
                        totalDx += it.positionChange().x
                    }
                    if (movedChanges.contains(it)) {
                        totalDy += it.positionChange().y
                    }
                }

                if (totalDx != 0f || totalDy != 0f) {

                    // At this point, if we have not started, check to see if we should start
                    // and if we should, update our state and call onStart().
                    if (!started) {
                        started = true
                        dragObserver.onStart(downPositions.values.averagePosition())
                        downPositions.clear()
                    }

                    val consumed = dragObserver.onDrag(
                        Offset(
                            totalDx / changes.size,
                            totalDy / changes.size
                        )
                    )

                    if (consumed.x != 0f || consumed.y != 0f) {
                        movedChanges.fastForEach { it.consumePositionChange() }
                    }
                }
            }
        }
    }

    override fun onCancel() {
        downPositions.clear()
        velocityTrackers.clear()
        if (started) {
            started = false
            dragObserver.onCancel()
        }
        reset()
    }

    private fun reset() {
        downPositions.clear()
        velocityTrackers.clear()
    }
}

private fun Iterable<Offset>.averagePosition(): Offset {
    var x = 0f
    var y = 0f
    this.forEach {
        x += it.x
        y += it.y
    }
    return Offset(x / count(), y / count())
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

/**
 * Reacts if the first pointer input change it sees is an unconsumed down change, and if it reacts,
 * consumes all further down changes.
 *
 * This GestureDetector is not generally intended to be used directly, but is instead intended to be
 * used as a building block to create more complex GestureDetectors.
 *
 * This GestureDetector is a bit more experimental then the other GestureDetectors (the number and
 * types of GestureDetectors is still very much a work in progress) and is intended to be a
 * generically useful building block for more complicated GestureDetectors.
 *
 * The theory is that this GestureDetector can be reused in PressIndicatorGestureDetector, and there
 * could be a corresponding RawPressReleasedGestureDetector.
 *
 * @param onPressStart Called when the first pointer "presses" on the GestureDetector.  [Offset]
 * is the position of that first pointer on press.
 * @param enabled If false, this GestureDetector will effectively act as if it is not in the
 * hierarchy.
 * @param executionPass The [PointerEventPass] during which this GestureDetector will attempt to
 * react to and consume down changes.  Defaults to [PointerEventPass.Main].
 */
@Deprecated("Use Modifier.pointerInput{} with custom gesture detection code")
internal fun Modifier.rawPressStartGestureFilter(
    onPressStart: (Offset) -> Unit,
    enabled: Boolean = false,
    executionPass: PointerEventPass = PointerEventPass.Main
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "rawPressStartGestureFilter"
        properties["onPressStart"] = onPressStart
        properties["enabled"] = enabled
        properties["executionPass"] = executionPass
    }
) {
    val filter = remember { RawPressStartGestureFilter() }
    filter.onPressStart = onPressStart
    filter.setEnabled(enabled = enabled)
    filter.setExecutionPass(executionPass)
    DragPointerInputModifierImpl(filter)
}

internal class RawPressStartGestureFilter : PointerInputFilter() {

    lateinit var onPressStart: (Offset) -> Unit
    private var enabled: Boolean = true
    private var executionPass = PointerEventPass.Initial

    private var active = false

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == executionPass) {
            if (enabled && changes.all { it.changedToDown() }) {
                // If we have not yet started and all of the changes changed to down, we are
                // starting.
                active = true
                onPressStart(changes.first().position)
            } else if (changes.all { it.changedToUp() }) {
                // If we have started and all of the changes changed to up, we are stopping.
                active = false
            }

            if (active) {
                // If we have started, we should consume the down change on all changes.
                changes.fastForEach {
                    it.consumeDownChange()
                }
            }
        }
    }

    override fun onCancel() {
        active = false
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        // Whenever we are disabled, we can just go ahead and become inactive (which is the state we
        // should be in if we are to pretend that we aren't in the hierarchy.
        if (!enabled) {
            onCancel()
        }
    }

    fun setExecutionPass(executionPass: PointerEventPass) {
        this.executionPass = executionPass
    }
}

internal fun Modifier.dragSlopExceededGestureFilter(
    onDragSlopExceeded: () -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dragSlopExceededGestureFilter"
        properties["onDragSlopExceeded"] = onDragSlopExceeded
    }
) {
    val touchSlop = with(LocalDensity.current) { TouchSlop.toPx() }
    val filter = remember {
        DragSlopExceededGestureFilter(touchSlop)
    }
    filter.onDragSlopExceeded = onDragSlopExceeded
    DragPointerInputModifierImpl(filter)
}

internal class DragSlopExceededGestureFilter(
    private val touchSlop: Float
) : PointerInputFilter() {
    private var dxForPass = 0f
    private var dyForPass = 0f
    private var dxUnderSlop = 0f
    private var dyUnderSlop = 0f
    private var passedSlop = false

    private var canDrag: ((Direction) -> Boolean)? = null

    var onDragSlopExceeded: () -> Unit = {}

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {

        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main || pass == PointerEventPass.Final) {

            // Filter changes for those that we can interact with due to our orientation.

            if (!passedSlop) {

                // Get current average change.
                val averagePositionChange = getAveragePositionChange(changes)
                val dx = averagePositionChange.x
                val dy = averagePositionChange.y

                // Track changes during main and during final.  This allows for fancy dragging
                // due to a parent being dragged and will likely be removed.
                // TODO(b/157087973): Likely remove this two pass complexity.
                if (pass == PointerEventPass.Main) {
                    dxForPass = dx
                    dyForPass = dy
                    dxUnderSlop += dx
                    dyUnderSlop += dy
                } else {
                    dxUnderSlop += dx - dxForPass
                    dyUnderSlop += dy - dyForPass
                }

                // Map the distance to the direction enum for a call to canDrag.
                val directionX = averagePositionChange.horizontalDirection()
                val directionY = averagePositionChange.verticalDirection()

                val canDragX = directionX != null && canDrag?.invoke(directionX) ?: true
                val canDragY = directionY != null && canDrag?.invoke(directionY) ?: true

                val passedSlopX = canDragX && abs(dxUnderSlop) > touchSlop
                val passedSlopY = canDragY && abs(dyUnderSlop) > touchSlop

                if (passedSlopX || passedSlopY) {
                    passedSlop = true
                    onDragSlopExceeded.invoke()
                } else {
                    // If we have passed slop in a direction that we can't drag in, we should reset
                    // our tracking back to zero so that a user doesn't have to later scroll the slop
                    // + the extra distance they scrolled in the wrong direction.
                    if (!canDragX &&
                        (
                            (directionX == Direction.LEFT && dxUnderSlop < 0) ||
                                (directionX == Direction.RIGHT && dxUnderSlop > 0)
                            )
                    ) {
                        dxUnderSlop = 0f
                    }
                    if (!canDragY &&
                        (
                            (directionY == Direction.UP && dyUnderSlop < 0) ||
                                (directionY == Direction.DOWN && dyUnderSlop > 0)
                            )
                    ) {
                        dyUnderSlop = 0f
                    }
                }
            }

            if (pass == PointerEventPass.Final &&
                changes.all { it.changedToUpIgnoreConsumed() }
            ) {
                // On the final pass, check to see if all pointers have changed to up, and if they
                // have, reset.
                reset()
            }
        }
    }

    override fun onCancel() {
        reset()
    }

    private fun reset() {
        passedSlop = false
        dxForPass = 0f
        dyForPass = 0f
        dxUnderSlop = 0f
        dyUnderSlop = 0f
    }
}

/**
 * Gets the average distance change of all pointers as an Offset.
 */
private fun getAveragePositionChange(changes: List<PointerInputChange>): Offset {
    if (changes.isEmpty()) {
        return Offset.Zero
    }

    val sum = changes.fold(Offset.Zero) { sum, change ->
        sum + change.positionChange()
    }
    val sizeAsFloat = changes.size.toFloat()
    // TODO(b/148980115): Once PxPosition is removed, sum will be an Offset, and this line can
    //  just be straight division.
    return Offset(sum.x / sizeAsFloat, sum.y / sizeAsFloat)
}

/**
 * Maps an [Offset] value to a horizontal [Direction].
 */
private fun Offset.horizontalDirection() =
    when {
        this.x < 0f -> Direction.LEFT
        this.x > 0f -> Direction.RIGHT
        else -> null
    }

/**
 * Maps a [Offset] value to a vertical [Direction].
 */
private fun Offset.verticalDirection() =
    when {
        this.y < 0f -> Direction.UP
        this.y > 0f -> Direction.DOWN
        else -> null
    }

private data class DragPointerInputModifierImpl(
    override val pointerInputFilter: PointerInputFilter
) : PointerInputModifier

@Deprecated("Use Modifier.pointerInput {detectDragGesturesAfterLongPress(...)} instead")
internal interface LongPressDragObserver {

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
 * [LongPressDragObserver.onStop] is called when the gesture ends due to all of the pointers
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
internal fun Modifier.longPressDragGestureFilter(
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
        .then(LongPointerInputModifierImpl(glue))
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
    LongPointerInputModifierImpl(filter)
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
            changes.fastAny { it.positionChangeConsumed() }
        ) {
            // If we are anything but Idle and something consumed movement, reset.
            resetToIdle()
        }
    }

    // TODO(shepshapard): This continues to be very confusing to use.  Have to come up with a better
//  way of easily expressing this.
    /**
     * Utility method that determines if any pointers are currently in [bounds].
     *
     * A pointer is considered in bounds if it is currently down and it's current
     * position is within the provided [bounds]
     *
     * @return True if at least one pointer is in bounds.
     */
    private fun List<PointerInputChange>.anyPointersInBounds(bounds: IntSize) =
        fastAny {
            it.pressed &&
                it.position.x >= 0 &&
                it.position.x < bounds.width &&
                it.position.y >= 0 &&
                it.position.y < bounds.height
        }

    override fun onCancel() {
        resetToIdle()
    }

    private fun fireLongPress() {
        state = State.Fired
        onLongPress.invoke(pointerPositions.asIterable().first().value)
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

private data class LongPointerInputModifierImpl(
    override val pointerInputFilter: PointerInputFilter
) : PointerInputModifier