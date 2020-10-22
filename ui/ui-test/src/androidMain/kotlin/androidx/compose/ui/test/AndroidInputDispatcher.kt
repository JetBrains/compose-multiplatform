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

import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.runtime.dispatch.AndroidUiDispatcher
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.test.android.AndroidOwnerRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.math.max

internal actual fun InputDispatcher(owner: Owner): InputDispatcher {
    require(owner is AndroidOwner) {
        "InputDispatcher currently only supports dispatching to AndroidOwner, not to " +
            owner::class.java.simpleName
    }
    val view = owner.view
    return AndroidInputDispatcher(owner) { view.dispatchTouchEvent(it) }
}

internal class AndroidInputDispatcher(
    owner: AndroidOwner?,
    private val sendEvent: (MotionEvent) -> Unit
) : PersistingInputDispatcher(owner) {

    companion object : AndroidOwnerRegistry.OnRegistrationChangedListener {
        init {
            AndroidOwnerRegistry.addOnRegistrationChangedListener(this)
        }

        override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
            if (!registered) {
                removeState(owner)
            }
        }
    }

    private val batchLock = Any()
    // Batched events are generated just-in-time, given the "lateness" of the dispatching (see
    // sendAllSynchronous), so enqueue generators rather than instantiated events
    private var batchedEvents = mutableListOf<(Long) -> MotionEvent>()
    private var acceptEvents = true
    private var firstEventTime = Long.MAX_VALUE

    override val now: Long get() = SystemClock.uptimeMillis()

    override fun saveState(owner: Owner?) {
        if (AndroidOwnerRegistry.getUnfilteredOwners().contains(owner)) {
            super.saveState(owner)
        }
    }

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        batchMotionEvent(
            if (lastPositions.size == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.keys.sorted().indexOf(pointerId)
        )
    }

    override fun PartialGesture.enqueueMove() {
        batchMotionEvent(ACTION_MOVE, 0)
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        batchMotionEvent(
            if (lastPositions.size == 1) ACTION_UP else ACTION_POINTER_UP,
            lastPositions.keys.sorted().indexOf(pointerId)
        )
    }

    override fun PartialGesture.enqueueCancel() {
        batchMotionEvent(ACTION_CANCEL, 0)
    }

    /**
     * Generates a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture, and adds the MotionEvent to the batch.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    private fun PartialGesture.batchMotionEvent(action: Int, actionIndex: Int) {
        val entries = lastPositions.entries.sortedBy { it.key }
        batchMotionEvent(
            downTime,
            lastEventTime,
            action,
            actionIndex,
            List(entries.size) { entries[it].value },
            List(entries.size) { entries[it].key }
        )
    }

    /**
     * Generates an event with the given parameters.
     */
    private fun batchMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        actionIndex: Int,
        coordinates: List<Offset>,
        pointerIds: List<Int>
    ) {
        synchronized(batchLock) {
            check(acceptEvents) {
                "Can't enqueue event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "actionIndex=$actionIndex, " +
                    "pointerIds=$pointerIds, " +
                    "coordinates=$coordinates" +
                    "), events have already been (or are being) dispatched or disposed"
            }
            if (firstEventTime == Long.MAX_VALUE) {
                firstEventTime = eventTime
            }
            batchedEvents.add { lateness ->
                MotionEvent.obtain(
                    /* downTime = */ lateness + downTime,
                    /* eventTime = */ lateness + eventTime,
                    /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                    /* pointerCount = */ coordinates.size,
                    /* pointerProperties = */ Array(coordinates.size) {
                        MotionEvent.PointerProperties().apply { id = pointerIds[it] }
                    },
                    /* pointerCoords = */ Array(coordinates.size) {
                        MotionEvent.PointerCoords().apply {
                            x = coordinates[it].x
                            y = coordinates[it].y
                        }
                    },
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 0f,
                    /* yPrecision = */ 0f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ 0,
                    /* flags = */ 0
                )
            }
        }
    }

    override fun sendAllSynchronous() {
        runBlocking {
            withContext(AndroidUiDispatcher.Main) {
                checkAndStopAcceptingEvents()

                // Use gestureLateness if already calculated; calculate, store and use it otherwise
                val lateness = gestureLateness ?: max(0, now - firstEventTime).also {
                    gestureLateness = it
                }

                batchedEvents.forEach {
                    sendAndRecycleEvent(it(lateness))
                }
            }
        }
        // Each invocation of performGesture (Actions.kt) uses a new instance of an input
        // dispatcher, so we don't have to reset firstEventTime after use
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
     * Sends and recycles the given [event]. If [InputDispatcher.dispatchInRealTime] is `true`,
     * suspends until [now] is equal to the event's `eventTime`. Doesn't suspend otherwise, or if
     * the event's `eventTime` is before [now].
     */
    private suspend fun sendAndRecycleEvent(event: MotionEvent) {
        try {
            if (dispatchInRealTime) {
                val delayMs = event.eventTime - now
                if (delayMs > 0) {
                    delay(delayMs)
                }
            }
            sendEvent(event)
        } finally {
            event.recycle()
        }
    }

    /**
     * A test rule that modifies [InputDispatcher]s behavior. Can be used to disable dispatching
     * of MotionEvents in real time (skips the suspend before injection of an event) or to change
     * the time between consecutive injected events.
     *
     * @param disableDispatchInRealTime If set, controls whether or not events with an eventTime
     * in the future will be dispatched as soon as possible or at that exact eventTime. If
     * `false` or not set, will suspend until the eventTime, if `true`, will send the event
     * immediately without suspending. See also [InputDispatcher.dispatchInRealTime].
     * @param eventPeriodOverride If set, specifies a different period in milliseconds between
     * two consecutive injected motion events injected by this [InputDispatcher]. If not
     * set, the event period of 10 milliseconds is unchanged.
     *
     * @see InputDispatcher.eventPeriod
     */
    internal class InputDispatcherTestRule(
        private val disableDispatchInRealTime: Boolean = false,
        private val eventPeriodOverride: Long? = null
    ) : TestRule {

        override fun apply(base: Statement, description: Description?): Statement {
            return ModifyingStatement(base)
        }

        inner class ModifyingStatement(private val base: Statement) : Statement() {
            override fun evaluate() {
                if (disableDispatchInRealTime) {
                    dispatchInRealTime = false
                }
                if (eventPeriodOverride != null) {
                    eventPeriod = eventPeriodOverride
                }
                try {
                    base.evaluate()
                } finally {
                    if (disableDispatchInRealTime) {
                        dispatchInRealTime = true
                    }
                    if (eventPeriodOverride != null) {
                        eventPeriod = 10L
                    }
                }
            }
        }
    }
}
