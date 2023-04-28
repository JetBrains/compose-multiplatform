/*
 * Copyright 2023 The Android Open Source Project
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
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp)
    }

    @Theory
    fun `drag slider when it is hidden`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp)
        }
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Theory
    fun `drag slider to the edges`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 25f), end = Offset(0f, 500f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 99f), end = Offset(0f, -500f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Theory
    fun `drag outside slider`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(10f, 25f), end = Offset(0f, 50f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Theory
    fun `drag outside slider and back`(scrollbarProvider: ScrollbarProvider) {
        val scale = 2f  // Content distance to corresponding scrollbar distance
        rule.setContent(scrollbarProvider) {
            TestBox(
                size = 100.dp,
                childSize = 10.dp * scale,
                childCount = 10,
                scrollbarWidth = 10.dp
            )
        }

        // While thumb is at the top, drag it up and then down the same distance.
        // Content should not move.
        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 25f))
            press()
            moveBy(Offset(0f, -50f))
            moveBy(Offset(0f, 50f))
            release()
        }
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

    @Theory
    fun `drag slider with varying size items`(scrollbarProvider: ScrollbarProvider) {
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

    @Theory
    fun `scroll lazy column to bottom with content padding`(scrollbarProvider: ScrollbarProvider) {
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

        // Drag to the bottom
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 20f), end = Offset(0f, 80f))
        }

        // Note that if the scrolling is incorrect, this can fail not only with a wrong value,
        // but also by not finding the box node, as it may have not scrolled into view.
        // Last box should be at containerSize - bottomPadding - boxSize
        rule.onNodeWithTag("box19").assertTopPositionInRootIsEqualTo(100.dp - 25.dp - 10.dp)
    }

    @Test
    fun `thumb size on scrollbar smaller than viewport`() {
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

        rule.testVerticalThumbEnd(
            lastPixelPosition = Offset(0f, 49f),
            dragAmount = 10f,
            assertNotMoved = {
                assertEquals(0, scrollState.value)
            },
            assertDraggedBy = { pixelAmount ->
                assertEquals((4 * pixelAmount).toInt(), scrollState.value)
            },
            assertPageDown = {
                assertEquals(200, scrollState.value)
            }
        )
    }

    // See https://github.com/JetBrains/compose-jb/issues/2640
    @Test
    fun `drag scrollbar to bottom with content padding`() {
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

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 5f), end = Offset(0f, 500f))
        }

        // Test whether the scrollbar is at the bottom by trying to drag it by the last pixel.
        // If it's not at the bottom, the drag will not succeed
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 299f), end = Offset(0f, 0f))
        }

        assertEquals(true, listState.canScrollForward)
        val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
        assertEquals(0, firstVisibleItem.index)
        assertEquals(0, firstVisibleItem.offset)
    }

    // See https://github.com/JetBrains/compose-jb/issues/2679
    @Test
    fun `drag scrollbar to bottom and top with large and small items`() {
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

        // Slowly drag to the bottom
        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 5f))
            press()
            repeat(100){
                moveBy(Offset(0f, 3f))
            }
            release()
        }

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

        assertEquals(true, listState.canScrollForward)
        val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
        assertEquals(0, firstVisibleItem.index)
        assertEquals(0, firstVisibleItem.offset)
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
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
        }

        rule.performMouseScroll(0, 25, 1f)
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-10.dp)
    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `mouseScroll over scrollbar outside slider`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
        }

        rule.performMouseScroll(0, 99, 1f)
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-10.dp)
    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `vertical mouseScroll over horizontal scrollbar `(scrollbarProvider: ScrollbarProvider) {
        // TODO(demin): write tests for vertical mouse scrolling over
        //  horizontalScrollbar for the case when we have two-way scrollable content:
        //  Modifier.verticalScrollbar(...).horizontalScrollbar(...)
        //  Content should scroll vertically.
    }

    @Theory
    fun `mouseScroll over column then drag to the beginning`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
        }

        rule.performMouseScroll(20, 25, 10f)
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 99f), end = Offset(0f, -500f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Theory
    fun `press on track just below slider`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
        }

        // Press such that only one page-down happens
        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 26f))
            press()
        }

        // Give enough time for many scrolls
        rule.mainClock.advanceTimeBy(timeUntilScrollsByPressOnTrack(10))
        rule.onNodeWithTag("scrollbar").performMouseInput {
            release()
        }

        // Expect only one page-down scroll
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
    }

    @Theory
    fun `press on the end of track outside slider`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 99f))
            press()
        }

        // 3 page-down scrolls are required to reach the end.
        rule.mainClock.advanceTimeBy(timeUntilScrollsByPressOnTrack(3))
        rule.onNodeWithTag("scrollbar").performMouseInput {
            release()
        }

        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-300.dp)
    }

    @Theory
    fun `press on track outside slider then move forward`(
        scrollbarProvider: ScrollbarProvider
    ) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 26f))
            press()
            moveTo(Offset(0f, 51f)) // Move immediately to allow 2 scrolls
        }

        // 2 page-down scrolls are required to reach 50-75 thumb range
        rule.mainClock.advanceTimeBy(timeUntilScrollsByPressOnTrack(2))
        rule.onNodeWithTag("scrollbar").performMouseInput {
            release()
        }

        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-200.dp)
    }

    @Theory
    fun `press on track outside slider then move back`(
        scrollbarProvider: ScrollbarProvider
    ) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 51f))
            press()
            moveTo(Offset(0f, 26f)) // Move immediately to allow only a single scroll
        }

        // Give enough time for many scrolls
        rule.mainClock.advanceTimeBy(timeUntilScrollsByPressOnTrack(10))
        rule.onNodeWithTag("scrollbar").performMouseInput {
            release()
        }

        // Expect only one scroll to have occurred
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
    }

    @Theory
    fun `press on track outside slider then move outside scrollbar`(
        scrollbarProvider: ScrollbarProvider
    ) {
        rule.setContent(scrollbarProvider) {
            TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
        }

        rule.onNodeWithTag("scrollbar").performMouseInput {
            moveTo(Offset(0f, 99f))
            press()
            moveTo(Offset(-20f, 99f)) // Move outside the scrollbar
        }

        // 3 scrolls are needed to move to the bottom
        rule.mainClock.advanceTimeBy(timeUntilScrollsByPressOnTrack(3))
        rule.onNodeWithTag("scrollbar").performMouseInput {
            release()
        }

        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-300.dp)
    }

    @Theory
    fun `dynamically change content then drag slider to the end`(
        scrollbarProvider: ScrollbarProvider
    ) {
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

        rule.onNodeWithTag("box0").assertDoesNotExist()

        isContentVisible.value = true
        rule.waitForIdle()

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 25f), end = Offset(0f, 500f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
    }

    @Theory
    @Suppress("SameParameterValue")
    fun `scroll by less than one page in lazy list`(scrollbarProvider: ScrollbarProvider) {
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

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 0f), end = Offset(0f, 11f))
        }

        assertEquals(2, state.firstVisibleItemIndex)
        assertEquals(4, state.firstVisibleItemScrollOffset)
    }

    @Theory
    @Suppress("SameParameterValue")
    fun `scroll in reversed lazy list`(scrollbarProvider: ScrollbarProvider) {
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

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 99f), end = Offset(0f, 88f))
        }
        assertEquals(2, state.firstVisibleItemIndex)
        assertEquals(4, state.firstVisibleItemScrollOffset)
    }

    @Theory
    @Suppress("SameParameterValue")
    fun `scroll by more than one page in lazy list`(scrollbarProvider: ScrollbarProvider) {
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

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 0f), end = Offset(0f, 26f))
        }
        assertEquals(5, state.firstVisibleItemIndex)
        assertEquals(4, state.firstVisibleItemScrollOffset)
    }

    @Theory
    @Suppress("SameParameterValue")
    fun `scroll outside of scrollbar bounds in lazy list`(scrollbarProvider: ScrollbarProvider) {
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

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 0f), end = Offset(0f, 10000f))
        }
        assertEquals(15, state.firstVisibleItemIndex)
        assertEquals(0, state.firstVisibleItemScrollOffset)

        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 99f), end = Offset(0f, -10000f))
        }
        assertEquals(0, state.firstVisibleItemIndex)
        assertEquals(0, state.firstVisibleItemScrollOffset)
    }

    @Theory
    fun `drag lazy slider when it is hidden`(scrollbarProvider: ScrollbarProvider) {
        rule.setContent(scrollbarProvider) {
            LazyListTestBox(
                size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp
            )
        }
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `basic lazy grid test`() {
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

        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)

        rule.testVerticalThumbEnd(
            lastPixelPosition = Offset(0f, 99f),
            dragAmount = 5f,
            assertNotMoved = {
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
            },
            assertDraggedBy = { pixelAmount ->
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo((-2 * pixelAmount).dp)
            },
            assertPageDown = {
                rule.onNodeWithTag("box30").assertTopPositionInRootIsEqualTo(0.dp)
            }
        )

        // Drag the scrollbar to the bottom and test the position of the last item
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 104f), end = Offset(0f, 199f))
        }
        rule.onNodeWithTag("box59").assertTopPositionInRootIsEqualTo(180.dp)

        // Press above the scrollbar and test the new position
        rule.onNodeWithTag("scrollbar").performMouseInput {
            click(position = Offset(0f, 0f))
        }
        rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `test empty lazy list`() {
        rule.setContent {
            LazyListTestBox(
                size = 100.dp, childSize = 20.dp, childCount = 0, scrollbarWidth = 10.dp
            )
        }

        // Just play around and make sure it doesn't crash
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
        }
        rule.onNodeWithTag("scrollbar").performMouseInput {
            click(position = Offset(0f, 0f))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private inline fun TextFieldScrollState.assertChangeInOffset(
        expectedChange: Float,
        action: () -> Unit
    ){
        val before = offset
        try{
            action()
        } finally {
            assertEquals(expectedChange, offset - before)
        }
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    fun `basic text field with vertical scrolling test`() {
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

        // Click to focus
        rule.onNodeWithTag("textfield").performMouseInput {
            click(Offset(0f, 0f))
        }

        fun pressDownAndWaitForIdle(){
            rule.onNodeWithTag("textfield").performKeyInput {
                pressKey(Key.DirectionDown)
            }
            rule.waitForIdle()
        }

        // Press "down" 9 times, which should bring the caret to the last visible line
        repeat(9){
            pressDownAndWaitForIdle()
        }
        // Scroll offset should not change yet
        assertEquals(0f, scrollState.offset)

        // The scrollbar thumb should still be at the top, so clicking at 0 shouldn't change
        // the scroll offset
        scrollState.assertChangeInOffset(0f) {
            rule.onNodeWithTag("scrollbar").performMouseInput {
                click(position = Offset(0f, 0f))
            }
        }

        // Press "down" one more time, which should bring the caret to the 11th line, and cause
        // the text field to scroll down by one line (out of a possible 10)
        pressDownAndWaitForIdle()
        assertEquals(10f, scrollState.maxOffset / scrollState.offset)

        // The scrollbar thumb should move by 1/10th of its range, which is 50 pixels, so
        // clicking on the 5th pixel should do nothing
        scrollState.assertChangeInOffset(0f) {
            rule.onNodeWithTag("scrollbar").performMouseInput {
                click(position = Offset(0f, 5f))
            }
        }

        // But clicking on the 4th pixel should scroll to top
        scrollState.assertChangeInOffset(-scrollState.offset) {
            rule.onNodeWithTag("scrollbar").performMouseInput {
                click(position = Offset(0f, 4f))
            }
        }

        // Press down 9 more times to reach the bottom
        repeat(9){
            pressDownAndWaitForIdle()
        }
        assertEquals(scrollState.maxOffset, scrollState.offset)

        // The scrollbar thumb should move to the bottom, so clicking on the 50th pixel should
        // do nothing
        scrollState.assertChangeInOffset(0f) {
            rule.onNodeWithTag("scrollbar").performMouseInput {
                click(position = Offset(0f, 50f))
            }
        }

        // But clicking on the 49th pixel should scroll to the very top
        scrollState.assertChangeInOffset(-scrollState.offset) {
            rule.onNodeWithTag("scrollbar").performMouseInput {
                click(position = Offset(0f, 49f))
            }
        }
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    fun `basic text field with horizontal scrolling test`() {
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

        // Click to focus
        rule.onNodeWithTag("textfield").performMouseInput {
            click(Offset(0f, 0f))
        }

        fun pressRightWaitForIdle(){
            rule.onNodeWithTag("textfield").performKeyInput {
                pressKey(Key.DirectionRight)
            }
            rule.waitForIdle()
        }

        // Press "right" 100 times, which should bring the caret to the very end
        repeat(100){
            pressRightWaitForIdle()
        }
        assertEquals(scrollState.offset, scrollState.maxOffset)

        // The scrollbar thumb should still be at the end, so clicking at the last pixel
        // shouldn't change the scroll offset
        scrollState.assertChangeInOffset(0f) {
            rule.onNodeWithTag("scrollbar").performMouseInput {
                click(position = Offset(99f, 0f))
            }
        }

        // Dragging the scrollbar to the very left should reset the scroll offset to 0
        scrollState.assertChangeInOffset(-scrollState.offset){
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(99f, 0f), end = Offset(0f, 0f))
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun ComposeContentTestRule.testLazyContentWithLineSpacing(
        firstBoxTag: String,
        lastBoxTag: String
    ){
        // Test the size of the scrollbar thumb by trying to drag by one pixel below where it
        // should end
        testVerticalThumbEnd(
            lastPixelPosition = Offset(0f, 49f),
            dragAmount = 5f,
            assertNotMoved = {
                onNodeWithTag(firstBoxTag).assertTopPositionInRootIsEqualTo(0.dp)
            },
            assertDraggedBy = { pixelAmount ->
                onNodeWithTag(firstBoxTag).assertTopPositionInRootIsEqualTo((-2 * pixelAmount).dp)
            },
            assertPageDown = {
                onNodeWithTag(lastBoxTag).assertTopPositionInRootIsEqualTo(80.dp)
            }
        )

        // Scroll to the bottom and check the last item position
        onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 49f), end = Offset(0f, 99f))
        }
        onNodeWithTag(lastBoxTag).assertTopPositionInRootIsEqualTo(80.dp)
    }

    @Test
    fun `lazy list with line spacing`(){
        rule.setContent {
            LazyListTestBox(
                size = 100.dp,
                childSize = 20.dp,
                childCount = 5,
                scrollbarWidth = 10.dp,
                verticalArrangement = Arrangement.spacedBy(25.dp)
            )
        }

        rule.testLazyContentWithLineSpacing("box0", "box4")
    }

    @Test
    fun `lazy grid with line spacing`(){
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

        rule.testLazyContentWithLineSpacing("box0", "box17")
    }

    /**
     * Validates the expected bottom pixel position of the scrollbar thumb.
     *
     * The testing is done by:
     * 1. Clicking the expected bottom pixel of the thumb and verifying that nothing moves.
     * 2. Dragging the thumb by its expected bottom pixel and verifying that the content moved.
     * 3. Clicking one pixel below the expected bottom pixel of the thumb and verifying a page-down.
     */
    private fun ComposeContentTestRule.testVerticalThumbEnd(
        nodeTag: String = "scrollbar",
        lastPixelPosition: Offset,
        dragAmount: Float = 10f,
        assertNotMoved: () -> Unit,
        assertDraggedBy: (Float) -> Unit,
        assertPageDown: () -> Unit,
    ) {
        // Test that clicking the last pixel of the thumb doesn't do anything
        onNodeWithTag(nodeTag).performMouseInput {
            click(lastPixelPosition)
        }
        assertNotMoved()

        // Test that the last pixel of the thumb can be dragged by,
        val dragEnd = lastPixelPosition.plus(Offset(0f, dragAmount))
        onNodeWithTag(nodeTag).performMouseInput {
            instantDrag(start = lastPixelPosition, end = dragEnd)
        }
        assertDraggedBy(dragAmount)

        // Drag back and click one pixel below the thumb
        onNodeWithTag(nodeTag).performMouseInput {
            instantDrag(start = dragEnd, end = lastPixelPosition)
        }
        assertNotMoved()
        onNodeWithTag(nodeTag).performMouseInput {
            click(lastPixelPosition.plus(Offset(0f, 1f)))
        }
        assertPageDown()

        // Click above the thumb to reset the state of the scrollbar back to its initial state
        onNodeWithTag(nodeTag).performMouseInput {
            click(lastPixelPosition)
        }
        assertNotMoved()
    }

    @Test
    fun `thumb bounds test`(){
        // Test that the last pixel of the thumb can be dragged by,
        // and clicking one pixel below causes a page-down
        rule.setContent {
            LazyListTestBox(
                size = 100.dp,
                scrollbarWidth = 10.dp
            ) {
                items(10) {
                    Box(Modifier.size(20.dp).testTag("box$it"))
                }
            }
        }

        rule.testVerticalThumbEnd(
            lastPixelPosition = Offset(0f, 49f),
            dragAmount = 10f,
            assertNotMoved = {
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
            },
            assertDraggedBy = { pixelAmount ->
                rule.onNodeWithTag("box5")
                    .assertTopPositionInRootIsEqualTo((100 - pixelAmount * 2).dp)
            },
            assertPageDown = {
                rule.onNodeWithTag("box5").assertTopPositionInRootIsEqualTo(0.dp)
            }
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun `basic lazy list with sticky headers test`() {
        rule.setContent {
            LazyListTestBox(
                size = 200.dp,
                scrollbarWidth = 10.dp
            ) {
                stickyHeader {
                    Box(Modifier.size(20.dp).testTag("header1"))
                }
                items(9) {
                    Box(Modifier.size(20.dp).testTag("box1_$it"))
                }
                stickyHeader {
                    Box(Modifier.size(20.dp).testTag("header2"))
                }
                items(9) {
                    Box(Modifier.size(20.dp).testTag("box2_$it"))
                }
            }
        }
        rule.onNodeWithTag("box1_0").assertTopPositionInRootIsEqualTo(20.dp)

        rule.testVerticalThumbEnd(
            lastPixelPosition = Offset(0f, 99f),
            dragAmount = 10f,
            assertNotMoved = {
                rule.onNodeWithTag("box1_0").assertTopPositionInRootIsEqualTo(20.dp)
            },
            assertDraggedBy = { pixelAmount ->
                rule.onNodeWithTag("box1_0")
                    .assertTopPositionInRootIsEqualTo((20 - 2 * pixelAmount).dp)
            },
            assertPageDown = {
                rule.onNodeWithTag("box2_0").assertTopPositionInRootIsEqualTo(20.dp)
            }
        )

        // Drag the scrollbar to the bottom and test the position of the last item
        rule.onNodeWithTag("scrollbar").performMouseInput {
            instantDrag(start = Offset(0f, 0f), end = Offset(0f, 100f))
        }
        rule.onNodeWithTag("box2_8").assertTopPositionInRootIsEqualTo(180.dp)

        // Press above the scrollbar and test the new position
        rule.onNodeWithTag("scrollbar").performMouseInput {
            click(position = Offset(0f, 0f))
        }
        rule.onNodeWithTag("box1_0").assertTopPositionInRootIsEqualTo(20.dp)
    }


    @OptIn(InternalTestApi::class)
    private fun ComposeTestRule.performMouseScroll(x: Int, y: Int, delta: Float) {
        (this as DesktopComposeTestRule).scene.sendPointerEvent(
            PointerEventType.Scroll,
            Offset(x.toFloat(), y.toFloat()),
            scrollDelta = Offset(x = 0f, y = delta),
            nativeEvent = awtWheelEvent()
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
    // the formula was determined experimentally based on macOS Finder behaviour
    // macOS driver will send events with accelerating delta
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        return -event.totalScrollDelta * 10.dp.toPx()
    }
}

private val PointerEvent.totalScrollDelta
    get() = this.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }

/**
 * Returns the time needed for the given number of page-scrolls when pressing the scrollbar track
 * outside the thumb.
 */
private fun timeUntilScrollsByPressOnTrack(count: Int) = when {
    count <= 1 -> 0L
    count == 2 -> DelayBeforeSecondScrollOnTrackPress
    else -> DelayBeforeSecondScrollOnTrackPress + (count - 2) * DelayBetweenScrollsOnTrackPress
}

