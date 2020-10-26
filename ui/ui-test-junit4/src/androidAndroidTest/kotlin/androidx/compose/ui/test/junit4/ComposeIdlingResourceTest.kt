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

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.android.ComposeIdlingResource
import androidx.test.espresso.Espresso.onIdle
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@LargeTest
class ComposeIdlingResourceTest {
    companion object {
        private const val nonIdleDuration = 1000L
        private const val animateFromX = 0f
        private const val animateToX = 50f
        private val rectSize = Size(50.0f, 50.0f)
    }

    private val handler = Handler(Looper.getMainLooper())

    private var animationRunning = false
    private val recordedAnimatedValues = mutableListOf<Float>()
    private var hasRecomposed = false

    @get:Rule
    val rule = createComposeRule()

    /**
     * High level test to only verify that [ComposeTestRule.runOnIdle] awaits animations.
     */
    @Test
    fun testRunOnIdle() {
        val animationState = mutableStateOf(AnimationStates.From)
        rule.setContent { Ui(animationState) }

        rule.runOnIdle {
            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To
        }

        // Verify that animation is kicked off
        assertThat(animationRunning).isTrue()
        // Wait until it is finished
        rule.runOnIdle {
            // Verify it was finished
            assertThat(animationRunning).isFalse()
        }
    }

    /**
     * High level test to only verify that [onIdle] awaits animations.
     */
    @Test
    fun testAnimationIdle_simple() {
        val animationState = mutableStateOf(AnimationStates.From)
        rule.setContent { Ui(animationState) }

        rule.runOnIdle {
            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To
        }

        // Verify that animation is kicked off
        assertThat(animationRunning).isTrue()
        // Wait until it is finished
        onIdle()
        // Verify it was finished
        assertThat(animationRunning).isFalse()
    }

    /**
     * Detailed test to verify if [ComposeIdlingResource.isIdle] reports idleness correctly at
     * key moments during the animation kick-off process.
     */
    @Test
    fun testAnimationIdle_detailed() {
        var wasIdleAfterCommit = false
        var wasIdleAfterRecompose = false
        var wasIdleBeforeKickOff = false
        var wasIdleBeforeCommit = false

        val animationState = mutableStateOf(AnimationStates.From)
        rule.setContent { Ui(animationState) }

        rule.runOnIdle {
            // Record idleness after this frame is committed. The mutation we're about to make
            // will trigger a commit of the frame, which is posted at the front of the handler's
            // queue. By posting a message at the front of the queue here, it will be executed
            // right after the frame commit.
            handler.postAtFrontOfQueue {
                wasIdleAfterCommit = ComposeIdlingResource.isIdle()
            }

            // Record idleness after the next recomposition. Since we can't get a signal from the
            // recomposer, keep polling until we detect we have been recomposed.
            hasRecomposed = false
            handler.pollUntil({ hasRecomposed }) {
                wasIdleAfterRecompose = ComposeIdlingResource.isIdle()
            }

            // Record idleness before kickoff of animation
            wasIdleBeforeKickOff = ComposeIdlingResource.isIdle()

            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To

            // Record idleness after kickoff of animation, but before the frame is committed
            wasIdleBeforeCommit = ComposeIdlingResource.isIdle()
        }

        // Verify that animation is kicked off
        assertThat(animationRunning).isTrue()
        // Wait until it is finished
        onIdle()
        // Verify it was finished
        assertThat(animationRunning).isFalse()

        // Before the animation is kicked off, it is still idle
        assertThat(wasIdleBeforeKickOff).isTrue()
        // After animation is kicked off, but before the frame is committed, it must be busy
        assertThat(wasIdleBeforeCommit).isFalse()
        // After the frame is committed, it must still be busy
        assertThat(wasIdleAfterCommit).isFalse()
        // After recomposition, it must still be busy
        assertThat(wasIdleAfterRecompose).isFalse()
    }

    private fun Handler.pollUntil(condition: () -> Boolean, onDone: () -> Unit) {
        object : Runnable {
            override fun run() {
                if (condition()) {
                    onDone()
                } else {
                    this@pollUntil.post(this)
                }
            }
        }.run()
    }

    @Composable
    private fun Ui(animationState: State<AnimationStates>) {
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
                recordedAnimatedValues.add(state[x])
                drawRect(Color.Cyan, Offset(state[x], 0f), rectSize)
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
            this[x] = animateFromX
        }
        state(AnimationStates.To) {
            this[x] = animateToX
        }
        transition(AnimationStates.From to AnimationStates.To) {
            x using tween(
                easing = LinearEasing,
                durationMillis = nonIdleDuration.toInt()
            )
        }
        transition(AnimationStates.To to AnimationStates.From) {
            x using snap()
        }
    }
}