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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.animation.smoothScrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayout
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.AmbientContentColor
import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.demos.PagingDemos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.random.Random

val LazyListDemos = listOf(
    ComposableDemo("Simple column") { LazyColumnDemo() },
    ComposableDemo("Add/remove items") { ListAddRemoveItemsDemo() },
    ComposableDemo("Hoisted state") { ListHoistedStateDemo() },
    ComposableDemo("Horizontal list") { LazyRowItemsDemo() },
    ComposableDemo("List with indexes") { ListWithIndexSample() },
    ComposableDemo("Pager-like list") { PagerLikeDemo() },
    ComposableDemo("Rtl list") { RtlListDemo() },
    ComposableDemo("LazyColumn DSL") { LazyColumnScope() },
    ComposableDemo("LazyRow DSL") { LazyRowScope() },
    ComposableDemo("Arrangements") { LazyListArrangements() },
    ComposableDemo("Reverse scroll direction") { ReverseLayout() },
    ComposableDemo("Nested lazy lists") { NestedLazyDemo() },
    ComposableDemo("LazyGrid") { LazyGridDemo() },
    PagingDemos
)

@Composable
private fun LazyColumnDemo() {
    LazyColumn {
        items(
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
        LazyColumn(Modifier.fillMaxWidth()) {
            items((1..numItems).map { it + offset }.toList()) {
                Text("$it", style = AmbientTextStyle.current.copy(fontSize = 40.sp))
            }
        }
    }
}

@OptIn(ExperimentalLayout::class)
@Composable
private fun ListHoistedStateDemo() {
    val interactionState = remember { InteractionState() }
    val state = rememberLazyListState(interactionState = interactionState)
    var lastScrollDescription: String by remember { mutableStateOf("") }
    Column {
        @Suppress("DEPRECATION")
        FlowRow {
            val buttonModifier = Modifier.padding(8.dp)
            val density = AmbientDensity.current
            val coroutineScope = rememberCoroutineScope()
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.snapToItemIndex(state.firstVisibleItemIndex - 1)
                    }
                }
            ) {
                Text("Previous")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.snapToItemIndex(state.firstVisibleItemIndex + 1)
                    }
                }
            ) {
                Text("Next")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    with(density) {
                        coroutineScope.launch {
                            val requestedScroll = 3000.dp.toPx()
                            lastScrollDescription = try {
                                val actualScroll = state.smoothScrollBy(requestedScroll)
                                "$actualScroll/$requestedScroll px"
                            } catch (_: CancellationException) {
                                "Interrupted!"
                            }
                        }
                    }
                }
            ) {
                Text("Scroll")
            }
        }
        Column {
            Text(
                "First item: ${state.firstVisibleItemIndex}, Last scroll: $lastScrollDescription",
                fontSize = 20.sp
            )
            Text(
                "Dragging: ${interactionState.contains(Interaction.Dragged)}, " +
                    "Flinging: ${state.isAnimationRunning}",
                fontSize = 20.sp
            )
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = state
        ) {
            items((0..1000).toList()) {
                Text("$it", style = AmbientTextStyle.current.copy(fontSize = 40.sp))
            }
        }
    }
}

@Composable
fun Button(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .background(Color(0xFF6200EE), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Providers(AmbientContentColor provides Color.White, content = content)
    }
}

@Composable
private fun LazyRowItemsDemo() {
    LazyRow {
        items((1..1000).toList()) {
            Square(it)
        }
    }
}

@Composable
private fun Square(index: Int) {
    val width = remember { Random.nextInt(50, 150).dp }
    Box(
        Modifier.preferredWidth(width).fillMaxHeight().background(colors[index % colors.size]),
        contentAlignment = Alignment.Center
    ) {
        Text(index.toString())
    }
}

