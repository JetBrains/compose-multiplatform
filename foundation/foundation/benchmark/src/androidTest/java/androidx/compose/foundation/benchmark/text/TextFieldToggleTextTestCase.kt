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

package androidx.compose.foundation.benchmark.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.benchmark.RandomTextGenerator
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

class TextFieldToggleTextTestCase(
    private val textGenerator: RandomTextGenerator,
    private val textLength: Int,
    private val textNumber: Int,
    private val width: Dp,
    private val fontSize: TextUnit
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val textInputService = TextInputService(TestPlatformTextInputService())

    private val texts = List(textNumber) {
        mutableStateOf(textGenerator.nextParagraph(length = textLength))
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun MeasuredContent() {
        for (text in texts) {
            BasicTextField(
                value = text.value,
                onValueChange = {},
                textStyle = TextStyle(color = Color.Black, fontSize = fontSize),
                modifier = Modifier.background(color = Color.Cyan).requiredWidth(width)
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.width(width).verticalScroll(rememberScrollState())
        ) {
            CompositionLocalProvider(LocalTextInputService provides textInputService) {
                content()
            }
        }
    }

    override fun toggleState() {
        texts.forEach {
            it.value = textGenerator.nextParagraph(length = textLength)
        }
    }

    private class TestPlatformTextInputService : PlatformTextInputService {
        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<EditCommand>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) { /*do nothing*/ }
        override fun stopInput() { /*do nothing*/ }
        override fun showSoftwareKeyboard() { /*do nothing*/ }
        override fun hideSoftwareKeyboard() { /*do nothing*/ }
        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
            /*do nothing*/
        }
    }
}