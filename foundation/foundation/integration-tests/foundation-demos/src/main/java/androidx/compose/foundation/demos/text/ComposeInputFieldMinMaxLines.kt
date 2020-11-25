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

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextStyle

@Composable
fun BasicTextFieldMinMaxDemo() {
    ScrollableColumn {
        TagLine("empty text, no maxLines")
        TextFieldWithMaxLines("", maxLines = Int.MAX_VALUE)
        TagLine("maxLines == line count")
        TextFieldWithMaxLines("abc", maxLines = 1)
        TagLine("empty text, maxLines > line count")
        TextFieldWithMaxLines("", maxLines = 2)
        TagLine("maxLines > line count")
        TextFieldWithMaxLines("abc", maxLines = 4)
        TagLine("maxLines < line count")
        TextFieldWithMaxLines("abc".repeat(20), maxLines = 1)
    }
}

@Composable
@OptIn(InternalTextApi::class)
private fun TextFieldWithMaxLines(str: String? = null, maxLines: Int) {
    val state = savedInstanceState { str ?: "abc ".repeat(20) }
    BasicTextField(
        modifier = demoTextFieldModifiers.clipToBounds(),
        value = state.value,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        cursorColor = Color.Red,
        maxLines = maxLines
    )
}