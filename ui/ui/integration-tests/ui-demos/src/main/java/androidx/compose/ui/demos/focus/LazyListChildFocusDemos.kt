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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Preview(widthDp = 200, heightDp = 400)
@Composable
fun LazyListChildFocusDemos() {
    LazyColumn {
        stickyHeader { Text("Default Compose Behavior") }
        item {
            LazyRow {
                items(10) {
                    FocusableBox()
                }
            }
        }

        stickyHeader { Text("Direct Focus to First Child") }
        item {
            val firstItem = remember { FocusRequester() }
            val firstItemModifier = Modifier.focusRequester(firstItem)
            val coroutineScope = rememberCoroutineScope()
            val state = rememberLazyListState()
            LazyRow(
                Modifier
                    .onFocusChanged {
                        if (it.isFocused) coroutineScope.launch {
                            state.animateScrollToItem(0)
                            firstItem.requestFocus()
                        }
                    }
                    .focusable(),
                state,
            ) {
                items(10) {
                    FocusableBox(if (it == 0) firstItemModifier else Modifier)
                }
            }
        }

        stickyHeader { Text("Direct Focus to previously Focused Child") }
        item {
            var previouslyFocusedItem: FocusRequester? by remember { mutableStateOf(null) }
            LazyRow(
                Modifier
                    .onFocusChanged { if (it.isFocused) previouslyFocusedItem?.requestFocus() }
                    .then(previouslyFocusedItem?.let { Modifier.focusable() } ?: Modifier)
            ) {
                items(10) {
                    val focusRequester = remember { FocusRequester() }
                    FocusableBox(Modifier
                        .onFocusChanged { if (it.isFocused) previouslyFocusedItem = focusRequester }
                        .focusRequester(focusRequester)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusableBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    var borderColor by remember { mutableStateOf(Color.Black) }
    Box(
        modifier = modifier
            .size(100.dp)
            .padding(2.dp)
            .onFocusChanged { borderColor = if (it.isFocused) Color.Red else Color.Black }
            .border(2.dp, borderColor)
            .focusable(),
        content = content
    )
}
