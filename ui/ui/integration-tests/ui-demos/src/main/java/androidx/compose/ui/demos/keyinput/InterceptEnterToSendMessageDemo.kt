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

package androidx.compose.ui.demos.keyinput

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun InterceptEnterToSendMessageDemo() {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    Scaffold(scaffoldState = scaffoldState) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Text(
                text = "Use a physical keyboard with this demo. As you enter text into this " +
                    "textfield, notice how the enter key is intercepted to show a snackbar." +
                    "You can use Ctrl+Enter or Shift+Enter to start a new line."
            )
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxSize()
                    .onPreviewKeyEvent {
                    @OptIn(ExperimentalComposeUiApi::class)
                    // Intercept all the "Enter" key events.
                    if (it.key == Key.Enter && it.type == KeyDown) {
                        // If this is a ctrl or shift key is pressed, we want to enter a new line.
                        // Sending ctrl+enter or shift+enter to the text field does not generate a
                        // new line, so we have to add the new line ourselves.
                        if (it.isCtrlPressed || it.isShiftPressed) {
                            textFieldValue = textFieldValue.insertString("\n")
                        } else {
                            // Perform Send Message and clear the textfield.
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar("Message is sent")
                            }
                            textFieldValue = TextFieldValue("")
                        }
                        // Consume the key event so that it is not propagated further.
                        true
                    } else {
                        // Let all other key events pass through.
                        false
                    }
                },
                textStyle = TextStyle(color = Color.Blue)
            )
        }
    }
}

private fun TextFieldValue.insertString(value: String): TextFieldValue {
    val cursorLocation = selection.start + value.length
    return copy(
        annotatedString.replaceRange(selection.start, selection.end, value).toString(),
        TextRange(cursorLocation, cursorLocation)
    )
}