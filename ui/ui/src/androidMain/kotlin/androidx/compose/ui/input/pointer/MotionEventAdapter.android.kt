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
import android.view.MotionEvent.TOOL_TYPE_ERASER
import android.view.MotionEvent.TOOL_TYPE_FINGER
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import android.view.MotionEvent.TOOL_TYPE_STYLUS
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
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
     * Add any new pointer IDs.
     */
    private fun addFreshIds(motionEvent: MotionEvent) {
        when (motionEvent.actionMasked) {
            ACTION_HOVER_ENTER -> {
                val pointerId = motionEvent.getPointerId(0)
                if (motionEventToComposePointerIdMap.indexOfKey(pointerId) < 0) {
                    motionEventToComposePointerIdMap.put(pointerId, nextId++)
                }
            }
            ACTION_DOWN,
            ACTION_POINTER_DOWN -> {
                val actionIndex = motionEvent.actionIndex
                val pointerId = motionEvent.getPointerId(actionIndex)
                if (motionEventToComposePointerIdMap.indexOfKey(pointerId) < 0 ||
                    motionEvent.getToolType(actionIndex) != TOOL_TYPE_MOUSE
                ) {
                    motionEventToComposePointerIdMap.put(pointerId, nextId++)
                }
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
            ACTION_POINTER_UP,
            ACTION_UP -> {
                val actionIndex = motionEvent.actionIndex
                if (motionEvent.getToolType(actionIndex) != TOOL_TYPE_MOUSE) {
                    removePointerId(motionEvent.getPointerId(actionIndex))
                }
            }
        }

        // Remove any IDs that don't currently exist in the MotionEvent.
        // This can happen, for example, when a mouse cursor disappears and the next
        // event is a touch event.
        if (motionEventToComposePointerIdMap.size() > motionEvent.pointerCount) {
            for (i in motionEventToComposePointerIdMap.size() - 1 downTo 0) {
                val pointerId = motionEventToComposePointerIdMap.keyAt(i)
                if (!motionEvent.hasPointerId(pointerId)) {
                    motionEventToComposePointerIdMap.removeAt(i)
                }
            }
        }
    }

    private fun MotionEvent.hasPointerId(pointerId: Int): Boolean {
        for (i in 0 until pointerCount) {
            if (getPointerId(i) == pointerId) {
                return true
            }
        }
        return false
    }

    private fun getComposePointerId(motionEventPointerId: Int): PointerId {
        val pointerIndex = motionEventToComposePointerIdMap.indexOfKey(motionEventPointerId)
        val id = if (pointerIndex >= 0) {
            motionEventToComposePointerIdMap.valueAt(pointerIndex)
        } else {
            // An unexpected pointer was added or we may have previously removed it
            val newId = nextId++
            motionEventToComposePointerIdMap.put(motionEventPointerId, newId)
            newId
        }
        return PointerId(id)
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

        val pointerId = getComposePointerId(motionEventPointerId)

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
            TOOL_TYPE_UNKNOWN -> PointerType.Unknown
            TOOL_TYPE_FINGER -> PointerType.Touch
            TOOL_TYPE_STYLUS -> PointerType.Stylus
            TOOL_TYPE_MOUSE -> PointerType.Mouse
            TOOL_TYPE_ERASER -> PointerType.Eraser
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
