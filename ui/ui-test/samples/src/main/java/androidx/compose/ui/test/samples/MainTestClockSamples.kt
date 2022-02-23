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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Test

// We want the sample to include the function and @Test annotation to show it's a
// self-contained test, so define the function again inside the sampled function.
@Sampled
fun testSlideOut() {
    @Test
    fun testSlideOut() {
        var showBox by mutableStateOf(true)

        composeTestRule.setContent {
            AnimatedVisibility(
                visible = showBox,
                exit = slideOutHorizontally(tween(3000)) { -it }
            ) {
                Box(Modifier.size(100.dp).testTag("box")) {}
            }
        }

        // Take control of the clock
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("box").assertExists()
        // Start hiding the box
        showBox = false

        // Trigger recomposition
        composeTestRule.mainClock.advanceTimeByFrame()
        // Await layout pass to set up animation
        composeTestRule.waitForIdle()
        // Give animation a start time
        composeTestRule.mainClock.advanceTimeByFrame()

        // Advance clock by first half the animation duration
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithTag("box").assertExists()

        // Advance clock by second half the animation duration
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithTag("box").assertDoesNotExist()
    }
}

// We want the sample to include the function and @Test annotation to show it's a
// self-contained test, so define the function again inside the sampled function.
@Sampled
fun testControlClock() {
    @Test
    fun testControlClock() {
        var toggle by mutableStateOf(false)

        composeTestRule.setContent {
            var count by remember { mutableStateOf(0) }
            DisposableEffect(toggle) {
                count++
                // Apply the change to `count` in the snapshot:
                Snapshot.sendApplyNotifications()
                // Note: we apply the snapshot manually here for illustration purposes. In general
                // we recommended against doing this in production code.
                onDispose {}
            }
            Text("Effect ran $count time(s), toggle is $toggle")
        }

        // Check initial state
        composeTestRule.onNodeWithText("Effect ran 1 time(s), toggle is false").assertExists()
        // Take control of the clock
        composeTestRule.mainClock.autoAdvance = false

        // Change the `toggle` state variable
        toggle = true
        // Apply the change to `toggle` in the snapshot:
        Snapshot.sendApplyNotifications()

        // Recomposition hasn't yet happened:
        composeTestRule.onNodeWithText("Effect ran 1 time(s), toggle is false").assertExists()
        // Forward the clock by 2 frames: 1 for `toggle` and then 1 for `count`
        composeTestRule.mainClock.advanceTimeBy(32)
        // UI now fully reflects the new state
        composeTestRule.onNodeWithText("Effect ran 2 time(s), toggle is true").assertExists()
    }
}
