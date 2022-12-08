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

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.fractionalPositionalThreshold
import androidx.compose.material.rememberSwipeableV2State
import androidx.compose.material.swipeAnchors
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterialApi::class)
class SwipeableV2StateTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun swipeable_state_canSkipStateByFling() {
        lateinit var state: SwipeableV2State<TestState>
        rule.setContent {
            state = rememberSwipeableV2State(initialValue = A)
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical,
                possibleStates = setOf(A, B, C)
            )
        }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown() }

        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(C)
    }

    @Test
    fun swipeable_targetState_updatedOnSwipe() {
        lateinit var state: SwipeableV2State<TestState>
        rule.setContent {
            state = rememberSwipeableV2State(initialValue = A)
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical,
                possibleStates = setOf(A, B, C)
            )
        }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(endY = bottom * 0.45f) }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(B)

        // Assert that swipe below threshold upward settles at current state
        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(endY = bottom * 0.95f, durationMillis = 1000) }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(B)

        // Assert that swipe below threshold downward settles at current state
        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(endY = bottom * 0.05f) }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(B)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(endY = bottom * 0.9f) }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(C)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(endY = top * 1.1f) }
        rule.waitForIdle()
        assertThat(state.targetValue).isEqualTo(A)
    }

    @Test
    fun swipeable_targetState_updatedWithAnimation() {
        rule.mainClock.autoAdvance = false
        val animationDuration = 300
        val frameLengthMillis = 16L
        lateinit var state: SwipeableV2State<TestState>
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = remember {
                SwipeableV2State(
                    initialValue = A,
                    animationSpec = tween(animationDuration, easing = LinearEasing),
                    positionalThreshold = fractionalPositionalThreshold(0.5f)
                )
            }
            scope = rememberCoroutineScope()
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical,
                possibleStates = setOf(A, B, C)
            )
        }

        scope.launch {
            state.animateTo(targetValue = B)
        }
        rule.mainClock.advanceTimeBy(1 * frameLengthMillis)

        assertWithMessage("Current state")
            .that(state.currentValue)
            .isEqualTo(A)
        assertWithMessage("Target state")
            .that(state.targetValue)
            .isEqualTo(B)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertWithMessage("Current state")
            .that(state.currentValue)
            .isEqualTo(B)
        assertWithMessage("Target state")
            .that(state.targetValue)
            .isEqualTo(B)
    }

    @Test
    fun swipeable_progress_matchesSwipePosition() {
        lateinit var state: SwipeableV2State<TestState>
        rule.setContent {
            state = rememberSwipeableV2State(initialValue = A)
            WithTouchSlop(touchSlop = 0f) {
                SwipeableBox(
                    swipeableState = state,
                    orientation = Orientation.Vertical
                )
            }
        }

        val anchorA = state.anchors.getValue(A)
        val anchorB = state.anchors.getValue(B)
        val almostAnchorB = anchorB * 0.9f
        var expectedProgress = almostAnchorB / (anchorB - anchorA)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(endY = almostAnchorB) }

        assertThat(state.targetValue).isEqualTo(B)
        assertThat(state.progress).isEqualTo(expectedProgress)

        val almostAnchorA = anchorA + ((anchorB - anchorA) * 0.1f)
        expectedProgress = 1 - (almostAnchorA / (anchorB - anchorA))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(startY = anchorB, endY = almostAnchorA) }

        assertThat(state.targetValue).isEqualTo(A)
        assertThat(state.progress).isEqualTo(expectedProgress)
    }

    @Test
    fun swipeable_snapTo_updatesImmediately() = runBlocking {
        lateinit var state: SwipeableV2State<TestState>
        rule.setContent {
            state = rememberSwipeableV2State(initialValue = A)
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical
            )
        }

        state.snapTo(C)
        assertThat(state.currentValue)
            .isEqualTo(C)
    }

    @Test
    fun swipeable_rememberSwipeableState_restored() {
        val restorationTester = StateRestorationTester(rule)

        val initialState = C
        val animationSpec = tween<Float>(durationMillis = 1000)
        lateinit var state: SwipeableV2State<TestState>
        lateinit var scope: CoroutineScope

        restorationTester.setContent {
            state = rememberSwipeableV2State(initialState, animationSpec)
            state.updateAnchors(mapOf(A to 0f, B to 100f, C to 200f))
            scope = rememberCoroutineScope()
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(state.currentValue).isEqualTo(initialState)
        assertThat(state.animationSpec).isEqualTo(animationSpec)

        scope.launch {
            state.animateTo(B)
        }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(B)

        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun swipeable_targetState_accessedInInitialComposition() {
        lateinit var targetState: TestState
        rule.setContent {
            val state = rememberSwipeableV2State(initialValue = B)
            LaunchedEffect(state.targetValue) {
                targetState = state.targetValue
            }
            SwipeableBox(state)
        }

        assertThat(targetState).isEqualTo(B)
    }

    @Test
    fun swipeable_progress_accessedInInitialComposition() {
        var progress = Float.NaN
        rule.setContent {
            val state = rememberSwipeableV2State(initialValue = B)
            LaunchedEffect(state.progress) {
                progress = state.progress
            }
            SwipeableBox(state)
        }

        assertThat(progress).isEqualTo(1f)
    }

    @Test
    @Ignore("Todo: Fix differences between tests and real code - this shouldn't work :)")
    fun swipeable_requireOffset_accessedInInitialComposition_throws() {
        var exception: Throwable? = null
        lateinit var state: SwipeableV2State<TestState>
        var offset: Float? = null
        rule.setContent {
            state = rememberSwipeableV2State(initialValue = B)
            SwipeableBox(state)
            exception = runCatching { offset = state.requireOffset() }.exceptionOrNull()
        }

        assertThat(state.anchors).isNotEmpty()
        assertThat(offset).isNull()
        assertThat(exception).isNotNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception).hasMessageThat().contains("offset")
    }

    @Test
    @Ignore("LaunchedEffects execute instantly in tests. How can we delay?")
    fun swipeable_requireOffset_accessedInEffect_doesntThrow() {
        var exception: Throwable? = null
        rule.setContent {
            val state = rememberSwipeableV2State(initialValue = B)
            LaunchedEffect(Unit) {
                exception = runCatching { state.requireOffset() }.exceptionOrNull()
            }
        }

        assertThat(exception).isNull()
    }

    @Test
    fun swipeable_animateTo_animatesBeyondBounds() {
        rule.mainClock.autoAdvance = false
        val minBound = 0f
        val maxBound = 500f
        val anchors = mapOf(
            A to minBound,
            C to maxBound
        )

        val animationSpec = FloatSpringSpec(dampingRatio = Spring.DampingRatioHighBouncy)
        val animationDuration = animationSpec.getDurationNanos(
            initialValue = minBound,
            targetValue = maxBound,
            initialVelocity = 0f
        ).let { TimeUnit.NANOSECONDS.toMillis(it) }

        lateinit var state: SwipeableV2State<TestState>
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberSwipeableV2State(
                initialValue = A,
                animationSpec = animationSpec
            )
            SwipeableBox(
                state,
                calculateAnchor = { state, _ -> anchors[state] }
            )
        }
        scope.launch {
            state.animateTo(C)
        }
        var highestOffset = 0f
        for (i in 0..animationDuration step 16) {
            highestOffset = state.requireOffset()
            rule.mainClock.advanceTimeBy(16)
        }
        assertThat(highestOffset).isGreaterThan(anchors.getValue(C))
    }

    @Test
    fun swipeable_bounds_minBoundIsSmallestAnchor() {
        var minBound = 0f
        var maxBound = 500f
        var anchors = mapOf(
            A to minBound,
            B to maxBound / 2,
            C to maxBound
        )
        val state = SwipeableV2State(initialValue = A)
        var size by mutableStateOf(100.dp)

        rule.setContent {
            Box(
                Modifier
                    .size(size)
                    .swipeAnchors(
                        state = state,
                        possibleValues = anchors.keys,
                        calculateAnchor = { state, _ -> anchors[state] }
                    )
            )
        }

        assertThat(state.minOffset).isEqualTo(minBound)
        assertThat(state.maxOffset).isEqualTo(maxBound)

        minBound *= 3
        maxBound *= 10
        anchors = mapOf(
            A to minBound,
            C to maxBound
        )
        size = 200.dp
        rule.waitForIdle()

        assertThat(state.minOffset).isEqualTo(minBound)
        assertThat(state.maxOffset).isEqualTo(maxBound)
    }

    @Test
    fun swipeable_targetNotInAnchors_animateTo_updatesCurrentValue() {
        val state = SwipeableV2State(initialValue = A)
        assertThat(state.anchors).isEmpty()
        assertThat(state.currentValue).isEqualTo(A)
        runBlocking { state.animateTo(B) }
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun swipeable_targetNotInAnchors_snapTo_updatesCurrentValue() {
        val state = SwipeableV2State(initialValue = A)
        assertThat(state.anchors).isEmpty()
        assertThat(state.currentValue).isEqualTo(A)
        runBlocking { state.snapTo(B) }
        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun swipeable_updateAnchors_initialUpdate_initialValueInAnchors_shouldntUpdate() {
        val state = SwipeableV2State(initialValue = A)
        val anchors = mapOf(A to 200f, C to 300f)
        val shouldInvokeChangeHandler = state.updateAnchors(anchors)
        assertThat(shouldInvokeChangeHandler).isFalse()
    }

    @Test
    fun swipeable_updateAnchors_initialUpdate_initialValueNotInAnchors_shouldUpdate() {
        val state = SwipeableV2State(initialValue = A)
        val anchors = mapOf(B to 200f, C to 300f)
        val shouldInvokeChangeHandler = state.updateAnchors(anchors)
        assertThat(shouldInvokeChangeHandler).isTrue()
    }

    @Test
    fun swipeable_updateAnchors_updateExistingAnchors_shouldUpdate() {
        val state = SwipeableV2State(initialValue = A)
        val anchors = mapOf(A to 0f, B to 200f, C to 300f)

        var shouldInvokeChangeHandler = state.updateAnchors(anchors)
        assertThat(shouldInvokeChangeHandler).isFalse()

        shouldInvokeChangeHandler = state.updateAnchors(mapOf(A to 100f, B to 500f, C to 700f))
        assertThat(shouldInvokeChangeHandler).isTrue()
    }
}