/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.mouse

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.DataPoint
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.test.util.verifyEvents

@OptIn(ExperimentalTestApi::class)
object Common {
    val PrimaryButton = PointerButtons(MouseButton.Primary.buttonId)
    val PrimarySecondaryButton = PointerButtons(
        MouseButton.Primary.buttonId or MouseButton.Secondary.buttonId
    )

    fun runMouseInputInjectionTest(
        mouseInput: MouseInjectionScope.() -> Unit,
        vararg eventVerifiers: DataPoint.() -> Unit
    ): Unit = runComposeUiTest {
        mainClock.autoAdvance = false
        val recorder = SinglePointerInputRecorder()
        setContent {
            ClickableTestBox(recorder)
        }
        onNodeWithTag(ClickableTestBox.defaultTag).performMouseInput(mouseInput)
        runOnIdle { recorder.verifyEvents(*eventVerifiers) }
    }

    /**
     * Verifies [DataPoint]s for events that are expected to come from a mouse
     */
    fun DataPoint.verifyMouseEvent(
        expectedTimestamp: Long,
        expectedEventType: PointerEventType,
        expectedDown: Boolean,
        expectedPosition: Offset,
        expectedButtons: PointerButtons = PointerButtons(0)
    ) {
        verify(
            expectedTimestamp = expectedTimestamp,
            expectedId = null,
            expectedDown = expectedDown,
            expectedPosition = expectedPosition,
            expectedPointerType = PointerType.Mouse,
            expectedEventType = expectedEventType,
            expectedButtons = expectedButtons
        )
    }

    /**
     * Overload of [verifyMouseEvent] that takes a scroll delta too
     */
    fun DataPoint.verifyMouseEvent(
        expectedTimestamp: Long,
        expectedEventType: PointerEventType,
        expectedDown: Boolean,
        expectedPosition: Offset,
        expectedScrollDelta: Offset,
        expectedButtons: PointerButtons = PointerButtons(0)
    ) {
        verify(
            expectedTimestamp = expectedTimestamp,
            expectedId = null,
            expectedDown = expectedDown,
            expectedPosition = expectedPosition,
            expectedPointerType = PointerType.Mouse,
            expectedEventType = expectedEventType,
            expectedButtons = expectedButtons,
            expectedScrollDelta = expectedScrollDelta
        )
    }
}
