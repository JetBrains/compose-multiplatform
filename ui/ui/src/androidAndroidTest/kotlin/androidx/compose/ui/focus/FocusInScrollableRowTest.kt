/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.onRelocationRequest
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusInScrollableRowTest {
    @get:Rule
    val rule = createComposeRule()
    val itemSize = with(rule.density) { 100.toDp() }
    lateinit var scrollState: ScrollState
    lateinit var focusManager: FocusManager

    @Test
    fun focusingOnVisibleItemDoesNotScroll() {
        // Arrange.
        val visibleItem = FocusRequester()
        rule.setContent {
            ScrollableRow {
                FocusableBox(Modifier.focusRequester(visibleItem))
                FocusableBox()
                FocusableBox()
                FocusableBox()
            }
        }

        // Act.
        rule.runOnIdle { visibleItem.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }
    }

    @Test
    fun focusingOutOfBoundsItem_bringsItIntoView() {
        // Arrange.
        val outOfBoundsItem = FocusRequester()
        rule.setContent {
            ScrollableRow {
                FocusableBox()
                FocusableBox()
                FocusableBox(Modifier.focusRequester(outOfBoundsItem))
                FocusableBox()
            }
        }

        // Act.
        rule.runOnIdle { outOfBoundsItem.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(100)
        }
    }

    @Test
    fun moveRightFromBoundaryItem_bringsNextItemIntoView() {
        // Arrange.
        val itemOnBoundary = FocusRequester()
        rule.setContent {
            ScrollableRow {
                FocusableBox()
                FocusableBox(Modifier.focusRequester(itemOnBoundary))
                FocusableBox()
                FocusableBox()
            }
        }
        rule.runOnIdle { itemOnBoundary.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Right) }

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    @Composable
    private fun ScrollableRow(content: @Composable RowScope.() -> Unit) {
        scrollState = rememberScrollState()
        focusManager = LocalFocusManager.current
        Row(
            modifier = Modifier
                .size(itemSize * 2, itemSize)
                .horizontalScrollWithRelocation(scrollState),
            content = content
        )
    }

    @Composable
    private fun FocusableBox(modifier: Modifier = Modifier) {
        Box(
            modifier
                .size(itemSize)
                .focusableWithRelocation()
        )
    }
}

// This is a hel function that users will have to use until bringIntoView is added to
// Modifier.focusable()
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.focusableWithRelocation() = composed {
    val relocationRequester = remember { RelocationRequester() }
    val coroutineScope = rememberCoroutineScope()
    Modifier
        .relocationRequester(relocationRequester)
        .onFocusChanged {
            if (it.isFocused) {
                coroutineScope.launch { relocationRequester.bringIntoView() }
            }
        }
        .focusable()
}

// This is a helper function that users will have to use since experimental "ui" API cannot be used
// inside Scrollable, which is ihe "foundation" package. After onRelocationRequest is added
// to Scrollable, users can use Modifier.horizontalScroll directly.
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.horizontalScrollWithRelocation(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier {
    return this
        .onRelocationRequest(
            onProvideDestination = { rect, layoutCoordinates ->
                val size = layoutCoordinates.size.toSize()
                rect.translate(relocationDistance(rect.left, rect.right, size.width), 0f)
            },
            onPerformRelocation = { source, destination ->
                val offset = destination.left - source.left
                state.animateScrollBy(if (reverseScrolling) -offset else offset)
            }
        )
        .horizontalScroll(state, enabled, flingBehavior, reverseScrolling)
}

// Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
// closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
// The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
// 'bottom').
private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) = when {
    // If the item is already visible, no need to scroll.
    leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

    // If the item is visible but larger than the parent, we don't scroll.
    leadingEdge < 0 && trailingEdge > parentSize -> 0f

    // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
    abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
    else -> trailingEdge - parentSize
}
