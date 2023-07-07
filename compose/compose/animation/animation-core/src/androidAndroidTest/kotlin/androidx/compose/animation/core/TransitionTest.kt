/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.animateColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TransitionTest {

    @get:Rule
    val rule = createComposeRule()

    private enum class AnimStates {
        From,
        To
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun transitionTest() {
        val target = mutableStateOf(AnimStates.From)
        val floatAnim1 = TargetBasedAnimation(
            spring(dampingRatio = Spring.DampingRatioHighBouncy),
            Float.VectorConverter,
            0f,
            1f
        )
        val floatAnim2 = TargetBasedAnimation(
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            Float.VectorConverter,
            1f,
            0f
        )

        val colorAnim1 = TargetBasedAnimation(
            tween(1000),
            Color.VectorConverter(Color.Red.colorSpace),
            Color.Red,
            Color.Green
        )
        val colorAnim2 = TargetBasedAnimation(
            tween(1000),
            Color.VectorConverter(Color.Red.colorSpace),
            Color.Green,
            Color.Red,
        )

        // Animate from 0f to 0f for 1000ms
        val keyframes1 = keyframes<Float> {
            durationMillis = 1000
            0f at 0
            200f at 400
            1000f at 1000
        }

        val keyframes2 = keyframes<Float> {
            durationMillis = 800
            0f at 0
            -500f at 400
            -1000f at 800
        }

        val keyframesAnim1 = TargetBasedAnimation(
            keyframes1,
            Float.VectorConverter,
            0f,
            0f
        )
        val keyframesAnim2 = TargetBasedAnimation(
            keyframes2,
            Float.VectorConverter,
            0f,
            0f
        )
        val animFloat = mutableStateOf(-1f)
        val animColor = mutableStateOf(Color.Gray)
        val animFloatWithKeyframes = mutableStateOf(-1f)
        rule.setContent {
            val transition = updateTransition(target.value)
            animFloat.value = transition.animateFloat(
                transitionSpec = {
                    if (AnimStates.From isTransitioningTo AnimStates.To) {
                        spring(dampingRatio = Spring.DampingRatioHighBouncy)
                    } else {
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    }
                }
            ) {
                when (it) {
                    AnimStates.From -> 0f
                    AnimStates.To -> 1f
                }
            }.value

            animColor.value = transition.animateColor(
                transitionSpec = { tween(durationMillis = 1000) }
            ) {
                when (it) {
                    AnimStates.From -> Color.Red
                    AnimStates.To -> Color.Green
                }
            }.value

            animFloatWithKeyframes.value = transition.animateFloat(
                transitionSpec = {
                    if (AnimStates.From isTransitioningTo AnimStates.To) {
                        keyframes1
                    } else {
                        keyframes2
                    }
                }
            ) {
                // Same values for all states, but different transitions from state to state.
                0f
            }.value

            if (transition.isRunning) {
                if (transition.targetState == AnimStates.To) {
                    assertEquals(
                        floatAnim1.getValueFromNanos(transition.playTimeNanos),
                        animFloat.value, 0.00001f
                    )
                    assertEquals(
                        colorAnim1.getValueFromNanos(transition.playTimeNanos),
                        animColor.value
                    )
                    assertEquals(
                        keyframesAnim1.getValueFromNanos(transition.playTimeNanos),
                        animFloatWithKeyframes.value, 0.00001f
                    )

                    assertEquals(AnimStates.To, transition.segment.targetState)
                    assertEquals(AnimStates.From, transition.segment.initialState)
                } else {
                    assertEquals(
                        floatAnim2.getValueFromNanos(transition.playTimeNanos),
                        animFloat.value, 0.00001f
                    )
                    assertEquals(
                        colorAnim2.getValueFromNanos(transition.playTimeNanos),
                        animColor.value
                    )
                    assertEquals(
                        keyframesAnim2.getValueFromNanos(transition.playTimeNanos),
                        animFloatWithKeyframes.value, 0.00001f
                    )
                    assertEquals(AnimStates.From, transition.segment.targetState)
                    assertEquals(AnimStates.To, transition.segment.initialState)
                }
            }
        }

        assertEquals(0f, animFloat.value)
        assertEquals(Color.Red, animColor.value)
        rule.runOnIdle {
            target.value = AnimStates.To
        }
        rule.waitForIdle()

        assertEquals(1f, animFloat.value)
        assertEquals(Color.Green, animColor.value)

        // Animate back to the `from` state
        rule.runOnIdle {
            target.value = AnimStates.From
        }
        rule.waitForIdle()

        assertEquals(0f, animFloat.value)
        assertEquals(Color.Red, animColor.value)
    }

    @Test
    fun startPulsingNextFrameTest() {
        val target = mutableStateOf(AnimStates.From)
        var playTime by mutableStateOf(0L)
        rule.setContent {
            val transition = updateTransition(target.value)
            val actual = transition.animateFloat(
                transitionSpec = { tween(200) }
            ) {
                if (it == AnimStates.From) 0f else 1000f
            }

            val anim = TargetBasedAnimation(tween(200), Float.VectorConverter, 0f, 1000f)

            if (target.value == AnimStates.To) {
                LaunchedEffect(transition) {
                    val startTime = withFrameNanos { it }

                    assertEquals(0f, actual.value)
                    do {
                        playTime = withFrameNanos { it } - startTime
                        assertEquals(anim.getValueFromNanos(playTime), actual.value)
                    } while (playTime <= 200 * MillisToNanos)
                }
            }
        }

        rule.runOnIdle {
            target.value = AnimStates.To
        }
        rule.waitForIdle()
        assertTrue(playTime > 200 * MillisToNanos)
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun addNewAnimationInFlightTest() {
        val target = mutableStateOf(AnimStates.From)
        var playTime by mutableStateOf(0L)
        rule.setContent {
            val transition = updateTransition(target.value)

            transition.animateFloat(
                transitionSpec = { tween(1000) }
            ) {
                if (it == AnimStates.From) -100f else 0f
            }

            if (transition.playTimeNanos > 0) {
                val startTime = remember { transition.playTimeNanos }
                val laterAdded = transition.animateFloat(
                    transitionSpec = { tween(800) }
                ) {
                    if (it == AnimStates.From) 0f else 1000f
                }
                val anim = TargetBasedAnimation(tween(800), Float.VectorConverter, 0f, 1000f)
                playTime = transition.playTimeNanos - startTime
                assertEquals(anim.getValueFromNanos(playTime), laterAdded.value)
            }
        }

        rule.runOnIdle {
            target.value = AnimStates.To
        }
        rule.waitForIdle()
        assertTrue(playTime > 800 * MillisToNanos)
    }

    @Test
    fun initialStateTest() {
        val target = MutableTransitionState(AnimStates.From)
        target.targetState = AnimStates.To
        var playTime by mutableStateOf(0L)
        var floatAnim: State<Float>? = null
        rule.setContent {
            val transition = updateTransition(target)
            floatAnim = transition.animateFloat(
                transitionSpec = { tween(800) }
            ) {
                if (it == AnimStates.From) 0f else 1000f
            }
            // Verify that animation starts right away
            LaunchedEffect(transition) {
                val startTime = withFrameNanos { it }
                val anim = TargetBasedAnimation(tween(800), Float.VectorConverter, 0f, 1000f)
                while (!anim.isFinishedFromNanos(playTime)) {
                    playTime = withFrameNanos { it } - startTime
                    assertEquals(anim.getValueFromNanos(playTime), floatAnim?.value)
                }
            }
        }
        rule.waitForIdle()
        assertTrue(playTime >= 800 * MillisToNanos)
        assertEquals(1000f, floatAnim?.value)
    }

    @Test
    fun recreatingMutableStatesAmidTransition() {
        var playTime by mutableStateOf(0L)
        var targetRecreated by mutableStateOf(false)
        rule.setContent {
            var target by remember { mutableStateOf(MutableTransitionState(AnimStates.From)) }
            target.targetState = AnimStates.To
            val transition = updateTransition(target)
            val floatAnim = transition.animateFloat(
                transitionSpec = { tween(800) }
            ) {
                if (it == AnimStates.From) 0f else 1000f
            }
            LaunchedEffect(Unit) {
                delay(100)
                target = MutableTransitionState(AnimStates.From)
                target.targetState = AnimStates.To
                targetRecreated = true
            }

            if (targetRecreated) {
                LaunchedEffect(transition) {
                    // Verify that animation restarted
                    assertEquals(0f, floatAnim.value)

                    val startTime = withFrameNanos { it }
                    val anim = TargetBasedAnimation(tween(800), Float.VectorConverter, 0f, 1000f)
                    while (!anim.isFinishedFromNanos(playTime)) {
                        playTime = withFrameNanos { it } - startTime
                        assertEquals(anim.getValueFromNanos(playTime), floatAnim.value)
                    }
                }
            }
        }

        rule.waitForIdle()
        assertTrue(targetRecreated)
        assertTrue(playTime >= 800 * MillisToNanos)
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Test
    fun testMutableTransitionStateIsIdle() {
        val mutableTransitionState = MutableTransitionState(false)
        var transition: Transition<Boolean>? = null
        rule.setContent {
            transition = updateTransition(mutableTransitionState).apply {
                animateFloat {
                    if (it) 1f else 0f
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            assertTrue(mutableTransitionState.isIdle)
            mutableTransitionState.targetState = true
            assertFalse(mutableTransitionState.isIdle)
        }

        while (transition?.currentState != true) {
            // Animation has not finished or even started from Transition's perspective
            assertFalse(mutableTransitionState.isIdle)
            rule.mainClock.advanceTimeByFrame()
        }
        assertTrue(mutableTransitionState.isIdle)

        // Now that transition false -> true finished, go back to false
        rule.runOnIdle {
            assertTrue(mutableTransitionState.isIdle)
            mutableTransitionState.targetState = false
            assertFalse(mutableTransitionState.isIdle)
        }

        while (transition?.currentState == true) {
            // Animation has not finished or even started from Transition's perspective
            assertFalse(mutableTransitionState.isIdle)
            rule.mainClock.advanceTimeByFrame()
        }
        assertTrue(mutableTransitionState.isIdle)
    }

    @OptIn(ExperimentalTransitionApi::class, InternalAnimationApi::class)
    @Test
    fun testCreateChildTransition() {
        val intState = mutableStateOf(1)
        val parentTransitionFloat = mutableStateOf(1f)
        val childTransitionFloat = mutableStateOf(1f)
        rule.setContent {
            val transition = updateTransition(intState.value)
            parentTransitionFloat.value = transition.animateFloat({ tween(100) }) {
                when (it) {
                    0 -> 0f
                    1 -> 1f
                    else -> 2f
                }
            }.value
            val booleanTransition = transition.createChildTransition {
                it == 1
            }
            childTransitionFloat.value = booleanTransition.animateFloat({ tween(500) }) {
                if (it) 1f else 0f
            }.value
            LaunchedEffect(intState.value) {
                while (true) {
                    if (transition.targetState == transition.currentState) {
                        break
                    }
                    withFrameNanos { it }
                    if (intState.value == 0) {
                        // 1 -> 0
                        if (
                            transition.playTimeNanos > 0 && transition.playTimeNanos <= 500_000_000L
                        ) {
                            assertTrue(transition.isRunning)
                            assertTrue(booleanTransition.isRunning)
                        }
                    } else if (intState.value == 2) {
                        // 0 -> 2
                        assertFalse(booleanTransition.isRunning)
                        if (transition.playTimeNanos > 120_000_000L) {
                            assertFalse(transition.isRunning)
                        } else if (transition.playTimeNanos > 0) {
                            assertTrue(transition.isRunning)
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1f, parentTransitionFloat.value)
            assertEquals(1f, childTransitionFloat.value)
            intState.value = 0
        }
        rule.runOnIdle {
            assertEquals(0f, parentTransitionFloat.value)
            assertEquals(0f, childTransitionFloat.value)
            intState.value = 2
        }
        rule.runOnIdle {
            assertEquals(2f, parentTransitionFloat.value)
            assertEquals(0f, childTransitionFloat.value)
        }
    }
}