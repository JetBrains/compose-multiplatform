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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerTest(val config: ParamConfig) : BasePagerTest(config) {

    @Before
    fun setUp() {
        placed.clear()
    }

    @Test
    fun userScrollEnabledIsOff_shouldNotAllowGestureScroll() {
        // Arrange
        val state = PagerState()
        createPager(
            state = state,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        // Act
        onPager().performTouchInput { swipeWithVelocityAcrossMainAxis(1000f) }

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(0)
        }

        confirmPageIsInCorrectPosition(0, 0)
    }

    @Test
    fun userScrollEnabledIsOff_shouldAllowAnimationScroll() {
        // Arrange
        val state = PagerState()
        createPager(
            state = state,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        // Act
        rule.runOnIdle {
            scope.launch {
                state.animateScrollToPage(5)
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(5)
        }
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun userScrollEnabledIsOn_shouldAllowGestureScroll() {
        // Arrange
        val state = PagerState(5)
        createPager(
            state = state,
            userScrollEnabled = true,
            modifier = Modifier.fillMaxSize()
        )

        onPager().performTouchInput { swipeWithVelocityAcrossMainAxis(1000f) }

        rule.runOnIdle {
            assertThat(state.currentPage).isNotEqualTo(5)
        }
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun pageCount_pagerOnlyContainsGivenPageCountItems() {
        // Arrange
        val state = PagerState()

        // Act
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Assert
        repeat(DefaultPageCount) {
            rule.onNodeWithTag("$it").assertIsDisplayed()
            rule.runOnIdle {
                scope.launch {
                    state.scroll {
                        scrollBy(pagerSize.toFloat())
                    }
                }
            }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("$DefaultPageCount").assertDoesNotExist()
    }

    @Test
    fun mutablePageCount_assertPagesAreChangedIfCountIsChanged() {
        // Arrange
        val state = PagerState()
        val pageCount = mutableStateOf(2)
        createPager(
            state = state,
            modifier = Modifier.fillMaxSize(),
            pageCount = { pageCount.value }
        )

        rule.onNodeWithTag("3").assertDoesNotExist()

        // Act
        pageCount.value = DefaultPageCount
        rule.waitForIdle()

        // Assert
        repeat(DefaultPageCount) {
            rule.onNodeWithTag("$it").assertIsDisplayed()
            rule.runOnIdle {
                scope.launch {
                    state.scroll {
                        scrollBy(pagerSize.toFloat())
                    }
                }
            }
            rule.waitForIdle()
        }
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