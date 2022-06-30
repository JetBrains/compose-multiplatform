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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.injectionscope.key.Common.assertTyped
import androidx.compose.ui.test.injectionscope.key.Common.performKeyInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.util.TestTextField
import androidx.compose.ui.test.util.TestTextField.Tag
import androidx.compose.ui.test.withKeyToggled
import androidx.compose.ui.test.withKeysToggled
import androidx.test.filters.MediumTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if the lock key methods in [KeyInjectionScope] like [KeyInjectionScope.isCapsLockOn] work.
 */
@MediumTest
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class LockKeysTest {

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
    fun lockKeys_startOff() = rule.performKeyInput { assertAllLockKeysOff() }

    @Test
    fun lockKeys_areOn_afterToggle() {
        toggleAllLockKeys()
        assertAllLockKeysOn()
    }

    @Test
    fun lockKeys_areOff_afterDoubleToggle() {
        toggleAllLockKeys()
        toggleAllLockKeys()
        assertAllLockKeysOff()
    }

    @Test
    fun allLockKeysOn_withKeysToggled() {
        rule.performKeyInput {
            withKeysToggled(
                listOf(Key.CapsLock, Key.NumLock, Key.ScrollLock)
            ) {
                assertTrue(isCapsLockOn)
                assertTrue(isNumLockOn)
                assertTrue(isScrollLockOn)
            }
        }
    }

    @Test
    fun lettersTyped_withCapsLockOn_areUppercase() {
        rule.performKeyInput {
            pressKey(Key.A)
            withKeyToggled(Key.CapsLock) {
                pressKey(Key.A)
                pressKey(Key.B)
            }
            pressKey(Key.B)
        }

        rule.assertTyped("aABb")
    }

    @Test
    fun withKeyToggled_turnsCapsLockOff_ifCapsLockAlreadyOn() {
        rule.performKeyInput {
            pressKey(Key.CapsLock)
            pressKey(Key.A)
            pressKey(Key.B)
            withKeyToggled(Key.CapsLock) {
                pressKey(Key.A)
                pressKey(Key.B)
            }
            pressKey(Key.A)
            pressKey(Key.B) }

        rule.assertTyped("ABabAB")
    }

    @Test
    fun numPadKeysPressed_withNumLockToggled_areNumbers() {
        rule.performKeyInput {
            pressKey(Key.NumPad0)
            withKeyToggled(Key.NumLock) {
                pressKey(Key.NumPad1)
                pressKey(Key.NumPad0)
            }
            pressKey(Key.NumPad1)
        }

        rule.assertTyped("10")
    }

    @Test
    fun withKeyToggled_turnsNumLockOff_ifNumLockAlreadyOn() {
        rule.performKeyInput {
            pressKey(Key.NumLock)
            pressKey(Key.NumPad0)
            pressKey(Key.NumPad1)
            withKeyToggled(Key.NumLock) {
                pressKey(Key.NumPad0)
            }
        }

        rule.assertTyped("01")
    }

    @Test
    fun scrollLockOn_isTrue_withKeyToggled() {
        rule.performKeyInput {
            assertFalse(isScrollLockOn)
            withKeyToggled(Key.ScrollLock) { assertTrue(isScrollLockOn) }
            assertFalse(isScrollLockOn)
        }
    }

    private fun toggleAllLockKeys() {
        rule.performKeyInput {
            pressKey(Key.CapsLock)
            pressKey(Key.NumLock)
            pressKey(Key.ScrollLock)
        }
    }

    private fun assertAllLockKeysOn() {
        rule.performKeyInput {
            assertTrue(isCapsLockOn)
            assertTrue(isNumLockOn)
            assertTrue(isScrollLockOn)
        }
    }

    private fun assertAllLockKeysOff() {
        rule.performKeyInput {
            assertFalse(isCapsLockOn)
            assertFalse(isNumLockOn)
            assertFalse(isScrollLockOn)
        }
    }
}
