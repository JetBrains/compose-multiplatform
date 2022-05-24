/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyGridsContentPaddingTest {
    private val LazyListTag = "LazyList"
    private val ItemTag = "item"
    private val ContainerTag = "container"

    @get:Rule
    val rule = createComposeRule()

    private var itemSize: Dp = Dp.Infinity
    private var smallPaddingSize: Dp = Dp.Infinity
    private var itemSizePx = 50f
    private var smallPaddingSizePx = 12f

    @Before
    fun before() {
        with(rule.density) {
            itemSize = itemSizePx.toDp()
            smallPaddingSize = smallPaddingSizePx.toDp()
        }
    }

    @Test
    fun verticalGrid_contentPaddingIsApplied() {
        lateinit var state: LazyGridState
        val containerSize = itemSize * 2
        val largePaddingSize = itemSize
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(containerSize)
                    .testTag(LazyListTag),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(
                    start = smallPaddingSize,
                    top = largePaddingSize,
                    end = smallPaddingSize,
                    bottom = largePaddingSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(Modifier.height(itemSize).testTag(ItemTag))
                }
            }
        }

        rule.onNodeWithTag(ItemTag)
            .assertLeftPositionInRootIsEqualTo(smallPaddingSize)
            .assertTopPositionInRootIsEqualTo(largePaddingSize)
            .assertWidthIsEqualTo(containerSize - smallPaddingSize * 2)
            .assertHeightIsEqualTo(itemSize)

        state.scrollBy(largePaddingSize)

        rule.onNodeWithTag(ItemTag)
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertHeightIsEqualTo(itemSize)
    }

    @Test
    fun verticalGrid_contentPaddingIsNotAffectingScrollPosition() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(itemSize * 2)
                    .testTag(LazyListTag),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(
                    top = itemSize,
                    bottom = itemSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(Modifier.height(itemSize).testTag(ItemTag))
                }
            }
        }

        state.assertScrollPosition(0, 0.dp)

        state.scrollBy(itemSize)

        state.assertScrollPosition(0, itemSize)
    }

    @Test
    fun verticalGrid_scrollForwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyGridState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(
                    top = padding,
                    bottom = padding
                )
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(padding)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize + padding)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2 + padding)

        state.scrollBy(padding)

        state.assertScrollPosition(1, padding - itemSize)

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 3)
    }

    @Test
    fun verticalGrid_scrollBackwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyGridState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(itemSize + padding * 2)
                    .testTag(LazyListTag),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(
                    top = padding,
                    bottom = padding
                )
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        state.scrollBy(itemSize * 3)
        state.scrollBy(-itemSize * 1.5f)

        state.assertScrollPosition(1, itemSize * 0.5f)

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 3.5f - padding)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 4.5f - padding)
    }

    @Test
    fun verticalGrid_scrollForwardTillTheEnd() {
        lateinit var state: LazyGridState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(
                    top = padding,
                    bottom = padding
                )
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        state.scrollBy(itemSize * 3)

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 3 - padding)

        // there are no space to scroll anymore, so it should change nothing
        state.scrollBy(10.dp)

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 3 - padding)
    }

    @Test
    fun verticalGrid_scrollForwardTillTheEndAndABitBack() {
        lateinit var state: LazyGridState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(
                    top = padding,
                    bottom = padding
                )
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        state.scrollBy(itemSize * 3)
        state.scrollBy(-itemSize / 2)

        state.assertScrollPosition(2, itemSize / 2)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 3.5f - padding)
    }

    @Test
    fun verticalGrid_contentPaddingFixedWidthContainer() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag).width(itemSize + 8.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 4.dp,
                        end = 6.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(listOf(1)) {
                        Spacer(Modifier.size(itemSize).testTag(ItemTag))
                    }
                }
            }
        }

        rule.onNodeWithTag(ItemTag)
            .assertLeftPositionInRootIsEqualTo(2.dp)
            .assertTopPositionInRootIsEqualTo(4.dp)
            .assertWidthIsEqualTo(itemSize)
            .assertHeightIsEqualTo(itemSize)

        rule.onNodeWithTag(ContainerTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(itemSize + 2.dp + 6.dp)
            .assertHeightIsEqualTo(itemSize + 4.dp + 8.dp)
    }

    @Test
    fun verticalGrid_contentPaddingAndNoContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 4.dp,
                        end = 6.dp,
                        bottom = 8.dp
                    )
                ) { }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(8.dp)
            .assertHeightIsEqualTo(12.dp)
    }

    @Test
    fun verticalGrid_contentPaddingAndZeroSizedItem() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 4.dp,
                        end = 6.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(0) { }
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(8.dp)
            .assertHeightIsEqualTo(12.dp)
    }

    @Test
    fun verticalGrid_contentPaddingAndReverseLayout() {
        val topPadding = itemSize * 2
        val bottomPadding = itemSize / 2
        val listSize = itemSize * 3
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                reverseLayout = true,
                state = rememberLazyGridState().also { state = it },
                modifier = Modifier.size(listSize),
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            ) {
                items(3) { index ->
                    Box(Modifier.size(itemSize).testTag("$index"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(listSize - bottomPadding - itemSize)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(listSize - bottomPadding - itemSize * 2)
        // Partially visible.
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(-itemSize / 2)

        // Scroll to the top.
        state.scrollBy(itemSize * 2.5f)

        rule.onNodeWithTag("2").assertTopPositionInRootIsEqualTo(topPadding)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun verticalGrid_largeContentPaddingAndReverseLayout() {
        val topPadding = itemSize * 2
        val bottomPadding = itemSize * 2
        val listSize = itemSize * 3
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                reverseLayout = true,
                state = rememberLazyGridState().also { state = it },
                modifier = Modifier.size(listSize),
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            ) {
                items(3) { index ->
                    Box(Modifier.size(itemSize).testTag("$index"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        // Not visible.
        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        // Scroll to the top.
        state.scrollBy(itemSize * 5f)

        rule.onNodeWithTag("2").assertTopPositionInRootIsEqualTo(topPadding)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun column_overscrollWithContentPadding() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize + smallPaddingSize * 2)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(
                        vertical = smallPaddingSize
                    )
                ) {
                    items(2) {
                        Box(Modifier.testTag("$it").height(itemSize))
                    }
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(smallPaddingSize)
            .assertHeightIsEqualTo(itemSize)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(smallPaddingSize + itemSize)
            .assertHeightIsEqualTo(itemSize)

        rule.runOnIdle {
            runBlocking {
                // itemSizePx is the maximum offset, plus if we overscroll the content padding
                // the layout mechanism will decide the item 0 is not needed until we start
                // filling the over scrolled gap.
                state.scrollBy(value = itemSizePx + smallPaddingSizePx * 1.5f)
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(smallPaddingSize)
            .assertHeightIsEqualTo(itemSize)

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(smallPaddingSize - itemSize)
            .assertHeightIsEqualTo(itemSize)
    }

    @Test
    fun totalPaddingLargerParentSize_initialState() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        rule.runOnIdle {
            state.assertScrollPosition(0, 0.dp)
            state.assertVisibleItems(0 to 0.dp)
            state.assertLayoutInfoOffsetRange(-itemSize, itemSize * 0.5f)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollByPadding() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(1, 0.dp)
            state.assertVisibleItems(0 to -itemSize, 1 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollToLastItem() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollTo(3)

        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollToLastItemByDelta() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize * 3)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollTillTheEnd() {
        // the whole end content padding is displayed
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize * 4.5f)

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(-itemSize * 0.5f)

        rule.runOnIdle {
            state.assertScrollPosition(3, itemSize * 1.5f)
            state.assertVisibleItems(3 to -itemSize * 1.5f)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_initialState() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize * 2)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(0, 0.dp)
            state.assertVisibleItems(0 to 0.dp)
            state.assertLayoutInfoOffsetRange(-itemSize * 2, -itemSize * 0.5f)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollByPadding() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize * 2)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize * 2)

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(2, 0.dp)
            state.assertVisibleItems(0 to -itemSize * 2, 1 to -itemSize, 2 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollToLastItem() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize * 2)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollTo(3)

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(1 to -itemSize * 2, 2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollToLastItemByDelta() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize * 2)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize * 3)

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(1 to -itemSize * 2, 2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollTillTheEnd() {
        // only the end content padding is displayed
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyVerticalGrid(
                    GridCells.Fixed(1),
                    state = state,
                    contentPadding = PaddingValues(vertical = itemSize * 2)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(
            itemSize * 1.5f + // container size
                itemSize * 2 + // start padding
                itemSize * 3 // all items
        )

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, itemSize * 3.5f)
            state.assertVisibleItems(3 to -itemSize * 3.5f)
        }
    }

    // @Test
    // fun row_contentPaddingIsApplied() {
    //     lateinit var state: LazyGridState
    //     val containerSize = itemSize * 2
    //     val largePaddingSize = itemSize
    //     rule.setContent {
    //         LazyRow(
    //             modifier = Modifier.requiredSize(containerSize)
    //                 .testTag(LazyListTag),
    //             state = rememberLazyGridState().also { state = it },
    //             contentPadding = PaddingValues(
    //                 top = smallPaddingSize,
    //                 start = largePaddingSize,
    //                 bottom = smallPaddingSize,
    //                 end = largePaddingSize
    //             )
    //         ) {
    //             items(listOf(1)) {
    //                 Spacer(Modifier.fillParentMaxHeight().width(itemSize).testTag(ItemTag))
    //             }
    //         }
    //     }

    //     rule.onNodeWithTag(ItemTag)
    //         .assertTopPositionInRootIsEqualTo(smallPaddingSize)
    //         .assertLeftPositionInRootIsEqualTo(largePaddingSize)
    //         .assertHeightIsEqualTo(containerSize - smallPaddingSize * 2)
    //         .assertWidthIsEqualTo(itemSize)

    //     state.scrollBy(largePaddingSize)

    //     rule.onNodeWithTag(ItemTag)
    //         .assertLeftPositionInRootIsEqualTo(0.dp)
    //         .assertWidthIsEqualTo(itemSize)
    // }

    // @Test
    // fun row_contentPaddingIsNotAffectingScrollPosition() {
    //     lateinit var state: LazyGridState
    //     val itemSize = with(rule.density) {
    //         50.dp.roundToPx().toDp()
    //     }
    //     rule.setContent {
    //         LazyRow(
    //             modifier = Modifier.requiredSize(itemSize * 2)
    //                 .testTag(LazyListTag),
    //             state = rememberLazyGridState().also { state = it },
    //             contentPadding = PaddingValues(
    //                 start = itemSize,
    //                 end = itemSize
    //             )
    //         ) {
    //             items(listOf(1)) {
    //                 Spacer(Modifier.fillParentMaxHeight().width(itemSize).testTag(ItemTag))
    //             }
    //         }
    //     }

    //     state.assertScrollPosition(0, 0.dp)

    //     state.scrollBy(itemSize)

    //     state.assertScrollPosition(0, itemSize)
    // }

    // @Test
    // fun row_scrollForwardItemWithinStartPaddingDisplayed() {
    //     lateinit var state: LazyGridState
    //     val padding = itemSize * 1.5f
    //     rule.setContent {
    //         LazyRow(
    //             modifier = Modifier.requiredSize(padding * 2 + itemSize)
    //                 .testTag(LazyListTag),
    //             state = rememberLazyGridState().also { state = it },
    //             contentPadding = PaddingValues(
    //                 start = padding,
    //                 end = padding
    //             )
    //         ) {
    //             items((0..3).toList()) {
    //                 Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
    //             }
    //         }
    //     }

    //     rule.onNodeWithTag("0")
    //         .assertLeftPositionInRootIsEqualTo(padding)
    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(itemSize + padding)
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 2 + padding)

    //     state.scrollBy(padding)

    //     state.assertScrollPosition(1, padding - itemSize)

    //     rule.onNodeWithTag("0")
    //         .assertLeftPositionInRootIsEqualTo(0.dp)
    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(itemSize)
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 2)
    //     rule.onNodeWithTag("3")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 3)
    // }

    // @Test
    // fun row_scrollBackwardItemWithinStartPaddingDisplayed() {
    //     lateinit var state: LazyGridState
    //     val padding = itemSize * 1.5f
    //     rule.setContent {
    //         LazyRow(
    //             modifier = Modifier.requiredSize(itemSize + padding * 2)
    //                 .testTag(LazyListTag),
    //             state = rememberLazyGridState().also { state = it },
    //             contentPadding = PaddingValues(
    //                 start = padding,
    //                 end = padding
    //             )
    //         ) {
    //             items((0..3).toList()) {
    //                 Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
    //             }
    //         }
    //     }

    //     state.scrollBy(itemSize * 3)
    //     state.scrollBy(-itemSize * 1.5f)

    //     state.assertScrollPosition(1, itemSize * 0.5f)

    //     rule.onNodeWithTag("0")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 1.5f - padding)
    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f - padding)
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 3.5f - padding)
    //     rule.onNodeWithTag("3")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 4.5f - padding)
    // }

    // @Test
    // fun row_scrollForwardTillTheEnd() {
    //     lateinit var state: LazyGridState
    //     val padding = itemSize * 1.5f
    //     rule.setContent {
    //         LazyRow(
    //             modifier = Modifier.requiredSize(padding * 2 + itemSize)
    //                 .testTag(LazyListTag),
    //             state = rememberLazyGridState().also { state = it },
    //             contentPadding = PaddingValues(
    //                 start = padding,
    //                 end = padding
    //             )
    //         ) {
    //             items((0..3).toList()) {
    //                 Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
    //             }
    //         }
    //     }

    //     state.scrollBy(itemSize * 3)

    //     state.assertScrollPosition(3, 0.dp)

    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(itemSize - padding)
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 2 - padding)
    //     rule.onNodeWithTag("3")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 3 - padding)

    //     // there are no space to scroll anymore, so it should change nothing
    //     state.scrollBy(10.dp)

    //     state.assertScrollPosition(3, 0.dp)

    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(itemSize - padding)
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 2 - padding)
    //     rule.onNodeWithTag("3")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 3 - padding)
    // }

    // @Test
    // fun row_scrollForwardTillTheEndAndABitBack() {
    //     lateinit var state: LazyGridState
    //     val padding = itemSize * 1.5f
    //     rule.setContent {
    //         LazyRow(
    //             modifier = Modifier.requiredSize(padding * 2 + itemSize)
    //                 .testTag(LazyListTag),
    //             state = rememberLazyGridState().also { state = it },
    //             contentPadding = PaddingValues(
    //                 start = padding,
    //                 end = padding
    //             )
    //         ) {
    //             items((0..3).toList()) {
    //                 Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
    //             }
    //         }
    //     }

    //     state.scrollBy(itemSize * 3)
    //     state.scrollBy(-itemSize / 2)

    //     state.assertScrollPosition(2, itemSize / 2)

    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 1.5f - padding)
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f - padding)
    //     rule.onNodeWithTag("3")
    //         .assertLeftPositionInRootIsEqualTo(itemSize * 3.5f - padding)
    // }

    // @Test
    // fun row_contentPaddingAndWrapContent() {
    //     rule.setContent {
    //         Box(modifier = Modifier.testTag(ContainerTag)) {
    //             LazyRow(
    //                 contentPadding = PaddingValues(
    //                     start = 2.dp,
    //                     top = 4.dp,
    //                     end = 6.dp,
    //                     bottom = 8.dp
    //                 )
    //             ) {
    //                 items(listOf(1)) {
    //                     Spacer(Modifier.requiredSize(itemSize).testTag(ItemTag))
    //                 }
    //             }
    //         }
    //     }

    //     rule.onNodeWithTag(ItemTag)
    //         .assertLeftPositionInRootIsEqualTo(2.dp)
    //         .assertTopPositionInRootIsEqualTo(4.dp)
    //         .assertWidthIsEqualTo(itemSize)
    //         .assertHeightIsEqualTo(itemSize)

    //     rule.onNodeWithTag(ContainerTag)
    //         .assertLeftPositionInRootIsEqualTo(0.dp)
    //         .assertTopPositionInRootIsEqualTo(0.dp)
    //         .assertWidthIsEqualTo(itemSize + 2.dp + 6.dp)
    //         .assertHeightIsEqualTo(itemSize + 4.dp + 8.dp)
    // }

    // @Test
    // fun row_contentPaddingAndNoContent() {
    //     rule.setContent {
    //         Box(modifier = Modifier.testTag(ContainerTag)) {
    //             LazyRow(
    //                 contentPadding = PaddingValues(
    //                     start = 2.dp,
    //                     top = 4.dp,
    //                     end = 6.dp,
    //                     bottom = 8.dp
    //                 )
    //             ) { }
    //         }
    //     }

    //     rule.onNodeWithTag(ContainerTag)
    //         .assertLeftPositionInRootIsEqualTo(0.dp)
    //         .assertTopPositionInRootIsEqualTo(0.dp)
    //         .assertWidthIsEqualTo(8.dp)
    //         .assertHeightIsEqualTo(12.dp)
    // }

    // @Test
    // fun row_contentPaddingAndZeroSizedItem() {
    //     rule.setContent {
    //         Box(modifier = Modifier.testTag(ContainerTag)) {
    //             LazyRow(
    //                 contentPadding = PaddingValues(
    //                     start = 2.dp,
    //                     top = 4.dp,
    //                     end = 6.dp,
    //                     bottom = 8.dp
    //                 )
    //             ) {
    //                 items(0) {}
    //             }
    //         }
    //     }

    //     rule.onNodeWithTag(ContainerTag)
    //         .assertLeftPositionInRootIsEqualTo(0.dp)
    //         .assertTopPositionInRootIsEqualTo(0.dp)
    //         .assertWidthIsEqualTo(8.dp)
    //         .assertHeightIsEqualTo(12.dp)
    // }

    // @Test
    // fun row_contentPaddingAndReverseLayout() {
    //     val startPadding = itemSize * 2
    //     val endPadding = itemSize / 2
    //     val listSize = itemSize * 3
    //     lateinit var state: LazyGridState
    //     rule.setContentWithTestViewConfiguration {
    //         LazyRow(
    //             reverseLayout = true,
    //             state = rememberLazyGridState().also { state = it },
    //             modifier = Modifier.requiredSize(listSize),
    //             contentPadding = PaddingValues(start = startPadding, end = endPadding),
    //         ) {
    //             items(3) { index ->
    //                 Box(Modifier.requiredSize(itemSize).testTag("$index"))
    //             }
    //         }
    //     }

    //     rule.onNodeWithTag("0")
    //         .assertLeftPositionInRootIsEqualTo(listSize - endPadding - itemSize)
    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(listSize - endPadding - itemSize * 2)
    //     // Partially visible.
    //     rule.onNodeWithTag("2")
    //         .assertLeftPositionInRootIsEqualTo(-itemSize / 2)

    //     // Scroll to the top.
    //     state.scrollBy(itemSize * 2.5f)

    //     rule.onNodeWithTag("2").assertLeftPositionInRootIsEqualTo(startPadding)
    //     // Shouldn't be visible
    //     rule.onNodeWithTag("1").assertIsNotDisplayed()
    //     rule.onNodeWithTag("0").assertIsNotDisplayed()
    // }

    // @Test
    // fun row_overscrollWithContentPadding() {
    //     lateinit var state: LazyListState
    //     rule.setContent {
    //         state = rememberLazyListState()
    //         Box(modifier = Modifier.testTag(ContainerTag).size(itemSize + smallPaddingSize * 2)) {
    //             LazyRow(
    //                 state = state,
    //                 contentPadding = PaddingValues(
    //                     horizontal = smallPaddingSize
    //                 )
    //             ) {
    //                 items(2) {
    //                     Box(Modifier.testTag("$it").fillParentMaxSize())
    //                 }
    //             }
    //         }
    //     }

    //     rule.onNodeWithTag("0")
    //         .assertLeftPositionInRootIsEqualTo(smallPaddingSize)
    //         .assertWidthIsEqualTo(itemSize)

    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(smallPaddingSize + itemSize)
    //         .assertWidthIsEqualTo(itemSize)

    //     rule.runOnIdle {
    //         runBlocking {
    //             // itemSizePx is the maximum offset, plus if we overscroll the content padding
    //             // the layout mechanism will decide the item 0 is not needed until we start
    //             // filling the over scrolled gap.
    //             state.scrollBy(value = itemSizePx + smallPaddingSizePx * 1.5f)
    //         }
    //     }

    //     rule.onNodeWithTag("1")
    //         .assertLeftPositionInRootIsEqualTo(smallPaddingSize)
    //         .assertWidthIsEqualTo(itemSize)

    //     rule.onNodeWithTag("0")
    //         .assertLeftPositionInRootIsEqualTo(smallPaddingSize - itemSize)
    //         .assertWidthIsEqualTo(itemSize)
    // }

    private fun LazyGridState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    private fun LazyGridState.assertScrollPosition(index: Int, offset: Dp) = with(rule.density) {
        assertThat(this@assertScrollPosition.firstVisibleItemIndex).isEqualTo(index)
        assertThat(firstVisibleItemScrollOffset.toDp().value).isWithin(0.5f).of(offset.value)
    }

    private fun LazyGridState.assertLayoutInfoOffsetRange(from: Dp, to: Dp) = with(rule.density) {
        assertThat(layoutInfo.viewportStartOffset to layoutInfo.viewportEndOffset)
            .isEqualTo(from.roundToPx() to to.roundToPx())
    }

    private fun LazyGridState.assertVisibleItems(vararg expected: Pair<Int, Dp>) =
        with(rule.density) {
            assertThat(layoutInfo.visibleItemsInfo.map { it.index to it.offset.y })
                .isEqualTo(expected.map { it.first to it.second.roundToPx() })
        }

    fun LazyGridState.scrollTo(index: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            scrollToItem(index)
        }
    }
}
