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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.customevents.DelayUpEvent
import androidx.compose.ui.gesture.customevents.DelayUpMessage
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.inMilliseconds
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO(b/138754591): The behavior of this gesture detector needs to be finalized.
// TODO(b/139020678): Probably has shared functionality with other press based detectors.
/**
 * Responds to pointers going down and up (tap) and then down and up again (another tap)
 * with minimal gap of time between the first up and the second down.
 *
 * Note: This is a temporary implementation to unblock dependents.  Once the underlying API that
 * allows double tap to temporarily block tap from firing is complete, this gesture detector will
 * not block tap when the first "up" occurs. It will however block the 2nd up from causing tap to
 * fire.
 *
 * Also, given that this gesture detector is so temporary, opting to not write substantial tests.
 */
fun Modifier.doubleTapGestureFilter(
    onDoubleTap: (Offset) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "doubleTapGestureFilter"
        value = onDoubleTap
    }
) {
    val scope = rememberCoroutineScope()
    val filter = remember { DoubleTapGestureFilter(scope) }
    filter.onDoubleTap = onDoubleTap
    PointerInputModifierImpl(filter)
}

internal class DoubleTapGestureFilter(
    val coroutineScope: CoroutineScope
) : PointerInputFilter() {

    lateinit var onDoubleTap: (Offset) -> Unit

    private enum class State {
        Idle, Down, Up, SecondDown
    }

    @VisibleForTesting
    internal var doubleTapTimeout = DoubleTapTimeout

    private var state = State.Idle
    private var job: Job? = null
    private lateinit var delayUpDispatcher: DelayUpDispatcher

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        delayUpDispatcher = DelayUpDispatcher(customEventDispatcher)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {

        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main) {
            if (state == State.Idle && changes.all { it.changedToDown() }) {
                state = State.Down
                return
            }

            if (state == State.Down && changes.all { it.changedToUp() }) {
                state = State.Up
                delayUpDispatcher.delayUp(changes)

                job = coroutineScope.launch {
                    delay(doubleTapTimeout.inMilliseconds())
                    state = State.Idle
                    delayUpDispatcher.allowUp()
                }

                return
            }

            if (state == State.Up && changes.all { it.changedToDown() }) {
                state = State.SecondDown
                job?.cancel()
                delayUpDispatcher.disallowUp()
                return
            }

            if (state == State.SecondDown && changes.all { it.changedToUp() }) {
                state = State.Idle
                onDoubleTap.invoke(changes[0].previous.position)
                changes.fastForEach {
                    it.consumeDownChange()
                }
            }
        }

        if (pass == PointerEventPass.Final) {

            val noPointersAreInBoundsAndNotUpState =
                (state != State.Up && !changes.anyPointersInBounds(bounds))

            val anyPositionChangeConsumed = changes.fastAny { it.anyPositionChangeConsumed() }

            if (noPointersAreInBoundsAndNotUpState || anyPositionChangeConsumed) {
                // A pointers movement was consumed or all of our pointers are out of bounds, so
                // reset to idle.
                fullReset()
            }
        }
    }

    override fun onCancel() {
        fullReset()
    }

    private fun fullReset() {
        delayUpDispatcher.disallowUp()
        job?.cancel()
        state = State.Idle
    }

    private class DelayUpDispatcher(val customEventDispatcher: CustomEventDispatcher) {

        // Non-writeable because we send this to customEventDispatcher and we don't want to ever
        // accidentally mutate what we have sent.
        private var blockedUpEvents: Set<PointerId>? = null

        fun delayUp(changes: List<PointerInputChange>) {
            blockedUpEvents =
                changes
                    .mapTo(mutableSetOf()) { it.id }
                    .also {
                        customEventDispatcher.retainHitPaths(it)
                        customEventDispatcher.dispatchCustomEvent(
                            DelayUpEvent(DelayUpMessage.DelayUp, it)
                        )
                    }
        }

        fun disallowUp() {
            unBlockUpEvents(true)
        }

        fun allowUp() {
            unBlockUpEvents(false)
        }

        private fun unBlockUpEvents(upIsConsumed: Boolean) {
            blockedUpEvents?.let {
                val message =
                    if (upIsConsumed) {
                        DelayUpMessage.DelayedUpConsumed
                    } else {
                        DelayUpMessage.DelayedUpNotConsumed
                    }
                customEventDispatcher.dispatchCustomEvent(
                    DelayUpEvent(message, it)
                )
                customEventDispatcher.releaseHitPaths(it)
            }
            blockedUpEvents = null
        }
    }
}