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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun BasicTextFieldMinMaxDemo() {
    LazyColumn {
        item {
            TagLine("empty text, no maxLines")
            TextFieldWithMaxLines("", maxLines = Int.MAX_VALUE)
        }
        item {
            TagLine("maxLines == line count")
            TextFieldWithMaxLines("abc", maxLines = 1)
        }
        item {
            TagLine("empty text, maxLines > line count")
            TextFieldWithMaxLines("", maxLines = 2)
        }
        item {
            TagLine("maxLines > line count")
            TextFieldWithMaxLines("abc", maxLines = 4)
        }
        item {
            TagLine("maxLines < line count")
            TextFieldWithMaxLines("abc".repeat(20), maxLines = 1)
        }
    }
}

@Composable
private fun TextFieldWithMaxLines(str: String? = null, maxLines: Int) {
    val state = rememberSaveable { mutableStateOf(str ?: "abc ".repeat(20)) }
    BasicTextField(
        modifier = demoTextFieldModifiers.clipToBounds(),
        value = state.value,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        cursorBrush = SolidColor(Color.Red),
        maxLines = maxLines
    )
}