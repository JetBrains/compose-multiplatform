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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
class LazyListHeadersTest {

    private val LazyListTag = "LazyList"

    @Test
    fun lazyColumnShowsHeader() = runSkikoComposeUiTest {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        setContent {
            LazyColumn(Modifier.height(300.dp)) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag(secondHeaderTag)
            .assertDoesNotExist()
    }

    @Test
    fun lazyColumnShowsHeadersOnScroll() = runSkikoComposeUiTest {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: LazyListState

        setContent {
            LazyColumn(
                Modifier.height(300.dp).testTag(LazyListTag),
                rememberLazyListState().also { state = it }
            ) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        onNodeWithTag(LazyListTag).performMouseInput {
            scroll(102f)
        }

        onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)

        runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnHeaderIsReplaced() = runSkikoComposeUiTest {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        setContent {
            LazyColumn(Modifier.height(300.dp).testTag(LazyListTag)) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(firstHeaderTag)
                    )
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(secondHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyListTag).performMouseInput {
            scroll(105f)
        }

        onNodeWithTag(firstHeaderTag)
            .assertIsNotDisplayed()

        onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowShowsHeader() = runSkikoComposeUiTest {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        setContent {
            LazyRow(Modifier.width(300.dp)) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag(secondHeaderTag)
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowShowsHeadersOnScroll() = runSkikoComposeUiTest {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: LazyListState

        setContent {
            LazyRow(
                Modifier.width(300.dp).testTag(LazyListTag),
                rememberLazyListState().also { state = it }
            ) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        onNodeWithTag(LazyListTag).performMouseInput {
            scroll(102f, ScrollWheel.Horizontal)
        }

        onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)

        runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowHeaderIsReplaced() = runSkikoComposeUiTest {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        setContent {
            LazyRow(Modifier.width(300.dp).testTag(LazyListTag)) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyListTag).performMouseInput {
            scroll(102f, ScrollWheel.Horizontal)
        }

        onNodeWithTag(firstHeaderTag)
            .assertIsNotDisplayed()

        onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()
    }

    @Test
    fun headerIsDisplayedWhenItIsFullyInContentPadding() = runSkikoComposeUiTest {
        val headerTag = "header"
        val itemIndexPx = 100
        val itemIndexDp = with(density) { itemIndexPx.toDp() }
        lateinit var state: LazyListState

        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            LazyColumn(
                Modifier.requiredSize(itemIndexDp * 4),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(top = itemIndexDp * 2)
            ) {
                stickyHeader {
                    Spacer(Modifier.requiredSize(itemIndexDp).testTag(headerTag))
                }

                items((0..4).toList()) {
                    Spacer(Modifier.requiredSize(itemIndexDp).testTag("$it"))
                }
            }
        }

        runOnIdle {
            scope.launch { state.scrollToItem(1, itemIndexPx / 2) }
        }

        onNodeWithTag(headerTag)
            .assertTopPositionInRootIsEqualTo(itemIndexDp / 2)

        runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(
                itemIndexPx / 2 - /* content padding size */ itemIndexPx * 2,
                state.layoutInfo.visibleItemsInfo.first().offset
            )
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemIndexDp * 3 / 2)
    }
}
