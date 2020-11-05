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

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun CoreTextFieldMinMaxDemo() {
    ScrollableColumn {
        CoreTextFieldWithMaxLines("", maxLines = Int.MAX_VALUE) // empty text, no maxLines
        CoreTextFieldWithMaxLines("abc", maxLines = 1) // maxLines == line count
        CoreTextFieldWithMaxLines("", maxLines = 2) // empty text, maxLines > line count
        CoreTextFieldWithMaxLines("abc", maxLines = 4) // maxLines > line count
        CoreTextFieldWithMaxLines("abc".repeat(20), maxLines = 1) // maxLines < line count
    }
}

@Composable
@OptIn(InternalTextApi::class)
private fun CoreTextFieldWithMaxLines(str: String? = null, maxLines: Int) {
    val state = savedInstanceState(saver = TextFieldValue.Saver) {
        TextFieldValue(str ?: "abc ".repeat(20))
    }
    CoreTextField(
        modifier = demoTextFieldModifiers.clipToBounds(),
        value = state.value,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        cursorColor = Color.Red,
        maxLines = maxLines
    )
}