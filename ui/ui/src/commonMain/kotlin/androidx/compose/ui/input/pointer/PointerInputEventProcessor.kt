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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.util.fastForEach

internal interface PositionCalculator {
    fun screenToLocal(positionOnScreen: Offset): Offset
    fun localToScreen(localPosition: Offset): Offset
}

/**
 * The core element that receives [PointerInputEvent]s and process them in Compose UI.
 */
@OptIn(InternalCoreApi::class)
internal class PointerInputEventProcessor(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker(root.coordinates)
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()
    private val hitResult = HitTestResult<PointerInputFilter>()

    /**
     * [process] doesn't currently support reentrancy. This prevents reentrant calls
     * from causing a crash with an early exit.
     */
    private var isProcessing = false

    /**
     * Receives [PointerInputEvent]s and process them through the tree rooted on [root].
     *
     * @param pointerEvent The [PointerInputEvent] to process.
     *
     * @return the result of processing.
     *
     * @see ProcessResult
     * @see PointerInputEvent
     */
    fun process(
        pointerEvent: PointerInputEvent,
        positionCalculator: PositionCalculator,
        isInBounds: Boolean = true
    ): ProcessResult {
        if (isProcessing) {
            // Processing currently does not support reentrancy.
            return ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false
            )
        }
        try {
            isProcessing = true

            // Gets a new PointerInputChangeEvent with the PointerInputEvent.
            val internalPointerEvent =
                pointerInputChangeEventProducer.produce(pointerEvent, positionCalculator)

            val isHover =
                !internalPointerEvent.changes.values.any { it.pressed || it.previousPressed }

            // Add new hit paths to the tracker due to down events.
            internalPointerEvent.changes.values.forEach { pointerInputChange ->
                if (isHover || pointerInputChange.changedToDownIgnoreConsumed()) {
                    val isTouchEvent = pointerInputChange.type == PointerType.Touch
                    root.hitTest(pointerInputChange.position, hitResult, isTouchEvent)
                    if (hitResult.isNotEmpty()) {
                        hitPathTracker.addHitPath(pointerInputChange.id, hitResult)
                        hitResult.clear()
                    }
                }
            }

            // Remove [PointerInputFilter]s that are no longer valid and refresh the offset information
            // for those that are.
            hitPathTracker.removeDetachedPointerInputFilters()

            // Dispatch to PointerInputFilters
            val dispatchedToSomething =
                hitPathTracker.dispatchChanges(internalPointerEvent, isInBounds)

            val anyMovementConsumed = if (internalPointerEvent.suppressMovementConsumption) {
                false
            } else {
                internalPointerEvent.changes.values
                    .any { it.positionChangedIgnoreConsumed() && it.isConsumed }
            }

            return ProcessResult(dispatchedToSomething, anyMovementConsumed)
        } finally {
            isProcessing = false
        }
    }

    /**
     * Responds appropriately to Android ACTION_CANCEL events.
     *
     * Specifically, [PointerInputFilter.onCancel] is invoked on tracked [PointerInputFilter]s and
     * and this [PointerInputEventProcessor] is reset such that it is no longer tracking any
     * [PointerInputFilter]s and expects the next [PointerInputEvent] it processes to represent only
     * new pointers.
     */
    fun processCancel() {
        pointerInputChangeEventProducer.clear()
        hitPathTracker.processCancel()
    }
}

/**
 * Produces [InternalPointerEvent]s by tracking changes between [PointerInputEvent]s
 */
@OptIn(InternalCoreApi::class, ExperimentalComposeUiApi::class)
private class PointerInputChangeEventProducer {
    private val previousPointerInputData: MutableMap<PointerId, PointerInputData> = mutableMapOf()

    /**
     * Produces [InternalPointerEvent]s by tracking changes between [PointerInputEvent]s
     */
    fun produce(
        pointerInputEvent: PointerInputEvent,
        positionCalculator: PositionCalculator
    ): InternalPointerEvent {
        // Set initial capacity to avoid resizing - we know the size the map will be.
        val changes: MutableMap<PointerId, PointerInputChange> =
            LinkedHashMap(pointerInputEvent.pointers.size)
        pointerInputEvent.pointers.fastForEach {
            val previousTime: Long
            val previousPosition: Offset
            val previousDown: Boolean

            val previousData = previousPointerInputData[it.id]
            if (previousData == null) {
                previousTime = it.uptime
                previousPosition = it.position
                previousDown = false
            } else {
                previousTime = previousData.uptime
                previousDown = previousData.down
                previousPosition =
                    positionCalculator.screenToLocal(previousData.positionOnScreen)
            }

            changes[it.id] =
                PointerInputChange(
                    it.id,
                    it.uptime,
                    it.position,
                    it.down,
                    previousTime,
                    previousPosition,
                    previousDown,
                    false,
                    it.type,
                    it.historical,
                    it.scrollDelta
                )
            if (it.down) {
                previousPointerInputData[it.id] = PointerInputData(
                    it.uptime,
                    it.positionOnScreen,
                    it.down,
                    it.type
                )
            } else {
                previousPointerInputData.remove(it.id)
            }
        }

        return InternalPointerEvent(changes, pointerInputEvent)
    }

    /**
     * Clears all tracked information.
     */
    fun clear() {
        previousPointerInputData.clear()
    }

    private class PointerInputData(
        val uptime: Long,
        val positionOnScreen: Offset,
        val down: Boolean,
        val type: PointerType
    )
}

/**
 * The result of a call to [PointerInputEventProcessor.process].
 */
// TODO(shepshpard): Not sure if storing these values in a int is most efficient overall.
@kotlin.jvm.JvmInline
internal value class ProcessResult(private val value: Int) {
    val dispatchedToAPointerInputModifier
        get() = (value and 1) != 0

    val anyMovementConsumed
        get() = (value and (1 shl 1)) != 0
}

/**
 * Constructs a new ProcessResult.
 *
 * @param dispatchedToAPointerInputModifier True if the dispatch resulted in at least 1
 * [PointerInputModifier] receiving the event.
 * @param anyMovementConsumed True if any movement occurred and was consumed.
 */
internal fun ProcessResult(
    dispatchedToAPointerInputModifier: Boolean,
    anyMovementConsumed: Boolean
): ProcessResult {
    val val1 = if (dispatchedToAPointerInputModifier) 1 else 0
    val val2 = if (anyMovementConsumed) (1 shl 1) else 0
    return ProcessResult(val1 or val2)
}
