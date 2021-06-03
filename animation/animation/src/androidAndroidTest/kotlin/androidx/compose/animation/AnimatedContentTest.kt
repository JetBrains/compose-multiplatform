/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalTestApi::class)
class AnimatedContentTest {

    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
    @Test
    fun AnimatedContentSizeTransformTest() {
        val size1 = 40
        val size2 = 200
        val testModifier by mutableStateOf(TestModifier())
        val transitionState = MutableTransitionState(true)
        var playTimeMillis by mutableStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = updateTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    testModifier,
                    transitionSpec = {
                        if (true isTransitioningTo false) {
                            fadeIn() with fadeOut() using SizeTransform { initialSize, targetSize ->
                                keyframes {
                                    durationMillis = 320
                                    IntSize(targetSize.width, initialSize.height) at 160 with
                                        LinearEasing
                                    targetSize at 320 with LinearEasing
                                }
                            }
                        } else {
                            fadeIn() with fadeOut() using SizeTransform { _, _ ->
                                tween(durationMillis = 80, easing = LinearEasing)
                            }
                        }
                    }
                ) {
                    if (it) {
                        Box(modifier = Modifier.size(size = size1.dp))
                    } else {
                        Box(modifier = Modifier.size(size = size2.dp))
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(40, testModifier.height)
            assertEquals(40, testModifier.width)
            assertTrue(transitionState.targetState)
            transitionState.targetState = false
        }

        // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
        // then full height in the next 160ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                if (playTimeMillis <= 160) {
                    assertEquals(playTimeMillis + 40, testModifier.width)
                    assertEquals(40, testModifier.height)
                } else {
                    assertEquals(200, testModifier.width)
                    assertEquals(playTimeMillis - 120, testModifier.height)
                }
            }
            rule.mainClock.advanceTimeByFrame()
        }

        rule.runOnIdle {
            assertEquals(200, testModifier.width)
            assertEquals(200, testModifier.height)
            transitionState.targetState = true
        }

        // Transition from item2 to item1 in 80ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                if (playTimeMillis <= 80) {
                    assertEquals(200 - playTimeMillis * 2, testModifier.width)
                    assertEquals(200 - playTimeMillis * 2, testModifier.height)
                }
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
    @Test
    fun AnimatedContentSizeTransformEmptyComposableTest() {
        val size1 = 160
        val testModifier by mutableStateOf(TestModifier())
        val transitionState = MutableTransitionState(true)
        var playTimeMillis by mutableStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = updateTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    testModifier,
                    transitionSpec = {
                        EnterTransition.None with ExitTransition.None using SizeTransform { _, _ ->
                            tween(durationMillis = 160, easing = LinearEasing)
                        }
                    }
                ) {
                    if (it) {
                        Box(modifier = Modifier.size(size = size1.dp))
                    }
                    // Empty composable for it == false
                }
            }
        }
        rule.runOnIdle {
            assertEquals(160, testModifier.height)
            assertEquals(160, testModifier.width)
            assertTrue(transitionState.targetState)
            transitionState.targetState = false
        }

        // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
        // then full height in the next 160ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                assertEquals(160 - playTimeMillis, testModifier.width)
                assertEquals(160 - playTimeMillis, testModifier.height)
            }
            rule.mainClock.advanceTimeByFrame()
        }

        // Now there's only an empty composable
        rule.runOnIdle {
            assertEquals(0, testModifier.width)
            assertEquals(0, testModifier.height)
            transitionState.targetState = true
        }

        // Transition from item2 to item1 in 80ms
        while (transitionState.currentState != transitionState.targetState) {
            rule.runOnIdle {
                assertEquals(playTimeMillis, testModifier.width)
                assertEquals(playTimeMillis, testModifier.height)
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
    @Test
    fun AnimatedContentContentAlignmentTest() {
        val size1 = IntSize(80, 80)
        val size2 = IntSize(160, 240)
        val testModifier by mutableStateOf(TestModifier())
        var offset1 by mutableStateOf(Offset.Zero)
        var offset2 by mutableStateOf(Offset.Zero)
        var playTimeMillis by mutableStateOf(0)
        val transitionState = MutableTransitionState(true)
        val alignment = listOf(
            Alignment.TopStart, Alignment.BottomStart, Alignment.Center,
            Alignment.BottomEnd, Alignment.TopEnd
        )
        var contentAlignment by mutableStateOf(Alignment.TopStart)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val transition = updateTransition(transitionState)
                playTimeMillis = (transition.playTimeNanos / 1_000_000L).toInt()
                transition.AnimatedContent(
                    testModifier,
                    contentAlignment = contentAlignment,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 80)) with fadeOut(
                            animationSpec = tween(durationMillis = 80)
                        ) using SizeTransform { _, _ ->
                            tween(durationMillis = 80, easing = LinearEasing)
                        }
                    }
                ) {
                    if (it) {
                        Box(
                            modifier = Modifier.onGloballyPositioned {
                                offset1 = it.positionInRoot()
                            }.size(size1.width.dp, size1.height.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.onGloballyPositioned {
                                offset2 = it.positionInRoot()
                            }.size(size2.width.dp, size2.height.dp)
                        )
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false

        alignment.forEach {
            rule.runOnIdle {
                assertEquals(80, testModifier.height)
                assertEquals(80, testModifier.width)
                assertTrue(transitionState.targetState)
                contentAlignment = it
            }

            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.runOnIdle { transitionState.targetState = false }

            // Transition from item1 to item2 in 320ms, animating to full width in the first 160ms
            // then full height in the next 160ms
            while (transitionState.currentState != transitionState.targetState) {
                rule.runOnIdle {
                    val space = IntSize(testModifier.width, testModifier.height)
                    val position1 = it.align(size1, space, LayoutDirection.Ltr)
                    val position2 = it.align(size2, space, LayoutDirection.Ltr)
                    if (playTimeMillis < 80) {
                        // This gets removed when the animation is finished at 80ms
                        assertEquals(
                            position1,
                            IntOffset(offset1.x.roundToInt(), offset1.y.roundToInt())
                        )
                    }
                    if (playTimeMillis > 0) {
                        assertEquals(
                            position2,
                            IntOffset(offset2.x.roundToInt(), offset2.y.roundToInt())
                        )
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }

            rule.runOnIdle {
                assertEquals(size2.width, testModifier.width)
                assertEquals(size2.height, testModifier.height)
                // After the animation the size should be the same as parent, offset should be 0
                assertEquals(offset2, Offset.Zero)
                transitionState.targetState = true
            }

            // Transition from item2 to item1 in 80ms
            while (transitionState.currentState != transitionState.targetState) {
                rule.runOnIdle {
                    val space = IntSize(testModifier.width, testModifier.height)
                    val position1 = it.align(size1, space, LayoutDirection.Ltr)
                    val position2 = it.align(size2, space, LayoutDirection.Ltr)
                    if (playTimeMillis > 0) {
                        assertEquals(
                            position1,
                            IntOffset(offset1.x.roundToInt(), offset1.y.roundToInt())
                        )
                    }
                    if (playTimeMillis < 80) {
                        assertEquals(
                            position2,
                            IntOffset(offset2.x.roundToInt(), offset2.y.roundToInt())
                        )
                    }
                }
                rule.mainClock.advanceTimeByFrame()
            }

            rule.runOnIdle {
                assertEquals(size1.width, testModifier.width)
                assertEquals(size1.height, testModifier.height)
                // After the animation the size should be the same as parent, offset should be 0
                assertEquals(offset1, Offset.Zero)
            }
        }
    }
}
