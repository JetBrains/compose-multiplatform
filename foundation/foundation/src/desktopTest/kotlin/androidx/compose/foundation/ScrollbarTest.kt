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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.LocalScrollConfig
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollConfig
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextFieldScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.DesktopComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith


@Suppress("WrapUnaryOperator")
@OptIn(ExperimentalTestApi::class)
@RunWith(Theories::class)
class ScrollbarTest {

    @get:Rule
    val rule = createComposeRule()

    @Theory
    fun `drag slider to the middle`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp)
        }
    }

    @Theory
    fun `drag slider when it is hidden`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    fun `drag slider to the edges`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, -500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    fun `drag outside slider`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(10f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    fun `drag outside slider and back`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            val scale = 2f  // Content distance to corresponding scrollbar distance
            rule.setContent(scrollbarProvider) {
                TestBox(
                    size = 100.dp,
                    childSize = 10.dp * scale,
                    childCount = 10,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            // While thumb is at the top, drag it up and then down the same distance.
            // Content should not move.
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 25f))
                press()
                moveBy(Offset(0f, -50f))
                moveBy(Offset(0f, 50f))
                release()
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)

            // While thumb is at the top, drag it up and then down a bit more.
            // Content should move by the diff.
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 25f))
                press()
                moveBy(Offset(0f, -50f))
                moveBy(Offset(0f, 51f))
                release()
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-1.dp * scale)

            // Drag thumb exactly to the end. Content should be at the bottom.
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 25f))
                press()
                moveBy(Offset(0f, 50f))
                release()
            }
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp * scale)

            // While thumb is at the bottom, drag it down and then up the same distance.
            // Content should not move
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 75f))
                press()
                moveBy(Offset(0f, 50f))
                moveBy(Offset(0f, -50f))
                release()
            }
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp * scale)

            // While thumb is at the bottom, drag it down and then up a bit more.
            // Content should move by the diff
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 75f))
                press()
                moveBy(Offset(0f, 50f))
                moveBy(Offset(0f, -51f))
                release()
            }
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-49.dp * scale)

        }
    }

    @Theory
    fun `drag slider with varying size items`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent(scrollbarProvider) {
                LazyListTestBox(state = listState, size = 100.dp, scrollbarWidth = 10.dp){
                    item {
                        Box(Modifier.size(20.dp))
                    }
                    item {
                        Box(Modifier.size(180.dp))
                    }
                    item {
                        Box(Modifier.size(20.dp))
                    }
                    item {
                        Box(Modifier.size(180.dp))
                    }
                }
            }
            rule.awaitIdle()


            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 5f))
                press()
            }

            // Scroll all the way down, one pixel at a time.
            // Make sure the content moves "up" every time.
            for (i in 1..100){
                val firstVisibleItemIndexBefore = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetBefore = listState.firstVisibleItemScrollOffset
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    moveBy(Offset(0f, 1f))
                }
                rule.awaitIdle()
                val firstVisibleItemIndexAfter = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetAfter = listState.firstVisibleItemScrollOffset

                if (firstVisibleItemIndexAfter < firstVisibleItemIndexBefore)
                    throw AssertionError(
                        "First visible item index decreased on iteration $i when dragging down; " +
                        "before=$firstVisibleItemIndexBefore, after=$firstVisibleItemIndexAfter"
                    )
                else if ((firstVisibleItemIndexAfter == firstVisibleItemIndexBefore) &&
                    (firstVisibleItemScrollOffsetAfter < firstVisibleItemScrollOffsetBefore))
                    throw AssertionError(
                        "First visible item offset decreased on iteration $i when dragging down; " +
                            "item index=$firstVisibleItemIndexAfter, " +
                            "offset before=$firstVisibleItemScrollOffsetBefore, " +
                            "offset after=$firstVisibleItemScrollOffsetAfter"
                    )
            }

            // Scroll back all the way up, one pixel at a time.
            // Make sure the content moves "down" every time
            for (i in 1..100){
                val firstVisibleItemIndexBefore = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetBefore = listState.firstVisibleItemScrollOffset
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    moveBy(Offset(0f, -1f))
                }
                rule.awaitIdle()
                val firstVisibleItemIndexAfter = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetAfter = listState.firstVisibleItemScrollOffset

                if (firstVisibleItemIndexAfter > firstVisibleItemIndexBefore)
                    throw AssertionError(
                        "First visible item index increased on iteration $i while dragging up; " +
                            "before=$firstVisibleItemIndexBefore, after=$firstVisibleItemIndexAfter"
                    )
                else if ((firstVisibleItemIndexAfter == firstVisibleItemIndexBefore) &&
                    (firstVisibleItemScrollOffsetAfter > firstVisibleItemScrollOffsetBefore))
                    throw AssertionError(
                        "First visible item offset increased on iteration $i while dragging up; " +
                            "item index=$firstVisibleItemIndexAfter, " +
                            "offset before=$firstVisibleItemScrollOffsetBefore, " +
                            "offset after=$firstVisibleItemScrollOffsetAfter"
                    )
            }

            rule.onNodeWithTag("scrollbar").performMouseInput {
                release()
            }
        }
    }

    @Theory
    fun `scroll lazy column to bottom with content padding`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent(scrollbarProvider) {
                LazyListTestBox(
                    state = listState,
                    size = 100.dp,
                    childSize = 10.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    contentPadding = PaddingValues(vertical = 25.dp)
                )
            }
            rule.awaitIdle()

            // Drag to the bottom
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 20f), end = Offset(0f, 80f))
            }

            rule.awaitIdle()

            // Note that if the scrolling is incorrect, this can fail not only with a wrong value,
            // but also by not finding the box node, as it may have not scrolled into view.
            // Last box should be at containerSize - bottomPadding - boxSize
            rule.onNodeWithTag("box19").assertTopPositionInRootIsEqualTo(100.dp - 25.dp - 10.dp)
        }
    }

    @Test
    fun `thumb size on scrollbar smaller than viewport`() {
        runBlocking(Dispatchers.Main) {
            val scrollState = ScrollState(0)
            rule.setContent {
                TestBox(
                    scrollState = scrollState,
                    size = 200.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    scrollbarHeight = 100.dp,
                )
            }
            rule.awaitIdle()

            // Thumb should be half the height of the scrollbar, as the viewport (200.dp) is half
            // the height of the content (400.dp). So clicking on the top half of the scrollbar
            // should do nothing.
            for (offset in 1..50){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, offset.toFloat()))
                assertEquals(0, scrollState.value)
            }

            // Clicking one pixel below the thumb should scroll the content by one viewport
            rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 51f))
            assertEquals(200, scrollState.value)
        }
    }

    // See https://github.com/JetBrains/compose-jb/issues/2640
    @Test
    fun `drag scrollbar to bottom with content padding`() {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent {
                LazyListTestBox(
                    state = listState,
                    size = 300.dp,
                    scrollbarWidth = 10.dp,
                    contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp),
                ){
                    val childHeights = listOf(100.dp, 200.dp, 75.dp)
                    items(childHeights.size){ index ->
                        Box(Modifier.size(childHeights[index]))
                    }
                }
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 5f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()

            // Test whether the scrollbar is at the bottom by trying to drag it by the last pixel.
            // If it's not at the bottom, the drag will not succeed
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 299f), end = Offset(0f, 0f))
            }
            rule.awaitIdle()

            assertEquals(true, listState.canScrollForward)
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
            assertEquals(0, firstVisibleItem.index)
            assertEquals(0, firstVisibleItem.offset)
        }
    }

    // See https://github.com/JetBrains/compose-jb/issues/2679
    @Test
    fun `drag scrollbar to bottom and top with large and small items`() {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent {
                LazyListTestBox(
                    state = listState,
                    size = 300.dp,
                    scrollbarWidth = 10.dp,
                ){
                    val childHeights =  List(4){ 200.dp } + List(10){ 50.dp }
                    items(childHeights.size){ index ->
                        Box(Modifier.size(childHeights[index]))
                    }
                }
            }
            rule.awaitIdle()

            // Slowly drag to the bottom
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 5f))
                press()
                repeat(100){
                    moveBy(Offset(0f, 3f))
                }
                release()
            }
            rule.awaitIdle()

            // Test whether the scrollbar is at the bottom by trying to drag it by the last pixel.
            // If it's not at the bottom, the drag will not succeed
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 299f))
                press()
                repeat(100){
                    moveBy(Offset(0f, -3f))
                }
                release()
            }
            rule.awaitIdle()

            assertEquals(true, listState.canScrollForward)
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
            assertEquals(0, firstVisibleItem.index)
            assertEquals(0, firstVisibleItem.offset)
        }
    }

    // TODO(demin): write a test when we support DesktopComposeTestRule.mainClock:
    //  see https://github.com/JetBrains/compose-jb/issues/637
