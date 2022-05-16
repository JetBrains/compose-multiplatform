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

import android.os.Build
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.list.TestTouchSlop
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyGridTest(
    private val orientation: Orientation
) : BaseLazyGridTestWithOrientation(orientation) {
    private val LazyGridTag = "LazyGridTag"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    @Test
    fun lazyGridShowsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyGrid(
                cells = 3
            ) {
                item {
                    Spacer(
                        Modifier.size(10.dp).testTag(itemTestTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyGridShowsOneLine() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                cells = 3,
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyGridShowsSecondLineOnScroll() {
        val items = (1..9).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 3,
                modifier = Modifier.mainAxisSize(100.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(offset = 50.dp)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("8")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("9")
            .assertIsNotDisplayed()
    }

    @Test
    fun lazyGridScrollHidesFirstLine() {
        val items = (1..9).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 3,
                modifier = Modifier.mainAxisSize(200.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(offset = 103.dp)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertIsDisplayed()

        rule.onNodeWithTag("8")
            .assertIsDisplayed()

        rule.onNodeWithTag("9")
            .assertIsDisplayed()
    }

    @Test
    fun adaptiveLazyGridFillsAllCrossAxisSize() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                cells = GridCells.Adaptive(130.dp),
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertCrossAxisStartPositionInRootIsEqualTo(150.dp)

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun adaptiveLazyGridAtLeastOneSlot() {
        val items = (1..3).map { it.toString() }

        rule.setContent {
            LazyGrid(
                cells = GridCells.Adaptive(301.dp),
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun adaptiveLazyGridAppliesHorizontalSpacings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 10.toDp() }
        val itemSize = with(rule.density) { 100.toDp() }

        rule.setContent {
            LazyGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize * 3 + spacing * 2, itemSize),
                crossAxisSpacedBy = spacing
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize + spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize * 2 + spacing * 2)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesHorizontalSpacingsWithContentPaddings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 8.toDp() }
        val itemSize = with(rule.density) { 40.toDp() }

        rule.setContent {
            LazyGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize * 3 + spacing * 4, itemSize),
                crossAxisSpacedBy = spacing,
                contentPadding = PaddingValues(crossAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize + spacing * 2)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize * 2 + spacing * 3)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesVerticalSpacings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 4.toDp() }
        val itemSize = with(rule.density) { 32.toDp() }

        rule.setContent {
            LazyGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize, itemSize * 3 + spacing * 2),
                mainAxisSpacedBy = spacing
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize + spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 2 + spacing * 2)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesVerticalSpacingsWithContentPadding() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 16.toDp() }
        val itemSize = with(rule.density) { 72.toDp() }

        rule.setContent {
            LazyGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize, itemSize * 3 + spacing * 2),
                mainAxisSpacedBy = spacing,
                contentPadding = PaddingValues(mainAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 3 + itemSize * 2)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesVerticalSpacings() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 24.toDp() }
        val itemSize = with(rule.density) { 80.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize, itemSize * 2 + spacing),
                mainAxisSpacedBy = spacing,
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesHorizontalSpacings() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 15.toDp() }
        val itemSize = with(rule.density) { 30.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize * 2 + spacing, itemSize * 2),
                crossAxisSpacedBy = spacing
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesVerticalSpacingsWithContentPadding() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 30.toDp() }
        val itemSize = with(rule.density) { 77.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize, itemSize * 2 + spacing),
                mainAxisSpacedBy = spacing,
                contentPadding = PaddingValues(mainAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesHorizontalSpacingsWithContentPadding() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 22.toDp() }
        val itemSize = with(rule.density) { 44.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize * 2 + spacing * 3, itemSize * 2),
                crossAxisSpacedBy = spacing,
                contentPadding = PaddingValues(crossAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.crossAxisSize(itemSize * 2)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("4")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                Modifier.crossAxisSize(itemSize * 2)
            ) {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.mainAxisSize(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3*4")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: LazyGridState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyGridState()
            LazyGrid(
                cells = 1,
                modifier = Modifier.mainAxisSize(10.dp),
                state = state
            ) {
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
                // we try to scroll to the index after 10, but we expect that the component will
                // already be aware there is a new count and not compose items with index > 10
                state.scrollToItem(50)
            }
            composedIndexes.forEach {
                Truth.assertThat(it).isLessThan(count)
            }
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(9)
        }
    }

    @Test
    fun maxIntElements() {
        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.size(itemSize * 2).testTag(LazyGridTag),
                state = LazyGridState(firstVisibleItemIndex = Int.MAX_VALUE - 3)
            ) {
                items(Int.MAX_VALUE) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("${Int.MAX_VALUE - 3}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE - 2}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("${Int.MAX_VALUE - 1}")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE}").assertDoesNotExist()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun pointerInputScrollingIsAllowedWhenUserScrollingIsEnabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = true,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).apply {
            scrollBy(offset = itemSize)
        }

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun pointerInputScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(offset = itemSize)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun programmaticScrollingIsAllowedWhenUserScrollingIsDisabled() {
        val itemSizePx = 30f
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3),
                state = rememberLazyGridState().also { state = it },
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx)
            }
        }

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun semanticScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollBy))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollToIndex))
            // but we still have a read only scroll range property
            .assert(
                keyIsDefined(
                    if (orientation == Orientation.Vertical) {
                        SemanticsProperties.VerticalScrollAxisRange
                    } else {
                        SemanticsProperties.HorizontalScrollAxisRange
                    }
                )
            )
    }

    @Test
    fun rtl() {
        val gridCrossAxisSize = 30
        val gridCrossAxisSizeDp = with(rule.density) { gridCrossAxisSize.toDp() }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyGrid(
                    cells = 3,
                    modifier = Modifier.crossAxisSize(gridCrossAxisSizeDp)
                ) {
                    items(3) {
                        Box(Modifier.mainAxisSize(1.dp).testTag("$it"))
                    }
                }
            }
        }

        val tags = if (orientation == Orientation.Vertical) {
            listOf("0", "1", "2")
        } else {
            listOf("2", "1", "0")
        }
        rule.onNodeWithTag(tags[0])
            .assertCrossAxisStartPositionInRootIsEqualTo(gridCrossAxisSizeDp * 2 / 3)
        rule.onNodeWithTag(tags[1])
            .assertCrossAxisStartPositionInRootIsEqualTo(gridCrossAxisSizeDp / 3)
        rule.onNodeWithTag(tags[2]).assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun withMissingItems() {
        val itemMainAxisSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyGrid(
                cells = 2,
                modifier = Modifier.mainAxisSize(itemMainAxisSize + 1.dp),
                state = state
            ) {
                items((0..8).map { it.toString() }) {
                    if (it != "3") {
                        Box(Modifier.mainAxisSize(itemMainAxisSize).testTag(it))
                    }
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3)
            }
        }

        rule.onNodeWithTag("0").assertIsNotDisplayed()
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertDoesNotExist()
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    @Test
    fun withZeroSizedFirstItem() {
        var scrollConsumedAccumulator = Offset.Zero
        val collectingDataConnection = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                scrollConsumedAccumulator += consumed
                return Offset.Zero
            }
        }

        rule.setContent {
            val state = rememberLazyGridState()
            LazyGrid(
                cells = 1,
                state = state,
                modifier = Modifier
                    .testTag("mainList")
                    .nestedScroll(connection = collectingDataConnection),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(all = 10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.size(size = 0.dp))
                }
                items((0..8).map { it.toString() }) {
                    Box(Modifier.testTag(it)) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        rule.onNodeWithTag("mainList").performTouchInput {
            swipeDown()
        }

        rule.runOnIdle {
            assertThat(scrollConsumedAccumulator).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun withZeroSizedFirstItem_shouldKeepItemOnSizeChange() {
        val firstItemSize = mutableStateOf(0.dp)

        rule.setContent {
            val state = rememberLazyGridState()
            LazyGrid(
                cells = 1,
                state = state,
                modifier = Modifier
                    .testTag("mainList"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(all = 10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier
                        .testTag("firstItem")
                        .size(size = firstItemSize.value)
                        .background(Color.Black))
                }
                items((0..8).map { it.toString() }) {
                    Box(Modifier.testTag(it)) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        rule.onNodeWithTag("firstItem").assertIsNotDisplayed()
        firstItemSize.value = 20.dp
        rule.onNodeWithTag("firstItem").assertIsDisplayed()
    }

    @Test
    fun passingNegativeItemsCountIsNotAllowed() {
        var exception: Exception? = null
        rule.setContentWithTestViewConfiguration {
            LazyGrid(cells = 1) {
                try {
                    items(-1) {
                        Box(Modifier)
                    }
                } catch (e: Exception) {
                    exception = e
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun recomposingWithNewComposedModifierObjectIsNotCausingRemeasure() {
        var remeasureCount = 0
        val layoutModifier = Modifier.layout { measurable, constraints ->
            remeasureCount++
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
        val counter = mutableStateOf(0)

        rule.setContentWithTestViewConfiguration {
            counter.value // just to trigger recomposition
            LazyGrid(
                cells = 1,
                // this will return a new object everytime causing LazyGrid recomposition
                // without causing remeasure
                modifier = Modifier.composed { layoutModifier }
            ) {
                items(1) {
                    Spacer(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(remeasureCount).isEqualTo(1)
            counter.value++
        }

        rule.runOnIdle {
            Truth.assertThat(remeasureCount).isEqualTo(1)
        }
    }

    @Test
    fun scrollingALotDoesntCauseLazyLayoutRecomposition() {
        var recomposeCount = 0
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyGrid(
                cells = 1,
                modifier = Modifier.composed {
                    recomposeCount++
                    Modifier
                }.size(100.dp),
                state
            ) {
                items(1000) {
                    Spacer(Modifier.size(100.dp))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(recomposeCount).isEqualTo(1)

            runBlocking {
                state.scrollToItem(100)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(recomposeCount).isEqualTo(1)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun zIndexOnItemAffectsDrawingOrder() {
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(6.dp).testTag(LazyGridTag)
            ) {
                items(listOf(Color.Blue, Color.Green, Color.Red)) { color ->
                    Box(
                        Modifier
                            .axisSize(6.dp, 2.dp)
                            .zIndex(if (color == Color.Green) 1f else 0f)
                            .drawBehind {
                                drawRect(
                                    color,
                                    topLeft = Offset(-10.dp.toPx(), -10.dp.toPx()),
                                    size = Size(20.dp.toPx(), 20.dp.toPx())
                                )
                            })
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .captureToImage()
            .assertPixels { Color.Green }
    }

    @Test
    fun customGridCells() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                // Two columns in ratio 1:2
                cells = object : GridCells {
                    override fun Density.calculateCrossAxisCellSizes(
                        availableSize: Int,
                        spacing: Int
                    ): List<Int> {
                        val availableCrossAxis = availableSize - spacing
                        val columnSize = availableCrossAxis / 3
                        return listOf(columnSize, columnSize * 2)
                    }
                },
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(100.dp)

        rule.onNodeWithTag("2")
            .assertCrossAxisStartPositionInRootIsEqualTo(100.dp)
            .assertCrossAxisSizeIsEqualTo(200.dp)

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun onlyOneInitialMeasurePass() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyGrid(
                1,
                Modifier.requiredSize(100.dp).testTag(LazyGridTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.numMeasurePasses).isEqualTo(1)
        }
    }

    @Test
    fun laysOutRtlCorrectlyWithLargerContainer() {
        val mainAxisSize = with(rule.density) { 250.toDp() }
        val crossAxisSize = with(rule.density) { 110.toDp() }
        val itemSize = with(rule.density) { 50.toDp() }

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyGrid(cells = 2, modifier = Modifier.axisSize(crossAxisSize, mainAxisSize)) {
                    items(4) { index ->
                        val label = (index + 1).toString()
                        BasicText(label, Modifier.size(itemSize).testTag(label))
                    }
                }
            }
        }

        rule.onNodeWithTag("1").apply {
            if (vertical) {
                // 2 1
                // 4 3
                assertMainAxisStartPositionInRootIsEqualTo(0.dp)
                assertCrossAxisStartPositionInRootIsEqualTo(crossAxisSize / 2)
            } else {
                // 3 1
                // 4 2
                assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
                assertMainAxisStartPositionInRootIsEqualTo(mainAxisSize - itemSize)
            }
        }
    }

    @Test
    fun scrollDuringMeasure() {
        rule.setContent {
            BoxWithConstraints {
                val state = rememberLazyGridState()
                LazyGrid(
                    cells = 2,
                    state = state,
                    modifier = Modifier.axisSize(40.dp, 100.dp)
                ) {
                    items(20) {
                        val tag = it.toString()
                        BasicText(
                            text = tag,
                            modifier = Modifier.axisSize(20.dp, 20.dp).testTag(tag)
                        )
                    }
                }
                LaunchedEffect(state) {
                    state.scrollToItem(10)
                }
            }
        }

        rule.onNodeWithTag("10")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun scrollInLaunchedEffect() {
        rule.setContent {
            val state = rememberLazyGridState()
            LazyGrid(
                cells = 2,
                state = state,
                modifier = Modifier.axisSize(40.dp, 100.dp)
            ) {
                items(20) {
                    val tag = it.toString()
                    BasicText(
                        text = tag,
                        modifier = Modifier.axisSize(20.dp, 20.dp).testTag(tag)
                    )
                }
            }
            LaunchedEffect(state) {
                state.scrollToItem(10)
            }
        }

        rule.onNodeWithTag("10")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun changedLinesRemeasuredCorrectly() {
        var flag by mutableStateOf(false)
        rule.setContent {
            LazyGrid(cells = GridCells.Fixed(2), modifier = Modifier.axisSize(60.dp, 100.dp)) {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(Modifier.mainAxisSize(32.dp).background(Color.Red))
                }

                if (flag) {
                    item {
                        Box(Modifier.mainAxisSize(32.dp).background(Color.Blue))
                    }

                    item {
                        Box(Modifier.mainAxisSize(32.dp).background(Color.Yellow).testTag("target"))
                    }
                } else {
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Box(Modifier.mainAxisSize(32.dp).background(Color.Blue))
                    }

                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Box(Modifier.mainAxisSize(32.dp).background(Color.Yellow).testTag("target"))
                    }
                }
            }
        }

        flag = true
        rule.onNodeWithTag("target")
            .assertCrossAxisSizeIsEqualTo(30.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(32.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(30.dp)
    }
}

internal fun IntegerSubject.isEqualTo(expected: Int, tolerance: Int) {
    isIn(Range.closed(expected - tolerance, expected + tolerance))
}

internal fun SemanticsNodeInteraction.scrollBy(x: Dp = 0.dp, y: Dp = 0.dp, density: Density) =
    performTouchInput {
        with(density) {
            val touchSlop = TestTouchSlop.toInt()
            val xPx = x.roundToPx()
            val yPx = y.roundToPx()
            val offsetX = if (xPx > 0) xPx + touchSlop else if (xPx < 0) xPx - touchSlop else 0
            val offsetY = if (yPx > 0) yPx + touchSlop else if (yPx < 0) yPx - touchSlop else 0
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - offsetX, center.y - offsetY),
                endVelocity = 0f
            )
        }
    }
