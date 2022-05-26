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
import androidx.compose.ui.test.RobolectricMinSdk
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
            keyDown, aKey.nativeKeyCode, 0, 0, capsLockMetaMask or leftShiftMetaMask
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
        verifyMetaKeyMetaState(leftCtrl, leftCtrlMetaMask)

    @Test
    fun rightCtrl_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightCtrl, rightCtrlMetaMask)

    @Test
    fun leftAlt_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftAlt, leftAltMetaMask)

    @Test
    fun rightAlt_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightAlt, rightAltMetaMask)

    @Test
    fun leftMeta_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftMeta, leftMetaMetaMask)

    @Test
    fun rightMeta_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightMeta, rightMetaMetaMask)

    @Test
    fun leftShift_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(leftShift, leftShiftMetaMask)

    @Test
    fun rightShift_metaState_generatedCorrectly() =
        verifyMetaKeyMetaState(rightShift, rightShiftMetaMask)

    @Test
    fun capsLock_metaState_generatedCorrectly() =
        verifyLockKeyMetaState(capsLock, capsLockMetaMask)

    @Test
    fun numLock_metaState_generatedCorrectly() =
        verifyLockKeyMetaState(numLock, numLockMetaMask)

    @Test
    fun scrollLock_metaState_generatedCorrectly() =
        verifyLockKeyMetaState(scrollLock, scrollLockMetaMask)

    @Test
    fun lockKeys_metaState_combinedCorrectly() {
        enqueueKeyPress(capsLock)
        enqueueKeyPress(numLock)
        enqueueKeyPress(scrollLock)
        subject.flush()

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

        assertEquals(15, recorder.events.size)
        recorder.events.last().verifyKeyEvent(
            keyDown, rightShift.nativeKeyCode, expectedMetaState = allMasks
        )
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

        recorder.events[0].verifyKeyEvent(
            keyDown, key.nativeKeyCode, expectedMetaState = expectedMetaState
        )
        recorder.events[1].verifyKeyEvent(keyUp, key.nativeKeyCode)
    }

    private fun verifyLockKeyMetaState(key: Key, expectedMetaState: Int) {
        enqueueKeyPress(key)
        enqueueKeyPress(key)
        subject.flush()

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