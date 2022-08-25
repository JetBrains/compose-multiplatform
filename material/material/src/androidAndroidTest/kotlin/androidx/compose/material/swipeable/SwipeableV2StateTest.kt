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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.rememberSwipeableV2State
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        val state = SwipeableV2State(A)
        rule.setContent {
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical,
                possibleStates = setOf(A, B, C)
            )
        }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown() }

        rule.waitForIdle()

        assertThat(state.currentState).isEqualTo(C)
    }

    @Test
    fun swipeable_targetState_updatedOnSwipe() {
        val state = SwipeableV2State(A)
        rule.setContent {
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical,
                possibleStates = setOf(A, B, C)
            )
        }

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(endY = bottom * 0.45f) }
        rule.waitForIdle()
        assertThat(state.targetState).isEqualTo(B)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeDown(endY = bottom * 0.9f) }
        rule.waitForIdle()
        assertThat(state.targetState).isEqualTo(C)

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(endY = top * 1.1f) }
        rule.waitForIdle()
        assertThat(state.targetState).isEqualTo(A)
    }

    @Test
    fun swipeable_targetState_updatedWithAnimation() {
        rule.mainClock.autoAdvance = false
        val animationDuration = 300
        val state = SwipeableV2State(
            initialState = A,
            animationSpec = tween(animationDuration, easing = LinearEasing)
        )
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical,
                possibleStates = setOf(A, B, C)
            )
        }

        scope.launch {
            state.animateTo(targetState = B)
        }
        rule.mainClock.advanceTimeBy((animationDuration * 0.6).toLong())

        assertWithMessage("Current state")
            .that(state.currentState)
            .isEqualTo(A)
        assertWithMessage("Target state")
            .that(state.targetState)
            .isEqualTo(B)

        rule.mainClock.advanceTimeBy((animationDuration * 0.4).toLong())

        assertWithMessage("Current state")
            .that(state.currentState)
            .isEqualTo(B)
        assertWithMessage("Target state")
            .that(state.targetState)
            .isEqualTo(B)
    }

    @Test
    fun swipeable_progress_matchesSwipePosition() {
        val state = SwipeableV2State(A)
        rule.setContent {
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

        assertThat(state.targetState).isEqualTo(B)
        assertThat(state.progress).isEqualTo(expectedProgress)

        val almostAnchorA = anchorA + ((anchorB - anchorA) * 0.1f)
        expectedProgress = 1 - (almostAnchorA / (anchorB - anchorA))

        rule.onNodeWithTag(swipeableTestTag)
            .performTouchInput { swipeUp(startY = anchorB, endY = almostAnchorA) }

        assertThat(state.targetState).isEqualTo(A)
        assertThat(state.progress).isEqualTo(expectedProgress)
    }

    @Test
    fun swipeable_snapTo_updatesImmediately() = runBlocking {
        val state = SwipeableV2State(A)
        rule.setContent {
            SwipeableBox(
                swipeableState = state,
                orientation = Orientation.Vertical
            )
        }

        state.snapTo(C)
        assertThat(state.currentState)
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

        assertThat(state.currentState).isEqualTo(initialState)
        assertThat(state.animationSpec).isEqualTo(animationSpec)

        scope.launch {
            state.animateTo(B)
        }
        rule.waitForIdle()
        assertThat(state.currentState).isEqualTo(B)

        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state.currentState).isEqualTo(B)
    }

    @Test
    fun swipeable_targetState_accessedInInitialComposition() {
        lateinit var targetState: TestState
        rule.setContent {
            val state = rememberSwipeableV2State(initialState = B)
            LaunchedEffect(state.targetState) {
                targetState = state.targetState
            }
            SwipeableBox(state)
        }

        assertThat(targetState).isEqualTo(B)
    }

    @Test
    fun swipeable_progress_accessedInInitialComposition() {
        var progress = Float.NaN
        rule.setContent {
            val state = rememberSwipeableV2State(initialState = B)
            LaunchedEffect(state.progress) {
                progress = state.progress
            }
            SwipeableBox(state)
        }

        assertThat(progress).isEqualTo(1f)
    }
}