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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyListPrefetcherTest {

    @get:Rule
    val rule = createComposeRule()

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyListState

    @Test
    fun notPrefetchingForwardInitially() {
        composeList()

        rule.onNodeWithTag("2")
            .assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeList(firstItem = 2)

        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll() {
        composeList()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(2)

        rule.onNodeWithTag("2")
            .assertExists()
        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll() {
        composeList(firstItem = 2, itemOffset = 10)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(1)

        rule.onNodeWithTag("1")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackward() {
        composeList(firstItem = 1)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(3)

        rule.onNodeWithTag("3")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        waitForPrefetch(0)

        rule.onNodeWithTag("0")
            .assertExists()
        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardTwice() {
        composeList()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(2)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemsSizePx / 2f)
                state.scrollBy(itemsSizePx / 2f)
            }
        }

        waitForPrefetch(3)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
        rule.onNodeWithTag("3")
            .assertExists()
        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardTwice() {
        composeList(firstItem = 4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(2)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemsSizePx / 2f)
                state.scrollBy(-itemsSizePx / 2f)
            }
        }

        waitForPrefetch(1)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardReverseLayout() {
        composeList(firstItem = 1, reverseLayout = true)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(3)

        rule.onNodeWithTag("3")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        waitForPrefetch(0)

        rule.onNodeWithTag("0")
            .assertExists()
        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardWithContentPadding() {
        val halfItemSize = itemsSizeDp / 2f
        composeList(
            firstItem = 2,
            itemOffset = 5,
            contentPadding = PaddingValues(vertical = halfItemSize)
        )

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
        rule.onNodeWithTag("2")
            .assertIsDisplayed()
        rule.onNodeWithTag("3")
            .assertIsDisplayed()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(3)

        rule.onNodeWithTag("4")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
            }
        }

        waitForPrefetch(0)

        rule.onNodeWithTag("0")
            .assertExists()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun prefetchingStickyHeaderItem() {
        rule.setContent {
            state = rememberLazyListState(
                initialFirstVisibleItemIndex = 1,
                initialFirstVisibleItemScrollOffset = itemsSizePx / 2
            )
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state,
            ) {
                stickyHeader {
                    Spacer(
                        Modifier
                            .height(itemsSizeDp)
                            .fillParentMaxWidth()
                            .testTag("header")
                    )
                }
                items(100) {
                    Spacer(
                        Modifier
                            .height(itemsSizeDp)
                            .fillParentMaxWidth()
                            .testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        rule.onNodeWithTag("header")
            .assertIsDisplayed()
        rule.onNodeWithTag("0")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()
    }

    private fun waitForPrefetch(index: Int) {
        rule.waitUntil {
            activeNodes.contains(index) && activeMeasuredNodes.contains(index)
        }
    }

    private val activeNodes = mutableSetOf<Int>()
    private val activeMeasuredNodes = mutableSetOf<Int>()

    private fun composeList(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        rule.setContent {
            state = rememberLazyListState(
                initialFirstVisibleItemIndex = firstItem,
                initialFirstVisibleItemScrollOffset = itemOffset
            )
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding
            ) {
                items(100) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose {
                            activeNodes.remove(it)
                            activeMeasuredNodes.remove(it)
                        }
                    }
                    Spacer(
                        Modifier
                            .height(itemsSizeDp)
                            .fillParentMaxWidth()
                            .testTag("$it")
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                activeMeasuredNodes.add(it)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
    }
}
