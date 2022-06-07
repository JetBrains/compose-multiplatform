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

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal class ImeOptionsData(
    val keyboardOptions: KeyboardOptions,
    val singleLine: Boolean = false,
    val name: String,
)

private val ImeOptionsList = listOf(
    ImeOptionsData(
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text
        ),
        name = "singleLine/Text"
    ),
    ImeOptionsData(
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text
        ),
        name = "multiLine/Text"
    ),
    ImeOptionsData(
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        name = "singleLine/Text/Search"
    ),
    ImeOptionsData(
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        name = "multiLine/Text/Search"
    ),
    ImeOptionsData(
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        name = "singleLine/Number"
    ),
    ImeOptionsData(
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        name = "multiLine/Number"
    ),
    ImeOptionsData(
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Send
        ),
        name = "singleLine/Number/Send"
    ),
    ImeOptionsData(
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Send
        ),
        name = "multiLine/Number/Send"
    ),
    ImeOptionsData(
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        name = "singleLine/Decimal"
    ),
    ImeOptionsData(
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        name = "multiLine/Decimal"
    )
)

@Preview
@Composable
fun ImeSingleLineDemo() {
    LazyColumn {
        items(ImeOptionsList) {
            TagLine(tag = "${it.name}")
            MyTextField(it)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MyTextField(data: ImeOptionsData) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    BasicTextField(
        modifier = demoTextFieldModifiers.defaultMinSize(100.dp),
        value = state.value,
        keyboardOptions = data.keyboardOptions,
        keyboardActions = KeyboardActions { keyboardController?.hide() },
        singleLine = data.singleLine,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        cursorBrush = SolidColor(Color.Red)
    )
}