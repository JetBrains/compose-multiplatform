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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp

@Composable
@OptIn(ExperimentalFocus::class)
fun TextFieldFocusTransition() {
    val focusRequesters = List(6) { FocusRequester() }

    ScrollableColumn {
        TextFieldWithFocusRequesters(focusRequesters[0], focusRequesters[1])
        TextFieldWithFocusRequesters(focusRequesters[1], focusRequesters[2])
        TextFieldWithFocusRequesters(focusRequesters[2], focusRequesters[3])
        TextFieldWithFocusRequesters(focusRequesters[3], focusRequesters[4])
        TextFieldWithFocusRequesters(focusRequesters[4], focusRequesters[5])
        TextFieldWithFocusRequesters(focusRequesters[5], focusRequesters[0])
    }
}

@OptIn(ExperimentalFocus::class)
@Composable
private fun TextFieldWithFocusRequesters(
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester
) {
    val state = savedInstanceState { "Focus Transition Test" }
    var color by remember { mutableStateOf(Black) }

    BasicTextField(
        value = state.value,
        modifier = demoTextFieldModifiers
            .focusObserver { color = if (it.isFocused) Red else Black }
            .focusRequester(focusRequester),
        textStyle = TextStyle(color = color, fontSize = 32.sp),
        onValueChange = { state.value = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        onImeActionPerformed = { if (it == ImeAction.Next) nextFocusRequester.requestFocus() }
    )
}
