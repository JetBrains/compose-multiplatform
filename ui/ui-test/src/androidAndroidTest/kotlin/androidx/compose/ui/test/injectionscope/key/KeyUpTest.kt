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

package androidx.compose.ui.test.injectionscope.key

import androidx.compose.testutils.expectError
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.injectionscope.key.Common.assertTyped
import androidx.compose.ui.test.injectionscope.key.Common.performKeyInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.util.TestTextField
import androidx.test.filters.MediumTest
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [KeyInjectionScope.keyUp] works
 */
@MediumTest
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class KeyUpTest {
    companion object {
        private val A = Key.A
    }

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // Set content to a simple text field.
        rule.setContent {
            TestTextField()
        }
    }

    @Test
    fun upWithoutDown_throwsIllegalStateException() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send key up event, Key\\($A\\) is not pressed down."
        ) {
            rule.performKeyInput { keyUp(A) }
        }
    }

    @Test
    fun doubleUp_throwsIllegalStateException() {
        rule.performKeyInput { keyDown(A) }
        rule.performKeyInput { keyUp(A) }
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send key up event, Key\\($A\\) is not pressed down."
        ) {
            rule.performKeyInput { keyUp(A) }
        }
    }

    @Test
    fun upKey_isNotDown() {
        rule.performKeyInput {
            keyDown(A)
            keyUp(A)
            assertFalse(isKeyDown(A))
        }
    }

    @Test
    fun letterDownAfterShiftUp_typesLowercaseLetter() {
        rule.onNodeWithTag(TestTextField.Tag).performClick()
        rule.performKeyInput {
            keyDown(Key.ShiftLeft)
            keyUp(Key.ShiftLeft)
            keyDown(A)
        }
        rule.assertTyped("a")
    }
}
