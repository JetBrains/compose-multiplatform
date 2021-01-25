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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.invokeOverAllPasses
import androidx.compose.ui.input.pointer.invokeOverPasses
import androidx.compose.ui.input.pointer.moveBy
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.input.pointer.up
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following tests.
// Test for cases where things are reset when last pointer goes up
// Verify methods called during Main and Final
// Verify correct behavior when distance is consumed at different moments between passes.
// Verify correct behavior with no canStartScaling callback.

@RunWith(JUnit4::class)
class RawScaleGestureFilterTest {

    private lateinit var filter: RawScaleGestureFilter
    private lateinit var scaleObserver: MockScaleObserver
    private lateinit var log: MutableList<LogItem>
    private var scaleStartBlocked = true

    @Before
    fun setup() {
        log = mutableListOf()
        scaleObserver = MockScaleObserver(log)
        filter = RawScaleGestureFilter()
        filter.canStartScaling = { !scaleStartBlocked }
        filter.scaleObserver = scaleObserver
    }

    // Verify the circumstances under which onStart/onScale should not be called.

    @Test
    fun onPointerEvent_blocked_onStartAndOnScaleNotCalled() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(1, x = -1f, y = 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        pointer1 = pointer1.moveBy(10, 1f, 1f)
        pointer2 = pointer1.moveBy(10, -1f, -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

    @Test
    fun onPointerEvent_noMove_onStartAndOnScaleNotCalled() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(1, x = -1f, y = 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveBy(10, 0f, 0f)
        pointer2 = pointer1.moveBy(10, 0f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

    @Test
    fun onPointerEvent_1PointerExistsAndMoves_onStartAndOnScaleNotCalled() {

        val down1 = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down1))
        scaleStartBlocked = false

        val move1 = down1.moveBy(10, 1f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(move1))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

/**
     * 2 points move, but the distance between them stays the same (and thus, the average
     * distance to the average point is also the same), so no scaling occurs.
     *
     * The pointers start here:
     *
     * |
     * |
     * |    *
     * |
     * |*
     *  - - - - -
     *
     *  Then end up here:
     *
     * |    *
     * |
     * |        *
     * |
     * |
     *  - - - - -
     */

    @Test
    fun onPointerEvent_2PointsAvgDistanceToCenterDoesNotChange_onStartAndOnScaleNotCalled() {

        var pointer1 = down(1, x = 1f, y = 1f)
        var pointer2 = down(0, x = 3f, y = 3f)

        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        // The pointers move and rotate, but the average distance to the center doesn't change, so
        // no scaling occurs.
        pointer1 = pointer1.moveTo(10, 3f, 5f)
        pointer2 = pointer2.moveTo(10, 5f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

/**
     * Here, 3 points move, but their average distance to the average point stays the same, so no
     * scaling occurs.
     *
     * The pointers start here:
     *
     * |
     * |      *
     * |
     * |*
     * |  *
     *  - - - - -
     *
     *  Then end up here:
     *
     * |  *
     * |
     * |        *
     * |      *
     * |
     *  - - - - -
     */

    @Test
    fun onPointerEvent_3PointsAvgDistanceToCenterDoesNotChange_onStartAndOnScaleNotCalled() {

        // Arrange

        var pointer1 = down(0, x = 1f, y = 2f)
        var pointer2 = down(1, x = 2f, y = 1f)
        var pointer3 = down(2, x = 4f, y = 4f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))
        scaleStartBlocked = false

        // Act

        // The pointers move and rotate, but the average distance to the center doesn't change, so
        // no scaling occurs.
        pointer1 = pointer1.moveTo(10, 2f, 5f)
        pointer2 = pointer2.moveTo(10, 4f, 2f)
        pointer3 = pointer3.moveTo(10, 5f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))

        // Assert
        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

/**
     * Here, 4 points move and rotate, but their average distance to the average point stays the
     * same, so no scaling occurs.
     *
     * The pointers start here:
     *
     * |      *
     * |
     * |  *       *
     * |
     * |      *
     *  - - - - - - -
     *
     *  Then end up here:
     *
     * |
     * |      *
     * |*           *
     * |      *
     * |
     *  - - - - - - -
     */

    @Test
    fun onPointerEvent_4PointsAvgDistanceToCenterDoesNotChange_onStartAndOnScaleNotCalled() {

        // Arrange

        var pointer1 = down(0, x = 2f, y = 3f)
        var pointer2 = down(1, x = 4f, y = 1f)
        var pointer3 = down(2, x = 4f, y = 5f)
        var pointer4 = down(3, x = 6f, y = 3f)
        filter::onPointerEvent
            .invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3, pointer4))
        scaleStartBlocked = false

        // Act

        // The pointers move and rotate, but the average distance to the center doesn't change, so
        // no scaling occurs.
        pointer1 = pointer1.moveTo(10, 1f, 2f)
        pointer2 = pointer2.moveTo(10, 4f, 1f)
        pointer3 = pointer3.moveTo(10, 4f, 3f)
        pointer4 = pointer4.moveTo(10, 7f, 2f)
        filter::onPointerEvent
            .invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3, pointer4))

        // Assert
        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

/**
     * Here, 4 points move and rotate, but their average distance to the average point stays the
     * same, so no scaling occurs.
     *
     * The pointers start here:
     *
     * |
     * |
     * |
     * |  *
     * |*
     *  - - - - - - -
     *
     *  Then end up here (the points in parentheses are before consumption):
     *
     * |
     * |    *
     * |      *
     * | (*)
     * |       (*)
     *  - - - - - - -
     */

    @Test
    fun onPointerEvent_movementConsumedSoAvgDistanceUnchanged_onStartAndOnScaleNotCalled() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 =
            pointer1.moveTo(10, 2f, 2f).apply { consumePositionChange(-1f, -2f) }
        pointer2 =
            pointer2.moveTo(10, 5f, 1f).apply { consumePositionChange(1f, -2f) }
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onScale" }).isEmpty()
    }

