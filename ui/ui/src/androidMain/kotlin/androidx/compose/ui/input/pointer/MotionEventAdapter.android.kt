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
import android.util.SparseBooleanArray
import android.util.SparseLongArray
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
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
    private val canHover = SparseBooleanArray()

    private val pointers = mutableListOf<PointerInputEventData>()

    /**
     * The previous event's tool type. This is used in combination with [previousSource] to
     * determine when a different device was used to send events.
     */
    private var previousToolType = -1

    /**
     * The previous event's source. This is used in combination with [previousToolType] to
     * determine when a different device was used to send events.
     */
    private var previousSource = -1

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
            canHover.clear()
            return null
        }
        clearOnDeviceChange(motionEvent)

        addFreshIds(motionEvent)

        val isHover = action == ACTION_HOVER_EXIT || action == ACTION_HOVER_MOVE ||
            action == ACTION_HOVER_ENTER
        val isScroll = action == ACTION_SCROLL

        if (isHover) {
            val hoverId = motionEvent.getPointerId(motionEvent.actionIndex)
            canHover.put(hoverId, true)
        }

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
                    // "pressed" means:
                    // 1. we're not hovered
                    // 2. we didn't get UP event for a pointer
                    // 3. button on the mouse is pressed BUT it's not a "scroll" simulated button
                    !isHover && i != upIndex && (!isScroll || motionEvent.buttonState != 0)
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
     * An ACTION_DOWN or ACTION_POINTER_DOWN was received, but not handled, so the stream should
     * be considered ended.
     */
    fun endStream(pointerId: Int) {
        canHover.delete(pointerId)
        motionEventToComposePointerIdMap.delete(pointerId)
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
                if (motionEventToComposePointerIdMap.indexOfKey(pointerId) < 0) {
                    motionEventToComposePointerIdMap.put(pointerId, nextId++)
                    if (motionEvent.getToolType(actionIndex) == TOOL_TYPE_MOUSE) {
                        canHover.put(pointerId, true)
                    }
                }
            }
        }
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
                val pointerId = motionEvent.getPointerId(actionIndex)
                if (!canHover.get(pointerId, false)) {
                    motionEventToComposePointerIdMap.delete(pointerId)
                    canHover.delete(pointerId)
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
                    canHover.delete(pointerId)
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
     * When the device has changed (noted by source and tool type), we don't need to track
     * any of the previous pointers.
     */
    private fun clearOnDeviceChange(motionEvent: MotionEvent) {
        if (motionEvent.pointerCount != 1) {
            return
        }
        val toolType = motionEvent.getToolType(0)
        val source = motionEvent.source

        if (toolType != previousToolType || source != previousSource) {
            previousToolType = toolType
            previousSource = source
            canHover.clear()
            motionEventToComposePointerIdMap.clear()
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

        val pointerId = getComposePointerId(motionEventPointerId)

        val pressure = motionEvent.getPressure(index)

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
            repeat(historySize) { pos ->
                val x = getHistoricalX(index, pos)
                val y = getHistoricalY(index, pos)
                if (x.isFinite() && y.isFinite()) {
                    val historicalChange = HistoricalChange(
                        getHistoricalEventTime(pos),
                        Offset(x, y)
                    )
                    historical.add(historicalChange)
                }
            }
        }
        val scrollDelta = if (motionEvent.actionMasked == ACTION_SCROLL) {
            val x = motionEvent.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val y = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)
            // NOTE: we invert the y scroll offset because android is special compared to other
            // platforms and uses the opposite sign for vertical mouse wheel scrolls. In order to
            // support better x-platform mouse scroll, we invert the y-offset to be in line with
            // desktop and web.
            //
            // This looks more natural, because when we scroll mouse wheel up,
            // we move the wheel point (that touches the finger) up. And if we work in the usual
            // coordinate system, it means we move that point by "-1".
            //
            // Web also behaves this way. See deltaY:
            // https://developer.mozilla.org/en-US/docs/Web/API/Element/wheel_event
            // https://jsfiddle.net/27zwteog
            // (wheelDelta on the other hand is deprecated and inverted)
            //
            // We then add 0f to prevent injecting -0.0f into the pipeline, which can be
            // problematic when doing comparisons.
            Offset(x, -y + 0f)
        } else {
            Offset.Zero
        }

        val issuesEnterExit = canHover.get(motionEvent.getPointerId(index), false)
        return PointerInputEventData(
            pointerId,
            motionEvent.eventTime,
            rawPosition,
            position,
            pressed,
            pressure,
            toolType,
            issuesEnterExit,
            historical,
            scrollDelta
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
