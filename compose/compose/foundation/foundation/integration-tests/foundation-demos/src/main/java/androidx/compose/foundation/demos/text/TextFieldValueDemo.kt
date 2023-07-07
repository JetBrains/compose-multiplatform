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

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun TextFieldValueDemo() {
    LazyColumn {
        item {
            TagLine("Empty callback")
            val textFieldValue1 = remember { TextFieldValue("") }
            BasicTextField(
                value = textFieldValue1,
                onValueChange = {},
                textStyle = TextStyle(fontSize = fontSize8),
                modifier = demoTextFieldModifiers
            )
        }
        item {
            TagLine("Regular string overload")
            var string by remember { mutableStateOf("") }
            BasicTextField(
                value = string,
                onValueChange = {
                    string = it
                },
                textStyle = TextStyle(fontSize = fontSize8),
                modifier = demoTextFieldModifiers
            )
        }
        item {
            TagLine("Reformat by uppercase ")
            var uppercaseValue by remember { mutableStateOf("") }
            BasicTextField(
                value = uppercaseValue,
                onValueChange = {
                    uppercaseValue = it.uppercase(java.util.Locale.US)
                },
                textStyle = TextStyle(fontSize = fontSize8),
                modifier = demoTextFieldModifiers
            )
        }
        item {
            TagLine("Clear text")
            var clearedValue by remember { mutableStateOf("") }
            BasicTextField(
                value = clearedValue,
                onValueChange = {
                    clearedValue = it
                },
                textStyle = TextStyle(fontSize = fontSize8),
                modifier = demoTextFieldModifiers
            )
            Button(onClick = { clearedValue = "" }) {
                Text("Clear")
            }
        }
        item {
            TagLine("Delayed callback")
            var text by remember { mutableStateOf("") }
            val handler = remember { Handler(Looper.getMainLooper()) }
            BasicTextField(
                value = text,
                onValueChange = {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ text = it }, 50)
                },
                textStyle = TextStyle(fontSize = fontSize8),
                modifier = demoTextFieldModifiers
            )
        }
    }
}
