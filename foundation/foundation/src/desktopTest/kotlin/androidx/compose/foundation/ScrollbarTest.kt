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
import androidx.compose.foundation.gestures.ScrollConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.DesktopComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Suppress("WrapUnaryOperator")
@OptIn(ExperimentalTestApi::class)
class ScrollbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `drag slider to the middle`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp)
        }
    }

    @Test
    fun `drag slider when it is hidden`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()
            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun `drag slider to the edges`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 99f), end = Offset(0f, -500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun `drag outside slider`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(10f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun `drag outside slider and back`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                down(Offset(10f, 25f))
                moveBy(Offset(0f, 50f))
                moveBy(Offset(0f, -50f))
                up()
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    // TODO(demin): write a test when we support DesktopComposeTestRule.mainClock:
    //  see https://github.com/JetBrains/compose-jb/issues/637
//    fun `move mouse to the slider and drag it`() {
//        ...
//        rule.performMouseMove(0, 25)
//        rule.mainClock.advanceTimeByFrame()
//        down(Offset(0f, 25f))
//        rule.mainClock.advanceTimeByFrame()
//        moveTo(Offset(0f, 30f))
//        rule.mainClock.advanceTimeByFrame()
//        moveTo(Offset(0f, 50f))
//        rule.mainClock.advanceTimeByFrame()
//        up()
//        ...
//    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Test
    fun `mouseScroll over slider`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
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
    @Test
    fun `mouseScroll over scrollbar outside slider`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
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
    @Test
    fun `vertical mouseScroll over horizontal scrollbar `() {
        runBlocking(Dispatchers.Main) {
            // TODO(demin): write tests for vertical mouse scrolling over
            //  horizontalScrollbar for the case when we have two-way scrollable content:
            //  Modifier.verticalScrollbar(...).horizontalScrollbar(...)
            //  Content should scroll vertically.
        }
    }

    @Test
    fun `mouseScroll over column then drag to the beginning`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(20, 25, 10f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 99f), end = Offset(0f, -500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test(timeout = 3000)
    fun `press on scrollbar outside slider`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                down(Offset(0f, 26f))
            }

            tryUntilSucceeded {
                rule.awaitIdle()
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
            }
        }
    }

    @Test(timeout = 3000)
    fun `press on the end of scrollbar outside slider`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                down(Offset(0f, 99f))
            }

            tryUntilSucceeded {
                rule.awaitIdle()
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-300.dp)
            }
        }
    }

    @Test(timeout = 3000)
    fun `dynamically change content then drag slider to the end`() {
        runBlocking(Dispatchers.Main) {
            val isContentVisible = mutableStateOf(false)
            rule.setContent {
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

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
        }
    }

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalFoundationApi::class)
    @Test(timeout = 3000)
    fun `scroll by less than one page in lazy list`() {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 0f), end = Offset(0f, 11f))
            }
            rule.awaitIdle()
            assertEquals(2, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalFoundationApi::class)
    @Test(timeout = 3000)
    fun `scroll in reversed lazy list`() {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    reverseLayout = true
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 99f), end = Offset(0f, 88f))
            }
            rule.awaitIdle()
            assertEquals(2, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalFoundationApi::class)
    @Test(timeout = 3000)
    fun `scroll by more than one page in lazy list`() {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 0f), end = Offset(0f, 26f))
            }
            rule.awaitIdle()
            assertEquals(5, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalFoundationApi::class)
    @Test(timeout = 3000)
    fun `scroll outside of scrollbar bounds in lazy list`() {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 0f), end = Offset(0f, 10000f))
            }
            rule.awaitIdle()
            assertEquals(15, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)

            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 99f), end = Offset(0f, -10000f))
            }
            rule.awaitIdle()
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun `drag lazy slider when it is hidden`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                LazyTestBox(
                    size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()
            rule.onNodeWithTag("scrollbar").performTouchInput {
                instantSwipe(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
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
            scrollDelta = Offset(x = 0f, y = delta)
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
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            val state = rememberScrollState()

            Column(
                Modifier.fillMaxSize().testTag("column").verticalScroll(state)
            ) {
                repeat(childCount) {
                    Box(Modifier.size(childSize).testTag("box$it"))
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
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

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun LazyTestBox(
        state: LazyListState = rememberLazyListState(),
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
        reverseLayout: Boolean = false
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("column"),
                state,
                reverseLayout = reverseLayout
            ) {
                items((0 until childCount).toList()) {
                    Box(Modifier.size(childSize).testTag("box$it"))
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    private fun TouchInjectionScope.instantSwipe(start: Offset, end: Offset) {
        down(start)
        moveTo(end)
        up()
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