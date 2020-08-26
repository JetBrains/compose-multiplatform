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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.BackdropValue.Concealed
import androidx.compose.material.BackdropValue.Revealed
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.click
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.runOnIdle
import androidx.ui.test.swipeDown
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class BackdropScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val peekHeight = 75.dp
    private val headerHeight = 100.dp
    private val contentHeight = 150.dp

    private val frontLayer = "frontLayerTag"

    private lateinit var clock: ManualAnimationClock

    private fun advanceClock() {
        clock.clockTimeMillis += 100000L
    }

    @Before
    fun init() {
        clock = ManualAnimationClock(initTimeMillis = 0L)
    }

    @Test
    fun backdropScaffold_testOffset_whenConcealed() {
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = rememberBackdropState(Concealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed() {
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = rememberBackdropState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_backContentTooLarge() {
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = rememberBackdropState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.fillMaxHeight()) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(rootHeight() - headerHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_nonPersistentAppBar() {
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = rememberBackdropState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                persistentAppBar = false,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(contentHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_nonStickyFrontLayer() {
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = rememberBackdropState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                stickyFrontLayer = false,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(rootHeight() - headerHeight)
    }

    @Test
    fun backdropScaffold_revealAndConceal_manually() {
        val backdropState = BackdropScaffoldState(Concealed, clock = clock)
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = backdropState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)

        runOnIdle {
            backdropState.reveal()
        }

        advanceClock()

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)

        runOnIdle {
            backdropState.conceal()
        }

        advanceClock()

        onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    fun backdropScaffold_revealBySwiping() {
        val backdropState = BackdropScaffoldState(Concealed, clock)
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = backdropState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        runOnIdle {
            assertThat(backdropState.value).isEqualTo(Concealed)
        }

        onNodeWithTag(frontLayer)
            .performGesture { swipeDown() }

        advanceClock()

        runOnIdle {
            assertThat(backdropState.value).isEqualTo(Revealed)
        }
    }

    @Test
    fun backdropScaffold_concealByTapingOnFrontLayer() {
        val backdropState = BackdropScaffoldState(Revealed, clock)
        composeTestRule.setContent {
            BackdropScaffold(
                backdropScaffoldState = backdropState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        runOnIdle {
            assertThat(backdropState.value).isEqualTo(Revealed)
        }

        onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        runOnIdle {
            assertThat(backdropState.value).isEqualTo(Concealed)
        }
    }
}