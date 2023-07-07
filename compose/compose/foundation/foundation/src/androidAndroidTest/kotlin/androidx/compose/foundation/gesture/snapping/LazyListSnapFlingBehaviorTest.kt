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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.list.BaseLazyListTestWithOrientation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalFoundationApi::class)
class LazyListSnapFlingBehaviorTest(private val orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    private val density: Density
        get() = rule.density

    private lateinit var snapLayoutInfoProvider: SnapLayoutInfoProvider
    private lateinit var snapFlingBehavior: FlingBehavior

    @Test
    fun belowThresholdVelocity_lessThanAnItemScroll_shouldStayInSamePage() {
        var lazyListState: LazyListState? = null
        var stepSize = 0f
        var velocityThreshold = 0f
        // arrange
        rule.setContent {
            val density = LocalDensity.current
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(density) { ItemSize.toPx() }
            velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = density.getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(stepSize / 2, velocityThreshold / 2)
        }

        // assert
        rule.runOnIdle {
            val nextItem = density.getCurrentSnappedItem(lazyListState)
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
            val density = LocalDensity.current
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(density) { ItemSize.toPx() }
            velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = density.getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                stepSize,
                velocityThreshold / 2
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = density.getCurrentSnappedItem(lazyListState)
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
            val density = LocalDensity.current
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(density) { ItemSize.toPx() }
            velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = density.getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                stepSize / 2,
                velocityThreshold * 2
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = density.getCurrentSnappedItem(lazyListState)
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
            val density = LocalDensity.current
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(density) { ItemSize.toPx() }
            velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = density.getCurrentSnappedItem(lazyListState)

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
            val nextItem = density.getCurrentSnappedItem(lazyListState)
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
            val density = LocalDensity.current
            val state = rememberLazyListState().also { lazyListState = it }
            stepSize = with(density) { ItemSize.toPx() }
            velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state)
        }

        // Scroll a bit
        onMainList().swipeOnMainAxis()
        rule.waitForIdle()
        val currentItem = density.getCurrentSnappedItem(lazyListState)

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                1.5f * stepSize,
                velocityThreshold * 3
            )
        }

        // assert
        rule.runOnIdle {
            val nextItem = density.getCurrentSnappedItem(lazyListState)
            assertEquals(currentItem + 2, nextItem)
        }
    }

    @Test
    fun performFling_shouldPropagateVelocityIfHitEdges() {
        var stepSize = 0f
        var latestAvailableVelocity = Velocity.Zero
        lateinit var lazyListState: LazyListState
        val inspectingNestedScrollConnection = object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                latestAvailableVelocity = available
                return Velocity.Zero
            }
        }

        // arrange
        rule.setContent {
            val density = LocalDensity.current
            lazyListState = rememberLazyListState(180) // almost at the end
            stepSize = with(density) { ItemSize.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(inspectingNestedScrollConnection)
            ) {
                MainLayout(state = lazyListState)
            }
        }

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                1.5f * stepSize,
                30000f
            )
        }

        // assert
        rule.runOnIdle {
            assertNotEquals(latestAvailableVelocity.toAbsoluteFloat(), 0f)
        }

        // arrange
        rule.runOnIdle {
            runBlocking {
                lazyListState.scrollToItem(20) // almost at the start
            }
        }

        latestAvailableVelocity = Velocity.Zero

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                -1.5f * stepSize,
                30000f
            )
        }

        // assert
        rule.runOnIdle {
            assertNotEquals(latestAvailableVelocity.toAbsoluteFloat(), 0f)
        }
    }

    @Test
    fun performFling_shouldConsumeAllVelocityIfInTheMiddleOfTheList() {
        var stepSize = 0f
        var latestAvailableVelocity = Velocity.Zero
        lateinit var lazyListState: LazyListState
        val inspectingNestedScrollConnection = object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                latestAvailableVelocity = available
                return Velocity.Zero
            }
        }

        // arrange
        rule.setContent {
            val density = LocalDensity.current
            lazyListState = rememberLazyListState(100) // middle of the list
            stepSize = with(density) { ItemSize.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(inspectingNestedScrollConnection)
            ) {
                MainLayout(state = lazyListState)
            }
        }

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                1.5f * stepSize,
                10000f // use a not so high velocity
            )
        }

        // assert
        rule.runOnIdle {
            assertEquals(latestAvailableVelocity.toAbsoluteFloat(), 0f)
        }

        // arrange
        rule.runOnIdle {
            runBlocking {
                lazyListState.scrollToItem(100) // return to the middle
            }
        }

        latestAvailableVelocity = Velocity.Zero

        // act
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                -1.5f * stepSize,
                10000f // use a not so high velocity
            )
        }

        // assert
        rule.runOnIdle {
            assertEquals(latestAvailableVelocity.toAbsoluteFloat(), 0f)
        }
    }

    @Test
    fun remainingScrollOffset_shouldFollowAnimationOffsets() {
        var stepSize = 0f
        var velocityThreshold = 0f
        val scrollOffset = mutableListOf<Float>()
        // arrange
        rule.setContent {
            val density = LocalDensity.current
            val state = rememberLazyListState()
            stepSize = with(density) { ItemSize.toPx() }
            velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }
            MainLayout(state = state, scrollOffset)
        }

        rule.mainClock.autoAdvance = false
        // act
        val velocity = velocityThreshold * 3
        onMainList().performTouchInput {
            swipeMainAxisWithVelocity(
                1.5f * stepSize,
                velocity
            )
        }
        rule.mainClock.advanceTimeByFrame()

        // assert
        val initialTargetOffset =
            with(snapLayoutInfoProvider) { density.calculateApproachOffset(velocity) }
        Truth.assertThat(scrollOffset.first { it != 0f }).isWithin(0.5f)
            .of(initialTargetOffset)

        // act: wait for remaining offset to grow instead of decay, this indicates the last
        // snap step will start
        rule.mainClock.advanceTimeUntil {
            scrollOffset.size > 2 &&
                scrollOffset.last() > scrollOffset[scrollOffset.lastIndex - 1]
        }

        // assert: next calculated bound is the first value emitted by remainingScrollOffset
        val bounds = with(snapLayoutInfoProvider) { density.calculateSnappingOffsetBounds() }
        val finalRemainingOffset = bounds.endInclusive
        Truth.assertThat(scrollOffset.last()).isWithin(0.5f)
            .of(finalRemainingOffset)
        rule.mainClock.autoAdvance = true

        // assert: value settles back to zero
        rule.runOnIdle {
            Truth.assertThat(scrollOffset.last()).isEqualTo(0f)
        }
    }

    private fun onMainList() = rule.onNodeWithTag(TestTag)

    @Composable
    fun MainLayout(state: LazyListState, scrollOffset: MutableList<Float> = mutableListOf()) {
        snapLayoutInfoProvider = remember(state) { SnapLayoutInfoProvider(state) }
        val innerFlingBehavior =
            rememberSnapFlingBehavior(snapLayoutInfoProvider = snapLayoutInfoProvider)
        snapFlingBehavior = remember(innerFlingBehavior) {
            QuerySnapFlingBehavior(innerFlingBehavior) {
                scrollOffset.add(it)
            }
        }
        LazyColumnOrRow(
            state = state,
            modifier = Modifier.testTag(TestTag),
            flingBehavior = snapFlingBehavior
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

    private fun Density.getCurrentSnappedItem(state: LazyListState?): Int {
        var itemIndex = -1
        if (state == null) return -1
        var minDistance = Float.POSITIVE_INFINITY
        (state.layoutInfo.visibleItemsInfo).forEach {
            val distance = calculateDistanceToDesiredSnapPosition(
                state.layoutInfo,
                it,
                CenterToCenter
            )
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

    private fun Velocity.toAbsoluteFloat(): Float {
        return (if (orientation == Orientation.Vertical) y else x).absoluteValue
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)

        val ItemSize = 200.dp
        const val TestTag = "MainList"
        val CenterToCenter: Density.(Float, Float) -> Float =
            { layoutSize, itemSize -> layoutSize / 2f - itemSize / 2f }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class QuerySnapFlingBehavior(
    val snapFlingBehavior: SnapFlingBehavior,
    val onAnimationStep: (Float) -> Unit
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return with(snapFlingBehavior) {
            performFling(initialVelocity, onAnimationStep)
        }
    }
}