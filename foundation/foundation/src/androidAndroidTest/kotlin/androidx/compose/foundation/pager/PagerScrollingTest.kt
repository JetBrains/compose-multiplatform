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
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerScrollingTest(
    val config: ParamConfig
) : BasePagerTest(config) {

    @Test
    fun swipeWithLowVelocity_shouldBounceBack() {
        // Arrange
        val state = PagerState(5)
        createPager(state = state, modifier = Modifier.fillMaxSize())
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                delta
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)

        // Act - backward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                delta * -1
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithHighVelocity_shouldGoToNextPage() {
        // Arrange
        val state = PagerState(5)
        createPager(state = state, modifier = Modifier.fillMaxSize())
        // make sure the scroll distance is not enough to go to next page
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                delta
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                delta * -1
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithHighVelocity_overHalfPage_shouldGoToNextPage() {
        // Arrange
        val state = PagerState(5)
        createPager(state = state, modifier = Modifier.fillMaxSize())
        // make sure the scroll distance is not enough to go to next page
        val delta = pagerSize * 0.8f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                delta
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                delta * -1
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun scrollWithoutVelocity_shouldSettlingInClosestPage() {
        // Arrange
        val state = PagerState(5)
        createPager(state = state, modifier = Modifier.fillMaxSize())
        // This will scroll 1 whole page before flinging
        val delta = pagerSize * 1.4f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(0f, delta)
        }
        rule.waitForIdle()

        // Assert
        assertThat(state.currentPage).isAtMost(7)
        rule.onNodeWithTag("${state.currentPage}").assertIsDisplayed()
        confirmPageIsInCorrectPosition(state.currentPage)

        // Act - backward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(0f, delta * -1)
        }
        rule.waitForIdle()

        // Assert
        assertThat(state.currentPage).isAtLeast(5)
        rule.onNodeWithTag("${state.currentPage}").assertIsDisplayed()
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun scrollWithSameVelocity_shouldYieldSameResult_forward() {
        // Arrange
        var initialPage = 1
        val state = PagerState(initialPage)
        createPager(
            pageSize = PageSize.Fixed(200.dp),
            state = state,
            modifier = Modifier.fillMaxSize(),
            pageCount = { 100 },
            snappingPage = PagerSnapDistance.atMost(3)
        )
        // This will scroll 0.5 page before flinging
        val delta = pagerSize * 0.5f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        val pageDisplacement = state.currentPage - initialPage

        // Repeat starting from different places
        // reset
        initialPage = 10
        rule.runOnIdle {
            runBlocking { state.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(state.currentPage - initialPage).isEqualTo(pageDisplacement)

        initialPage = 50
        rule.runOnIdle {
            runBlocking { state.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(state.currentPage - initialPage).isEqualTo(pageDisplacement)
    }

    @Test
    fun scrollWithSameVelocity_shouldYieldSameResult_backward() {
        // Arrange
        var initialPage = 90
        val state = PagerState(initialPage)
        createPager(
            pageSize = PageSize.Fixed(200.dp),
            state = state,
            modifier = Modifier.fillMaxSize(),
            pageCount = { 100 },
            snappingPage = PagerSnapDistance.atMost(3)
        )
        // This will scroll 0.5 page before flinging
        val delta = pagerSize * -0.5f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        val pageDisplacement = state.currentPage - initialPage

        // Repeat starting from different places
        // reset
        initialPage = 70
        rule.runOnIdle {
            runBlocking { state.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(state.currentPage - initialPage).isEqualTo(pageDisplacement)

        initialPage = 30
        rule.runOnIdle {
            runBlocking { state.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(state.currentPage - initialPage).isEqualTo(pageDisplacement)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (pageSpacing in TestPageSpacing) {
                    add(
                        ParamConfig(
                            orientation = orientation,
                            pageSpacing = pageSpacing
                        )
                    )
                }
            }
        }
    }
}