@Composable
private fun ListWithIndexSample() {
    val friends = listOf("Alex", "John", "Danny", "Sam")
    Column {
        LazyRow(Modifier.fillMaxWidth()) {
            itemsIndexed(friends) { index, friend ->
                Text("$friend at index $index", Modifier.padding(16.dp))
            }
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(friends) { index, friend ->
                Text("$friend at index $index", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun RtlListDemo() {
    Providers(AmbientLayoutDirection provides LayoutDirection.Rtl) {
        LazyRow(Modifier.fillMaxWidth()) {
            itemsIndexed((0..100).toList()) { index, item ->
                Text(
                    "$item",
                    Modifier
                        .size(100.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.Transparent)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PagerLikeDemo() {
    val pages = listOf(Color.LightGray, Color.White, Color.DarkGray)
    LazyRow {
        items(pages) {
            Spacer(Modifier.fillParentMaxSize().background(it))
        }
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
            Box(
                modifier = Modifier.background(item).size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", fontSize = 30.sp)
            }
        }
    }
}

@Composable
private fun LazyListArrangements() {
    var count by remember { mutableStateOf(3) }
    var arrangement by remember { mutableStateOf(6) }
    Column {
        Row {
            Button(onClick = { count-- }) {
                Text("--")
            }
            Button(onClick = { count++ }) {
                Text("++")
            }
            Button(
                onClick = {
                    arrangement++
                    if (arrangement == Arrangements.size) {
                        arrangement = 0
                    }
                }
            ) {
                Text("Next")
            }
            Text("$arrangement ${Arrangements[arrangement]}")
        }
        Row {
            val item = @Composable {
                Box(
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .background(Color.Red)
                        .border(1.dp, Color.Cyan)
                )
            }
            ScrollableColumn(
                verticalArrangement = Arrangements[arrangement],
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                (1..count).forEach {
                    item()
                }
            }
            LazyColumn(
                verticalArrangement = Arrangements[arrangement],
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                items((1..count).toList()) {
                    item()
                }
            }
        }
    }
}

private val Arrangements = listOf(
    Arrangement.Center,
    Arrangement.Top,
    Arrangement.Bottom,
    Arrangement.SpaceAround,
    Arrangement.SpaceBetween,
    Arrangement.SpaceEvenly,
    Arrangement.spacedBy(40.dp),
    Arrangement.spacedBy(40.dp, Alignment.Bottom),
)

@Composable
fun ReverseLayout() {
    Column {
        val scrollState = rememberScrollState()
        val lazyState = rememberLazyListState()
        var count by remember { mutableStateOf(3) }
        var reverse by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { count -= 5 }) {
                Text("--")
            }
            Button(onClick = { count += 5 }) {
                Text("++")
            }
            Button(onClick = { reverse = !reverse }) {
                Text("=!")
            }
            Text("Scroll=${scrollState.value.toInt()}")
            Text(
                "Lazy=${lazyState.firstVisibleItemIndex}; " +
                    "${lazyState.firstVisibleItemScrollOffset}"
            )
        }
        Row {
            val item1 = @Composable { index: Int ->
                Text(
                    "$index",
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .background(Color.Red)
                        .border(1.dp, Color.Cyan)
                )
            }
            val item2 = @Composable { index: Int ->
                Text("After $index")
            }
            ScrollableColumn(
                reverseScrollDirection = reverse,
                verticalArrangement = if (reverse) Arrangement.Bottom else Arrangement.Top,
                scrollState = scrollState,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                if (reverse) {
                    (count downTo 1).forEach {
                        item2(it)
                        item1(it)
                    }
                } else {
                    (1..count).forEach {
                        item1(it)
                        item2(it)
                    }
                }
            }
            LazyColumn(
                reverseLayout = reverse,
                state = lazyState,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                items((1..count).toList()) {
                    item1(it)
                    item2(it)
                }
            }
        }
    }
}

@Composable
private fun NestedLazyDemo() {
    val item = @Composable { index: Int ->
        Box(
            Modifier.padding(16.dp).size(200.dp).background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            var state by savedInstanceState { 0 }
            Button(onClick = { state++ }) {
                Text("Index=$index State=$state")
            }
        }
    }
    LazyColumn {
        item {
            LazyRow {
                items(List(100) { it }) {
                    item(it)
                }
            }
        }
        items(List(100) { it }) {
            item(it)
        }
    }
}

@Composable
private fun LazyGridDemo() {
    val columnModes = listOf(
        GridCells.Fixed(3),
        GridCells.Adaptive(minSize = 60.dp)
    )
    var currentMode by remember { mutableStateOf(0) }
    Column {
        Button(
            modifier = Modifier.wrapContentSize(),
            onClick = {
                currentMode = (currentMode + 1) % columnModes.size
            }
        ) {
            Text("Switch mode")
        }
        LazyGridForMode(columnModes[currentMode])
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyGridForMode(mode: GridCells) {
    LazyVerticalGrid(
        cells = mode
    ) {
        items(
            items = (1..100).toList()
        ) {
            Text(
                text = "$it",
                fontSize = 20.sp,
                modifier = Modifier
                    .background(Color.Gray.copy(alpha = (it % 10) / 10f))
                    .padding(8.dp)
            )
        }
    }
}