//    fun `move mouse to the slider and drag it`() {
//        ...
//        rule.performMouseMove(0, 25)
//        rule.mainClock.advanceTimeByFrame()
//        press(Offset(0f, 25f))
//        rule.mainClock.advanceTimeByFrame()
//        moveTo(Offset(0f, 30f))
//        rule.mainClock.advanceTimeByFrame()
//        moveTo(Offset(0f, 50f))
//        rule.mainClock.advanceTimeByFrame()
//        release()
//        ...
//    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `mouseScroll over slider`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(0, 25, 1f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-10.dp)
        }
    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `mouseScroll over scrollbar outside slider`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(0, 99, 1f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-10.dp)
        }
    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `vertical mouseScroll over horizontal scrollbar `(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            // TODO(demin): write tests for vertical mouse scrolling over
            //  horizontalScrollbar for the case when we have two-way scrollable content:
            //  Modifier.verticalScrollbar(...).horizontalScrollbar(...)
            //  Content should scroll vertically.
        }
    }

    @Theory
    fun `mouseScroll over column then drag to the beginning`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(20, 25, 10f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, -500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("JUnitMalformedDeclaration")
    fun `press on scrollbar outside slider`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 26f))
                press()
            }

            tryUntilSucceeded {
                rule.awaitIdle()
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
            }
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("JUnitMalformedDeclaration")
    fun `press on the end of scrollbar outside slider`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 99f))
                press()
            }

            tryUntilSucceeded {
                rule.awaitIdle()
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-300.dp)
            }
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("JUnitMalformedDeclaration")
    fun `dynamically change content then drag slider to the end`(
        scrollbarProvider: ScrollbarProvider
    ) {
        runBlocking(Dispatchers.Main) {
            val isContentVisible = mutableStateOf(false)
            rule.setContent(scrollbarProvider) {
                TestBox(
                    size = 100.dp,
                    scrollbarWidth = 10.dp
                ) {
                    if (isContentVisible.value) {
                        repeat(10) {
                            Box(Modifier.size(20.dp).testTag("box$it"))
                        }
                    }
                }
            }
            rule.awaitIdle()

            isContentVisible.value = true
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll by less than one page in lazy list`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarProvider) {
                state = rememberLazyListState()
                LazyListTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 0f), end = Offset(0f, 11f))
            }
            rule.awaitIdle()
            assertEquals(2, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll in reversed lazy list`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarProvider) {
                state = rememberLazyListState()
                LazyListTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    reverseLayout = true
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, 88f))
            }
            rule.awaitIdle()
            assertEquals(2, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll by more than one page in lazy list`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarProvider) {
                state = rememberLazyListState()
                LazyListTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 0f), end = Offset(0f, 26f))
            }
            rule.awaitIdle()
            assertEquals(5, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll outside of scrollbar bounds in lazy list`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarProvider) {
                state = rememberLazyListState()
                LazyListTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 0f), end = Offset(0f, 10000f))
            }
            rule.awaitIdle()
            assertEquals(15, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, -10000f))
            }
            rule.awaitIdle()
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    fun `drag lazy slider when it is hidden`(scrollbarProvider: ScrollbarProvider) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarProvider) {
                LazyListTestBox(
                    size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun `basic lazy grid scrollbar test`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                LazyGridTestBox(
                    // 3x20 grid, each item is 30x20 dp
                    columns = GridCells.Adaptive(30.dp),
                    size = DpSize(100.dp, 200.dp),
                    childSize = DpSize(30.dp, 20.dp),
                    childCount = 60,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)

            // Test the size of the scrollbar thumb by trying to drag by one pixel below where it
            // should end
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 100f), end = Offset(0f, 200f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)

            // Test the size of the scrollbar thumb by trying to drag by its bottommost pixel
            // This also tests the proportionality of the scrolling
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, 104f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo((-10).dp)

            // Drag the scrollbar to the bottom and test the position of the last item
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 104f), end = Offset(0f, 199f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box59").assertTopPositionInRootIsEqualTo(180.dp)

            // Press above the scrollbar and test the new position
            rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 0f))
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun `test empty lazy list`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                LazyListTestBox(
                    size = 100.dp, childSize = 20.dp, childCount = 0, scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            // Just play around and make sure it doesn't crash
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 0f))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private inline fun TextFieldScrollState.assertChangeInOffset(
        expectedChange: Float,
        action: () -> Unit
    ){
        val before = offset
        action()
        assertEquals(expectedChange, offset - before)
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    fun `basic text field with vertical scrolling test`() {
        runBlocking(Dispatchers.Main) {
            val scrollState = TextFieldScrollState(Orientation.Vertical)
            rule.setContent {
                // Set up a text field that is exactly 10 lines tall, with text that has 20 lines,
                // Scrollbar thumb should be 50.dp -- half the scrollbar height
                Row{
                    Box(
                        modifier = Modifier.width(100.dp)
                    ){
                        var text by remember {
                            mutableStateOf(
                                TextFieldValue(
                                    buildString {
                                        repeat(19) { // 20 lines including the last empty one
                                            append("A\n")
                                        }
                                    }
                                )
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = {
                                text = it
                            },
                            scrollState = scrollState,
                            maxLines = 10,  // Make sure not to give the text field any pixel height
                            modifier = Modifier
                                .testTag("textfield"),
                        )
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier
                            .width(10.dp)
                            .height(100.dp)
                            .testTag("scrollbar")
                    )
                }
            }
            rule.awaitIdle()

            // Click to focus
            rule.onNodeWithTag("textfield").performMouseInput {
                click(Offset(0f, 0f))
            }
            rule.awaitIdle()

            suspend fun pressDownAndAwaitIdle(){
                rule.onNodeWithTag("textfield").performKeyInput {
                    pressKey(Key.DirectionDown)
                }
                rule.awaitIdle()
            }

            // Press "down" 9 times, which should bring the caret to the last visible line
            repeat(9){
                pressDownAndAwaitIdle()
            }
            // Scroll offset should not change yet
            assertEquals(0f, scrollState.offset)

            // The scrollbar thumb should still be at the top, so clicking at 0 shouldn't change
            // the scroll offset
            scrollState.assertChangeInOffset(0f){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 0f))
            }

            // Press "down" one more time, which should bring the caret to the 11th line, and cause
            // the text field to scroll down by one line (out of a possible 10)
            pressDownAndAwaitIdle()
            assertEquals(10f, scrollState.maxOffset / scrollState.offset)

            // The scrollbar thumb should move by 1/10th of its range, which is 50 pixels, so
            // clicking on the 5th pixel should do nothing
            scrollState.assertChangeInOffset(0f){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 5f))
            }

            // But clicking on the 4th pixel should scroll to top
            scrollState.assertChangeInOffset(-scrollState.offset){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 4f))
            }

            // Press down 9 more times to reach the bottom
            repeat(9){
                pressDownAndAwaitIdle()
            }
            assertEquals(scrollState.maxOffset, scrollState.offset)

            // The scrollbar thumb should move to the bottom, so clicking on the 50th pixel should
            // do nothing
            scrollState.assertChangeInOffset(0f){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 50f))
            }

            // But clicking on the 49th pixel should scroll to the very top
            scrollState.assertChangeInOffset(-scrollState.offset){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(0f, 49f))
            }
        }
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    fun `basic text field with horizontal scrolling test`() {
        runBlocking(Dispatchers.Main) {
            val scrollState = TextFieldScrollState(Orientation.Horizontal)
            rule.setContent {
                // Set up a single-line text field with 100 characters of text and 100 pixels of
                // width so the text has to scroll significantly.
                // We won't be doing exact measurements in this test, due to the difficulty in
                // measuring text width.
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                ){
                    var text by remember {
                        mutableStateOf(
                            TextFieldValue(
                                buildString {
                                    repeat(100) {
                                        append("A")
                                    }
                                }
                            )
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = {
                            text = it
                        },
                        scrollState = scrollState,
                        singleLine = true,
                        modifier = Modifier
                            .testTag("textfield"),
                    )

                    HorizontalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .testTag("scrollbar")
                    )
                }
            }
            rule.awaitIdle()

            // Click to focus
            rule.onNodeWithTag("textfield").performMouseInput {
                click(Offset(0f, 0f))
            }
            rule.awaitIdle()

            suspend fun pressRightAndAwaitIdle(){
                rule.onNodeWithTag("textfield").performKeyInput {
                    pressKey(Key.DirectionRight)
                }
                rule.awaitIdle()
            }

            // Press "right" 100 times, which should bring the caret to the very end
            repeat(100){
                pressRightAndAwaitIdle()
            }
            assertEquals(scrollState.offset, scrollState.maxOffset)

            // The scrollbar thumb should still be at the end, so clicking at the last pixel
            // shouldn't change the scroll offset
            scrollState.assertChangeInOffset(0f){
                rule.clickScrollbarAndAwaitIdle("scrollbar", position = Offset(99f, 0f))
            }

            // Dragging the scrollbar to the very left should reset the scroll offset to 0
            scrollState.assertChangeInOffset(-scrollState.offset){
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    instantDrag(start = Offset(99f, 0f), end = Offset(0f, 0f))
                }
            }
        }
    }

    private suspend fun testLazyContentWithLineSpacing(firstBoxTag: String, lastBoxTag: String){
        // Test the size of the scrollbar thumb by trying to drag by one pixel below where it
        // should end
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 50f), end = Offset(0f, 200f))
        }
        rule.awaitIdle()
        rule.onNodeWithTag(firstBoxTag).assertTopPositionInRootIsEqualTo(0.dp)

        // Test the size of the scrollbar thumb by trying to drag by its bottommost pixel
        // This also tests the proportionality of the scrolling
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 49f), end = Offset(0f, 54f))
        }
        rule.awaitIdle()
        rule.onNodeWithTag(firstBoxTag).assertTopPositionInRootIsEqualTo((-10).dp)

        // Scroll to the bottom and check the last item position
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 54f), end = Offset(0f, 99f))
        }
        rule.onNodeWithTag(lastBoxTag).assertTopPositionInRootIsEqualTo(80.dp)
    }

    @Test
    fun `lazy list with line spacing`(){
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                LazyListTestBox(
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 5,
                    scrollbarWidth = 10.dp,
                    verticalArrangement = Arrangement.spacedBy(25.dp)
                )
            }
            rule.awaitIdle()

            testLazyContentWithLineSpacing("box0", "box4")
        }
    }

    @Test
    fun `lazy grid with line spacing`(){
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                LazyGridTestBox(
                    columns = GridCells.Fixed(4),
                    size = DpSize(100.dp, 100.dp),
                    childSize = DpSize(20.dp, 20.dp),
                    childCount = 18,
                    scrollbarWidth = 10.dp,
                    verticalArrangement = Arrangement.spacedBy(25.dp),
                )
            }
            rule.awaitIdle()

            testLazyContentWithLineSpacing("box0", "box17")
        }
    }

    private suspend fun tryUntilSucceeded(block: suspend () -> Unit) {
        while (true) {
            try {
                block()
                break
            } catch (e: Throwable) {
                delay(10)
            }
        }
    }

    @OptIn(InternalTestApi::class, ExperimentalComposeUiApi::class)
    private fun ComposeTestRule.performMouseScroll(x: Int, y: Int, delta: Float) {
        (this as DesktopComposeTestRule).scene.sendPointerEvent(
            PointerEventType.Scroll,
            Offset(x.toFloat(), y.toFloat()),
            scrollDelta = Offset(x = 0f, y = delta),
            nativeEvent = awtWheelEvent()
        )
    }

    @OptIn(ExperimentalComposeUiApi::class, InternalTestApi::class)
    private fun ComposeTestRule.performMouseMove(x: Int, y: Int) {
        (this as DesktopComposeTestRule).scene.sendPointerEvent(
            PointerEventType.Move,
            Offset(x.toFloat(), y.toFloat())
        )
    }

    @Composable
    private fun TestBox(
        scrollState: ScrollState = rememberScrollState(),
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
        scrollbarHeight: Dp = size
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            Column(
                Modifier.fillMaxSize().testTag("column").verticalScroll(scrollState)
            ) {
                repeat(childCount) {
                    Box(Modifier.size(childSize).testTag("box$it"))
                }
            }

            ScrollbarProviderLocal.current.VerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .height(scrollbarHeight)
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun TestBox(
        size: Dp,
        scrollbarWidth: Dp,
        scrollableContent: @Composable ColumnScope.() -> Unit
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            val state = rememberScrollState()

            Column(
                Modifier.fillMaxSize().testTag("column").verticalScroll(state),
                content = scrollableContent
            )

            ScrollbarProviderLocal.current.VerticalScrollbar(
                scrollState = state,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun LazyListTestBox(
        state: LazyListState = rememberLazyListState(),
        size: Dp,
        scrollbarWidth: Dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        content: LazyListScope.() -> Unit
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("column"),
                state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                content = content
            )

            ScrollbarProviderLocal.current.VerticalScrollbar(
                scrollState = state,
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun LazyListTestBox(
        state: LazyListState = rememberLazyListState(),
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    ) = LazyListTestBox(
        state = state,
        size = size,
        scrollbarWidth = scrollbarWidth,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement
    ) {
        items(childCount) {
            Box(Modifier.size(childSize).testTag("box$it"))
        }
    }

    @Composable
    private fun LazyGridTestBox(
        state: LazyGridState = rememberLazyGridState(),
        columns: GridCells,
        size: DpSize,
        scrollbarWidth: Dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        content: LazyGridScope.() -> Unit
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize().testTag("grid"),
                state = state,
                columns = columns,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                content = content
            )

            ScrollbarProviderLocal.current.VerticalScrollbar(
                scrollState = state,
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun LazyGridTestBox(
        state: LazyGridState = rememberLazyGridState(),
        columns: GridCells,
        size: DpSize,
        childSize: DpSize,
        childCount: Int,
        scrollbarWidth: Dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    ) = LazyGridTestBox(
        state = state,
        columns = columns,
        size = size,
        scrollbarWidth = scrollbarWidth,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement
    ){
        items(childCount) {
            Box(Modifier.size(childSize).testTag("box$it"))
        }
    }

    private fun MouseInjectionScope.instantDrag(start: Offset, end: Offset) {
        moveTo(start)
        press()
        moveTo(end)
        release()
    }

    /**
     * A "click" for scrolling scrollbars.
     * TODO: figure out why [MouseInjectionScope.click] doesn't work to scroll a scrollbar.
     */
    private suspend fun ComposeContentTestRule.clickScrollbarAndAwaitIdle(tag: String, position: Offset){
        onNodeWithTag(tag).performMouseInput {
            moveTo(position)
            press()
        }
        awaitIdle()
        onNodeWithTag(tag).performMouseInput {
            release()
        }
        awaitIdle()
    }

    @Composable
    private fun withTestEnvironment(content: @Composable () -> Unit) = CompositionLocalProvider(
        LocalScrollbarStyle provides ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            shape = RectangleShape,
            hoverDurationMillis = 300,
            unhoverColor = Color.Black,
            hoverColor = Color.Red
        ),
        LocalScrollConfig provides TestConfig,
        content = content
    )

    companion object{

        // The old and new scrollbar implementations, allowing us to run the same tests on both
        // Tests that should run on both, should:
        // 1. Be marked as `@Theory`
        // 2. Take a ScrollbarImpl argument
        // 3. Set the argument as the ScrollbarImplLocal, typically via
        //    ComposeContentTestRule.setContent(ScrollbarImpl, @Composable () -> Unit)
        // Tests that should only run on the new implementation should just be marked with `@Test`
        // as usual.

        @JvmField
        @DataPoint
        val NewScrollbarProvider: ScrollbarProvider = NewScrollbar

        @JvmField
        @DataPoint
        val OldScrollbarProvider: ScrollbarProvider = OldScrollbar

    }

}

/**
 * Abstracts the implementation of the scrollbar (actually just the adapter) to allow us to test
 * both the new and old adapters.
 */
sealed class ScrollbarImpl<A: Any> {

    @Composable
    fun VerticalScrollbar(
        scrollState: ScrollState,
        modifier: Modifier = Modifier,
        reverseLayout: Boolean = false,
        style: ScrollbarStyle = LocalScrollbarStyle.current,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    ) = VerticalScrollbarImpl(
        adapter = remember(scrollState) { adapterFor(scrollState) },
        modifier = modifier,
        reverseLayout = reverseLayout,
        style = style,
        interactionSource = interactionSource
    )

    @Composable
    fun VerticalScrollbar(
        scrollState: LazyListState,
        modifier: Modifier = Modifier,
        reverseLayout: Boolean = false,
        style: ScrollbarStyle = LocalScrollbarStyle.current,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    ) = VerticalScrollbarImpl(
        adapter = remember(scrollState) { adapterFor(scrollState) },
        modifier = modifier,
        reverseLayout = reverseLayout,
        style = style,
        interactionSource = interactionSource
    )

    @Composable
    fun VerticalScrollbar(
        scrollState: LazyGridState,
        modifier: Modifier = Modifier,
        reverseLayout: Boolean = false,
        style: ScrollbarStyle = LocalScrollbarStyle.current,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    ) = VerticalScrollbarImpl(
        adapter = remember(scrollState) { adapterFor(scrollState) },
        modifier = modifier,
        reverseLayout = reverseLayout,
        style = style,
        interactionSource = interactionSource
    )

    @Composable
    protected abstract fun VerticalScrollbarImpl(
        adapter: A,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    )

    protected abstract fun adapterFor(scrollState: ScrollState): A

    protected abstract fun adapterFor(scrollState: LazyListState): A

    protected abstract fun adapterFor(scrollState: LazyGridState): A

}

/**
 * The old scrollbar implementation.
 */
@Suppress("DEPRECATION")
private object OldScrollbar: ScrollbarImpl<ScrollbarAdapter>() {

    // Our old implementation of the old scrollbar adapter interface
    private class OldScrollableScrollbarAdapter(
        private val scrollState: ScrollState
    ) : ScrollbarAdapter {
        override val scrollOffset: Float get() = scrollState.value.toFloat()

        override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
            scrollState.scrollTo(scrollOffset.roundToInt())
        }

        override fun maxScrollOffset(containerSize: Int) =
            scrollState.maxValue.toFloat()
    }

    // Our old implementation of the old scrollbar adapter interface
    private class OldLazyScrollbarAdapter(
        private val scrollState: LazyListState
    ) : ScrollbarAdapter {

        override val scrollOffset: Float
            get() = scrollState.firstVisibleItemIndex * averageItemSize +
                scrollState.firstVisibleItemScrollOffset

        override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
            val distance = scrollOffset - this@OldLazyScrollbarAdapter.scrollOffset
            if (abs(distance) <= containerSize) {
                scrollState.scrollBy(distance)
            } else {
                snapTo(containerSize, scrollOffset)
            }
        }

        private suspend fun snapTo(containerSize: Int, scrollOffset: Float) {
            val maximumValue = maxScrollOffset(containerSize).toDouble()
            val scrollOffsetCoerced = scrollOffset.toDouble().coerceIn(0.0, maximumValue)
            val averageItemSize = averageItemSize.toDouble()

            val index = (scrollOffsetCoerced / averageItemSize)
                .toInt()
                .coerceAtLeast(0)
                .coerceAtMost(itemCount - 1)

            val offset = (scrollOffsetCoerced - index * averageItemSize)
                .toInt()
                .coerceAtLeast(0)

            scrollState.scrollToItem(index = index, scrollOffset = offset)
        }

        override fun maxScrollOffset(containerSize: Int) =
            (averageItemSize * itemCount
                + scrollState.layoutInfo.beforeContentPadding
                + scrollState.layoutInfo.afterContentPadding
                - containerSize
                ).coerceAtLeast(0f)

        private val itemCount get() = scrollState.layoutInfo.totalItemsCount

        private val averageItemSize by derivedStateOf {
            scrollState
                .layoutInfo
                .visibleItemsInfo
                .asSequence()
                .map { it.size }
                .average()
                .toFloat()
        }

    }

    @Composable
    override fun VerticalScrollbarImpl(
        adapter: ScrollbarAdapter,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    ) {
        VerticalScrollbar(
            adapter = adapter,
            modifier = modifier,
            reverseLayout = reverseLayout,
            style = style,
            interactionSource = interactionSource
        )
    }

    override fun adapterFor(scrollState: ScrollState): ScrollbarAdapter {
        return OldScrollableScrollbarAdapter(scrollState)
    }

    override fun adapterFor(scrollState: LazyListState): ScrollbarAdapter {
        return OldLazyScrollbarAdapter(scrollState)
    }

    override fun adapterFor(scrollState: LazyGridState): ScrollbarAdapter {
        throw NotImplementedError("Old ScrollbarAdapter was not implemented for lazy grids")
    }
    
}

/**
 * The new scrollbar implementation
 */
private object NewScrollbar: ScrollbarImpl<androidx.compose.foundation.v2.ScrollbarAdapter>() {

    @Composable
    override fun VerticalScrollbarImpl(
        adapter: androidx.compose.foundation.v2.ScrollbarAdapter,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    ) {
        VerticalScrollbar(
            adapter = adapter,
            modifier = modifier,
            reverseLayout = reverseLayout,
            style = style,
            interactionSource = interactionSource
        )
    }

    override fun adapterFor(
        scrollState: ScrollState
    ): androidx.compose.foundation.v2.ScrollbarAdapter {
        return ScrollbarAdapter(scrollState)
    }

    override fun adapterFor(
        scrollState: LazyListState
    ): androidx.compose.foundation.v2.ScrollbarAdapter {
        return ScrollbarAdapter(scrollState)
    }

    override fun adapterFor(
        scrollState: LazyGridState
    ): androidx.compose.foundation.v2.ScrollbarAdapter {
        return ScrollbarAdapter(scrollState)
    }
    
}

private typealias ScrollbarProvider = ScrollbarImpl<*>

private val ScrollbarProviderLocal = compositionLocalOf<ScrollbarProvider>{ NewScrollbar }

private fun ComposeContentTestRule.setContent(
    scrollbarProvider: ScrollbarProvider,
    composable: @Composable () -> Unit
){
    setContent {
        CompositionLocalProvider(ScrollbarProviderLocal provides scrollbarProvider){
            composable()
        }
    }
}

internal object TestConfig : ScrollConfig {
    // the formula was determined experimentally based on MacOS Finder behaviour
    // MacOS driver will send events with accelerating delta
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        return -event.totalScrollDelta * 10.dp.toPx()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val PointerEvent.totalScrollDelta
    get() = this.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }