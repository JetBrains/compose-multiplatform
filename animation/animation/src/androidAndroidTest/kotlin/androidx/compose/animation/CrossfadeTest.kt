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
import androidx.compose.foundation.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.filters.MediumTest
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithText
import androidx.ui.test.runOnIdle
import androidx.ui.test.waitForIdle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@MediumTest
class CrossfadeTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = false)

    @Test
    fun crossfadeTest_showsContent() {
        composeTestRule.clockTestRule.pauseClock()

        composeTestRule.setContent {
            val showFirst by remember { mutableStateOf(true) }
            Crossfade(showFirst) {
                Text(if (it) First else Second)
            }
        }
        composeTestRule.clockTestRule.advanceClock(DefaultDurationMillis.toLong())

        onNodeWithText(First).assertExists()
    }

    @Test
    fun crossfadeTest_disposesContentOnChange() {
        composeTestRule.clockTestRule.pauseClock()

        var showFirst by mutableStateOf(true)
        var disposed = false
        composeTestRule.setContent {
            Crossfade(showFirst) {
                Text(if (it) First else Second)
                onDispose {
                    disposed = true
                }
            }
        }
        composeTestRule.clockTestRule.advanceClock(DefaultDurationMillis.toLong())

        runOnIdle {
            showFirst = false
        }

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(DefaultDurationMillis.toLong())

        runOnIdle {
            assertTrue(disposed)
        }

        onNodeWithText(First).assertDoesNotExist()
        onNodeWithText(Second).assertExists()
    }

    @Test
    fun crossfadeTest_durationCanBeModifierUsingAnimationSpec() {
        composeTestRule.clockTestRule.pauseClock()

        val duration = 100 // smaller than default 300
        var showFirst by mutableStateOf(true)
        var disposed = false
        composeTestRule.setContent {
            Crossfade(
                showFirst,
                animation = TweenSpec(durationMillis = duration)
            ) {
                Text(if (it) First else Second)
                onDispose {
                    disposed = true
                }
            }
        }
        composeTestRule.clockTestRule.advanceClock(duration.toLong())

        runOnIdle {
            showFirst = false
        }

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(duration.toLong())

        runOnIdle {
            assertTrue(disposed)
        }
    }

    @Test
    fun nullInitialValue() {
        composeTestRule.clockTestRule.pauseClock()
        var current by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            Crossfade(current) { value ->
                Text(if (value == null) First else Second)
            }
        }
        composeTestRule.clockTestRule.advanceClock(DefaultDurationMillis.toLong())

        onNodeWithText(First).assertExists()
        onNodeWithText(Second).assertDoesNotExist()

        runOnIdle {
            current = "other"
        }

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(DefaultDurationMillis.toLong())

        onNodeWithText(First).assertDoesNotExist()
        onNodeWithText(Second).assertExists()
    }

    companion object {
        private const val First = "first"
        private const val Second = "second"
    }
}
