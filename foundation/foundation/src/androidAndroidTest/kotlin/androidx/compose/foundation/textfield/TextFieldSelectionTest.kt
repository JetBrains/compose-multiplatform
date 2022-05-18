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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test

class TextFieldSelectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun readOnlyTextField_showsSelectionHandles() {
        val testTag = "text field"
        val textFieldValue = mutableStateOf("text text text")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(testTag),
                readOnly = true
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        // make selection
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.waitForIdle()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }
}