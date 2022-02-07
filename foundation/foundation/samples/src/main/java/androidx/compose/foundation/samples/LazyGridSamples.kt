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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun LazyVerticalGridSample() {
    val itemsList = (0..5).toList()
    val itemsIndexedList = listOf("A", "B", "C")

    val itemModifier = Modifier.border(1.dp, Color.Blue).height(80.dp).wrapContentSize()

    LazyVerticalGrid(
        cells = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemsList) {
            Text("Item is $it", itemModifier)
        }

        item {
            Text("Single item", itemModifier)
        }

        itemsIndexed(itemsIndexedList) { index, item ->
            Text("Item at index $index is $item", itemModifier)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun LazyVerticalGridSpanSample() {
    val sections = (0 until 25).toList().chunked(5)
    LazyVerticalGrid(
        cells = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sections.forEachIndexed { index, items ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "This is section $index",
                    Modifier.border(1.dp, Color.Gray).height(80.dp).wrapContentSize()
                )
            }
            items(
                items,
                // not required as it is the default
                span = { GridItemSpan(1) }
            ) {
                Text(
                    "Item $it",
                    Modifier.border(1.dp, Color.Blue).height(80.dp).wrapContentSize()
                )
            }
        }
    }
}
