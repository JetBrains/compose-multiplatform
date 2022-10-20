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
package androidx.compose.animation

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@OptIn(ExperimentalTestApi::class)
class CrossfadeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun crossfadeTest_showsContent() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            val showFirst by remember { mutableStateOf(true) }
            Crossfade(showFirst) {
                BasicText(if (it) First else Second)
            }
        }
        rule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong())

        rule.onNodeWithText(First).assertExists()
    }

    @Test
    fun crossfadeTest_disposesContentOnChange() {
        rule.mainClock.autoAdvance = false

        var showFirst by mutableStateOf(true)
        var disposed = false
        rule.setContent {
            Crossfade(showFirst) {
                BasicText(if (it) First else Second)
                DisposableEffect(Unit) {
                    onDispose {
                        disposed = true
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong())

        rule.runOnUiThread {
            showFirst = false
        }

        // Wait for content to be disposed
        rule.mainClock.advanceTimeUntil { disposed }

        rule.onNodeWithText(First).assertDoesNotExist()
        rule.onNodeWithText(Second).assertExists()
    }

    @Test
    fun crossfadeTest_durationCanBeModifierUsingAnimationSpec() {
        rule.mainClock.autoAdvance = false

        val duration = 100 // smaller than default 300
        var showFirst by mutableStateOf(true)
        var disposed = false
        rule.setContent {
            Crossfade(
                showFirst,
                animationSpec = TweenSpec(durationMillis = duration)
            ) {
                BasicText(if (it) First else Second)
                DisposableEffect(Unit) {
                    onDispose {
                        disposed = true
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(duration.toLong())

        rule.runOnUiThread {
            showFirst = false
        }

        rule.mainClock.advanceTimeBy(duration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        assertTrue(disposed)
    }

    @Test
    fun nullInitialValue() {
        rule.mainClock.autoAdvance = false
        var current by mutableStateOf<String?>(null)

        rule.setContent {
            Crossfade(current) { value ->
                BasicText(if (value == null) First else Second)
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong())

        rule.onNodeWithText(First).assertExists()
        rule.onNodeWithText(Second).assertDoesNotExist()

        rule.runOnUiThread {
            current = "other"
        }

        rule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        rule.onNodeWithText(First).assertDoesNotExist()
        rule.onNodeWithText(Second).assertExists()
    }

    @Test
    fun crossfadeTest_rememberSaveableIsNotRecreatedForScreens() {
        rule.mainClock.autoAdvance = false

        val duration = 100
        var showFirst by mutableStateOf(true)
        var counter = 1
        var counter1 = 0
        var counter2 = 0
        rule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            Crossfade(
                showFirst,
                animationSpec = TweenSpec(durationMillis = duration)
            ) {
                saveableStateHolder.SaveableStateProvider(it) {
                    if (it) {
                        counter1 = rememberSaveable { counter++ }
                    } else {
                        counter2 = rememberSaveable { counter++ }
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(duration.toLong())

        rule.runOnUiThread {
            showFirst = false
        }

        rule.mainClock.advanceTimeBy(duration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        // and go back to the second screen

        rule.runOnUiThread {
            showFirst = true
        }

        rule.mainClock.advanceTimeBy(duration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        assertEquals(1, counter1)
        assertEquals(2, counter2)
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun crossfadeTest_contentKey() {
        var targetState by mutableStateOf(1)
        val list = mutableListOf<Int>()
        rule.setContent {
            val transition = updateTransition(targetState)
            val holder = rememberSaveableStateHolder()
            transition.Crossfade(contentKey = { it > 0 }) {
                if (it > 0) {
                    holder.SaveableStateProvider(true) {
                        var count by rememberSaveable { mutableStateOf(0) }
                        LaunchedEffect(Unit) {
                            list.add(++count)
                        }
                    }
                }
                Box(Modifier.requiredSize(200.dp))
            }
            LaunchedEffect(Unit) {
                // Expect no animation when targetState changed while the contentKey remains
                // the same.
                assertFalse(transition.isRunning)
                targetState = 2
                withFrameMillis {
                    assertFalse(transition.isRunning)
                    assertEquals(transition.currentState, transition.targetState)
                    // This state change should now change the contentKey & hence trigger an animation
                    targetState = -1
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
            // Switch back to 1, expect a targetState change in Transition.
            targetState = 1
        }

        rule.runOnIdle {
            // Check that save worked
            assertEquals(2, list.size)
            assertEquals(1, list[0])
            assertEquals(2, list[1])
        }
    }

    companion object {
        private const val First = "first"
        private const val Second = "second"
    }
}
