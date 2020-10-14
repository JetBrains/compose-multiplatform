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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.customevents.LongPressFiredEvent
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.invokeOverAllPasses
import androidx.compose.ui.input.pointer.moveBy
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.input.pointer.up
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@kotlinx.coroutines.ObsoleteCoroutinesApi
@RunWith(JUnit4::class)
class LongPressGestureFilterTest {

    private val LongPressTimeoutMillis = 100.milliseconds
    @Suppress("DEPRECATION")
    private val testContext = kotlinx.coroutines.test.TestCoroutineContext()
    private val onLongPress: (Offset) -> Unit = mock()
    private val customEventDispatcher: CustomEventDispatcher = mock()
    private lateinit var filter: LongPressGestureFilter

    @Before
    fun setup() {
        filter = LongPressGestureFilter(CoroutineScope(testContext))
        filter.onLongPress = onLongPress
        filter.longPressTimeout = LongPressTimeoutMillis
        filter.onInit(customEventDispatcher)
    }

    // Tests that verify conditions under which onLongPress will not be called.

    @Test
    fun onPointerEvent_down_eventNotFired() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_downWithinTimeout_eventNotFired() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_DownMoveConsumed_eventNotFired() {
        val down = down(0)
        val move =
            down.moveBy(50.milliseconds, 1f, 1f).apply { consumePositionChange(1f, 0f) }

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_2Down1MoveConsumed_eventNotFired() {
        val down0 = down(0)
        val down1 = down(1)
        val move0 =
            down0.moveBy(50.milliseconds, 1f, 1f).apply { consumePositionChange(1f, 0f) }
        val move1 = down0.moveBy(50.milliseconds, 0f, 0f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down0, down1))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move0, move1))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_DownUpConsumed_eventNotFired() {
        val down = down(0)
        val up = down.up(50.milliseconds).apply { consumeDownChange() }

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_DownUpNotConsumed_eventNotFired() {
        val down = down(0)
        val up = down.up(50.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_2DownIndependentlyUnderTimeoutAndDoNotOverlap_eventNotFired() {

        // Arrange

        val down0 = down(0)

        val up0 = down0.up(50.milliseconds)

        val down1 = down(1, 51.milliseconds)

        // Act

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down0))

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up0))

        testContext.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down1))

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        // Assert

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerEvent_downMoveOutOfBoundsWait_eventNotFired() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    // Tests that verify conditions under which onLongPress will be called.

    @Test
    fun onPointerEvent_downBeyondTimeout_eventFiredOnce() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerEvent_2DownBeyondTimeout_eventFiredOnce() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0), down(1)))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerEvent_downMoveOutOfBoundsWaitUpThenDownWait_eventFiredOnce() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        pointer = pointer.up(105.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        pointer = down(1, 200.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerEvent_2DownIndependentlyUnderTimeoutButOverlapTimeIsOver_eventFiredOnce() {

        // Arrange

        val down0 = down(0)

        val move0 = down0.moveTo(50.milliseconds, 0f, 0f)
        val down1 = down(1, 50.milliseconds)

        val up0 = move0.up(75.milliseconds)
        val move1 = down1.moveTo(75.milliseconds, 0f, 0f)

        // Act

        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(down0)
        )

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(move0, down1)
        )

        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(up0, move1)
        )

        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)

        // Assert

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerEvent_downMoveNotConsumed_eventFiredOnce() {
        val down = down(0)
        val move = down.moveBy(50.milliseconds, 1f, 1f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    // Tests that verify correctness of PxPosition value passed to onLongPress

    @Test
    fun onPointerEvent_down_onLongPressCalledWithDownPosition() {
        val down = down(0, x = 13f, y = 17f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(Offset(13f, 17f))
    }

    @Test
    fun onPointerEvent_downMove_onLongPressCalledWithMovePosition() {
        val down = down(0, x = 13f, y = 17f)
        val move = down.moveTo(50.milliseconds, 7f, 5f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(Offset(7f, 5f))
    }

    @Test
    fun onPointerEvent_downThenDown_onLongPressCalledWithFirstDownPosition() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveBy(50.milliseconds, 0f, 0f)
        val down1 = down(1, 50.milliseconds, 11f, 19f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down0))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move0, down1))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(Offset(13f, 17f))
    }

    @Test
    fun onPointerEvent_down0ThenDown1ThenUp0_onLongPressCalledWithDown1Position() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveTo(50.milliseconds, 27f, 29f)
        val down1 = down(1, 50.milliseconds, 11f, 19f)

        val up0 = move0.up(75.milliseconds)
        val move1 = down1.moveBy(25.milliseconds, 0f, 0f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down0))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move0, down1))
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up0, move1))
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(Offset(11f, 19f))
    }

    @Test
    fun onPointerEvent_down0ThenMove0AndDown1_onLongPressCalledWithMove0Position() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveTo(50.milliseconds, 27f, 29f)
        val down1 = down(1, 50.milliseconds, 11f, 19f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down0))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move0, down1))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(Offset(27f, 29f))
    }

    @Test
    fun onPointerEvent_down0Down1Move1Up0_onLongPressCalledWithMove1Position() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveBy(25.milliseconds, 0f, 0f)
        val down1 = down(1, 25.milliseconds, 11f, 19f)

        val up0 = move0.up(50.milliseconds)
        val move1 = down1.moveTo(50.milliseconds, 27f, 23f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down0))
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move0, down1))
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up0, move1))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(Offset(27f, 23f))
    }

    // Tests that verify that consumption behavior

    @Test
    fun onPointerEvent_1Down_notConsumed() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        assertThat(down.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerEvent_1DownThen1Down_notConsumed() {

        // Arrange

        val down0 = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(down0)
        )

        // Act

        testContext.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        val move0 = down0.moveTo(10.milliseconds, 0f, 0f)
        val down1 = down(0, 10.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(move0, down1)
        )

        // Assert

        assertThat(move0.consumed.downChange).isFalse()
        assertThat(down1.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerEvent_1DownUnderTimeUp_upNotConsumed() {

        // Arrange

        val down0 = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(down0)
        )

        // Act

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        val up = down0.up(50.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        // Assert

        assertThat(up.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerEvent_1DownOverTimeUp_upConsumedOnInitial() {

        // Arrange

        val down0 = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(
            pointerEventOf(down0)
        )

        // Act

        testContext.advanceTimeBy(101, TimeUnit.MILLISECONDS)
        val up0 = down0.up(100.milliseconds)
        filter.onPointerEvent(
            pointerEventOf(up0),
            PointerEventPass.Initial,
            IntSize(0, 0)
        )

        // Assert

        assertThat(up0.consumed.downChange).isTrue()
    }

    @Test
    fun onPointerEvent_1DownOverTimeMoveConsumedUp_upNotConsumed() {

        // Arrange

        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        pointer = pointer.moveTo(50.milliseconds, 5f).apply { consumePositionChange(1f, 0f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Act

        testContext.advanceTimeBy(51, TimeUnit.MILLISECONDS)
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Assert

        assertThat(pointer.consumed.downChange).isFalse()
    }

    // Tests that verify correct behavior around cancellation.

    @Test
    fun onCancel_downCancelBeyondTimeout_eventNotFired() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        filter.onCancel()
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onCancel_downAlmostTimeoutCancelTimeout_eventNotFired() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        filter.onCancel()
        testContext.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onCancel_downCancelDownTimeExpires_eventFiredOnce() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        filter.onCancel()
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    // Verify correct behavior around responding to LongPressFiredEvent

    @Test
    fun onCustomEvent_downCustomEventTimeout_eventNotFired() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        filter::onCustomEvent.invokeOverAllPasses(LongPressFiredEvent)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onCustomEvent_downCustomEventTimeoutDownTimeout_eventFiredOnce() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        filter::onCustomEvent.invokeOverAllPasses(LongPressFiredEvent)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }
}