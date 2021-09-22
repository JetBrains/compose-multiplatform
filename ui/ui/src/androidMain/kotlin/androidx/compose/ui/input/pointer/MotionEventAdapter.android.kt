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

package androidx.compose.ui.input.pointer

import android.os.Build
import android.util.SparseLongArray
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset

/**
 * Converts Android framework [MotionEvent]s into Compose [PointerInputEvent]s.
 */
internal class MotionEventAdapter {

    private var nextId = 0L

    /**
     * Whenever a new MotionEvent pointer is added, we create a new PointerId that is associated
     * with it. This holds that association.
     */
    @VisibleForTesting
    internal val motionEventToComposePointerIdMap = SparseLongArray()

    private val pointers: MutableList<PointerInputEventData> = mutableListOf()

    /**
     * The most recent hover event's tool type.
     */
    private var hoverPointerToolType: Int = -1

    /**
     * The most recent hover event's source type.
     */
    private var hoverPointerSource: Int = -1

    /**
     * Converts a single [MotionEvent] from an Android event stream into a [PointerInputEvent], or
     * null if the [MotionEvent.getActionMasked] is [ACTION_CANCEL].
     *
     * All MotionEvents should be passed to this method so that it can correctly maintain it's
     * internal state.
     *
     * @param motionEvent The MotionEvent to process.
     *
     * @return The PointerInputEvent or null if the event action was ACTION_CANCEL.
     */
    internal fun convertToPointerInputEvent(
        motionEvent: MotionEvent,
        positionCalculator: PositionCalculator
    ): PointerInputEvent? {
        val action = motionEvent.actionMasked
        if (action == ACTION_CANCEL) {
            hoverPointerToolType = -1
            hoverPointerSource = -1
            motionEventToComposePointerIdMap.clear()
            return null
        }
        addFreshIds(motionEvent)

        val isHover = action == ACTION_HOVER_EXIT || action == ACTION_HOVER_MOVE ||
            action == ACTION_HOVER_ENTER

        val upIndex = when (action) {
            ACTION_UP -> 0
            ACTION_POINTER_UP -> motionEvent.actionIndex
            else -> -1
        }

        pointers.clear()

        // This converts the MotionEvent into a list of PointerInputEventData, and updates
        // internal record keeping.
        for (i in 0 until motionEvent.pointerCount) {
            pointers.add(
                createPointerInputEventData(
                    positionCalculator,
                    motionEvent,
                    i,
                    !isHover && i != upIndex
                )
            )
        }

        removeStaleIds(motionEvent)

        return PointerInputEvent(
            motionEvent.eventTime,
            pointers,
            motionEvent
        )
    }

    /**
     * Add any new pointer IDs. If a pointer is new, [hoverPointerToolType] and [hoverPointerSource]
     * are cleared and possibly set to new values if the new pointer hovers.
     */
    private fun addFreshIds(motionEvent: MotionEvent) {
        if (hoverPointerSource == -1) {
            addNewIdIfNecessary(motionEvent)
            return
        }
        if (motionEvent.pointerCount == 1 &&
            motionEventToComposePointerIdMap.size() == 1 &&
            motionEventToComposePointerIdMap.indexOfKey(motionEvent.getPointerId(0)) == 0 &&
            motionEvent.isFromSource(hoverPointerSource) &&
            motionEvent.getToolType(0) == hoverPointerToolType
        ) {
            return // nothing to add. This is the same one as the last event.
        }
        hoverPointerSource = -1
        hoverPointerToolType = -1
        addNewIdIfNecessary(motionEvent)
    }

    /**
     * If there is a new pointer, it is added. If the new pointer is hovering, [hoverPointerSource]
     * and [hoverPointerToolType] are set to the new pointer's values.
     */
    private fun addNewIdIfNecessary(motionEvent: MotionEvent) {
        when (motionEvent.actionMasked) {
            ACTION_HOVER_ENTER -> {
                // Must be a new hoverable source
                hoverPointerSource = motionEvent.source
                hoverPointerToolType = motionEvent.getToolType(0)
                motionEventToComposePointerIdMap.clear()
                motionEventToComposePointerIdMap.put(motionEvent.getPointerId(0), nextId++)
            }
            ACTION_DOWN -> {
                motionEventToComposePointerIdMap.clear()
                motionEventToComposePointerIdMap.put(motionEvent.getPointerId(0), nextId++)
            }
            ACTION_POINTER_DOWN -> {
                val index = motionEvent.actionIndex
                motionEventToComposePointerIdMap.put(motionEvent.getPointerId(index), nextId++)
            }
        }
    }

