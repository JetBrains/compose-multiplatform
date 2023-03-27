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

package androidx.compose.material.pullrefresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.pow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class PullRefreshStateTest {

    @get:Rule
    val rule = createComposeRule()

    private val pullRefreshNode = rule.onNodeWithTag(PullRefreshTag)

    @Test
    fun pullBeyondThreshold_triggersRefresh() {

        var refreshCount = 0
        var touchSlop = 0f
        val threshold = 400f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            val state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        // Account for PullModifier - pull down twice the threshold value.
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop + 1f) }

        rule.runOnIdle { assertThat(refreshCount).isEqualTo(1) }
    }

    @Test
    fun pullLessThanOrEqualToThreshold_doesNot_triggerRefresh() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        var touchSlop = 0f
        val threshold = 400f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        // Account for PullModifier - pull down twice the threshold value.

        // Less than threshold
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop - 1f) }

        rule.waitForIdle()

        // Equal to threshold
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop) }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(0)
            // Since onRefresh was not called, we should reset the position back to 0
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(0f)
        }
    }

    @Test
    fun progressAndPosition_scaleCorrectly_untilThreshold() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = {
                    state.setRefreshing(true)
                    refreshCount++
                    state.setRefreshing(false)
                },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(threshold)

        rule.runOnIdle {
            val adjustedDistancePulled = threshold / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(0.5f)
            assertThat(state.position).isEqualTo(adjustedDistancePulled)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onPull(threshold + 1f)

        rule.runOnIdle {
            val adjustedDistancePulled = (2 * threshold + 1f) / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(adjustedDistancePulled / threshold)
            assertThat(state.position).isEqualTo(
                calculateIndicatorPosition(adjustedDistancePulled, threshold)
            )
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onRelease(0f)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun progressAndPosition_scaleCorrectly_beyondThreshold() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(2 * threshold)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(1f)
            assertThat(state.position).isEqualTo(threshold) // Account for PullMultiplier.
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onPull(threshold)

        rule.runOnIdle {
            val adjustedDistancePulled = 3 * threshold / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(1.5f)
            assertThat(state.position).isEqualTo(
                calculateIndicatorPosition(adjustedDistancePulled, threshold)
            )
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onRelease(0f)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun positionIsCapped() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(10 * threshold)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(5f) // Account for PullMultiplier.
            // Indicator position is capped to 2 times the refresh threshold.
            assertThat(state.position).isEqualTo(2 * threshold)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onRelease(0f)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun pullInterrupted() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f
        val refreshingOffset = 200f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(threshold)

        rule.runOnIdle {
            val adjustedDistancePulled = threshold / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(0.5f)
            assertThat(state.position).isEqualTo(adjustedDistancePulled)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.setRefreshing(true)

        val consumed = state.onPull(threshold)

        rule.runOnIdle {
            assertThat(consumed).isEqualTo(0f)
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(refreshingOffset)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.setRefreshing(false)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(0)
        }
    }

    @Test
    fun pullBeyondThreshold_refreshingNotChangedToTrue_animatePositionBackToZero() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        var touchSlop = 0f
        val threshold = 400f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        // Account for PullModifier - pull down twice the threshold value.
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop + 1f) }

        rule.runOnIdle {
            // onRefresh should be called
            assertThat(refreshCount).isEqualTo(1)
            // Since onRefresh did not change refreshing, we should have reset the position back to
            // 0
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(0f)
        }
    }

    @Test
    fun thresholdAndRefreshingOffsetUpdated() {
        val initialThreshold = 800f
        val newThreshold = 1600f
        val initialOffset = 400f
        val newOffset = 800f
        lateinit var state: PullRefreshState
        val refreshingOffset = mutableStateOf(initialOffset)
        val refreshThreshold = mutableStateOf(initialThreshold)

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = { },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.value.toDp() },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.value.toDp() }
            )
        }

        rule.runOnIdle {
            assertThat(state.threshold).isEqualTo(initialThreshold)
            assertThat(state.position).isEqualTo(initialOffset)
            refreshThreshold.value = newThreshold
            refreshingOffset.value = newOffset
        }

        rule.runOnIdle {
            assertThat(state.threshold).isEqualTo(newThreshold)
            assertThat(state.position).isEqualTo(newOffset)
        }
    }

    @Test
    fun nestedPreScroll_negativeDelta_notRefreshing() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels up
        val dragUpOffset = Offset(0f, -100f)

        rule.runOnIdle {
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // Pull refresh is not showing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state by a bit
        state.onPull(200f)

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(100f /* 200 / 2 for drag multiplier */)
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // Pull refresh is currently showing, so we should consume all the delta
            assertThat(preConsumed).isEqualTo(dragUpOffset)
            assertThat(state.position).isEqualTo(50f /* (200 - 100) / 2 for drag multiplier */)
        }
    }

    @Test
    fun nestedPreScroll_negativeDelta_refreshing() {
        val refreshingOffset = 500f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = { },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels up
        val dragUpOffset = Offset(0f, -100f)

        rule.runOnIdle {
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // Pull refresh is refreshing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(refreshingOffset)
        }
    }

    @Test
    fun nestedPreScroll_positiveDelta_notRefreshing() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels down
        val dragUpOffset = Offset(0f, 100f)

        rule.runOnIdle {
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // We should ignore positive delta in prescroll, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state by a bit
        state.onPull(200f)

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(100f /* 200 / 2 for drag multiplier */)
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // We should ignore positive delta in prescroll, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(100f /* 200 / 2 for drag multiplier */)
        }
    }

    @Test
    fun nestedPreScroll_positiveDelta_refreshing() {
        val refreshingOffset = 500f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = { },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels down
        val dragUpOffset = Offset(0f, 100f)

        rule.runOnIdle {
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // Pull refresh is refreshing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(refreshingOffset)
        }
    }

    @Test
    fun nestedPostScroll_negativeDelta_notRefreshing() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels up
        val dragUpOffset = Offset(0f, -100f)

        rule.runOnIdle {
            val postConsumed = dispatcher.dispatchPostScroll(
                Offset.Zero,
                dragUpOffset,
                NestedScrollSource.Drag
            )
            // We should ignore negative delta in postscroll, so we should consume nothing
            assertThat(postConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state by a bit
        state.onPull(200f)

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(100f /* 200 / 2 for drag multiplier */)
            val postConsumed = dispatcher.dispatchPostScroll(
                Offset.Zero,
                dragUpOffset,
                NestedScrollSource.Drag
            )
            // We should ignore negative delta in postscroll, so we should consume nothing
            assertThat(postConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(100f /* 200 / 2 for drag multiplier */)
        }
    }

    @Test
    fun nestedPostScroll_negativeDelta_refreshing() {
        val refreshingOffset = 500f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = { },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels up
        val dragUpOffset = Offset(0f, -100f)

        rule.runOnIdle {
            val postConsumed = dispatcher.dispatchPostScroll(
                Offset.Zero,
                dragUpOffset,
                NestedScrollSource.Drag
            )
            // Pull refresh is refreshing, so we should consume nothing
            assertThat(postConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(refreshingOffset)
        }
    }

    @Test
    fun nestedPostScroll_positiveDelta_notRefreshing() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels down
        val dragUpOffset = Offset(0f, 100f)

        rule.runOnIdle {
            val postConsumed = dispatcher.dispatchPostScroll(
                Offset.Zero,
                dragUpOffset,
                NestedScrollSource.Drag
            )
            // We should consume all the delta
            assertThat(postConsumed).isEqualTo(dragUpOffset)
            assertThat(state.position).isEqualTo(50f /* 100 / 2 for drag multiplier */)
        }

        // Pull the state by a bit
        state.onPull(200f)

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(150f /* (100 + 200) / 2 for drag multiplier */)
            val postConsumed = dispatcher.dispatchPostScroll(
                Offset.Zero,
                dragUpOffset,
                NestedScrollSource.Drag
            )
            // We should consume all the delta again
            assertThat(postConsumed).isEqualTo(dragUpOffset)
            assertThat(state.position)
                .isEqualTo(200f /* (100 + 200 + 100) / 2 for drag multiplier */)
        }
    }

    @Test
    fun nestedPostScroll_positiveDelta_refreshing() {
        val refreshingOffset = 500f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = { },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // 100 pixels down
        val dragUpOffset = Offset(0f, 100f)

        rule.runOnIdle {
            val postConsumed = dispatcher.dispatchPostScroll(
                Offset.Zero,
                dragUpOffset,
                NestedScrollSource.Drag
            )
            // Pull refresh is refreshing, so we should consume nothing
            assertThat(postConsumed).isEqualTo(Offset.Zero)
            assertThat(state.position).isEqualTo(refreshingOffset)
        }
    }

    @Test
    fun nestedPreFling_negativeVelocity_notRefreshing() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState
        var onRefreshCalled = false

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { onRefreshCalled = true },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // Fling upwards
        val flingUp = Velocity(0f, -100f)

        rule.runOnIdle {
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingUp) }
            // Pull refresh is not showing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Velocity.Zero)
            // Not past the threshold, so we shouldn't have called onRefresh
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state but not past the threshold
        state.onPull(refreshThreshold / 2f)

        rule.runOnIdle {
            assertThat(state.position)
                .isEqualTo(refreshThreshold / 4f /* account for drag multiplier */)
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingUp) }
            // Upwards fling, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Velocity.Zero)
            // Not past the threshold, so we shouldn't have called onRefresh
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            // Indicator should be reset
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state past the threshold
        state.onPull(refreshThreshold * 3f)

        rule.runOnIdle {
            assertThat(state.position)
                .isEqualTo(calculateIndicatorPosition(
                    refreshThreshold * (3 / 2f) /* account for drag multiplier */,
                    refreshThreshold
                ))
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingUp) }
            // Upwards fling, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Velocity.Zero)
            // Past the threshold, so we should call onRefresh
            assertThat(onRefreshCalled).isTrue()
        }

        rule.runOnIdle {
            // Indicator should be reset since we never changed refreshing state
            assertThat(state.position).isEqualTo(0f)
        }
    }

    @Test
    fun nestedPreFling_negativeVelocity_refreshing() {
        val refreshingOffset = 500f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = {},
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // Fling upwards
        val flingUp = Velocity(0f, -100f)

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(refreshingOffset)
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingUp) }
            // Currently refreshing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Velocity.Zero)
        }

        rule.runOnIdle {
            // Shouldn't change position since we are refreshing
            assertThat(state.position).isEqualTo(refreshingOffset)
        }
    }

    @Test
    fun nestedPreFling_positiveVelocity_notRefreshing() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState
        var onRefreshCalled = false

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { onRefreshCalled = true },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // Fling downwards
        val flingDown = Velocity(0f, 100f)

        rule.runOnIdle {
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingDown) }
            // Pull refresh is not showing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Velocity.Zero)
            // Not past the threshold, so we shouldn't have called onRefresh
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state but not past the threshold
        state.onPull(refreshThreshold / 2f)

        rule.runOnIdle {
            assertThat(state.position)
                .isEqualTo(refreshThreshold / 4f /* account for drag multiplier */)
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingDown) }
            // Downwards fling, and we are currently showing, so we should consume all
            assertThat(preConsumed).isEqualTo(flingDown)
            // Not past the threshold, so we shouldn't have called onRefresh
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            // Indicator should be reset
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state past the threshold
        state.onPull(refreshThreshold * 3f)

        rule.runOnIdle {
            assertThat(state.position)
                .isEqualTo(calculateIndicatorPosition(
                    refreshThreshold * (3 / 2f) /* account for drag multiplier */,
                    refreshThreshold
                ))
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingDown) }
            // Downwards fling, and we are currently showing, so we should consume all
            assertThat(preConsumed).isEqualTo(flingDown)
            // Past the threshold, so we should call onRefresh
            assertThat(onRefreshCalled).isTrue()
        }

        rule.runOnIdle {
            // Indicator should be reset since we never changed refreshing state
            assertThat(state.position).isEqualTo(0f)
        }
    }

    @Test
    fun nestedPreFling_positiveVelocity_refreshing() {
        val refreshingOffset = 500f
        lateinit var state: PullRefreshState

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = {},
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // Fling downwards
        val flingUp = Velocity(0f, 100f)

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(refreshingOffset)
            val preConsumed = runBlocking { dispatcher.dispatchPreFling(flingUp) }
            // Currently refreshing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Velocity.Zero)
        }

        rule.runOnIdle {
            // Shouldn't change position since we are refreshing
            assertThat(state.position).isEqualTo(refreshingOffset)
        }
    }

    @Test
    fun nestedPostFling_noop() {
        val refreshThreshold = 200f
        lateinit var state: PullRefreshState
        var onRefreshCalled = false

        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { onRefreshCalled = true },
                refreshThreshold = with(LocalDensity.current) { refreshThreshold.toDp() }
            )
            Box(Modifier.size(200.dp).pullRefresh(state)) {
                Box(Modifier.size(100.dp).nestedScroll(connection, dispatcher))
            }
        }

        // Fling upwards
        val flingUp = Velocity(0f, 100f)
        // Fling downwards
        val flingDown = Velocity(0f, 100f)

        rule.runOnIdle {
            val postConsumedUp = runBlocking {
                dispatcher.dispatchPostFling(Velocity.Zero, flingUp)
            }
            // Noop
            assertThat(postConsumedUp).isEqualTo(Velocity.Zero)
            val postConsumedDown = runBlocking {
                dispatcher.dispatchPostFling(Velocity.Zero, flingDown)
            }
            // Noop
            assertThat(postConsumedDown).isEqualTo(Velocity.Zero)
            // Noop
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            assertThat(state.position).isEqualTo(0f)
        }

        // Pull the state but not past the threshold
        state.onPull(refreshThreshold / 2f)

        rule.runOnIdle {
            assertThat(state.position)
                .isEqualTo(refreshThreshold / 4f /* account for drag multiplier */)
            val postConsumedUp = runBlocking {
                dispatcher.dispatchPostFling(Velocity.Zero, flingUp)
            }
            // Noop
            assertThat(postConsumedUp).isEqualTo(Velocity.Zero)
            val postConsumedDown = runBlocking {
                dispatcher.dispatchPostFling(Velocity.Zero, flingDown)
            }
            // Noop
            assertThat(postConsumedDown).isEqualTo(Velocity.Zero)
            // Noop
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            // Position should stay the same
            assertThat(state.position)
                .isEqualTo(refreshThreshold / 4f /* account for drag multiplier */)
        }

        // Pull the state past the threshold (we have already pulled half of this, so this is now
        // 1.5 x refreshThreshold for the pull)
        state.onPull(refreshThreshold)

        rule.runOnIdle {
            assertThat(state.position)
                .isEqualTo((refreshThreshold * (3 / 2f)) / 2f /* account for drag multiplier */)
            val postConsumedUp = runBlocking {
                dispatcher.dispatchPostFling(Velocity.Zero, flingUp)
            }
            // Noop
            assertThat(postConsumedUp).isEqualTo(Velocity.Zero)
            val postConsumedDown = runBlocking {
                dispatcher.dispatchPostFling(Velocity.Zero, flingDown)
            }
            // Noop
            assertThat(postConsumedDown).isEqualTo(Velocity.Zero)
            // Noop
            assertThat(onRefreshCalled).isFalse()
        }

        rule.runOnIdle {
            // Position should be unchanged
            assertThat(state.position)
                .isEqualTo(refreshThreshold * (3 / 2f) / 2f /* account for drag multiplier */)
        }
    }

    /**
     * Taken from the private function of the same name in [PullRefreshState].
     */
    private fun calculateIndicatorPosition(distance: Float, threshold: Float): Float = when {
        distance <= threshold -> distance
        else -> {
            val overshootPercent = abs(distance / threshold) - 1.0f
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            val extraOffset = threshold * tensionPercent
            threshold + extraOffset
        }
    }
}

private const val PullRefreshTag = "PullRefresh"
