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

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LazyArrangementsTest {

    private val ContainerTag = "ContainerTag"

    @get:Rule
    val rule = createComposeRule()

    private var itemSize: Dp = Dp.Infinity
    private var smallerItemSize: Dp = Dp.Infinity
    private var containerSize: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSize = 50.toDp()
        }
        with(rule.density) {
            smallerItemSize = 40.toDp()
        }
        containerSize = itemSize * 5
    }

    // cases when we have not enough items to fill min constraints:

    @Test
    fun column_defaultArrangementIsTop() {
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Top)
    }

    @Test
    fun column_centerArrangement() {
        composeColumnWith(Arrangement.Center)
        assertArrangementForTwoItems(Arrangement.Center)
    }

    @Test
    fun column_bottomArrangement() {
        composeColumnWith(Arrangement.Bottom)
        assertArrangementForTwoItems(Arrangement.Bottom)
    }

    @Test
    fun column_spacedArrangementNotFillingViewport() {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeColumnWith(arrangement)
        assertArrangementForTwoItems(arrangement)
    }

    @Test
    fun row_defaultArrangementIsStart() {
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Start, LayoutDirection.Ltr)
    }

    @Test
    fun row_centerArrangement() {
        composeRowWith(Arrangement.Center, LayoutDirection.Ltr)
        assertArrangementForTwoItems(Arrangement.Center, LayoutDirection.Ltr)
    }

    @Test
    fun row_endArrangement() {
        composeRowWith(Arrangement.End, LayoutDirection.Ltr)
        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Ltr)
    }

    @Test
    fun row_spacedArrangementNotFillingViewport() {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeRowWith(arrangement, LayoutDirection.Ltr)
        assertArrangementForTwoItems(arrangement, LayoutDirection.Ltr)
    }

    @Test
    fun row_rtl_startArrangement() {
        composeRowWith(Arrangement.Center, LayoutDirection.Rtl)
        assertArrangementForTwoItems(Arrangement.Center, LayoutDirection.Rtl)
    }

    @Test
    fun row_rtl_endArrangement() {
        composeRowWith(Arrangement.End, LayoutDirection.Rtl)
        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Rtl)
    }

    @Test
    fun row_rtl_spacedArrangementNotFillingViewport() {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeRowWith(arrangement, LayoutDirection.Rtl)
        assertArrangementForTwoItems(arrangement, LayoutDirection.Rtl)
    }

    // wrap content and spacing

    @Test
    fun column_spacing_affects_wrap_content() {
        rule.setContent {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.testTag(ContainerTag)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertWidthIsEqualTo(itemSize)
            .assertHeightIsEqualTo(itemSize * 3)
    }

    @Test
    fun row_spacing_affects_wrap_content() {
        rule.setContent {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.testTag(ContainerTag)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertWidthIsEqualTo(itemSize * 3)
            .assertHeightIsEqualTo(itemSize)
    }

    // spacing added when we have enough items to fill the viewport

    @Test
    fun column_spacing_scrolledToTheTop() {
        rule.setContent {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun column_spacing_scrolledToTheBottom() {
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f).testTag(ContainerTag)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(y = itemSize * 2, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 0.5f)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2.5f)
    }

    @Test
    fun row_spacing_scrolledToTheStart() {
        rule.setContent {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun row_spacing_scrolledToTheEnd() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f).testTag(ContainerTag)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(x = itemSize * 2, density = rule.density)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 0.5f)

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f)
    }

    @Test
    fun column_scrollingByExactlyTheItemSizePlusSpacer_switchesTheFirstVisibleItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun column_scrollingByExactlyTheItemSizePlusHalfTheSpacer_staysOnTheSameItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx / 2).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset)
                .isEqualTo(itemSizePx + spacingSizePx / 2)
        }
    }

    @Test
    fun row_scrollingByExactlyTheItemSizePlusSpacer_switchesTheFirstVisibleItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                horizontalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun row_scrollingByExactlyTheItemSizePlusHalfTheSpacer_staysOnTheSameItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                horizontalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx / 2).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset)
                .isEqualTo(itemSizePx + spacingSizePx / 2)
        }
    }

    // with reverseLayout == true

    @Test
    fun column_defaultArrangementIsBottomWithReverseLayout() {
        rule.setContent {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Bottom, reverseLayout = true)
    }

    @Test
    fun row_defaultArrangementIsEndWithReverseLayout() {
        rule.setContent {
            LazyRow(
                reverseLayout = true,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(
            Arrangement.End, LayoutDirection.Ltr, reverseLayout = true
        )
    }

    @Test
    fun column_whenArrangementChanges() {
        var arrangement by mutableStateOf(Arrangement.Top)
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = arrangement
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Top)

        rule.runOnIdle {
            arrangement = Arrangement.Bottom
        }

        assertArrangementForTwoItems(Arrangement.Bottom)
    }

    @Test
    fun row_whenArrangementChanges() {
        var arrangement by mutableStateOf(Arrangement.Start)
        rule.setContent {
            LazyRow(
                modifier = Modifier.requiredSize(containerSize),
                horizontalArrangement = arrangement
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Start, LayoutDirection.Ltr)

        rule.runOnIdle {
            arrangement = Arrangement.End
        }

        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Ltr)
    }

    fun composeColumnWith(arrangement: Arrangement.Vertical) {
        rule.setContent {
            LazyColumn(
                verticalArrangement = arrangement,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }
    }

    fun composeRowWith(arrangement: Arrangement.Horizontal, layoutDirection: LayoutDirection) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyRow(
                    horizontalArrangement = arrangement,
                    modifier = Modifier.requiredSize(containerSize)
                ) {
                    items(2) {
                        Item(it)
                    }
                }
            }
        }
    }

    @Composable
    fun Item(index: Int) {
        require(index < 2)
        val size = if (index == 0) itemSize else smallerItemSize
        Box(Modifier.requiredSize(size).testTag(index.toString()))
    }

    fun assertArrangementForTwoItems(
        arrangement: Arrangement.Vertical,
        reverseLayout: Boolean = false
    ) {
        with(rule.density) {
            val sizes = IntArray(2) {
                val index = if (reverseLayout) if (it == 0) 1 else 0 else it
                if (index == 0) itemSize.roundToPx() else smallerItemSize.roundToPx()
            }
            val outPositions = IntArray(2) { 0 }
            with(arrangement) { arrange(containerSize.roundToPx(), sizes, outPositions) }

            outPositions.forEachIndexed { index, position ->
                val realIndex = if (reverseLayout) if (index == 0) 1 else 0 else index
                rule.onNodeWithTag("$realIndex")
                    .assertTopPositionInRootIsEqualTo(position.toDp())
            }
        }
    }

    fun assertArrangementForTwoItems(
        arrangement: Arrangement.Horizontal,
        layoutDirection: LayoutDirection,
        reverseLayout: Boolean = false
    ) {
        with(rule.density) {
            val sizes = IntArray(2) {
                val index = if (reverseLayout) if (it == 0) 1 else 0 else it
                if (index == 0) itemSize.roundToPx() else smallerItemSize.roundToPx()
            }
            val outPositions = IntArray(2) { 0 }
            with(arrangement) {
                arrange(containerSize.roundToPx(), sizes, layoutDirection, outPositions)
            }

            outPositions.forEachIndexed { index, position ->
                val realIndex = if (reverseLayout) if (index == 0) 1 else 0 else index
                val expectedPosition = position.toDp()
                rule.onNodeWithTag("$realIndex")
                    .assertLeftPositionInRootIsEqualTo(expectedPosition)
            }
        }
    }
}
