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

package androidx.compose.ui.test.inputdispatcher.multimodal

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.inputdispatcher.InputDispatcherTest
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verifyKeyEvent
import androidx.compose.ui.test.util.verifyMouseEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests if [AndroidInputDispatcher.enqueueKeyDown], [AndroidInputDispatcher.enqueueKeyUp]  and
 * friends work.
 */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class KeyAndMouseEventsTest : InputDispatcherTest() {

    companion object {

        // Positions
        private val position1 = Offset(1f, 1f)

        // Keys and meta state masks
        private const val keyDown = KeyEvent.ACTION_DOWN
        private const val keyUp = KeyEvent.ACTION_UP

        private val functionKey = Key.Function
        private const val functionKeyMetaMask = KeyEvent.META_FUNCTION_ON

        private val leftCtrl = Key.CtrlLeft
        private const val leftCtrlMetaMask = KeyEvent.META_CTRL_LEFT_ON
        private val rightCtrl = Key.CtrlRight
        private const val rightCtrlMetaMask = KeyEvent.META_CTRL_RIGHT_ON

        private val leftAlt = Key.AltLeft
        private const val leftAltMetaMask = KeyEvent.META_ALT_LEFT_ON
        private val rightAlt = Key.AltRight
        private const val rightAltMetaMask = KeyEvent.META_ALT_RIGHT_ON

        private val leftMeta = Key.MetaLeft
        private const val leftMetaMetaMask = KeyEvent.META_META_LEFT_ON
        private val rightMeta = Key.MetaRight
        private const val rightMetaMetaMask = KeyEvent.META_META_RIGHT_ON

        private val leftShift = Key.ShiftLeft
        private const val leftShiftMetaMask = KeyEvent.META_SHIFT_LEFT_ON
        private val rightShift = Key.ShiftRight
        private const val rightShiftMetaMask = KeyEvent.META_SHIFT_RIGHT_ON

        private val capsLock = Key.CapsLock
        private const val capsLockMetaMask = KeyEvent.META_CAPS_LOCK_ON
        private val numLock = Key.NumLock
        private const val numLockMetaMask = KeyEvent.META_NUM_LOCK_ON
        private val scrollLock = Key.ScrollLock
        private const val scrollLockMetaMask = KeyEvent.META_SCROLL_LOCK_ON

        private const val allLockMasks = capsLockMetaMask or numLockMetaMask or scrollLockMetaMask

        private const val allMetaMasks = functionKeyMetaMask or leftCtrlMetaMask or
            rightCtrlMetaMask or leftAltMetaMask or rightAltMetaMask or leftMetaMetaMask or
            rightMetaMetaMask or leftShiftMetaMask or rightShiftMetaMask

        private const val allMasks = allLockMasks or allMetaMasks
    }

    @Test
    fun functionKey_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(functionKey, functionKeyMetaMask)

    @Test
    fun leftCtrl_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(leftCtrl, leftCtrlMetaMask)

    @Test
    fun rightCtrl_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(rightCtrl, rightCtrlMetaMask)

    @Test
    fun leftAlt_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(leftAlt, leftAltMetaMask)

    @Test
    fun rightAlt_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(rightAlt, rightAltMetaMask)

    @Test
    fun leftMeta_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(leftMeta, leftMetaMetaMask)

    @Test
    fun rightMeta_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(rightMeta, rightMetaMetaMask)

    @Test
    fun leftShift_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(leftShift, leftShiftMetaMask)

    @Test
    fun rightShift_metaState_generatedCorrectly() =
        verifyMetaKeyClickState(rightShift, rightShiftMetaMask)

    @Test
    fun capsLock_metaState_generatedCorrectly() =
        verifyLockKeyClickState(capsLock, capsLockMetaMask)

    @Test
    fun numLock_metaState_generatedCorrectly() =
        verifyLockKeyClickState(numLock, numLockMetaMask)

    @Test
    fun scrollLock_metaState_generatedCorrectly() =
        verifyLockKeyClickState(scrollLock, scrollLockMetaMask)

    @Test
    fun lockKeys_metaState_combinedCorrectly_inMousePress() {
        enqueueKeyPress(capsLock)
        enqueueKeyPress(numLock)
        enqueueKeyPress(scrollLock)
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        subject.flush()

        assertEquals(8, recorder.events.size)
        recorder.events[5].verifyKeyEvent(keyUp, scrollLock.nativeKeyCode,
            expectedMetaState = allLockMasks)
        recorder.events[6].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = allLockMasks)
        recorder.events[7].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = allLockMasks)
    }

    @Test
    fun metaKeys_metaState_combinedCorrectly_inMousePress() {
        subject.enqueueKeyDown(functionKey)
        subject.enqueueKeyDown(leftCtrl)
        subject.enqueueKeyDown(rightCtrl)
        subject.enqueueKeyDown(leftAlt)
        subject.enqueueKeyDown(rightAlt)
        subject.enqueueKeyDown(leftMeta)
        subject.enqueueKeyDown(rightMeta)
        subject.enqueueKeyDown(leftShift)
        subject.enqueueKeyDown(rightShift)
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        subject.flush()

        assertEquals(11, recorder.events.size)
        recorder.events[8].verifyKeyEvent(keyDown, rightShift.nativeKeyCode,
            expectedMetaState = allMetaMasks)
        recorder.events[9].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = allMetaMasks)
        recorder.events[10].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = allMetaMasks)
    }

    @Test
    fun metaAndLockKeys_metaState_combinedCorrectly_inMousePress() {
        enqueueKeyPress(capsLock) // key up + down = 2
        enqueueKeyPress(numLock) // + key up + down = 4
        enqueueKeyPress(scrollLock) // + key up + down = 6
        subject.enqueueKeyDown(functionKey) // + key down = 7
        subject.enqueueKeyDown(leftCtrl) // + key down = 8
        subject.enqueueKeyDown(rightCtrl) // + key down = 9
        subject.enqueueKeyDown(leftAlt) // + key down = 10
        subject.enqueueKeyDown(rightAlt) // + key down = 11
        subject.enqueueKeyDown(leftMeta) // + key down = 12
        subject.enqueueKeyDown(rightMeta) // + key down = 13
        subject.enqueueKeyDown(leftShift) // + key down = 14
        subject.enqueueKeyDown(rightShift) // + key down = 15
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMousePress(MouseButton.Primary.buttonId) // + mouse down + press = 17
        subject.flush()

        assertEquals(17, recorder.events.size)
        recorder.events[14].verifyKeyEvent(keyDown, rightShift.nativeKeyCode,
            expectedMetaState = allMasks)
        recorder.events[15].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = allMasks)
        recorder.events[16].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = allMasks)
    }

    @Test
    fun scroll_vertically() {
        scrollTest(ScrollWheel.Vertical) { Pair(MotionEvent.AXIS_VSCROLL, -it) }
    }

    @Test
    fun scroll_horizontally() {
        scrollTest(ScrollWheel.Horizontal) { Pair(MotionEvent.AXIS_HSCROLL, it) }
    }

    private fun scrollTest(scrollWheel: ScrollWheel, scrollAxis: (Float) -> Pair<Int, Float>) {

        var expectedEvents = 0
        enqueueKeyPress(capsLock) // key up + down = 2
        enqueueKeyPress(numLock) // + key up + down = 4
        enqueueKeyPress(scrollLock) // + key up + down = 6
        subject.enqueueKeyDown(functionKey) // + key down = 7
        subject.enqueueKeyDown(leftCtrl) // + key down = 8
        subject.enqueueKeyDown(rightCtrl) // + key down = 9
        subject.enqueueKeyDown(leftAlt) // + key down = 10
        subject.enqueueKeyDown(rightAlt) // + key down = 11
        subject.enqueueKeyDown(leftMeta) // + key down = 12
        subject.enqueueKeyDown(rightMeta) // + key down = 13
        subject.enqueueKeyDown(leftShift) // + key down = 14
        subject.enqueueKeyDown(rightShift) // + key down = 15
        expectedEvents += 15
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
        Truth.assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // Remove key presses - they aren't being tested here.
        events.removeFirst(15)

        // enter + hover
        var t = 0L
        var buttonState = 0
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_ENTER, t,
                position1, buttonState, expectedMetaState = allMasks)
            hoverEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_MOVE, t,
                position1, buttonState, expectedMetaState = allMasks)
        }

        // hover + scroll
        t += InputDispatcher.eventPeriodMillis
        events.removeFirst(2).let { (hoverEvent, scrollEvent) ->
            hoverEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_MOVE, t,
                position1, buttonState, expectedMetaState = allMasks)
            scrollEvent.verifyMouseEvent(
                MotionEvent.ACTION_SCROLL, t,
                position1, buttonState, scrollAxis(1f), expectedMetaState = allMasks)
        }

        // exit + down + press
        t = 0L // down resets downTime
        buttonState = MotionEvent.BUTTON_PRIMARY
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_EXIT, t,
                position1, buttonState, expectedMetaState = allMasks)
            downEvent.verifyMouseEvent(
                MotionEvent.ACTION_DOWN, t,
                position1, buttonState, expectedMetaState = allMasks)
            pressEvent.verifyMouseEvent(
                MotionEvent.ACTION_BUTTON_PRESS, t,
                position1, buttonState, expectedMetaState = allMasks)
        }

        // move + scroll
        t += InputDispatcher.eventPeriodMillis
        events.removeFirst(2).let { (moveEvent, scrollEvent) ->
            moveEvent.verifyMouseEvent(
                MotionEvent.ACTION_MOVE, t,
                position1, buttonState, expectedMetaState = allMasks)
            scrollEvent.verifyMouseEvent(
                MotionEvent.ACTION_SCROLL, t,
                position1, buttonState, scrollAxis(2f), expectedMetaState = allMasks)
        }

        // release + up + enter + hover
        t += InputDispatcher.eventPeriodMillis
        buttonState = 0
        events.removeFirst(4).let { (releaseEvent, upEvent, enterEvent, hoverEvent) ->
            releaseEvent.verifyMouseEvent(
                MotionEvent.ACTION_BUTTON_RELEASE, t,
                position1, buttonState, expectedMetaState = allMasks)
            upEvent.verifyMouseEvent(
                MotionEvent.ACTION_UP, t,
                position1, buttonState, expectedMetaState = allMasks)
            enterEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_ENTER, t,
                position1, buttonState, expectedMetaState = allMasks)
            hoverEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_MOVE, t,
                position1, buttonState, expectedMetaState = allMasks)
        }

        // hover + scroll
        t += InputDispatcher.eventPeriodMillis
        events.removeFirst(2).let { (hoverEvent, scrollEvent) ->
            hoverEvent.verifyMouseEvent(
                MotionEvent.ACTION_HOVER_MOVE, t,
                position1, buttonState, expectedMetaState = allMasks)
            scrollEvent.verifyMouseEvent(
                MotionEvent.ACTION_SCROLL, t,
                position1, buttonState, scrollAxis(3f), expectedMetaState = allMasks)
        }
    }

    private fun AndroidInputDispatcher.verifyMousePosition(expectedPosition: Offset) {
        Truth.assertWithMessage("currentMousePosition")
            .that(currentMousePosition).isEqualTo(expectedPosition)
    }

    private fun <E> MutableList<E>.removeFirst(n: Int): List<E> {
        return mutableListOf<E>().also { result ->
            repeat(n) { result.add(removeFirst()) }
        }
    }

    private fun enqueueKeyPress(key: Key) {
        subject.enqueueKeyDown(key)
        subject.enqueueKeyUp(key)
    }

    private fun verifyMetaKeyClickState(key: Key, expectedMetaState: Int = 0) {
        subject.enqueueKeyDown(key)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        subject.enqueueMouseRelease(MouseButton.Primary.buttonId)
        subject.enqueueKeyUp(key)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        subject.flush()

        recorder.assertHasValidEventTimes()
        Truth.assertThat(recorder.events).hasSize(11)

        // Key Down
        recorder.events[0].verifyKeyEvent(keyDown, key.nativeKeyCode,
            expectedMetaState = expectedMetaState)

        // Mouse Press
        recorder.events[1].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = expectedMetaState)
        recorder.events[2].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = expectedMetaState)

        // Mouse Release
        recorder.events[3].verifyMouseEvent(MotionEvent.ACTION_BUTTON_RELEASE, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)
        recorder.events[4].verifyMouseEvent(MotionEvent.ACTION_UP, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)
        recorder.events[5].verifyMouseEvent(MotionEvent.ACTION_HOVER_ENTER, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)
        recorder.events[6].verifyMouseEvent(MotionEvent.ACTION_HOVER_MOVE, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)

        // Key Release
        recorder.events[7].verifyKeyEvent(keyUp, key.nativeKeyCode)

        // Mouse Press
        recorder.events[8].verifyMouseEvent(MotionEvent.ACTION_HOVER_EXIT, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY)
        recorder.events[9].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY)
        recorder.events[10].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY)
    }

    private fun verifyLockKeyClickState(key: Key, expectedMetaState: Int = 0) {
        enqueueKeyPress(key)
        subject.verifyMousePosition(Offset.Zero)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        subject.enqueueMouseRelease(MouseButton.Primary.buttonId)
        enqueueKeyPress(key)
        subject.enqueueMousePress(MouseButton.Primary.buttonId)
        subject.flush()

        recorder.assertHasValidEventTimes()
        Truth.assertThat(recorder.events).hasSize(13)

        // Key Toggle On
        recorder.events[0].verifyKeyEvent(keyDown, key.nativeKeyCode,
            expectedMetaState = expectedMetaState)
        recorder.events[1].verifyKeyEvent(keyUp, key.nativeKeyCode,
            expectedMetaState = expectedMetaState)

        // Mouse Press
        recorder.events[2].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = expectedMetaState)
        recorder.events[3].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY, expectedMetaState = expectedMetaState)

        // Mouse Release
        recorder.events[4].verifyMouseEvent(MotionEvent.ACTION_BUTTON_RELEASE, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)
        recorder.events[5].verifyMouseEvent(MotionEvent.ACTION_UP, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)
        recorder.events[6].verifyMouseEvent(MotionEvent.ACTION_HOVER_ENTER, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)
        recorder.events[7].verifyMouseEvent(MotionEvent.ACTION_HOVER_MOVE, 0L,
            Offset.Zero, 0, expectedMetaState = expectedMetaState)

        // Key Toggle Off
        recorder.events[8].verifyKeyEvent(keyDown, key.nativeKeyCode)
        recorder.events[9].verifyKeyEvent(keyUp, key.nativeKeyCode)

        // Mouse Press
        recorder.events[10].verifyMouseEvent(MotionEvent.ACTION_HOVER_EXIT, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY)
        recorder.events[11].verifyMouseEvent(MotionEvent.ACTION_DOWN, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY)
        recorder.events[12].verifyMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, 0L,
            Offset.Zero, MotionEvent.BUTTON_PRIMARY)
    }
}