/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyGridAnimateItemPlacementTest(private val config: Config) {

    private val isVertical: Boolean get() = config.isVertical
    private val reverseLayout: Boolean get() = config.reverseLayout

    @get:Rule
    val rule = createComposeRule()

    private val itemSize: Float = 50f
    private var itemSizeDp: Dp = Dp.Infinity
    private val itemSize2: Float = 30f
    private var itemSize2Dp: Dp = Dp.Infinity
    private val itemSize3: Float = 20f
    private var itemSize3Dp: Dp = Dp.Infinity
    private val containerSize: Float = itemSize * 5
    private var containerSizeDp: Dp = Dp.Infinity
    private val spacing: Float = 10f
    private var spacingDp: Dp = Dp.Infinity
    private val itemSizePlusSpacing = itemSize + spacing
    private var itemSizePlusSpacingDp = Dp.Infinity
    private lateinit var state: LazyGridState

    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        with(rule.density) {
            itemSizeDp = itemSize.toDp()
            itemSize2Dp = itemSize2.toDp()
            itemSize3Dp = itemSize3.toDp()
            containerSizeDp = containerSize.toDp()
            spacingDp = spacing.toDp()
            itemSizePlusSpacingDp = itemSizePlusSpacing.toDp()
        }
    }

    @Test
    fun reorderTwoItems() {
        var list by mutableStateOf(listOf(0, 1))
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(0f, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(1, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0f + itemSize * fraction),
                1 to AxisOffset(0f, itemSize - itemSize * fraction),
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderTwoByTwoItems() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        rule.setContent {
            LazyGrid(2) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(3, 2, 1, 0)
        }

        onAnimationFrame { fraction ->
            val increasing = 0 + itemSize * fraction
            val decreasing = itemSize - itemSize * fraction
            assertPositions(
                0 to AxisOffset(increasing, increasing),
                1 to AxisOffset(decreasing, increasing),
                2 to AxisOffset(increasing, decreasing),
                3 to AxisOffset(decreasing, decreasing),
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderTwoItems_layoutInfoHasFinalPositions() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        rule.setContent {
            LazyGrid(2) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertLayoutInfoPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(3, 2, 1, 0)
        }

        onAnimationFrame {
            // fraction doesn't affect the offsets in layout info
            assertLayoutInfoPositions(
                3 to AxisOffset(0f, 0f),
                2 to AxisOffset(itemSize, 0f),
                1 to AxisOffset(0f, itemSize),
                0 to AxisOffset(itemSize, itemSize)
            )
        }
    }

    @Test
    fun reorderFirstAndLastItems() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(0f, itemSize),
            2 to AxisOffset(0f, itemSize * 2),
            3 to AxisOffset(0f, itemSize * 3),
            4 to AxisOffset(0f, itemSize * 4)
        )

        rule.runOnUiThread {
            list = listOf(4, 1, 2, 3, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0f + itemSize * 4 * fraction),
                1 to AxisOffset(0f, itemSize),
                2 to AxisOffset(0f, itemSize * 2),
                3 to AxisOffset(0f, itemSize * 3),
                4 to AxisOffset(0f, itemSize * 4 - itemSize * 4 * fraction),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveFirstItemToEndCausingAllItemsToAnimate() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyGrid(2) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize),
            4 to AxisOffset(0f, itemSize * 2),
            5 to AxisOffset(itemSize, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(1, 2, 3, 4, 5, 0)
        }

        onAnimationFrame { fraction ->
            val increasingX = 0 + itemSize * fraction
            val decreasingX = itemSize - itemSize * fraction
            assertPositions(
                0 to AxisOffset(increasingX, 0f + itemSize * 2 * fraction),
                1 to AxisOffset(decreasingX, 0f),
                2 to AxisOffset(increasingX, itemSize - itemSize * fraction),
                3 to AxisOffset(decreasingX, itemSize),
                4 to AxisOffset(increasingX, itemSize * 2 - itemSize * fraction),
                5 to AxisOffset(decreasingX, itemSize * 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun itemSizeChangeAnimatesNextItems() {
        var size by mutableStateOf(itemSizeDp)
        rule.setContent {
            LazyGrid(1, minSize = itemSizeDp * 5, maxSize = itemSizeDp * 5) {
                items(listOf(0, 1, 2, 3), key = { it }) {
                    Item(it, size = if (it == 1) size else itemSizeDp)
                }
            }
        }

        rule.runOnUiThread {
            size = itemSizeDp * 2
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(size)

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0f),
                1 to AxisOffset(0f, itemSize),
                2 to AxisOffset(0f, itemSize * 2 + itemSize * fraction),
                3 to AxisOffset(0f, itemSize * 3 + itemSize * fraction),
                fraction = fraction
            )
        }
    }

    @Test
    fun onlyItemsWithModifierAnimates() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    Item(it, animSpec = if (it == 1 || it == 3) AnimSpec else null)
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(1, 2, 3, 4, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, itemSize * 4),
                1 to AxisOffset(0f, itemSize - itemSize * fraction),
                2 to AxisOffset(0f, itemSize),
                3 to AxisOffset(0f, itemSize * 3 - itemSize * fraction),
                4 to AxisOffset(0f, itemSize * 3),
                fraction = fraction
            )
        }
    }

    @Test
    fun animationsWithDifferentDurations() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    val duration = if (it == 1 || it == 3) Duration * 2 else Duration
                    Item(it, animSpec = tween(duration.toInt(), easing = LinearEasing))
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(1, 2, 3, 4, 0)
        }

        onAnimationFrame(duration = Duration * 2) { fraction ->
            val shorterAnimFraction = (fraction * 2).coerceAtMost(1f)
            assertPositions(
                0 to AxisOffset(0f, 0 + itemSize * 4 * shorterAnimFraction),
                1 to AxisOffset(0f, itemSize - itemSize * fraction),
                2 to AxisOffset(0f, itemSize * 2 - itemSize * shorterAnimFraction),
                3 to AxisOffset(0f, itemSize * 3 - itemSize * fraction),
                4 to AxisOffset(0f, itemSize * 4 - itemSize * shorterAnimFraction),
                fraction = fraction
            )
        }
    }

    @Test
    fun multipleChildrenPerItem() {
        var list by mutableStateOf(listOf(0, 2))
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    Item(it)
                    Item(it + 1)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(0f, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(0f, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(2, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0 + itemSize * fraction),
                1 to AxisOffset(0f, 0 + itemSize * fraction),
                2 to AxisOffset(0f, itemSize - itemSize * fraction),
                3 to AxisOffset(0f, itemSize - itemSize * fraction),
                fraction = fraction
            )
        }
    }

    @Test
    fun multipleChildrenPerItemSomeDoNotAnimate() {
        var list by mutableStateOf(listOf(0, 2))
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    Item(it)
                    Item(it + 1, animSpec = null)
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(2, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0 + itemSize * fraction),
                1 to AxisOffset(0f, itemSize),
                2 to AxisOffset(0f, itemSize - itemSize * fraction),
                3 to AxisOffset(0f, 0f),
                fraction = fraction
            )
        }
    }

    @Test
    fun animateArrangementChange() {
        var arrangement by mutableStateOf(Arrangement.Center)
        rule.setContent {
            LazyGrid(
                1,
                arrangement = arrangement,
                minSize = itemSizeDp * 5,
                maxSize = itemSizeDp * 5
            ) {
                items(listOf(1, 2, 3), key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            1 to AxisOffset(0f, itemSize),
            2 to AxisOffset(0f, itemSize * 2),
            3 to AxisOffset(0f, itemSize * 3),
        )

        rule.runOnUiThread {
            arrangement = Arrangement.SpaceBetween
        }
        rule.mainClock.advanceTimeByFrame()

        onAnimationFrame { fraction ->
            assertPositions(
                1 to AxisOffset(0f, itemSize - itemSize * fraction),
                2 to AxisOffset(0f, itemSize * 2),
                3 to AxisOffset(0f, itemSize * 3 + itemSize * fraction),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        val gridSize = itemSize * 3
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(2, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize),
            4 to AxisOffset(0f, itemSize * 2),
            5 to AxisOffset(itemSize, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            // item 1 moves to and item 8 moves from `gridSize`, right after the end edge
            val item1Offset = AxisOffset(itemSize, 0 + gridSize * fraction)
            val item8Offset =
                AxisOffset(itemSize, gridSize - gridSize * fraction)
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                if (item1Offset.mainAxis < itemSize * 3) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(2 to AxisOffset(0f, itemSize))
                add(3 to AxisOffset(itemSize, itemSize))
                add(4 to AxisOffset(0f, itemSize * 2))
                add(5 to AxisOffset(itemSize, itemSize * 2))
                if (item8Offset.mainAxis < itemSize * 3) {
                    add(8 to item8Offset)
                } else {
                    rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        rule.setContent {
            LazyGrid(2, maxSize = itemSizeDp * 3, startIndex = 6) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            6 to AxisOffset(0f, 0f),
            7 to AxisOffset(itemSize, 0f),
            8 to AxisOffset(0f, itemSize),
            9 to AxisOffset(itemSize, itemSize),
            10 to AxisOffset(0f, itemSize * 2),
            11 to AxisOffset(itemSize, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            // item 1 moves from and item 8 moves to `0 - itemSize`, right before the start edge
            val item8Offset = AxisOffset(0f, itemSize - itemSize * 2 * fraction)
            val item1Offset = AxisOffset(0f, -itemSize + itemSize * 2 * fraction)
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                if (item1Offset.mainAxis > -itemSize) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(6 to AxisOffset(0f, 0f))
                add(7 to AxisOffset(itemSize, 0f))
                if (item8Offset.mainAxis > -itemSize) {
                    add(8 to item8Offset)
                } else {
                    rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
                add(9 to AxisOffset(itemSize, itemSize))
                add(10 to AxisOffset(0f, itemSize * 2))
                add(11 to AxisOffset(itemSize, itemSize * 2))
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveFirstItemToEndCausingAllItemsToAnimate_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        rule.setContent {
            LazyGrid(2, arrangement = Arrangement.spacedBy(spacingDp)) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(1, 2, 3, 4, 5, 6, 7, 0)
        }

        onAnimationFrame { fraction ->
            val increasingX = fraction * itemSize
            val decreasingX = itemSize - itemSize * fraction
            assertPositions(
                0 to AxisOffset(increasingX, itemSizePlusSpacing * 3 * fraction),
                1 to AxisOffset(decreasingX, 0f),
                2 to AxisOffset(
                    increasingX,
                    itemSizePlusSpacing - itemSizePlusSpacing * fraction
                ),
                3 to AxisOffset(decreasingX, itemSizePlusSpacing),
                4 to AxisOffset(
                    increasingX,
                    itemSizePlusSpacing * 2 - itemSizePlusSpacing * fraction
                ),
                5 to AxisOffset(decreasingX, itemSizePlusSpacing * 2),
                6 to AxisOffset(
                    increasingX,
                    itemSizePlusSpacing * 3 - itemSizePlusSpacing * fraction
                ),
                7 to AxisOffset(decreasingX, itemSizePlusSpacing * 3),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        val gridSize = itemSize * 3 + spacing * 2
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(
                2,
                maxSize = gridSizeDp,
                arrangement = Arrangement.spacedBy(spacingDp)
            ) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSizePlusSpacing),
            3 to AxisOffset(itemSize, itemSizePlusSpacing),
            4 to AxisOffset(0f, itemSizePlusSpacing * 2),
            5 to AxisOffset(itemSize, itemSizePlusSpacing * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9)
        }

        onAnimationFrame { fraction ->
            // item 1 moves to and item 8 moves from `gridSize`, right after the end edge
            val item1Offset = AxisOffset(itemSize, gridSize * fraction)
            val item8Offset = AxisOffset(itemSize, gridSize - gridSize * fraction)
            val screenSize = itemSize * 3 + spacing * 2
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                if (item1Offset.mainAxis < screenSize) {
                    add(1 to item1Offset)
                }
                add(2 to AxisOffset(0f, itemSizePlusSpacing))
                add(3 to AxisOffset(itemSize, itemSizePlusSpacing))
                add(4 to AxisOffset(0f, itemSizePlusSpacing * 2))
                add(5 to AxisOffset(itemSize, itemSizePlusSpacing * 2))
                if (item8Offset.mainAxis < screenSize) {
                    add(8 to item8Offset)
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        rule.setContent {
            LazyGrid(
                2,
                maxSize = itemSizeDp * 3 + spacingDp * 2,
                arrangement = Arrangement.spacedBy(spacingDp),
                startIndex = 4
            ) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            4 to AxisOffset(0f, 0f),
            5 to AxisOffset(itemSize, 0f),
            6 to AxisOffset(0f, itemSizePlusSpacing),
            7 to AxisOffset(itemSize, itemSizePlusSpacing),
            8 to AxisOffset(0f, itemSizePlusSpacing * 2),
            9 to AxisOffset(itemSize, itemSizePlusSpacing * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            // item 8 moves to and item 1 moves from `-itemSize`, right before the start edge
            val item1Offset = AxisOffset(
                0f,
                -itemSize + (itemSize + itemSizePlusSpacing * 2) * fraction
            )
            val item8Offset = AxisOffset(
                0f,
                itemSizePlusSpacing * 2 -
                    (itemSize + itemSizePlusSpacing * 2) * fraction
            )
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                if (item1Offset.mainAxis > -itemSize) {
                    add(1 to item1Offset)
                }
                add(4 to AxisOffset(0f, 0f))
                add(5 to AxisOffset(itemSize, 0f))
                add(6 to AxisOffset(0f, itemSizePlusSpacing))
                add(7 to AxisOffset(itemSize, itemSizePlusSpacing))
                if (item8Offset.mainAxis > -itemSize) {
                    add(8 to item8Offset)
                }
                add(9 to AxisOffset(itemSize, itemSizePlusSpacing * 2))
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds_differentSizes() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        rule.setContent {
            LazyGrid(2, maxSize = itemSize2Dp + itemSize3Dp + itemSizeDp, startIndex = 6) {
                items(list, key = { it }) {
                    val height = when (it) {
                        2 -> itemSize3Dp
                        3 -> itemSize3Dp / 2
                        6 -> itemSize2Dp
                        7 -> itemSize2Dp / 2
                        else -> {
                            if (it % 2 == 0) itemSizeDp else itemSize3Dp / 2
                        }
                    }
                    Item(it, size = height)
                }
            }
        }

        val line3Size = itemSize2
        val line4Size = itemSize
        assertPositions(
            6 to AxisOffset(0f, 0f),
            7 to AxisOffset(itemSize, 0f),
            8 to AxisOffset(0f, line3Size),
            9 to AxisOffset(itemSize, line3Size),
            10 to AxisOffset(0f, line3Size + line4Size),
            11 to AxisOffset(itemSize, line3Size + line4Size)
        )

        rule.runOnUiThread {
            // swap 8 and 2
            list = listOf(0, 1, 8, 3, 4, 5, 6, 7, 2, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            // items 4 and 5 were lines 1 and 3 but we don't compose it
            rule.onNodeWithTag("4").assertDoesNotExist()
            rule.onNodeWithTag("5").assertDoesNotExist()
            val item2Size = itemSize3 /* the real size of the item 2 */
            // item 2 moves from and item 4 moves to `0 - item size`, right before the start edge
            val startItem2Offset = -item2Size
            val item2Offset =
                startItem2Offset + (itemSize2 - startItem2Offset) * fraction
            val item8Size = itemSize /* the real size of the item 8 */
            val endItem8Offset = -item8Size
            val item8Offset = line3Size - (line3Size - endItem8Offset) * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                if (item8Offset > -line4Size) {
                    add(8 to AxisOffset(0f, item8Offset))
                } else {
                    rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
                add(6 to AxisOffset(0f, 0f))
                add(7 to AxisOffset(itemSize, 0f))
                if (item2Offset > -item2Size) {
                    add(2 to AxisOffset(0f, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
                add(9 to AxisOffset(itemSize, line3Size))
                add(10 to AxisOffset(
                    0f,
                    line3Size + line4Size - (itemSize - itemSize3) * fraction
                ))
                add(11 to AxisOffset(
                    itemSize,
                    line3Size + line4Size - (itemSize - itemSize3) * fraction
                ))
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_differentSizes() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        val gridSize = itemSize2 + itemSize3 + itemSize
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(2, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    val height = when (it) {
                        0 -> itemSize2Dp
                        8 -> itemSize3Dp
                        else -> {
                            if (it % 2 == 0) itemSizeDp else itemSize3Dp / 2
                        }
                    }
                    Item(it, size = height)
                }
            }
        }

        val line0Size = itemSize2
        val line1Size = itemSize
        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, line0Size),
            3 to AxisOffset(itemSize, line0Size),
            4 to AxisOffset(0f, line0Size + line1Size),
            5 to AxisOffset(itemSize, line0Size + line1Size),
        )

        rule.runOnUiThread {
            list = listOf(0, 1, 8, 3, 4, 5, 6, 7, 2, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            // item 1 moves from and item 8 moves to `gridSize`, right after the end edge
            val startItem8Offset = gridSize
            val endItem2Offset = gridSize
            val line4Size = itemSize3
            val item2Offset =
                line0Size + (endItem2Offset - line0Size) * fraction
            val item8Offset =
                startItem8Offset - (startItem8Offset - line0Size) * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                add(1 to AxisOffset(itemSize, 0f))
                if (item8Offset < gridSize) {
                    add(8 to AxisOffset(0f, item8Offset))
                } else {
                    rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
                add(3 to AxisOffset(itemSize, line0Size))
                add(4 to AxisOffset(
                    0f,
                    line0Size + line1Size - (line1Size - line4Size) * fraction
                ))
                add(5 to AxisOffset(
                    itemSize,
                    line0Size + line1Size - (line1Size - line4Size) * fraction
                ))
                if (item2Offset < gridSize) {
                    add(2 to AxisOffset(0f, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    // @Test
    // fun animateAlignmentChange() {
    //     var alignment by mutableStateOf(CrossAxisAlignment.End)
    //     rule.setContent{
    //         LazyGrid(1,
    //             crossAxisAlignment = alignment,
    //             crossAxisSize = itemSizeDp
    //         ) {
    //             items(listOf(1, 2, 3), key = { it }) {
    //                 val crossAxisSize =
    //                     if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
    //                 Item(it, crossAxisSize = crossAxisSize)
    //             }
    //         }
    //     }

    //     val item2Start = itemSize - itemSize2
    //     val item3Start = itemSize - itemSize3
    //     assertPositions(
    //         1 to 0,
    //         2 to itemSize,
    //         3 to itemSize * 2,
    //         crossAxis = listOf(
    //             1 to 0,
    //             2 to item2Start,
    //             3 to item3Start,
    //         )
    //     )

    //     rule.runOnUiThread {
    //         alignment = CrossAxisAlignment.Center
    //     }
    //     rule.mainClock.advanceTimeByFrame()

    //     val item2End = itemSize / 2 - itemSize2 / 2
    //     val item3End = itemSize / 2 - itemSize3 / 2
    //     onAnimationFrame { fraction ->
    //         assertPositions(
    //             1 to 0,
    //             2 to itemSize,
    //             3 to itemSize * 2,
    //             crossAxis = listOf(
    //                 1 to 0,
    //                 2 to item2Start + ((item2End - item2Start) * fraction,
    //                 3 to item3Start + ((item3End - item3Start) * fraction,
    //             ),
    //             fraction = fraction
    //         )
    //     }
    // }

    // @Test
    // fun animateAlignmentChange_multipleChildrenPerItem() {
    //     var alignment by mutableStateOf(CrossAxisAlignment.Start)
    //     rule.setContent{
    //         LazyGrid(1,
    //             crossAxisAlignment = alignment,
    //             crossAxisSize = itemSizeDp * 2
    //         ) {
    //             items(1) {
    //                 listOf(1, 2, 3).forEach {
    //                     val crossAxisSize =
    //                         if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
    //                     Item(it, crossAxisSize = crossAxisSize)
    //                 }
    //             }
    //         }
    //     }

    //     rule.runOnUiThread {
    //         alignment = CrossAxisAlignment.End
    //     }
    //     rule.mainClock.advanceTimeByFrame()

    //     val containerSize = itemSize * 2
    //     onAnimationFrame { fraction ->
    //         assertPositions(
    //             1 to 0,
    //             2 to itemSize,
    //             3 to itemSize * 2,
    //             crossAxis = listOf(
    //                 1 to ((containerSize - itemSize) * fraction,
    //                 2 to ((containerSize - itemSize2) * fraction,
    //                 3 to ((containerSize - itemSize3) * fraction
    //             ),
    //             fraction = fraction
    //         )
    //     }
    // }

    // @Test
    // fun animateAlignmentChange_rtl() {
    //     // this test is not applicable to LazyRow
    //     assumeTrue(isVertical)

    //     var alignment by mutableStateOf(CrossAxisAlignment.End)
    //     rule.setContent{
    //         CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    //             LazyGrid(1,
    //                 crossAxisAlignment = alignment,
    //                 crossAxisSize = itemSizeDp
    //             ) {
    //                 items(listOf(1, 2, 3), key = { it }) {
    //                     val crossAxisSize =
    //                         if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
    //                     Item(it, crossAxisSize = crossAxisSize)
    //                 }
    //             }
    //         }
    //     }

    //     assertPositions(
    //         1 to 0,
    //         2 to itemSize,
    //         3 to itemSize * 2,
    //         crossAxis = listOf(
    //             1 to 0,
    //             2 to 0,
    //             3 to 0,
    //         )
    //     )

    //     rule.runOnUiThread {
    //         alignment = CrossAxisAlignment.Center
    //     }
    //     rule.mainClock.advanceTimeByFrame()

    //     onAnimationFrame { fraction ->
    //         assertPositions(
    //             1 to 0,
    //             2 to itemSize,
    //             3 to itemSize * 2,
    //             crossAxis = listOf(
    //                 1 to 0,
    //                 2 to ((itemSize / 2 - itemSize2 / 2) * fraction,
    //                 3 to ((itemSize / 2 - itemSize3 / 2) * fraction,
    //             ),
    //             fraction = fraction
    //         )
    //     }
    // }

    @Test
    fun moveItemToEndCausingNextItemsToAnimate_withContentPadding() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val rawStartPadding = 8f
        val rawEndPadding = 12f
        val (startPaddingDp, endPaddingDp) = with(rule.density) {
            rawStartPadding.toDp() to rawEndPadding.toDp()
        }
        rule.setContent {
            LazyGrid(1, startPadding = startPaddingDp, endPadding = endPaddingDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        val startPadding = if (reverseLayout) rawEndPadding else rawStartPadding
        assertPositions(
            0 to AxisOffset(0f, startPadding),
            1 to AxisOffset(0f, startPadding + itemSize),
            2 to AxisOffset(0f, startPadding + itemSize * 2),
            3 to AxisOffset(0f, startPadding + itemSize * 3),
            4 to AxisOffset(0f, startPadding + itemSize * 4),
        )

        rule.runOnUiThread {
            list = listOf(0, 2, 3, 4, 1)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, startPadding),
                1 to AxisOffset(
                    0f,
                    startPadding + itemSize + itemSize * 3 * fraction
                ),
                2 to AxisOffset(
                    0f,
                    startPadding + itemSize * 2 - itemSize * fraction
                ),
                3 to AxisOffset(
                    0f,
                    startPadding + itemSize * 3 - itemSize * fraction
                ),
                4 to AxisOffset(
                    0f,
                    startPadding + itemSize * 4 - itemSize * fraction
                ),
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderFirstAndLastItems_noNewLayoutInfoProduced() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))

        var measurePasses = 0
        rule.setContent {
            LazyGrid(1) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow { state.layoutInfo }
                    .collect {
                        measurePasses++
                    }
            }
        }

        rule.runOnUiThread {
            list = listOf(4, 1, 2, 3, 0)
        }

        var startMeasurePasses = Int.MIN_VALUE
        onAnimationFrame { fraction ->
            if (fraction == 0f) {
                startMeasurePasses = measurePasses
            }
        }
        rule.mainClock.advanceTimeByFrame()
        // new layoutInfo is produced on every remeasure of Lazy lists.
        // but we want to avoid remeasuring and only do relayout on each animation frame.
        // two extra measures are possible as we switch inProgress flag.
        assertThat(measurePasses).isAtMost(startMeasurePasses + 2)
    }

    @Test
    fun noAnimationWhenScrolledToOtherPosition() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollToItem(0, (itemSize / 2).roundToInt())
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, -itemSize / 2),
                1 to AxisOffset(0f, itemSize / 2),
                2 to AxisOffset(0f, itemSize * 3 / 2),
                3 to AxisOffset(0f, itemSize * 5 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardBySmallOffset() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(itemSize / 2f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, -itemSize / 2),
                1 to AxisOffset(0f, itemSize / 2),
                2 to AxisOffset(0f, itemSize * 3 / 2),
                3 to AxisOffset(0f, itemSize * 5 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardBySmallOffset() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3, startIndex = 2) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(-itemSize / 2f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                1 to AxisOffset(0f, -itemSize / 2),
                2 to AxisOffset(0f, itemSize / 2),
                3 to AxisOffset(0f, itemSize * 3 / 2),
                4 to AxisOffset(0f, itemSize * 5 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                2 to AxisOffset(0f, -itemSize / 2),
                3 to AxisOffset(0f, itemSize / 2),
                4 to AxisOffset(0f, itemSize * 3 / 2),
                5 to AxisOffset(0f, itemSize * 5 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3, startIndex = 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(-itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, -itemSize / 2),
                1 to AxisOffset(0f, itemSize / 2),
                2 to AxisOffset(0f, itemSize * 3 / 2),
                3 to AxisOffset(0f, itemSize * 5 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset_differentSizes() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it, size = if (it % 2 == 0) itemSizeDp else itemSize2Dp)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(itemSize + itemSize2 + itemSize / 2f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                2 to AxisOffset(0f, -itemSize / 2),
                3 to AxisOffset(0f, itemSize / 2),
                4 to AxisOffset(0f, itemSize2 + itemSize / 2),
                5 to AxisOffset(0f, itemSize2 + itemSize * 3 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset_differentSizes() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3, startIndex = 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it, size = if (it % 2 == 0) itemSizeDp else itemSize2Dp)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(-(itemSize + itemSize2 + itemSize / 2f))
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, -itemSize / 2),
                1 to AxisOffset(0f, itemSize / 2),
                2 to AxisOffset(0f, itemSize2 + itemSize / 2),
                3 to AxisOffset(0f, itemSize2 + itemSize * 3 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset_multipleCells() {
        rule.setContent {
            LazyGrid(3, maxSize = itemSizeDp * 2) {
                items(List(20) { it }, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(itemSize * 2, 0f),
            3 to AxisOffset(0f, itemSize),
            4 to AxisOffset(itemSize, itemSize),
            5 to AxisOffset(itemSize * 2, itemSize)
        )

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                6 to AxisOffset(0f, -itemSize / 2),
                7 to AxisOffset(itemSize, -itemSize / 2),
                8 to AxisOffset(itemSize * 2, -itemSize / 2),
                9 to AxisOffset(0f, itemSize / 2),
                10 to AxisOffset(itemSize, itemSize / 2),
                11 to AxisOffset(itemSize * 2, itemSize / 2),
                12 to AxisOffset(0f, itemSize * 3 / 2),
                13 to AxisOffset(itemSize, itemSize * 3 / 2),
                14 to AxisOffset(itemSize * 2, itemSize * 3 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset_multipleCells() {
        rule.setContent {
            LazyGrid(3, maxSize = itemSizeDp * 2, startIndex = 9) {
                items(List(20) { it }, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            9 to AxisOffset(0f, 0f),
            10 to AxisOffset(itemSize, 0f),
            11 to AxisOffset(itemSize * 2, 0f),
            12 to AxisOffset(0f, itemSize),
            13 to AxisOffset(itemSize, itemSize),
            14 to AxisOffset(itemSize * 2, itemSize)
        )

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(-itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, -itemSize / 2),
                1 to AxisOffset(itemSize, -itemSize / 2),
                2 to AxisOffset(itemSize * 2, -itemSize / 2),
                3 to AxisOffset(0f, itemSize / 2),
                4 to AxisOffset(itemSize, itemSize / 2),
                5 to AxisOffset(itemSize * 2, itemSize / 2),
                6 to AxisOffset(0f, itemSize * 3 / 2),
                7 to AxisOffset(itemSize, itemSize * 3 / 2),
                8 to AxisOffset(itemSize * 2, itemSize * 3 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset_differentSpans() {
        rule.setContent {
            LazyGrid(3, maxSize = itemSizeDp * 2) {
                items(
                    List(20) { it },
                    key = { it },
                    span = { GridItemSpan(if (it == 9) 3 else if (it == 10) 2 else 1) }
                ) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(itemSize * 2, 0f),
            3 to AxisOffset(0f, itemSize),
            4 to AxisOffset(itemSize, itemSize),
            5 to AxisOffset(itemSize * 2, itemSize)
        )

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                6 to AxisOffset(0f, -itemSize / 2),
                7 to AxisOffset(itemSize, -itemSize / 2),
                8 to AxisOffset(itemSize * 2, -itemSize / 2),
                9 to AxisOffset(0f, itemSize / 2), // 3 spans
                10 to AxisOffset(0f, itemSize * 3 / 2), // 2 spans
                11 to AxisOffset(itemSize * 2, itemSize * 3 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset_differentSpans() {
        rule.setContent {
            LazyGrid(3, maxSize = itemSizeDp * 2, startIndex = 6) {
                items(
                    List(20) { it },
                    key = { it },
                    span = { GridItemSpan(if (it == 3) 3 else if (it == 4) 2 else 1) }
                ) {
                    Item(it)
                }
            }
        }

        assertPositions(
            6 to AxisOffset(0f, 0f),
            7 to AxisOffset(itemSize, 0f),
            8 to AxisOffset(itemSize * 2, 0f),
            9 to AxisOffset(0f, itemSize),
            10 to AxisOffset(itemSize, itemSize),
            11 to AxisOffset(itemSize * 2, itemSize)
        )

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(-itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, -itemSize / 2),
                1 to AxisOffset(itemSize, -itemSize / 2),
                2 to AxisOffset(itemSize * 2, -itemSize / 2),
                3 to AxisOffset(0f, itemSize / 2), // 3 spans
                4 to AxisOffset(0f, itemSize * 3 / 2), // 2 spans
                5 to AxisOffset(itemSize * 2, itemSize * 3 / 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset_differentSpansAndDifferentSizes() {
        rule.setContent {
            LazyGrid(3, maxSize = itemSizeDp * 2) {
                items(
                    List(20) { it },
                    key = { it },
                    span = { GridItemSpan(if (it == 9) 3 else if (it == 10) 2 else 1) }
                ) {
                    Item(
                        it, size = when (it) {
                            in 6..8 -> itemSize2Dp
                            9 -> itemSize3Dp
                            else -> itemSizeDp
                        }
                    )
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(itemSize * 2, 0f),
            3 to AxisOffset(0f, itemSize),
            4 to AxisOffset(itemSize, itemSize),
            5 to AxisOffset(itemSize * 2, itemSize)
        )

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            val startOffset = -itemSize / 2
            assertPositions(
                6 to AxisOffset(0f, startOffset),
                7 to AxisOffset(itemSize, startOffset),
                8 to AxisOffset(itemSize * 2, startOffset),
                9 to AxisOffset(0f, startOffset + itemSize2), // 3 spans
                10 to AxisOffset(0f, startOffset + itemSize2 + itemSize3), // 2 spans
                11 to AxisOffset(itemSize * 2, startOffset + itemSize2 + itemSize3),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset_differentSpansAndDifferentSizes() {
        rule.setContent {
            LazyGrid(3, maxSize = itemSizeDp * 2, startIndex = 6) {
                items(
                    List(20) { it },
                    key = { it },
                    span = { GridItemSpan(if (it == 3) 3 else if (it == 4) 2 else 1) }
                ) {
                    Item(
                        it, size = when (it) {
                            in 0..2 -> itemSize2Dp
                            3 -> itemSize3Dp
                            else -> itemSizeDp
                        }
                    )
                }
            }
        }

        assertPositions(
            6 to AxisOffset(0f, 0f),
            7 to AxisOffset(itemSize, 0f),
            8 to AxisOffset(itemSize * 2, 0f),
            9 to AxisOffset(0f, itemSize),
            10 to AxisOffset(itemSize, itemSize),
            11 to AxisOffset(itemSize * 2, itemSize)
        )

        rule.runOnUiThread {
            runBlocking {
                state.scrollBy(-itemSize - itemSize3 - itemSize2 / 2f)
            }
        }

        onAnimationFrame { fraction ->
            val startOffset = -itemSize2 / 2
            assertPositions(
                0 to AxisOffset(0f, startOffset),
                1 to AxisOffset(itemSize, startOffset),
                2 to AxisOffset(itemSize * 2, startOffset),
                3 to AxisOffset(0f, startOffset + itemSize2), // 3 spans
                4 to AxisOffset(0f, startOffset + itemSize2 + itemSize3), // 2 spans
                5 to AxisOffset(itemSize * 2, startOffset + itemSize2 + itemSize3),
                fraction = fraction
            )
        }
    }

    @Test
    fun animatingItemsWithPreviousIndexLargerThanTheNewItemCount() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        val gridSize = itemSize * 2
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(2, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertLayoutInfoPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(0, 2, 4, 6)
        }

        onAnimationFrame { fraction ->
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                add(
                    2 to AxisOffset(
                        itemSize * fraction,
                        itemSize * (1f - fraction)
                    )
                )
                val item4and6MainAxis = gridSize - (gridSize - itemSize) * fraction
                if (item4and6MainAxis < gridSize) {
                    add(4 to AxisOffset(0f, item4and6MainAxis))
                    add(6 to AxisOffset(itemSize, item4and6MainAxis))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                    rule.onNodeWithTag("6").assertIsNotDisplayed()
                }
            }

            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun animatingItemsWithPreviousIndexLargerThanTheNewItemCount_differentSpans() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6))
        val gridSize = itemSize * 2
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(2, maxSize = gridSizeDp) {
                items(list, key = { it }, span = {
                    GridItemSpan(if (it == 6) maxLineSpan else 1)
                }) {
                    Item(it)
                }
            }
        }

        assertLayoutInfoPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(0, 4, 6)
        }

        onAnimationFrame { fraction ->
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                val item4MainAxis = gridSize - gridSize * fraction
                if (item4MainAxis < gridSize) {
                    add(
                        4 to AxisOffset(itemSize, item4MainAxis)
                    )
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
                val item6MainAxis = gridSize + itemSize - gridSize * fraction
                if (item6MainAxis < gridSize) {
                    add(
                        6 to AxisOffset(0f, item6MainAxis)
                    )
                } else {
                    rule.onNodeWithTag("6").assertIsNotDisplayed()
                }
            }

            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun itemWithSpecsIsMovingOut() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        val gridSize = itemSize * 2
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(1, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    Item(it, animSpec = if (it == 1) AnimSpec else null)
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(0, 2, 3, 1)
        }

        onAnimationFrame { fraction ->
            // item 1 moves to `gridSize`
            val item1Offset = itemSize + (gridSize - itemSize) * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                if (item1Offset < gridSize) {
                    add(1 to AxisOffset(0f, item1Offset))
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveTwoItemsToTheTopOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3f, startIndex = 3) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            3 to AxisOffset(0f, 0f),
            4 to AxisOffset(0f, itemSize),
            5 to AxisOffset(0f, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 4, 5, 3, 1, 2)
        }

        onAnimationFrame { fraction ->
            // item 2 moves from and item 5 moves to `-itemSize`, right before the start edge
            val item2Offset = -itemSize + itemSize * 3 * fraction
            val item5Offset = itemSize * 2 - itemSize * 3 * fraction
            // item 1 moves from and item 4 moves to `-itemSize * 2`, right before item 2
            val item1Offset = -itemSize * 2 + itemSize * 3 * fraction
            val item4Offset = itemSize - itemSize * 3 * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                if (item1Offset > -itemSize) {
                    add(1 to AxisOffset(0f, item1Offset))
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                if (item2Offset > -itemSize) {
                    add(2 to AxisOffset(0f, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
                add(3 to AxisOffset(0f, 0f))
                if (item4Offset > -itemSize) {
                    add(4 to AxisOffset(0f, item4Offset))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
                if (item5Offset > -itemSize) {
                    add(5 to AxisOffset(0f, item5Offset))
                } else {
                    rule.onNodeWithTag("5").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveTwoItemsToTheTopOutsideOfBounds_withReordering() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3f, startIndex = 3) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            3 to AxisOffset(0f, 0f),
            4 to AxisOffset(0f, itemSize),
            5 to AxisOffset(0f, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 5, 4, 3, 2, 1)
        }

        onAnimationFrame { fraction ->
            // item 2 moves from and item 4 moves to `-itemSize`, right before the start edge
            val item2Offset = -itemSize + itemSize * 2 * fraction
            val item4Offset = itemSize - itemSize * 2 * fraction
            // item 1 moves from and item 5 moves to `-itemSize * 2`, right before item 2
            val item1Offset = -itemSize * 2 + itemSize * 4 * fraction
            val item5Offset = itemSize * 2 - itemSize * 4 * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                if (item1Offset > -itemSize) {
                    add(1 to AxisOffset(0f, item1Offset))
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                if (item2Offset > -itemSize) {
                    add(2 to AxisOffset(0f, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
                add(3 to AxisOffset(0f, 0f))
                if (item4Offset > -itemSize) {
                    add(4 to AxisOffset(0f, item4Offset))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
                if (item5Offset > -itemSize) {
                    add(5 to AxisOffset(0f, item5Offset))
                } else {
                    rule.onNodeWithTag("5").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveTwoItemsToTheTopOutsideOfBounds_cellsOfTheSameLine() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyGrid(2, maxSize = itemSizeDp * 2f, startIndex = 3) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            2 to AxisOffset(0f, 0f),
            3 to AxisOffset(itemSize, 0f),
            4 to AxisOffset(0f, itemSize),
            5 to AxisOffset(itemSize, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(4, 5, 2, 3, 0, 1)
        }

        onAnimationFrame { fraction ->
            // items 0 and 2 moves from and items 4 and 5 moves to `-itemSize`,
            // right before the start edge
            val items0and1Offset = -itemSize + itemSize * 2 * fraction
            val items4and5Offset = itemSize - itemSize * 2 * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                if (items0and1Offset > -itemSize) {
                    add(0 to AxisOffset(0f, items0and1Offset))
                    add(1 to AxisOffset(itemSize, items0and1Offset))
                } else {
                    rule.onNodeWithTag("0").assertIsNotDisplayed()
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(2 to AxisOffset(0f, 0f))
                add(3 to AxisOffset(itemSize, 0f))
                if (items4and5Offset > -itemSize) {
                    add(4 to AxisOffset(0f, items4and5Offset))
                    add(5 to AxisOffset(itemSize, items4and5Offset))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                    rule.onNodeWithTag("5").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveTwoItemsToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val gridSize = itemSize * 3
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(1, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(0f, itemSize),
            2 to AxisOffset(0f, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 3, 4, 1, 2)
        }

        onAnimationFrame { fraction ->
            // item 1 moves to and item 3 moves from `gridSize`, right after the end edge
            val item1Offset = itemSize + (gridSize - itemSize) * fraction
            val item3Offset = gridSize - (gridSize - itemSize) * fraction
            // item 2 moves to and item 4 moves from `gridSize + itemSize`, right after item 4
            val item2Offset = itemSize * 2 + (gridSize - itemSize) * fraction
            val item4Offset = gridSize + itemSize - (gridSize - itemSize) * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                if (item1Offset < gridSize) {
                    add(1 to AxisOffset(0f, item1Offset))
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                if (item2Offset < gridSize) {
                    add(2 to AxisOffset(0f, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
                if (item3Offset < gridSize) {
                    add(3 to AxisOffset(0f, item3Offset))
                } else {
                    rule.onNodeWithTag("3").assertIsNotDisplayed()
                }
                if (item4Offset < gridSize) {
                    add(4 to AxisOffset(0f, item4Offset))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveTwoItemsToTheBottomOutsideOfBounds_withReordering() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val gridSize = itemSize * 3
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(1, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(0f, itemSize),
            2 to AxisOffset(0f, itemSize * 2)
        )

        rule.runOnUiThread {
            list = listOf(0, 4, 3, 2, 1)
        }

        onAnimationFrame { fraction ->
            // item 2 moves to and item 3 moves from `gridSize`, right after the end edge
            val item2Offset = itemSize * 2 + (gridSize - itemSize * 2) * fraction
            val item3Offset = gridSize - (gridSize - itemSize * 2) * fraction
            // item 1 moves to and item 4 moves from `gridSize + itemSize`, right after item 4
            val item1Offset = itemSize + (gridSize + itemSize - itemSize) * fraction
            val item4Offset =
                gridSize + itemSize - (gridSize + itemSize - itemSize) * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                if (item1Offset < gridSize) {
                    add(1 to AxisOffset(0f, item1Offset))
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                if (item2Offset < gridSize) {
                    add(2 to AxisOffset(0f, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
                if (item3Offset < gridSize) {
                    add(3 to AxisOffset(0f, item3Offset))
                } else {
                    rule.onNodeWithTag("3").assertIsNotDisplayed()
                }
                if (item4Offset < gridSize) {
                    add(4 to AxisOffset(0f, item4Offset))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveTwoItemsToTheBottomOutsideOfBounds_cellsOfTheSameLine() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val gridSize = itemSize * 2
        val gridSizeDp = with(rule.density) { gridSize.toDp() }
        rule.setContent {
            LazyGrid(2, maxSize = gridSizeDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisOffset(0f, 0f),
            1 to AxisOffset(itemSize, 0f),
            2 to AxisOffset(0f, itemSize),
            3 to AxisOffset(itemSize, itemSize)
        )

        rule.runOnUiThread {
            list = listOf(0, 1, 4, 5, 2, 3)
        }

        onAnimationFrame { fraction ->
            // items 4 and 5 moves from and items 2 and 3 moves to `gridSize`,
            // right before the start edge
            val items4and5Offset = gridSize - (gridSize - itemSize) * fraction
            val items2and3Offset = itemSize + (gridSize - itemSize) * fraction
            val expected = mutableListOf<Pair<Any, Offset>>().apply {
                add(0 to AxisOffset(0f, 0f))
                add(1 to AxisOffset(itemSize, 0f))
                if (items2and3Offset < gridSize) {
                    add(2 to AxisOffset(0f, items2and3Offset))
                    add(3 to AxisOffset(itemSize, items2and3Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                    rule.onNodeWithTag("3").assertIsNotDisplayed()
                }
                if (items4and5Offset < gridSize) {
                    add(4 to AxisOffset(0f, items4and5Offset))
                    add(5 to AxisOffset(itemSize, items4and5Offset))
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                    rule.onNodeWithTag("5").assertIsNotDisplayed()
                }
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenParentSizeShrinks() {
        var size by mutableStateOf(itemSizeDp * 3)
        rule.setContent {
            LazyGrid(1, maxSize = size) {
                items(listOf(0, 1, 2), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            size = itemSizeDp * 2
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0f),
                1 to AxisOffset(0f, itemSize),
                fraction = fraction
            )
            rule.onNodeWithTag("2").assertIsNotDisplayed()
        }
    }

    @Test
    fun noAnimationWhenParentSizeExpands() {
        var size by mutableStateOf(itemSizeDp * 2)
        rule.setContent {
            LazyGrid(1, maxSize = size) {
                items(listOf(0, 1, 2), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnUiThread {
            size = itemSizeDp * 3
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisOffset(0f, 0f),
                1 to AxisOffset(0f, itemSize),
                2 to AxisOffset(0f, itemSize * 2),
                fraction = fraction
            )
        }
    }

    private fun AxisOffset(crossAxis: Float, mainAxis: Float) =
        if (isVertical) Offset(crossAxis, mainAxis) else Offset(mainAxis, crossAxis)

    private val Offset.mainAxis: Float get() = if (isVertical) y else x

    private fun assertPositions(
        vararg expected: Pair<Any, Offset>,
        crossAxis: List<Pair<Any, Float>>? = null,
        fraction: Float? = null,
        autoReverse: Boolean = reverseLayout
    ) {
        val roundedExpected = expected.map { it.first to it.second.round() }
        val actualBounds = rule.onAllNodes(NodesWithTagMatcher)
            .fetchSemanticsNodes()
            .associateBy(
                keySelector = { it.config[SemanticsProperties.TestTag] },
                valueTransform = { IntRect(it.positionInRoot.round(), it.size) }
            )
        val actualPositions = expected.map {
            it.first to actualBounds.getValue(it.first.toString()).topLeft
        }
        val subject = if (fraction == null) {
            assertThat(actualPositions)
        } else {
            assertWithMessage("Fraction=$fraction").that(actualPositions)
        }
        subject.isEqualTo(
            roundedExpected.let { list ->
                if (!autoReverse) {
                    list
                } else {
                    val containerSize = actualBounds.getValue(ContainerTag).size
                    list.map {
                        val itemSize = actualBounds.getValue(it.first.toString()).size
                        it.first to
                            IntOffset(
                                if (isVertical) {
                                    it.second.x
                                } else {
                                    containerSize.width - itemSize.width - it.second.x
                                },
                                if (!isVertical) {
                                    it.second.y
                                } else {
                                    containerSize.height - itemSize.height - it.second.y
                                }
                            )
                    }
                }
            }
        )
        if (crossAxis != null) {
            val actualCross = expected.map {
                it.first to actualBounds.getValue(it.first.toString()).topLeft
                    .let { offset -> if (isVertical) offset.x else offset.y }
            }
            assertWithMessage(
                "CrossAxis" + if (fraction != null) "for fraction=$fraction" else ""
            )
                .that(actualCross)
                .isEqualTo(crossAxis.map { it.first to it.second.roundToInt() })
        }
    }

    private fun assertLayoutInfoPositions(vararg offsets: Pair<Any, Offset>) {
        rule.runOnIdle {
            assertThat(visibleItemsOffsets).isEqualTo(offsets.map { it.first to it.second.round() })
        }
    }

    private val visibleItemsOffsets: List<Pair<Any, IntOffset>>
        get() = state.layoutInfo.visibleItemsInfo.map {
            it.key to it.offset
        }

    private fun onAnimationFrame(duration: Long = Duration, onFrame: (fraction: Float) -> Unit) {
        require(duration.mod(FrameDuration) == 0L)
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        for (i in 0..duration step FrameDuration) {
            val fraction = i / duration.toFloat()
            onFrame(fraction)
            rule.mainClock.advanceTimeBy(FrameDuration)
            expectedTime += FrameDuration
            assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
        }
    }

    @Composable
    private fun LazyGrid(
        cells: Int,
        arrangement: Arrangement.HorizontalOrVertical? = null,
        minSize: Dp = 0.dp,
        maxSize: Dp = containerSizeDp,
        startIndex: Int = 0,
        startPadding: Dp = 0.dp,
        endPadding: Dp = 0.dp,
        content: LazyGridScope.() -> Unit
    ) {
        state = rememberLazyGridState(startIndex)
        if (isVertical) {
            LazyVerticalGrid(
                GridCells.Fixed(cells),
                Modifier
                    .requiredHeightIn(minSize, maxSize)
                    .requiredWidth(itemSizeDp * cells)
                    .testTag(ContainerTag),
                state = state,
                verticalArrangement = arrangement as? Arrangement.Vertical
                    ?: if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
                reverseLayout = reverseLayout,
                contentPadding = PaddingValues(top = startPadding, bottom = endPadding),
                content = content
            )
        } else {
            LazyHorizontalGrid(
                GridCells.Fixed(cells),
                Modifier
                    .requiredWidthIn(minSize, maxSize)
                    .requiredHeight(itemSizeDp * cells)
                    .testTag(ContainerTag),
                state = state,
                horizontalArrangement = arrangement as? Arrangement.Horizontal
                    ?: if (!reverseLayout) Arrangement.Start else Arrangement.End,
                reverseLayout = reverseLayout,
                contentPadding = PaddingValues(start = startPadding, end = endPadding),
                content = content
            )
        }
    }

    @Composable
    private fun LazyGridItemScope.Item(
        tag: Int,
        size: Dp = itemSizeDp,
        animSpec: FiniteAnimationSpec<IntOffset>? = AnimSpec
    ) {
        Box(
            Modifier
                .then(
                    if (isVertical) {
                        Modifier.requiredHeight(size)
                    } else {
                        Modifier.requiredWidth(size)
                    }
                )
                .testTag(tag.toString())
                .then(
                    if (animSpec != null) {
                        Modifier.animateItemPlacement(animSpec)
                    } else {
                        Modifier
                    }
                )
        )
    }

    private fun SemanticsNodeInteraction.assertMainAxisSizeIsEqualTo(
        expected: Dp
    ): SemanticsNodeInteraction {
        return if (isVertical) assertHeightIsEqualTo(expected) else assertWidthIsEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(
            Config(isVertical = true, reverseLayout = false),
            Config(isVertical = false, reverseLayout = false),
            Config(isVertical = true, reverseLayout = true),
            Config(isVertical = false, reverseLayout = true),
        )

        class Config(
            val isVertical: Boolean,
            val reverseLayout: Boolean
        ) {
            override fun toString() =
                (if (isVertical) "LazyVerticalGrid" else "LazyHorizontalGrid") +
                    (if (reverseLayout) "(reverse)" else "")
        }
    }
}

private val FrameDuration = 16L
private val Duration = 64L // 4 frames, so we get 0f, 0.25f, 0.5f, 0.75f and 1f fractions
private val AnimSpec = tween<IntOffset>(Duration.toInt(), easing = LinearEasing)
private val ContainerTag = "container"
private val NodesWithTagMatcher = SemanticsMatcher("NodesWithTag") {
    it.config.contains(SemanticsProperties.TestTag)
}
