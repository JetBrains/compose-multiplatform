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
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.DesktopPlatformAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.DesktopComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skija.Surface
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Suppress("WrapUnaryOperator")
@OptIn(ExperimentalTestApi::class)
class ScrollbarTest {
    @get:Rule
    val rule = createComposeRule()

    // don't inline, surface controls canvas life time
    private val surface = Surface.makeRasterN32Premul(100, 100)
    private val canvas = surface.canvas

    @Test
    fun `drag slider to the middle`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp)
        }
    }

    @Test
    fun `drag slider to the edges`() {
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 99f), end = Offset(0f, -500f))
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

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(10f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

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

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 99f), end = Offset(0f, -500f))
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

            rule.onNodeWithTag("scrollbar").performGesture {
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

            rule.onNodeWithTag("scrollbar").performGesture {
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

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 25f), end = Offset(0f, 500f))
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

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 0f), end = Offset(0f, 11f), durationMillis = 1)
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

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 0f), end = Offset(0f, 26f), durationMillis = 1)
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

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 0f), end = Offset(0f, 10000f), durationMillis = 1)
            }
            rule.awaitIdle()
            assertEquals(15, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)

            rule.onNodeWithTag("scrollbar").performGesture {
                swipe(start = Offset(0f, 99f), end = Offset(0f, -10000f), durationMillis = 1)
            }
            rule.awaitIdle()
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
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

    private fun ComposeTestRule.performMouseScroll(x: Int, y: Int, delta: Float) {
        (this as DesktopComposeTestRule).window.onMouseScroll(
            x, y, MouseScrollEvent(MouseScrollUnit.Line(delta), Orientation.Vertical)
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
        state: LazyListState,
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("column"),
                state
            ) {
                items((0 until childCount).toList()) {
                    Box(Modifier.size(childSize).testTag("box$it"))
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state, childCount, childSize),
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun withTestEnvironment(content: @Composable () -> Unit) = Providers(
        ScrollbarStyleAmbient provides ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            shape = RectangleShape,
            hoverDurationMillis = 300,
            unhoverColor = Color.Black,
            hoverColor = Color.Red
        ),
        DesktopPlatformAmbient provides DesktopPlatform.MacOS,
        content = content
    )
}
