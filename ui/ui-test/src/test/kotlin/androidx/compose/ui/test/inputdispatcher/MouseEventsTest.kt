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

package androidx.compose.ui.test.inputdispatcher

import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.AXIS_HSCROLL
import android.view.MotionEvent.AXIS_VSCROLL
import android.view.MotionEvent.BUTTON_PRIMARY
import android.view.MotionEvent.BUTTON_SECONDARY
import android.view.MotionEvent.BUTTON_TERTIARY
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verifyMouseEvent
import androidx.compose.ui.test.util.verifyTouchEvent
import androidx.compose.ui.test.util.verifyTouchPointer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests if [AndroidInputDispatcher.enqueueMousePress] and friends work.
 */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
@OptIn(ExperimentalTestApi::class)
class MouseEventsTest : InputDispatcherTest() {
    companion object {
        // Positions
        private val position1 = Offset(1f, 1f)
        private val position2 = Offset(2f, 2f)
        private val position3 = Offset(3f, 3f)
        private val position4 = Offset(4f, 4f)
        private val positionMin1 = Offset(-1f, -1f)
        private val positionMin2 = Offset(-2f, -2f)
    }

    @Test
    fun oneButton_primary() {
        oneButton(MouseButton.Primary, BUTTON_PRIMARY)
    }

    @Test
    fun oneButton_secondary() {
        oneButton(MouseButton.Secondary, BUTTON_SECONDARY)
    }

    @Test
    fun oneButton_tertiary() {
        oneButton(MouseButton.Tertiary, BUTTON_TERTIARY)
    }

    private fun oneButton(mouseButton: MouseButton, expectedButtonState: Int) {
        // Scenario:
        // move mouse
        // press button
        // move mouse
        // release button
        // move mouse

        var expectedEvents = 0
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMouseMove(position1)
        subject.verifyMousePosition(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueMousePress(mouseButton.buttonId)
        expectedEvents += 3 // exit + down + press
        subject.advanceEventTime()
        subject.enqueueMouseMove(position2)
        subject.verifyMousePosition(position2)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueMouseRelease(mouseButton.buttonId)
        expectedEvents += 4 // release + up + enter + hover
        subject.advanceEventTime()
        subject.enqueueMouseMove(position3)
        subject.verifyMousePosition(position3)
        expectedEvents += 1 // hover
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position1, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position1, 0)
        }

