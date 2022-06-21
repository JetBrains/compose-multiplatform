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

package androidx.compose.ui.test.inputdispatcher

import android.view.KeyEvent
import androidx.compose.testutils.expectError
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verifyKeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests if [AndroidInputDispatcher.enqueueKeyDown], [AndroidInputDispatcher.enqueueKeyUp]  and
 * friends work.
 */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
@OptIn(ExperimentalComposeUiApi::class)
class KeyEventsTest : InputDispatcherTest() {

    companion object {
        private const val keyDown = KeyEvent.ACTION_DOWN
        private const val keyUp = KeyEvent.ACTION_UP

        private val oneKey = Key.One
        private val aKey = Key.A
        private val enterKey = Key.Enter
        private val backspaceKey = Key.Backspace

        private val functionKey = Key.Function
        private const val functionKeyMetaMask = KeyEvent.META_FUNCTION_ON

        private val leftCtrl = Key.CtrlLeft
        private const val leftCtrlMask = KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON
        private val rightCtrl = Key.CtrlRight
        private const val rightCtrlMask = KeyEvent.META_CTRL_RIGHT_ON or KeyEvent.META_CTRL_ON

        private val leftAlt = Key.AltLeft
        private const val leftAltMask = KeyEvent.META_ALT_LEFT_ON or KeyEvent.META_ALT_ON
        private val rightAlt = Key.AltRight
        private const val rightAltMask = KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_ALT_ON

        private val leftMeta = Key.MetaLeft
        private const val leftMetaMask = KeyEvent.META_META_LEFT_ON or KeyEvent.META_META_ON
        private val rightMeta = Key.MetaRight
        private const val rightMetaMask = KeyEvent.META_META_RIGHT_ON or KeyEvent.META_META_ON

        private val leftShift = Key.ShiftLeft
        private const val leftShiftMask = KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON
        private val rightShift = Key.ShiftRight
        private const val rightShiftMask = KeyEvent.META_SHIFT_RIGHT_ON or KeyEvent.META_SHIFT_ON

        private val capsLock = Key.CapsLock
        private const val capsLockMask = KeyEvent.META_CAPS_LOCK_ON
        private val numLock = Key.NumLock
        private const val numLockMask = KeyEvent.META_NUM_LOCK_ON
        private val scrollLock = Key.ScrollLock
        private const val scrollLockMask = KeyEvent.META_SCROLL_LOCK_ON

        private const val allLockMasks = capsLockMask or numLockMask or scrollLockMask

        private const val allMetaMasks = functionKeyMetaMask or leftCtrlMask or rightCtrlMask or
            leftAltMask or rightAltMask or leftMetaMask or rightMetaMask or
            leftShiftMask or rightShiftMask

        private const val allMasks = allLockMasks or allMetaMasks

        private const val initialRepeatDelay = InputDispatcher.InitialRepeatDelay
        private const val subsequentRepeatDelay = InputDispatcher.SubsequentRepeatDelay
    }

    @Test
    fun keyStartsUp() {
        assertFalse(subject.isKeyDown(oneKey))
    }

    @Test
    fun keyIsDownAfterEnqueueDown() {
        subject.enqueueKeyDown(aKey)
        assertTrue(subject.isKeyDown(aKey))
    }

    @Test
    fun keyIsUpAfterEnqueueUp() {
        subject.enqueueKeyDown(backspaceKey)
        subject.enqueueKeyUp(backspaceKey)
        assertFalse(subject.isKeyDown(backspaceKey))
    }

    @Test
    fun enqueueKeyDown_enqueuesKeyEvent() {
        subject.enqueueKeyDown(enterKey)
        subject.flush()

        assertEquals(1, recorder.events.size)
        recorder.events.first().verifyKeyEvent(keyDown, enterKey.nativeKeyCode, 0, 0)
    }

    @Test
    fun enqueueKeyUp_enqueuesKeyEvent() {
        subject.enqueueKeyDown(oneKey)
        subject.enqueueKeyUp(oneKey)
        subject.flush()

        assertEquals(2, recorder.events.size)
        recorder.events.last().verifyKeyEvent(keyUp, oneKey.nativeKeyCode, 0, 0)
    }

