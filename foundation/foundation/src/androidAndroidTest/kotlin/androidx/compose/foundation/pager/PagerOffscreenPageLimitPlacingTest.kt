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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerOffscreenPageLimitPlacingTest(
    val config: ParamConfig
) : BasePagerTest(config) {

    @Test
    fun offscreenPageLimitIsUsed_shouldPlaceMoreItemsThanVisibleOnesAsWeScroll() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize(), offscreenPageLimit = 1)
        val delta = pagerSize * 1.4f * scrollForwardSign

        repeat(DefaultAnimationRepetition) {
            // Act
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(0f, delta)
            }

            rule.waitForIdle()
            // Next page was placed
            rule.runOnIdle {
                Truth.assertThat(placed).contains(
                    (state.currentPage + 1)
                        .coerceAtMost(DefaultPageCount - 1)
                )
            }
        }
        rule.waitForIdle()
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun offscreenPageLimitIsUsed_shouldPlaceMoreItemsThanVisibleOnes() {
        // Arrange
        val initialIndex = 5
        val state = PagerState(initialIndex)

        // Act
        createPager(state = state, modifier = Modifier.fillMaxSize(), offscreenPageLimit = 2)
        val firstVisible = state.layoutInfo.visibleItemsInfo.first().index
        val lastVisible = state.layoutInfo.visibleItemsInfo.last().index
        // Assert
        rule.runOnIdle {
            Truth.assertThat(placed).contains(firstVisible - 2)
            Truth.assertThat(placed).contains(firstVisible - 1)
            Truth.assertThat(placed).contains(lastVisible + 1)
            Truth.assertThat(placed).contains(lastVisible + 2)
        }
        confirmPageIsInCorrectPosition(initialIndex, firstVisible - 2)
        confirmPageIsInCorrectPosition(initialIndex, firstVisible - 1)
        confirmPageIsInCorrectPosition(initialIndex, lastVisible + 1)
        confirmPageIsInCorrectPosition(initialIndex, lastVisible + 2)
    }

    @Test
    fun offscreenPageLimitIsNotUsed_shouldNotPlaceMoreItemsThanVisibleOnes() {
        // Arrange
        val state = PagerState(5)

        // Act
        createPager(state = state, modifier = Modifier.fillMaxSize(), offscreenPageLimit = 0)

        // Assert
        rule.waitForIdle()
        val firstVisible = state.layoutInfo.visibleItemsInfo.first().index
        val lastVisible = state.layoutInfo.visibleItemsInfo.last().index
        Truth.assertThat(placed).doesNotContain(firstVisible - 1)
        Truth.assertThat(placed).contains(5)
        Truth.assertThat(placed).doesNotContain(lastVisible + 1)
        confirmPageIsInCorrectPosition(5)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (pageSpacing in TestPageSpacing) {
                    for (contentPadding in testContentPaddings(orientation)) {
                        add(
                            ParamConfig(
                                orientation = orientation,
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