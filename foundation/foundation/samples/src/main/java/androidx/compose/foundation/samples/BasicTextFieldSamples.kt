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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Sampled
@Composable
fun BasicTextFieldSample() {
    var value by savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }
    BasicTextField(
        value = value,
        onValueChange = {
            // it is crucial that the update is fed back into BasicTextField in order to
            // see updates on the text
            value = it
        }
    )
}

@Sampled
@Composable
fun BasicTextFieldWithStringSample() {
    var value by savedInstanceState { "initial value" }
    BasicTextField(
        value = value,
        onValueChange = {
            // it is crucial that the update is fed back into BasicTextField in order to
            // see updates on the text
            value = it
        }
    )
}

@Sampled
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PlaceholderBasicTextFieldSample() {
    var value by savedInstanceState { "initial value" }
    Box {
        BasicTextField(
            value = value,
            onValueChange = { value = it }
        )
        if (value.isEmpty()) {
            Text(text = "Placeholder")
        }
    }
}