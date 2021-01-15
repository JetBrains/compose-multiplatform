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
import androidx.compose.ui.gesture.scrollorientationlocking.InternalScrollOrientationLocker
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.gesture.scrollorientationlocking.ShareScrollOrientationLockerEvent
import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.invokeOverAllPasses
import androidx.compose.ui.input.pointer.invokeOverPasses
import androidx.compose.ui.input.pointer.moveBy
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.input.pointer.up
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following tests.
// Test for cases where things are reset when last pointer goes up
// Verify methods called during Main and Final
// Verify correct behavior when distance is consumed at different moments between passes.
// Verify correct behavior with no DragBlocker

@RunWith(JUnit4::class)
class RawDragGestureFilterTest {

    private lateinit var filter: RawDragGestureFilter
    private lateinit var dragObserver: MockDragObserver
    private lateinit var log: MutableList<LogItem>
    private lateinit var customEventDispatcher: CustomEventDispatcher
    private var dragStartBlocked = true

    @Before
    fun setup() {
        log = mutableListOf()
        dragObserver = MockDragObserver(log)
        filter = RawDragGestureFilter()
        filter.canStartDragging = { !dragStartBlocked }
        filter.dragObserver = dragObserver
        customEventDispatcher = mock()
        filter.onInit(customEventDispatcher)
    }

    // Verify the circumstances under which onStart/OnDrag should not be called.

