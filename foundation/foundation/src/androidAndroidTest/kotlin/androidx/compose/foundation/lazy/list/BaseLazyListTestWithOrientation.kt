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

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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

open class BaseLazyListTestWithOrientation(private val orientation: Orientation) {

    @get:Rule
    val rule = createComposeRule()

    val vertical: Boolean
        get() = orientation == Orientation.Vertical

    fun Modifier.mainAxisSize(size: Dp) =
        if (vertical) {
            this.height(size)
        } else {
            this.width(size)
        }

    fun Modifier.crossAxisSize(size: Dp) =
        if (vertical) {
            this.width(size)
        } else {
            this.height(size)
        }

    fun Modifier.fillMaxCrossAxis() =
        if (vertical) {
            this.fillMaxWidth()
        } else {
            this.fillMaxHeight()
        }

    fun LazyItemScope.fillParentMaxCrossAxis() =
        if (vertical) {
            Modifier.fillParentMaxWidth()
        } else {
            Modifier.fillParentMaxHeight()
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

    fun SemanticsNodeInteraction.assertStartPositionInRootIsEqualTo(expectedStart: Dp) =
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

    fun LazyListState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    fun LazyListState.scrollTo(index: Int) {
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
    fun LazyColumnOrRow(
        modifier: Modifier = Modifier,
        state: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        spacedBy: Dp = 0.dp,
        content: LazyListScope.() -> Unit
    ) {
        if (vertical) {
            val verticalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.Bottom
                else -> Arrangement.Top
            }
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement,
                content = content
            )
        } else {
            val horizontalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.End
                else -> Arrangement.Start
            }
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement,
                content = content
            )
        }
    }
}