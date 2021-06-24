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

import android.os.Build
import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyRowTest {
    private val LazyListTag = "LazyListTag"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyRowShowsCombinedItems() {
        val itemTestTag = "itemTestTag"
        val items = listOf(1, 2).map { it.toString() }
        val indexedItems = listOf(3, 4, 5)

        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.width(200.dp)) {
                item {
                    Spacer(
                        Modifier.width(40.dp).fillParentMaxHeight().testTag(itemTestTag)
                    )
                }
                items(items) {
                    Spacer(Modifier.width(40.dp).fillParentMaxHeight().testTag(it))
                }
                itemsIndexed(indexedItems) { index, item ->
                    Spacer(
                        Modifier.width(41.dp).fillParentMaxHeight()
                            .testTag("$index-$item")
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("0-3")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-4")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowAllowEmptyListItems() {
        val itemTag = "itemTag"

        rule.setContentWithTestViewConfiguration {
            LazyRow {
                items(emptyList<Any>()) { }
                item {
                    Spacer(Modifier.size(10.dp).testTag(itemTag))
                }
            }
        }

        rule.onNodeWithTag(itemTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowAllowsNullableItems() {
        val items = listOf("1", null, "3")
        val nullTestTag = "nullTestTag"

        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.width(200.dp)) {
                items(items) {
                    if (it != null) {
                        Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                    } else {
                        Spacer(
                            Modifier.width(101.dp).fillParentMaxHeight()
                                .testTag(nullTestTag)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag(nullTestTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowOnlyVisibleItemsAdded() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.width(200.dp)) {
                LazyRow {
                    items(items) {
                        Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowScrollToShowItems123() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.width(200.dp)) {
                LazyRow(Modifier.testTag(LazyListTag)) {
                    items(items) {
                        Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 50.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertIsNotDisplayed()
    }

    @Test
    fun lazyRowScrollToHideFirstItem() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.width(200.dp)) {
                LazyRow(Modifier.testTag(LazyListTag)) {
                    items(items) {
                        Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 105.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowScrollToShowItems234() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.width(200.dp)) {
                LazyRow(Modifier.testTag(LazyListTag)) {
                    items(items) {
                        Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 150.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowWrapsContent() = with(rule.density) {
        val itemInsideLazyRow = "itemInsideLazyRow"
        val itemOutsideLazyRow = "itemOutsideLazyRow"
        var sameSizeItems by mutableStateOf(true)

        rule.setContentWithTestViewConfiguration {
            Column {
                LazyRow(Modifier.testTag(LazyListTag)) {
                    items(listOf(1, 2)) {
                        if (it == 1) {
                            Spacer(Modifier.size(50.dp).testTag(itemInsideLazyRow))
                        } else {
                            Spacer(Modifier.size(if (sameSizeItems) 50.dp else 70.dp))
                        }
                    }
                }
                Spacer(Modifier.size(50.dp).testTag(itemOutsideLazyRow))
            }
        }

        rule.onNodeWithTag(itemInsideLazyRow)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyRow)
            .assertIsDisplayed()

        var lazyRowBounds = rule.onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        assertThat(lazyRowBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyRowBounds.right.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        assertThat(lazyRowBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyRowBounds.bottom.roundToPx()).isWithin1PixelFrom(50.dp.roundToPx())

        rule.runOnIdle {
            sameSizeItems = false
        }

        rule.waitForIdle()

        rule.onNodeWithTag(itemInsideLazyRow)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyRow)
            .assertIsDisplayed()

        lazyRowBounds = rule.onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        assertThat(lazyRowBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyRowBounds.right.roundToPx()).isWithin1PixelFrom(120.dp.roundToPx())
        assertThat(lazyRowBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyRowBounds.bottom.roundToPx()).isWithin1PixelFrom(70.dp.roundToPx())
    }

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun prepareLazyRowForAlignment(verticalGravity: Alignment.Vertical) {
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                Modifier.testTag(LazyListTag).requiredHeight(100.dp),
                verticalAlignment = verticalGravity
            ) {
                items(listOf(1, 2)) {
                    if (it == 1) {
                        Spacer(Modifier.size(50.dp).testTag(firstItemTag))
                    } else {
                        Spacer(Modifier.size(70.dp).testTag(secondItemTag))
                    }
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        rule.onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyRowBounds = rule.onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        with(rule.density) {
            // Verify the height of the row
            assertThat(lazyRowBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
            assertThat(lazyRowBounds.bottom.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        }
    }

    @Test
    fun lazyRowAlignmentCenterVertically() {
        prepareLazyRowForAlignment(Alignment.CenterVertically)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 25.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 15.dp)
    }

    @Test
    fun lazyRowAlignmentTop() {
        prepareLazyRowForAlignment(Alignment.Top)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)
    }

    @Test
    fun lazyRowAlignmentBottom() {
        prepareLazyRowForAlignment(Alignment.Bottom)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 30.dp)
    }

    @Test
    fun itemFillingParentWidth() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth().requiredHeight(50.dp).testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeight() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp).fillParentMaxHeight().testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSize() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentWidthFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth(0.7f)
                            .requiredHeight(50.dp)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeightFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp)
                            .fillParentMaxHeight(0.3f)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(45.dp)
    }

    @Test
    fun itemFillingParentSizeFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize(0.5f).testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(75.dp)
    }

    @Test
    fun itemFillingParentSizeParentResized() {
        var parentSize by mutableStateOf(100.dp)
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(parentSize)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
                }
            }
        }

        rule.runOnIdle {
            parentSize = 150.dp
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun scrollsLeftInRtl() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(100.dp)) {
                    state = rememberLazyListState()
                    LazyRow(Modifier.testTag(LazyListTag), state) {
                        items(4) {
                            Spacer(
                                Modifier.width(101.dp).fillParentMaxHeight().testTag("$it")
                            )
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = (-150).dp, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
        }
    }

    @Test
    fun whenNotAnymoreAvailableItemWasDisplayed() {
        var items by mutableStateOf((1..30).toList())
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 16-20
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 300.dp, density = rule.density)

        rule.runOnIdle {
            items = (1..10).toList()
        }

        // there is no item 16 anymore so we will just display the last items 6-10
        rule.onNodeWithTag("6")
            .assertLeftPositionIsAlmost(0.dp)
    }

    @Test
    fun whenFewDisplayedItemsWereRemoved() {
        var items by mutableStateOf((1..10).toList())
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 100.dp, density = rule.density)

        rule.runOnIdle {
            items = (1..8).toList()
        }

        // there are no more items 9 and 10, so we have to scroll back
        rule.onNodeWithTag("4")
            .assertLeftPositionIsAlmost(0.dp)
    }

    @Test
    fun whenItemsBecameEmpty() {
        var items by mutableStateOf((1..10).toList())
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSizeIn(maxHeight = 100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 2-6
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 20.dp, density = rule.density)

        rule.runOnIdle {
            items = emptyList()
        }

        // there are no more items so the LazyRow is zero sized
        rule.onNodeWithTag(LazyListTag)
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)

        // and has no children
        rule.onNodeWithTag("1")
            .assertDoesNotExist()
        rule.onNodeWithTag("2")
            .assertDoesNotExist()
    }

    @Test
    fun scrollBackAndForth() {
        val items by mutableStateOf((1..20).toList())
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 100.dp, density = rule.density)

        // and scroll back
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = (-100).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertLeftPositionIsAlmost(0.dp)
    }

    @Test
    fun tryToScrollBackwardWhenAlreadyOnTop() {
        val items by mutableStateOf((1..20).toList())
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // we already displaying the first item, so this should do nothing
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = (-50).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertLeftPositionIsAlmost(0.dp)
        rule.onNodeWithTag("5")
            .assertLeftPositionIsAlmost(80.dp)
    }

    private fun SemanticsNodeInteraction.assertLeftPositionIsAlmost(expected: Dp) {
        getUnclippedBoundsInRoot().left.assertIsEqualTo(expected, tolerance = 1.dp)
    }

    @Test
    fun contentOfNotStableItemsIsNotRecomposedDuringScroll() {
        val items = listOf(NotStable(1), NotStable(2))
        var firstItemRecomposed = 0
        var secondItemRecomposed = 0
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    if (it.count == 1) {
                        firstItemRecomposed++
                    } else {
                        secondItemRecomposed++
                    }
                    Spacer(Modifier.requiredSize(75.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = (50).dp, density = rule.density)

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }
    }

    @Test
    fun onlyOneMeasurePassForScrollEvent() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            state.prefetchingEnabled = false
            LazyRow(Modifier.requiredSize(100.dp), state = state) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        val initialMeasurePasses = state.numMeasurePasses

        rule.runOnIdle {
            with(rule.density) {
                state.onScroll(-110.dp.toPx())
            }
        }

        rule.waitForIdle()

        assertThat(state.numMeasurePasses).isEqualTo(initialMeasurePasses + 1)
    }

    @Test
    fun stateUpdatedAfterScroll() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 30.dp, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)

            with(rule.density) {
                // TODO(b/169232491): test scrolling doesn't appear to be scrolling exactly the right
                //  number of pixels
                val expectedOffset = 10.dp.roundToPx()
                val tolerance = 2.dp.roundToPx()
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset, tolerance)
            }
        }
    }

    @Test
    fun stateUpdatedAfterScrollWithinTheSameItem() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 10.dp, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) {
                val expectedOffset = 10.dp.roundToPx()
                val tolerance = 2.dp.roundToPx()
                assertThat(state.firstVisibleItemScrollOffset)
                    .isEqualTo(expectedOffset, tolerance)
            }
        }
    }

    @Test
    fun initialScrollIsApplied() {
        lateinit var state: LazyListState
        val expectedOffset = with(rule.density) { 10.dp.roundToPx() }
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState(2, expectedOffset)
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag), state = state) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
        }

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun stateIsRestored() {
        val restorationTester = StateRestorationTester(rule)
        var state: LazyListState? = null
        restorationTester.setContent {
            state = rememberLazyListState()
            LazyRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state!!
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 30.dp, density = rule.density)

        val (index, scrollOffset) = rule.runOnIdle {
            state!!.firstVisibleItemIndex to state!!.firstVisibleItemScrollOffset
        }

        state = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.firstVisibleItemIndex).isEqualTo(index)
            assertThat(state!!.firstVisibleItemScrollOffset).isEqualTo(scrollOffset)
        }
    }

    @Test
    fun snapToItemIndex() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3, 10)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(3)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun itemsAreNotRedrawnDuringScroll() {
        val redrawCount = Array(6) { 0 }
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(21) {
                    Spacer(
                        Modifier.requiredSize(20.dp)
                            .drawBehind { redrawCount[it]++ }
                    )
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(x = 10.dp, density = rule.density)

        rule.runOnIdle {
            redrawCount.forEachIndexed { index, i ->
                Truth.assertWithMessage("Item with index $index was redrawn $i times")
                    .that(i).isEqualTo(1)
            }
        }
    }

    @Test
    fun itemInvalidationIsNotCausingAnotherItemToRedraw() {
        val redrawCount = Array(2) { 0 }
        var stateUsedInDrawScope by mutableStateOf(false)
        rule.setContentWithTestViewConfiguration {
            LazyRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(2) {
                    Spacer(
                        Modifier.requiredSize(50.dp)
                            .drawBehind {
                                redrawCount[it]++
                                if (it == 1) {
                                    stateUsedInDrawScope.hashCode()
                                }
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            stateUsedInDrawScope = true
        }

        rule.runOnIdle {
            Truth.assertWithMessage("First items is not expected to be redrawn")
                .that(redrawCount[0]).isEqualTo(1)
            Truth.assertWithMessage("Second items is expected to be redrawn")
                .that(redrawCount[1]).isEqualTo(2)
        }
    }

    @Test
    fun notVisibleAnymoreItemNotAffectingCrossAxisSize() {
        val items = (0..1).toList()
        val itemSize = with(rule.density) { 30.toDp() }
        val itemSizeMinusOne = with(rule.density) { 29.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                Modifier.requiredWidth(itemSizeMinusOne).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(items) {
                    Spacer(
                        if (it == 0) {
                            Modifier.requiredHeight(30.dp).requiredWidth(itemSizeMinusOne)
                        } else {
                            Modifier.requiredHeight(20.dp).requiredWidth(itemSize)
                        }
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag(LazyListTag)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun itemStillVisibleAfterOverscrollIsAffectingCrossAxisSize() {
        val items = (0..2).toList()
        val itemSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                Modifier.requiredWidth(itemSize * 1.75f).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(items) {
                    Spacer(
                        if (it == 0) {
                            Modifier.requiredHeight(30.dp).requiredWidth(itemSize / 2)
                        } else if (it == 1) {
                            Modifier.requiredHeight(20.dp).requiredWidth(itemSize / 2)
                        } else {
                            Modifier.requiredHeight(20.dp).requiredWidth(itemSize)
                        }
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag(LazyListTag)
            .assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            LazyRow {
                items(items) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            LazyRow {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.requiredSize(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: LazyListState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyListState()
            LazyRow(Modifier.fillMaxHeight().width(10.dp), state) {
                items(count) { index ->
                    composedIndexes.add(index)
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            composedIndexes.clear()
            count = 10
            runBlocking(AutoTestFrameClock()) {
                state.scrollToItem(50)
            }
            composedIndexes.forEach {
                assertThat(it).isLessThan(count)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(9)
        }
    }

    @Test
    fun overscrollingBackwardFromNotTheFirstPosition() {
        val containerTag = "container"
        val itemSizePx = 10
        val itemSizeDp = with(rule.density) { itemSizePx.toDp() }
        val containerSize = itemSizeDp * 5
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier
                    .testTag(containerTag)
                    .size(containerSize)
            ) {
                LazyRow(
                    Modifier
                        .testTag(LazyListTag)
                        .background(Color.Blue),
                    state = rememberLazyListState(2, 5)
                ) {
                    items(100) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(itemSizeDp)
                                .testTag("$it")
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .performGesture {
                // we do move manually and not with swipe() utility because we want to have one
                // drag gesture, not multiple smaller ones
                down(center)
                moveBy(Offset(-TestTouchSlop, 0f))
                moveBy(
                    Offset(
                        itemSizePx * 15f, // large value which makes us overscroll
                        0f
                    )
                )
                up()
            }

        rule.onNodeWithTag(LazyListTag)
            .assertWidthIsEqualTo(containerSize)

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("4")
            .assertLeftPositionInRootIsEqualTo(containerSize - itemSizeDp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun lazyRowDoesNotClipHorizontalOverdraw() {
        rule.setContent {
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                LazyRow(
                    Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    rememberLazyListState(1)
                ) {
                    items(4) {
                        Box(Modifier.size(20.dp).drawOutsideOfBounds())
                    }
                }
            }
        }

        rule.onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 20.dp,
                verticalPadding = 0.dp
            )
    }

    @Test
    fun initialScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        lateinit var state: LazyListState
        var itemsCount by mutableStateOf(0)
        rule.setContent {
            state = rememberLazyListState(2, 10)
            LazyRow(Modifier.fillMaxSize(), state) {
                items(itemsCount) {
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            itemsCount = 100
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun restoredScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        lateinit var state: LazyListState
        var itemsCount = 100
        val recomposeCounter = mutableStateOf(0)
        val tester = StateRestorationTester(rule)
        tester.setContent {
            state = rememberLazyListState()
            LazyRow(Modifier.fillMaxSize(), state) {
                recomposeCounter.value
                items(itemsCount) {
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2, 10)
            }
            itemsCount = 0
        }

        tester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            itemsCount = 100
            recomposeCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun animateScrollToItemDoesNotScrollPastItem() {
        lateinit var state: LazyListState
        var target = 0
        var reverse = false
        rule.setContent {
            val listState = rememberLazyListState()
            SideEffect {
                state = listState
            }
            LazyRow(Modifier.fillMaxSize(), listState) {
                items(2500) { _ ->
                    Box(Modifier.size(100.dp))
                }
            }

            if (reverse) {
                assertThat(listState.firstVisibleItemIndex).isAtLeast(target)
            } else {
                assertThat(listState.firstVisibleItemIndex).isAtMost(target)
            }
        }

        // Try a bunch of different targets with varying spacing
        listOf(500, 800, 1500, 1600, 1800).forEach {
            target = it
            rule.runOnIdle {
                runBlocking(AutoTestFrameClock()) {
                    state.animateScrollToItem(target)
                }
            }

            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(target)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }

        reverse = true

        listOf(1600, 1500, 800, 500, 0).forEach {
            target = it
            rule.runOnIdle {
                runBlocking(AutoTestFrameClock()) {
                    state.animateScrollToItem(target)
                }
            }

            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(target)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }
    }

    @Test
    fun animateScrollToTheLastItemWhenItemsAreLargerThenTheScreen() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyRow(Modifier.height(150.dp).width(100.dp), state) {
                items(20) {
                    Box(Modifier.size(150.dp))
                }
            }
        }

        // Try a bunch of different start indexes
        listOf(0, 5, 12).forEach {
            val startIndex = it
            rule.runOnIdle {
                runBlocking(AutoTestFrameClock()) {
                    state.scrollToItem(startIndex)
                    state.animateScrollToItem(19)
                }
            }

            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(19)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }
    }

    @Test
    fun recreatingContentLambdaTriggersItemRecomposition() {
        val countState = mutableStateOf(0)
        rule.setContent {
            val count = countState.value
            LazyRow {
                item {
                    BasicText(text = "Count $count")
                }
            }
        }

        rule.onNodeWithText("Count 0")
            .assertIsDisplayed()

        rule.runOnIdle {
            countState.value++
        }

        rule.onNodeWithText("Count 1")
            .assertIsDisplayed()
    }

    @Test
    fun semanticsScroll_isAnimated() {
        rule.mainClock.autoAdvance = false
        val state = LazyListState()

        rule.setContent {
            LazyRow(Modifier.testTag(LazyListTag), state = state) {
                items(50) {
                    Box(Modifier.width(200.dp))
                }
            }
        }

        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        rule.onNodeWithTag(LazyListTag).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(100f, 0f)
        }

        // We haven't advanced time yet, make sure it's still zero
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        // Advance and make sure we're partway through
        // Note that we need two frames for the animation to actually happen
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // The items are 200dp each, so still the first one, but offset
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
        assertThat(state.firstVisibleItemScrollOffset).isLessThan(100)

        // Finish the scroll, make sure we're at the target
        rule.mainClock.advanceTimeBy(5000)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(100)
    }

    private fun LazyListState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }
}
