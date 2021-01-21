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

import android.os.Looper
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.State
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.android.ComposeIdlingResource
import androidx.test.espresso.Espresso.onIdle
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
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

    private var animationRunning = false
    private val recordedAnimatedValues = mutableListOf<Float>()

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val composeIdlingResource = rule.composeIdlingResource

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
     * Detailed test to verify if [ComposeIdlingResource.isIdleNow] reports idleness correctly at
     * key moments during the animation kick-off process.
     */
    @Test
    @Ignore("b/173798666: Idleness not detected after Snapshot.sendApplyNotifications()")
    fun testAnimationIdle_detailed() {
        var wasIdleBeforeKickOff = false
        var wasIdleBeforeApplySnapshot = false
        var wasIdleAfterApplySnapshot = false

        val animationState = mutableStateOf(AnimationStates.From)
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            Ui(animationState)
        }

        runBlocking(scope.coroutineContext) {
            // Verify that we're on the main thread, which is important for isIdle() later
            assertThat(Looper.myLooper()).isEqualTo(Looper.getMainLooper())
        }

        val wasIdleAfterRecompose = rule.runOnIdle {
            // Record idleness before kickoff of animation
            wasIdleBeforeKickOff = composeIdlingResource.isIdleNow

            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To

            // Record idleness after kickoff of animation, but before the snapshot is applied
            wasIdleBeforeApplySnapshot = composeIdlingResource.isIdleNow

            // Apply the snapshot
            @OptIn(ExperimentalComposeApi::class)
            Snapshot.sendApplyNotifications()

            // Record idleness after this snapshot is applied
            wasIdleAfterApplySnapshot = composeIdlingResource.isIdleNow

            // Record idleness after the first recomposition
            @OptIn(ExperimentalCoroutinesApi::class)
            scope.async(start = CoroutineStart.UNDISPATCHED) {
                // Await a single recomposition
                withFrameNanos {}
                composeIdlingResource.isIdleNow
            }
        }.let {
            runBlocking {
                it.await()
            }
        }

        // Wait until it is finished
        rule.runOnIdle {
            // Verify it was finished
            assertThat(animationRunning).isFalse()

            // Before the animation is kicked off, it is still idle
            assertThat(wasIdleBeforeKickOff).isTrue()
            // After animation is kicked off, but before the frame is committed, it must be busy
            assertThat(wasIdleBeforeApplySnapshot).isFalse()
            // After the frame is committed, it must still be busy
            assertThat(wasIdleAfterApplySnapshot).isFalse()
            // After recomposition, it must still be busy
            assertThat(wasIdleAfterRecompose).isFalse()
        }
    }

    @Composable
    private fun Ui(animationState: State<AnimationStates>) {
        Box(modifier = Modifier.background(color = Color.Yellow).fillMaxSize()) {
            val transition = updateTransition(animationState.value)
            animationRunning = transition.currentState != transition.targetState
            val x by transition.animateFloat(
                transitionSpec = {
                    if (AnimationStates.From isTransitioningTo AnimationStates.To) {
                        tween(
                            easing = LinearEasing,
                            durationMillis = nonIdleDuration.toInt()
                        )
                    } else {
                        snap()
                    }
                }
            ) {
                if (it == AnimationStates.From) {
                    animateFromX
                } else {
                    animateToX
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                recordedAnimatedValues.add(x)
                drawRect(Color.Cyan, Offset(x, 0f), rectSize)
            }
        }
    }
}

private enum class AnimationStates {
    From,
    To
}