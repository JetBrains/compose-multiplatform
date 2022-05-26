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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule

open class BaseLazyGridTestWithOrientation(private val orientation: Orientation) {

    @get:Rule
    val rule = createComposeRule()

    val vertical: Boolean
        get() = orientation == Orientation.Vertical

    @Stable
    fun Modifier.crossAxisSize(size: Dp) =
        if (vertical) {
            this.width(size)
        } else {
            this.height(size)
        }

    @Stable
    fun Modifier.mainAxisSize(size: Dp) =
        if (vertical) {
            this.height(size)
        } else {
            this.width(size)
        }

    @Stable
    fun Modifier.axisSize(crossAxis: Dp, mainAxis: Dp) =
        if (vertical) {
            this.size(crossAxis, mainAxis)
        } else {
            this.size(mainAxis, crossAxis)
        }

    fun SemanticsNodeInteraction.scrollMainAxisBy(distance: Dp) {
        if (vertical) {
            this.scrollBy(y = distance, density = rule.density)
        } else {
            this.scrollBy(x = distance, density = rule.density)
        }
    }

    fun SemanticsNodeInteraction.assertMainAxisSizeIsEqualTo(expectedSize: Dp) =
        if (vertical) {
            assertHeightIsEqualTo(expectedSize)
        } else {
            assertWidthIsEqualTo(expectedSize)
        }

    fun SemanticsNodeInteraction.assertCrossAxisSizeIsEqualTo(expectedSize: Dp) =
        if (vertical) {
            assertWidthIsEqualTo(expectedSize)
        } else {
            assertHeightIsEqualTo(expectedSize)
        }

    fun SemanticsNodeInteraction.assertStartPositionIsAlmost(expected: Dp) {
        val position = if (vertical) {
            getUnclippedBoundsInRoot().top
        } else {
            getUnclippedBoundsInRoot().left
        }
        position.assertIsEqualTo(expected, tolerance = 1.dp)
    }

    fun SemanticsNodeInteraction.assertMainAxisStartPositionInRootIsEqualTo(expectedStart: Dp) =
        if (vertical) {
            assertTopPositionInRootIsEqualTo(expectedStart)
        } else {
            assertLeftPositionInRootIsEqualTo(expectedStart)
        }

    fun SemanticsNodeInteraction.assertCrossAxisStartPositionInRootIsEqualTo(expectedStart: Dp) =
        if (vertical) {
            assertLeftPositionInRootIsEqualTo(expectedStart)
        } else {
            assertTopPositionInRootIsEqualTo(expectedStart)
        }

    fun PaddingValues(
        mainAxis: Dp = 0.dp,
        crossAxis: Dp = 0.dp
    ) = PaddingValues(
        beforeContent = mainAxis,
        afterContent = mainAxis,
        beforeContentCrossAxis = crossAxis,
        afterContentCrossAxis = crossAxis
    )

    fun PaddingValues(
        beforeContent: Dp = 0.dp,
        afterContent: Dp = 0.dp,
        beforeContentCrossAxis: Dp = 0.dp,
        afterContentCrossAxis: Dp = 0.dp,
    ) = if (vertical) {
        PaddingValues(
            start = beforeContentCrossAxis,
            top = beforeContent,
            end = afterContentCrossAxis,
            bottom = afterContent
        )
    } else {
        PaddingValues(
            start = beforeContent,
            top = beforeContentCrossAxis,
            end = afterContent,
            bottom = afterContentCrossAxis
        )
    }

    fun LazyGridState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    fun LazyGridState.scrollTo(index: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            scrollToItem(index)
        }
    }

    fun SemanticsNodeInteraction.scrollBy(offset: Dp) = scrollBy(
        x = if (vertical) 0.dp else offset,
        y = if (!vertical) 0.dp else offset,
        density = rule.density
    )

    @Composable
    fun LazyGrid(
        cells: Int,
        modifier: Modifier = Modifier,
        state: LazyGridState = rememberLazyGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        crossAxisSpacedBy: Dp = 0.dp,
        mainAxisSpacedBy: Dp = 0.dp,
        content: LazyGridScope.() -> Unit
    ) = LazyGrid(
        GridCells.Fixed(cells),
        modifier,
        state,
        contentPadding,
        reverseLayout,
        reverseArrangement,
        flingBehavior,
        userScrollEnabled,
        crossAxisSpacedBy,
        mainAxisSpacedBy,
        content
    )

    @Composable
    fun LazyGrid(
        cells: GridCells,
        modifier: Modifier = Modifier,
        state: LazyGridState = rememberLazyGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        crossAxisSpacedBy: Dp = 0.dp,
        mainAxisSpacedBy: Dp = 0.dp,
        content: LazyGridScope.() -> Unit
    ) {
        if (vertical) {
            val verticalArrangement = when {
                mainAxisSpacedBy != 0.dp -> Arrangement.spacedBy(mainAxisSpacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.Bottom
                else -> Arrangement.Top
            }
            val horizontalArrangement = when {
                crossAxisSpacedBy != 0.dp -> Arrangement.spacedBy(crossAxisSpacedBy)
                else -> Arrangement.Start
            }
            LazyVerticalGrid(
                columns = cells,
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement,
                content = content
            )
        } else {
            val horizontalArrangement = when {
                mainAxisSpacedBy != 0.dp -> Arrangement.spacedBy(mainAxisSpacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.End
                else -> Arrangement.Start
            }
            val verticalArrangement = when {
                crossAxisSpacedBy != 0.dp -> Arrangement.spacedBy(crossAxisSpacedBy)
                else -> Arrangement.Top
            }
            LazyHorizontalGrid(
                rows = cells,
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement,
                verticalArrangement = verticalArrangement,
                content = content
            )
        }
    }
}