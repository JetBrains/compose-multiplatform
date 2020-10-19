/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.integration.test.core.text

import androidx.compose.foundation.BaseTextField
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.InternalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.blinkingCursorEnabled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.textInputServiceFactory
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.ui.integration.test.RandomTextGenerator

class TextFieldToggleTextTestCase(
    private val textGenerator: RandomTextGenerator,
    private val textLength: Int,
    private val textNumber: Int,
    private val width: Dp,
    private val fontSize: TextUnit
) : LayeredComposeTestCase, ToggleableTestCase {

    private val textInputService = TextInputService(TestPlatformTextInputService())

    private val texts = mutableStateOf(
        List(textNumber) {
            textGenerator.nextParagraph(length = textLength)
        }
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun emitMeasuredContent() {
        for (text in texts.value) {
            BaseTextField(
                value = TextFieldValue(text),
                onValueChange = {},
                textStyle = TextStyle(color = Color.Black, fontSize = fontSize),
                modifier = Modifier.background(color = Color.Cyan).width(width)
            )
        }
    }

    @OptIn(InternalFoundationApi::class)
    @Composable
    override fun emitContentWrappers(content: @Composable () -> Unit) {
        // Override IME input connection since we are not interested in it, and it might cause
        // flakiness
        @Suppress("DEPRECATION_ERROR")
        @OptIn(InternalTextApi::class)
        textInputServiceFactory = {
            textInputService
        }
        @Suppress("DEPRECATION_ERROR")
        @OptIn(InternalTextApi::class)
        blinkingCursorEnabled = false
        Column(
            modifier = Modifier.preferredWidth(width)
        ) {
            content()
        }
    }

    override fun toggleState() {
        texts.value = List(textNumber) {
            textGenerator.nextParagraph(length = textLength)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    private class TestPlatformTextInputService : PlatformTextInputService {
        override fun startInput(
            value: TextFieldValue,
            keyboardType: KeyboardType,
            imeAction: ImeAction,
            keyboardOptions: KeyboardOptions,
            onEditCommand: (List<EditOperation>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) { /*do nothing*/ }
        override fun stopInput() { /*do nothing*/ }
        override fun showSoftwareKeyboard() { /*do nothing*/ }
        override fun hideSoftwareKeyboard() { /*do nothing*/ }
        override fun onStateUpdated(value: TextFieldValue) { /*do nothing*/ }
        override fun notifyFocusedRect(rect: Rect) { /*do nothing*/ }
    }
}