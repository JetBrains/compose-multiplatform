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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun TextFieldFocusTransition() {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LazyColumn {
        itemsIndexed(focusRequesters) { index, item ->
            val nextIndex = if (index == focusRequesters.size - 1) 0 else index + 1
            TextFieldWithFocusRequesters(item, focusRequesters[nextIndex])
        }
    }
}

@Composable
private fun TextFieldWithFocusRequesters(
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester
) {
    val state = rememberSaveable { mutableStateOf("Focus Transition Test") }
    var color by remember { mutableStateOf(Black) }

    BasicTextField(
        value = state.value,
        modifier = demoTextFieldModifiers
            .onFocusChanged { color = if (it.isFocused) Red else Black }
            .focusRequester(focusRequester)
            .focusProperties { next = nextFocusRequester },
        textStyle = TextStyle(color = color, fontSize = 32.sp),
        onValueChange = { state.value = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}
