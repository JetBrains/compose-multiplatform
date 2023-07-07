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

import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
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
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
import android.view.ViewConfiguration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ViewRootForTest
import androidx.core.view.InputDeviceCompat.SOURCE_MOUSE
import androidx.core.view.InputDeviceCompat.SOURCE_ROTARY_ENCODER
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCHSCREEN
import androidx.core.view.MotionEventCompat.AXIS_SCROLL
import androidx.core.view.ViewConfigurationCompat.getScaledHorizontalScrollFactor
import androidx.core.view.ViewConfigurationCompat.getScaledVerticalScrollFactor

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
        when (it) {
            is KeyEvent -> view.dispatchKeyEvent(it)
            is MotionEvent -> {
                when (it.source) {
                    SOURCE_TOUCHSCREEN -> view.dispatchTouchEvent(it)
                    SOURCE_ROTARY_ENCODER -> view.dispatchGenericMotionEvent(it)
                    SOURCE_MOUSE -> when (it.action) {
                        in MouseAsTouchEvents -> view.dispatchTouchEvent(it)
                        else -> view.dispatchGenericMotionEvent(it)
                    }
                    else -> throw IllegalArgumentException(
                        "Can't dispatch MotionEvents with source ${it.source}"
                    )
                }
            }
        }
    }
}

