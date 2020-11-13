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
import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun SoftWrapDemo() {
    ScrollableColumn {
        CoreTextFieldWithWrap(false)
        CoreTextFieldWithWrap(true)
    }
}

@Composable
@OptIn(InternalTextApi::class)
private fun CoreTextFieldWithWrap(softWrap: Boolean) {
    val state = savedInstanceState(saver = TextFieldValue.Saver) {
        TextFieldValue("abc ".repeat(20))
    }
    CoreTextField(
        modifier = demoTextFieldModifiers.defaultMinSizeConstraints(100.dp),
        value = state.value,
        softWrap = softWrap,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        cursorColor = Color.Red
    )
}