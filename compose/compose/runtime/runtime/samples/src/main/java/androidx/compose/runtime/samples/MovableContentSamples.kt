/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember

@Composable
fun ItemView(@Suppress("UNUSED_PARAMETER") userId: Int) { }
typealias Item = Int

@Suppress("unused")
@Sampled
@Composable
fun MovableContentColumnRowSample(content: @Composable () -> Unit, vertical: Boolean) {
    val movableContent = remember(content as Any) { movableContentOf(content) }

    if (vertical) {
        Column {
            movableContent()
        }
    } else {
        Row {
            movableContent()
        }
    }
}

@Suppress("unused")
@Sampled
@Composable
fun MovableContentMultiColumnSample(items: List<Item>) {
    val itemMap = remember {
        mutableMapOf<Item, @Composable () -> Unit>()
    }
    val movableItems =
        items.map { item -> itemMap.getOrPut(item) { movableContentOf { ItemView(item) } } }

    val itemsPerColumn = 10
    val columns = items.size / itemsPerColumn + (if (items.size % itemsPerColumn == 0) 0 else 1)
    Row {
        repeat(columns) { column ->
            Column {
                val base = column * itemsPerColumn
                val end = minOf(base + itemsPerColumn, items.size)
                for (index in base until end) {
                    movableItems[index]()
                }
            }
        }
    }
}
