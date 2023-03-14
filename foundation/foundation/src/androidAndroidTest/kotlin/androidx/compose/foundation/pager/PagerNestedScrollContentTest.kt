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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerNestedScrollContentTest(
    config: ParamConfig
) : BasePagerTest(config = config) {

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun nestedScrollContent_shouldNotPropagateUnconsumedFlings() {
        // Arrange
        val pagerState = PagerState()
        createPager(pagerState) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = isVertical, // scrollable content on the same direction as pager
                reverseLayout = false,
                state = rememberLazyListState(),
                userScrollEnabled = true,
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        // Act: High velocity swipe should fling inner list to edge
        val forwardDelta = pagerSize / 2f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, forwardDelta)
        }
        rule.waitForIdle()

        // Assert: Fling was not propagated, so we didn't move pages
        assertThat(pagerState.currentPage).isEqualTo(0)
        assertEquals(pagerState.currentPageOffsetFraction, 0f, 0.01f)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun nestedScrollContent_shouldPropagateCrossAxisUnconsumedFlings() {
        // Arrange
        val pagerState = PagerState()
        var postFlingVelocity = Velocity.Zero
        val dataCapturingConnection = object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                postFlingVelocity = available
                return Velocity.Zero
            }
        }
        createPager(pagerState, nestedScrollConnection = dataCapturingConnection) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = !isVertical, // scrollable content on the cross direction of pager
                reverseLayout = false,
                state = rememberLazyListState(),
                userScrollEnabled = true,
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        // Act
        val forwardDelta = pagerSize / 2f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossCrossAxis(10000f, forwardDelta)
        }
        rule.waitForIdle()

        // Assert
        val mainAxisVelocity = if (isVertical) postFlingVelocity.y else postFlingVelocity.x
        val crossAxisVelocity = if (isVertical) postFlingVelocity.x else postFlingVelocity.y
        assertThat(mainAxisVelocity.absoluteValue).isEqualTo(0f)
        assertThat(crossAxisVelocity.absoluteValue).isNotEqualTo(0f)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun nestedScrollContent_shouldPropagateScrollCorrectly() {
        // Arrange
        val pagerState = PagerState()
        val lazyListState = LazyListState(9)
        createPager(pagerState) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = isVertical, // scrollable content on the same direction as pager
                reverseLayout = false,
                state = lazyListState,
                userScrollEnabled = true,
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        // Act: Scroll More than Half an Item
        val forwardDelta = pagerSize * 0.6f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, forwardDelta)
        }
        rule.waitForIdle()

        // Assert: Inner list won't consume scroll and Pager can scroll to the next page
        assertThat(pagerState.currentPage).isEqualTo(1)
        assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0f)

        // reset inner list
        rule.runOnIdle {
            runBlocking {
                lazyListState.scrollToItem(0)
            }
        }

        // Act: Scroll More than Half an Item
        val backwardDelta = pagerSize * 0.6f * scrollForwardSign.toFloat() * -1f
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, backwardDelta)
        }
        rule.waitForIdle()

        // Assert: Inner list won't consume scroll and Pager can scroll to the previous page
        assertThat(pagerState.currentPage).isEqualTo(0)
        assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0f)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                add(ParamConfig(orientation = orientation))
            }
        }
    }
}

private const val TestTag = "pager"