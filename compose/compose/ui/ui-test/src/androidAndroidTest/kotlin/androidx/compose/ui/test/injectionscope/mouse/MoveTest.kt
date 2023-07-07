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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.runMouseInputInjectionTest
import androidx.compose.ui.test.injectionscope.mouse.Common.verifyMouseEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
@Suppress("KotlinConstantConditions") // for "0 * T"
class MoveTest {
    companion object {
        private val T = InputDispatcher.eventPeriodMillis
        private val positionIn = Offset(1f, 1f)
        private val positionMove1 = Offset(2f, 2f)
        private val positionMove2 = Offset(3f, 3f)
        private val positionOut = Offset(101f, 101f)
    }

    @Test
    fun moveTo() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveToAndCheck(positionIn, delayMillis = 0L)
            // move around the box
            moveToAndCheck(positionMove1)
            // move around the box with long delay
            moveToAndCheck(positionMove2, delayMillis = 2 * eventPeriodMillis)
            // exit the box
            moveToAndCheck(positionOut)
            // move back in the box
            moveToAndCheck(positionIn)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(0 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Move, false, positionMove1) },
            { verifyMouseEvent(3 * T, Move, false, positionMove2) },
            { verifyMouseEvent(4 * T, Exit, false, positionOut) },
            { verifyMouseEvent(5 * T, Enter, false, positionIn) },
        )
    )

    @Test
    fun moveBy() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            moveByAndCheck(positionIn, delayMillis = 0L)
            // move around the box
            moveByAndCheck(positionMove1 - positionIn)
            // move around the box with long delay
            moveByAndCheck(positionMove2 - positionMove1, delayMillis = 2 * eventPeriodMillis)
            // exit the box
            moveByAndCheck(positionOut - positionMove2)
            // move back in the box
            moveByAndCheck(positionIn - positionOut)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(0 * T, Enter, false, positionIn) },
            { verifyMouseEvent(1 * T, Move, false, positionMove1) },
            { verifyMouseEvent(3 * T, Move, false, positionMove2) },
            { verifyMouseEvent(4 * T, Exit, false, positionOut) },
            { verifyMouseEvent(5 * T, Enter, false, positionIn) },
        )
    )

    @Test
    fun updatePointerTo() = runMouseInputInjectionTest(
        mouseInput = {
            // move around
            updatePointerToAndCheck(positionIn)
            updatePointerToAndCheck(positionMove1)
            updatePointerToAndCheck(positionMove2)
            // press primary button
            press(MouseButton.Primary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(0, Enter, false, positionMove2) },
            { verifyMouseEvent(0, Press, true, positionMove2, PrimaryButton) },
        )
    )

    @Test
    fun updatePointerBy() = runMouseInputInjectionTest(
        mouseInput = {
            // move around
            updatePointerByAndCheck(positionIn)
            updatePointerByAndCheck(positionMove1 - positionIn)
            updatePointerByAndCheck(positionMove2 - positionMove1)
            // press primary button
            press(MouseButton.Primary)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(0, Enter, false, positionMove2) },
            { verifyMouseEvent(0, Press, true, positionMove2, PrimaryButton) },
        )
    )

    @Test
    fun enter_exit() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            enter(positionIn)
            // move around the box
            moveTo(positionMove1)
            // exit the box
            exit(positionOut)
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
            { verifyMouseEvent(2 * T, Move, false, positionMove1) },
            { verifyMouseEvent(3 * T, Exit, false, positionOut) },
        )
    )

    @Test
    fun enter_alreadyEntered() = runMouseInputInjectionTest(
        mouseInput = {
            // enter the box
            enter(positionIn)
            // enter again
            expectError<IllegalStateException>(
                expectedMessage = "Cannot send mouse hover enter event, mouse is already hovering"
            ) {
                enter(positionMove1)
            }
        },
        eventVerifiers = arrayOf(
            { verifyMouseEvent(1 * T, Enter, false, positionIn) },
        )
    )

    @Test
    fun exit_notEntered() = runMouseInputInjectionTest(
        mouseInput = {
            // exit the box
            expectError<IllegalStateException>(
                expectedMessage = "Cannot send mouse hover exit event, mouse is not hovering"
            ) {
                exit(positionOut)
            }
        }
    )

    private fun MouseInjectionScope.moveToAndCheck(
        position: Offset,
        delayMillis: Long = eventPeriodMillis
    ) {
        moveTo(position, delayMillis)
        assertThat(currentPosition).isEqualTo(position)
    }

    private fun MouseInjectionScope.moveByAndCheck(
        delta: Offset,
        delayMillis: Long = eventPeriodMillis
    ) {
        val expectedPosition = currentPosition + delta
        moveBy(delta, delayMillis)
        assertThat(currentPosition).isEqualTo(expectedPosition)
    }

    private fun MouseInjectionScope.updatePointerToAndCheck(position: Offset) {
        updatePointerTo(position)
        assertThat(currentPosition).isEqualTo(position)
    }

    private fun MouseInjectionScope.updatePointerByAndCheck(delta: Offset) {
        val expectedPosition = currentPosition + delta
        updatePointerBy(delta)
        assertThat(currentPosition).isEqualTo(expectedPosition)
    }
}
