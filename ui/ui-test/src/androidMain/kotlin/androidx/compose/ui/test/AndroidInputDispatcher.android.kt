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

package androidx.compose.ui.test

import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ViewRootForTest

private val MouseAsTouchEvents = listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP)

internal actual fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher {
    require(root is ViewRootForTest) {
        "InputDispatcher only supports dispatching to ViewRootForTest, not to " +
            root::class.java.simpleName
    }
    val view = root.view
    return AndroidInputDispatcher(testContext, root) {
        when (it.source) {
            InputDevice.SOURCE_TOUCHSCREEN -> {
                view.dispatchTouchEvent(it)
            }
            InputDevice.SOURCE_MOUSE -> {
                if (it.action in MouseAsTouchEvents) {
                    view.dispatchTouchEvent(it)
                } else {
                    view.dispatchGenericMotionEvent(it)
                }
            }
            else -> throw IllegalArgumentException(
                "Can't dispatch MotionEvents with source ${it.source}"
            )
        }
    }
}

internal class AndroidInputDispatcher(
    private val testContext: TestContext,
    private val root: ViewRootForTest?,
    private val sendEvent: (MotionEvent) -> Unit
) : InputDispatcher(testContext, root) {

    private val batchLock = Any()
    private var batchedEvents = mutableListOf<MotionEvent>()
    private var acceptEvents = true
    private var currentClockTime = currentTime

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.keys.sorted().indexOf(pointerId)
        )
    }

    override fun PartialGesture.enqueueMove() {
        enqueueTouchEvent(ACTION_MOVE, 0)
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_UP else ACTION_POINTER_UP,
            lastPositions.keys.sorted().indexOf(pointerId)
        )
    }

    override fun PartialGesture.enqueueCancel() {
        enqueueTouchEvent(ACTION_CANCEL, 0)
    }

    override fun MouseInputState.enqueuePress(buttonId: Int) {
        enqueueMouseEvent(if (hasOneButtonPressed) ACTION_DOWN else ACTION_MOVE)
        enqueueMouseEvent(ACTION_BUTTON_PRESS)
    }

    override fun MouseInputState.enqueueMove() {
        enqueueMouseEvent(if (isEntered) ACTION_HOVER_MOVE else ACTION_MOVE)
    }

    override fun MouseInputState.enqueueRelease(buttonId: Int) {
        enqueueMouseEvent(ACTION_BUTTON_RELEASE)
        enqueueMouseEvent(if (hasNoButtonsPressed) ACTION_UP else ACTION_MOVE)
    }

    override fun MouseInputState.enqueueEnter() {
        enqueueMouseEvent(ACTION_HOVER_ENTER)
    }

    override fun MouseInputState.enqueueExit() {
        enqueueMouseEvent(ACTION_HOVER_EXIT)
    }

    override fun MouseInputState.enqueueCancel() {
        enqueueMouseEvent(ACTION_CANCEL)
    }

    @OptIn(ExperimentalTestApi::class)
    override fun MouseInputState.enqueueScroll(delta: Float, scrollWheel: ScrollWheel) {
        enqueueMouseEvent(
            ACTION_SCROLL,
            delta,
            when (scrollWheel) {
                ScrollWheel.Horizontal -> MotionEvent.AXIS_HSCROLL
                ScrollWheel.Vertical -> MotionEvent.AXIS_VSCROLL
                else -> -1
            }
        )
    }

    /**
     * Generates a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture, and adds the MotionEvent to the batch.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    private fun PartialGesture.enqueueTouchEvent(action: Int, actionIndex: Int) {
        val entries = lastPositions.entries.sortedBy { it.key }
        enqueueTouchEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            actionIndex = actionIndex,
            coordinates = List(entries.size) { entries[it].value },
            pointerIds = List(entries.size) { entries[it].key }
        )
    }

    /**
     * Generates an event with the given parameters.
     */
    private fun enqueueTouchEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        actionIndex: Int,
        coordinates: List<Offset>,
        pointerIds: List<Int>
    ) {
        synchronized(batchLock) {
            check(acceptEvents) {
                "Can't enqueue touch event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "actionIndex=$actionIndex, " +
                    "pointerIds=$pointerIds, " +
                    "coordinates=$coordinates" +
                    "), events have already been (or are being) dispatched or disposed"
            }
            val positionInScreen = root?.let {
                val array = intArrayOf(0, 0)
                it.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            } ?: Offset.Zero
            batchedEvents.add(
                MotionEvent.obtain(
                    /* downTime = */ downTime,
                    /* eventTime = */ eventTime,
                    /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                    /* pointerCount = */ coordinates.size,
                    /* pointerProperties = */ Array(coordinates.size) {
                        MotionEvent.PointerProperties().apply {
                            id = pointerIds[it]
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    },
                    /* pointerCoords = */ Array(coordinates.size) {
                        MotionEvent.PointerCoords().apply {
                            x = positionInScreen.x + coordinates[it].x
                            y = positionInScreen.y + coordinates[it].y
                        }
                    },
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                    /* flags = */ 0
                ).apply {
                    offsetLocation(-positionInScreen.x, -positionInScreen.y)
                }
            )
        }
    }

    private fun MouseInputState.enqueueMouseEvent(action: Int, delta: Float = 0f, axis: Int = -1) {
        enqueueMouseEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            coordinate = lastPosition,
            buttonState = pressedButtons.fold(0) { state, buttonId -> state or buttonId },
            axis = axis,
            axisDelta = delta
        )
    }

    private fun enqueueMouseEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinate: Offset,
        buttonState: Int,
        axis: Int = -1,
        axisDelta: Float = 0f
    ) {
        synchronized(batchLock) {
            check(acceptEvents) {
                "Can't enqueue mouse event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "buttonState=$buttonState, " +
                    "axis=$axis, " +
                    "axisDelta=$axisDelta" +
                    "), events have already been (or are being) dispatched or disposed"
            }
            val positionInScreen = root?.let {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            } ?: Offset.Zero
            batchedEvents.add(
                MotionEvent.obtain(
                    /* downTime = */ downTime,
                    /* eventTime = */ eventTime,
                    /* action = */ action,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        MotionEvent.PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_MOUSE
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        MotionEvent.PointerCoords().apply {
                            x = positionInScreen.x + coordinate.x
                            y = positionInScreen.y + coordinate.y
                            if (axis != -1) {
                                setAxisValue(axis, axisDelta)
                            }
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ buttonState,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* flags = */ 0
                ).apply {
                    offsetLocation(-positionInScreen.x, -positionInScreen.y)
                }
            )
        }
    }

    override fun sendAllSynchronous() {
        // Must inject on the main thread, because it might modify View properties
        @OptIn(InternalTestApi::class)
        testContext.testOwner.runOnUiThread {
            checkAndStopAcceptingEvents()

            batchedEvents.forEach { event ->
                // Before injecting the next event, pump the clock
                // by the difference between this and the last event
                advanceClockTime(event.eventTime - currentClockTime)
                currentClockTime = event.eventTime
                sendAndRecycleEvent(event)
            }
        }
        // Each invocation of perform.*Input (Actions.kt) uses a new instance of an input
        // dispatcher, so we don't have to reset firstEventTime after use
    }

    @OptIn(InternalTestApi::class)
    private fun advanceClockTime(millis: Long) {
        // Don't bother advancing the clock if there's nothing to advance
        if (millis > 0) {
            testContext.testOwner.mainClock.advanceTimeBy(millis, ignoreFrameDuration = true)
        }
    }

    override fun onDispose() {
        stopAcceptingEvents()
    }

    private fun checkAndStopAcceptingEvents() {
        synchronized(batchLock) {
            check(acceptEvents) { "Events have already been (or are being) dispatched or disposed" }
            acceptEvents = false
        }
    }

    private fun stopAcceptingEvents(): Boolean {
        synchronized(batchLock) {
            return acceptEvents.also { acceptEvents = false }
        }
    }

    /**
     * Sends and recycles the given [event].
     */
    private fun sendAndRecycleEvent(event: MotionEvent) {
        try {
            sendEvent(event)
        } finally {
            event.recycle()
        }
    }
}
