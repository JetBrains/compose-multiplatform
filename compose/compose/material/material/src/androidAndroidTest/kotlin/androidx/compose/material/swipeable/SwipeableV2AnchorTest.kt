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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.AnchorChangeHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2Defaults
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.fractionalPositionalThreshold
import androidx.compose.material.rememberSwipeableV2State
import androidx.compose.material.swipeAnchors
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterialApi::class)
class SwipeableV2AnchorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun swipeable_swipeAnchors_updatedInSameFrame() {
        rule.mainClock.autoAdvance = false

        var compositionCounter = 0
        lateinit var state: SwipeableV2State<TestState>

        rule.setContent {
            state = rememberSwipeableV2State(initialValue = A)
            compositionCounter++
            Box(
                Modifier
                    .height(200.dp)
                    .swipeAnchors(
                        state,
                        possibleValues = setOf(A, B, C)
                    ) { state, layoutSize ->
                        when (state) {
                            A -> 0f
                            B -> layoutSize.height.toFloat() / 2
                            C -> layoutSize.height.toFloat()
                        }
                    }
            )
        }
        // Verify composed initially but didn't recompose
        assertThat(compositionCounter).isEqualTo(1)
        // Verify that the anchors are present after one composition
        assertThat(state.anchors).containsKey(A)
        assertThat(state.anchors).containsKey(B)
        assertThat(state.anchors).containsKey(C)
    }

    @Test
    fun swipeable_swipeAnchors_calculatedCorrectlyFromLayoutSize() {
        lateinit var state: SwipeableV2State<TestState>

        fun anchorA() = 0f
        fun anchorB(layoutHeight: Int) = layoutHeight / 2f
        fun anchorC(layoutHeight: Int) = layoutHeight.toFloat()

        val swipeableSize = 200.dp

        rule.setContent {
            state = rememberSwipeableV2State(initialValue = A)
            Box(
                Modifier
                    .requiredHeight(swipeableSize)
                    .swipeAnchors(
                        state,
                        possibleValues = setOf(A, B, C)
                    ) { state, layoutSize ->
                        when (state) {
                            A -> 0f
                            B -> anchorB(layoutSize.height)
                            C -> anchorC(layoutSize.height)
                        }
                    }
            )
        }

        val expectedHeight = with(rule.density) { swipeableSize.roundToPx() }
        assertThat(state.anchors[A]).isEqualTo(anchorA())
        assertThat(state.anchors[B]).isEqualTo(anchorB(expectedHeight))
        assertThat(state.anchors[C]).isEqualTo(anchorC(expectedHeight))
    }

    @Test
    fun swipeable_swipeAnchors_updateOnSizeChange() {
        lateinit var state: SwipeableV2State<TestState>

        val firstAnchors = mapOf(A to 0f, B to 100f, C to 200f)
        val secondAnchors = mapOf(B to 300f, C to 600f)
        var anchors = firstAnchors
        var size by mutableStateOf(100)

        rule.setContent {
            state = rememberSwipeableV2State(A)
            Box(
                Modifier
                    .size(size.dp) // Trigger remeasure when size changes
                    .swipeAnchors(
                        state,
                        possibleValues = setOf(A, B, C),
                        calculateAnchor = { state, _ -> anchors[state] }
                    )
            )
        }

        assertThat(state.anchors).isEqualTo(firstAnchors)

        anchors = secondAnchors
        size = 200
        rule.waitForIdle()

        assertThat(state.anchors).isEqualTo(secondAnchors)
    }

    @Test
    fun swipeable_reconcileAnchorChangeHandler_retargetsAnimationWhenOffsetChanged() {
        val animationDurationMillis = 2000
        lateinit var state: SwipeableV2State<TestState>
        lateinit var scope: CoroutineScope

        val firstAnchors = mapOf(A to 0f, B to 100f, C to 200f)
        val secondAnchors = mapOf(A to 300f, B to 400f, C to 600f)
        var anchors = firstAnchors
        var size by mutableStateOf(100)

        val animationTarget = B

        rule.setContent {
            state = remember {
                SwipeableV2State(
                    initialValue = A,
                    animationSpec = tween(animationDurationMillis, easing = LinearEasing),
                    positionalThreshold = fractionalPositionalThreshold(0.5f)
                )
            }
            scope = rememberCoroutineScope()
            val anchorChangeHandler = remember(state, scope) {
                SwipeableV2Defaults.ReconcileAnimationOnAnchorChangeHandler(
                    state = state,
                    animate = { target, velocity ->
                        scope.launch {
                            state.animateTo(
                                target,
                                velocity
                            )
                        }
                    },
                    snap = { target -> scope.launch { state.snapTo(target) } }
                )
            }
            Box(
                Modifier
                    .size(size.dp) // Trigger anchor recalculation when size changes
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(A, B, C),
                        anchorChangeHandler = anchorChangeHandler,
                        calculateAnchor = { state, _ -> anchors[state] }
                    )
            )
        }

        assertThat(state.currentValue == A)
        rule.mainClock.autoAdvance = false

        scope.launch { state.animateTo(animationTarget) }

        rule.mainClock.advanceTimeByFrame()
        anchors = secondAnchors
        size = 200
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertThat(state.offset).isEqualTo(secondAnchors.getValue(animationTarget))
    }

    @Test
    fun swipeable_reconcileAnchorChangeHandler_snapsWhenPreviousAnchorRemoved() {
        val state = SwipeableV2State(initialValue = A)
        lateinit var scope: CoroutineScope

        val firstAnchors = mapOf(A to 0f, B to 100f, C to 200f)
        val secondAnchors = mapOf(B to 400f, C to 600f)
        var anchors = firstAnchors
        var size by mutableStateOf(100)

        rule.setContent {
            scope = rememberCoroutineScope()
            val anchorChangeHandler = remember(state, scope) {
                SwipeableV2Defaults.ReconcileAnimationOnAnchorChangeHandler(
                    state = state,
                    animate = { target, velocity ->
                        scope.launch { state.animateTo(target, velocity) }
                    },
                    snap = { target -> scope.launch { state.snapTo(target) } }
                )
            }
            Box(
                Modifier
                    .size(size.dp) // Trigger anchor recalculation when size changes
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(A, B, C),
                        anchorChangeHandler = anchorChangeHandler,
                        calculateAnchor = { state, _ -> anchors[state] }
                    )
            )
        }

        assertThat(state.currentValue == A)

        anchors = secondAnchors
        size = 200
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun swipeable_anchorChangeHandler_calledWithUpdatedAnchorsWhenChanged() {
        val state = SwipeableV2State(initialValue = A)
        val initialSize = 100.dp
        var size: Dp by mutableStateOf(initialSize)
        var anchorChangeHandlerInvocationCount = 0
        var actualPreviousAnchors: Map<TestState, Float>? = null
        var actualNewAnchors: Map<TestState, Float>? = null
        val testChangeHandler = AnchorChangeHandler { _, previousAnchors, newAnchors ->
            anchorChangeHandlerInvocationCount++
            actualPreviousAnchors = previousAnchors
            actualNewAnchors = newAnchors
        }
        fun calculateAnchor(value: TestState, layoutSize: IntSize) = when (value) {
            A -> 0f
            B -> layoutSize.height / 2f
            C -> layoutSize.height.toFloat()
        }
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(size) // Trigger anchor recalculation when size changes
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(A, B, C),
                        anchorChangeHandler = testChangeHandler,
                        calculateAnchor = ::calculateAnchor
                    )
            )
        }

        // The change handler should not get invoked when the anchors are first set
        assertThat(anchorChangeHandlerInvocationCount).isEqualTo(0)

        val expectedPreviousAnchors = state.anchors
        size = 200.dp // Recompose with new size so anchors change
        val sizePx = with(rule.density) { size.roundToPx() }
        val layoutSize = IntSize(sizePx, sizePx)
        val expectedNewAnchors = mapOf(
            A to calculateAnchor(A, layoutSize),
            B to calculateAnchor(B, layoutSize),
            C to calculateAnchor(C, layoutSize),
        )
        rule.waitForIdle()

        assertThat(anchorChangeHandlerInvocationCount).isEqualTo(1)
        assertThat(actualPreviousAnchors).isEqualTo(expectedPreviousAnchors)
        assertThat(actualNewAnchors).isEqualTo(expectedNewAnchors)
    }

    @Test
    fun swipeable_anchorChangeHandler_invokedWithPreviousTarget() {
        val state = SwipeableV2State(
            initialValue = A,
            positionalThreshold = fractionalPositionalThreshold(0.5f)
        )
        var recordedPreviousTargetValue: TestState? = null
        val testChangeHandler = AnchorChangeHandler<TestState> { previousTarget, _, _ ->
            recordedPreviousTargetValue = previousTarget
        }
        var anchors = mapOf(
            A to 0f,
            B to 100f,
            C to 200f
        )
        var recompose by mutableStateOf(false)

        rule.setContent {
            Box(
                key(recompose) {
                    Modifier
                        .swipeAnchors(
                            state = state,
                            possibleValues = setOf(A, B, C),
                            anchorChangeHandler = testChangeHandler,
                            calculateAnchor = { value, _ -> anchors[value] }
                        )
                }
            )
        }

        assertThat(state.targetValue).isEqualTo(A)
        anchors = mapOf(B to 500f)
        recompose = true
        rule.waitForIdle()
        assertThat(recordedPreviousTargetValue).isEqualTo(A) // A is not in the anchors anymore, so
        // we can be sure that is not the targetValue calculated from the new anchors
    }

    @Test
    fun swipeable_anchorChangeHandler_invokedIfInitialValueNotInInitialAnchors() {
        val state = SwipeableV2State(initialValue = A)
        var anchorChangeHandlerInvocationCount = 0
        val testChangeHandler = AnchorChangeHandler<TestState> { _, _, _ ->
            anchorChangeHandlerInvocationCount++
        }
        val anchors = mapOf(B to 100f, C to 200f)

        rule.setContent {
            Box(
                Modifier
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(B, C),
                        anchorChangeHandler = testChangeHandler,
                        calculateAnchor = { value, _ -> anchors[value] }
                    )
            )
        }

        assertThat(anchorChangeHandlerInvocationCount).isEqualTo(1)
    }
}