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

@file:Suppress("PrivatePropertyName")

package androidx.compose.ui.gesture

import androidx.compose.ui.gesture.scrollorientationlocking.InternalScrollOrientationLocker
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.gesture.scrollorientationlocking.ScrollOrientationLocker
import androidx.compose.ui.gesture.scrollorientationlocking.ShareScrollOrientationLockerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.invokeOverAllPasses
import androidx.compose.ui.input.pointer.invokeOverPass
import androidx.compose.ui.input.pointer.invokeOverPasses
import androidx.compose.ui.input.pointer.moveBy
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.input.pointer.up
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following tests.
// Verify correct shape of slop area (should it be a square or circle)?
// Test for cases with more than one pointer
// Test for cases where things are reset when last pointer goes up
// Verify all methods called during onMain
// Verify default behavior when no callback provided for filter or canDrag

// Changing this value will break tests that expect the value to be 10.
private const val TestTouchSlop = 10

@RunWith(JUnit4::class)
class DragSlopExceededGestureFilterTest {

    private val onDragSlopExceeded: () -> Unit = { onDragSlopExceededCallCount++ }
    private val canDrag: (Direction) -> Boolean = { direction ->
        canDragDirections.add(direction)
        canDragReturn
    }
    private var onDragSlopExceededCallCount: Int = 0
    private var canDragReturn = false
    private var canDragDirections: MutableList<Direction> = mutableListOf()
    private lateinit var filter: DragSlopExceededGestureFilter

    private val TinyNum = .01f

    @Before
    fun setup() {
        onDragSlopExceededCallCount = 0
        canDragReturn = true
        canDragDirections.clear()
        filter =
            DragSlopExceededGestureFilter(TestTouchSlop.toFloat())
        filter.setDraggableData(null, canDrag)
        filter.onDragSlopExceeded = onDragSlopExceeded
        filter.scrollOrientationLocker = ScrollOrientationLocker(mock())
    }

    // Verify the circumstances under which canDrag should not be called.

