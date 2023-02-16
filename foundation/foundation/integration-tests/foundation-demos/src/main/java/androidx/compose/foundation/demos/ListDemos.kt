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
@file:SuppressLint("FrequentlyChangedStateReadInComposition")

package androidx.compose.foundation.demos

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.samples.StickyHeaderSample
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.demos.PagingDemos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    ComposableDemo("LazyColumn with sticky headers") { StickyHeaderSample() },
    ComposableDemo("Arrangements") { LazyListArrangements() },
    ComposableDemo("ReverseLayout and RTL") { ReverseLayoutAndRtlDemo() },
    ComposableDemo("Nested lazy lists") { NestedLazyDemo() },
    ComposableDemo("LazyGrid") { LazyGridDemo() },
    ComposableDemo("LazyGrid with Spacing") { LazyGridWithSpacingDemo() },
    ComposableDemo("Custom keys") { ReorderWithCustomKeys() },
    ComposableDemo("Fling Config") { LazyWithFlingConfig() },
    ComposableDemo("Item reordering") { PopularBooksDemo() },
    ComposableDemo("List drag and drop") { LazyColumnDragAndDropDemo() },
    ComposableDemo("Grid drag and drop") { LazyGridDragAndDropDemo() },
    ComposableDemo("Staggered grid") { LazyStaggeredGridDemo() },
    ComposableDemo("Animate item placement") { AnimateItemPlacementDemo() },
    PagingDemos
)

@Preview
@Composable
private fun LazyColumnDemo() {
    LazyColumn {
        items(
            items = listOf(
                "Hello,", "World:", "It works!", "",
                "this one is really long and spans a few lines for scrolling purposes",
                "these", "are", "offscreen"
            )
        ) {
            Text(text = it, fontSize = 80.sp)

            if (it.contains("works")) {
                Text("You can even emit multiple components per item.")
            }
        }
        items(100) {
            Text(text = "$it", fontSize = 80.sp)
        }
    }
}

@Preview
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
            items((1..numItems).map { it + offset }) {
                Text("$it", style = LocalTextStyle.current.copy(fontSize = 40.sp))
            }
        }
    }
}

@Preview
@Composable
private fun ListHoistedStateDemo() {
    val state = rememberLazyListState()
    var lastScrollDescription: String by remember { mutableStateOf("") }
    Column {
        val numItems = 10000
        Row {
            val buttonModifier = Modifier.padding(8.dp)
            val density = LocalDensity.current
            val coroutineScope = rememberCoroutineScope()
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.scrollToItem(state.firstVisibleItemIndex - 1)
                    }
                }
            ) {
                Text("Previous")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.scrollToItem(state.firstVisibleItemIndex + 1)
                    }
                }
            ) {
                Text("Next")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        val index = min(state.firstVisibleItemIndex + 500, numItems - 1)
                        state.animateScrollToItem(index)
                    }
                }
            ) {
                Text("+500")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        val index = max(state.firstVisibleItemIndex - 500, 0)
                        state.animateScrollToItem(index)
                    }
                }
            ) {
                Text("-500")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.animateScrollToItem(
                            state.firstVisibleItemIndex,
                            500
                        )
                    }
                }
            ) {
                Text("Offset")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    with(density) {
                        coroutineScope.launch {
                            val requestedScroll = 10000.dp.toPx()
                            lastScrollDescription = try {
                                val actualScroll = state.animateScrollBy(requestedScroll)
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
                "Dragging: ${state.interactionSource.collectIsDraggedAsState().value}, " +
                    "Flinging: ${state.isScrollInProgress}",
                fontSize = 20.sp
            )
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = state
        ) {
            items(numItems) {
                Text("$it", style = LocalTextStyle.current.copy(fontSize = 40.sp))
            }
        }
    }
}

@Preview
@Composable
private fun LazyRowItemsDemo() {
    LazyRow {
        items(1000) {
            Square(it)
        }
    }
}

