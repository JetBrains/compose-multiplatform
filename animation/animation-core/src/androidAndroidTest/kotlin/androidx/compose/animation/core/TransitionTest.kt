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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertEquals
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
                {
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
                { tween(durationMillis = 1000) }
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
                        floatAnim1.getValue(transition.playTimeNanos / 1_000_000L),
                        animFloat.value, 0.00001f
                    )
                    assertEquals(
                        colorAnim1.getValue(transition.playTimeNanos / 1_000_000L),
                        animColor.value
                    )
                    assertEquals(
                        keyframesAnim1.getValue(transition.playTimeNanos / 1_000_000L),
                        animFloatWithKeyframes.value, 0.00001f
                    )

                    assertEquals(AnimStates.To, transition.transitionStates.targetState)
                    assertEquals(AnimStates.From, transition.transitionStates.initialState)
                } else {
                    assertEquals(
                        floatAnim2.getValue(transition.playTimeNanos / 1_000_000L),
                        animFloat.value, 0.00001f
                    )
                    assertEquals(
                        colorAnim2.getValue(transition.playTimeNanos / 1_000_000L),
                        animColor.value
                    )
                    assertEquals(
                        keyframesAnim2.getValue(transition.playTimeNanos / 1_000_000L),
                        animFloatWithKeyframes.value, 0.00001f
                    )
                    assertEquals(AnimStates.From, transition.transitionStates.targetState)
                    assertEquals(AnimStates.To, transition.transitionStates.initialState)
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
}