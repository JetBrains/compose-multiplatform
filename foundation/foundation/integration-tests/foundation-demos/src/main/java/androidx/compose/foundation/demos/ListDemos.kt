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

package androidx.compose.foundation.demos

import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentColorAmbient
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.currentTextStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.ExperimentalLazyDsl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyRowFor
import androidx.compose.foundation.lazy.LazyRowForIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.demos.common.ComposableDemo
import kotlin.random.Random

val LazyListDemos = listOf(
    ComposableDemo("Simple column") { LazyColumnDemo() },
    ComposableDemo("Add/remove items") { ListAddRemoveItemsDemo() },
    ComposableDemo("Horizontal list") { LazyRowItemsDemo() },
    ComposableDemo("List with indexes") { ListWithIndexSample() },
    ComposableDemo("Pager-like list") { PagerLikeDemo() },
    ComposableDemo("Rtl list") { RtlListDemo() },
    ComposableDemo("LazyColumn DSL") { LazyColumnScope() },
    ComposableDemo("LazyRow DSL") { LazyRowScope() }
)

@Composable
private fun LazyColumnDemo() {
    LazyColumnFor(
        items = listOf(
            "Hello,", "World:", "It works!", "",
            "this one is really long and spans a few lines for scrolling purposes",
            "these", "are", "offscreen"
        ) + (1..100).map { "$it" }
    ) {
        Text(text = it, fontSize = 80.sp)

        if (it.contains("works")) {
            Text("You can even emit multiple components per item.")
        }
    }
}

@Composable
private fun ListAddRemoveItemsDemo() {
    var numItems by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(0) }
    Column {
        Row {
            val buttonModifier = Modifier.padding(8.dp)
            Button(modifier = buttonModifier, onClick = { numItems++ }) { Text("Add") }
            Button(modifier = buttonModifier, onClick = { numItems-- }) { Text("Remove") }
            Button(modifier = buttonModifier, onClick = { offset++ }) { Text("Offset") }
        }
        Column {
            LazyColumnFor((1..numItems).map { it + offset }.toList()) {
                Text("$it", style = currentTextStyle().copy(fontSize = 20.sp))
            }
        }
    }
}

@Composable
fun Button(modifier: Modifier, onClick: () -> Unit, children: @Composable () -> Unit) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        backgroundColor = Color(0xFF6200EE),
        paddingStart = 16.dp,
        paddingEnd = 16.dp,
        paddingTop = 8.dp,
        paddingBottom = 8.dp
    ) {
        Providers(ContentColorAmbient provides Color.White) {
            children()
        }
    }
}

@Composable
private fun LazyRowItemsDemo() {
    LazyRowFor(items = (1..1000).toList()) {
        Square(it)
    }
}

@Composable
private fun Square(index: Int) {
    val width = remember { Random.nextInt(50, 150).dp }
    Box(
        Modifier.preferredWidth(width).fillMaxHeight(),
        backgroundColor = colors[index % colors.size],
        gravity = ContentGravity.Center
    ) {
        Text(index.toString())
    }
}

@Composable
private fun ListWithIndexSample() {
    val friends = listOf("Alex", "John", "Danny", "Sam")
    Column {
        LazyRowForIndexed(friends, Modifier.fillMaxWidth()) { index, friend ->
            Text("$friend at index $index", Modifier.padding(16.dp))
        }
        LazyColumnForIndexed(friends, Modifier.fillMaxWidth()) { index, friend ->
            Text("$friend at index $index", Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun RtlListDemo() {
    Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
        LazyRowForIndexed((0..100).toList(), Modifier.fillMaxWidth()) { index, item ->
            Text(
                "$item", Modifier
                    .size(100.dp)
                    .background(if (index % 2 == 0) Color.LightGray else Color.Transparent)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PagerLikeDemo() {
    val pages = listOf(Color.LightGray, Color.White, Color.DarkGray)
    LazyRowFor(pages) {
        Spacer(Modifier.fillParentMaxSize().background(it))
    }
}

private val colors = listOf(
    Color(0xFFffd7d7.toInt()),
    Color(0xFFffe9d6.toInt()),
    Color(0xFFfffbd0.toInt()),
    Color(0xFFe3ffd9.toInt()),
    Color(0xFFd0fff8.toInt())
)

@Composable
@OptIn(ExperimentalLazyDsl::class)
private fun LazyColumnScope() {
    LazyColumn {
        items((1..10).toList()) {
            Text("$it", fontSize = 40.sp)
        }

        item {
            Text("Single item", fontSize = 40.sp)
        }

        val items = listOf("A", "B", "C")
        itemsIndexed(items) { index, item ->
            Text("Item $item has index $index", fontSize = 40.sp)
        }
    }
}

@Composable
@OptIn(ExperimentalLazyDsl::class)
private fun LazyRowScope() {
    LazyRow {
        items((1..10).toList()) {
            Text("$it", fontSize = 40.sp)
        }

        item {
            Text("Single item", fontSize = 40.sp)
        }

        val items = listOf(Color.Cyan, Color.Blue, Color.Magenta)
        itemsIndexed(items) { index, item ->
            Box(modifier = Modifier.background(item).size(40.dp), gravity = ContentGravity.Center) {
                Text("$index", fontSize = 30.sp)
            }
        }
    }
}
