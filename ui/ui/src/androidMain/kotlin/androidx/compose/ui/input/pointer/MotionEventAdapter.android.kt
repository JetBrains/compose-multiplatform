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
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
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
    internal val motionEventToComposePointerIdMap: MutableMap<Int, PointerId> = mutableMapOf()

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

        if (motionEvent.actionMasked == ACTION_CANCEL) {
            motionEventToComposePointerIdMap.clear()
            return null
        }

        val downIndex = when (motionEvent.actionMasked) {
            ACTION_POINTER_DOWN -> motionEvent.actionIndex
            ACTION_DOWN -> 0
            else -> null
        }

        val upIndex = when (motionEvent.actionMasked) {
            ACTION_POINTER_UP -> motionEvent.actionIndex
            ACTION_UP -> 0
            else -> null
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
                    downIndex,
                    upIndex
                )
            )
        }

        return PointerInputEvent(
            motionEvent.eventTime,
            pointers,
            motionEvent
        )
    }

    /**
     * Creates a new PointerInputEventData.
     */
    private fun createPointerInputEventData(
        positionCalculator: PositionCalculator,
        motionEvent: MotionEvent,
        index: Int,
        downIndex: Int?,
        upIndex: Int?
    ): PointerInputEventData {

        val motionEventPointerId = motionEvent.getPointerId(index)

        val pointerId =
            when (index) {
                downIndex ->
                    PointerId(nextId++).also {
                        motionEventToComposePointerIdMap[motionEventPointerId] = it
                    }
                upIndex ->
                    motionEventToComposePointerIdMap.remove(motionEventPointerId)
                else ->
                    motionEventToComposePointerIdMap[motionEventPointerId]
            } ?: throw IllegalStateException(
                "Compose assumes that all pointer ids in MotionEvents are first provided " +
                    "alongside ACTION_DOWN or ACTION_POINTER_DOWN.  This appears not " +
                    "to have been the case"
            )

        return createPointerInputEventData(
            positionCalculator,
            pointerId,
            motionEvent.eventTime,
            motionEvent,
            index,
            upIndex
        )
    }
}

/**
 * Creates a new PointerInputData.
 */
private fun createPointerInputEventData(
    positionCalculator: PositionCalculator,
    pointerId: PointerId,
    timestamp: Long,
    motionEvent: MotionEvent,
    index: Int,
    upIndex: Int?
): PointerInputEventData {
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

    return PointerInputEventData(
        pointerId,
        timestamp,
        rawPosition,
        position,
        index != upIndex,
        toolType
    )
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
