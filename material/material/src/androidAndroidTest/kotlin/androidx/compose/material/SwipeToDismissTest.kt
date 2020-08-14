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

package androidx.compose.material

import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.foundation.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Providers
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.runOnIdle
import androidx.ui.test.swipeLeft
import androidx.ui.test.swipeRight
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class SwipeToDismissTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val backgroundTag = "background"
    private val dismissContentTag = "dismissContent"
    private val swipeToDismissTag = "swipeToDismiss"

    private lateinit var clock: ManualAnimationClock

    private fun advanceClock() {
        clock.clockTimeMillis += 100000L
    }

    @Before
    fun init() {
        clock = ManualAnimationClock(initTimeMillis = 0L)
    }

    @Test
    fun swipeToDismiss_testOffset_whenDefault() {
        composeTestRule.setContent {
            SwipeToDismiss(
                state = rememberDismissState(DismissValue.Default),
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
            )
        }

        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun swipeToDismiss_testOffset_whenDismissedToEnd() {
        composeTestRule.setContent {
            SwipeToDismiss(
                state = rememberDismissState(DismissValue.DismissedToEnd),
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
            )
        }

        val width = rootWidth()
        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(width)
    }

    @Test
    fun swipeToDismiss_testOffset_whenDismissedToStart() {
        composeTestRule.setContent {
            SwipeToDismiss(
                state = rememberDismissState(DismissValue.DismissedToStart),
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
            )
        }

        val width = rootWidth()
        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(-width)
    }

    @Test
    fun swipeToDismiss_testBackgroundMatchesContentSize() {
        composeTestRule.setContent {
            SwipeToDismiss(
                state = rememberDismissState(DismissValue.Default),
                background = { Box(Modifier.fillMaxSize().testTag(backgroundTag)) },
                dismissContent = { Box(Modifier.preferredSize(100.dp)) }
            )
        }

        onNodeWithTag(backgroundTag)
            .assertIsSquareWithSize(100.dp)
    }

    @Test
    fun swipeToDismiss_dismissAndReset() {
        val dismissState = DismissState(DismissValue.Default, clock)
        composeTestRule.setContent {
            SwipeToDismiss(
                state = dismissState,
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
            )
        }

        val width = rootWidth()

        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        runOnIdle {
            dismissState.dismiss(DismissDirection.StartToEnd)
        }

        advanceClock()

        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(width)

        runOnIdle {
            dismissState.reset()
        }

        advanceClock()

        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        runOnIdle {
            dismissState.dismiss(DismissDirection.EndToStart)
        }

        advanceClock()

        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(-width)

        runOnIdle {
            dismissState.reset()
        }

        advanceClock()

        onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun swipeToDismiss_dismissBySwipe_toEnd() {
        val dismissState = DismissState(DismissValue.Default, clock)
        composeTestRule.setContent {
            SwipeToDismiss(
                modifier = Modifier.testTag(swipeToDismissTag),
                state = dismissState,
                directions = setOf(DismissDirection.StartToEnd),
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize()) }
            )
        }

        onNodeWithTag(swipeToDismissTag).performGesture { swipeRight() }

        advanceClock()

        runOnIdle {
            assertThat(dismissState.value).isEqualTo(DismissValue.DismissedToEnd)
        }
    }

    @Test
    fun swipeToDismiss_dismissBySwipe_toStart() {
        val dismissState = DismissState(DismissValue.Default, clock)
        composeTestRule.setContent {
            SwipeToDismiss(
                modifier = Modifier.testTag(swipeToDismissTag),
                state = dismissState,
                directions = setOf(DismissDirection.EndToStart),
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize()) }
            )
        }

        onNodeWithTag(swipeToDismissTag).performGesture { swipeLeft() }

        advanceClock()

        runOnIdle {
            assertThat(dismissState.value).isEqualTo(DismissValue.DismissedToStart)
        }
    }

    @Test
    fun swipeToDismiss_dismissBySwipe_toEnd_rtl() {
        val dismissState = DismissState(DismissValue.Default, clock)
        composeTestRule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                SwipeToDismiss(
                    modifier = Modifier.testTag(swipeToDismissTag),
                    state = dismissState,
                    directions = setOf(DismissDirection.StartToEnd),
                    background = { },
                    dismissContent = { Box(Modifier.fillMaxSize()) }
                )
            }
        }

        onNodeWithTag(swipeToDismissTag).performGesture { swipeLeft() }

        advanceClock()

        runOnIdle {
            assertThat(dismissState.value).isEqualTo(DismissValue.DismissedToEnd)
        }
    }

    @Test
    fun swipeToDismiss_dismissBySwipe_toStart_rtl() {
        val dismissState = DismissState(DismissValue.Default, clock)
        composeTestRule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                SwipeToDismiss(
                    modifier = Modifier.testTag(swipeToDismissTag),
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart),
                    background = { },
                    dismissContent = { Box(Modifier.fillMaxSize()) }
                )
            }
        }

        onNodeWithTag(swipeToDismissTag).performGesture { swipeRight() }

        advanceClock()

        runOnIdle {
            assertThat(dismissState.value).isEqualTo(DismissValue.DismissedToStart)
        }
    }

    @Test
    fun swipeToDismiss_dismissBySwipe_disabled() {
        val dismissState = DismissState(DismissValue.Default, clock)
        composeTestRule.setContent {
            SwipeToDismiss(
                modifier = Modifier.testTag(swipeToDismissTag),
                state = dismissState,
                directions = setOf(),
                background = { },
                dismissContent = { Box(Modifier.fillMaxSize()) }
            )
        }

        onNodeWithTag(swipeToDismissTag).performGesture { swipeRight() }

        advanceClock()

        runOnIdle {
            assertThat(dismissState.value).isEqualTo(DismissValue.Default)
        }

        onNodeWithTag(swipeToDismissTag).performGesture { swipeLeft() }

        advanceClock()

        runOnIdle {
            assertThat(dismissState.value).isEqualTo(DismissValue.Default)
        }
    }
}