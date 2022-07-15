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

package androidx.compose.material.swipeable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterialApi::class)
class SwipeableV2GestureTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun swipeable_swipe_horizontal() = directionalSwipeTest(
        SwipeableV2State(initialState = A),
        orientation = Orientation.Horizontal,
        verify = verifyOffsetMatchesAnchor()
    )

    @Test
    fun swipeable_swipe_vertical() = directionalSwipeTest(
        SwipeableV2State(initialState = A),
        orientation = Orientation.Vertical,
        verify = verifyOffsetMatchesAnchor()
    )

    @Test
    fun swipeable_swipe_disabled_horizontal() = directionalSwipeTest(
        SwipeableV2State(initialState = A),
        orientation = Orientation.Horizontal,
        enabled = false,
        verify = verifyOffset0(),
    )

    @Test
    fun swipeable_swipe_disabled_vertical(): Unit = directionalSwipeTest(
        SwipeableV2State(initialState = A),
        orientation = Orientation.Vertical,
        enabled = false,
        verify = verifyOffset0(),
    )

    @Test
    fun swipeable_swipe_reverse_direction_horizontal() = directionalSwipeTest(
        SwipeableV2State(initialState = A),
        orientation = Orientation.Horizontal,
        verify = verifyOffsetMatchesAnchor()
    )

    private fun verifyOffset0() = { state: SwipeableV2State<TestState>, _: TestState ->
        assertThat(state.offset.value).isEqualTo(0f)
    }

    private fun verifyOffsetMatchesAnchor() =
        { state: SwipeableV2State<TestState>, target: TestState ->
            val swipeableSizePx = with(rule.density) { swipeableSize.toPx() }
            val targetOffset = when (target) {
                A -> 0f
                B -> swipeableSizePx / 2
                C -> swipeableSizePx
            }
            assertThat(state.offset.value).isEqualTo(targetOffset)
        }

    private fun directionalSwipeTest(
        state: SwipeableV2State<TestState>,
        orientation: Orientation,
        enabled: Boolean = true,
        reverseDirection: Boolean = false,
        swipeForward: TouchInjectionScope.(end: Float) -> Unit = {
            if (orientation == Orientation.Horizontal) swipeRight(endX = it)
            else swipeDown(endY = it)
        },
        swipeBackward: TouchInjectionScope.(end: Float) -> Unit = {
            if (orientation == Orientation.Horizontal) swipeLeft(endX = it) else swipeUp(endY = it)
        },
        verify: (state: SwipeableV2State<TestState>, target: TestState) -> Unit,
    ) {
        rule.setContent {
            SwipeableBox(state, orientation, enabled = enabled, reverseDirection = reverseDirection)
        }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeForward(endEdge(orientation) / 2) }
        rule.waitForIdle()
        verify(state, B)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeForward(endEdge(orientation)) }
        rule.waitForIdle()
        verify(state, C)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeBackward(endEdge(orientation) / 2) }
        rule.waitForIdle()
        verify(state, B)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeBackward(startEdge(orientation)) }
        rule.waitForIdle()
        verify(state, A)
    }

    private fun TouchInjectionScope.endEdge(orientation: Orientation) =
        if (orientation == Orientation.Horizontal) right else bottom

    private fun TouchInjectionScope.startEdge(orientation: Orientation) =
        if (orientation == Orientation.Horizontal) left else top
}