@Composable
private fun Square(index: Int) {
    val width = remember { Random.nextInt(50, 150).dp }
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(colors[index % colors.size]),
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyRow(Modifier.fillMaxWidth()) {
            items(100) {
                Text(
                    "$it",
                    Modifier
                        .requiredSize(100.dp)
                        .background(if (it % 2 == 0) Color.LightGray else Color.Transparent)
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
            Spacer(
                Modifier
                    .fillParentMaxSize()
                    .background(it))
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
        items(10) {
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
        items(10) {
            Text("$it", fontSize = 40.sp)
        }

        item {
            Text("Single item", fontSize = 40.sp)
        }

        val items = listOf(Color.Cyan, Color.Blue, Color.Magenta)
        itemsIndexed(items) { index, item ->
            Box(
                modifier = Modifier
                    .background(item)
                    .requiredSize(40.dp),
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
                        .requiredHeight(200.dp)
                        .fillMaxWidth()
                        .background(Color.Red)
                        .border(1.dp, Color.Cyan)
                )
            }
            Column(
                verticalArrangement = Arrangements[arrangement],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(count) {
                    item()
                }
            }
            LazyColumn(
                verticalArrangement = Arrangements[arrangement],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                items(count) {
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
private fun ReverseLayoutAndRtlDemo() {
    val backgroundColor = Color(1f, .8f, .8f)
    Column {
        val scrollState = rememberScrollState()
        val lazyState = rememberLazyListState()
        var count by remember { mutableStateOf(10) }
        var reverse by remember { mutableStateOf(false) }
        var rtl by remember { mutableStateOf(false) }
        var column by remember { mutableStateOf(true) }
        val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr

        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { count = max(0, count - 5) }) {
                Text("--")
            }
            Button(onClick = { count += 5 }) {
                Text("++")
            }
            Column {
                Row {
                    Checkbox(checked = reverse, onCheckedChange = { reverse = it })
                    Text("reverse")
                }
                Row {
                    Checkbox(checked = rtl, onCheckedChange = { rtl = it })
                    Text("RTL")
                }
            }
            Column {
                Row {
                    RadioButton(selected = column, { column = true })
                    Text("Cols")
                }
                Row {
                    RadioButton(selected = !column, { column = false })
                    Text("Rows")
                }
            }
        }

        val itemModifier = if (column) {
            Modifier
                .heightIn(200.dp)
                .fillMaxWidth()
        } else {
            Modifier
                .widthIn(200.dp)
                .fillMaxHeight()
        }
        val item1 = @Composable { index: Int ->
            Text(
                "${index}A",
                itemModifier
                    .background(backgroundColor)
                    .border(1.dp, Color.Cyan)
            )
        }
        val item2 = @Composable { index: Int ->
            Text("${index}B")
        }

        @Composable
        fun NonLazyContent() {
            if (reverse) {
                (count - 1 downTo 0).forEach {
                    item2(it)
                    item1(it)
                }
            } else {
                (0 until count).forEach {
                    item1(it)
                    item2(it)
                }
            }
        }
        val lazyContent: LazyListScope.() -> Unit = {
            items(count) {
                item1(it)
                item2(it)
            }
        }

        if (column) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Column: scroll=${scrollState.value}", Modifier.weight(1f))
                Text(
                    "LazyColumn: index=${lazyState.firstVisibleItemIndex}, " +
                        "offset=${lazyState.firstVisibleItemScrollOffset}",
                    Modifier.weight(1f)
                )
            }
            Row {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Column(
                        verticalArrangement = if (reverse) Arrangement.Bottom else Arrangement.Top,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState, reverseScrolling = reverse)
                    ) {
                        NonLazyContent()
                    }
                }
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    LazyColumn(
                        reverseLayout = reverse,
                        state = lazyState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        content = lazyContent
                    )
                }
            }
        } else {
            Text("Row: scroll=${scrollState.value}")
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Row(
                    horizontalArrangement = if (reverse) Arrangement.End else Arrangement.Start,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .horizontalScroll(scrollState, reverseScrolling = reverse)
                ) {
                    NonLazyContent()
                }
            }
            Text(
                "LazyRow: index=${lazyState.firstVisibleItemIndex}, " +
                    "offset=${lazyState.firstVisibleItemScrollOffset}"
            )
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                LazyRow(
                    state = lazyState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = reverse,
                    content = lazyContent
                )
            }
        }
    }
}

