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

package androidx.compose.foundation.lazy.list

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.roundToInt

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalFoundationApi::class)
class LazyListAnimateItemPlacementTest(private val config: Config) {

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
    private lateinit var state: LazyListState

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
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(0 to 0, 1 to itemSize)

        rule.runOnIdle {
            list = listOf(1, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + (itemSize * fraction).roundToInt(),
                1 to itemSize - (itemSize * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderTwoItems_layoutInfoHasFinalPositions() {
        var list by mutableStateOf(listOf(0, 1))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertLayoutInfoPositions(0 to 0, 1 to itemSize)

        rule.runOnIdle {
            list = listOf(1, 0)
        }

        onAnimationFrame {
            // fraction doesn't affect the offsets in layout info
            assertLayoutInfoPositions(1 to 0, 0 to itemSize)
        }
    }

    @Test
    fun reorderFirstAndLastItems() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to 0,
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
            4 to itemSize * 4,
        )

        rule.runOnIdle {
            list = listOf(4, 1, 2, 3, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + (itemSize * 4 * fraction).roundToInt(),
                1 to itemSize,
                2 to itemSize * 2,
                3 to itemSize * 3,
                4 to itemSize * 4 - (itemSize * 4 * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveFirstItemToEndCausingAllItemsToAnimate() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to 0,
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
            4 to itemSize * 4,
        )

        rule.runOnIdle {
            list = listOf(1, 2, 3, 4, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + (itemSize * 4 * fraction).roundToInt(),
                1 to itemSize - (itemSize * fraction).roundToInt(),
                2 to itemSize * 2 - (itemSize * fraction).roundToInt(),
                3 to itemSize * 3 - (itemSize * fraction).roundToInt(),
                4 to itemSize * 4 - (itemSize * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun itemSizeChangeAnimatesNextItems() {
        var size by mutableStateOf(itemSizeDp)
        rule.setContent {
            LazyList(
                minSize = itemSizeDp * 5,
                maxSize = itemSizeDp * 5
            ) {
                items(listOf(0, 1, 2, 3), key = { it }) {
                    Item(it, size = if (it == 1) size else itemSizeDp)
                }
            }
        }

        rule.runOnIdle {
            size = itemSizeDp * 2
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(size)

        onAnimationFrame { fraction ->
            if (!reverseLayout) {
                assertPositions(
                    0 to 0,
                    1 to itemSize,
                    2 to itemSize * 2 + (itemSize * fraction).roundToInt(),
                    3 to itemSize * 3 + (itemSize * fraction).roundToInt(),
                    fraction = fraction,
                    autoReverse = false
                )
            } else {
                assertPositions(
                    3 to itemSize - (itemSize * fraction).roundToInt(),
                    2 to itemSize * 2 - (itemSize * fraction).roundToInt(),
                    1 to itemSize * 3 - (itemSize * fraction).roundToInt(),
                    0 to itemSize * 4,
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
            LazyList {
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
                0 to itemSize * 4,
                1 to itemSize - (itemSize * fraction).roundToInt(),
                2 to itemSize,
                3 to itemSize * 3 - (itemSize * fraction).roundToInt(),
                4 to itemSize * 3,
                fraction = fraction
            )
        }
    }

    @Test
    fun animationsWithDifferentDurations() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyList {
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
                0 to 0 + (itemSize * 4 * shorterAnimFraction).roundToInt(),
                1 to itemSize - (itemSize * fraction).roundToInt(),
                2 to itemSize * 2 - (itemSize * shorterAnimFraction).roundToInt(),
                3 to itemSize * 3 - (itemSize * fraction).roundToInt(),
                4 to itemSize * 4 - (itemSize * shorterAnimFraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun multipleChildrenPerItem() {
        var list by mutableStateOf(listOf(0, 2))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                    Item(it + 1)
                }
            }
        }

        assertPositions(
            0 to 0,
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
        )

        rule.runOnIdle {
            list = listOf(2, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + (itemSize * 2 * fraction).roundToInt(),
                1 to itemSize + (itemSize * 2 * fraction).roundToInt(),
                2 to itemSize * 2 - (itemSize * 2 * fraction).roundToInt(),
                3 to itemSize * 3 - (itemSize * 2 * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun multipleChildrenPerItemSomeDoNotAnimate() {
        var list by mutableStateOf(listOf(0, 2))
        rule.setContent {
            LazyList {
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
                0 to 0 + (itemSize * 2 * fraction).roundToInt(),
                1 to itemSize * 3,
                2 to itemSize * 2 - (itemSize * 2 * fraction).roundToInt(),
                3 to itemSize,
                fraction = fraction
            )
        }
    }

    @Test
    fun animateArrangementChange() {
        var arrangement by mutableStateOf(Arrangement.Center)
        rule.setContent {
            LazyList(
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
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
        )

        rule.runOnIdle {
            arrangement = Arrangement.SpaceBetween
        }
        rule.mainClock.advanceTimeByFrame()

        onAnimationFrame { fraction ->
            assertPositions(
                1 to itemSize - (itemSize * fraction).roundToInt(),
                2 to itemSize * 2,
                3 to itemSize * 3 + (itemSize * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to 0,
            1 to itemSize,
            2 to itemSize * 2
        )

        rule.runOnIdle {
            list = listOf(0, 4, 2, 3, 1, 5)
        }

        onAnimationFrame { fraction ->
            val item1Offset = itemSize + (itemSize * 3 * fraction).roundToInt()
            val item4Offset = itemSize * 4 - (itemSize * 3 * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, Int>>().apply {
                add(0 to 0)
                if (item1Offset < itemSize * 3) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(2 to itemSize * 2)
                if (item4Offset < itemSize * 3) {
                    add(4 to item4Offset)
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
    fun moveItemToTheTopOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3f, startIndex = 3) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            3 to 0,
            4 to itemSize,
            5 to itemSize * 2
        )

        rule.runOnIdle {
            list = listOf(2, 4, 0, 3, 1, 5)
        }

        onAnimationFrame { fraction ->
            val item1Offset = itemSize * -2 + (itemSize * 3 * fraction).roundToInt()
            val item4Offset = itemSize - (itemSize * 3 * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, Int>>().apply {
                if (item4Offset > -itemSize) {
                    add(4 to item4Offset)
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
                add(3 to 0)
                if (item1Offset > -itemSize) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(5 to itemSize * 2)
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveFirstItemToEndCausingAllItemsToAnimate_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        rule.setContent {
            LazyList(arrangement = Arrangement.spacedBy(spacingDp)) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnIdle {
            list = listOf(1, 2, 3, 0)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + (itemSizePlusSpacing * 3 * fraction).roundToInt(),
                1 to itemSizePlusSpacing - (itemSizePlusSpacing * fraction).roundToInt(),
                2 to itemSizePlusSpacing * 2 - (itemSizePlusSpacing * fraction).roundToInt(),
                3 to itemSizePlusSpacing * 3 - (itemSizePlusSpacing * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(
                maxSize = itemSizeDp * 3 + spacingDp * 2,
                arrangement = Arrangement.spacedBy(spacingDp)
            ) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            0 to 0,
            1 to itemSizePlusSpacing,
            2 to itemSizePlusSpacing * 2
        )

        rule.runOnIdle {
            list = listOf(0, 4, 2, 3, 1, 5)
        }

        onAnimationFrame { fraction ->
            val item1Offset =
                itemSizePlusSpacing + (itemSizePlusSpacing * 3 * fraction).roundToInt()
            val item4Offset =
                itemSizePlusSpacing * 4 - (itemSizePlusSpacing * 3 * fraction).roundToInt()
            val screenSize = itemSize * 3 + spacing * 2
            val expected = mutableListOf<Pair<Any, Int>>().apply {
                add(0 to 0)
                if (item1Offset < screenSize) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(2 to itemSizePlusSpacing * 2)
                if (item4Offset < screenSize) {
                    add(4 to item4Offset)
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
    fun moveItemToTheTopOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        rule.setContent {
            LazyList(
                maxSize = itemSizeDp * 3 + spacingDp * 2,
                startIndex = 3,
                arrangement = Arrangement.spacedBy(spacingDp)
            ) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        assertPositions(
            3 to 0,
            4 to itemSizePlusSpacing,
            5 to itemSizePlusSpacing * 2
        )

        rule.runOnIdle {
            list = listOf(2, 4, 0, 3, 1, 5, 6, 7)
        }

        onAnimationFrame { fraction ->
            val item1Offset =
                itemSizePlusSpacing * -2 + (itemSizePlusSpacing * 3 * fraction).roundToInt()
            val item4Offset =
                (itemSizePlusSpacing - itemSizePlusSpacing * 3 * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, Int>>().apply {
                if (item4Offset > -itemSize) {
                    add(4 to item4Offset)
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
                add(3 to 0)
                if (item1Offset > -itemSize) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(5 to itemSizePlusSpacing * 2)
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds_differentSizes() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSize2Dp + itemSize3Dp + itemSizeDp, startIndex = 3) {
                items(list, key = { it }) {
                    val size =
                        if (it == 3) itemSize2Dp else if (it == 1) itemSize3Dp else itemSizeDp
                    Item(it, size = size)
                }
            }
        }

        val item3Size = itemSize2
        val item4Size = itemSize
        assertPositions(
            3 to 0,
            4 to item3Size,
            5 to item3Size + item4Size
        )

        rule.runOnIdle {
            // swap 4 and 1
            list = listOf(0, 4, 2, 3, 1, 5)
        }

        onAnimationFrame { fraction ->
            rule.onNodeWithTag("2").assertDoesNotExist()
            // item 2 was between 1 and 3 but we don't compose it and don't know the real size,
            // so we use an average size.
            val item2Size = (itemSize + itemSize2 + itemSize3) / 3
            val item1Size = itemSize3 /* the real size of the item 1 */
            val startItem1Offset = -item1Size - item2Size
            val item1Offset =
                startItem1Offset + ((itemSize2 - startItem1Offset) * fraction).roundToInt()
            val endItem4Offset = -item4Size - item2Size
            val item4Offset = item3Size - ((item3Size - endItem4Offset) * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, Int>>().apply {
                if (item4Offset > -item4Size) {
                    add(4 to item4Offset)
                } else {
                    rule.onNodeWithTag("4").assertIsNotDisplayed()
                }
                add(3 to 0)
                if (item1Offset > -item1Size) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(5 to item3Size + item4Size - ((item4Size - item1Size) * fraction).roundToInt())
            }
            assertPositions(
                expected = expected.toTypedArray(),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_differentSizes() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val listSize = itemSize2 + itemSize3 + itemSize - 1
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp) {
                items(list, key = { it }) {
                    val size =
                        if (it == 0) itemSize2Dp else if (it == 4) itemSize3Dp else itemSizeDp
                    Item(it, size = size)
                }
            }
        }

        val item0Size = itemSize2
        val item1Size = itemSize
        assertPositions(
            0 to 0,
            1 to item0Size,
            2 to item0Size + item1Size
        )

        rule.runOnIdle {
            list = listOf(0, 4, 2, 3, 1, 5)
        }

        onAnimationFrame { fraction ->
            val item2Size = itemSize
            val item4Size = itemSize3
            // item 3 was between 2 and 4 but we don't compose it and don't know the real size,
            // so we use an average size.
            val item3Size = (itemSize + itemSize2 + itemSize3) / 3
            val startItem4Offset = item0Size + item1Size + item2Size + item3Size
            val endItem1Offset = item0Size + item4Size + item2Size + item3Size
            val item1Offset =
                item0Size + ((endItem1Offset - item0Size) * fraction).roundToInt()
            val item4Offset =
                startItem4Offset - ((startItem4Offset - item0Size) * fraction).roundToInt()
            val expected = mutableListOf<Pair<Any, Int>>().apply {
                add(0 to 0)
                if (item1Offset < listSize) {
                    add(1 to item1Offset)
                } else {
                    rule.onNodeWithTag("1").assertIsNotDisplayed()
                }
                add(2 to item0Size + item1Size - ((item1Size - item4Size) * fraction).roundToInt())
                if (item4Offset < listSize) {
                    add(4 to item4Offset)
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
    fun animateAlignmentChange() {
        var alignment by mutableStateOf(CrossAxisAlignment.End)
        rule.setContent {
            LazyList(
                crossAxisAlignment = alignment,
                crossAxisSize = itemSizeDp
            ) {
                items(listOf(1, 2, 3), key = { it }) {
                    val crossAxisSize =
                        if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
                    Item(it, crossAxisSize = crossAxisSize)
                }
            }
        }

        val item2Start = itemSize - itemSize2
        val item3Start = itemSize - itemSize3
        assertPositions(
            1 to 0,
            2 to itemSize,
            3 to itemSize * 2,
            crossAxis = listOf(
                1 to 0,
                2 to item2Start,
                3 to item3Start,
            )
        )

        rule.runOnIdle {
            alignment = CrossAxisAlignment.Center
        }
        rule.mainClock.advanceTimeByFrame()

        val item2End = itemSize / 2 - itemSize2 / 2
        val item3End = itemSize / 2 - itemSize3 / 2
        onAnimationFrame { fraction ->
            assertPositions(
                1 to 0,
                2 to itemSize,
                3 to itemSize * 2,
                crossAxis = listOf(
                    1 to 0,
                    2 to item2Start + ((item2End - item2Start) * fraction).roundToInt(),
                    3 to item3Start + ((item3End - item3Start) * fraction).roundToInt(),
                ),
                fraction = fraction
            )
        }
    }

    @Test
    fun animateAlignmentChange_multipleChildrenPerItem() {
        var alignment by mutableStateOf(CrossAxisAlignment.Start)
        rule.setContent {
            LazyList(
                crossAxisAlignment = alignment,
                crossAxisSize = itemSizeDp * 2
            ) {
                items(1) {
                    listOf(1, 2, 3).forEach {
                        val crossAxisSize =
                            if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
                        Item(it, crossAxisSize = crossAxisSize)
                    }
                }
            }
        }

        rule.runOnIdle {
            alignment = CrossAxisAlignment.End
        }
        rule.mainClock.advanceTimeByFrame()

        val containerSize = itemSize * 2
        onAnimationFrame { fraction ->
            assertPositions(
                1 to 0,
                2 to itemSize,
                3 to itemSize * 2,
                crossAxis = listOf(
                    1 to ((containerSize - itemSize) * fraction).roundToInt(),
                    2 to ((containerSize - itemSize2) * fraction).roundToInt(),
                    3 to ((containerSize - itemSize3) * fraction).roundToInt()
                ),
                fraction = fraction
            )
        }
    }

    @Test
    fun animateAlignmentChange_rtl() {
        // this test is not applicable to LazyRow
        assumeTrue(isVertical)

        var alignment by mutableStateOf(CrossAxisAlignment.End)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(
                    crossAxisAlignment = alignment,
                    crossAxisSize = itemSizeDp
                ) {
                    items(listOf(1, 2, 3), key = { it }) {
                        val crossAxisSize =
                            if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
                        Item(it, crossAxisSize = crossAxisSize)
                    }
                }
            }
        }

        assertPositions(
            1 to 0,
            2 to itemSize,
            3 to itemSize * 2,
            crossAxis = listOf(
                1 to 0,
                2 to 0,
                3 to 0,
            )
        )

        rule.runOnIdle {
            alignment = CrossAxisAlignment.Center
        }
        rule.mainClock.advanceTimeByFrame()

        onAnimationFrame { fraction ->
            assertPositions(
                1 to 0,
                2 to itemSize,
                3 to itemSize * 2,
                crossAxis = listOf(
                    1 to 0,
                    2 to ((itemSize / 2 - itemSize2 / 2) * fraction).roundToInt(),
                    3 to ((itemSize / 2 - itemSize3 / 2) * fraction).roundToInt(),
                ),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToEndCausingNextItemsToAnimate_withContentPadding() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val rawStartPadding = 8
        val rawEndPadding = 12
        val (startPaddingDp, endPaddingDp) = with(rule.density) {
            rawStartPadding.toDp() to rawEndPadding.toDp()
        }
        rule.setContent {
            LazyList(startPadding = startPaddingDp, endPadding = endPaddingDp) {
                items(list, key = { it }) {
                    Item(it)
                }
            }
        }

        val startPadding = if (reverseLayout) rawEndPadding else rawStartPadding
        assertPositions(
            0 to startPadding,
            1 to startPadding + itemSize,
            2 to startPadding + itemSize * 2,
            3 to startPadding + itemSize * 3,
            4 to startPadding + itemSize * 4,
        )

        rule.runOnIdle {
            list = listOf(0, 2, 3, 4, 1)
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to startPadding,
                1 to startPadding + itemSize + (itemSize * 3 * fraction).roundToInt(),
                2 to startPadding + itemSize * 2 - (itemSize * fraction).roundToInt(),
                3 to startPadding + itemSize * 3 - (itemSize * fraction).roundToInt(),
                4 to startPadding + itemSize * 4 - (itemSize * fraction).roundToInt(),
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderFirstAndLastItems_noNewLayoutInfoProduced() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))

        var measurePasses = 0
        rule.setContent {
            LazyList {
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
            LazyList(maxSize = itemSizeDp * 3) {
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
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize * 3 / 2,
                3 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardBySmallOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSize / 2f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize * 3 / 2,
                3 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardBySmallOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3, startIndex = 2) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemSize / 2f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                1 to -itemSize / 2,
                2 to itemSize / 2,
                3 to itemSize * 3 / 2,
                4 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                2 to -itemSize / 2,
                3 to itemSize / 2,
                4 to itemSize * 3 / 2,
                5 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3, startIndex = 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemSize * 2.5f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize * 3 / 2,
                3 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset_differentSizes() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it, size = if (it % 2 == 0) itemSizeDp else itemSize2Dp)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSize + itemSize2 + itemSize / 2f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                2 to -itemSize / 2,
                3 to itemSize / 2,
                4 to itemSize2 + itemSize / 2,
                5 to itemSize2 + itemSize * 3 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset_differentSizes() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3, startIndex = 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it, size = if (it % 2 == 0) itemSizeDp else itemSize2Dp)
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-(itemSize + itemSize2 + itemSize / 2f))
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize2 + itemSize / 2,
                3 to itemSize2 + itemSize * 3 / 2,
                fraction = fraction
            )
        }
    }

    private fun assertPositions(
        vararg expected: Pair<Any, Int>,
        crossAxis: List<Pair<Any, Int>>? = null,
        fraction: Float? = null,
        autoReverse: Boolean = reverseLayout
    ) {
        with(rule.density) {
            val actual = expected.map {
                val actualOffset = rule.onNodeWithTag(it.first.toString())
                    .getUnclippedBoundsInRoot().let { bounds ->
                        val offset = if (isVertical) bounds.top else bounds.left
                        if (offset == Dp.Unspecified) Int.MIN_VALUE else offset.roundToPx()
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
                        val mainAxisSize =
                            if (isVertical) containerBounds.height else containerBounds.width
                        val mainAxisSizePx = with(rule.density) { mainAxisSize.roundToPx() }
                        list.map {
                            val itemSize = rule.onNodeWithTag(it.first.toString())
                                .getUnclippedBoundsInRoot().let { bounds ->
                                    (if (isVertical) bounds.height else bounds.width).roundToPx()
                                }
                            it.first to (mainAxisSizePx - itemSize - it.second)
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

    private fun assertLayoutInfoPositions(vararg offsets: Pair<Any, Int>) {
        rule.runOnIdle {
            assertThat(visibleItemsOffsets).isEqualTo(listOf(*offsets))
        }
    }

    private val visibleItemsOffsets: List<Pair<Any, Int>>
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
    private fun LazyList(
        arrangement: Arrangement.HorizontalOrVertical? = null,
        minSize: Dp = 0.dp,
        maxSize: Dp = containerSizeDp,
        startIndex: Int = 0,
        crossAxisSize: Dp = Dp.Unspecified,
        crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
        startPadding: Dp = 0.dp,
        endPadding: Dp = 0.dp,
        content: LazyListScope.() -> Unit
    ) {
        state = rememberLazyListState(startIndex)
        if (isVertical) {
            val verticalArrangement =
                arrangement ?: if (!reverseLayout) Arrangement.Top else Arrangement.Bottom
            val horizontalAlignment = if (crossAxisAlignment == CrossAxisAlignment.Start) {
                Alignment.Start
            } else if (crossAxisAlignment == CrossAxisAlignment.Center) {
                Alignment.CenterHorizontally
            } else {
                Alignment.End
            }
            LazyColumn(
                state = state,
                modifier = Modifier
                    .requiredHeightIn(min = minSize, max = maxSize)
                    .then(
                        if (crossAxisSize != Dp.Unspecified) {
                            Modifier.requiredWidth(crossAxisSize)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .testTag(ContainerTag),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                reverseLayout = reverseLayout,
                contentPadding = PaddingValues(top = startPadding, bottom = endPadding),
                content = content
            )
        } else {
            val horizontalArrangement =
                arrangement ?: if (!reverseLayout) Arrangement.Start else Arrangement.End
            val verticalAlignment = if (crossAxisAlignment == CrossAxisAlignment.Start) {
                Alignment.Top
            } else if (crossAxisAlignment == CrossAxisAlignment.Center) {
                Alignment.CenterVertically
            } else {
                Alignment.Bottom
            }
            LazyRow(
                state = state,
                modifier = Modifier
                    .requiredWidthIn(min = minSize, max = maxSize)
                    .then(
                        if (crossAxisSize != Dp.Unspecified) {
                            Modifier.requiredHeight(crossAxisSize)
                        } else {
                            Modifier.fillMaxHeight()
                        }
                    )
                    .testTag(ContainerTag),
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = verticalAlignment,
                reverseLayout = reverseLayout,
                contentPadding = PaddingValues(start = startPadding, end = endPadding),
                content = content
            )
        }
    }

    @Composable
    private fun LazyItemScope.Item(
        tag: Int,
        size: Dp = itemSizeDp,
        crossAxisSize: Dp = size,
        animSpec: FiniteAnimationSpec<IntOffset>? = AnimSpec
    ) {
        Box(
            Modifier
                .then(
                    if (isVertical) {
                        Modifier.requiredHeight(size).requiredWidth(crossAxisSize)
                    } else {
                        Modifier.requiredWidth(size).requiredHeight(crossAxisSize)
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
                (if (isVertical) "LazyColumn" else "LazyRow") +
                    (if (reverseLayout) "(reverse)" else "")
        }
    }
}

private val FrameDuration = 16L
private val Duration = 400L
private val AnimSpec = tween<IntOffset>(Duration.toInt(), easing = LinearEasing)
private val ContainerTag = "container"

private enum class CrossAxisAlignment {
    Start,
    End,
    Center
}
