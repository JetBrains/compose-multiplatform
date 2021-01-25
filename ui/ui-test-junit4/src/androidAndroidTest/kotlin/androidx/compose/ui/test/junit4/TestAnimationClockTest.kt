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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.test.junit4

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.State
import androidx.compose.runtime.dispatch.AndroidUiDispatcher
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.espresso.Espresso.onIdle
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

@LargeTest
@OptIn(ExperimentalTestApi::class)
class TestAnimationClockTest {

    companion object {
        private const val startValue = 0f
        private const val middleValue = 25f
        private const val endValue = 50f
        private const val duration = 1000L
        private const val halfDuration = 500L
    }

    private var animationRunning = false
    private val recordedAnimatedValues = mutableListOf<Float>()
    private var hasRecomposed = false

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = createAndroidComposeRuleLegacy<ComponentActivity>()
    @Suppress("DEPRECATION")
    private val clockTestRule = rule.clockTestRule
    private val composeIdlingResource = rule.composeIdlingResource

    /**
     * Tests if advancing the clock manually works when the clock is paused, and that idleness is
     * reported correctly when doing that.
     */
    @Test
    fun testAnimation_manuallyAdvanceClock_paused() {
        clockTestRule.pauseClock()

        val animationState = mutableStateOf(AnimationStates.From)
        rule.setContent { Ui(animationState) }

        rule.runOnIdle {
            recordedAnimatedValues.clear()

            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To

            // Changes need to trickle down the animation system, so compose should be non-idle
            assertThat(composeIdlingResource.isIdleNow).isFalse()
        }

        // Await recomposition
        onIdle()

        // Did one recomposition, but no animation frames
        recordedAnimatedValues.forEach {
            assertThat(it).isIn(listOf(startValue))
        }

        // Advance first half of the animation (.5 sec)
        rule.runOnIdle {
            clockTestRule.advanceClock(halfDuration)
            assertThat(composeIdlingResource.isIdleNow).isFalse()
        }

        // Await next animation frame
        onIdle()

        // Did one animation frame
        recordedAnimatedValues.forEach {
            assertThat(it).isIn(listOf(startValue, middleValue))
        }

        // Advance second half of the animation (.5 sec)
        rule.runOnIdle {
            clockTestRule.advanceClock(halfDuration)
            assertThat(composeIdlingResource.isIdleNow).isFalse()
        }

        // Await next animation frame
        onIdle()

        // Did last animation frame
        assertThat(animationRunning).isFalse()
        recordedAnimatedValues.forEach {
            assertThat(it).isIn(listOf(startValue, middleValue, endValue))
        }
    }

    /**
     * Tests if advancing the clock manually works when the clock is resumed, and that idleness
     * is reported correctly when doing that.
     */
    @Test
    fun testAnimation_manuallyAdvanceClock_resumed() = runBlocking {
        val animationState = mutableStateOf(AnimationStates.From)
        rule.setContent {
            Ui(animationState)
        }

        // Before we kick off the animation, the test clock should be idle
        assertThat(clockTestRule.clock.isIdle).isTrue()

        rule.runOnIdle {
            recordedAnimatedValues.clear()

            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To

            // Changes need to trickle down the animation system, so compose should be non-idle
            assertThat(composeIdlingResource.isIdleNow).isFalse()
        }

        // Perform a single recomposition by awaiting the same signal as the Recomposer
        withContext(AndroidUiDispatcher.Main) { withFrameNanos {} }

        // After we kicked off the animation, the test clock should be non-idle
        assertThat(clockTestRule.clock.isIdle).isFalse()

        // Animation is running, fast-forward it because we don't want to wait on it
        clockTestRule.advanceClock(duration)

        // Force the clock forwarding through the pipeline
        // Avoid synchronization steps when doing this: if we would synchronize, we would never
        // know it if advanceClock didn't work.
        rule.runOnUiThread {
            @OptIn(ExperimentalComposeApi::class)
            Snapshot.sendApplyNotifications()
        }

        // After the animation is finished, ...
        assertThat(animationRunning).isFalse()
        // ... the test clock should be idle again
        assertThat(clockTestRule.clock.isIdle).isTrue()

        // Animation values are recorded in draw, so wait until we've drawn
        onIdle()

        // So the clock has now been pumped both by the test and the Choreographer, so there is
        // really no way to tell in which states the animation has been rendered. Only that we
        // rendered the final state.
        assertThat(recordedAnimatedValues).contains(endValue)
    }

    @Composable
    private fun Ui(animationState: State<AnimationStates>) {
        val size = Size(50.0f, 50.0f)
        hasRecomposed = true
        Box(modifier = Modifier.background(color = Color.Yellow).fillMaxSize()) {
            hasRecomposed = true
            val state = transition(
                definition = animationDefinition,
                toState = animationState.value,
                onStateChangeFinished = { animationRunning = false }
            )
            hasRecomposed = true
            Canvas(modifier = Modifier.fillMaxSize()) {
                val xValue = state[x]
                recordedAnimatedValues.add(xValue)
                drawRect(Color.Cyan, Offset(xValue, 0.0f), size)
            }
        }
    }

    private val x = FloatPropKey()

    private enum class AnimationStates {
        From,
        To
    }

    private val animationDefinition = transitionDefinition<AnimationStates> {
        state(AnimationStates.From) {
            this[x] = startValue
        }
        state(AnimationStates.To) {
            this[x] = endValue
        }
        transition(AnimationStates.From to AnimationStates.To) {
            x using tween(
                easing = LinearEasing,
                durationMillis = duration.toInt()
            )
        }
        transition(AnimationStates.To to AnimationStates.From) {
            x using snap()
        }
    }
}