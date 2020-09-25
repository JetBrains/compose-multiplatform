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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.BackdropValue.Concealed
import androidx.compose.material.BackdropValue.Revealed
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.click
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.swipeDown
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class BackdropScaffoldTest {

    @get:Rule
    val rule = createComposeRule()

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
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Concealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_backContentTooLarge() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.fillMaxHeight()) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - headerHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_nonPersistentAppBar() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                persistentAppBar = false,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(contentHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_nonStickyFrontLayer() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                stickyFrontLayer = false,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - headerHeight)
    }

    @Test
    @LargeTest
    fun backdropScaffold_revealAndConceal_manually() {
        val scaffoldState = BackdropScaffoldState(Concealed, clock = clock)
        rule.setContent {
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)

        rule.runOnIdle {
            scaffoldState.reveal()
        }

        advanceClock()

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)

        rule.runOnIdle {
            scaffoldState.conceal()
        }

        advanceClock()

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    fun backdropScaffold_revealBySwiping() {
        val scaffoldState = BackdropScaffoldState(Concealed, clock)
        rule.setContent {
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.value).isEqualTo(Concealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(scaffoldState.value).isEqualTo(Revealed)
        }
    }

    @Test
    fun backdropScaffold_concealByTapingOnFrontLayer() {
        val scaffoldState = BackdropScaffoldState(Revealed, clock)
        rule.setContent {
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                frontLayerScrimColor = Color.Red,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.value).isEqualTo(Revealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        rule.runOnIdle {
            assertThat(scaffoldState.value).isEqualTo(Concealed)
        }
    }

    @Test
    fun backdropScaffold_scrimIsDisabledWhenTransparent() {
        var frontLayerClicks = 0
        val scaffoldState = BackdropScaffoldState(Revealed, clock)
        rule.setContent {
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                frontLayerScrimColor = Color.Transparent,
                appBar = { Box(Modifier.preferredHeight(peekHeight)) },
                backLayerContent = { Box(Modifier.preferredHeight(contentHeight)) },
                frontLayerContent = {
                    Box(
                        Modifier.fillMaxSize().testTag(frontLayer).clickable {
                            frontLayerClicks += 1
                        }
                    )
                }
            )
        }

        rule.runOnIdle {
            assertThat(frontLayerClicks).isEqualTo(0)
            assertThat(scaffoldState.value).isEqualTo(Revealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        rule.runOnIdle {
            assertThat(frontLayerClicks).isEqualTo(1)
            assertThat(scaffoldState.value).isEqualTo(Revealed)
        }
    }
}