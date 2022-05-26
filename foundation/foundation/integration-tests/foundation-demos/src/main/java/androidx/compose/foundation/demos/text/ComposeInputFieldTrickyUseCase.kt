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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun InputFieldTrickyUseCase() {
    LazyColumn {
        item {
            TagLine(tag = "don't set if non number is added")
            RejectNonDigits()
        }

        item {
            TagLine(tag = "always clear composition")
            RejectComposition()
        }
    }
}

@Composable
private fun RejectNonDigits() {
    val state = rememberSaveable { mutableStateOf("") }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = state.value,
        textStyle = TextStyle(fontSize = 32.sp),
        onValueChange = {
            if (it.all { text -> text.isDigit() }) {
                state.value = it
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun RejectComposition() {
    val state = rememberSaveable { mutableStateOf(({ "" })()) }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = state.value,
        textStyle = TextStyle(fontSize = 32.sp),
        onValueChange = {
            state.value = it
        }
    )
}