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

import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerAccessibilityTest(config: ParamConfig) : BasePagerTest(config = config) {

    private val accessibilityNodeProvider: AccessibilityNodeProvider
        get() = checkNotNull(composeView) {
            "composeView not initialized."
        }.let { composeView ->
            ViewCompat
                .getAccessibilityDelegate(composeView)!!
                .getAccessibilityNodeProvider(composeView)!!
                .provider as AccessibilityNodeProvider
        }

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
    fun accessibilityPaging_animateScrollToPage() {
        val state = PagerState(initialPage = 5)
        createPager(state)

        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(5) }

        val actionBackward = if (isVertical) {
            android.R.id.accessibilityActionPageUp
        } else {
            android.R.id.accessibilityActionPageLeft
        }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(
                id,
                actionBackward,
                null
            )
        }

        // Go to the previous page
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(4) }
        rule.runOnIdle { assertThat(state.currentPageOffsetFraction).isEqualTo(0.0f) }

        val actionForward = if (isVertical) {
            android.R.id.accessibilityActionPageDown
        } else {
            android.R.id.accessibilityActionPageRight
        }

        rule.onNodeWithTag(PagerTestTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(
                id,
                actionForward,
                null
            )
        }

        // Go to the next page
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(5) }
        rule.runOnIdle { assertThat(state.currentPageOffsetFraction).isEqualTo(0.0f) }
    }

    @Test
    fun userScrollEnabledIsOff_shouldNotAllowPageAccessibilityActions() {
        // Arrange
        val state = PagerState()
        createPager(
            state = state,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        // Act
        onPager()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageUp))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageDown))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageRight))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PageLeft))
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

    private fun <T> SemanticsNodeInteraction.withSemanticsNode(block: SemanticsNode.() -> T): T {
        return block.invoke(fetchSemanticsNode())
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