/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScrollableLazyRowFocusDemo() {
    val state = rememberLazyListState()
    var reverseLayout by remember { mutableStateOf(false) }
    Column {
        Text("Use the DPad or Tab keys to move focus in the Row")
        LazyRow(state = state, reverseLayout = reverseLayout) {
            items(count = 10) { FocusableBox(it.toString()) }
            items(count = 10) { NonFocusableBox((it + 10).toString()) }
            items(count = 10) { FocusableBox((it + 20).toString()) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "ReverseLayout")
            Checkbox(reverseLayout, { reverseLayout = it })
        }
    }
}

@Composable
private fun FocusableBox(text: String, modifier: Modifier = Modifier) {
    var color by remember { mutableStateOf(Color.White) }
    Text(
        text = text,
        fontSize = 50.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .size(100.dp)
            .border(2.dp, Color.Black)
            .onFocusChanged { color = if (it.isFocused) Color.Red else Color.White }
            .background(color)
            .focusable()
    )
}

@Composable
private fun NonFocusableBox(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 50.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .size(100.dp)
            .border(2.dp, Color.Black)
            .background(Color.White)
    )
}
