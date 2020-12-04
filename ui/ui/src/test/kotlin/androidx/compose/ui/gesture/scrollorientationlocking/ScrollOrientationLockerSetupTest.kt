/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.gesture.scrollorientationlocking

import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.input.pointer.up
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScrollOrientationLockerSetupTest {

    // Scenarios where dispatch does not happen.

    // Valid CustomEvent, Down event, up event, all passes.
    @Test
    fun onPointerInput_validCustomEventThenDown_doesNotDispatchEvent() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)
        scrollOrientationLocker.onCustomEventAllPasses(
            ShareScrollOrientationLockerEvent(
                InternalScrollOrientationLocker()
            )
        )

        val down = down(0, 0.milliseconds)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down))

        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    // CustomEvent, Down then move, all passes.
    @Test
    fun onPointerInput_downThenMove_doesNotDispatchEvent() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val down = down(0, x = 0f, y = 0f)
        val moveA = down.moveTo(1.milliseconds, 1f, 0f)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down))

        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onPointerInputAllPasses(listOf(moveA))

        // Assert
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    // CustomEvent, Down then up, all passes.
    @Test
    fun onPointerInput_downThenUp_doesNotDispatchEvent() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val down = down(0, x = 0f, y = 0f)
        val up = down.up(1.milliseconds)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down))

        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onPointerInputAllPasses(listOf(up))

        // Assert
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    // CustomEvent, Down then up, all passes.
    @Test
    fun onPointerInput_downThenCancel_doesNotDispatchEvent() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val down = down(0, x = 0f, y = 0f)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down))

        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onCancel()

        // Assert
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    // Scenarios where dispatch does happen.

    // Down event, all passes.
    @Test
    fun onPointerInput_downAllPasses_dispatchesNewInternalScrollOrientationLockerOnce() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down(0)))

        verify(customEventDispatcher).dispatchCustomEvent(any())
        verifyNoMoreInteractions(customEventDispatcher)
    }

    // Down event, InitialTunnel only.
    @Test
    fun onPointerInput_downInitialTunnel_dispatchesNewInternalScrollOrientationLockerOnce() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        scrollOrientationLocker.onPointerInput(listOf(down(0)), PointerEventPass.Initial)

        verify(customEventDispatcher).dispatchCustomEvent(any())
        verifyNoMoreInteractions(customEventDispatcher)
    }

    // Valid CustomEvent, Down event, up event, all passes.
    @Test
    fun onPointerInput_invalidCustomEventThenDown_dispatchesEvent() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)
        scrollOrientationLocker.onCustomEventAllPasses(
            object : CustomEvent {}
        )

        val down = down(0, 0.milliseconds)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down))

        verify(customEventDispatcher).dispatchCustomEvent(any())
    }

    // Down event, followed by down event, all passes.
    @Test
    fun onPointerInput_downDown_dispatchesExistingScrollOrientationLockerOnce() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0, x = 0f, y = 0f)

        val moveA = downA.moveTo(1.milliseconds, 0f, 0f)
        val downB = down(1)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        var expected: InternalScrollOrientationLocker
        argumentCaptor<ShareScrollOrientationLockerEvent>().apply {
            verify(customEventDispatcher).dispatchCustomEvent(capture())
            expected = this.firstValue.scrollOrientationLocker
        }

        reset(customEventDispatcher)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(moveA, downB))
        var actual: InternalScrollOrientationLocker
        argumentCaptor<ShareScrollOrientationLockerEvent>().apply {
            verify(customEventDispatcher).dispatchCustomEvent(capture())
            actual = this.firstValue.scrollOrientationLocker
        }

        assertThat(actual).isEqualTo(expected)
    }

    // Down event, followed by down event, InitialTunnel only.
    @Test
    fun onPointerInput_downDownInitialTunnel_dispatchesExistingScrollOrientationLockerOnce() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0, x = 0f, y = 0f)

        val moveA = downA.moveTo(1.milliseconds, 0f, 0f)
        val downB = down(1)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        var expected: InternalScrollOrientationLocker
        argumentCaptor<ShareScrollOrientationLockerEvent>().apply {
            verify(customEventDispatcher).dispatchCustomEvent(capture())
            expected = this.firstValue.scrollOrientationLocker
        }

        reset(customEventDispatcher)

        scrollOrientationLocker.onPointerInput(
            listOf(moveA, downB),
            PointerEventPass.Initial
        )
        var actual: InternalScrollOrientationLocker
        argumentCaptor<ShareScrollOrientationLockerEvent>().apply {
            verify(customEventDispatcher).dispatchCustomEvent(capture())
            actual = this.firstValue.scrollOrientationLocker
        }

        assertThat(actual).isEqualTo(expected)
    }

    // Down then up then down, all passes.
    @Test
    fun onPointerInput_downThenUpThenDown_dispatchesEventDuringSecondDown() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0)
        val upA = downA.up(1.milliseconds)
        val downB = down(1)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        scrollOrientationLocker.onPointerInputAllPasses(listOf(upA))
        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onPointerInputAllPasses(listOf(downB))

        // Assert
        verify(customEventDispatcher).dispatchCustomEvent(any())
    }

    // Down then up then down, all passes: first internalLocker != last internalLocker
    @Test
    fun onPointerInput_downThenUpThenDown_firstInternalLockerNotEqualsFinalInternalLocker() {

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0, x = 0f, y = 0f)
        val upA = downA.up(1.milliseconds)
        val downB = down(1)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        val expected =
            argumentCaptor<ShareScrollOrientationLockerEvent>().run {
                verify(customEventDispatcher).dispatchCustomEvent(capture())
                this.firstValue.scrollOrientationLocker
            }

        scrollOrientationLocker.onPointerInputAllPasses(listOf(upA))

        reset(customEventDispatcher)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downB))
        val actual =
            argumentCaptor<ShareScrollOrientationLockerEvent>().run {
                verify(customEventDispatcher).dispatchCustomEvent(capture())
                this.firstValue.scrollOrientationLocker
            }

        assertThat(actual).isNotEqualTo(expected)
    }

    // Down then cancel then down, all passes.
    @Test
    fun onPointerInput_downThenCancelThenDown_dispatchesEventDuringSecondDown() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0)
        val downB = down(1)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        scrollOrientationLocker.onCancel()
        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onPointerInputAllPasses(listOf(downB))

        // Assert
        verify(customEventDispatcher).dispatchCustomEvent(any())
    }

    // Down then up then down, all passes: first internalLocker != last internalLocker
    @Test
    fun onPointerInput_downCancelThenDown_firstInternalLockerNotEqualsFinalInternalLocker() {

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0, x = 0f, y = 0f)
        val downB = down(1)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        val expected =
            argumentCaptor<ShareScrollOrientationLockerEvent>().run {
                verify(customEventDispatcher).dispatchCustomEvent(capture())
                this.firstValue.scrollOrientationLocker
            }

        scrollOrientationLocker.onCancel()
        reset(customEventDispatcher)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downB))
        val actual =
            argumentCaptor<ShareScrollOrientationLockerEvent>().apply {
                verify(customEventDispatcher).dispatchCustomEvent(capture())
                this.firstValue.scrollOrientationLocker
            }

        assertThat(actual).isNotEqualTo(expected)
    }

    // Valid CustomEvent, Down then up then down, all passes.
    @Test
    fun onPointerInput_validCustomEventDownUpDown_dispatchesEvent() {
        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)
        scrollOrientationLocker.onCustomEventAllPasses(
            ShareScrollOrientationLockerEvent(
                InternalScrollOrientationLocker()
            )
        )

        val downA = down(0, 0.milliseconds)
        val upA = downA.up(1.milliseconds)
        val downB = down(1, 2.milliseconds)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        scrollOrientationLocker.onPointerInputAllPasses(listOf(upA))
        scrollOrientationLocker.onPointerInputAllPasses(listOf(downB))

        verify(customEventDispatcher).dispatchCustomEvent(any())
    }

    // CustomEvent, Down then move then down, all passes.
    @Test
    fun onPointerInput_downThenMoveThenDown_doesDispatchesEvent() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0, 0.milliseconds, x = 0f, y = 0f)

        val moveA1 = downA.moveTo(1.milliseconds, 1f, 0f)

        val moveA2 = moveA1.moveTo(2.milliseconds, 1f, 0f)
        val downB = down(0, 2.milliseconds, x = 0f, y = 0f)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        scrollOrientationLocker.onPointerInputAllPasses(listOf(moveA1))

        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onPointerInputAllPasses(listOf(moveA2, downB))

        // Assert
        verify(customEventDispatcher).dispatchCustomEvent(any())
    }

    // CustomEvent, Down then move then down, all passes.
    @Test
    fun onPointerInput_downThenMoveThenDown_firstInternalLockerMatchesSecondInternalLocker() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        val downA = down(0, 0.milliseconds, x = 0f, y = 0f)

        val moveA1 = downA.moveTo(1.milliseconds, 1f, 0f)

        val moveA2 = moveA1.moveTo(2.milliseconds, 1f, 0f)
        val downB = down(0, 2.milliseconds, x = 0f, y = 0f)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(downA))
        val expected =
            argumentCaptor<ShareScrollOrientationLockerEvent>().run {
                verify(customEventDispatcher).dispatchCustomEvent(capture())
                this.firstValue.scrollOrientationLocker
            }
        scrollOrientationLocker.onPointerInputAllPasses(listOf(moveA1))

        reset(customEventDispatcher)

        // Act
        scrollOrientationLocker.onPointerInputAllPasses(listOf(moveA2, downB))
        val actual =
            argumentCaptor<ShareScrollOrientationLockerEvent>().run {
                verify(customEventDispatcher).dispatchCustomEvent(capture())
                this.firstValue.scrollOrientationLocker
            }

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    // Verification of situations when exceptions should be thrown.

    @Test(expected = IllegalStateException::class)
    fun attemptToLockPointers_notInitialized_illegalStateExceptionThrown() {
        val customEventDispatcher = mock<CustomEventDispatcher>()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        scrollOrientationLocker.attemptToLockPointers(listOf(down(0)), Orientation.Vertical)
    }

    @Test(expected = IllegalStateException::class)
    fun getPointersFor_notInitialized_illegalStateExceptionThrown() {
        val customEventDispatcher = mock<CustomEventDispatcher>()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        scrollOrientationLocker.getPointersFor(listOf(down(0)), Orientation.Vertical)
    }

    @Test(expected = IllegalStateException::class)
    fun onCustomEvent_isLockerOWner_illegalStateExceptionThrown() {
        val customEventDispatcher = mock<CustomEventDispatcher>()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)

        scrollOrientationLocker.onPointerInputAllPasses(listOf(down(0)))

        scrollOrientationLocker.onCustomEventAllPasses(
            ShareScrollOrientationLockerEvent(
                InternalScrollOrientationLocker()
            )
        )
    }
}

private fun ScrollOrientationLocker.onPointerInputAllPasses(
    changes: List<PointerInputChange>
) {
    PointerEventPass.values().forEach {
        onPointerInputSetup(changes, it)
        onPointerInputTearDown(changes, it)
    }
}

private fun ScrollOrientationLocker.onPointerInput(
    changes: List<PointerInputChange>,
    pass: PointerEventPass
) {
    onPointerInputSetup(changes, pass)
    onPointerInputTearDown(changes, pass)
}

private fun ScrollOrientationLocker.onCustomEventAllPasses(
    event: CustomEvent
) {
    PointerEventPass.values().forEach {
        onCustomEvent(event, it)
    }
}