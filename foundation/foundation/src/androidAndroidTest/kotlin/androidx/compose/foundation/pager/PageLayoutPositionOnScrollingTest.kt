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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PageLayoutPositionOnScrollingTest(
    val config: ParamConfig
) : BasePagerTest(config) {

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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (pageSpacing in TestPageSpacing) {
                    for (reverseLayout in TestReverseLayout) {
                        for (layoutDirection in TestLayoutDirection) {
                            for (contentPadding in testContentPaddings(orientation)) {
                                add(
                                    ParamConfig(
                                        orientation = orientation,
                                        pageSpacing = pageSpacing,
                                        mainAxisContentPadding = contentPadding,
                                        reverseLayout = reverseLayout,
                                        layoutDirection = layoutDirection
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