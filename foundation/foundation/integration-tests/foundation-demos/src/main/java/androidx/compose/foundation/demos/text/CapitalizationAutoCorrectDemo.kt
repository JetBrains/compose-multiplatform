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

import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalTextApi::class)
private val KeyboardOptionsList = listOf(
    ImeOptionsData(
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Characters
        ),

        name = "Capitalize Characters"
    ),
    ImeOptionsData(
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words
        ),
        name = "Capitalize Words"
    ),
    ImeOptionsData(
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences
        ),
        name = "Capitalize Sentences"
    ),
    ImeOptionsData(
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            autoCorrect = true
        ),
        name = "AutoCorrect On"
    ),
    ImeOptionsData(
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            autoCorrect = false
        ),
        name = "AutoCorrect Off"
    )
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun CapitalizationAutoCorrectDemo() {
    LazyColumn {
        items(KeyboardOptionsList) { data ->
            TagLine(tag = data.name)
            MyTextField(data)
        }
    }
}

@Composable
@OptIn(
    ExperimentalTextApi::class,
    InternalTextApi::class
)
private fun MyTextField(data: ImeOptionsData) {
    val controller = remember { mutableStateOf<SoftwareKeyboardController?>(null) }
    val state = savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }
    BasicTextField(
        modifier = demoTextFieldModifiers.defaultMinSizeConstraints(100.dp),
        value = state.value,
        keyboardOptions = data.keyboardOptions,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        onTextInputStarted = { controller.value = it },
        onImeActionPerformed = {
            controller.value?.hideSoftwareKeyboard()
        },
        cursorColor = Color.Red
    )
}