    @Test
    fun onPointerEvent_down_canDragNotCalled() {
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down(0)))
        assertThat(canDragDirections).isEmpty()
    }

    @Test
    fun onPointerEvent_downUp_canDragNotCalled() {
        val down = down(0, duration = 0.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val up = down.up(10.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        assertThat(canDragDirections).isEmpty()
    }

    @Test
    fun onPointerEvent_downMoveFullyConsumed_canDragNotCalled() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move =
            down.moveBy(Duration(milliseconds = 10), 3f, 5f)
                .apply { consumePositionChange(3f, 5f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(canDragDirections).isEmpty()
    }

    // Verify the circumstances under which canDrag should be called.

    @Test
    fun onPointerEvent_downMove1Dimension_canDragCalledOnce() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(Duration(milliseconds = 10), 3f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Twice because while under touch slop, TouchSlopExceededGestureDetector checks during Main and Final
        assertThat(canDragDirections).hasSize(2)
    }

    @Test
    fun onPointerEvent_downMove2Dimensions_canDragCalledTwice() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(Duration(milliseconds = 10), 3f, 5f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // 4 times because while under touch slop, TouchSlopExceededGestureDetector checks during Main and
        // Final
        assertThat(canDragDirections).hasSize(4)
    }

    @Test
    fun onPointerEvent_downMoveOneDimensionPartiallyConsumed_canDragCalledOnce() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move =
            down.moveBy(Duration(milliseconds = 10), 0f, 5f)
                .apply { consumePositionChange(0f, 4f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Twice because while under touch slop, DragGestureDetector checks during Main and
        // Final
        assertThat(canDragDirections).hasSize(2)
    }

    @Test
    fun onPointerEvent_downMoveTwoDimensionPartiallyConsumed_canDragCalledTwice() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move =
            down.moveBy(Duration(milliseconds = 10), 3f, 5f)
                .apply { consumePositionChange(2f, 4f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // 4 times because while under touch slop, DragGestureDetector checks during Main and
        // Final
        assertThat(canDragDirections).hasSize(4)
    }

    @Test
    fun onPointerEvent_dragPastTouchSlopOneDimensionAndDrag3MoreTimes_canDragCalledOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        var move = down.moveTo(10.milliseconds, 0f, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        repeat(3) {
            move = move.moveBy(Duration(milliseconds = 10), 0f, 1f)
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        }

        // Once because although DragGestureDetector checks during Main and Final, slop is
        // surpassed during Main, and thus isn't checked again.
        assertThat(canDragDirections).hasSize(1)
    }

    @Test
    fun onPointerEvent_downMoveUnderSlop3Times_canDragCalled3Times() {
        val thirdSlop = TestTouchSlop / 3

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        var move = down
        repeat(3) {
            move = move.moveBy(Duration(milliseconds = 10), 0f, thirdSlop.toFloat())
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        }

        // 6 times because while under touch slop, DragGestureDetector checks during Main and
        // Final
        assertThat(canDragDirections).hasSize(6)
    }

    @Test
    fun onPointerEvent_moveBeyondSlopThenIntoTouchSlopAreaAndOutAgain_canDragCalledOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        var event = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))
        // Out of touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))
        // Back into touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, -beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))
        // Out of touch slop region again
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))

        // Once because although DragGestureDetector checks during Main and Final, slop is
        // surpassed during Main, and thus isn't checked again.
        assertThat(canDragDirections).hasSize(1)
    }

    // Verification of correctness of values passed to canDrag.

    @Test
    fun onPointerEvent_canDragCalledWithCorrectDirection() {
        onPointerEvent_canDragCalledWithCorrectDirection(
            -1f, 0f, arrayOf(Direction.LEFT)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            0f, -1f, arrayOf(Direction.UP)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            1f, 0f, arrayOf(Direction.RIGHT)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            0f, 1f, arrayOf(Direction.DOWN)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            -1f, -1f, arrayOf(Direction.LEFT, Direction.UP)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            -1f, 1f, arrayOf(Direction.LEFT, Direction.DOWN)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            1f, -1f, arrayOf(Direction.RIGHT, Direction.UP)
        )
        onPointerEvent_canDragCalledWithCorrectDirection(
            1f, 1f, arrayOf(Direction.RIGHT, Direction.DOWN)
        )
    }

    private fun onPointerEvent_canDragCalledWithCorrectDirection(
        dx: Float,
        dy: Float,
        expectedDirections: Array<Direction>
    ) {
        canDragDirections.clear()
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(Duration(milliseconds = 10), dx, dy)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Everything here is twice because DragGestureDetector checks during Main and Final.
        assertThat(canDragDirections).hasSize(expectedDirections.size * 2)
        expectedDirections.forEach { direction ->
            assertThat(canDragDirections.count { it == direction })
                .isEqualTo(2)
        }
    }

    // Verify the circumstances under which onTouchSlopExceeded should not be called.

    // TODO(b/129701831): This test assumes that if a pointer moves by slop in both x and y, we are
    // still under slop even though sqrt(slop^2 + slop^2) > slop.  This may be inaccurate and this
    // test may therefore need to be updated.
    @Test
    fun onPointerEvent_downMoveWithinSlop_onTouchSlopExceededNotCalled() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(
            Duration(milliseconds = 10),
            TestTouchSlop.toFloat(),
            TestTouchSlop.toFloat()
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_moveBeyondSlopInUnsupportedDirection_onTouchSlopExceededNotCalled() {
        val beyondSlop = TestTouchSlop + TinyNum
        canDragReturn = false

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(
            Duration(milliseconds = 10),
            beyondSlop,
            beyondSlop
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_moveBeyondSlopButConsumeUnder_onTouchSlopExceededNotCalled() {

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move =
            down.moveBy(10.milliseconds, TestTouchSlop + TinyNum, 0f)
                .apply { consumePositionChange(1f, 0f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_moveUnderToMainThenModOverInOppDir_onTouchSlopExceededNotCalled() {

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, TestTouchSlop.toFloat(), 0f)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(move),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        val move2 =
            move.apply { consumePositionChange(TestTouchSlop * 2f + TinyNum, 0f) }
        filter::onPointerEvent.invokeOverPass(
            pointerEventOf(move2),
            PointerEventPass.Final
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    // TODO(b/129701831): This test assumes that if a pointer moves by slop in both x and y, we are
    // still under slop even though sqrt(slop^2 + slop^2) > slop.  This may be inaccurate and this
    // test may therefore need to be updated.
    @Test
    fun onPointerEvent_moveAroundWithinSlop_onTouchSlopExceededNotCalled() {
        val slop = TestTouchSlop.toFloat()

        var change = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        // Go around the border of the touch slop area

        // To top left
        change = change.moveTo(10.milliseconds, -slop, -slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        // To bottom left
        change = change.moveTo(20.milliseconds, -slop, slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        // To bottom right
        change = change.moveTo(30.milliseconds, slop, slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        // To top right
        change = change.moveTo(40.milliseconds, slop, -slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        // Jump from corner to opposite corner and back

        // To bottom left
        change = change.moveTo(50.milliseconds, -slop, slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        // To top right
        change = change.moveTo(60.milliseconds, slop, -slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        // Move the other diagonal

        // To top left
        change = change.moveTo(70.milliseconds, -slop, -slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        // Jump from corner to opposite corner and back

        // To bottom right
        change = change.moveTo(80.milliseconds, slop, slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        // To top left
        change = change.moveTo(90.milliseconds, -slop, -slop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    // Verify the circumstances under which onTouchSlopExceeded should be called.

    @Test
    fun onPointerEvent_movePassedSlop_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(
            Duration(milliseconds = 100),
            beyondSlop,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_movePassedSlopIn2Events_onTouchSlopExceededCallOnce() {

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(
            Duration(milliseconds = 100),
            TestTouchSlop.toFloat(),
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))
        val move2 = down.moveBy(
            Duration(milliseconds = 100),
            1f,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move2))

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_passSlopThenInSlopAreaThenOut_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        var event = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))
        // Out of touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))
        // Back into touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, -beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))
        // Out of touch slop region again
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(event))

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_downConsumedMovePassedSlop_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0).apply { consumeDownChange() }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(Duration(milliseconds = 100), beyondSlop, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_beyondInUnsupportThenBeyondInSupport_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        var change = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        canDragReturn = false
        change = change.moveBy(
            Duration(milliseconds = 10),
            0f,
            beyondSlop
        )
        // Validity check that onTouchSlopExceeded has not been called.
        assertThat(onDragSlopExceededCallCount).isEqualTo(0)

        canDragReturn = true
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))
        change = change.moveBy(
            Duration(milliseconds = 10),
            0f,
            -beyondSlop
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change))

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_2PointsMoveInOpposite_onTouchSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(1)
        var pointer2 = down(2)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Act

        pointer1 = pointer1.moveBy(
            Duration(milliseconds = 100),
            beyondSlop,
            0f
        )
        pointer2 = pointer2.moveBy(
            Duration(milliseconds = 100),
            -beyondSlop,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_3PointsMoveAverage0_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        val pointers = arrayOf(down(0), down(1), down(2))
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(*pointers))

        // Act

        // These movements average to no movement.
        pointers[0] =
            pointers[0].moveBy(
                Duration(milliseconds = 100),
                beyondSlop * -1,
                beyondSlop * -1
            )
        pointers[1] =
            pointers[1].moveBy(
                Duration(milliseconds = 100),
                beyondSlop * 1,
                beyondSlop * -1
            )
        pointers[2] =
            pointers[2].moveBy(
                Duration(milliseconds = 100),
                0f,
                beyondSlop * 2
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(*pointers))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_2Points1MoveJustBeyondSlop_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(0)
        var pointer2 = down(1)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Act

        // These movements average to no movement.

        pointer1 =
            pointer1.moveBy(
                Duration(milliseconds = 100),
                0f,
                0f
            )
        pointer2 =
            pointer2.moveBy(
                Duration(milliseconds = 100),
                beyondSlop * -1,
                0f
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_2Points1MoveJustUnderTwiceSlop_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(0)
        var pointer2 = down(1)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Act

        // These movements average to no movement.

        pointer1 =
            pointer1.moveBy(
                Duration(milliseconds = 100),
                0f,
                0f
            )
        pointer2 =
            pointer2.moveBy(
                Duration(milliseconds = 100),
                beyondSlop * 2 - 1,
                0f
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerEvent_2Points1MoveToTwiceSlop_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(0)
        var pointer2 = down(1)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Act

        // These movements average to no movement.

        pointer1 =
            pointer1.moveBy(
                Duration(milliseconds = 100),
                0f,
                0f
            )
        pointer2 =
            pointer2.moveBy(
                Duration(milliseconds = 100),
                beyondSlop * 2,
                0f
            )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_1PointMovesBeyondSlopAndThenManyTimes_onDragSlopExceededCallOnce() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Act

        repeat(5) {
            pointer = pointer.moveBy(100.milliseconds, beyondSlop, beyondSlop)
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))
        }

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_1ModifiedToMoveBeyondSlopBeforeMain_onDragSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move =
            down.moveBy(10.milliseconds, 0f, 0f).apply { consumePositionChange(beyondSlop, 0f) }
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(move),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_1ModedToMoveBeyondSlopBeforeFinal_onDragSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, 0f, 0f)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(move),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )

        val moveConsumed = move.apply { consumePositionChange(beyondSlop, 0f) }
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(moveConsumed),
            PointerEventPass.Final
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_moveUnderToMainThenModOverToFinal_onDragSlopExceededCallOnce() {
        val halfSlop = TestTouchSlop / 2
        val restOfSlopAndBeyond = TestTouchSlop - halfSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, halfSlop.toFloat(), 0f)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(move),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )

        val moveConsumed = move.apply { consumePositionChange(-restOfSlopAndBeyond, 0f) }
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(moveConsumed),
            PointerEventPass.Final
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerEvent_moveBeyondSlopAllPassesUpToMain_onDragSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, beyondSlop, 0f)
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(move),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    // Verification that TouchSlopExceededGestureDetector does not consume any changes.

    @Test
    fun onPointerEvent_1Down_nothingConsumed() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        // Assert

        assertThat(down.consumed.downChange).isFalse()
        assertThat(down.consumed.positionChange.x).isEqualTo(0f)
        assertThat(down.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerEvent_1MoveUnderSlop_nothingConsumed() {

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, TestTouchSlop.toFloat(), TestTouchSlop.toFloat())
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Assert

        assertThat(move.consumed.downChange).isFalse()
        assertThat(move.consumed.positionChange.x).isEqualTo(0f)
        assertThat(move.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerEvent_1MoveUnderSlopThenUp_nothingConsumed() {

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, TestTouchSlop.toFloat(), TestTouchSlop.toFloat())
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        val up = move.up(20.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        // Assert

        assertThat(up.consumed.downChange).isFalse()
        assertThat(up.consumed.positionChange.x).isEqualTo(0f)
        assertThat(up.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerEvent_1MoveOverSlop_nothingConsumed() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, beyondSlop, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        // Assert

        assertThat(move.consumed.downChange).isFalse()
        assertThat(move.consumed.positionChange.x).isEqualTo(0f)
        assertThat(move.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerEvent_1MoveOverSlopThenUp_nothingConsumed() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

        val move = down.moveBy(10.milliseconds, beyondSlop, beyondSlop)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        val up = move.up(20.milliseconds)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))

        // Assert

        assertThat(up.consumed.downChange).isFalse()
        assertThat(up.consumed.positionChange.x).isEqualTo(0f)
        assertThat(up.consumed.positionChange.y).isEqualTo(0f)
    }

    // Verification that TouchSlopExceededGestureDetector resets after up correctly.

    @Test
    fun onPointerEvent_MoveBeyondUpDownMoveBeyond_onDragSlopExceededCalledTwice() {
        val beyondSlop = TestTouchSlop + TinyNum

        repeat(2) {
            val down = down(0)
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))

            val move = down.moveBy(10.milliseconds, beyondSlop, 0f)
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

            val up = move.up(20.milliseconds)
            filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(up))
        }

        assertThat(onDragSlopExceededCallCount).isEqualTo(2)
    }

    // Orientation tests

    // Tests that verify correct behavior when orientation is set.

    @Test
    fun onPointerInput_filterIsHorizontalMovementVertical_canDragNotCalled() {
        filter.setDraggableData(Orientation.Horizontal, canDrag)

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move1 = down.moveBy(Duration(milliseconds = 10), 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1))
        val move2 = down.moveBy(Duration(milliseconds = 10), 0f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move2))

        assertThat(canDragDirections).isEmpty()
    }

    @Test
    fun onPointerInput_filterIsVerticalMovementIsHorizontal_canDragNotCalled() {
        filter.setDraggableData(Orientation.Vertical, canDrag)

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move1 = down.moveBy(Duration(milliseconds = 10), 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1))
        val move2 = down.moveBy(Duration(milliseconds = 10), -1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move2))

        assertThat(canDragDirections).isEmpty()
    }

    @Test
    fun onPointerInput_filterIsHorizontalMovementHorizontal_canDragCalled() {
        filter.setDraggableData(Orientation.Horizontal, canDrag)

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move1 = down.moveBy(Duration(milliseconds = 10), 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1))
        val move2 = down.moveBy(Duration(milliseconds = 10), -1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move2))

        // 2 for each because canDrag is currently checked on both main and final
        assertThat(canDragDirections.filter { it == Direction.LEFT }).hasSize(2)
        assertThat(canDragDirections.filter { it == Direction.RIGHT }).hasSize(2)
    }

    @Test
    fun onPointerInput_filterIsVerticalMovementIsVertical_canDragCalled() {
        filter.setDraggableData(Orientation.Vertical, canDrag)

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move1 = down.moveBy(Duration(milliseconds = 10), 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1))
        val move2 = down.moveBy(Duration(milliseconds = 10), 0f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move2))

        // 2 for each because canDrag is currently checked on both main and final
        assertThat(canDragDirections.filter { it == Direction.UP }).hasSize(2)
        assertThat(canDragDirections.filter { it == Direction.DOWN }).hasSize(2)
    }

    @Test
    fun onPointerInput_filterIsHorizontalMoveLeftPassedSlop_onTouchSlopExceededCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Horizontal, -1, 0, 1)
    }

    @Test
    fun onPointerInput_filterIsHorizontalMoveUpPassedSlop_onTouchSlopExceededNotCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Horizontal, 0, -1, 0)
    }

    @Test
    fun onPointerInput_filterIsHorizontalMoveRightPassedSlop_onTouchSlopExceededCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Horizontal, 1, 0, 1)
    }

    @Test
    fun onPointerInput_filterIsHorizontalMoveDownPassedSlop_onTouchSlopExceededNotCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Horizontal, 0, 1, 0)
    }

    @Test
    fun onPointerInput_filterIsVerticalMoveLeftPassedSlop_onTouchSlopExceededNotCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Vertical, -1, 0, 0)
    }

    @Test
    fun onPointerInput_filterIsVerticalMoveUpPassedSlop_onTouchSlopExceededCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Vertical, 0, -1, 1)
    }

    @Test
    fun onPointerInput_filterIsVerticalMoveRightPassedSlop_onTouchSlopExceededNotCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Vertical, 1, 0, 0)
    }

    @Test
    fun onPointerInput_filterIsVerticalMoveDownPassedSlop_onTouchSlopExceededCalled() {
        onPointerInput_filterHasOrientationMovePassedSlop(Orientation.Vertical, 0, 1, 1)
    }

    private fun onPointerInput_filterHasOrientationMovePassedSlop(
        filterOrientation: Orientation,
        horizontalDirection: Int,
        verticalDirection: Int,
        expectecdOnDragSlopExceededCallCount: Int
    ) {
        val beyondSlop = TestTouchSlop + TinyNum

        filter.setDraggableData(filterOrientation, canDrag)

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(
            Duration(milliseconds = 100),
            horizontalDirection * beyondSlop,
            verticalDirection * beyondSlop
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(onDragSlopExceededCallCount).isEqualTo(expectecdOnDragSlopExceededCallCount)
    }

    @Test
    fun onPointerInput_filterHorizontalPointerVerticalMovesLeftPastSlop_callBackNotCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Horizontal, Orientation.Vertical, -1, 0, 0
        )
    }

    @Test
    fun onPointerInput_filterHorizontalPointerVerticalMovesRightPastSlop_callBackNotCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Horizontal, Orientation.Vertical, 1, 0, 0
        )
    }

    @Test
    fun onPointerInput_filterHorizontalPointerHorizontalMovesLeftPastSlop_callBackCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Horizontal, Orientation.Horizontal, -1, 0, 1
        )
    }

    @Test
    fun onPointerInput_filterHorizontalPointerHorizontallMovesRightPastSlop_callBackCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Horizontal, Orientation.Horizontal, 1, 0, 1
        )
    }

    @Test
    fun onPointerInput_filterVerticalPointerHorizontalMovesUpPastSlop_callBackNotCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Vertical, Orientation.Horizontal, 0, -1, 0
        )
    }

    @Test
    fun onPointerInput_filterVerticalPointerHorizontalMovesDownPastSlop_callBackNotCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Vertical, Orientation.Horizontal, 0, 1, 0
        )
    }

    @Test
    fun onPointerInput_filterVerticalPointerVerticalMovesUpPastSlop_callBackCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Vertical, Orientation.Vertical, 0, -1, 1
        )
    }

    @Test
    fun onPointerInput_filterVerticalPointerVerticalMovesDownPastSlop_callBackCalled() {
        onPointerInput_pointerIsLockedMovesPassedSlop(
            Orientation.Vertical, Orientation.Vertical, 0, 1, 1
        )
    }

    private fun onPointerInput_pointerIsLockedMovesPassedSlop(
        filterOrientation: Orientation,
        lockedOrientation: Orientation,
        horizontalDirection: Int,
        verticalDirection: Int,
        expectedOnDragSlopExceededCallCount: Int
    ) {
        val beyondSlop = TestTouchSlop + TinyNum

        filter.onInit(mock())
        val scrollOrientationLocker = InternalScrollOrientationLocker()
        filter::onCustomEvent.invokeOverAllPasses(
            ShareScrollOrientationLockerEvent(scrollOrientationLocker)
        )

        filter.setDraggableData(filterOrientation, canDrag)

        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        val move = down.moveBy(
            Duration(milliseconds = 100),
            horizontalDirection * beyondSlop,
            verticalDirection * beyondSlop
        )
        scrollOrientationLocker.attemptToLockPointers(listOf(move), lockedOrientation)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move))

        assertThat(onDragSlopExceededCallCount).isEqualTo(expectedOnDragSlopExceededCallCount)
    }

    // Verification that cancellation behavior is correct.

    @Test
    fun onCancel_underSlopCancelUnderSlop_onDragSlopExceededNotCalled() {
        val underSlop = TestTouchSlop - TinyNum

        // Arrange

        var pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        pointer = pointer.moveTo(
            10.milliseconds,
            underSlop,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Act

        filter.onCancel()

        pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        pointer = pointer.moveTo(
            10.milliseconds,
            underSlop,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onCancel_pastSlopCancelPastSlop_onScaleSlopExceededCalledTwice() {
        val overSlop = TestTouchSlop + TinyNum

        // Arrange

        var pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        pointer = pointer.moveTo(
            10.milliseconds,
            overSlop,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Act

        filter.onCancel()

        pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        pointer = pointer.moveTo(
            10.milliseconds,
            overSlop,
            0f
        )
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer))

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(2)
    }
}