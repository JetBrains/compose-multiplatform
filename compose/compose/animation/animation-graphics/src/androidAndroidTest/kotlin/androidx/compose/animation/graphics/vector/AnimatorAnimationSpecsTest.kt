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

package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatorAnimationSpecsTest {

    @get:Rule
    val rule = createComposeRule()

    private val tolerance = 0.01f

    @OptIn(InternalAnimationApi::class)
    @Test
    fun reversed_tween() {
        val isAtEnd = mutableStateOf(false)
        rule.setContent {
            val transition = updateTransition(targetState = isAtEnd.value, label = "test")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) {
                if (it) 1000f else 0f
            }
            val reversed = transition.animateFloat(
                label = "reversed",
                transitionSpec = {
                    tween<Float>(durationMillis = 1000, easing = LinearEasing).reversed(1000)
                }
            ) {
                if (it) 1000f else 0f
            }
            assertWithMessage("at playTimeNanos: ${transition.playTimeNanos}")
                .that(reversed.value).isWithin(tolerance).of(control.value)
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun reversed_keyframes() {
        val isAtEnd = mutableStateOf(false)
        rule.setContent {
            val transition = updateTransition(targetState = isAtEnd.value, label = "test")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    keyframes {
                        durationMillis = 1000
                        0f at 0 with LinearEasing
                        100f at 100 with LinearEasing
                        1000f at 1000 with LinearEasing
                    }
                }
            ) {
                if (it) 1000f else 0f
            }
            val reversed = transition.animateFloat(
                label = "reversed",
                transitionSpec = {
                    keyframes<Float> {
                        durationMillis = 1000
                        1000f at 0 with LinearEasing
                        100f at 900 with LinearEasing
                        0f at 1000 with LinearEasing
                    }.reversed(1000)
                }
            ) {
                if (it) 1000f else 0f
            }
            assertWithMessage("at playTimeNanos: ${transition.playTimeNanos}")
                .that(reversed.value).isWithin(tolerance).of(control.value)
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun reversed_keyframes_delay() {
        val isAtEnd = mutableStateOf(false)
        rule.setContent {
            val transition = updateTransition(targetState = isAtEnd.value, label = "test")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    keyframes {
                        durationMillis = 1000
                        0f at 0 with LinearEasing
                        1000f at 500 with LinearEasing
                    }
                }
            ) {
                if (it) 1000f else 0f
            }
            val reversed = transition.animateFloat(
                label = "reversed",
                transitionSpec = {
                    keyframes<Float> {
                        durationMillis = 1000
                        1000f at 0 with LinearEasing
                        1000f at 500 with LinearEasing
                        0f at 1000 with LinearEasing
                    }.reversed(1000)
                }
            ) {
                if (it) 1000f else 0f
            }
            assertWithMessage("at playTimeNanos: ${transition.playTimeNanos}")
                .that(reversed.value).isWithin(tolerance).of(control.value)
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun combined_single() {
        val isAtEnd = mutableStateOf(false)
        rule.setContent {
            val transition = updateTransition(targetState = isAtEnd.value, label = "test")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }
            ) {
                if (it) 1000f else 0f
            }
            val combined = transition.animateFloat(
                label = "combined",
                transitionSpec = {
                    combined(
                        listOf(
                            0 to tween(durationMillis = 1000, easing = LinearEasing)
                        )
                    )
                }
            ) {
                if (it) 1000f else 0f
            }
            assertWithMessage("at playTimeNanos: ${transition.playTimeNanos}")
                .that(combined.value).isWithin(tolerance).of(control.value)
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun combined_multiple() {
        val isAtEnd = mutableStateOf(false)
        rule.setContent {
            val transition = updateTransition(targetState = isAtEnd.value, label = "test")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    keyframes {
                        durationMillis = 1000
                        0f at 0 with LinearEasing
                        1000f at 1000 with LinearEasing
                    }
                }
            ) {
                if (it) 1000f else 0f
            }
            val combined = transition.animateFloat(
                label = "combined",
                transitionSpec = {
                    combined(
                        listOf(
                            0 to keyframes {
                                durationMillis = 300
                                0f at 0 with LinearEasing
                                300f at 300 with LinearEasing
                            },
                            300 to keyframes {
                                durationMillis = 700
                                300f at 0 with LinearEasing
                                1000f at 700 with LinearEasing
                            }
                        )
                    )
                }
            ) {
                if (it) 1000f else 0f
            }
            assertWithMessage("at playTimeNanos: ${transition.playTimeNanos}")
                .that(combined.value).isWithin(tolerance).of(control.value)
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
    }
}
