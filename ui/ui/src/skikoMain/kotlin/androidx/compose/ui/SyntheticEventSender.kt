/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData

/**
 * Compose or user code can't work well if we miss some events.
 *
 * For example:
 * - if we miss Move before Press event with a different position
 * - if we send one event with 2 pressed touches without sending 1 pressed touch first
 *
 * Platforms can send events this way.
 *
 * This class generates new synthetic events based on the previous event, if something is missing
 */
internal class SyntheticEventSender(
    send: (PointerInputEvent) -> Unit
) {
    private val _send: (PointerInputEvent) -> Unit = send
    private var previousEvent: PointerInputEvent? = null

    fun reset() {
        previousEvent = null
    }

    /**
     * Send [event] and synthetic events before it if needed. On each sent event we just call [send]
     */
    fun send(event: PointerInputEvent) {
        sendMissingMove(event)
        sendMissingReleases(event)
        sendMissingPresses(event)
        sendInternal(event)
    }

    fun sendSyntheticMove() {
        val previousEvent = previousEvent ?: return
        sendSyntheticMove(previousEvent)
    }

    /**
     * @param pointersSourceEvent the event which we treat as a source of the pointers
     */
    private fun sendSyntheticMove(pointersSourceEvent: PointerInputEvent) {
        val previousEvent = previousEvent ?: return
        val idToPosition = pointersSourceEvent.pointers.associate { it.id to it.position }
        sendInternal(
            previousEvent.copySynthetic(
                type = PointerEventType.Move,
                copyPointer = { it.copySynthetic(position = idToPosition[it.id] ?: it.position) },
            )
        )
    }

    private fun sendMissingMove(currentEvent: PointerInputEvent) {
        if (isMoveEventMissing(previousEvent, currentEvent)) {
            sendSyntheticMove(currentEvent)
        }
    }

    private fun sendMissingReleases(currentEvent: PointerInputEvent) {
        val previousEvent = previousEvent ?: return
        val previousPressed = previousEvent.pressedIds()
        val currentPressed = currentEvent.pressedIds()
        val newReleased = (previousPressed - currentPressed.toSet()).toList()
        val sendingAsUp = HashSet<PointerId>(newReleased.size)

        // Don't send the first released pointer
        // It will be sent as a real event. Here we only need to send synthetic events
        // before a real one.
        for (i in newReleased.size - 2 downTo 0) {
            sendingAsUp.add(newReleased[i])

            sendInternal(
                previousEvent.copySynthetic(
                    type = PointerEventType.Release,
                    copyPointer = {
                        it.copySynthetic(
                            down = if (it.id in sendingAsUp) {
                                !sendingAsUp.contains(it.id)
                            } else {
                                it.down
                            }
                        )
                    }
                )
            )
        }
    }

    private fun sendMissingPresses(currentEvent: PointerInputEvent) {
        val previousPressed = previousEvent?.pressedIds().orEmpty()
        val currentPressed = currentEvent.pressedIds()
        val newPressed = (currentPressed - previousPressed.toSet()).toList()
        val sendingAsDown = HashSet<PointerId>(newPressed.size)

        // Don't send the last pressed pointer (newPressed.size - 1)
        // It will be sent as a real event. Here we only need to send synthetic events
        // before a real one.
        for (i in 0..newPressed.size - 2) {
            sendingAsDown.add(newPressed[i])

            sendInternal(
                currentEvent.copySynthetic(
                    type = PointerEventType.Press,
                    copyPointer = {
                        it.copySynthetic(
                            down = if (it.id in newPressed) {
                                sendingAsDown.contains(it.id)
                            } else {
                                it.down
                            }
                        )
                    }
                )
            )
        }
    }

    private fun PointerInputEvent.pressedIds(): Sequence<PointerId> =
        pointers.asSequence().filter { it.down }.map { it.id }

    private fun sendInternal(event: PointerInputEvent) {
        _send(event)
        // We don't send nativeEvent for synthetic events.
        // Nullify to avoid memory leaks (native events can point to native views).
        previousEvent = event.copy(nativeEvent = null)
    }

    private fun isMoveEventMissing(
        previousEvent: PointerInputEvent?,
        currentEvent: PointerInputEvent,
    ) = !currentEvent.isMove() && !currentEvent.isSamePosition(previousEvent)

    private fun PointerInputEvent.isMove() =
        eventType == PointerEventType.Move ||
            eventType == PointerEventType.Enter ||
            eventType == PointerEventType.Exit

    private fun PointerInputEvent.isSamePosition(previousEvent: PointerInputEvent?): Boolean {
        val previousIdToPosition = previousEvent?.pointers?.associate { it.id to it.position }
        return pointers.all {
            val previousPosition = previousIdToPosition?.get(it.id)
            previousPosition == null || it.position == previousPosition
        }
    }

    // we don't use copy here to not forget to nullify properties that shouldn't be in
    // a synthetic event
    private fun PointerInputEvent.copySynthetic(
        type: PointerEventType,
        copyPointer: (PointerInputEventData) -> PointerInputEventData,
    ) = PointerInputEvent(
        eventType = type,
        pointers = pointers.map(copyPointer),
        uptime = uptime,
        nativeEvent = null,
        buttons = buttons,
        keyboardModifiers = keyboardModifiers,
        button = null
    )

    private fun PointerInputEventData.copySynthetic(
        position: Offset = this.position,
        down: Boolean = this.down
    ) = PointerInputEventData(
        id,
        uptime,
        position,
        position,
        down,
        pressure,
        type,
        issuesEnterExit,
        scrollDelta = Offset(0f, 0f),
    )
}