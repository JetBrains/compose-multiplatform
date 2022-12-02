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
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
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
    fun swipePageTowardsEdge_shouldNotMove() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - backward
        rule.onNodeWithTag("0").performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                delta * -1.0f
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("0").assertIsDisplayed()
        confirmPageIsInCorrectPosition(0)

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                delta
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("1").assertIsDisplayed()
        confirmPageIsInCorrectPosition(1)
    }

    @Test
    fun swipeForwardAndBackward_verifyPagesAreLaidOutCorrectly() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act and Assert - forward
        repeat(DefaultAnimationRepetition) {
            rule.onNodeWithTag(it.toString()).assertIsDisplayed()
            confirmPageIsInCorrectPosition(it)
            rule.onNodeWithTag(it.toString()).performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
            rule.waitForIdle()
        }

        // Act - backward
        repeat(DefaultAnimationRepetition) {
            val countDown = DefaultAnimationRepetition - it
            rule.onNodeWithTag(countDown.toString()).assertIsDisplayed()
            confirmPageIsInCorrectPosition(countDown)
            rule.onNodeWithTag(countDown.toString()).performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                    delta * -1f
                )
            }
            rule.waitForIdle()
        }
    }

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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (reverseLayout in TestReverseLayout) {
                    for (layoutDirection in TestLayoutDirection) {
                        for (pageSpacing in TestPageSpacing) {
                            for (contentPadding in testContentPaddings(orientation)) {
                                add(
                                    ParamConfig(
                                        orientation = orientation,
                                        reverseLayout = reverseLayout,
                                        layoutDirection = layoutDirection,
                                        pageSpacing = pageSpacing,
                                        mainAxisContentPadding = contentPadding
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}