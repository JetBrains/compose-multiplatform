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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.customevents.DelayUpEvent
import androidx.compose.ui.gesture.customevents.DelayUpMessage
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.invokeOverAllPasses
import androidx.compose.ui.input.pointer.invokeOverPass
import androidx.compose.ui.input.pointer.invokeOverPasses
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.input.pointer.up
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TapGestureFilterTest {

    private lateinit var filter: TapGestureFilter

    @Before
    fun setup() {
        filter = TapGestureFilter()
        filter.onTap = mock()
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // Verification for when onReleased should not be called.

    @Test
    fun onPointerEvent_down_onReleaseNotCalled() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0, 0.milliseconds)))
        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downConsumedUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds).apply { consumeDownChange() }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveConsumedUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.moveTo(100.milliseconds, 5f).apply { consumePositionChange(5f, 0f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(200.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downUpConsumed_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds).apply { consumeDownChange() }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(100.milliseconds).apply { consumeDownChange() }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsNegativeXUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, -1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsPositiveXUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsNegativeYUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 0f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsPositiveYUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        verify(filter.onTap, never()).invoke(any())
    }

    // Verification for when onReleased should be called.

    @Test
    fun onPointerEvent_downUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        verify(filter.onTap).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.moveTo(100.milliseconds, 5f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(200.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        verify(filter.onTap).invoke(any())
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsUpDownUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = down(1, duration = 150.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(200.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        verify(filter.onTap).invoke(any())
    }

    // Verification of correct up position reported

    @Test
    fun onPointerEvent_downUp_pxPositionIsCorrect() {
        val down = down(0, 0.milliseconds, 123f, 456f)
        val up = down.up(1.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        verify(filter.onTap).invoke(Offset(123f, 456f))
    }

    @Test
    fun onPointerEvent_downMoveUp_pxPositionIsCorrect() {
        val down = down(0, 0.milliseconds, 123f, 456f)
        val move = down.moveTo(1.milliseconds, 321f, 654f)
        val up = move.up(1.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        verify(filter.onTap).invoke(Offset(321f, 654f))
    }

    @Test
    fun onPointerEvent_2Down1Up2Up_pxPositionIsCorrect() {
        val downA = down(0, 0.milliseconds, 123f, 456f)

        val moveA = downA.moveTo(1.milliseconds, 123f, 456f)
        val downB = down(1, 1.milliseconds, 321f, 654f)

        val upA = moveA.up(2.milliseconds)
        val moveB = downB.moveTo(2.milliseconds, 321f, 654f)

        val upB = moveB.up(1.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(downA))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(moveA, downB))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upA, moveB))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upB))

        verify(filter.onTap).invoke(Offset(321f, 654f))
    }

    @Test
    fun onPointerEvent_2Down2Up1Up_pxPositionIsCorrect() {
        val downA = down(0, 0.milliseconds, 123f, 456f)

        val moveA1 = downA.moveTo(1.milliseconds, 123f, 456f)
        val downB = down(1, 1.milliseconds, 321f, 654f)

        val moveA2 = moveA1.moveTo(2.milliseconds, 123f, 456f)
        val upB = downB.up(2.milliseconds)

        val upA = moveA2.up(3.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(downA))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(moveA1, downB))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(moveA2, upB))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upA))

        verify(filter.onTap).invoke(Offset(123f, 456f))
    }

    @Test
    fun onPointerEvent_downDelayUpUpDelayUpReleased_pxPositionIsCorrect() {
        val down = down(890, 0.milliseconds, 123f, 456f)
        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    890
                )
            )
        )
        val up = down.up(1.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        890
                    )
                )
            )

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap).invoke(Offset(123f, 456f))
    }

    @Test
    fun onPointerEvent_2Down1DelayUp1Up2Up_pxPositionIsCorrect() {
        val downA = down(0, 0.milliseconds, 123f, 456f)

        val moveA = downA.moveTo(1.milliseconds, 123f, 456f)
        val downB = down(1, 1.milliseconds, 321f, 654f)

        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    0
                )
            )
        )

        val upA = moveA.up(2.milliseconds)
        val moveB = downB.moveTo(2.milliseconds, 321f, 654f)

        val upB = moveB.up(3.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(downA))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(moveA, downB))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upA, moveB))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upB))

        verify(filter.onTap).invoke(Offset(321f, 654f))
    }

    @Test
    fun onPointerEvent_2Down1Up2DelayUp2Up2ReleasedUp_pxPositionIsCorrect() {
        val downA = down(0, 0.milliseconds, 123f, 456f)

        val moveA = downA.moveTo(1.milliseconds, 123f, 456f)
        val downB = down(1, 1.milliseconds, 321f, 654f)

        val upA = moveA.up(2.milliseconds)
        val moveB = downB.moveTo(2.milliseconds, 321f, 654f)

        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    1
                )
            )
        )

        val upB = moveB.up(3.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        1
                    )
                )
            )

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(downA))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(moveA, downB))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upA, moveB))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upB))
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap).invoke(Offset(321f, 654f))
    }

    // Verification that the down changes should not be consumed.

    @Test
    fun onPointerEvent_down_downChangeNotConsumed() {
        val down = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        assertThat(down.consumed.downChange, `is`(false))
    }

    // Verification for when the up change should not be consumed.

    @Test
    fun onPointerEvent_downMoveOutsideBoundsNegativeXUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, -1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        assertThat(pointer.consumed.downChange, `is`(false))
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsPositiveXUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        assertThat(pointer.consumed.downChange, `is`(false))
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsNegativeYUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 0f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        assertThat(pointer.consumed.downChange, `is`(false))
    }

    @Test
    fun onPointerEvent_downMoveOutsideBoundsPositiveYUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer), IntSize(1, 1))

        assertThat(pointer.consumed.downChange, `is`(false))
    }

    @Test
    fun onPointerEvent_consumeChangesIsFalse_upChangeNotConsumed() {
        filter.consumeChanges = false
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        assertThat(pointer.consumed.downChange, `is`(false))
    }

    // Verification for when the up change should be consumed.

    @Test
    fun onPointerEvent_consumeChangesIsTrue_upChangeConsumed() {
        filter.consumeChanges = true
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    @Test
    fun onPointerEvent_consumeChangesIsDefault_upChangeConsumed() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    // Verification for during what pass the changes are consumed.

    @Test
    fun onPointerEvent_upChangeConsumedDuringMain() {
        val pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        val pointerEventChange = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(pointerEventChange),
            PointerEventPass.Initial
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(false))

        filter::onPointerEvent.invokeOverPass(
            pointerEventOf(pointerEventChange),
            PointerEventPass.Main,
            IntSize(0, 0)
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    // Verification of correct cancellation behavior.

    @Test
    fun cancellationHandler_downCancelUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        filter.onCancel()
        pointer = pointer.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        verify(filter.onTap, never()).invoke(any())
    }

    // Verification of correct behavior regarding custom messages.

    @Test
    fun onPointerEvent_downDelayUpUp_onTapNotCalled() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    0
                )
            )
        )
        val up = down.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downDelayUpUpDelayUpConsumed_onTapNotCalled() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    0
                )
            )
        )
        val up = down.up(100.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpConsumed,
                setOf(
                    PointerId(
                        0
                    )
                )
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap, never()).invoke(any())
    }

    @Test
    fun onPointerEvent_downDelayUpUpDelayUpNotConsumed_onTapCalled() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    0
                )
            )
        )
        val up = down.up(100.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        0
                    )
                )
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap).invoke(any())
    }

    @Test
    fun onPointerEvent_downDelayUpUp_upNotConsumed() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    0
                )
            )
        )
        val up = down.up(100.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        assertThat(up.consumed.downChange, `is`(false))
    }

    @Test
    fun onPointerEvent_downDelayUpUpDelayUpNotConsumed_messageChangedToConsumed() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(
            DelayUpMessage.DelayUp,
            setOf(
                PointerId(
                    0
                )
            )
        )
        val up = down.up(100.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        0
                    )
                )
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        assertThat(delayUpNotConsumed.message, `is`(DelayUpMessage.DelayedUpConsumed))
    }

    // Verification of complex tests where some up events are blocked and others are not

    /**
     * 2 Pointers down, first up but blocked, then first is released.  1 pointer still down so
     * onTap should not be called.
     */

    @Test
    fun onPointerEvent_2Down1DelayUpAndUpAndDelayUpNotConsumed_onTapNotCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap, never()).invoke(any())
    }

    /**
     * 2 Pointers down, first up but blocked, then first is released, then 2nd goes up.  2nd
     * pointer went up and nothing was blocked, so on tap should be called (and only once).
     */

    @Test
    fun onPointerEvent_2Down1DelayUpAndUpAndDelayUpNotConsumed2ndUp_onTapNotCalledOnce() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(bUp))

        verify(filter.onTap).invoke(any())
        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up but blocked, then second is up.  onTap should fire because the
     * last finger to leave was not blocked.
     */

    @Test
    fun onPointerEvent_2Down1DelayUpAndUp2ndUp_onTapCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(bUp))

        verify(filter.onTap).invoke(any())
        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up but blocked, then second is up, then first is unblocked.  onTap
     * should have already fired with the second went up, so we shouldn't fire again.
     */

    @Test
    fun onPointerEvent_2Down1DelayUpAndUp2ndUp1stDelayUpNotConsumed_onTapNotCalledAfterLastCall() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val bUp = bMove.up(3.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(bUp))
        reset(filter.onTap)
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up, then second up and is blocked. Last to go up was blocked so
     * onTap should not be called.
     */

    @Test
    fun onPointerEvent_2Down1stUp2ndDelayUpAndUp_onTapNotCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        456
                    )
                )
            )

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(bUp))

        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up, then second up and is blocked, then 2nd released. Last to go up
     * was blocked, and then unblocked, so onTap should be called.
     */

    @Test
    fun onPointerEvent_2Down1stUp2ndDelayUpAndUpAndReleased_onTapCalledOnce() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayBUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        456
                    )
                )
            )

        val bUp = bMove.up(3.milliseconds)

        val releaseBUp =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        456
                    )
                )
            )

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onCustomEvent.invokeOverAllPasses(delayBUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(bUp))
        filter::onCustomEvent.invokeOverAllPasses(releaseBUp)

        verify(filter.onTap).invoke(any())
        verifyNoMoreInteractions(filter.onTap)
    }

    // 2 down, then 1 up delayed and up, then same up not consumed.  We are still waiting for the
    // 2nd to go up so onTap should not be called.
    @Test
    fun onPointerEvent_2Down1stDelayUpThenUpThenDelayUpNotConsumed_onTapNotCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayAUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayAUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayAUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onCustomEvent.invokeOverAllPasses(delayAUpNotConsumed)

        verifyNoMoreInteractions(filter.onTap)
    }

    // 2 down, then 1 up delayed and up, then same up not consumed, then 2nd up.  By the time the
    // last pointer went up, the other pointer was delayed up, and then went up, and then was
    // allowed to go up, so by the time the 2nd up happened, we should fire onTap.
    @Test
    fun onPointerEvent_2Down1stDelayUpThenUpThenDelayUpNotConsumedThen2ndUp_onTapCalledOnce() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayAUp =
            DelayUpEvent(
                DelayUpMessage.DelayUp,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayAUpNotConsumed =
            DelayUpEvent(
                DelayUpMessage.DelayedUpNotConsumed,
                setOf(
                    PointerId(
                        123
                    )
                )
            )

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aDown))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aMove, bDown))
        filter::onCustomEvent.invokeOverAllPasses(delayAUp)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(aUp, bMove))
        filter::onCustomEvent.invokeOverAllPasses(delayAUpNotConsumed)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(bUp))

        verify(filter.onTap).invoke(any())
        verifyNoMoreInteractions(filter.onTap)
    }

    @Test
    fun testInspectorValueForTapGestureFilter() = runBlocking<Unit> {
        val onTap: (Offset) -> Unit = {}
        val modifier = Modifier.tapGestureFilter(onTap) as InspectableValue

        assertThat(modifier.nameFallback).isEqualTo("tapGestureFilter")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("onTap", onTap)
        )
    }

    @Test
    fun testInspectorValueForNoConsumptionTapGestureFilter() = runBlocking<Unit> {
        val onTap: (Offset) -> Unit = {}
        val modifier = Modifier.noConsumptionTapGestureFilter(onTap) as InspectableValue

        assertThat(modifier.nameFallback).isEqualTo("noConsumptionTapGestureFilter")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("onTap", onTap)
        )
    }
}