@Composable
private fun NestedLazyDemo() {
    val item = @Composable { index: Int ->
        Box(
            Modifier
                .padding(16.dp)
                .requiredSize(200.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            var state by rememberSaveable { mutableStateOf(0) }
            Button(onClick = { state++ }) {
                Text("Index=$index State=$state")
            }
        }
    }
    LazyColumn {
        item {
            LazyRow {
                items(100) {
                    item(it)
                }
            }
        }
        items(100) {
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

@Composable
private fun LazyGridForMode(mode: GridCells) {
    LazyVerticalGrid(columns = mode) {
        items(100) {
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

@Composable
private fun LazyGridWithSpacingDemo() {
    val columnModes = listOf(
        GridCells.Fixed(3),
        GridCells.Adaptive(minSize = 60.dp),
        object : GridCells {
            // columns widths have ratio 1:1:2:3
            override fun Density.calculateCrossAxisCellSizes(
                availableSize: Int,
                spacing: Int,
            ): List<Int> {
                val totalSlots = 1 + 1 + 2 + 3
                val slotWidth = (availableSize - spacing * 3) / totalSlots
                return listOf(slotWidth, slotWidth, slotWidth * 2, slotWidth * 3)
            }
        }
    )
    var currentMode by remember { mutableStateOf(0) }
    var horizontalSpacing by remember { mutableStateOf(8) }
    var horizontalSpacingExpanded by remember { mutableStateOf(false) }
    var verticalSpacing by remember { mutableStateOf(8) }
    var verticalSpacingExpanded by remember { mutableStateOf(false) }
    Column {
        Row {
            Button(
                modifier = Modifier.wrapContentSize(),
                onClick = {
                    currentMode = (currentMode + 1) % columnModes.size
                }
            ) {
                Text("Switch mode")
            }
            Box {
                OutlinedButton(
                    modifier = Modifier.wrapContentSize(),
                    onClick = { verticalSpacingExpanded = true }
                ) {
                    Text("Vertical:\n$verticalSpacing dp")
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "expand")
                }
                DropdownMenu(
                    expanded = verticalSpacingExpanded,
                    onDismissRequest = { verticalSpacingExpanded = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            verticalSpacing = 0
                            verticalSpacingExpanded = false
                        }
                    ) {
                        Text("None")
                    }
                    DropdownMenuItem(
                        onClick = {
                            verticalSpacing = 8
                            verticalSpacingExpanded = false
                        }
                    ) {
                        Text("8 dp")
                    }
                    DropdownMenuItem(
                        onClick = {
                            verticalSpacing = 16
                            verticalSpacingExpanded = false
                        }
                    ) {
                        Text("16 dp")
                    }
                    DropdownMenuItem(
                        onClick = {
                            verticalSpacing = 32
                            verticalSpacingExpanded = false
                        }
                    ) {
                        Text("32 dp")
                    }
                }
            }

            Box {
                OutlinedButton(
                    modifier = Modifier.wrapContentSize(),
                    onClick = { horizontalSpacingExpanded = true }
                ) {
                    Text("Horizontal:\n$horizontalSpacing dp")
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "expand")
                }
                DropdownMenu(
                    expanded = horizontalSpacingExpanded,
                    onDismissRequest = { horizontalSpacingExpanded = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            horizontalSpacing = 0
                            horizontalSpacingExpanded = false
                        }
                    ) {
                        Text("None")
                    }
                    DropdownMenuItem(
                        onClick = {
                            horizontalSpacing = 8
                            horizontalSpacingExpanded = false
                        }
                    ) {
                        Text("8 dp")
                    }
                    DropdownMenuItem(
                        onClick = {
                            horizontalSpacing = 16
                            horizontalSpacingExpanded = false
                        }
                    ) {
                        Text("16 dp")
                    }
                    DropdownMenuItem(
                        onClick = {
                            horizontalSpacing = 32
                            horizontalSpacingExpanded = false
                        }
                    ) {
                        Text("32 dp")
                    }
                }
            }
        }

        LazyGridWithSpacingForMode(
            mode = columnModes[currentMode],
            horizontalSpacing = horizontalSpacing.dp,
            verticalSpacing = verticalSpacing.dp
        )
    }
}

@Composable
private fun LazyGridWithSpacingForMode(
    mode: GridCells,
    horizontalSpacing: Dp,
    verticalSpacing: Dp
) {
    LazyVerticalGrid(
        columns = mode,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        items(100) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderWithCustomKeys() {
    var names by remember { mutableStateOf(listOf("John", "Sara", "Dan")) }
    Column {
        Button(onClick = { names = names.shuffled() }) {
            Text("Shuffle")
        }
        LazyColumn {
            item {
                var counter by rememberSaveable { mutableStateOf(0) }
                Button(onClick = { counter++ }) {
                    Text("Header has $counter")
                }
            }
            items(
                items = names,
                key = { it }
            ) {
                var counter by rememberSaveable { mutableStateOf(0) }
                Button(onClick = { counter++ }, modifier = Modifier.animateItemPlacement()) {
                    Text("$it has $counter")
                }
            }
        }
    }
}

@Composable
private fun LazyWithFlingConfig() {
    Column {
        Text(
            "Custom fling config will dance back and forth when you fling",
            modifier = Modifier.padding(16.dp)
        )
        val defaultDecay = rememberSplineBasedDecay<Float>()
        val flingConfig = remember {
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    val unspecifiedFrame = AnimationConstants.UnspecifiedTime
                    val target = defaultDecay.calculateTargetValue(0f, initialVelocity)
                    val perDance = target / 3
                    var velocityLeft = initialVelocity
                    var lastLeft = 0f
                    var lastFrameTime = unspecifiedFrame
                    while (abs(lastLeft) < 1f && abs(perDance) > 0) {
                        listOf(perDance * 3 / 4, -perDance * 1 / 4).forEach { toGo ->
                            if (abs(lastLeft) > 1f) return@forEach
                            var lastValue = 0f
                            AnimationState(
                                initialValue = 0f,
                                lastFrameTimeNanos = lastFrameTime
                            ).animateTo(
                                targetValue = toGo,
                                sequentialAnimation = lastFrameTime != unspecifiedFrame
                            ) {
                                val delta = value - lastValue
                                lastLeft = delta - scrollBy(delta)
                                lastValue = value
                                velocityLeft = this.velocity
                                lastFrameTime = this.lastFrameTimeNanos
                                if (abs(lastLeft) > 0.5f) this.cancelAnimation()
                            }
                        }
                    }
                    return velocityLeft
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            flingBehavior = flingConfig
        ) {
            items(100) {
                Text(
                    text = "$it",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .background(Color.Gray.copy(alpha = it / 100f))
                        .border(1.dp, Color.Gray)
                        .padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun LazyStaggeredGridDemo() {
    val heights = remember {
        List(100) {
            (Random.nextInt(100) + 100).dp
        }
    }

    val colors = remember {
        List(100) {
            Color.hsl(
                Random.nextFloat() * 360,
                .5f,
                .65f
            )
        }
    }

    val indices = remember {
        mutableStateOf(List(100) { it })
    }

    var count by remember { mutableStateOf(10) }
    var reverseLayout by remember { mutableStateOf(false) }
    var rtl by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { count++ }) { Text(text = "++") }
            Button(onClick = { reverseLayout = !reverseLayout }) {
                Text("reverse")
            }
            Button(onClick = { rtl = !rtl }) {
                Text(if (rtl) "rtl" else "ltr")
            }
            Button(onClick = {
                indices.value = indices.value.toMutableList().apply { shuffle() }
            }) { Text(text = "shuffle") }
            Button(onClick = { if (count != 0) count-- }) { Text(text = "--") }
        }

        val state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 29)

        val layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = PaddingValues(vertical = 30.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                reverseLayout = reverseLayout,
                content = {
                    items(
                        count,
                        span = {
                            if (it % 30 == 0)
                                StaggeredGridItemSpan.FullLine
                            else
                                StaggeredGridItemSpan.SingleLane
                        }
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val index = indices.value[it % indices.value.size]
                        val color = colors[index]
                        Box(
                            modifier = Modifier
                                .height(if (!expanded) heights[index] else heights[index] * 2)
                                .border(2.dp, color, RoundedCornerShape(5.dp))
                                .clickable {
                                    expanded = !expanded
                                }
                        ) {
                            Text(
                                "$it",
                                modifier = Modifier.align(Alignment.Center),
                                color = color,
                                fontSize = 36.sp
                            )
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimateItemPlacementDemo() {
    val items = remember { mutableStateListOf<Int>().apply {
        repeat(20) { add(it) }
    } }
    val selectedIndexes = remember { mutableStateMapOf<Int, Boolean>() }
    var reverse by remember { mutableStateOf(false) }
    Column {
        Row {
            Button(onClick = {
                selectedIndexes.entries.reversed().forEach { entry ->
                    if (entry.value) {
                        items.remove(entry.key)
                        items.add(items.size - 3, entry.key)
                    }
                }
            }) {
                Text("MoveToEnd")
            }
            Button(onClick = {
                selectedIndexes.clear()
            }) {
                Text("RmvSelected")
            }
            Button(onClick = {
                reverse = !reverse
            }) {
                Text("Reverse=$reverse")
            }
        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f), reverseLayout = reverse) {
            items(items, key = { it }) { item ->
                val selected = selectedIndexes.getOrDefault(item, false)
                val modifier = if (selected) Modifier.animateItemPlacement() else Modifier
                var height by remember { mutableStateOf(40.dp) }
                Row(
                    modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                        .clickable {
                            height = if (height == 40.dp) 120.dp else 40.dp
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selected, onCheckedChange = {
                        selectedIndexes[item] = it
                    })
                    Spacer(
                        Modifier
                            .width(16.dp)
                            .height(height))
                    Text("Item $item")
                }
            }
        }
        var size by remember { mutableStateOf(40.dp) }
        Box(
            Modifier
                .height(size)
                .fillMaxWidth()
                .border(1.dp, Color.DarkGray)
                .clickable {
                    size = if (size == 40.dp) 350.dp else 40.dp
                })
    }
}
