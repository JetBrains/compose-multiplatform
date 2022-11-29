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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerAccessibilityTest(config: ParamConfig) : BasePagerTest(config = config) {

    @Test
    fun accessibilityScroll_scrollToPage() {
        val state = PagerState()
        createPager(state, offscreenPageLimit = 1)

        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(0) }

        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("1").performScrollTo()

        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(1) }
        rule.runOnIdle { assertThat(state.currentPageOffsetFraction).isEqualTo(0.0f) }
    }

    @Test
    fun focusScroll_forwardAndBackward_shouldGoToPage_pageShouldBeCorrectlyPlaced() {
        // Arrange
        val state = PagerState()
        createPager(state)
        rule.runOnIdle { firstItemFocusRequester.requestFocus() }

        // Act: move forward
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        // Assert
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(1) }
        rule.runOnIdle { assertThat(state.currentPageOffsetFraction).isEqualTo(0.0f) }

        // Act: move backward
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        // Assert
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(0) }
        rule.runOnIdle { assertThat(state.currentPageOffsetFraction).isEqualTo(0.0f) }
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