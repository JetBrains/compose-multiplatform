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

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
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
class LazyListsContentPaddingTest {
    private val LazyListTag = "LazyList"
    private val ItemTag = "item"
    private val ContainerTag = "container"

    @get:Rule
    val rule = createComposeRule()

    private var itemSize: Dp = Dp.Infinity
    private var smallPaddingSize: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSize = 50.toDp()
            smallPaddingSize = 12.toDp()
        }
    }

    @Test
    fun column_contentPaddingIsApplied() {
        lateinit var state: LazyListState
        val containerSize = itemSize * 2
        val largePaddingSize = itemSize
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(containerSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    start = smallPaddingSize,
                    top = largePaddingSize,
                    end = smallPaddingSize,
                    bottom = largePaddingSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(Modifier.fillParentMaxWidth().height(itemSize).testTag(ItemTag))
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
    fun column_contentPaddingIsNotAffectingScrollPosition() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(itemSize * 2)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    top = itemSize,
                    bottom = itemSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(Modifier.fillParentMaxWidth().height(itemSize).testTag(ItemTag))
                }
            }
        }

        state.assertScrollPosition(0, 0.dp)

        state.scrollBy(itemSize)

        state.assertScrollPosition(0, itemSize)
    }

    @Test
    fun column_scrollForwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
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
    fun column_scrollBackwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(itemSize + padding * 2)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
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
    fun column_scrollForwardTillTheEnd() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
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
    fun column_scrollForwardTillTheEndAndABitBack() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
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
    fun column_contentPaddingAndWrapContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 4.dp,
                        end = 6.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(listOf(1)) {
                        Spacer(Modifier.requiredSize(itemSize).testTag(ItemTag))
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
    fun column_contentPaddingAndNoContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyColumn(
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
    fun column_contentPaddingAndZeroSizedItem() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyColumn(
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
    fun row_contentPaddingIsApplied() {
        lateinit var state: LazyListState
        val containerSize = itemSize * 2
        val largePaddingSize = itemSize
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(containerSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    top = smallPaddingSize,
                    start = largePaddingSize,
                    bottom = smallPaddingSize,
                    end = largePaddingSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(Modifier.fillParentMaxHeight().width(itemSize).testTag(ItemTag))
                }
            }
        }

        rule.onNodeWithTag(ItemTag)
            .assertTopPositionInRootIsEqualTo(smallPaddingSize)
            .assertLeftPositionInRootIsEqualTo(largePaddingSize)
            .assertHeightIsEqualTo(containerSize - smallPaddingSize * 2)
            .assertWidthIsEqualTo(itemSize)

        state.scrollBy(largePaddingSize)

        rule.onNodeWithTag(ItemTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(itemSize)
    }

    @Test
    fun row_contentPaddingIsNotAffectingScrollPosition() {
        lateinit var state: LazyListState
        val itemSize = with(rule.density) {
            50.dp.roundToPx().toDp()
        }
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(itemSize * 2)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    start = itemSize,
                    end = itemSize
                )
            ) {
                items(listOf(1)) {
                    Spacer(Modifier.fillParentMaxHeight().width(itemSize).testTag(ItemTag))
                }
            }
        }

        state.assertScrollPosition(0, 0.dp)

        state.scrollBy(itemSize)

        state.assertScrollPosition(0, itemSize)
    }

    @Test
    fun row_scrollForwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    start = padding,
                    end = padding
                )
            ) {
                items((0..3).toList()) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(padding)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize + padding)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 + padding)

        state.scrollBy(padding)

        state.assertScrollPosition(1, padding - itemSize)

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2)
        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 3)
    }

    @Test
    fun row_scrollBackwardItemWithinStartPaddingDisplayed() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(itemSize + padding * 2)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    start = padding,
                    end = padding
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
            .assertLeftPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 3.5f - padding)
        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 4.5f - padding)
    }

    @Test
    fun row_scrollForwardTillTheEnd() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    start = padding,
                    end = padding
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
            .assertLeftPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 3 - padding)

        // there are no space to scroll anymore, so it should change nothing
        state.scrollBy(10.dp)

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 3 - padding)
    }

    @Test
    fun row_scrollForwardTillTheEndAndABitBack() {
        lateinit var state: LazyListState
        val padding = itemSize * 1.5f
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(padding * 2 + itemSize)
                    .testTag(LazyListTag),
                state = rememberLazyListState().also { state = it },
                contentPadding = PaddingValues(
                    start = padding,
                    end = padding
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
            .assertLeftPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 3.5f - padding)
    }

    @Test
    fun row_contentPaddingAndWrapContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 4.dp,
                        end = 6.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(listOf(1)) {
                        Spacer(Modifier.requiredSize(itemSize).testTag(ItemTag))
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
    fun row_contentPaddingAndNoContent() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyRow(
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
    fun row_contentPaddingAndZeroSizedItem() {
        rule.setContent {
            Box(modifier = Modifier.testTag(ContainerTag)) {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 4.dp,
                        end = 6.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(0) {}
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(8.dp)
            .assertHeightIsEqualTo(12.dp)
    }

    private fun LazyListState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    private fun LazyListState.assertScrollPosition(index: Int, offset: Dp) = with(rule.density) {
        assertThat(firstVisibleItemIndex).isEqualTo(index)
        assertThat(firstVisibleItemScrollOffset.toDp().value).isWithin(0.5f).of(offset.value)
    }
}
