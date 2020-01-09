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

package androidx.compose.benchmark.siblings

import androidx.compose.Composable
import androidx.compose.Pivotal
import androidx.compose.key
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.text.TextStyle
import kotlin.random.Random

@Composable
fun PivotalItemRow(@Pivotal item: Item) {
    val color = when (item.id % 3) {
        0 -> Color.Blue
        1 -> Color.Black
        else -> Color.Magenta
    }
    Row(LayoutWidth.Fill) {
        ColoredRect(color = color, width = 16.dp, height = 16.dp)
        Text(text = "${item.id}", style = TextStyle(color = color))
    }
}

@Composable
fun ItemRow(item: Item) {
    // the complexity of this will influence the benchmark a lot because if
    // identity doesn't influence what the component looks like, it's not
    // very important to track it.
    val color = when (item.id % 3) {
        0 -> Color.Blue
        1 -> Color.Black
        else -> Color.Magenta
    }
    Row(LayoutWidth.Fill) {
        ColoredRect(color = color, width = 16.dp, height = 16.dp)
        Text(text = "${item.id}", style = TextStyle(color = color))
    }
}

data class Item(val id: Int)

enum class IdentityType { Pivotal, Index, Key }

enum class ReorderType {
    Shuffle, ShiftRight, ShiftLeft, Swap,
    AddEnd, RemoveEnd,
    AddStart, RemoveStart,
    AddMiddle, RemoveMiddle
}

fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (to < from) return move(to, from)
    if (from == to) return this
    val item = get(from)
    val currentItem = get(to)
    val left = if (from > 0) subList(0, from) else emptyList()
    val right = if (to < size) subList(to + 1, size) else emptyList()
    val middle = if (to - from > 1) subList(from + 1, to) else emptyList()
    return left + listOf(currentItem) + middle + listOf(item) + right
}

fun <T> List<T>.update(reorderType: ReorderType, random: Random, factory: (Int) -> T): List<T> {
    // NOTE: might be some off by one errors in here :)
    @Suppress("ReplaceSingleLineLet")
    return when (reorderType) {
        ReorderType.Shuffle -> shuffled(random)
        ReorderType.ShiftRight -> listOf(get(size - 1)) + subList(0, size - 1)
        ReorderType.ShiftLeft -> subList(1, size) + listOf(get(0))
        ReorderType.Swap -> move(random.nextInt(size), random.nextInt(size))
        ReorderType.AddEnd -> this + listOf(factory(size))
        ReorderType.RemoveEnd -> dropLast(1)
        ReorderType.AddStart -> listOf(factory(size)) + this
        ReorderType.RemoveStart -> drop(1)
        ReorderType.AddMiddle -> random.nextInt(size).let {
            subList(0, it) + listOf(factory(size)) + subList(it, size)
        }
        ReorderType.RemoveMiddle -> random.nextInt(size).let { filterIndexed { i, _ -> i == it } }
    }
}

@Composable
fun SiblingManagement(identity: IdentityType, items: List<Item>) {
    Column(LayoutHeight.Fill) {
        when (identity) {
            IdentityType.Pivotal -> {
                for (item in items) {
                    PivotalItemRow(item = item)
                }
            }
            IdentityType.Index -> {
                for (item in items) {
                    ItemRow(item = item)
                }
            }
            IdentityType.Key -> {
                for (item in items) {
                    key(v1 = item.id) {
                        ItemRow(item = item)
                    }
                }
            }
        }
    }
}
