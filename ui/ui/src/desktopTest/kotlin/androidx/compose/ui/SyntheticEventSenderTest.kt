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

import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerInputEvent
import kotlin.test.Test

class SyntheticEventSenderTest {
    @Test
    fun `mouse, shouldn't generate new events if order is correct`() {
        eventsSentBy(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Press, 10f, 20f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Release, 10f, 30f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Press, 10f, 40f, pressed = true),
            mouseEvent(Release, 10f, 40f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        ) positionAndDownShouldEqual listOf(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Press, 10f, 20f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Release, 10f, 30f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Press, 10f, 40f, pressed = true),
            mouseEvent(Release, 10f, 40f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        )
    }

    @Test
    fun `mouse, should generate new move before non-move if position isn't the same`() {
        eventsSentBy(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Press, 10f, 25f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Release, 10f, 35f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Press, 10f, 45f, pressed = true),
            mouseEvent(Release, 10f, 50f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        ) positionAndDownShouldEqual listOf(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Move, 10f, 25f, pressed = false),
            mouseEvent(Press, 10f, 25f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Move, 10f, 35f, pressed = true),
            mouseEvent(Release, 10f, 35f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Move, 10f, 45f, pressed = false),
            mouseEvent(Press, 10f, 45f, pressed = true),
            mouseEvent(Move, 10f, 50f, pressed = true),
            mouseEvent(Release, 10f, 50f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        )
    }

    private fun eventsSentBy(
        vararg inputEvents: PointerInputEvent
    ): List<PointerInputEvent> {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender(received::add)
        for (inputEvent in inputEvents) {
            sender.send(inputEvent)
        }
        return received
    }
}