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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.list

import androidx.compose.foundation.assertPixels
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.assertWithMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isLessThan
import androidx.compose.foundation.isNotEqualTo
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.isWithin1PixelFrom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * This class contains all LazyColumn-specific tests, as well as (by convention) tests that don't
 * need to be run in both orientations.
 *
 * To have a test run in both orientations (LazyRow and LazyColumn), add it to [LazyListTest]
 */
@OptIn(ExperimentalTestApi::class)
class LazyColumnTest {
    private val LazyListTag = "LazyListTag"

    private val NeverEqualObject = object {
        override fun equals(other: Any?): Boolean {
            return false
        }
    }

    @Test
    fun compositionsAreDisposed_whenDataIsChanged() = runSkikoComposeUiTest {
        var composed = 0
        var disposals = 0
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        setContent {
            LazyColumn(Modifier.testTag(LazyListTag).fillMaxSize()) {
                items(if (!part2) data1 else data2) {
                    DisposableEffect(NeverEqualObject) {
                        composed++
                        onDispose {
                            disposals++
                        }
                    }

                    Spacer(Modifier.height(50.dp))
                }
            }
        }

        runOnIdle {
            assertWithMessage("Not all items were composed")
                .that(composed).isEqualTo(data1.size)
            composed = 0

            part2 = true
        }

        runOnIdle {
            assertWithMessage(
                "No additional items were composed after data change, something didn't work"
            ).that(composed).isEqualTo(data2.size)

            // We may need to modify this test once we prefetch/cache items outside the viewport
            assertWithMessage(
                "Not enough compositions were disposed after scrolling, compositions were leaked"
            ).that(disposals).isEqualTo(data1.size)
        }
    }