    @Test
    fun capsLock_andShift_produceCorrectMetaState() {
        enqueueKeyPress(capsLock)
        subject.enqueueKeyDown(leftShift)
        subject.enqueueKeyDown(aKey)
        subject.flush()

        assertTrue(subject.isCapsLockOn)
        recorder.events.last().verifyKeyEvent(
            keyDown, aKey.nativeKeyCode, 0, 0, capsLockMask or leftShiftMask
        )
    }

    /* Lock key state tests. */

    @Test
    fun capsLock_startsOff() = assertFalse(subject.isCapsLockOn)

    @Test
    fun capsLockOn_afterDown() {
        subject.enqueueKeyDown(capsLock)
        assertTrue(subject.isCapsLockOn)
    }

    @Test
    fun capsLockOn_afterPress() {
        enqueueKeyPress(capsLock)
        assertTrue(subject.isCapsLockOn)
    }

    @Test
    fun capsLockOff_afterDoublePress() {
        enqueueKeyPress(capsLock)
        enqueueKeyPress(capsLock)
        assertFalse(subject.isCapsLockOn)
    }

    @Test
    fun numLock_startsOff() = assertFalse(subject.isNumLockOn)

    @Test
    fun numLockOn_afterDown() {
        subject.enqueueKeyDown(numLock)
        assertTrue(subject.isNumLockOn)
    }

    @Test
    fun numLockOn_afterPress() {
        enqueueKeyPress(numLock)
        assertTrue(subject.isNumLockOn)
    }

    @Test
    fun numLockOff_afterDoublePress() {
        enqueueKeyPress(numLock)
        enqueueKeyPress(numLock)
        assertFalse(subject.isNumLockOn)
    }

    @Test
    fun scrollLock_startsOff() = assertFalse(subject.isScrollLockOn)

    @Test
    fun scrollLockOn_afterDown() {
        subject.enqueueKeyDown(scrollLock)
        assertTrue(subject.isScrollLockOn)
    }

    @Test
    fun scrollLockOn_afterPress() {
        enqueueKeyPress(scrollLock)
        assertTrue(subject.isScrollLockOn)
    }

    @Test
    fun scrollLockOff_afterDoublePress() {
        enqueueKeyPress(scrollLock)
        enqueueKeyPress(scrollLock)
        assertFalse(subject.isScrollLockOn)
    }

    /* Meta state generation. */

    @Test
    fun functionKey_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(functionKey, functionKeyMetaMask)

