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

package androidx.compose.ui.test.injectionscope.key

import androidx.compose.testutils.expectError
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.injectionscope.key.Common.assertTyped
import androidx.compose.ui.test.injectionscope.key.Common.performKeyInput
import androidx.compose.ui.test.isAltDown
import androidx.compose.ui.test.isCtrlDown
import androidx.compose.ui.test.isFnDown
import androidx.compose.ui.test.isMetaDown
import androidx.compose.ui.test.isShiftDown
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.util.TestTextField
import androidx.compose.ui.test.util.TestTextField.Tag
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.withKeysDown
import androidx.test.filters.MediumTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if the meta key methods in [KeyInjectionScope] such as [isShiftDown] work.
 */
@MediumTest
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class MetaKeysTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // Set content to a simple text field.
        rule.setContent {
            TestTextField()
        }
        // Bring text field into focus by clicking on it.
        rule.onNodeWithTag(Tag).performClick()
    }

    @Test
    fun metaKeys_startUp() = assertAllMetaKeysUp()

    @Test
    fun metaKeys_areDown_afterDown() {
        pressAllMetaKeysDown()
        assertAllMetaKeysDown()
    }

    @Test
    fun metaKeys_areUp_afterUp() {
        pressAllMetaKeysDown()
        releaseAllMetaKeys()
        assertAllMetaKeysUp()
    }

    @Test
    fun functionKey_isDown_withFnDown() = assertKeyDownIsTrueWithKeyDown(Key.Function)

    @Test
    fun ctrl_isDown_withCtrlDown() = assertKeyDownIsTrueWithKeyDown(Key.CtrlLeft)

    @Test
    fun alt_isDown_withAltDown() = assertKeyDownIsTrueWithKeyDown(Key.AltLeft)

    @Test
    fun meta_isDown_withMetaDown() = assertKeyDownIsTrueWithKeyDown(Key.MetaLeft)

    @Test
    fun shift_isDown_withShiftDown() = assertKeyDownIsTrueWithKeyDown(Key.ShiftLeft)

    @Test
    fun allMetaKeysDown_withKeysDown() {
        rule.performKeyInput {
            withKeysDown(
                listOf(Key.Function, Key.CtrlRight, Key.AltRight, Key.MetaRight, Key.ShiftRight)
            ) {
                assertTrue(isFnDown)
                assertTrue(isCtrlDown)
                assertTrue(isAltDown)
                assertTrue(isMetaDown)
                assertTrue(isShiftDown)
            }
        }
    }

    @Test
    fun lettersTyped_withShiftDown_areUppercase() {
        rule.performKeyInput {
            pressKey(Key.A)
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.A)
                pressKey(Key.B)
            }
            pressKey(Key.B)
        }
        rule.assertTyped("aABb")
    }

    @Test
    fun fnDown_inWithFnDown_throwsIllegalStateException() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send key down event, " +
                "Key\\(${Key.Function}\\) is already pressed down."
        ) {
            rule.performKeyInput {
                withKeyDown(Key.Function) {
                    keyDown(Key.Function)
                    pressKey(Key.A)
                }
            }
        }
    }

    private fun pressAllMetaKeysDown() {
        rule.performKeyInput {
            keyDown(Key.Function)
            keyDown(Key.CtrlLeft)
            keyDown(Key.AltLeft)
            keyDown(Key.MetaLeft)
            keyDown(Key.ShiftLeft)
        }
    }

    private fun releaseAllMetaKeys() {
        rule.performKeyInput {
            keyUp(Key.Function)
            keyUp(Key.CtrlLeft)
            keyUp(Key.AltLeft)
            keyUp(Key.MetaLeft)
            keyUp(Key.ShiftLeft)
        }
    }

    private fun assertAllMetaKeysDown() {
        rule.performKeyInput {
            assertTrue(isFnDown)
            assertTrue(isCtrlDown)
            assertTrue(isAltDown)
            assertTrue(isMetaDown)
            assertTrue(isShiftDown)
        }
    }

    private fun assertAllMetaKeysUp() {
        rule.performKeyInput {
            assertFalse(isFnDown)
            assertFalse(isCtrlDown)
            assertFalse(isAltDown)
            assertFalse(isMetaDown)
            assertFalse(isShiftDown)
        }
    }

    private fun assertKeyDownIsTrueWithKeyDown(key: Key) {
        rule.performKeyInput {
            assertFalse(isKeyDown(key))
            withKeyDown(key) { assertTrue(isKeyDown(key)) }
            assertFalse(isKeyDown(key))
        }
    }
}