    // Verify the circumstances under which onStart/onScale should be called.

    @Test
    fun onPointerEvent_2Pointers1MovesOnX_onStartAndOnScaleCalledOnce() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onScale" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_2Pointers1MovesOnY_onStartAndOnScaleCalledOnce() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveBy(10, dx = 0f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = -1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onScale" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_2Pointers0Move1ConsumedOnX_onStartAndOnScaleCalledOnce() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 =
            pointer1.moveBy(10, dx = 0f, dy = 0f).apply {
                consumePositionChange(-1f, 0f)
            }
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onScale" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_2Pointers0Move1ConsumedOnY_onStartAndOnScaleCalledOnce() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 =
            pointer1
                .moveBy(10, dx = 0f, dy = 0f)
                .apply { consumePositionChange(0f, -1f) }
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onScale" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_2Pointers1MovesTheMovesAgain_onStartOnlyCalledOnce() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
    }

    // onScale called with correct values verification.

    @Test
    fun onPointerEvent_2PointersIncreaseDistanceOnXBy50Percent_onScaleCalledWith150Percent() {
        var pointer1 = down(0, x = 0f, y = 0f)
        var pointer2 = down(0, x = 2f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveTo(10, 0f, 0f)
        pointer2 = pointer2.moveTo(10, 3f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        val onScaleLog = log.filter { it.methodName == "onScale" }
        assertThat(onScaleLog).hasSize(1)
        assertThat(onScaleLog[0].percentageChanged).isEqualTo(1.5f)
    }

    @Test
    fun onPointerEvent_2PointersIncreaseDistanceOnYBy50Percent_onScaleCalledWith150Percent() {
        var pointer1 = down(0, x = 0f, y = 0f)
        var pointer2 = down(0, x = 0f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveTo(10, 0f, 0f)
        pointer2 = pointer2.moveTo(10, 0f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        val onScaleLog = log.filter { it.methodName == "onScale" }
        assertThat(onScaleLog).hasSize(1)
        assertThat(onScaleLog[0].percentageChanged).isEqualTo(1.5f)
    }

    @Test
    fun onPointerEvent_2PointersDecreaseDistanceOnXBy50Percent_onScaleCalledWith50Percent() {
        var pointer1 = down(0, x = 0f, y = 0f)
        var pointer2 = down(0, x = 2f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveTo(10, 0f, 0f)
        pointer2 = pointer2.moveTo(10, 1f, 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        val onScaleLog = log.filter { it.methodName == "onScale" }
        assertThat(onScaleLog).hasSize(1)
        assertThat(onScaleLog[0].percentageChanged).isEqualTo(.5f)
    }

    @Test
    fun onPointerEvent_2PointersDecreaseDistanceOnYBy50Percent_onScaleCalledWith50Percent() {
        var pointer1 = down(0, x = 0f, y = 0f)
        var pointer2 = down(0, x = 0f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveTo(10, 0f, 0f)
        pointer2 = pointer2.moveTo(10, 0f, 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        val onScaleLog = log.filter { it.methodName == "onScale" }
        assertThat(onScaleLog).hasSize(1)
        assertThat(onScaleLog[0].percentageChanged).isEqualTo(.5f)
    }

    @Test
    fun onPointerEvent_2PointersIncDistOnBothAxisBy300Percent_onScaleCalledWith400Percent() {
        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveTo(10, 0f, 0f)
        pointer2 = pointer2.moveTo(10, 4f, 4f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        val onScaleLog = log.filter { it.methodName == "onScale" }
        assertThat(onScaleLog).hasSize(1)
        assertThat(onScaleLog[0].percentageChanged).isEqualTo(4f)
    }

    @Test
    fun onPointerEvent_2PointersDecDistOnBothAxisBy75Percent_onScaleCalledWith25Percent() {
        var pointer1 = down(0, x = 0f, y = 0f)
        var pointer2 = down(0, x = 4f, y = 4f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false

        pointer1 = pointer1.moveTo(10, 1f, 1f)
        pointer2 = pointer2.moveTo(10, 2f, 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        val onScaleLog = log.filter { it.methodName == "onScale" }
        assertThat(onScaleLog).hasSize(1)
        assertThat(onScaleLog[0].percentageChanged).isEqualTo(.25f)
    }

    // onStop not called verification

    @Test
    fun onPointerEvent_blocked2PointersScaleThenOneUp_onStopNotCalled() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        pointer1 = pointer1.up(20)
        pointer2 = pointer2.moveBy(0, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun onPointerEvent_1PointerDownMoveUp_onStopNotCalled() {

        var pointer1 = down(0, x = 1f, y = 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1))
        scaleStartBlocked = false

        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1))

        pointer1 = pointer1.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun onPointerEvent_3PointersScaleThan1Up_onStopNotCalled() {

        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(1, x = 1f, y = 2f)
        var pointer3 = down(2, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))
        scaleStartBlocked = false

        pointer1 = pointer1.moveBy(10, dx = -1f, dy = -1f)
        pointer2 = pointer2.moveBy(10, dx = -1f, dy = 1f)
        pointer3 = pointer3.moveBy(10, dx = 1f, dy = 1f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))

        pointer1 = pointer1.up(20)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        pointer3 = pointer3.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2, pointer3))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    // onStop called verification

    @Test
    fun onPointerEvent_unblocked2DownMove2Up_onStopCalledOnce() {
        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(1)
    }

    @Test
    fun onPointerEvent_unblocked2DownMove1Up_onStopNotCalled() {
        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.moveTo(20, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(log.filter { it.methodName == "onStop" }).isEmpty()
    }

    // Verification that callbacks occur in the correct order

    @Test
    fun onPointerEvent_unblocked2DownMove2Up_callbacksCalledInCorrectOrder() {
        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(log).hasSize(3)
        assertThat(log[0].methodName).isEqualTo("onStart")
        assertThat(log[1].methodName).isEqualTo("onScale")
        assertThat(log[2].methodName).isEqualTo("onStop")
    }

    // Verification about what events are, or aren't consumed.

    @Test
    fun onPointerEvent_1down_downNotConsumed() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        assertThat(down.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerEvent_2Down_downNotConsumed() {
        val down1 = down(0, x = 1f, y = 1f)
        val down2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down1, down2))

        assertThat(down1.consumed.downChange).isFalse()
        assertThat(down2.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerEvent_blocked2DownMove_distanceChangeNotConsumed() {
        scaleObserver.resultingScaleChange = 3f

        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.anyPositionChangeConsumed()).isFalse()
        assertThat(change2.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerEvent_unblocked2DownMoveCallBackDoesNotConsume_distanceChangeNotConsumed() {
        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.anyPositionChangeConsumed()).isFalse()
        assertThat(change2.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerEvent_unblockedScaleOccursDefaultOnScale_distanceChangeNotConsumed() {
        scaleObserver.resultingScaleChange = null

        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.anyPositionChangeConsumed()).isFalse()
        assertThat(change2.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerEvent_onlyScaleUpXPartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 2f, y = 0f)
        var change2 = down(1, x = 4f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 6f, 0f)
        scaleObserver.resultingScaleChange = 2f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(-1f, 0f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(1f, 0f))
    }

    @Test
    fun onPointerEvent_onlyScaleUpYPartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 0f, y = 2f)
        var change2 = down(1, x = 0f, y = 4f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 0f, 6f)
        scaleObserver.resultingScaleChange = 2f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(0f, -1f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(0f, 1f))
    }

    @Test
    fun onPointerEvent_onlyScaleUpXYFullyConsumed_distancesConsumedByCorrectAmount() {
        scaleObserver.resultingScaleChange = 3f

        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(-1f, -1f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(1f, 1f))
    }

    @Test
    fun onPointerEvent_onlyScaleDownXPartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 0f, y = 0f)
        var change2 = down(1, x = 8f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 2f, 0f)
        change2 = change2.moveTo(10, 6f, 0f)
        scaleObserver.resultingScaleChange = .75f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(1f, 0f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(-1f, 0f))
    }

    @Test
    fun onPointerEvent_onlyScaleDownYPartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 0f, y = 0f)
        var change2 = down(1, x = 0f, y = 8f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, 2f)
        change2 = change2.moveTo(10, 0f, 6f)
        scaleObserver.resultingScaleChange = .75f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(0f, 1f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(0f, -1f))
    }

    @Test
    fun onPointerEvent_onlyScaleDownXYFullyConsumed_distancesConsumedByCorrectAmount() {
        scaleObserver.resultingScaleChange = .5f

        var change1 = down(0, x = 0f, y = 0f)
        var change2 = down(1, x = 8f, y = 8f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 2f, 2f)
        change2 = change2.moveTo(10, 6f, 6f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(2f, 2f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(-2f, -2f))
    }

    @Test
    fun onPointerEvent_scaleUpXTranslatePartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 2f, y = 0f)
        var change2 = down(1, x = 4f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 2f, 0f)
        change2 = change2.moveTo(10, 8f, 0f)
        scaleObserver.resultingScaleChange = 2f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2), listOf(PointerEventPass.Final)
        )

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(-1f, 0f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(1f, 0f))
    }

    @Test
    fun onPointerEvent_scaleUpYTranslatePartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 0f, y = 2f)
        var change2 = down(1, x = 0f, y = 4f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, 2f)
        change2 = change2.moveTo(10, 0f, 8f)
        scaleObserver.resultingScaleChange = 2f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(0f, -1f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(0f, 1f))
    }

    @Test
    fun onPointerEvent_scaleDownXTranslatePartConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 0f, y = 0f)
        var change2 = down(1, x = 8f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 4f, 0f)
        scaleObserver.resultingScaleChange = .75f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(1f, 0f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(-1f, 0f))
    }

    @Test
    fun onPointerEvent_scaleDownYTranslatePartConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = 0f, y = 0f)
        var change2 = down(1, x = 0f, y = 8f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 0f, 4f)
        scaleObserver.resultingScaleChange = .75f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(0f, 1f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(0f, -1f))
    }

    @Test
    fun onPointerEvent_scaleUpRotatePartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = -1f, y = 0f)
        var change2 = down(1, x = 1f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, -3f)
        change2 = change2.moveTo(10, 0f, 3f)
        scaleObserver.resultingScaleChange = 2f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(.5f, -1.5f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(-.5f, 1.5f))
    }

    @Test
    fun onPointerEvent_scaleDownRotatePartiallyConsumed_distancesConsumedByCorrectAmount() {

        // Act

        var change1 = down(0, x = -4f, y = 0f)
        var change2 = down(1, x = 4f, y = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false

        change1 = change1.moveTo(10, 0f, -2f)
        change2 = change2.moveTo(10, 0f, 2f)
        scaleObserver.resultingScaleChange = .75f
        filter::onPointerEvent.invokeOverPasses(
            pointerEventOf(change1, change2),
            listOf(
                PointerEventPass.Initial,
                PointerEventPass.Main
            )
        )
        scaleObserver.resultingScaleChange = 1f
        filter::onPointerEvent
            .invokeOverPasses(pointerEventOf(change1, change2), listOf(PointerEventPass.Final))

        // Assert

        assertThat(change1.consumed.positionChange).isEqualTo(Offset(2f, -1f))
        assertThat(change2.consumed.positionChange).isEqualTo(Offset(-2f, 1f))
    }

    @Test
    fun onPointerEvent_blocked2DownMoveUp_upChangeNotConsumed() {
        scaleObserver.resultingScaleChange = 1f

        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.consumed.downChange).isEqualTo(false)
        assertThat(change2.consumed.downChange).isEqualTo(false)
    }

    @Test
    fun onPointerEvent_unblocked2DownUp_upChangeNotConsumed() {
        scaleObserver.resultingScaleChange = 1f

        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.up(20)
        change2 = change2.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.consumed.downChange).isEqualTo(false)
        assertThat(change2.consumed.downChange).isEqualTo(false)
    }

    @Test
    fun onPointerEvent_scale1Up_upChangeConsumed() {
        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.moveTo(20, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.consumed.downChange).isEqualTo(true)
    }

    @Test
    fun onPointerEvent_scale2Up_onStopConsumesUp() {
        var change1 = down(0, x = 1f, y = 1f)
        var change2 = down(1, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        scaleStartBlocked = false
        change1 = change1.moveTo(10, 0f, 0f)
        change2 = change2.moveTo(10, 3f, 3f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))
        change1 = change1.up(20)
        change2 = change2.up(20)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(change1, change2))

        assertThat(change1.consumed.downChange).isEqualTo(true)
        assertThat(change2.consumed.downChange).isEqualTo(true)
    }

    // Tests that verify when onCancel should not be called.

    @Test
    fun onCancel_downCancel_onCancelNotCalled() {
        val down = down(0)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(down))
        scaleStartBlocked = false
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).isEmpty()
    }

    @Test
    fun onCancel_blockedDownMoveCancel_onCancelNotCalled() {
        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = true
        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).isEmpty()
    }

    // Tests that verify when onCancel should be called.

    @Test
    fun onCancel_downMoveCancel_onCancelCalledOnce() {
        var pointer1 = down(0, x = 1f, y = 1f)
        var pointer2 = down(0, x = 2f, y = 2f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        scaleStartBlocked = false
        pointer1 = pointer1.moveBy(10, dx = 1f, dy = 0f)
        pointer2 = pointer2.moveBy(10, dx = 0f, dy = 0f)
        filter::onPointerEvent.invokeOverAllPasses(pointerEventOf(pointer1, pointer2))
        filter.onCancel()

        assertThat(log.count { it.methodName == "onCancel" }).isEqualTo(1)
    }

    data class LogItem(
        val methodName: String,
        val percentageChanged: Float? = null
    )

    class MockScaleObserver(
        private val log: MutableList<LogItem>,
        var resultingScaleChange: Float? = null
    ) : RawScaleObserver {

        override fun onStart() {
            log.add(LogItem("onStart"))
            super.onStart()
        }

        override fun onScale(scaleFactor: Float): Float {
            log.add(LogItem("onScale", scaleFactor))
            return resultingScaleChange ?: super.onScale(scaleFactor)
        }

        override fun onStop() {
            log.add(LogItem("onStop"))
            super.onStop()
        }

        override fun onCancel() {
            log.add(LogItem("onCancel"))
            super.onCancel()
        }
    }
}
