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
import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/**
 * This gesture filter detects when the average distance change of all pointers surpasses the touch
 * slop.
 *
 * The value of touch slop is currently defined internally as the constant [TouchSlop].
 *
 * Note: [canDrag] and [orientation] interact such that [canDrag] will only be called for
 * [Direction]s that are included in the given [orientation].
 *
 * Note: Changing the value of [orientation] will reset the gesture filter such that it will not
 * respond to input until new pointers are detected.
 *
 * @param onDragSlopExceeded Called when touch slop is exceeded in a supported direction and
 * orientation.
 * @param canDrag Set to limit the types of directions under which touch slop can be exceeded.
 * Return true if you want a drag to be started due to the touch slop being surpassed in the
 * given [Direction]. If [canDrag] is not provided, touch slop will be able to be exceeded in all
 * directions.
 * @param orientation If provided, limits the [Direction]s that scroll slop can be exceeded in to
 * those that are included in the given orientation and does not consider pointers that are locked
 * to other orientations.
 */
fun Modifier.dragSlopExceededGestureFilter(
    onDragSlopExceeded: () -> Unit,
    canDrag: ((Direction) -> Boolean)? = null,
    orientation: Orientation? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dragSlopExceededGestureFilter"
        properties["onDragSlopExceeded"] = onDragSlopExceeded
        properties["canDrag"] = canDrag
        properties["orientation"] = orientation
    }
) {
    val touchSlop = with(AmbientDensity.current) { TouchSlop.toPx() }
    val filter = remember {
        DragSlopExceededGestureFilter(touchSlop)
    }
    filter.onDragSlopExceeded = onDragSlopExceeded
    filter.setDraggableData(orientation, canDrag)
    PointerInputModifierImpl(filter)
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
    private var orientation: Orientation? = null

    var onDragSlopExceeded: () -> Unit = {}

    lateinit var scrollOrientationLocker: ScrollOrientationLocker
    lateinit var customEventDispatcher: CustomEventDispatcher

    fun setDraggableData(orientation: Orientation?, canDrag: ((Direction) -> Boolean)?) {
        this.orientation = orientation
        this.canDrag = { direction ->
            when {
                orientation == Orientation.Horizontal && direction == Direction.UP -> false
                orientation == Orientation.Horizontal && direction == Direction.DOWN -> false
                orientation == Orientation.Vertical && direction == Direction.LEFT -> false
                orientation == Orientation.Vertical && direction == Direction.RIGHT -> false
                else -> canDrag?.invoke(direction) ?: true
            }
        }
    }

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {

        val changes = pointerEvent.changes

        scrollOrientationLocker.onPointerInputSetup(changes, pass)

        if (pass == PointerEventPass.Main || pass == PointerEventPass.Final) {

            // Filter changes for those that we can interact with due to our orientation.
            val applicableChanges =
                with(orientation) {
                    if (this != null) {
                        scrollOrientationLocker.getPointersFor(changes, this)
                    } else {
                        changes
                    }
                }

            if (!passedSlop) {

                // Get current average change.
                val averagePositionChange = getAveragePositionChange(applicableChanges)
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

        scrollOrientationLocker.onPointerInputTearDown(changes, pass)
    }

    override fun onCancel() {
        scrollOrientationLocker.onCancel()
        reset()
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        scrollOrientationLocker.onCustomEvent(customEvent, pass)
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
 * Get's the average distance change of all pointers as an Offset.
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