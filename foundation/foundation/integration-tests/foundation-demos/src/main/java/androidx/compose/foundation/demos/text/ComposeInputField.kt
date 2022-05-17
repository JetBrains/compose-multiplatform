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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Preview
@Composable
fun InputFieldDemo() {
    LazyColumn {
        item {
            TagLine(tag = "LTR Layout")
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TagLine(tag = "simple editing single line")
                    EditLine(singleLine = true)
                    TagLine(tag = "simple editing multi line")
                    EditLine(text = displayTextHindi)
                    TagLine(tag = "simple editing RTL")
                    EditLine(text = displayTextArabic)
                }
            }
        }
        item {
            TagLine(tag = "RTL Layout")
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TagLine(tag = "simple editing RTL")
                    EditLine()
                    EditLine(text = displayTextArabic)
                    EditLine(text = displayText)
                }
            }
        }
    }
}

@Composable
fun DialogInputFieldDemo(onNavigateUp: () -> Unit) {
    Dialog(onDismissRequest = onNavigateUp) {
        InputFieldDemo()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun EditLine(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    singleLine: Boolean = false,
    text: String = ""
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = rememberSaveable { mutableStateOf(text) }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = state.value,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions { keyboardController?.hide() },
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
    )
}

val demoTextFieldModifiers = Modifier
    .padding(6.dp)
    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
    .padding(6.dp)