        // exit + down + press
        t = 0L // down resets downTime
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyMouseEvent(ACTION_HOVER_EXIT, t, position1, expectedButtonState)
            downEvent.verifyMouseEvent(ACTION_DOWN, t, position1, expectedButtonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, position1, expectedButtonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position2, expectedButtonState)
        }

        // release + up + enter + hover
        t += eventPeriodMillis
        events.removeFirst(4).let { (releaseEvent, upEvent, enterEvent, hoverEvent) ->
            releaseEvent.verifyMouseEvent(ACTION_BUTTON_RELEASE, t, position2, 0)
            upEvent.verifyMouseEvent(ACTION_UP, t, position2, 0)
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position2, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }

        // hover
        t += eventPeriodMillis
        events.removeFirst(1).let { (hoverEvent) ->
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position3, 0)
        }
    }

    @Test
    fun oneButton_cancel() {
        // Scenario:
        // press primary button
        // cancel mouse gesture

        var expectedEvents = 0
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueMouseCancel()
        expectedEvents += 1 // cancel
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down + press
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyMouseEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // cancel
        t += eventPeriodMillis
        buttonState = 0
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyMouseEvent(ACTION_CANCEL, t, Offset.Zero, buttonState)
        }
    }

    @Test
    fun hoverOutOfRootBounds() {
        // Scenario:
        // move mouse within bounds
        // move mouse out of bounds
        // move mouse out of bounds again
        // move mouse into bounds

        var expectedEvents = 0
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMouseMove(position1)
        subject.verifyMousePosition(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueMouseMove(positionMin1)
        subject.verifyMousePosition(positionMin1)
        expectedEvents += 1 // exit (suppressed move)
        subject.advanceEventTime()
        subject.enqueueMouseMove(positionMin2)
        subject.verifyMousePosition(positionMin2)
        expectedEvents += 0 // nothing (suppressed move)
        subject.advanceEventTime()
        subject.enqueueMouseMove(position2)
        subject.verifyMousePosition(position2)
        expectedEvents += 2 // enter + hover
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position1, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position1, 0)
        }

        // exit (suppressed move)
        t += eventPeriodMillis
        events.removeFirst(1).let { (exitEvent) ->
            exitEvent.verifyMouseEvent(ACTION_HOVER_EXIT, t, positionMin1, 0)
        }

        // nothing (suppressed move)
        t += eventPeriodMillis

        // enter + hover
        t += eventPeriodMillis
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position2, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }
    }

    @Test
    fun moveOutOfRootBounds() {
        // Scenario:
        // press primary button within bounds
        // move mouse out of bounds
        // press secondary button
        // release secondary button
        // release primary button

        var expectedEvents = 0
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueMouseMove(positionMin1)
        subject.verifyMousePosition(positionMin1)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueMousePress(MouseButton.Secondary.buttonId)
        expectedEvents += 1 // move (suppressed press)
        subject.advanceEventTime()
        subject.enqueueMouseRelease(MouseButton.Secondary.buttonId)
        expectedEvents += 1 // move (suppressed release)
        subject.advanceEventTime()
        subject.enqueueMouseRelease(MouseButton.Primary.buttonId)
        expectedEvents += 1 // up (suppressed release)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyMouseEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, positionMin1, buttonState)
        }

        // move (suppressed press)
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY or BUTTON_SECONDARY
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, positionMin1, buttonState)
        }

        // move (suppressed release)
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, positionMin1, buttonState)
        }

        // up (suppressed release)
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_UP, t, positionMin1, 0)
        }
    }

    @Test
    fun twoButtons() {
        // Scenario:
        // press primary button
        // move mouse
        // press secondary button
        // move mouse
        // release primary button
        // move mouse
        // release secondary button
        // move mouse
        // press tertiary button

        var expectedEvents = 0
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueMouseMove(position1)
        subject.verifyMousePosition(position1)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueMousePress(MouseButton.Secondary.buttonId)
        expectedEvents += 2 // move + press
        subject.advanceEventTime()
        subject.enqueueMouseMove(position2)
        subject.verifyMousePosition(position2)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueMouseRelease(MouseButton.Primary.buttonId)
        expectedEvents += 2 // release + move
        subject.advanceEventTime()
        subject.enqueueMouseMove(position3)
        subject.verifyMousePosition(position3)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueMouseRelease(MouseButton.Secondary.buttonId)
        expectedEvents += 4 // release + up + enter + hover
        subject.advanceEventTime()
        subject.enqueueMouseMove(position4)
        subject.verifyMousePosition(position4)
        expectedEvents += 1 // hover
        subject.advanceEventTime()
        subject.enqueueMousePress(MouseButton.Tertiary.buttonId)
        expectedEvents += 3 // exit + down + press
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down + press
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyMouseEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position1, buttonState)
        }

        // move + press
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY or BUTTON_SECONDARY
        events.removeFirst(2).let { (moveEvent, pressEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position1, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, position1, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position2, buttonState)
        }

        // release + move
        t += eventPeriodMillis
        buttonState = BUTTON_SECONDARY
        events.removeFirst(2).let { (releaseEvent, moveEvent) ->
            releaseEvent.verifyMouseEvent(ACTION_BUTTON_RELEASE, t, position2, buttonState)
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position2, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position3, buttonState)
        }

        // release + up + enter + hover
        t += eventPeriodMillis
        events.removeFirst(4).let { (releaseEvent, upEvent, enterEvent, hoverEvent) ->
            releaseEvent.verifyMouseEvent(ACTION_BUTTON_RELEASE, t, position3, 0)
            upEvent.verifyMouseEvent(ACTION_UP, t, position3, 0)
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position3, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position3, 0)
        }

        // hover
        t += eventPeriodMillis
        events.removeFirst(1).let { (hoverEvent) ->
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position4, 0)
        }

        // exit + down + press
        t = 0L // down resets downTime
        buttonState = BUTTON_TERTIARY
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyMouseEvent(ACTION_HOVER_EXIT, t, position4, buttonState)
            downEvent.verifyMouseEvent(ACTION_DOWN, t, position4, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, position4, buttonState)
        }
    }

    @Test
    fun manualEnterExit() {
        // Scenario:
        // send hover enter
        // move mouse
        // send hover exit

        var expectedEvents = 0
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMouseEnter(position1)
        subject.verifyMousePosition(position1)
        expectedEvents += 1 // enter
        subject.advanceEventTime()
        subject.enqueueMouseMove(position2)
        subject.verifyMousePosition(position2)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueMouseExit(position3)
        subject.verifyMousePosition(position3)
        expectedEvents += 1 // exit
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter
        var t = 0L
        events.removeFirst(1).let { (enterEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position1, 0)
        }

        // hover
        t += eventPeriodMillis
        events.removeFirst(1).let { (hoverEvent) ->
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }

        // exit
        t += eventPeriodMillis
        events.removeFirst(1).let { (exitEvent) ->
            exitEvent.verifyMouseEvent(ACTION_HOVER_EXIT, t, position3, 0)
        }
    }

    @Test
    fun scroll_vertically() {
        scrollTest(ScrollWheel.Vertical) { Pair(AXIS_VSCROLL, -it) }
    }

    @Test
    fun scroll_horizontally() {
        scrollTest(ScrollWheel.Horizontal) { Pair(AXIS_HSCROLL, it) }
    }

    private fun scrollTest(scrollWheel: ScrollWheel, scrollAxis: (Float) -> Pair<Int, Float>) {
        // Scenario:
        // move mouse
        // scroll by 1f
        // press primary button
        // scroll by 2f
        // release primary button
        // scroll by 3f

        var expectedEvents = 0
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMouseMove(position1)
        subject.verifyMousePosition(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueMouseScroll(1f, scrollWheel)
        expectedEvents += 2 // hover + scroll
        subject.advanceEventTime()
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        expectedEvents += 3 // exit + down + press
        subject.advanceEventTime()
        subject.enqueueMouseScroll(2f, scrollWheel)
        expectedEvents += 2 // move + scroll
        subject.advanceEventTime()
        subject.enqueueMouseRelease(MouseButton.Primary.buttonId)
        expectedEvents += 4 // release + up + enter + hover
        subject.advanceEventTime()
        subject.enqueueMouseScroll(3f, scrollWheel)
        expectedEvents += 2 // hover + scroll
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        var buttonState = 0
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position1, buttonState)
        }

        // hover + scroll
        t += eventPeriodMillis
        events.removeFirst(2).let { (hoverEvent, scrollEvent) ->
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position1, buttonState)
            scrollEvent.verifyMouseEvent(ACTION_SCROLL, t, position1, buttonState, scrollAxis(1f))
        }

        // exit + down + press
        t = 0L // down resets downTime
        buttonState = BUTTON_PRIMARY
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyMouseEvent(ACTION_HOVER_EXIT, t, position1, buttonState)
            downEvent.verifyMouseEvent(ACTION_DOWN, t, position1, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, position1, buttonState)
        }

        // move + scroll
        t += eventPeriodMillis
        events.removeFirst(2).let { (moveEvent, scrollEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, position1, buttonState)
            scrollEvent.verifyMouseEvent(ACTION_SCROLL, t, position1, buttonState, scrollAxis(2f))
        }

        // release + up + enter + hover
        t += eventPeriodMillis
        buttonState = 0
        events.removeFirst(4).let { (releaseEvent, upEvent, enterEvent, hoverEvent) ->
            releaseEvent.verifyMouseEvent(ACTION_BUTTON_RELEASE, t, position1, buttonState)
            upEvent.verifyMouseEvent(ACTION_UP, t, position1, buttonState)
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position1, buttonState)
        }

        // hover + scroll
        t += eventPeriodMillis
        events.removeFirst(2).let { (hoverEvent, scrollEvent) ->
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position1, buttonState)
            scrollEvent.verifyMouseEvent(ACTION_SCROLL, t, position1, buttonState, scrollAxis(3f))
        }
    }

    @Test
    fun twoButtons_cancel() {
        // Scenario:
        // press primary button
        // press secondary button
        // cancel

        var expectedEvents = 0
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueMousePress(MouseButton.Secondary.buttonId)
        expectedEvents += 2 // move + press
        subject.advanceEventTime()
        subject.enqueueMouseCancel()
        expectedEvents += 1 // cancel
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down + press
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyMouseEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // move + press
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY or BUTTON_SECONDARY
        events.removeFirst(2).let { (moveEvent, pressEvent) ->
            moveEvent.verifyMouseEvent(ACTION_MOVE, t, Offset.Zero, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // cancel
        t += eventPeriodMillis
        buttonState = 0
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyMouseEvent(ACTION_CANCEL, t, Offset.Zero, buttonState)
        }
    }

    @Test
    fun enqueueMousePress_interruptsTouch() {
        // Scenario:
        // finger 1 down
        // press primary button

        var expectedEvents = 0
        subject.enqueueTouchDown(1, position1)
        expectedEvents += 1 // down
        subject.advanceEventTime()
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        expectedEvents += 3 // cancel + down + press
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down
        var t = 0L
        events.removeFirst(1).let { (downEvent) ->
            downEvent.verifyTouchEvent(1, ACTION_DOWN, 0, t)
            downEvent.verifyTouchPointer(1, position1)
        }

        // cancel
        t += eventPeriodMillis
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTouchEvent(1, ACTION_CANCEL, 0, t)
            cancelEvent.verifyTouchPointer(1, position1)
        }

        // down + press
        t = 0L // down resets downTime
        val buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyMouseEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyMouseEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }
    }

    @Test
    fun enqueueMouseMove_interruptsTouch() {
        // Scenario:
        // finger 1 down
        // move mouse

        var expectedEvents = 0
        subject.enqueueTouchDown(1, position1)
        expectedEvents += 1 // down
        subject.advanceEventTime()
        subject.enqueueMouseMove(position2)
        expectedEvents += 3 // cancel + enter + hover
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down
        var t = 0L
        events.removeFirst(1).let { (downEvent) ->
            downEvent.verifyTouchEvent(1, ACTION_DOWN, 0, t)
            downEvent.verifyTouchPointer(1, position1)
        }

        // cancel
        t += eventPeriodMillis
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTouchEvent(1, ACTION_CANCEL, 0, t)
            cancelEvent.verifyTouchPointer(1, position1)
        }

        // enter + hover
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, position2, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }
    }

    @Test
    fun enqueueMouseScroll_interruptsTouch() {
        // Scenario:
        // finger 1 down
        // scroll by 1f

        var expectedEvents = 0
        subject.enqueueTouchDown(1, position1)
        expectedEvents += 1 // down
        subject.advanceEventTime()
        subject.enqueueMouseScroll(1f, ScrollWheel.Vertical)
        expectedEvents += 4 // cancel + enter + hover + scroll
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down
        var t = 0L
        events.removeFirst(1).let { (downEvent) ->
            downEvent.verifyTouchEvent(1, ACTION_DOWN, 0, t)
            downEvent.verifyTouchPointer(1, position1)
        }

        // cancel
        t += eventPeriodMillis
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTouchEvent(1, ACTION_CANCEL, 0, t)
            cancelEvent.verifyTouchPointer(1, position1)
        }

        // enter + hover + scroll
        events.removeFirst(3).let { (enterEvent, hoverEvent, scrollEvent) ->
            enterEvent.verifyMouseEvent(ACTION_HOVER_ENTER, t, Offset.Zero, 0)
            hoverEvent.verifyMouseEvent(ACTION_HOVER_MOVE, t, Offset.Zero, 0)
            scrollEvent.verifyMouseEvent(ACTION_SCROLL, t, Offset.Zero, 0, Pair(AXIS_VSCROLL, -1f))
        }
    }

    @Test
    fun enqueueMouseDown_alreadyDown() {
        subject.enqueueMousePress(1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse button down event, button 1 is already pressed"
        ) {
            subject.enqueueMousePress(1)
        }
    }

    @Test
    fun enqueueMouseDown_outOfBounds() {
        subject.updateMousePosition(positionMin1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot start a mouse gesture outside the Compose root bounds, " +
                "mouse position is .* and bounds are .*"
        ) {
            subject.enqueueMousePress(1)
        }
    }

    @Test
    fun enqueueMouseUp_withoutDown() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse button up event, button 1 is not pressed"
        ) {
            subject.enqueueMouseRelease(1)
        }
    }

    @Test
    fun enqueueMouseEnter_alreadyEntered() {
        subject.enqueueMouseEnter(position1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse hover enter event, mouse is already hovering"
        ) {
            subject.enqueueMouseEnter(position1)
        }
    }

    @Test
    fun enqueueMouseEnter_buttonsDown() {
        subject.enqueueMousePress(1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse hover enter event, mouse buttons are down"
        ) {
            subject.enqueueMouseEnter(position1)
        }
    }

    @Test
    fun enqueueMouseEnter_outOfBounds() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse hover enter event, " +
                "Offset\\(-1\\.0, -1\\.0\\) is out of bounds"
        ) {
            subject.enqueueMouseEnter(positionMin1)
        }
    }

    @Test
    fun enqueueMouseExit_notEntered() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse hover exit event, mouse is not hovering"
        ) {
            subject.enqueueMouseExit(position1)
        }
    }

    @Test
    fun enqueueMouseCancel_withoutDown() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send mouse cancel event, no mouse buttons are pressed"
        ) {
            subject.enqueueMouseCancel()
        }
    }

    private fun AndroidInputDispatcher.verifyMousePosition(expectedPosition: Offset) {
        assertWithMessage("currentMousePosition")
            .that(currentMousePosition).isEqualTo(expectedPosition)
    }

    private fun <E> MutableList<E>.removeFirst(n: Int): List<E> {
        return mutableListOf<E>().also { result ->
            repeat(n) { result.add(removeFirst()) }
        }
    }
}
