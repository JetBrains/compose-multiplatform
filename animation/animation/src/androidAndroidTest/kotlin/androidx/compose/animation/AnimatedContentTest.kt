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
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
                            modifier = Modifier
                                .onGloballyPositioned {
                                    offset1 = it.positionInRoot()
                                }
                                .size(size1.width.dp, size1.height.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned {
                                    offset2 = it.positionInRoot()
                                }
                                .size(size2.width.dp, size2.height.dp)
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

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun AnimatedContentSlideInAndOutOfContainerTest() {
        val transitionState = MutableTransitionState(true)
        // LinearEasing is required to ensure the animation doesn't reach final values before the
        // duration.
        val animSpec = tween<IntOffset>(200, easing = LinearEasing)
        lateinit var trueTransition: Transition<EnterExitState>
        lateinit var falseTransition: Transition<EnterExitState>
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                @Suppress("UpdateTransitionLabel")
                val rootTransition = updateTransition(transitionState)
                rootTransition.AnimatedContent(
                    transitionSpec = {
                        if (true isTransitioningTo false) {
                            slideIntoContainer(
                                towards = AnimatedContentScope.SlideDirection.Start, animSpec
                            ) with
                                slideOutOfContainer(
                                    towards = AnimatedContentScope.SlideDirection.Start, animSpec
                                )
                        } else {
                            slideIntoContainer(
                                towards = AnimatedContentScope.SlideDirection.End, animSpec
                            ) with
                                slideOutOfContainer(
                                    towards = AnimatedContentScope.SlideDirection.End,
                                    animSpec
                                )
                        }
                    }
                ) { target ->
                    if (target) {
                        trueTransition = transition
                    } else {
                        falseTransition = transition
                    }
                    Box(
                        Modifier
                            .requiredSize(200.dp)
                            .testTag(target.toString())
                    )
                }
            }
        }

        // Kick off the first animation.
        transitionState.targetState = false
        // The initial composition creates the transition…
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("true").assertExists()
        rule.onNodeWithTag("false").assertExists()
        // …but the animation won't actually start until one frame later.
        rule.mainClock.advanceTimeByFrame()
        assertThat(trueTransition.animations).isNotEmpty()
        assertThat(falseTransition.animations).isNotEmpty()

        // Loop to ensure the content is offset correctly at each frame.
        var trueAnim = trueTransition.animations[0]
        var falseAnim = falseTransition.animations[0]
        assertThat(transitionState.currentState).isTrue()
        while (transitionState.currentState) {
            // True is leaving: it should start at 0 and slide out to -200.
            assertThat(trueAnim.value).isEqualTo(IntOffset(-trueTransition.playTimeMillis, 0))
            // False is entering: it should start at 200 and slide in to 0.
            assertThat(falseAnim.value)
                .isEqualTo(IntOffset(200 - falseTransition.playTimeMillis, 0))
            rule.mainClock.advanceTimeByFrame()
        }
        // The animation should remove the newly-hidden node from the composition.
        rule.onNodeWithTag("true").assertDoesNotExist()

        // Kick off the second transition.
        transitionState.targetState = true
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("true").assertExists()
        rule.onNodeWithTag("false").assertExists()
        rule.mainClock.advanceTimeByFrame()
        assertThat(trueTransition.animations).isNotEmpty()

        trueAnim = trueTransition.animations[0]
        falseAnim = falseTransition.animations[0]
        assertThat(transitionState.currentState).isFalse()
        while (!transitionState.currentState) {
            // True is entering, it should start at -200 and slide in to 0.
            assertThat(trueAnim.value).isEqualTo(IntOffset(trueTransition.playTimeMillis - 200, 0))
            // False is leaving, it should start at 0 and slide out to 200.
            assertThat(falseAnim.value).isEqualTo(IntOffset(falseTransition.playTimeMillis, 0))
            rule.mainClock.advanceTimeByFrame()
        }
        rule.onNodeWithTag("false").assertDoesNotExist()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun AnimatedContentWithKeysTest() {
        var targetState by mutableStateOf(1)
        val list = mutableListOf<Int>()
        rule.setContent {
            val transition = updateTransition(targetState)
            val holder = rememberSaveableStateHolder()
            transition.AnimatedContent(contentKey = { it > 2 }) {
                if (it <= 2) {
                    holder.SaveableStateProvider(11) {
                        var count by rememberSaveable { mutableStateOf(0) }
                        LaunchedEffect(Unit) {
                            list.add(++count)
                        }
                    }
                }
                Box(Modifier.requiredSize(200.dp))
            }
            LaunchedEffect(Unit) {
                assertFalse(transition.isRunning)
                targetState = 2
                withFrameMillis {
                    assertFalse(transition.isRunning)
                    assertEquals(1, transition.currentState)
                    assertEquals(1, transition.targetState)

                    // This state change should now cause an animation
                    targetState = 3
                }
                withFrameMillis {
                    assertTrue(transition.isRunning)
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(1, list.size)
            assertEquals(1, list[0])
            targetState = 1
        }

        rule.runOnIdle {
            // Check that save worked
            assertEquals(2, list.size)
            assertEquals(1, list[0])
            assertEquals(2, list[1])
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun AnimatedContentWithInterruption() {
        var flag by mutableStateOf(true)
        var rootCoords: LayoutCoordinates? = null
        rule.setContent {
            AnimatedContent(targetState = flag,
                modifier = Modifier.onGloballyPositioned { rootCoords = it },
                transitionSpec = {
                if (targetState) {
                    fadeIn(tween(2000)) with slideOut(
                        tween(2000)) { fullSize ->
                        IntOffset(0, fullSize.height / 2) } + fadeOut(
                        tween(2000))
                } else {
                    fadeIn(tween(2000)) with fadeOut(tween(2000))
                }
            }) { state ->
                if (state) {
                    Box(modifier = Modifier
                        .onGloballyPositioned {
                            assertEquals(Offset.Zero, rootCoords!!.localPositionOf(it, Offset.Zero))
                        }
                        .fillMaxSize()
                        .background(Color.Green)
                    )
                } else {
                    LaunchedEffect(key1 = Unit) {
                        delay(200)
                        assertFalse(flag)
                        assertTrue(transition.isRunning)
                        // Interrupt
                        flag = true
                    }
                    Box(modifier = Modifier
                        .onGloballyPositioned {
                            assertEquals(Offset.Zero, rootCoords!!.localPositionOf(it, Offset.Zero))
                        }
                        .fillMaxSize()
                        .background(Color.Red)
                    )
                }
            }
        }
        rule.runOnIdle {
            flag = false
        }
    }

    @OptIn(InternalAnimationApi::class)
    private val Transition<*>.playTimeMillis get() = (playTimeNanos / 1_000_000L).toInt()
}