internal class AndroidInputDispatcher(
    private val testContext: TestContext,
    private val root: ViewRootForTest,
    private val sendEvent: (InputEvent) -> Unit
) : InputDispatcher(testContext, root) {

    private val batchLock = Any()
    private var batchedEvents = mutableListOf<InputEvent>()
    private var disposed = false
    private var currentClockTime = currentTime

    // TODO(b/214439478): Find out if we should add these values to Compose's ViewConfiguration.
    // Scroll factors for Rotary Input.
    private val verticalScrollFactor: Float by lazy {
        val context = root.view.context
        val config = ViewConfiguration.get(context)
        getScaledVerticalScrollFactor(config, context)
    }
    private val horizontalScrollFactor: Float by lazy {
        val context = root.view.context
        val config = ViewConfiguration.get(context)
        getScaledHorizontalScrollFactor(config, context)
    }

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.keys.sorted().indexOf(pointerId)
        )
    }

    override fun PartialGesture.enqueueMove() {
        enqueueTouchEvent(ACTION_MOVE, 0)
    }

    override fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>
    ) {
        val entries = lastPositions.entries.sortedBy { it.key }
        val absoluteHistoricalTimes = relativeHistoricalTimes.map { currentTime + it }
        enqueueTouchEvent(
            downTime = downTime,
            action = ACTION_MOVE,
            actionIndex = 0,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = absoluteHistoricalTimes + listOf(currentTime),
            coordinates = List(entries.size) {
                historicalCoordinates[it] + listOf(entries[it].value)
            }
        )
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
        if (isWithinRootBounds(currentMousePosition)) {
            enqueueMouseEvent(ACTION_BUTTON_PRESS)
        }
    }

    override fun MouseInputState.enqueueMove() {
        if (isWithinRootBounds(currentMousePosition)) {
            enqueueMouseEvent(if (isEntered) ACTION_HOVER_MOVE else ACTION_MOVE)
        } else if (hasAnyButtonPressed) {
            enqueueMouseEvent(ACTION_MOVE)
        }
    }

    override fun MouseInputState.enqueueRelease(buttonId: Int) {
        if (isWithinRootBounds(currentMousePosition)) {
            enqueueMouseEvent(ACTION_BUTTON_RELEASE)
        }
        enqueueMouseEvent(if (hasNoButtonsPressed) ACTION_UP else ACTION_MOVE)
    }

    override fun MouseInputState.enqueueEnter() {
        if (isWithinRootBounds(currentMousePosition)) {
            enqueueMouseEvent(ACTION_HOVER_ENTER)
        }
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
            // We invert vertical scrolling to align with another platforms.
            // Vertical scrolling on desktop/web have opposite sign.
            if (scrollWheel == ScrollWheel.Vertical) -delta else delta,
            when (scrollWheel) {
                ScrollWheel.Horizontal -> MotionEvent.AXIS_HSCROLL
                ScrollWheel.Vertical -> MotionEvent.AXIS_VSCROLL
                else -> -1
            }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun KeyInputState.constructMetaState(): Int {

        fun genState(key: Key, mask: Int) = if (isKeyDown(key)) mask else 0

        return (if (capsLockOn) KeyEvent.META_CAPS_LOCK_ON else 0) or
            (if (numLockOn) KeyEvent.META_NUM_LOCK_ON else 0) or
            (if (scrollLockOn) KeyEvent.META_SCROLL_LOCK_ON else 0) or
            genState(Key.Function, KeyEvent.META_FUNCTION_ON) or
            genState(Key.CtrlLeft, KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON) or
            genState(Key.CtrlRight, KeyEvent.META_CTRL_RIGHT_ON or KeyEvent.META_CTRL_ON) or
            genState(Key.AltLeft, KeyEvent.META_ALT_LEFT_ON or KeyEvent.META_ALT_ON) or
            genState(Key.AltRight, KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_ALT_ON) or
            genState(Key.MetaLeft, KeyEvent.META_META_LEFT_ON or KeyEvent.META_META_ON) or
            genState(Key.MetaRight, KeyEvent.META_META_RIGHT_ON or KeyEvent.META_META_ON) or
            genState(Key.ShiftLeft, KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON) or
            genState(Key.ShiftRight, KeyEvent.META_SHIFT_RIGHT_ON or KeyEvent.META_SHIFT_ON)
    }

    override fun KeyInputState.enqueueDown(key: Key) =
        enqueueKeyEvent(KeyEvent.ACTION_DOWN, key.nativeKeyCode, constructMetaState())

    override fun KeyInputState.enqueueUp(key: Key) =
        enqueueKeyEvent(KeyEvent.ACTION_UP, key.nativeKeyCode, constructMetaState())

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
            action = action,
            actionIndex = actionIndex,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = listOf(currentTime),
            coordinates = List(entries.size) { listOf(entries[it].value) }
        )
    }

    /**
     * Generates an event with the given parameters.
     */
    private fun enqueueTouchEvent(
        downTime: Long,
        action: Int,
        actionIndex: Int,
        pointerIds: List<Int>,
        eventTimes: List<Long>,
        coordinates: List<List<Offset>>
    ) {
        check(coordinates.size == pointerIds.size) {
            "Coordinates size should equal pointerIds size " +
                "(was: ${coordinates.size}, ${pointerIds.size})"
        }
        repeat(pointerIds.size) { pointerIndex ->
            check(eventTimes.size == coordinates[pointerIndex].size) {
                "Historical eventTimes size should equal coordinates[$pointerIndex] size " +
                    "(was: ${eventTimes.size}, ${coordinates[pointerIndex].size})"
            }
        }

        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue touch event (" +
                    "downTime=$downTime, " +
                    "action=$action, " +
                    "actionIndex=$actionIndex, " +
                    "pointerIds=$pointerIds, " +
                    "eventTimes=$eventTimes, " +
                    "coordinates=$coordinates)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }
            val motionEvent = MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ eventTimes[0],
                /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                /* pointerCount = */ coordinates.size,
                /* pointerProperties = */ Array(coordinates.size) { pointerIndex ->
                    PointerProperties().apply {
                        id = pointerIds[pointerIndex]
                        toolType = MotionEvent.TOOL_TYPE_FINGER
                    }
                },
                /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                    PointerCoords().apply {
                        x = positionInScreen.x + coordinates[pointerIndex][0].x
                        y = positionInScreen.y + coordinates[pointerIndex][0].y
                    }
                },
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 1f,
                /* yPrecision = */ 1f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ SOURCE_TOUCHSCREEN,
                /* flags = */ 0
            ).apply {
                // The current time & coordinates are the last element in the lists, and need to
                // be passed into the final addBatch call. If there are no historical events,
                // the list sizes are 1 and we don't need to call addBatch at all.
                for (timeIndex in 1 until eventTimes.size) {
                    addBatch(
                        /* eventTime = */ eventTimes[timeIndex],
                        /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                            PointerCoords().apply {
                                x = positionInScreen.x + coordinates[pointerIndex][timeIndex].x
                                y = positionInScreen.y + coordinates[pointerIndex][timeIndex].y
                            }
                        },
                        /* metaState = */ 0
                    )
                }
                offsetLocation(-positionInScreen.x, -positionInScreen.y)
            }

            batchedEvents.add(motionEvent)
        }
    }

    private fun MouseInputState.enqueueMouseEvent(action: Int, delta: Float = 0f, axis: Int = -1) {
        enqueueMouseEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            coordinate = lastPosition,
            metaState = keyInputState.constructMetaState(),
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
        metaState: Int,
        buttonState: Int,
        axis: Int = -1,
        axisDelta: Float = 0f
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue mouse event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "metaState=$metaState, " +
                    "buttonState=$buttonState, " +
                    "axis=$axis, " +
                    "axisDelta=$axisDelta)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }
            batchedEvents.add(
                MotionEvent.obtain(
                    /* downTime = */ downTime,
                    /* eventTime = */ eventTime,
                    /* action = */ action,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_MOUSE
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = positionInScreen.x + coordinate.x
                            y = positionInScreen.y + coordinate.y
                            if (axis != -1) {
                                setAxisValue(axis, axisDelta)
                            }
                        }
                    ),
                    /* metaState = */ metaState,
                    /* buttonState = */ buttonState,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ SOURCE_MOUSE,
                    /* flags = */ 0
                ).apply {
                    offsetLocation(-positionInScreen.x, -positionInScreen.y)
                }
            )
        }
    }

    override fun RotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        enqueueRotaryScrollEvent(
            eventTime = currentTime,
            scrollPixels = -horizontalScrollPixels / horizontalScrollFactor
        )
    }

    override fun RotaryInputState.enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        enqueueRotaryScrollEvent(
            eventTime = currentTime,
            scrollPixels = -verticalScrollPixels / verticalScrollFactor
        )
    }

    private fun enqueueRotaryScrollEvent(
        eventTime: Long,
        scrollPixels: Float
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue rotary scroll event (" +
                    "eventTime=$eventTime, " +
                    "scrollDelta=$scrollPixels)"
            }
            batchedEvents.add(
                MotionEvent.obtain(
                    /* downTime = */ 0,
                    /* eventTime = */ eventTime,
                    /* action = */ ACTION_SCROLL,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = TOOL_TYPE_UNKNOWN
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            setAxisValue(AXIS_SCROLL, scrollPixels)
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ SOURCE_ROTARY_ENCODER,
                    /* flags = */ 0
                )
            )
        }
    }

    /**
     * Generates a KeyEvent with the given [action] and [keyCode] and adds the KeyEvent to
     * the batch.
     *
     * @see KeyEvent.getAction
     * @see KeyEvent.getKeyCode
     */
    private fun KeyInputState.enqueueKeyEvent(
        action: Int,
        keyCode: Int,
        metaState: Int
    ) {
        enqueueKeyEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            code = keyCode,
            repeat = repeatCount,
            metaState = metaState
        )
    }

    /**
     * Generates a key event with the given parameters.
     */
    private fun enqueueKeyEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        code: Int,
        repeat: Int,
        metaState: Int
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue key event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "code=$code, " +
                    "repeat=$repeat, " +
                    "metaState=$metaState)"
            }

            val keyEvent = KeyEvent(
                /* downTime = */ downTime,
                /* eventTime = */ eventTime,
                /* action = */ action,
                /* code = */ code,
                /* repeat = */ repeat,
                /* metaState = */ metaState,
                /* deviceId = */ KeyCharacterMap.VIRTUAL_KEYBOARD,
                /* scancode = */ 0
            )

             batchedEvents.add(keyEvent)
        }
    }

    override fun flush() {
        // Must inject on the main thread, because it might modify View properties
        @OptIn(InternalTestApi::class)
        testContext.testOwner.runOnUiThread {
            val events = synchronized(batchLock) {
                ensureNotDisposed { "Can't flush events" }
                mutableListOf<InputEvent>().apply {
                    addAll(batchedEvents)
                    batchedEvents.clear()
                }
            }

            events.forEach { event ->
                // Before injecting the next event, pump the clock
                // by the difference between this and the last event
                advanceClockTime(event.eventTime - currentClockTime)
                currentClockTime = event.eventTime
                sendAndRecycleEvent(event)
            }
        }
    }

    @OptIn(InternalTestApi::class)
    private fun advanceClockTime(millis: Long) {
        // Don't bother advancing the clock if there's nothing to advance
        if (millis > 0) {
            testContext.testOwner.mainClock.advanceTimeBy(millis, ignoreFrameDuration = true)
        }
    }

    private fun ensureNotDisposed(lazyMessage: () -> String) {
        check(!disposed) {
            "${lazyMessage()}, AndroidInputDispatcher has already been disposed"
        }
    }

    override fun onDispose() {
        synchronized(batchLock) {
            if (!disposed) {
                disposed = true
                batchedEvents.forEach {
                    recycleEventIfPossible(it)
                }
            }
        }
    }

    /**
     * Sends and recycles the given [event].
     */
    private fun sendAndRecycleEvent(event: InputEvent) {
        try {
            sendEvent(event)
        } finally {
            recycleEventIfPossible(event)
        }
    }

    /**
     * Recycles the [event] if it is a [MotionEvent]. There is no notion of recycling a [KeyEvent].
     */
    private fun recycleEventIfPossible(event: InputEvent) {
        (event as? MotionEvent)?.recycle()
    }
}
