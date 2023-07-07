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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

@LargeTest
@OptIn(ExperimentalTestApi::class)
class ComposeIdlingResourceTest {
    companion object {
        private const val nonIdleDuration = 1000L
        private const val animateFromX = 0f
        private const val animateToX = 50f
        private val rectSize = Size(50.0f, 50.0f)
    }

    private var animationRunning = false
    private val recordedAnimatedValues = mutableListOf<Float>()

    /**
     * High level test to only verify that [ComposeUiTest.runOnIdle] awaits animations.
     */
    @Test
    fun testRunOnIdle() = runComposeUiTest {
        val animationState = mutableStateOf(AnimationStates.From)
        setContent { Ui(animationState) }

        runOnIdle {
            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To
        }

        // Verify that animation is kicked off
        assertThat(animationRunning).isTrue()
        // Wait until it is finished
        runOnIdle {
            // Verify it was finished
            assertThat(animationRunning).isFalse()
        }
    }

    /**
     * High level test to only verify that [Espresso.onIdle] awaits animations.
     */
    @Test
    fun testAnimationIdle_simple() = runComposeUiTest {
        val animationState = mutableStateOf(AnimationStates.From)
        setContent { Ui(animationState) }

        runOnIdle {
            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To
        }

        // Verify that animation is kicked off
        assertThat(animationRunning).isTrue()
        // Wait until it is finished
        Espresso.onIdle()
        // Verify it was finished
        assertThat(animationRunning).isFalse()
    }

    @Test
    fun testIdlingResourcesAreQueried() = runComposeUiTest {
        val idlingResource = object : IdlingResource {
            var readCount = MutableStateFlow(0)

            override var isIdleNow: Boolean = false
                get() {
                    readCount.value++
                    return field
                }

            // Returns a lambda that suspends until isIdleNow is queried 10 more times
            fun delayedTransitionToIdle(): () -> Unit {
                return {
                    runBlocking {
                        val start = readCount.value
                        readCount.first { it == start + 10 }
                        isIdleNow = true
                    }
                }
            }
        }

        registerIdlingResource(idlingResource)
        Executors.newSingleThreadExecutor().execute(idlingResource.delayedTransitionToIdle())

        val startReadCount = idlingResource.readCount.value
        waitForIdle()
        val endReadCount = idlingResource.readCount.value

        assertThat(idlingResource.isIdleNow).isTrue()
        assertThat(endReadCount - startReadCount).isAtLeast(10)
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