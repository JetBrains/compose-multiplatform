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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListsContentPaddingTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    private val LazyListTag = "LazyList"
    private val ItemTag = "item"
    private val ContainerTag = "container"

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
    fun contentPaddingIsApplied() {
        lateinit var state: LazyListState
        val containerSize = itemSize * 2
        val largePaddingSize = itemSize
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(containerSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    mainAxis = largePaddingSize,
                    crossAxis = smallPaddingSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(
                        Modifier
                            .then(fillParentMaxCrossAxis())
                            .mainAxisSize(itemSize)
                            .testTag(ItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(ItemTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertStartPositionInRootIsEqualTo(largePaddingSize)
            .assertCrossAxisSizeIsEqualTo(containerSize - smallPaddingSize * 2)
            .assertMainAxisSizeIsEqualTo(itemSize)

        state.scrollBy(largePaddingSize)

        rule.onNodeWithTag(ItemTag)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun contentPaddingIsNotAffectingScrollPosition() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(itemSize * 2)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = itemSize)
            ) {
                items(listOf(1)) {
                    Spacer(
                        Modifier
                            .then(fillParentMaxCrossAxis())
                            .mainAxisSize(itemSize)
                            .testTag(ItemTag))
                }
            }
        }

        state.assertScrollPosition(0, 0.dp)

        state.scrollBy(itemSize)

        state.assertScrollPosition(0, itemSize)
    }

    @Test
    fun scrollForwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding)
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(padding)
        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize + padding)
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize * 2 + padding)

        state.scrollBy(padding)

        state.assertScrollPosition(1, padding - itemSize)

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize * 2)
        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize * 3)
    }

    @Test
    fun scrollBackwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(itemSize + padding * 2)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding)
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
            .assertStartPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize * 3.5f - padding)
        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize * 4.5f - padding)
    }

    @Test
    fun scrollForwardTillTheEnd() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding)
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        state.scrollBy(itemSize * 3)

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize * 3 - padding)

        // there are no space to scroll anymore, so it should change nothing
        state.scrollBy(10.dp)

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize * 3 - padding)
    }

    @Test
    fun scrollForwardTillTheEndAndABitBack() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding)
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
            .assertStartPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize * 3.5f - padding)
    }

    @Test
    fun contentPaddingAndWrapContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyColumnOrRow(
                    contentPadding = PaddingValues(
                        beforeContentCrossAxis = 2.dp,
                        beforeContent = 4.dp,
                        afterContentCrossAxis = 6.dp,
                        afterContent = 8.dp
                    )
                ) {
                    items(listOf(1)) {
                        Spacer(Modifier.requiredSize(itemSize).testTag(ItemTag))
                    }
                }
            }
        }

        rule.onNodeWithTag(ItemTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(2.dp)
            .assertStartPositionInRootIsEqualTo(4.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag(ContainerTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize + 2.dp + 6.dp)
            .assertMainAxisSizeIsEqualTo(itemSize + 4.dp + 8.dp)
    }

    @Test
    fun contentPaddingAndNoContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyColumnOrRow(
                    contentPadding = PaddingValues(
                        beforeContentCrossAxis = 2.dp,
                        beforeContent = 4.dp,
                        afterContentCrossAxis = 6.dp,
                        afterContent = 8.dp
                    )
                ) { }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(8.dp)
            .assertMainAxisSizeIsEqualTo(12.dp)
    }

    @Test
    fun contentPaddingAndZeroSizedItem() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyColumnOrRow(
                    contentPadding = PaddingValues(
                        beforeContentCrossAxis = 2.dp,
                        beforeContent = 4.dp,
                        afterContentCrossAxis = 6.dp,
                        afterContent = 8.dp
                    )
                ) {
                    items(0) { }
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(8.dp)
            .assertMainAxisSizeIsEqualTo(12.dp)
    }

    @Test
    fun contentPaddingAndReverseLayout() {
        val topPadding = itemSize * 2
        val bottomPadding = itemSize / 2
        val listSize = itemSize * 3
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(listSize),
                contentPadding = PaddingValues(
                    beforeContent = topPadding,
                    afterContent = bottomPadding
                ),
            ) {
                items(3) { index ->
                    Box(Modifier.requiredSize(itemSize).testTag("$index"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(listSize - bottomPadding - itemSize)
        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(listSize - bottomPadding - itemSize * 2)
        // Partially visible.
        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(-itemSize / 2)

        // Scroll to the top.
        state.scrollBy(itemSize * 2.5f)

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(topPadding)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun contentLargePaddingAndReverseLayout() {
        val topPadding = itemSize * 2
        val bottomPadding = itemSize * 2
        val listSize = itemSize * 3
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(listSize),
                contentPadding = PaddingValues(
                    beforeContent = topPadding,
                    afterContent = bottomPadding
                ),
            ) {
                items(3) { index ->
                    Box(Modifier.requiredSize(itemSize).testTag("$index"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(0.dp)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()

        // Scroll to the top.
        state.scrollBy(itemSize * 5f)

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(topPadding)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun overscrollWithContentPadding() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize + smallPaddingSize * 2)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = smallPaddingSize)
                ) {
                    items(2) {
                        Box(Modifier.testTag("$it").fillParentMaxSize())
                    }
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.runOnIdle {
            runBlocking {
                // itemSizePx is the maximum offset, plus if we overscroll the content padding
                // the layout mechanism will decide the item 0 is not needed until we start
                // filling the over scrolled gap.
                state.scrollBy(value = itemSizePx + smallPaddingSizePx * 1.5f)
            }
        }

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize - itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun totalPaddingLargerParentSize_initialState() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(itemSize)

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
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(1, 0.dp)
            state.assertVisibleItems(0 to -itemSize, 1 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollToLastItem() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize)
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
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollToLastItemByDelta() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize)
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
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollTillTheEnd() {
        // the whole end content padding is displayed
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize)
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
            .assertStartPositionInRootIsEqualTo(-itemSize * 0.5f)

        rule.runOnIdle {
            state.assertScrollPosition(3, itemSize * 1.5f)
            state.assertVisibleItems(3 to -itemSize * 1.5f)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_initialState() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize * 2)
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
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize * 2)
                ) {
                    items(4) {
                        Box(Modifier.testTag("$it").size(itemSize))
                    }
                }
            }
        }

        state.scrollBy(itemSize * 2)

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(2, 0.dp)
            state.assertVisibleItems(0 to -itemSize * 2, 1 to -itemSize, 2 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollToLastItem() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize * 2)
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
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(1 to -itemSize * 2, 2 to -itemSize, 3 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollToLastItemByDelta() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize * 2)
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
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize)

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
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            Box(modifier = Modifier.testTag(ContainerTag).size(itemSize * 1.5f)) {
                LazyColumnOrRow(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = itemSize * 2)
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

    private fun LazyListState.assertScrollPosition(index: Int, offset: Dp) = with(rule.density) {
        assertThat(firstVisibleItemIndex).isEqualTo(index)
        assertThat(firstVisibleItemScrollOffset.toDp().value).isWithin(0.5f).of(offset.value)
    }

    private fun LazyListState.assertLayoutInfoOffsetRange(from: Dp, to: Dp) = with(rule.density) {
        assertThat(layoutInfo.viewportStartOffset to layoutInfo.viewportEndOffset)
            .isEqualTo(from.roundToPx() to to.roundToPx())
    }

    private fun LazyListState.assertVisibleItems(vararg expected: Pair<Int, Dp>) =
        with(rule.density) {
            assertThat(layoutInfo.visibleItemsInfo.map { it.index to it.offset })
                .isEqualTo(expected.map { it.first to it.second.roundToPx() })
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
