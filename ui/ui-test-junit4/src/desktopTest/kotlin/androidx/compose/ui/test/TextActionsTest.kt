/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Rule
import org.junit.Test

/**
 * Tests the text-actions functionality of the test framework.
 */
@OptIn(ExperimentalTestApi::class)
class TextActionsTest {

    @get:Rule
    val rule = createComposeRule()

    @Composable
    fun TestTextField(text: String) {
        var value by remember { mutableStateOf(TextFieldValue(text)) }
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.testTag("tag")
        )
    }

    @Test
    fun testPerformTextClearance() {
        rule.setContent {
            TestTextField("hello")
        }

        with(rule.onNodeWithTag("tag")){
            assertTextEquals("hello")
            performTextClearance()
            assertTextEquals("")
        }
    }

    @Test
    fun testPerformTextReplacement() {
        rule.setContent {
            TestTextField("hello")
        }

        with(rule.onNodeWithTag("tag")){
            assertTextEquals("hello")
            performTextReplacement("compose")
            assertTextEquals("compose")
        }
    }

    @Test
    fun testPerformTextInput() {
        rule.setContent {
            TestTextField("hello")
        }

        with(rule.onNodeWithTag("tag")){
            assertTextEquals("hello")
            performTextInput(" compose")
            assertTextEquals("hello compose")
        }
    }

    @Test
    fun testPerformTextInputSelection() {
        rule.setContent {
            TestTextField("hello")
        }

        with(rule.onNodeWithTag("tag")){
            assertTextEquals("hello")
            performTextInputSelection(TextRange(0))
            performTextInput("compose ")
            assertTextEquals("compose hello")
        }
    }
}