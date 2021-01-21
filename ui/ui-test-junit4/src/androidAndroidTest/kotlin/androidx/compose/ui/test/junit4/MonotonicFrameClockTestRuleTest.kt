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

package androidx.compose.ui.test.junit4

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@LargeTest
@OptIn(ExperimentalTestApi::class)
class MonotonicFrameClockTestRuleTest {

    companion object {
        private const val startValue = 0f
        private const val endValue = 50f
        private const val duration = 1000L
    }

    private var animationRunning = false
    private var hasRecomposed = false

    @get:Rule
    val rule = createAndroidComposeRule(driveClockByMonotonicFrameClock = true)

    /**
     * Tests if advancing the clock manually works when the clock is resumed, and that idleness
     * is reported correctly when doing that.
     */
    @Test
    fun test() {
        val animationState = mutableStateOf(AnimationStates.From)
        rule.setContent {
            Ui(animationState)
        }

        rule.runOnIdle {
            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To
        }

        // Perform a single recomposition by advancing one frame
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            // After the animation is finished, ...
            assertThat(animationRunning).isFalse()
        }
    }

    @Composable
    private fun Ui(animationState: State<AnimationStates>) {
        val size = Size(50.0f, 50.0f)
        hasRecomposed = true
        Box(modifier = Modifier.background(color = Color.Yellow).fillMaxSize()) {
            hasRecomposed = true
            val transition = updateTransition(animationState.value)
            animationRunning = transition.currentState != transition.targetState
            val x by transition.animateFloat(
                transitionSpec = {
                    if (AnimationStates.From isTransitioningTo AnimationStates.To) {
                        tween(
                            easing = LinearEasing,
                            durationMillis = duration.toInt()
                        )
                    } else {
                        snap()
                    }
                }
            ) {
                if (it == AnimationStates.From) {
                    startValue
                } else {
                    endValue
                }
            }
            hasRecomposed = true
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color.Cyan, Offset(x, 0.0f), size)
            }
        }
    }

    private enum class AnimationStates {
        From,
        To
    }
}