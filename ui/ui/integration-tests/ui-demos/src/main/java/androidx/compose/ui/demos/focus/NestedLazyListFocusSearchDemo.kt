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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(widthDp = 200, heightDp = 400)
@Composable
fun NestedLazyListFocusSearchDemo() {
    Column {
        Text("Use the DPad to navigate through these nested lazy lists.")
        LazyColumn {
            items(20) { verticalIndex ->
                LazyRow {
                    items(5) { horizontalIndex ->
                        var color by remember { mutableStateOf(White) }
                        Text(
                            text = "$verticalIndex,$horizontalIndex",
                            fontSize = 50.sp,
                            textAlign = Center,
                            modifier = Modifier
                                .size(100.dp)
                                .border(2.dp, Color.Gray)
                                .onFocusChanged { color = if (it.isFocused) Red else White }
                                .background(color)
                                .focusable()
                        )
                    }
                }
            }
        }
    }
}