    @Test
    fun onPointerEvent_blockedAndMove_onStartAndOnDragNotCalled() {

        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(10, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerEvent_unblockedNoMove_onStartAndOnDragNotCalled() {
        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false

        val move = down.moveBy(10, 0f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerEvent_unblockedMovementConsumed_onStartAndOnDragNotCalled() {

        val down1 = down(1)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down1))
        dragStartBlocked = false

        val move1 =
            down1.moveBy(10, 1f, 1f).apply { consumePositionChange(1f, 1f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerEvent_unblockedMovementIsInOppositeDirections_onStartAndOnDragNotCalled() {

        val down1 = down(1)
        val down2 = down(2)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down1, down2))
        dragStartBlocked = false

        val move1 = down1.moveBy(10, 1f, 1f)
        val move2 = down2.moveBy(10, -1f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1, move2))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerEvent_3PointsMoveAverage0_onStartAndOnDragNotCalled() {

        // Arrange

        val pointers = arrayOf(down(0), down(1), down(2))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(*pointers))
        dragStartBlocked = false

        // Act

        // These movements average to no movement.
        pointers[0] =
            pointers[0].moveBy(
                100,
                -1f,
                -1f
            )
        pointers[1] =
            pointers[1].moveBy(
                100,
                1f,
                -1f
            )
        pointers[2] =
            pointers[2].moveBy(
                100,
                0f,
                2f
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(*pointers))

        // Assert
        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    // Verify the circumstances under which onStart/OnDrag should be called.

    @Test
    fun onPointerEvent_unblockedAndMoveOnX_onStartAndOnDragCalledOnce() {

        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false

        val move = down.moveBy(10, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_unblockedAndMoveOnY_oonStartAndOnDragCalledOnce() {

        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false

        val move = down.moveBy(10, 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_unblockedAndMoveConsumedBeyond0_onStartAndOnDragCalledOnce() {

        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false

        val move = down.moveBy(10, 1f, 0f).apply { consumePositionChange(2f, 0f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(1)
    }

    // onDrag called with correct values verification.

    @Test
    fun onPointerEvent_unblockedMove_onDragCalledWithTotalDistance() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        change = change.moveBy(100, 5f, -2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(1)
        assertThat(onDragLog[0].pxPosition).isEqualTo(Offset(5f, -2f))
    }

    @Test
    fun onPointerEvent_unblockedMoveAndMoveConsumed_onDragCalledWithCorrectDistance() {

        // Arrange

        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        // Act

        change = change.moveBy(100, 3f, -5f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.moveBy(100, -3f, 7f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.moveBy(100, 11f, 13f)
            .apply { consumePositionChange(5f, 3f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.moveBy(100, -13f, -11f)
            .apply { consumePositionChange(-3f, -5f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        // Assert

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(4)
        // OnDrags get's called twice each time because RawDragGestureDetector calls it on both
        // Main and Final and the distance is not consumed by Main.
        assertThat(onDragLog[0].pxPosition).isEqualTo(Offset(3f, -5f))
        assertThat(onDragLog[1].pxPosition).isEqualTo(Offset(-3f, 7f))
        assertThat(onDragLog[2].pxPosition).isEqualTo(Offset(6f, 10f))
        assertThat(onDragLog[3].pxPosition).isEqualTo(Offset(-10f, -6f))
    }

    @Test
    fun onPointerEvent_3Down1Moves_onDragCalledWith3rdOfDistance() {

        // Arrange

        var pointer1 = down(0)
        var pointer2 = down(1)
        var pointer3 = down(2)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))
        dragStartBlocked = false

        pointer1 = pointer1.moveBy(100, 9f, -12f)
        pointer2 = pointer2.moveBy(100, 0f, 0f)
        pointer3 = pointer3.moveBy(100, 0f, 0f)

        // Act

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))

        // Assert

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(1)
        assertThat(onDragLog[0].pxPosition).isEqualTo(
            Offset(3f, -4f)
        )
    }

    // onStop not called verification

    @Test
    fun onPointerEvent_blockedDownMoveUp_onStopNotCalled() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.moveTo(10, 1f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun onPointerEvent_unBlockedDownUp_onStopNotCalled() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false
        change = change.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun onPointerEvent_unBlockedDownMoveAverage0Up_onStopNotCalled() {
        var change1 = down(1)
        var change2 = down(2)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        dragStartBlocked = false
        change1 = change1.moveBy(10, 1f, 1f)
        change2 = change2.moveBy(10, -1f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        assertThat(log.filter { it.methodName == "onStop" }).isEmpty()
    }

    // onStop called verification

    @Test
    fun onPointerEvent_unblockedDownMoveUp_onStopCalledOnce() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false
        change = change.moveTo(10, 1f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(1)
    }

    // onStop called with correct values verification

    @Test
    fun onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity() {
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(0f, 1f, 0f, 100f)
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(0f, -1f, 0f, -100f)
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, 0f, 100f, 0f)
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, 0f, -100f, 0f)

        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, 1f, 100f, 100f)
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, 1f, -100f, 100f)
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, -1f, 100f, -100f)
        onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, -1f, -100f, -100f)
    }

    private fun onPointerEvent_flingBeyondSlop_onStopCalledWithCorrectVelocity(
        incrementPerMilliX: Float,
        incrementPerMilliY: Float,
        expectedPxPerSecondDx: Float,
        expectedPxPerSecondDy: Float
    ) {
        log.clear()

        var time = 0L

        var change = down(0, time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        repeat(11) {
            time += 10
            change = change.moveBy(
                10,
                incrementPerMilliX,
                incrementPerMilliY
            )
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        }

        time += 10
        change = change.up(time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        val loggedStops = log.filter { it.methodName == "onStop" }
        assertThat(loggedStops).hasSize(1)
        val velocity = loggedStops[0].pxPosition!!
        assertThat(velocity.x).isWithin(.01f).of(expectedPxPerSecondDx)
        assertThat(velocity.y).isWithin(.01f).of(expectedPxPerSecondDy)
    }

    // Verification that callbacks occur in the correct order

    @Test
    fun onPointerEvent_unblockDownMoveUp_callBacksOccurInCorrectOrder() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        change = change.moveTo(
            10,
            0f,
            1f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(log).hasSize(3)
        assertThat(log[0].methodName).isEqualTo("onStart")
        assertThat(log[1].methodName).isEqualTo("onDrag")
        assertThat(log[2].methodName).isEqualTo("onStop")
    }

    // Verification about what events are, or aren't consumed.

    @Test
    fun onPointerEvent_down_downNotConsumed() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        assertThat(down.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerEvent_blockedDownMove_distanceChangeNotConsumed() {

        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.moveTo(
            10,
            1f,
            0f
        )
        dragObserver.dragConsume = Offset(7f, -11f)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change),
            PointerEventPass.Initial,
            PointerEventPass.Main
        )
        dragObserver.dragConsume = Offset.Zero
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change),
            PointerEventPass.Initial,
            PointerEventPass.Main
        )

        assertThat(change.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerEvent_unblockedDownMoveCallBackDoesNotConsume_distanceChangeNotConsumed() {
        dragObserver.dragConsume = Offset.Zero

        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        change = change.moveTo(
            10,
            1f,
            1f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(change.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerEvent_unblockedMoveOccursDefaultOnDrag_distanceChangeNotConsumed() {
        dragObserver.dragConsume = null

        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        change = change.moveTo(
            10,
            1f,
            1f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(change.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerEvent_moveCallBackConsumes_changeDistanceConsumedByCorrectAmount() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change), IntSize(0, 0))
        dragStartBlocked = false

        change = change.moveTo(
            10,
            3f,
            -5f
        )
        dragObserver.dragConsume = Offset(7f, -11f)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change),
            PointerEventPass.Initial,
            PointerEventPass.Main
        )
        dragObserver.dragConsume = Offset.Zero
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change),
            PointerEventPass.Final
        )

        assertThat(change.consumed.positionChange.x).isEqualTo(7f)
        assertThat(change.consumed.positionChange.y).isEqualTo(-11f)
    }

    @Test
    fun onPointerEvent_onStopConsumesUp() {
        var change = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false

        change = change.moveTo(
            10,
            1f,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(change.consumed.downChange).isTrue()
    }

    @Test
    fun onPointerEvent_move_onStartCalledWithDownPosition() {
        val down = down(0, 0, x = 3f, y = 4f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false

        val move = down.moveBy(10, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.first { it.methodName == "onStart" }.pxPosition)
            .isEqualTo(Offset(3f, 4f))
    }

    @Test
    fun onPointerEvent_3PointsMove_onStartCalledWithDownPositions() {
        var pointer1 = down(1, x = 1f, y = 2f)
        var pointer2 = down(2, x = 5f, y = 4f)
        var pointer3 = down(3, x = 3f, y = 6f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))
        dragStartBlocked = false

        pointer1 = pointer1.moveBy(100, 1f, 0f)
        pointer2 = pointer2.moveBy(100, 0f, 0f)
        pointer3 = pointer3.moveBy(100, 0f, 0f)

        // Act

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))

        assertThat(log.first { it.methodName == "onStart" }.pxPosition)
            // average position
            .isEqualTo(Offset(3f, 4f))
    }

    @Test
    fun onPointerEvent_hasOrientationDownEvent_customEventDispatchedOnceDuringInitial() {

        // Arrange

        filter.orientation = Orientation.Vertical

        val down = down(0)

        // Act / Verify 1

        filter::onPointerEvent.invokeOverPasses(pointerEventOf(down), PointerEventPass.Initial)
        argumentCaptor<ShareScrollOrientationLockerEvent>().run {
            verify(customEventDispatcher).dispatchCustomEvent(capture())
            assertThat(allValues).hasSize(1)
            assertThat(allValues.first().scrollOrientationLocker).isNotNull()
        }

        reset(customEventDispatcher)

        // Act / Verify 1
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(down),
            PointerEventPass.Main,
            PointerEventPass.Final
        )
        verifyNoMoreInteractions(customEventDispatcher)
    }

    @Test
    fun onPointerEvent_hasOrientationDownUpDown_customEventDispatchedTwiceWithDifferentLocker() {
        filter.orientation = Orientation.Vertical

        val downA = down(0)
        val upA = downA.up(1)
        val downB = down(1, 2)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(downA))

        val locker1 = argumentCaptor<ShareScrollOrientationLockerEvent>().run {
            verify(customEventDispatcher).dispatchCustomEvent(capture())

            assertThat(allValues).hasSize(1)
            allValues.first().scrollOrientationLocker
        }

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(upA))
        reset(customEventDispatcher)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(downB))

        val locker2 = argumentCaptor<ShareScrollOrientationLockerEvent>().run {
            verify(customEventDispatcher).dispatchCustomEvent(capture())

            assertThat(allValues).hasSize(1)
            allValues.first().scrollOrientationLocker
        }

        assertThat(locker1).isNotEqualTo(locker2)

        verifyNoMoreInteractions(customEventDispatcher)
    }

    // The below tests verify correct behavior in relation to scroll orientation locking.

    @Test
    fun onPointerEvent_filterHorizontalPointerLockedToVertical_noStart() {
        onPointerEvent_differentOrientations(
            Orientation.Vertical,
            Orientation.Horizontal
        )
    }

    @Test
    fun onPointerEvent_filterVerticalPointerLockedToHorizontal_noStart() {
        onPointerEvent_differentOrientations(
            Orientation.Horizontal,
            Orientation.Vertical
        )
    }

    @Test
    fun onPointerEvent_filterHorizontalPointerLockedToHorizontal_start() {
        onPointerEvent_differentOrientations(
            Orientation.Horizontal,
            Orientation.Horizontal
        )
    }

    @Test
    fun onPointerEvent_filterVerticalPointerLockedToVertical_start() {
        onPointerEvent_differentOrientations(
            Orientation.Vertical,
            Orientation.Vertical
        )
    }

    private fun onPointerEvent_differentOrientations(
        filterOrientation: Orientation,
        lockedOrientation: Orientation
    ) {

        // Arrange

        filter.orientation = filterOrientation
        dragStartBlocked = false

        val scrollOrientationLocker = InternalScrollOrientationLocker()

        filter::onCustomEvent.invokeOverAllPasses(
            ShareScrollOrientationLockerEvent(scrollOrientationLocker)
        )

        val down = down(0)
        val move = down.moveBy(1, 3f, 5f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        scrollOrientationLocker.attemptToLockPointers(listOf(move), lockedOrientation)

        // Act
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Assert
        if (filterOrientation == lockedOrientation) {
            assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
            assertThat(log.filter { it.methodName == "onDrag" }).hasSize(1)
        } else {
            assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
            assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
        }
    }

    @Test
    fun onPointerEvent_filterIsHorizontalMovementIsHorizontal_locksHorizontal() {
        onPointerEvent_mayLockPointers(false, Orientation.Horizontal, 1f, 0f)
    }

    @Test
    fun onPointerEvent_filterIsVerticalMovementIsVertical_locksVertical() {
        onPointerEvent_mayLockPointers(false, Orientation.Vertical, 0f, 1f)
    }

    @Test
    fun onPointerEvent_filterIsHorizontalMovementIsBoth_locksHorizontal() {
        onPointerEvent_mayLockPointers(false, Orientation.Horizontal, -1f, -1f)
    }

    @Test
    fun onPointerEvent_filterIsVerticalMovementIsBoth_locksVertical() {
        onPointerEvent_mayLockPointers(false, Orientation.Vertical, -1f, -1f)
    }

    @Test
    fun onPointerEvent_filterIsHorizontalMovementIsHorizontalBlocked_locksHorizontal() {
        onPointerEvent_mayLockPointers(true, Orientation.Horizontal, 1f, 0f)
    }

    @Test
    fun onPointerEvent_filterIsVerticalMovementIsVerticalBlocked_locksVertical() {
        onPointerEvent_mayLockPointers(true, Orientation.Vertical, 0f, 1f)
    }

    @Test
    fun onPointerEvent_filterIsHorizontalMovementIsBothBlocked_locksHorizontal() {
        onPointerEvent_mayLockPointers(true, Orientation.Horizontal, -1f, -1f)
    }

    @Test
    fun onPointerEvent_filterIsVerticalMovementIsBothBlocked_locksVertical() {
        onPointerEvent_mayLockPointers(true, Orientation.Vertical, -1f, -1f)
    }

    @Test
    fun onPointerEvent_filterIsHorizontalMovementIsVertical_locksNone() {
        onPointerEvent_mayLockPointers(false, Orientation.Horizontal, 0f, -1f)
    }

    @Test
    fun onPointerEvent_filterIsVerticalMovementIsHorizontal_locksNone() {
        onPointerEvent_mayLockPointers(false, Orientation.Vertical, -1f, 0f)
    }

    private fun onPointerEvent_mayLockPointers(
        blocked: Boolean,
        filterOrientation: Orientation,
        dx: Float,
        dy: Float
    ) {

        // Arrange

        val otherOrientation =
            when (filterOrientation) {
                Orientation.Vertical -> Orientation.Horizontal
                Orientation.Horizontal -> Orientation.Vertical
            }

        filter.orientation = filterOrientation

        dragStartBlocked = blocked

        val scrollOrientationLocker = InternalScrollOrientationLocker()

        filter::onCustomEvent.invokeOverAllPasses(
            ShareScrollOrientationLockerEvent(scrollOrientationLocker)
        )

        val down = down(0)
        val move = down.moveBy(1, dx, dy)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        // Act
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Assert
        if (!blocked && (
            filterOrientation == Orientation.Horizontal && dx != 0f ||
                filterOrientation == Orientation.Vertical && dy != 0f
            )
        ) {
            assertThat(scrollOrientationLocker.getPointersFor(listOf(move), otherOrientation))
                .hasSize(0)
        } else {
            assertThat(scrollOrientationLocker.getPointersFor(listOf(move), otherOrientation))
                .hasSize(1)
        }
    }

    @Test
    fun onPointerEvent_Hori3Pointers1LockedVert2Average0__onStartAndOnDragNotCalled() {

        // Arrange

        filter.orientation = Orientation.Horizontal

        val scrollOrientationLocker = InternalScrollOrientationLocker()
        filter::onCustomEvent.invokeOverAllPasses(
            ShareScrollOrientationLockerEvent(scrollOrientationLocker)
        )

        val pointers = arrayOf(down(0), down(1), down(2))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(*pointers))

        dragStartBlocked = false

        // This pointer is going to be locked to vertical.
        pointers[0] =
            pointers[0].moveBy(
                100,
                100f,
                0f
            )
        scrollOrientationLocker.attemptToLockPointers(listOf(pointers[0]), Orientation.Vertical)

        // These pointers average to no movement.
        pointers[1] =
            pointers[1].moveBy(
                100,
                1f,
                0f
            )
        pointers[2] =
            pointers[2].moveBy(
                100,
                -1f,
                0f
            )

        // Act
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(*pointers))

        // Assert
        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerEvent_2Pointers1LockedInWrongOrientationOtherGoesUpThenItGoesUp_isCorrect() {

        // Arrange

        // Basic set up
        filter.orientation = Orientation.Horizontal
        dragStartBlocked = false
        val scrollOrientationLocker = InternalScrollOrientationLocker()
        filter::onCustomEvent.invokeOverAllPasses(
            ShareScrollOrientationLockerEvent(scrollOrientationLocker)
        )

        // One finger down
        var time = 0L
        var pointer1 = down(0, time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1))

        // 2nd finger comes into play
        time = 10L
        pointer1.moveTo(time)
        var pointer2 = down(1, time)

        // Lock the 2nd pointer to vertical
        scrollOrientationLocker.attemptToLockPointers(listOf(pointer2), Orientation.Vertical)

        // Dispatch 2nd finger down.
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Both pointers move a bunch.
        repeat(11) {
            time = 10
            pointer1 = pointer1.moveBy(
                10,
                1f,
                0f
            )
            pointer2 = pointer2.moveBy(
                10,
                1f,
                0f
            )
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        }

        // Act 1

        // Only Pointer 1 goes up
        time = 10
        pointer1 = pointer1.up(time)
        pointer2 = pointer2.moveTo(time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Assert 1

        // One pointer is still down, and even though it is locked in the other orientation, we
        // still shouldn't stop yet.
        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)

        // Act 2

        // 2nd is up
        time = 10
        pointer2 = pointer2.up(time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer2))

        // This finger lifting should contribute no flinging since it was locked to a different
        // orientation.
        val loggedStops = log.filter { it.methodName == "onStop" }
        assertThat(loggedStops).hasSize(1)
        val velocity = loggedStops[0].pxPosition!!
        assertThat(velocity.x).isEqualTo(0)
        assertThat(velocity.y).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_2Pointers1LockedInWrongOrientationItGoesUpThenOtherGoesUp_isCorrect() {

        // Arrange

        // Basic set up
        filter.orientation = Orientation.Horizontal
        dragStartBlocked = false
        val scrollOrientationLocker = InternalScrollOrientationLocker()
        filter::onCustomEvent.invokeOverAllPasses(
            ShareScrollOrientationLockerEvent(scrollOrientationLocker)
        )

        var time = 0L

        // One finger down
        var pointer1 = down(0, time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1))

        // 2nd finger comes into play
        time += 10L
        pointer1.moveTo(time)
        var pointer2 = down(1, time)

        // Lock the 2nd pointer to vertical
        scrollOrientationLocker.attemptToLockPointers(listOf(pointer2), Orientation.Vertical)

        // Dispatch 2nd finger down.
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Both pointers move a bunch.
        repeat(11) {
            time += 10
            pointer1 = pointer1.moveBy(
                10,
                1f,
                0f
            )
            pointer2 = pointer2.moveBy(
                10,
                1f,
                0f
            )
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        }

        // Act 1

        // Only Pointer 2 goes up
        time += 10
        pointer1 = pointer1.moveBy(
            10,
            1f,
            0f
        )
        pointer2 = pointer2.up(time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Assert 1

        // One pointer is still down, and even though it is locked in the other orientation, we
        // still shouldn't stop yet.
        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)

        // Act 2

        // 2nd is up
        time += 10
        pointer1 = pointer1.up(time)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1))

        // This finger lifting should contribute no flinging since it was locked to a different
        // orientation.
        val loggedStops = log.filter { it.methodName == "onStop" }
        assertThat(loggedStops).hasSize(1)
        val velocity = loggedStops[0].pxPosition!!
        assertThat(velocity.x).isWithin(.01f).of(100f)
        assertThat(velocity.y).isWithin(.01f).of(0f)
    }

    // Tests that verify when onCancel should not be called.

    @Test
    fun onCancel_downCancel_onCancelNotCalled() {
        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).isEmpty()
    }

    @Test
    fun onCancel_blockedDownMoveCancel_onCancelNotCalled() {
        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = true
        val move = down.moveBy(1, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).isEmpty()
    }

    // Tests that verify when onCancel should be called.

    @Test
    fun onCancel_downMoveCancel_onCancelCalledOnce() {
        val down = down(0, 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        dragStartBlocked = false
        val move = down.moveBy(1, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).hasSize(1)
    }

    // Tests that cancel behavior is correct.

    @Test
    fun onCancel_downCancelDownMove_startPositionIsSecondDown() {
        val down1 = down(1, x = 3f, y = 5f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down1))
        dragStartBlocked = false
        filter.onCancel()

        val down2 = down(2, x = 7f, y = 11f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down2))

        val move = down2.moveBy(10, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(log.first { it.methodName == "onStart" }.pxPosition)
            .isEqualTo(Offset(7f, 11f))
    }

    @Test
    fun onCancel_downMoveCancelDownMoveUp_flingIgnoresMoveBeforeCancel() {

        // Act.

        // Down, move, cancel.
        var change = down(0, durationMillis = 0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false
        repeat(11) {
            change = change.moveBy(
                10,
                -1f,
                -1f
            )
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        }
        filter.onCancel()

        // Down, Move, Up
        change = down(1, durationMillis = 200)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        dragStartBlocked = false
        repeat(11) {
            change = change.moveBy(
                10,
                1f,
                1f
            )
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        }
        change = change.up(310)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        // Assert.

        // Fling velocity should only take account of the second Down, Move, Up.
        val loggedStops = log.filter { it.methodName == "onStop" }
        assertThat(loggedStops).hasSize(1)
        val velocity = loggedStops[0].pxPosition!!
        assertThat(velocity.x).isWithin(.01f).of(100f)
        assertThat(velocity.y).isWithin(.01f).of(100f)
    }

    data class LogItem(
        val methodName: String,
        val pxPosition: Offset? = null
    )

    class MockDragObserver(
        private val log: MutableList<LogItem>,
        var dragConsume: Offset? = null
    ) : DragObserver {

        override fun onStart(downPosition: Offset) {
            log.add(LogItem("onStart", pxPosition = downPosition))
            super.onStart(downPosition)
        }

        override fun onDrag(dragDistance: Offset): Offset {
            log.add(LogItem("onDrag", pxPosition = dragDistance))
            return dragConsume ?: super.onDrag(dragDistance)
        }

        override fun onStop(velocity: Offset) {
            log.add(LogItem("onStop", pxPosition = velocity))
            super.onStop(velocity)
        }

        override fun onCancel() {
            log.add(LogItem("onCancel", pxPosition = null))
            super.onCancel()
        }
    }
}