    @Test
    fun leftCtrl_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftCtrl, leftCtrlMask)

    @Test
    fun rightCtrl_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightCtrl, rightCtrlMask)

    @Test
    fun leftAlt_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftAlt, leftAltMask)

    @Test
    fun rightAlt_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightAlt, rightAltMask)

    @Test
    fun leftMeta_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftMeta, leftMetaMask)

    @Test
    fun rightMeta_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightMeta, rightMetaMask)

    @Test
    fun leftShift_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftShift, leftShiftMask)

    @Test
    fun rightShift_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightShift, rightShiftMask)

    @Test
    fun capsLock_metaState_generatedCorrectly() =
        verifyLockKeyMetaState(capsLock, capsLockMask)

    @Test
    fun numLock_metaState_generatedCorrectly() =
        verifyLockKeyMetaState(numLock, numLockMask)

    @Test
    fun scrollLock_metaState_generatedCorrectly() =
        verifyLockKeyMetaState(scrollLock, scrollLockMask)

    @Test
    fun lockKeys_metaState_combinedCorrectly() {
        enqueueKeyPress(capsLock)
        enqueueKeyPress(numLock)
        enqueueKeyPress(scrollLock)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(6, recorder.events.size)
        recorder.events.last().verifyKeyEvent(
            keyUp,
            scrollLock.nativeKeyCode,
            expectedMetaState = allLockMasks
        )
    }

    @Test
    fun metaKeys_metaState_combinedCorrectly() {
        subject.enqueueKeyDown(functionKey)
        subject.enqueueKeyDown(leftCtrl)
        subject.enqueueKeyDown(rightCtrl)
        subject.enqueueKeyDown(leftAlt)
        subject.enqueueKeyDown(rightAlt)
        subject.enqueueKeyDown(leftMeta)
        subject.enqueueKeyDown(rightMeta)
        subject.enqueueKeyDown(leftShift)
        subject.enqueueKeyDown(rightShift)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(9, recorder.events.size)
        recorder.events.last().verifyKeyEvent(
            keyDown, rightShift.nativeKeyCode, expectedMetaState = allMetaMasks
        )
    }

    @Test
    fun metaAndLockKeys_metaState_combinedCorrectly() {
        enqueueKeyPress(capsLock)
        enqueueKeyPress(numLock)
        enqueueKeyPress(scrollLock)
        subject.enqueueKeyDown(functionKey)
        subject.enqueueKeyDown(leftCtrl)
        subject.enqueueKeyDown(rightCtrl)
        subject.enqueueKeyDown(leftAlt)
        subject.enqueueKeyDown(rightAlt)
        subject.enqueueKeyDown(leftMeta)
        subject.enqueueKeyDown(rightMeta)
        subject.enqueueKeyDown(leftShift)
        subject.enqueueKeyDown(rightShift)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(15, recorder.events.size)
        recorder.events.last().verifyKeyEvent(
            keyDown, rightShift.nativeKeyCode, expectedMetaState = allMasks
        )
    }

    /* Repeat key tests */

    @Test
    fun noRepeat_before_repeatDelay() {
        subject.enqueueKeyDown(aKey)
        subject.advanceEventTime(initialRepeatDelay - 1)
        subject.enqueueKeyUp(aKey)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(2, recorder.events.size)
        recorder.events.first().verifyKeyEvent(keyDown, aKey.nativeKeyCode)
        recorder.events.last().verifyKeyEvent(
            keyUp, aKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay - 1
        )
    }

    @Test
    fun keyDownRepeats_exactlyAt_repeatDelay() {
        subject.enqueueKeyDown(aKey)
        subject.advanceEventTime(initialRepeatDelay)
        subject.enqueueKeyUp(aKey)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(3, recorder.events.size)
        recorder.events[0].verifyKeyEvent(keyDown, aKey.nativeKeyCode)
        recorder.events[1].verifyKeyEvent(keyDown, aKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay, expectedRepeat = 1)
        recorder.events[2].verifyKeyEvent(keyUp, aKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay)
    }

    @Test
    fun repeat_cancelledBy_newKeyDown() {
        subject.enqueueKeyDown(aKey)
        subject.advanceEventTime(initialRepeatDelay - 1)
        subject.enqueueKeyDown(oneKey)
        subject.advanceEventTime(initialRepeatDelay - 1)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(2, recorder.events.size)
        recorder.events[0].verifyKeyEvent(keyDown, aKey.nativeKeyCode)
        recorder.events[1].verifyKeyEvent(keyDown, oneKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay - 1, expectedDownTime = initialRepeatDelay - 1)
    }

    @Test
    fun secondKeyDown_isRepeated() {
        subject.enqueueKeyDown(aKey)
        subject.advanceEventTime(initialRepeatDelay - 1)
        subject.enqueueKeyDown(oneKey)
        subject.advanceEventTime(initialRepeatDelay)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(3, recorder.events.size)

        recorder.events[0].verifyKeyEvent(keyDown, aKey.nativeKeyCode)
        recorder.events[1].verifyKeyEvent(keyDown, oneKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay - 1, expectedDownTime = initialRepeatDelay - 1)
        recorder.events[2].verifyKeyEvent(
            keyDown,
            oneKey.nativeKeyCode,
            expectedEventTime = 2 * initialRepeatDelay - 1,
            expectedDownTime = initialRepeatDelay - 1,
            expectedRepeat = 1
        )
    }

    @Test
    fun firstKeyDown_notRepeated_afterLatestKeyUp() {
        subject.enqueueKeyDown(aKey)
        subject.advanceEventTime(initialRepeatDelay - 1)
        subject.enqueueKeyDown(oneKey)
        subject.advanceEventTime(initialRepeatDelay)
        subject.enqueueKeyUp(oneKey)
        subject.advanceEventTime(initialRepeatDelay * 3) // The A key should not repeat.
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(4, recorder.events.size)

        recorder.events[0].verifyKeyEvent(keyDown, aKey.nativeKeyCode)
        recorder.events[1].verifyKeyEvent(keyDown, oneKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay - 1, expectedDownTime = initialRepeatDelay - 1)
        recorder.events[2].verifyKeyEvent(
            keyDown,
            oneKey.nativeKeyCode,
            expectedEventTime = 2 * initialRepeatDelay - 1,
            expectedDownTime = initialRepeatDelay - 1,
            expectedRepeat = 1
        )
        recorder.events[3].verifyKeyEvent(
            keyUp,
            oneKey.nativeKeyCode,
            expectedEventTime = 2 * initialRepeatDelay - 1,
            expectedDownTime = initialRepeatDelay - 1
        )
    }

    @Test
    fun multipleRepeatKeyTest() {
        subject.enqueueKeyDown(aKey) // t = 0
        subject.advanceEventTime(initialRepeatDelay - 1) // t = 499
        subject.flush()

        assertEquals(1, recorder.events.size)
        recorder.events.first().verifyKeyEvent(keyDown, aKey.nativeKeyCode)

        subject.advanceEventTime(1) // t = 500
        subject.flush()

        assertEquals(2, recorder.events.size)
        recorder.events.last().verifyKeyEvent(keyDown, aKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay, expectedRepeat = 1)

        subject.advanceEventTime(subsequentRepeatDelay - 1) // t = 549
        subject.flush()

        assertEquals(2, recorder.events.size)
        subject.advanceEventTime(1) // t = 550
        subject.flush()

        assertEquals(3, recorder.events.size)
        recorder.events.last().verifyKeyEvent(keyDown, aKey.nativeKeyCode,
            expectedEventTime = initialRepeatDelay + subsequentRepeatDelay, expectedRepeat = 2)

        subject.advanceEventTime(subsequentRepeatDelay * 10) // t = 1050
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(13, recorder.events.size)

        recorder.events.drop(3).forEachIndexed { i, event ->
            event.verifyKeyEvent(keyDown, aKey.nativeKeyCode,
                expectedEventTime = (initialRepeatDelay + subsequentRepeatDelay * (i + 2)),
                expectedRepeat = 3 + i)
        }
    }

    /* Negative testing. */

    @Test
    fun enqueueKeyDown_alreadyDown() {
        subject.enqueueKeyDown(oneKey)
        expectError<IllegalStateException>(
            expectedMessage =
            "Cannot send key down event, Key\\($oneKey\\) is already pressed down."
        ) {
            subject.enqueueKeyDown(oneKey)
        }
    }

    @Test
    fun enqueueKeyUp_withoutDown() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send key up event, Key\\($aKey\\) is not pressed down."
        ) {
            subject.enqueueKeyUp(aKey)
        }
    }

    private fun enqueueKeyPress(key: Key) {
        subject.enqueueKeyDown(key)
        subject.enqueueKeyUp(key)
    }

    private fun verifyMetaKeyMetaState(key: Key, expectedMetaState: Int) {
        enqueueKeyPress(key)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(2, recorder.events.size)
        recorder.events[0].verifyKeyEvent(
            keyDown, key.nativeKeyCode, expectedMetaState = expectedMetaState
        )
        recorder.events[1].verifyKeyEvent(keyUp, key.nativeKeyCode)
    }

    private fun verifyLockKeyMetaState(key: Key, expectedMetaState: Int) {
        enqueueKeyPress(key)
        enqueueKeyPress(key)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertEquals(4, recorder.events.size)
        recorder.events[0].verifyKeyEvent(
            keyDown, key.nativeKeyCode, expectedMetaState = expectedMetaState
        )
        recorder.events[1].verifyKeyEvent(
            keyUp, key.nativeKeyCode, expectedMetaState = expectedMetaState
        )
        recorder.events[2].verifyKeyEvent(keyDown, key.nativeKeyCode)
        recorder.events[3].verifyKeyEvent(keyUp, key.nativeKeyCode)
    }
}