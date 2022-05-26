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

import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun InteractiveTextDemo() {
    TextOnClick()
}

@Preview
@Composable
fun TextOnClick() {
    val clickedOffset = remember { mutableStateOf(-1) }
    Column {
        Text("Clicked Offset: ${clickedOffset.value}")
        ClickableText(
            text = AnnotatedString("Click Me")
        ) { offset ->
            clickedOffset.value = offset
        }
    }
}