    /**
     * Remove an existing pointer.
     */
    private fun removePointerId(motionEventPointerId: Int) {
        val index = motionEventToComposePointerIdMap.indexOfKey(motionEventPointerId)
        check(index >= 0) {
            "Trying to remove pointer ID $motionEventPointerId that doesn't exist"
        }
        motionEventToComposePointerIdMap.removeAt(index)
    }

    /**
     * Remove any raised pointers if they didn't previously hover. Anything that hovers
     * will stay until a different event causes it to be removed.
     */
    private fun removeStaleIds(motionEvent: MotionEvent) {
        when (motionEvent.actionMasked) {
            ACTION_POINTER_UP -> removePointerId(motionEvent.getPointerId(motionEvent.actionIndex))
            ACTION_UP ->
                if (hoverPointerSource == -1) {
                    // This wasn't hovering, so we can remove it.
                    check(motionEventToComposePointerIdMap.size() == 1) {
                        "Should be removing the last pointer ID, but there are " +
                            "${motionEventToComposePointerIdMap.size()}"
                    }
                    removePointerId(motionEvent.getPointerId(0))
                }
        }
    }

    /**
     * Creates a new PointerInputEventData.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    private fun createPointerInputEventData(
        positionCalculator: PositionCalculator,
        motionEvent: MotionEvent,
        index: Int,
        pressed: Boolean
    ): PointerInputEventData {

        val motionEventPointerId = motionEvent.getPointerId(index)

        val pointerIndex = motionEventToComposePointerIdMap.indexOfKey(motionEventPointerId)
        check(pointerIndex >= 0) {
            "Compose assumes that all pointer ids in MotionEvents are first provided " +
                "alongside ACTION_DOWN, ACTION_POINTER_DOWN, or ACTION_HOVER_ENTER.  Instead" +
                " the first event was seen for ID $motionEventPointerId with " +
                MotionEvent.actionToString(motionEvent.actionMasked)
        }
        val pointerId = PointerId(motionEventToComposePointerIdMap.valueAt(pointerIndex))

        var position = Offset(motionEvent.getX(index), motionEvent.getY(index))
        val rawPosition: Offset
        if (index == 0) {
            rawPosition = Offset(motionEvent.rawX, motionEvent.rawY)
            position = positionCalculator.screenToLocal(rawPosition)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rawPosition = MotionEventHelper.toRawOffset(motionEvent, index)
            position = positionCalculator.screenToLocal(rawPosition)
        } else {
            rawPosition = positionCalculator.localToScreen(position)
        }
        val toolType = when (motionEvent.getToolType(index)) {
            MotionEvent.TOOL_TYPE_UNKNOWN -> PointerType.Unknown
            MotionEvent.TOOL_TYPE_FINGER -> PointerType.Touch
            MotionEvent.TOOL_TYPE_STYLUS -> PointerType.Stylus
            MotionEvent.TOOL_TYPE_MOUSE -> PointerType.Mouse
            MotionEvent.TOOL_TYPE_ERASER -> PointerType.Eraser
            else -> PointerType.Unknown
        }

        val historical = mutableListOf<HistoricalChange>()
        with(motionEvent) {
            repeat(getHistorySize()) { pos ->
                val historicalChange = HistoricalChange(
                    getHistoricalEventTime(pos),
                    Offset(getHistoricalX(index, pos), getHistoricalY(index, pos))
                )
                historical.add(historicalChange)
            }
        }

        return PointerInputEventData(
            pointerId,
            motionEvent.eventTime,
            rawPosition,
            position,
            pressed,
            toolType,
            historical
        )
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.Q)
private object MotionEventHelper {
    @DoNotInline
    fun toRawOffset(motionEvent: MotionEvent, index: Int): Offset {
        return Offset(motionEvent.getRawX(index), motionEvent.getRawY(index))
    }
}