    @Test
    fun compositionsAreDisposed_whenLazyListIsDisposed() = runSkikoComposeUiTest {
        var emitLazyList by mutableStateOf(true)
        var disposeCalledOnFirstItem = false
        var disposeCalledOnSecondItem = false

        setContent {
            if (emitLazyList) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(2) {
                        Box(Modifier.requiredSize(100.dp))
                        DisposableEffect(Unit) {
                            onDispose {
                                if (it == 1) {
                                    disposeCalledOnFirstItem = true
                                } else {
                                    disposeCalledOnSecondItem = true
                                }
                            }
                        }
                    }
                }
            }
        }

        runOnIdle {
            assertWithMessage("First item was incorrectly immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            assertWithMessage("Second item was incorrectly immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            emitLazyList = false
        }

        runOnIdle {
            assertWithMessage("First item was not correctly disposed")
                .that(disposeCalledOnFirstItem).isTrue()
            assertWithMessage("Second item was not correctly disposed")
                .that(disposeCalledOnSecondItem).isTrue()
        }
    }

    @Test
    fun removeItemsTest() = runSkikoComposeUiTest {
        val startingNumItems = 3
        var numItems = startingNumItems
        var numItemsModel by mutableStateOf(numItems)
        val tag = "List"
        setContent {
            LazyColumn(Modifier.testTag(tag)) {
                items((1..numItemsModel).toList()) {
                    BasicText("$it")
                }
            }
        }

        while (numItems >= 0) {
            // Confirm the number of children to ensure there are no extra items
            onNodeWithTag(tag)
                .onChildren()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (i in 1..3) {
                onNodeWithText("$i").apply {
                    if (i <= numItems) {
                        assertExists()
                    } else {
                        assertDoesNotExist()
                    }
                }
            }
            numItems--
            if (numItems >= 0) {
                // Don't set the model to -1
                runOnIdle { numItemsModel = numItems }
            }
        }
    }

    @Test
    fun changeItemsCountAndScrollImmediately() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(Modifier.fillMaxWidth().height(10.dp), state) {
                items(count) { index ->
                    composedIndexes.add(index)
                    Box(Modifier.size(20.dp))
                }
            }
        }

        runOnIdle {
            composedIndexes.clear()
            count = 10
            scope.launch {
                state.scrollToItem(50)
            }
        }
        runOnIdle {
            composedIndexes.forEach {
                assertThat(it).isLessThan(count)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(9)
        }
    }

    @Test
    fun changingDataTest() = runSkikoComposeUiTest {
        val dataLists = listOf(
            (1..3).toList(),
            (4..8).toList(),
            (3..4).toList()
        )
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        setContent {
            LazyColumn(Modifier.testTag(tag)) {
                items(dataModel) {
                    BasicText("$it")
                }
            }
        }

        for (data in dataLists) {
            runOnIdle { dataModel = data }

            // Confirm the number of children to ensure there are no extra items
            val numItems = data.size
            onNodeWithTag(tag)
                .onChildren()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (item in data) {
                onNodeWithText("$item").assertExists()
            }
        }
    }

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun SkikoComposeUiTest.prepareLazyColumnsItemsAlignment(horizontalGravity: Alignment.Horizontal) {
        setContent {
            LazyColumn(
                Modifier.testTag(LazyListTag).requiredWidth(100.dp),
                horizontalAlignment = horizontalGravity
            ) {
                items(listOf(1, 2)) {
                    if (it == 1) {
                        Spacer(Modifier.size(50.dp).testTag(firstItemTag))
                    } else {
                        Spacer(Modifier.size(70.dp).testTag(secondItemTag))
                    }
                }
            }
        }

        onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyColumnBounds = onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        with(density) {
            // Verify the width of the column
            assertThat(lazyColumnBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
            assertThat(lazyColumnBounds.right.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        }
    }

    @Test
    fun lazyColumnAlignmentCenterHorizontally() = runSkikoComposeUiTest {
        prepareLazyColumnsItemsAlignment(Alignment.CenterHorizontally)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(25.dp, 0.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(15.dp, 50.dp)
    }

    @Test
    fun lazyColumnAlignmentStart() = runSkikoComposeUiTest {
        prepareLazyColumnsItemsAlignment(Alignment.Start)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)
    }

    @Test
    fun lazyColumnAlignmentEnd() = runSkikoComposeUiTest {
        prepareLazyColumnsItemsAlignment(Alignment.End)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(30.dp, 50.dp)
    }

    @Test
    fun flingAnimationStopsOnFingerDown() = runSkikoComposeUiTest {
        val items by mutableStateOf((1..20).toList())
        val state = LazyListState()
        setContent {
            LazyColumn(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        mainClock.autoAdvance = false
        onNodeWithTag(LazyListTag)
            .performTouchInput { swipeUp() }
        mainClock.advanceTimeBy(100)

        val itemIndexWhenInterrupting = state.firstVisibleItemIndex
        val itemOffsetWhenInterrupting = state.firstVisibleItemScrollOffset

        assertThat(itemIndexWhenInterrupting).isNotEqualTo(0)
        assertThat(itemOffsetWhenInterrupting).isNotEqualTo(0)

        onNodeWithTag(LazyListTag)
            .performTouchInput { down(center) }
        mainClock.advanceTimeBy(100)

        assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndexWhenInterrupting)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemOffsetWhenInterrupting)
    }

    @Test
    fun removalWithMutableStateListOf() = runSkikoComposeUiTest {
        val items = mutableStateListOf("1", "2", "3")

        val itemSize = with(density) { 15.toDp() }

        setContent {
            LazyColumn {
                items(items) { item ->
                    Spacer(Modifier.size(itemSize).testTag(item))
                }
            }
        }

        runOnIdle {
            items.removeLast()
        }

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun recompositionOrder() = runSkikoComposeUiTest {
        val outerState = mutableStateOf(0)
        val innerState = mutableStateOf(0)
        val recompositions = mutableListOf<Pair<Int, Int>>()

        setContent {
            val localOuterState = outerState.value
            LazyColumn {
                items(count = 1) {
                    recompositions.add(localOuterState to innerState.value)
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        runOnIdle {
            innerState.value++
            outerState.value++
        }

        runOnIdle {
            assertThat(recompositions).isEqualTo(
                listOf(0 to 0, 1 to 1)
            )
        }
    }

    @Test
    fun scrolledAwayItemIsNotDisplayedAnymore() = runSkikoComposeUiTest(Size(10f, 10f)) {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier
                    .requiredSize(10.dp)
                    .testTag(LazyListTag)
                    .graphicsLayer()
                    .background(Color.Blue),
                state = state
            ) {
                items(2) {
                    val size = if (it == 0) 5.dp else 100.dp
                    val color = if (it == 0) Color.Red else Color.Transparent
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(size)
                            .background(color)
                            .testTag("$it")
                    )
                }
            }
        }

        runOnIdle {
            with(density) {
                scope.launch {
                    // we scroll enough to make the Red item not visible anymore
                    state.scrollBy(6.dp.toPx())
                }
            }
        }

        // and verify there is no Red item displayed
        captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun wrappedNestedLazyRowDisplayCorrectContent() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(Modifier.size(20.dp), state = state) {
                items(100) {
                    LazyRowWrapped {
                        BasicText("$it", Modifier.size(21.dp))
                    }
                }
            }
        }

        (1..10).forEach { item ->
            runOnIdle {
                scope.launch {
                    state.scrollToItem(item)
                }
            }

            onNodeWithText("$item")
                .assertIsDisplayed()
        }
    }

    @Composable
    private fun LazyRowWrapped(content: @Composable () -> Unit) {
        LazyRow {
            items(count = 1) {
                content()
            }
        }
    }
}

@Suppress("unused")
internal fun Modifier.drawOutsideOfBounds() = drawBehind {
    val inflate = 20.dp.roundToPx().toFloat()
    drawRect(
        Color.Red,
        Offset(-inflate, -inflate),
        Size(size.width + inflate * 2, size.height + inflate * 2)
    )
}