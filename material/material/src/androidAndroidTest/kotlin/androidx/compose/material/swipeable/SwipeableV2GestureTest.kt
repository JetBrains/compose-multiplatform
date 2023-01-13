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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material.AutoTestFrameClock
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2Defaults
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.fixedPositionalThreshold
import androidx.compose.material.fractionalPositionalThreshold
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    fun swipeable_swipe_horizontal() {
        val state = SwipeableTestState(initialState = A)
        val anchors = mapOf(
            A to 0f,
            B to 250f,
            C to 500f
        )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    SwipeableBox(
                        swipeableState = state,
                        orientation = Orientation.Horizontal,
                        anchors = anchors
                    )
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeRight(endX = right / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.getValue(B))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeRight(startX = right / 2, endX = right) }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.offset).isEqualTo(anchors.getValue(C))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeLeft(endX = right / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.getValue(B))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeLeft(startX = right / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.getValue(A))
    }

    @Test
    fun swipeable_swipe_vertical() {
        val state = SwipeableTestState(initialState = A)
        val anchors = mapOf(
            A to 0f,
            B to 250f,
            C to 500f
        )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    SwipeableBox(
                        swipeableState = state,
                        orientation = Orientation.Vertical,
                        anchors = anchors
                    )
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(startY = top, endY = bottom / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.getValue(B))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(startY = bottom / 2, endY = bottom) }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(C)
        assertThat(state.offset).isEqualTo(anchors.getValue(C))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(startY = bottom, endY = bottom / 2) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
        assertThat(state.offset).isEqualTo(anchors.getValue(B))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(startY = bottom / 2, endY = top) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isEqualTo(anchors.getValue(A))
    }

    @Test
    fun swipeable_swipe_disabled_horizontal() {
        val state = SwipeableTestState(initialState = A)
        val anchors = mapOf(
            A to 0f,
            B to 250f,
            C to 500f
        )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    SwipeableBox(
                        swipeableState = state,
                        orientation = Orientation.Horizontal,
                        anchors = anchors,
                        enabled = false
                    )
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeRight(startX = left, endX = right) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isZero()
    }

    @Test
    fun swipeable_swipe_disabled_vertical() {
        val state = SwipeableTestState(initialState = A)
        val anchors = mapOf(
            A to 0f,
            B to 250f,
            C to 500f
        )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                WithTouchSlop(0f) {
                    SwipeableBox(
                        swipeableState = state,
                        orientation = Orientation.Vertical,
                        anchors = anchors,
                        enabled = false
                    )
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(A)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(startY = top, endY = bottom) }
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.offset).isZero()
    }

    @Test
    fun swipeable_positionalThresholds_fractional_targetState() {
        val positionalThreshold = 0.5f
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
        rule.waitForIdle()

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
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_positionalThresholds_fractional_negativeThreshold_targetState() {
        val positionalThreshold = -0.5f
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
        rule.waitForIdle()

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
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_positionalThresholds_fixed_targetState() {
        val positionalThreshold = 56.dp
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
        rule.waitForIdle()

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
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_positionalThresholds_fixed_negativeThreshold_targetState() {
        val positionalThreshold = (-56).dp
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
        rule.waitForIdle()

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
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_velocityThreshold_settle_velocityHigherThanThreshold_advances() =
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
                velocityThreshold = velocity / 2
            )
            state.dispatchRawDelta(60f)
            state.settle(velocityPx)
            rule.waitForIdle()
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

    @Test
    fun swipeable_dragBeyondBounds_clampsAndSwipesBack() {
        val anchors = mapOf(
            A to 0f,
            C to 500f
        )
        val state = SwipeableTestState(
            initialState = A,
            velocityThreshold = 0.dp
        )
        rule.setContent {
            SwipeableBox(
                state,
                calculateAnchor = { state, _ ->
                    anchors[state]
                }
            )
        }

        val overdrag = 100f
        val maxBound = state.anchors.getValue(C)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput {
                down(Offset(0f, 0f))
                moveBy(Offset(x = maxBound + overdrag, y = 0f))
                moveBy(Offset(x = -overdrag, y = 0f))
            }

        rule.waitForIdle()

        // If we have not correctly coerced our drag deltas, its internal offset would be the
        // max bound + overdrag. If it is coerced correctly, it will not move past the max bound.
        // This means that once we swipe back by the amount of overdrag, we should end up at the
        // max bound - overdrag.
        assertThat(state.requireOffset()).isEqualTo(maxBound - overdrag)
    }

    @Test
    fun swipeable_animationCancelledByDrag_resetsTargetValueToClosest() {
        rule.mainClock.autoAdvance = false
        val animationDurationMillis = 500
        val offsetAtB = animationDurationMillis / 2f
        val offsetAtC = animationDurationMillis.toFloat()
        val anchors = mapOf(
            A to 0f,
            B to offsetAtB,
            C to offsetAtC
        )
        val state = SwipeableTestState(
            initialState = A,
            animationSpec = tween(animationDurationMillis, easing = LinearEasing),
            positionalThreshold = fractionalPositionalThreshold(0.5f)
        )
        lateinit var scope: CoroutineScope
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                scope = rememberCoroutineScope()
                SwipeableBox(
                    swipeableState = state,
                    orientation = Orientation.Horizontal,
                    calculateAnchor = { state, _ ->
                        anchors[state]
                    }
                )
            }
        }

        assertThat(state.currentValue).isEqualTo(A)
        assertThat(state.targetValue).isEqualTo(A)

        scope.launch { state.animateTo(C) }

        rule.mainClock.advanceTimeUntil {
            state.requireOffset() > abs(state.requireOffset() - offsetAtB)
        } // Advance until our closest anchor is B
        assertThat(state.targetValue).isEqualTo(C)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput {
                down(Offset.Zero)
            }

        assertThat(state.targetValue).isEqualTo(B) // B is the closest now so we should target it
    }

    private fun SwipeableTestState(
        initialState: TestState,
        density: Density = rule.density,
        positionalThreshold: Density.(distance: Float) -> Float = { 56.dp.toPx() },
        velocityThreshold: Dp = 125.dp,
        anchors: Map<TestState, Float>? = null,
        animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec
    ) = SwipeableV2State(
        initialValue = initialState,
        positionalThreshold = positionalThreshold,
        velocityThreshold = velocityThreshold,
        animationSpec = animationSpec
    ).apply {
        if (anchors != null) updateAnchors(anchors)
        this.density = density
    }
}

private val NoOpDensity = object : Density {
    override val density = 1f
    override val fontScale = 1f
}
