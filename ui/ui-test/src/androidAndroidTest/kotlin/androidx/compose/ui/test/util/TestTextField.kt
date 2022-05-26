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

package androidx.compose.ui.test.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.util.TestTextField.Content
import androidx.compose.ui.test.util.TestTextField.Height
import androidx.compose.ui.test.util.TestTextField.Tag
import androidx.compose.ui.test.util.TestTextField.Width
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

object TestTextField {
    const val Width = 400.0f
    const val Height = 300.0f
    const val Tag = "TestTextField"
    const val Content = "This text was typed:"
}

@Composable
fun TestTextField(
    width: Float = Width,
    height: Float = Height,
    tag: String = Tag,
    textContent: String = Content
) {
    Column(Modifier.padding(20.dp)) {
        val textState = remember { mutableStateOf(TextFieldValue()) }
        TextField(
            modifier = Modifier.padding(20.dp).testTag(tag).requiredSize(width.dp, height.dp),
            keyboardActions = KeyboardActions.Default,
            value = textState.value,
            onValueChange = { textState.value = it }
        )
        Text("$textContent ${textState.value.text}")
    }
}