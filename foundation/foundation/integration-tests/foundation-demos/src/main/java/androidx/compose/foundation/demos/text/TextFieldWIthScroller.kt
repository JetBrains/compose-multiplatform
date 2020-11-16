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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

private val LongText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam pellentesque" +
    " arcu quis diam malesuada pulvinar. In id condimentum metus. Suspendisse potenti. " +
    "Praesent arcu tortor, ultrices ut vehicula sit amet, accumsan id sem."

@Composable
fun ScrollableTextFieldDemo() {
    ScrollableColumn {
        TagLine("Multiline with 200.dp height")
        MultilineTextField()
        TagLine("Single line with 200.dp width")
        SingleLineTextField()
    }
}

@Composable
fun MultilineTextField() {
    val state = remember { mutableStateOf(LongText) }
    BasicTextField(
        value = state.value,
        onValueChange = { state.value = it },
        modifier = demoTextFieldModifiers.size(200.dp),
        singleLine = false,
        textStyle = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun SingleLineTextField() {
    val state = remember { mutableStateOf(LongText) }
    BasicTextField(
        value = state.value,
        onValueChange = { state.value = it },
        modifier = demoTextFieldModifiers.width(200.dp),
        singleLine = true,
        textStyle = TextStyle(fontSize = fontSize8)
    )
}