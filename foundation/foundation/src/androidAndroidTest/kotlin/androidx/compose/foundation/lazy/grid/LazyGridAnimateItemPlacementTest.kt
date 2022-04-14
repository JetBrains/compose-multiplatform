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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.width
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runners.Parameterized
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyGridAnimateItemPlacementTest(private val config: Config) {

    private val isVertical: Boolean get() = config.isVertical
    private val reverseLayout: Boolean get() = config.reverseLayout

    @get:Rule
    val rule = createComposeRule()

    private val itemSize: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private val itemSize2: Int = 30
    private var itemSize2Dp: Dp = Dp.Infinity
    private val itemSize3: Int = 20
    private var itemSize3Dp: Dp = Dp.Infinity
    private val containerSize: Int = itemSize * 5
    private var containerSizeDp: Dp = Dp.Infinity
    private val spacing: Int = 10
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
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(0, itemSize)
        )

        rule.runOnIdle {
            list = listOf(1, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, 0 + (itemSize * fraction).roundToInt()),
                1 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
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
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(itemSize, 0),
            2 to AxisIntOffset(0, itemSize),
            3 to AxisIntOffset(itemSize, itemSize)
        )

        rule.runOnIdle {
            list = listOf(3, 2, 1, 0)
        }

        onAnimationFrame { fraction ->
            val increasing = 0 + (itemSize * fraction).roundToInt()
            val decreasing = itemSize - (itemSize * fraction).roundToInt()
            assertPositions(
                0 to AxisIntOffset(increasing, increasing),
                1 to AxisIntOffset(decreasing, increasing),
                2 to AxisIntOffset(increasing, decreasing),
                3 to AxisIntOffset(decreasing, decreasing),
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
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(itemSize, 0),
            2 to AxisIntOffset(0, itemSize),
            3 to AxisIntOffset(itemSize, itemSize)
        )

        rule.runOnIdle {
            list = listOf(3, 2, 1, 0)
        }

        onAnimationFrame {
            // fraction doesn't affect the offsets in layout info
            assertLayoutInfoPositions(
                3 to AxisIntOffset(0, 0),
                2 to AxisIntOffset(itemSize, 0),
                1 to AxisIntOffset(0, itemSize),
                0 to AxisIntOffset(itemSize, itemSize)
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
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(0, itemSize),
            2 to AxisIntOffset(0, itemSize * 2),
            3 to AxisIntOffset(0, itemSize * 3),
            4 to AxisIntOffset(0, itemSize * 4)
        )

        rule.runOnIdle {
            list = listOf(4, 1, 2, 3, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, 0 + (itemSize * 4 * fraction).roundToInt()),
                1 to AxisIntOffset(0, itemSize),
                2 to AxisIntOffset(0, itemSize * 2),
                3 to AxisIntOffset(0, itemSize * 3),
                4 to AxisIntOffset(0, itemSize * 4 - (itemSize * 4 * fraction).roundToInt()),
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
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(itemSize, 0),
            2 to AxisIntOffset(0, itemSize),
            3 to AxisIntOffset(itemSize, itemSize),
            4 to AxisIntOffset(0, itemSize * 2),
            5 to AxisIntOffset(itemSize, itemSize * 2)
        )

        rule.runOnIdle {
            list = listOf(1, 2, 3, 4, 5, 0)
        }

        onAnimationFrame { fraction ->
            val increasingX = 0 + (itemSize * fraction).roundToInt()
            val decreasingX = itemSize - (itemSize * fraction).roundToInt()
            assertPositions(
                0 to AxisIntOffset(increasingX, 0 + (itemSize * 2 * fraction).roundToInt()),
                1 to AxisIntOffset(decreasingX, 0),
                2 to AxisIntOffset(increasingX, itemSize - (itemSize * fraction).roundToInt()),
                3 to AxisIntOffset(decreasingX, itemSize),
                4 to AxisIntOffset(increasingX, itemSize * 2 - (itemSize * fraction).roundToInt()),
                5 to AxisIntOffset(decreasingX, itemSize * 2),
                fraction = fraction
            )
        }
    }

    @Test
    fun itemSizeChangeAnimatesNextItems() {
        var height by mutableStateOf(itemSizeDp)
        rule.setContent {
            LazyGrid(1, minSize = itemSizeDp * 5, maxSize = itemSizeDp * 5) {
                items(listOf(0, 1, 2, 3), key = { it }) {
                    Item(it, height = if (it == 1) height else itemSizeDp)
                }
            }
        }

        rule.runOnIdle {
            height = itemSizeDp * 2
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(height)

        onAnimationFrame { fraction ->
            if (!reverseLayout) {
                assertPositions(
                    0 to AxisIntOffset(0, 0),
                    1 to AxisIntOffset(0, itemSize),
                    2 to AxisIntOffset(0, itemSize * 2 + (itemSize * fraction).roundToInt()),
                    3 to AxisIntOffset(0, itemSize * 3 + (itemSize * fraction).roundToInt()),
                    fraction = fraction,
                    autoReverse = false
                )
            } else {
                assertPositions(
                    3 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
                    2 to AxisIntOffset(0, itemSize * 2 - (itemSize * fraction).roundToInt()),
                    1 to AxisIntOffset(0, itemSize * 3 - (itemSize * fraction).roundToInt()),
                    0 to AxisIntOffset(0, itemSize * 4),
                    fraction = fraction,
                    autoReverse = false
                )
            }
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

        rule.runOnIdle {
            list = listOf(1, 2, 3, 4, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, itemSize * 4),
                1 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
                2 to AxisIntOffset(0, itemSize),
                3 to AxisIntOffset(0, itemSize * 3 - (itemSize * fraction).roundToInt()),
                4 to AxisIntOffset(0, itemSize * 3),
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

        rule.runOnIdle {
            list = listOf(1, 2, 3, 4, 0)
        }

        onAnimationFrame(duration = Duration * 2) { fraction ->
            val shorterAnimFraction = (fraction * 2).coerceAtMost(1f)
            assertPositions(
                0 to AxisIntOffset(0, 0 + (itemSize * 4 * shorterAnimFraction).roundToInt()),
                1 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
                2 to AxisIntOffset(0, itemSize * 2 - (itemSize * shorterAnimFraction).roundToInt()),
                3 to AxisIntOffset(0, itemSize * 3 - (itemSize * fraction).roundToInt()),
                4 to AxisIntOffset(0, itemSize * 4 - (itemSize * shorterAnimFraction).roundToInt()),
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
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(0, 0),
            2 to AxisIntOffset(0, itemSize),
            3 to AxisIntOffset(0, itemSize)
        )

        rule.runOnIdle {
            list = listOf(2, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, 0 + (itemSize * fraction).roundToInt()),
                1 to AxisIntOffset(0, 0 + (itemSize * fraction).roundToInt()),
                2 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
                3 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
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

        rule.runOnIdle {
            list = listOf(2, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, 0 + (itemSize * fraction).roundToInt()),
                1 to AxisIntOffset(0, itemSize),
                2 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
                3 to AxisIntOffset(0, 0),
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
            1 to AxisIntOffset(0, itemSize),
            2 to AxisIntOffset(0, itemSize * 2),
            3 to AxisIntOffset(0, itemSize * 3),
        )

        rule.runOnIdle {
            arrangement = Arrangement.SpaceBetween
        }
        rule.mainClock.advanceTimeByFrame()

        onAnimationFrame { fraction ->
            assertPositions(
                1 to AxisIntOffset(0, itemSize - (itemSize * fraction).roundToInt()),
                2 to AxisIntOffset(0, itemSize * 2),
                3 to AxisIntOffset(0, itemSize * 3 + (itemSize * fraction).roundToInt()),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        rule.setContent {
            LazyGrid(2, maxSize = itemSizeDp * 3) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(itemSize, 0),
            2 to AxisIntOffset(0, itemSize),
            3 to AxisIntOffset(itemSize, itemSize),
            4 to AxisIntOffset(0, itemSize * 2),
            5 to AxisIntOffset(itemSize, itemSize * 2)
        )

        rule.runOnIdle {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            val item1Offset = AxisIntOffset(itemSize, 0 + (itemSize * 4 * fraction).roundToInt())
            val item8Offset =
                AxisIntOffset(itemSize, itemSize * 4 - (itemSize * 4 * fraction).roundToInt())
            val expected = mutableListOf<Pair<Any, IntOffset>>().apply {
                add(0 to AxisIntOffset(0, 0))
                if (item1Offset.mainAxis < itemSize * 3) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(2 to AxisIntOffset(0, itemSize))
                add(3 to AxisIntOffset(itemSize, itemSize))
                add(4 to AxisIntOffset(0, itemSize * 2))
                add(5 to AxisIntOffset(itemSize, itemSize * 2))
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
            6 to AxisIntOffset(0, 0),
            7 to AxisIntOffset(itemSize, 0),
            8 to AxisIntOffset(0, itemSize),
            9 to AxisIntOffset(itemSize, itemSize),
            10 to AxisIntOffset(0, itemSize * 2),
            11 to AxisIntOffset(itemSize, itemSize * 2)
        )

        rule.runOnIdle {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            val item8Offset = AxisIntOffset(0, itemSize - (itemSize * 4 * fraction).roundToInt())
            val item1Offset = AxisIntOffset(
                0,
                itemSize * -3 + (itemSize * 4 * fraction).roundToInt()
            )
            val expected = mutableListOf<Pair<Any, IntOffset>>().apply {
                if (item1Offset.mainAxis > -itemSize) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(6 to AxisIntOffset(0, 0))
                add(7 to AxisIntOffset(itemSize, 0))
                if (item8Offset.mainAxis > -itemSize) {
                    add(8 to item8Offset)
                } else {
                    rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
                add(9 to AxisIntOffset(itemSize, itemSize))
                add(10 to AxisIntOffset(0, itemSize * 2))
                add(11 to AxisIntOffset(itemSize, itemSize * 2))
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

        rule.runOnIdle {
            list = listOf(1, 2, 3, 4, 5, 6, 7, 0)
        }

        onAnimationFrame { fraction ->
            val increasingX = (fraction * itemSize).roundToInt()
            val decreasingX = (itemSize - itemSize * fraction).roundToInt()
            assertPositions(
                0 to AxisIntOffset(increasingX, (itemSizePlusSpacing * 3 * fraction).roundToInt()),
                1 to AxisIntOffset(decreasingX, 0),
                2 to AxisIntOffset(
                    increasingX,
                    itemSizePlusSpacing - (itemSizePlusSpacing * fraction).roundToInt()
                ),
                3 to AxisIntOffset(decreasingX, itemSizePlusSpacing),
                4 to AxisIntOffset(
                    increasingX,
                    itemSizePlusSpacing * 2 - (itemSizePlusSpacing * fraction).roundToInt()
                ),
                5 to AxisIntOffset(decreasingX, itemSizePlusSpacing * 2),
                6 to AxisIntOffset(
                    increasingX,
                    itemSizePlusSpacing * 3 - (itemSizePlusSpacing * fraction).roundToInt()
                ),
                7 to AxisIntOffset(decreasingX, itemSizePlusSpacing * 3),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        rule.setContent {
            LazyGrid(
                2,
                maxSize = itemSizeDp * 3 + spacingDp * 2,
                arrangement = Arrangement.spacedBy(spacingDp)
            ) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(itemSize, 0),
            2 to AxisIntOffset(0, itemSizePlusSpacing),
            3 to AxisIntOffset(itemSize, itemSizePlusSpacing),
            4 to AxisIntOffset(0, itemSizePlusSpacing * 2),
            5 to AxisIntOffset(itemSize, itemSizePlusSpacing * 2)
        )

        rule.runOnIdle {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9)
        }

        onAnimationFrame { fraction ->
            val item1Offset = AxisIntOffset(
                itemSize,
                (itemSizePlusSpacing * 4 * fraction).roundToInt()
            )
            val item8Offset = AxisIntOffset(
                itemSize,
                itemSizePlusSpacing * 4 - (itemSizePlusSpacing * 4 * fraction).roundToInt()
            )
            val screenSize = itemSize * 3 + spacing * 2
            val expected = mutableListOf<Pair<Any, IntOffset>>().apply {
                add(0 to AxisIntOffset(0, 0))
                if (item1Offset.mainAxis < screenSize) {
                    add(1 to item1Offset)
                }
                add(2 to AxisIntOffset(0, itemSizePlusSpacing))
                add(3 to AxisIntOffset(itemSize, itemSizePlusSpacing))
                add(4 to AxisIntOffset(0, itemSizePlusSpacing * 2))
                add(5 to AxisIntOffset(itemSize, itemSizePlusSpacing * 2))
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
            4 to AxisIntOffset(0, 0),
            5 to AxisIntOffset(itemSize, 0),
            6 to AxisIntOffset(0, itemSizePlusSpacing),
            7 to AxisIntOffset(itemSize, itemSizePlusSpacing),
            8 to AxisIntOffset(0, itemSizePlusSpacing * 2),
            9 to AxisIntOffset(itemSize, itemSizePlusSpacing * 2)
        )

        rule.runOnIdle {
            list = listOf(0, 8, 2, 3, 4, 5, 6, 7, 1, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            val item1Offset = AxisIntOffset(
                0,
                itemSizePlusSpacing * -2 + (itemSizePlusSpacing * 4 * fraction).roundToInt()
            )
            val item8Offset = AxisIntOffset(
                0,
                itemSizePlusSpacing * 2 - (itemSizePlusSpacing * 4 * fraction).roundToInt()
            )
            val expected = mutableListOf<Pair<Any, IntOffset>>().apply {
                if (item1Offset.mainAxis > -itemSize) {
                    add(1 to item1Offset)
                }
                add(4 to AxisIntOffset(0, 0))
                add(5 to AxisIntOffset(itemSize, 0))
                add(6 to AxisIntOffset(0, itemSizePlusSpacing))
                add(7 to AxisIntOffset(itemSize, itemSizePlusSpacing))
                if (item8Offset.mainAxis > -itemSize) {
                    add(8 to item8Offset)
                }
                add(9 to AxisIntOffset(itemSize, itemSizePlusSpacing * 2))
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
                    Item(it, height = height)
                }
            }
        }

        val line3Size = itemSize2
        val line4Size = itemSize
        assertPositions(
            6 to AxisIntOffset(0, 0),
            7 to AxisIntOffset(itemSize, 0),
            8 to AxisIntOffset(0, line3Size),
            9 to AxisIntOffset(itemSize, line3Size),
            10 to AxisIntOffset(0, line3Size + line4Size),
            11 to AxisIntOffset(itemSize, line3Size + line4Size)
        )

        rule.runOnIdle {
            // swap 8 and 2
            list = listOf(0, 1, 8, 3, 4, 5, 6, 7, 2, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            rule.onNodeWithTag("4").assertDoesNotExist()
            rule.onNodeWithTag("5").assertDoesNotExist()
            // items 4,5 were between lines 1 and 3 but we don't compose them and don't know the
            // real size, so we use an average size.
            val line2Size = (itemSize + itemSize2 + itemSize3) / 3
            val line1Size = itemSize3 /* the real size of the item 2 */
            val startItem2Offset = -line1Size - line2Size
            val item2Offset =
                startItem2Offset + ((itemSize2 - startItem2Offset) * fraction).roundToInt()
            val endItem8Offset = -line2Size - itemSize
            val item8Offset = line3Size - ((line3Size - endItem8Offset) * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, IntOffset>>().apply {
                if (item8Offset > -line4Size) {
                    add(8 to AxisIntOffset(0, item8Offset))
                } else {
                    rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
                add(6 to AxisIntOffset(0, 0))
                add(7 to AxisIntOffset(itemSize, 0))
                if (item2Offset > -line1Size) {
                    add(2 to AxisIntOffset(0, item2Offset))
                } else {
                    rule.onNodeWithTag("2").assertIsNotDisplayed()
                }
                add(9 to AxisIntOffset(itemSize, line3Size))
                add(10 to AxisIntOffset(
                    0,
                    line3Size + line4Size - ((itemSize - itemSize3) * fraction).roundToInt()
                ))
                add(11 to AxisIntOffset(
                    itemSize,
                    line3Size + line4Size - ((itemSize - itemSize3) * fraction).roundToInt()
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
        val gridSize = itemSize2 + itemSize3 + itemSize - 1
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
                    Item(it, height = height)
                }
            }
        }

        val line0Size = itemSize2
        val line1Size = itemSize
        assertPositions(
            0 to AxisIntOffset(0, 0),
            1 to AxisIntOffset(itemSize, 0),
            2 to AxisIntOffset(0, line0Size),
            3 to AxisIntOffset(itemSize, line0Size),
            4 to AxisIntOffset(0, line0Size + line1Size),
            5 to AxisIntOffset(itemSize, line0Size + line1Size),
        )

        rule.runOnIdle {
            list = listOf(0, 1, 8, 3, 4, 5, 6, 7, 2, 9, 10, 11)
        }

        onAnimationFrame { fraction ->
            val line2Size = itemSize
            val line4Size = itemSize3
            // line 3 was between 2 and 4 but we don't compose it and don't know the real size,
            // so we use an average size.
            val line3Size = (itemSize + itemSize2 + itemSize3) / 3
            val startItem8Offset = line0Size + line1Size + line2Size + line3Size
            val endItem2Offset = line0Size + line4Size + line2Size + line3Size
            val item2Offset =
                line0Size + ((endItem2Offset - line0Size) * fraction).roundToInt()
            val item8Offset =
                startItem8Offset - ((startItem8Offset - line0Size) * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, IntOffset>>().apply {
                add(0 to AxisIntOffset(0, 0))
                add(1 to AxisIntOffset(itemSize, 0))
                if (item8Offset < gridSize) {
                    add(8 to AxisIntOffset(0, item8Offset))
                } else {
                    // rule.onNodeWithTag("8").assertIsNotDisplayed()
                }
                add(3 to AxisIntOffset(itemSize, line0Size))
                add(4 to AxisIntOffset(
                    0,
                    line0Size + line1Size - ((line1Size - line4Size) * fraction).roundToInt()
                ))
                add(5 to AxisIntOffset(
                    itemSize,
                    line0Size + line1Size - ((line1Size - line4Size) * fraction).roundToInt()
                ))
                if (item2Offset < gridSize) {
                    add(2 to AxisIntOffset(0, item2Offset))
                } else {
                    // rule.onNodeWithTag("2").assertIsNotDisplayed()
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
    //     rule.setContent {
    //         LazyList(
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

    //     rule.runOnIdle {
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
    //                 2 to item2Start + ((item2End - item2Start) * fraction).roundToInt(),
    //                 3 to item3Start + ((item3End - item3Start) * fraction).roundToInt(),
    //             ),
    //             fraction = fraction
    //         )
    //     }
    // }

    // @Test
    // fun animateAlignmentChange_multipleChildrenPerItem() {
    //     var alignment by mutableStateOf(CrossAxisAlignment.Start)
    //     rule.setContent {
    //         LazyList(
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

    //     rule.runOnIdle {
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
    //                 1 to ((containerSize - itemSize) * fraction).roundToInt(),
    //                 2 to ((containerSize - itemSize2) * fraction).roundToInt(),
    //                 3 to ((containerSize - itemSize3) * fraction).roundToInt()
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
    //     rule.setContent {
    //         CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    //             LazyList(
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

    //     rule.runOnIdle {
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
    //                 2 to ((itemSize / 2 - itemSize2 / 2) * fraction).roundToInt(),
    //                 3 to ((itemSize / 2 - itemSize3 / 2) * fraction).roundToInt(),
    //             ),
    //             fraction = fraction
    //         )
    //     }
    // }

    @Test
    fun moveItemToEndCausingNextItemsToAnimate_withContentPadding() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val rawStartPadding = 8
        val rawEndPadding = 12
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
            0 to AxisIntOffset(0, startPadding),
            1 to AxisIntOffset(0, startPadding + itemSize),
            2 to AxisIntOffset(0, startPadding + itemSize * 2),
            3 to AxisIntOffset(0, startPadding + itemSize * 3),
            4 to AxisIntOffset(0, startPadding + itemSize * 4),
        )

        rule.runOnIdle {
            list = listOf(0, 2, 3, 4, 1)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, startPadding),
                1 to AxisIntOffset(
                    0,
                    startPadding + itemSize + (itemSize * 3 * fraction).roundToInt()
                ),
                2 to AxisIntOffset(
                    0,
                    startPadding + itemSize * 2 - (itemSize * fraction).roundToInt()
                ),
                3 to AxisIntOffset(
                    0,
                    startPadding + itemSize * 3 - (itemSize * fraction).roundToInt()
                ),
                4 to AxisIntOffset(
                    0,
                    startPadding + itemSize * 4 - (itemSize * fraction).roundToInt()
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

        rule.runOnIdle {
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
    fun noAnimationWhenScrollOtherPosition() {
        rule.setContent {
            LazyGrid(1, maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(0, itemSize / 2)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to AxisIntOffset(0, -itemSize / 2),
                1 to AxisIntOffset(0, itemSize / 2),
                2 to AxisIntOffset(0, itemSize * 3 / 2),
                3 to AxisIntOffset(0, itemSize * 5 / 2),
                fraction = fraction
            )
        }
    }

    private fun AxisIntOffset(crossAxis: Int, mainAxis: Int) =
        if (isVertical) IntOffset(crossAxis, mainAxis) else IntOffset(mainAxis, crossAxis)

    private val IntOffset.mainAxis: Int get() = if (isVertical) y else x

    private fun assertPositions(
        vararg expected: Pair<Any, IntOffset>,
        crossAxis: List<Pair<Any, Int>>? = null,
        fraction: Float? = null,
        autoReverse: Boolean = reverseLayout
    ) {
        with(rule.density) {
            val actual = expected.map {
                val actualOffset = rule.onNodeWithTag(it.first.toString())
                    .getUnclippedBoundsInRoot().let { bounds ->
                        IntOffset(
                            if (bounds.left.isSpecified) bounds.left.roundToPx() else Int.MIN_VALUE,
                            if (bounds.top.isSpecified) bounds.top.roundToPx() else Int.MIN_VALUE
                        )
                    }
                it.first to actualOffset
            }
            val subject = if (fraction == null) {
                assertThat(actual)
            } else {
                assertWithMessage("Fraction=$fraction").that(actual)
            }
            subject.isEqualTo(
                listOf(*expected).let { list ->
                    if (!autoReverse) {
                        list
                    } else {
                        val containerBounds = rule.onNodeWithTag(ContainerTag).getBoundsInRoot()
                        val containerSize = with(rule.density) {
                            IntSize(
                                containerBounds.width.roundToPx(),
                                containerBounds.height.roundToPx()
                            )
                        }
                        list.map {
                            val itemSize = rule.onNodeWithTag(it.first.toString())
                                .getUnclippedBoundsInRoot().let {
                                    IntSize(it.width.roundToPx(), it.height.roundToPx())
                                }
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
                    val actualOffset = rule.onNodeWithTag(it.first.toString())
                        .getUnclippedBoundsInRoot().let { bounds ->
                            val offset = if (isVertical) bounds.left else bounds.top
                            if (offset == Dp.Unspecified) Int.MIN_VALUE else offset.roundToPx()
                        }
                    it.first to actualOffset
                }
                assertWithMessage(
                    "CrossAxis" + if (fraction != null) "for fraction=$fraction" else ""
                )
                    .that(actualCross)
                    .isEqualTo(crossAxis)
            }
        }
    }

    private fun assertLayoutInfoPositions(vararg offsets: Pair<Any, IntOffset>) {
        rule.runOnIdle {
            assertThat(visibleItemsOffsets).isEqualTo(listOf(*offsets))
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
            onFrame(i / duration.toFloat())
            rule.mainClock.advanceTimeBy(FrameDuration)
            expectedTime += FrameDuration
            assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            rule.waitForIdle()
        }
    }

    @Composable
    private fun LazyGrid(
        columns: Int,
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
                GridCells.Fixed(columns),
                Modifier
                    .requiredHeightIn(minSize, maxSize)
                    .requiredWidth(itemSizeDp * columns)
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
                GridCells.Fixed(columns),
                Modifier
                    .requiredWidthIn(minSize, maxSize)
                    .requiredHeight(itemSizeDp * columns)
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
        height: Dp = itemSizeDp,
        animSpec: FiniteAnimationSpec<IntOffset>? = AnimSpec
    ) {
        Box(
            Modifier
                .then(
                    if (isVertical) {
                        Modifier.requiredHeight(height)
                    } else {
                        Modifier.requiredWidth(height)
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
private val Duration = 400L
private val AnimSpec = tween<IntOffset>(Duration.toInt(), easing = LinearEasing)
private val ContainerTag = "container"
