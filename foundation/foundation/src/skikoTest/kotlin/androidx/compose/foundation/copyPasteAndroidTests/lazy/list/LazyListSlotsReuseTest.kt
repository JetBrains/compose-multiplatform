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

import androidx.compose.foundation.assertWithMessage
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class LazyListSlotsReuseTest {

    val density = Density(1f)
    val itemsSizePx = 30f
    val itemsSizeDp = with(density) { itemsSizePx.toDp() }

    @Test
    fun scroll1ItemScrolledOffItemIsKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }

        onNodeWithTag("0")
            .assertIsDisplayed()

        runOnIdle {
            scope.launch {
                state.scrollToItem(1)
            }
        }

        onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("1")
            .assertIsDisplayed()
    }

    @Test
    fun scroll2ItemsScrolledOffItemsAreKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }

        onNodeWithTag("0")
            .assertIsDisplayed()
        onNodeWithTag("1")
            .assertIsDisplayed()

        runOnIdle {
            scope.launch {
                state.scrollToItem(2)
            }
        }

        onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("1")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("2")
            .assertIsDisplayed()
    }

    @Test
    fun checkMaxItemsKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * (DefaultMaxItemsToRetain + 0.5f)),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }

        runOnIdle {
            scope.launch {
                state.scrollToItem(DefaultMaxItemsToRetain + 1)
            }
        }

        repeat(DefaultMaxItemsToRetain) {
            onNodeWithTag("$it")
                .assertExists()
                .assertIsNotDisplayed()
        }
        onNodeWithTag("$DefaultMaxItemsToRetain")
            .assertDoesNotExist()
        onNodeWithTag("${DefaultMaxItemsToRetain + 1}")
            .assertIsDisplayed()
    }

    @Test
    fun scroll3Items2OfScrolledOffItemsAreKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }

        onNodeWithTag("0")
            .assertIsDisplayed()
        onNodeWithTag("1")
            .assertIsDisplayed()

        runOnIdle {
            scope.launch {
                // after this step 0 and 1 are in reusable buffer
                state.scrollToItem(2)

                // this step requires one item and will take the last item from the buffer - item
                // 1 plus will put 2 in the buffer. so expected buffer is items 2 and 0
                state.scrollToItem(3)
            }
        }

        // recycled
        onNodeWithTag("1")
            .assertDoesNotExist()

        // in buffer
        onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("2")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        onNodeWithTag("3")
            .assertIsDisplayed()
        onNodeWithTag("4")
            .assertIsDisplayed()
    }

    @Test
    fun doMultipleScrollsOneByOne() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(1) // buffer is [0]
                state.scrollToItem(2) // 0 used, buffer is [1]
                state.scrollToItem(3) // 1 used, buffer is [2]
                state.scrollToItem(4) // 2 used, buffer is [3]
            }
        }

        // recycled
        onNodeWithTag("0")
            .assertDoesNotExist()
        onNodeWithTag("1")
            .assertDoesNotExist()
        onNodeWithTag("2")
            .assertDoesNotExist()

        // in buffer
        onNodeWithTag("3")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        onNodeWithTag("4")
            .assertIsDisplayed()
        onNodeWithTag("5")
            .assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOnce() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState(10)
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(8) // buffer is [10, 11]
            }
        }

        // in buffer
        onNodeWithTag("10")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("11")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        onNodeWithTag("8")
            .assertIsDisplayed()
        onNodeWithTag("9")
            .assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOneByOne() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState(10)
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(9) // buffer is [11]
                state.scrollToItem(7) // 11 reused, buffer is [9]
                state.scrollToItem(6) // 9 reused, buffer is [8]
            }
        }

        // in buffer
        onNodeWithTag("8")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        onNodeWithTag("6")
            .assertIsDisplayed()
        onNodeWithTag("7")
            .assertIsDisplayed()
    }

    @Test
    fun scrollingBackReusesTheSameSlot() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        var counter0 = 0
        var counter1 = 10
        var rememberedValue0 = -1
        var rememberedValue1 = -1
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    if (it == 0) {
                        rememberedValue0 = remember { counter0++ }
                    }
                    if (it == 1) {
                        rememberedValue1 = remember { counter1++ }
                    }
                    Spacer(Modifier.height(itemsSizeDp).fillParentMaxWidth().testTag("$it"))
                }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(2) // buffer is [0, 1]
                state.scrollToItem(0) // scrolled back, 0 and 1 are reused back. buffer: [2, 3]
            }
        }

        runOnIdle {
            assertWithMessage("Item 0 restored remembered value is $rememberedValue0")
                .that(rememberedValue0).isEqualTo(0)
            assertWithMessage("Item 1 restored remembered value is $rememberedValue1")
                .that(rememberedValue1).isEqualTo(10)
        }

        onNodeWithTag("0")
            .assertIsDisplayed()
        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("3")
            .assertExists()
            .assertIsNotDisplayed()
    }

    @Test
    fun differentContentTypes() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        val visibleItemsCount = (DefaultMaxItemsToRetain + 1) * 2
        val startOfType1 = DefaultMaxItemsToRetain + 1
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * (visibleItemsCount - 0.5f)),
                state
            ) {
                items(
                    100,
                    contentType = { if (it >= startOfType1) 1 else 0 }
                ) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }

        for (i in 0 until visibleItemsCount) {
            onNodeWithTag("$i")
                .assertIsDisplayed()
        }

        runOnIdle {
            scope.launch {
                state.scrollToItem(visibleItemsCount)
            }
        }

        onNodeWithTag("$visibleItemsCount")
            .assertIsDisplayed()

        // [DefaultMaxItemsToRetain] items of type 0 are left for reuse
        for (i in 0 until DefaultMaxItemsToRetain) {
            onNodeWithTag("$i")
                .assertExists()
                .assertIsNotDisplayed()
        }
        onNodeWithTag("$DefaultMaxItemsToRetain")
            .assertDoesNotExist()

        // and 7 items of type 1
        for (i in startOfType1 until startOfType1 + DefaultMaxItemsToRetain) {
            onNodeWithTag("$i")
                .assertExists()
                .assertIsNotDisplayed()
        }
        onNodeWithTag("${startOfType1 + DefaultMaxItemsToRetain}")
            .assertDoesNotExist()
    }

    @Test
    fun differentTypesFromDifferentItemCalls() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState()
            LazyColumn(
                Modifier.height(itemsSizeDp * 2.5f),
                state
            ) {
                val content = @Composable { tag: String ->
                    Spacer(Modifier.height(itemsSizeDp).width(10.dp).testTag(tag))
                }
                item(contentType = "not-to-reuse-0") {
                    content("0")
                }
                item(contentType = "reuse") {
                    content("1")
                }
                items(
                    List(100) { it + 2 },
                    contentType = { if (it == 10) "reuse" else "not-to-reuse-$it" }) {
                    content("$it")
                }
            }
        }

        runOnIdle {
            scope.launch {
                state.scrollToItem(2)
                // now items 0 and 1 are put into reusables
            }
        }

        onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("1")
            .assertExists()
            .assertIsNotDisplayed()

        runOnIdle {
            scope.launch {
                state.scrollToItem(9)
                // item 10 should reuse slot 1
            }
        }

        onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag("1")
            .assertDoesNotExist()
        onNodeWithTag("9")
            .assertIsDisplayed()
        onNodeWithTag("10")
            .assertIsDisplayed()
        onNodeWithTag("11")
            .assertIsDisplayed()
    }
}

private val DefaultMaxItemsToRetain = 7
