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
import androidx.compose.material.AutoTestFrameClock
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.fixedPositionalThreshold
import androidx.compose.material.fractionalPositionalThreshold
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
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
        SwipeableTestState(initialState = A),
        orientation = Orientation.Horizontal,
        verify = verifyOffsetMatchesAnchor()
    )

    @Test
    fun swipeable_swipe_vertical() = directionalSwipeTest(
        SwipeableTestState(initialState = A),
        orientation = Orientation.Vertical,
        verify = verifyOffsetMatchesAnchor()
    )

    @Test
    fun swipeable_swipe_disabled_horizontal() = directionalSwipeTest(
        SwipeableTestState(initialState = A),
        orientation = Orientation.Horizontal,
        enabled = false,
        verify = verifyOffset0(),
    )

    @Test
    fun swipeable_swipe_disabled_vertical(): Unit = directionalSwipeTest(
        SwipeableTestState(initialState = A),
        orientation = Orientation.Vertical,
        enabled = false,
        verify = verifyOffset0(),
    )

    @Test
    fun swipeable_swipe_reverse_direction_horizontal() = directionalSwipeTest(
        SwipeableTestState(initialState = A),
        orientation = Orientation.Horizontal,
        verify = verifyOffsetMatchesAnchor()
    )

    private fun verifyOffset0() = { state: SwipeableV2State<TestState>, _: TestState ->
        assertThat(state.offset).isEqualTo(0f)
    }

    private fun verifyOffsetMatchesAnchor() =
        { state: SwipeableV2State<TestState>, target: TestState ->
            val swipeableSizePx = with(rule.density) { swipeableSize.toPx() }
            val targetOffset = when (target) {
                A -> 0f
                B -> swipeableSizePx / 2
                C -> swipeableSizePx
            }
            assertThat(state.offset).isEqualTo(targetOffset)
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

    @Test
    fun swipeable_positionalThresholds_fractional_targetState() = fractionalThresholdsTest(0.5f)

    @Test
    fun swipeable_positionalThresholds_fractional_negativeThreshold_targetState() =
        fractionalThresholdsTest(-0.5f)

    private fun fractionalThresholdsTest(positionalThreshold: Float) {
        val absThreshold = abs(positionalThreshold)
        val state = SwipeableTestState(
            initialState = A,
            positionalThreshold = fractionalPositionalThreshold(positionalThreshold)
        )
        rule.setContent { SwipeableBox(state) }

        val positionOfA = state.anchors.getValue(A)
        val positionOfB = state.anchors.getValue(B)
        val distance = abs(positionOfA - positionOfB)
        state.dispatchRawDelta(positionOfA + distance * (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        state.dispatchRawDelta(distance * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(B)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(-distance * (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        state.dispatchRawDelta(-distance * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(A)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_positionalThresholds_fixed_targetState() = fixedThresholdsTest(56.dp)

    @Test
    fun swipeable_positionalThresholds_fixed_negativeThreshold_targetState() =
        fixedThresholdsTest((-56).dp)

    private fun fixedThresholdsTest(positionalThreshold: Dp) {
        val absThreshold = with(rule.density) { abs(positionalThreshold.toPx()) }
        val state = SwipeableTestState(
            initialState = A,
            positionalThreshold = fixedPositionalThreshold(positionalThreshold)
        )
        rule.setContent { SwipeableBox(state) }

        val initialOffset = state.requireOffset()

        // Swipe towards B, close before threshold
        state.dispatchRawDelta(initialOffset + (absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        // Swipe towards B, close after threshold
        state.dispatchRawDelta(absThreshold * 0.2f)
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(B)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        // Swipe towards A, close before threshold
        state.dispatchRawDelta(-(absThreshold * 0.9f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(B)

        // Swipe towards A, close after threshold
        state.dispatchRawDelta(-(absThreshold * 0.2f))
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.targetValue).isEqualTo(A)

        runBlocking(AutoTestFrameClock()) { state.settle(velocity = 0f) }

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_velocityThreshold_settle_velocityHigherThanThreshold_advances() =
        runBlocking(AutoTestFrameClock()) {
            val velocity = 100.dp
            val velocityPx = with(rule.density) { velocity.toPx() }
            val state = with(rule) {
                SwipeableTestState(
                    initialState = A,
                    anchors = mapOf(
                        A to 0f,
                        B to 100f,
                        C to 200f
                    ),
                    velocityThreshold = velocity / 2
                )
            }
            state.dispatchRawDelta(60f)
            state.settle(velocityPx)
            assertThat(state.currentValue).isEqualTo(B)
        }

    @Test
    fun swipeable_velocityThreshold_settle_velocityLowerThanThreshold_doesntAdvance() =
        runBlocking(AutoTestFrameClock()) {
            val velocity = 100.dp
            val velocityPx = with(rule.density) { velocity.toPx() }
            val state = SwipeableTestState(
                initialState = A,
                anchors = mapOf(
                    A to 0f,
                    B to 100f,
                    C to 200f
                ),
                velocityThreshold = velocity,
                positionalThreshold = { Float.POSITIVE_INFINITY }
            )
            state.dispatchRawDelta(60f)
            state.settle(velocityPx / 2)
            assertThat(state.currentValue).isEqualTo(A)
        }

    @Test
    fun swipeable_velocityThreshold_swipe_velocityHigherThanThreshold_advances() {
        val velocityThreshold = 100.dp
        val state = SwipeableTestState(
            initialState = A,
            velocityThreshold = velocityThreshold
        )
        rule.setContent { SwipeableBox(state) }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(left, 0f),
                    end = Offset(right / 2, 0f),
                    endVelocity = with(rule.density) { velocityThreshold.toPx() } * 1.1f
                )
            }

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun swipeable_velocityThreshold_swipe_velocityLowerThanThreshold_doesntAdvance() {
        val velocityThreshold = 100.dp
        val state = SwipeableTestState(
            initialState = A,
            velocityThreshold = velocityThreshold,
            positionalThreshold = { Float.POSITIVE_INFINITY }
        )
        rule.setContent { SwipeableBox(state) }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput {
                swipeWithVelocity(
                    start = Offset(left, 0f),
                    end = Offset(right / 2, 0f),
                    endVelocity = with(rule.density) { velocityThreshold.toPx() } * 0.9f
                )
            }

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(A)
    }

    private fun SwipeableTestState(
        initialState: TestState,
        density: Density = rule.density,
        positionalThreshold: Density.(distance: Float) -> Float = { 56f },
        velocityThreshold: Dp = 125.dp,
        anchors: Map<TestState, Float>? = null
    ) = SwipeableV2State(
        initialValue = initialState,
        positionalThreshold = positionalThreshold,
        velocityThreshold = velocityThreshold,
        density = density
    ).apply { if (anchors != null) updateAnchors(anchors) }

    private fun TouchInjectionScope.endEdge(orientation: Orientation) =
        if (orientation == Orientation.Horizontal) right else bottom

    private fun TouchInjectionScope.startEdge(orientation: Orientation) =
        if (orientation == Orientation.Horizontal) left else top
}