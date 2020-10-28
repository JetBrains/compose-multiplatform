/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.text.demos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

private val KEYBOARD_TYPES = listOf(
    Pair(KeyboardType.Text, "Text"),
    Pair(KeyboardType.Ascii, "Ascii"),
    Pair(KeyboardType.Number, "Number"),
    Pair(KeyboardType.Email, "Email"),
    Pair(KeyboardType.Phone, "Phone"),
    Pair(KeyboardType.Password, "Password"),
    Pair(KeyboardType.NumberPassword, "NumberPassword")
)

private val IME_ACTIONS = listOf(
    Pair(ImeAction.Unspecified, "Unspecified"),
    Pair(ImeAction.NoAction, "NoAction"),
    Pair(ImeAction.Go, "Go"),
    Pair(ImeAction.Search, "Search"),
    Pair(ImeAction.Send, "Send"),
    Pair(ImeAction.Next, "Next"),
    Pair(ImeAction.Done, "Done"),
    Pair(ImeAction.Previous, "Previous")
)

@Composable
fun InputFieldDemo() {
    ScrollableColumn {
        TagLine(tag = "simple editing")
        EditLine()
        TagLine(tag = "simple editing2")
        EditLine()

        for ((type, name) in KEYBOARD_TYPES) {
            key(name) {
                // key is needed because of b/154920561
                TagLine(tag = "Keyboard Type: $name")
                EditLine(keyboardType = type)
            }
        }

        for ((action, name) in IME_ACTIONS) {
            key(name) {
                // key is needed because of b/154920561
                TagLine(tag = "Ime Action: $name")
                EditLine(imeAction = action)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EditLine(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified
) {
    val controller = remember { mutableStateOf<SoftwareKeyboardController?>(null) }
    val state = savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = state.value,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        onTextInputStarted = { controller.value = it },
        onImeActionPerformed = {
            controller.value?.hideSoftwareKeyboard()
        }
    )
}

val demoTextFieldModifiers = Modifier
    .padding(6.dp)
    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
    .padding(6.dp)