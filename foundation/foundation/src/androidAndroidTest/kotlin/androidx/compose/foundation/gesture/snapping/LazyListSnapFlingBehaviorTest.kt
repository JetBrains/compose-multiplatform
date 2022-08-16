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

package androidx.compose.foundation.gesture.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import androidx.compose.foundation.gestures.snapping.lazyListSnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.list.BaseLazyListTestWithOrientation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.math.abs
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalFoundationApi::class)
class LazyListSnapFlingBehaviorTest(private val orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    @Test
    fun belowThresholdVelocity_lessThanAnItemScroll_shouldStayInSamePage() {
        var lazyListState: LazyListState? = null
        var stepSize = 0f
        var velocityThreshold = 0f

        // arrange
        rule.setContent {
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(LocalDensity.current) { ItemSize.toPx() }
            velocityThreshold = with(LocalDensity.current) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(stepSize / 2, velocityThreshold / 2)
        }

        // assert
        rule.runOnIdle {
            val nextItem = getCurrentSnappedItem(lazyListState)
            assertEquals(currentItem, nextItem)
        }
    }

    @Test
    fun belowThresholdVelocity_moreThanAnItemScroll_shouldGoToNextPage() {
        var lazyListState: LazyListState? = null
        var stepSize = 0f
        var velocityThreshold = 0f

        // arrange
        rule.setContent {
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(LocalDensity.current) { ItemSize.toPx() }
            velocityThreshold = with(LocalDensity.current) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                stepSize,
                velocityThreshold / 2
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = getCurrentSnappedItem(lazyListState)
            assertEquals(currentItem + 1, nextItem)
        }
    }

    @Test
    fun aboveThresholdVelocityForward_notLargeEnoughScroll_shouldGoToNextPage() {
        var lazyListState: LazyListState? = null
        var stepSize = 0f
        var velocityThreshold = 0f

        // arrange
        rule.setContent {
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(LocalDensity.current) { ItemSize.toPx() }
            velocityThreshold = with(LocalDensity.current) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                stepSize / 2,
                velocityThreshold * 2
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = getCurrentSnappedItem(lazyListState)
            assertEquals(currentItem + 1, nextItem)
        }
    }

    @Test
    fun aboveThresholdVelocityBackward_notLargeEnoughScroll_shouldGoToPreviousPage() {
        var lazyListState: LazyListState? = null
        var stepSize = 0f
        var velocityThreshold = 0f

        // arrange
        rule.setContent {
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(LocalDensity.current) { ItemSize.toPx() }
            velocityThreshold = with(LocalDensity.current) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                stepSize / 2,
                velocityThreshold * 2,
                true
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = getCurrentSnappedItem(lazyListState)
            assertEquals(currentItem - 1, nextItem)
        }
    }

    @Test
    fun aboveThresholdVelocity_largeEnoughScroll_shouldGoToNextNextPage() {
        var lazyListState: LazyListState? = null
        var stepSize = 0f
        var velocityThreshold = 0f

        // arrange
        rule.setContent {
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(LocalDensity.current) { ItemSize.toPx() }
            velocityThreshold = with(LocalDensity.current) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                1.5f * stepSize,
                velocityThreshold * 3
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = getCurrentSnappedItem(lazyListState)
            assertEquals(currentItem + 2, nextItem)
        }
    }

    private fun onMainList() = rule.onNodeWithTag(TestTag)

    @Composable
    fun MainLayout(state: LazyListState) {
        val layoutInfoProvider = remember(state) { lazyListSnapLayoutInfoProvider(state) }

        LazyColumnOrRow(
            state = state,
            modifier = Modifier.testTag(TestTag),
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfoProvider)
        ) {
            items(200) {
                Box(modifier = Modifier.size(ItemSize))
            }
        }
    }

    private fun SemanticsNodeInteraction.swipeOnMainAxis() {
        performTouchInput {
            if (orientation == Orientation.Vertical) {
                swipeUp()
            } else {
                swipeLeft()
            }
        }
    }

    private fun getCurrentSnappedItem(state: LazyListState?): Int {
        var itemIndex = -1
        if (state == null) return -1
        var minDistance = Float.POSITIVE_INFINITY
        (state.layoutInfo.visibleItemsInfo).forEach {
            val distance =
                calculateDistanceToDesiredSnapPosition(state.layoutInfo, it, CenterToCenter)
            if (abs(distance) < minDistance) {
                minDistance = abs(distance)
                itemIndex = it.index
            }
        }
        return itemIndex
    }

    private fun TouchInjectionScope.swipeMainAxisWithVelocity(
        scrollSize: Float,
        endVelocity: Float,
        reversed: Boolean = false
    ) {
        val (start, end) = if (orientation == Orientation.Vertical) {
            bottomCenter to bottomCenter.copy(y = bottomCenter.y - scrollSize)
        } else {
            centerRight to centerRight.copy(x = centerRight.x - scrollSize)
        }
        swipeWithVelocity(
            if (reversed) end else start,
            if (reversed) start else end,
            endVelocity
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)

        val ItemSize = 200.dp
        const val TestTag = "MainList"
        val CenterToCenter: (Float, Float) -> Float =
            { layoutSize, itemSize -> layoutSize / 2f - itemSize / 2f }
    }
}