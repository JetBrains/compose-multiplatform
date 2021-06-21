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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SoftwareKeyboardControllerDemo() {
    Column(Modifier.padding(16.dp)) {
        var isHidden by remember { mutableStateOf(true) }

        val focusRequester = remember { FocusRequester() }

        val text = if (isHidden) {
            "Click on TextField to show keyboard, even after hiding"
        } else {
            "Keyboard shown (input ignored)"
        }

        BasicTextField(
            value = text,
            onValueChange = {},
            textStyle = TextStyle.Default.copy(fontSize = 18.sp),
            modifier = Modifier
                .focusRequester(focusRequester)
                .height(200.dp)
        )

        val keyboardController = LocalSoftwareKeyboardController.current
        Button(
            onClick = {
                isHidden = true
                keyboardController?.hide()
            },
            enabled = !isHidden,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("hideSoftwareKeyboard()")
        }
        Button(
            onClick = {
                isHidden = false
                focusRequester.requestFocus()
                keyboardController?.show()
            },
            enabled = isHidden
        ) {
            Text("showSoftwareKeyboard()")
        }
    }
}
