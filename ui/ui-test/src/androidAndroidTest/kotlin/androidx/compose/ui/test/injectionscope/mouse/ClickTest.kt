/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimarySecondaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.runMouseInputInjectionTest
import androidx.compose.ui.test.injectionscope.mouse.Common.verifyMouseEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ClickTest {
    companion object {
        private val T = InputDispatcher.eventPeriodMillis
        private val positionIn = Offset(1f, 1f)
        private val positionMove1 = Offset(2f, 2f)
        private val positionMove2 = Offset(3f, 3f)
        private val positionOut = Offset(101f, 101f)
    }

    @Test
    fun click_pressIn_releaseIn() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveTo(positionIn)
            // press primary button
            press(MouseButton.Primary)
            // move around the box
            moveTo(positionMove1)
            // release primary button
            release(MouseButton.Primary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
            { verifyMouseEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
            { verifyMouseEvent(2 * T, Release, false, positionMove1) },
        )
    )

    @Test
    fun click_pressIn_moveOutIn_releaseIn() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveTo(positionIn)
            // press primary button
            press(MouseButton.Primary)
            // move out of the box
            moveTo(positionOut)
            // move back into the box
            moveTo(positionMove1)
            // release primary button in the box
            release(MouseButton.Primary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
            { verifyMouseEvent(2 * T, Exit, true, positionOut, PrimaryButton) },
            { verifyMouseEvent(3 * T, Enter, true, positionMove1, PrimaryButton) },
            { verifyMouseEvent(3 * T, Release, false, positionMove1) },
        )
    )

    @Test
    fun click_pressIn_releaseOut() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveTo(positionIn)
            // press primary button
            press(MouseButton.Primary)
            // move out of the box
            moveTo(positionOut)
            // release primary button
            release(MouseButton.Primary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
            { verifyMouseEvent(2 * T, Exit, true, positionOut, PrimaryButton) },
            { verifyMouseEvent(2 * T, Release, false, positionOut) },
        )
    )

    @Test
    fun click_twoButtons_symmetric() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveTo(positionIn)
            // press primary button
            press(MouseButton.Primary)
            // move around the box
            moveTo(positionMove1)
            // press secondary button
            press(MouseButton.Secondary)
            // move around a bit more
            moveTo(positionMove2)
            // release secondary button
            release(MouseButton.Secondary)
            // release primary button
            release(MouseButton.Primary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
            { verifyMouseEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
            // TODO(b/234439423): Expect more events when b/234439423 is fixed
//            { verifyMouseEvent(2 * T, Press, true, positionMove1, PrimarySecondaryButton) },
            { verifyMouseEvent(3 * T, Move, true, positionMove2, PrimarySecondaryButton) },
//            { verifyMouseEvent(3 * T, Release, true, positionMove2, PrimaryButton) },
            { verifyMouseEvent(3 * T, Release, false, positionMove2) },
        )
    )

    @Test
    fun click_twoButtons_staggered() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveTo(positionIn)
            // press primary button
            press(MouseButton.Primary)
            // move around the box
            moveTo(positionMove1)
            // press secondary button
            press(MouseButton.Secondary)
            // move around a bit more
            moveTo(positionMove2)
            // release primary button
            release(MouseButton.Primary)
            // release secondary button
            release(MouseButton.Secondary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
            { verifyMouseEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
            // TODO(b/234439423): Expect more events when b/234439423 is fixed
//            { verifyMouseEvent(2 * T, Press, true, positionMove1, PrimarySecondaryButton) },
            { verifyMouseEvent(3 * T, Move, true, positionMove2, PrimarySecondaryButton) },
//            { verifyMouseEvent(3 * T, Release, true, positionMove2, SecondaryButton) },
            { verifyMouseEvent(3 * T, Release, false, positionMove2) },
        )
    )

    @Test
    fun press_alreadyPressed() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveTo(positionIn)
            // press primary button
            press(MouseButton.Primary)
            // press primary button again
            expectError<IllegalStateException>(
                expectedMessage = "Cannot send mouse button down event, " +
                    "button ${MouseButton.Primary.buttonId} is already pressed"
            ) {
                press(MouseButton.Primary)
            }
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
        )
    )
}
