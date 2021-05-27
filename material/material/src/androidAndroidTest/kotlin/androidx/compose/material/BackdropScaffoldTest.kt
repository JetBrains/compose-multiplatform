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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BackdropValue.Concealed
import androidx.compose.material.BackdropValue.Revealed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
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

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    @Test
    fun backdropScaffold_testOffset_whenConcealed() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Concealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    @LargeTest
    fun backdropScaffold_testCollapseAction_whenConcealed() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Concealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .performSemanticsAction(SemanticsActions.Collapse)

        advanceClock()

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)
    }

    @Test
    @LargeTest
    fun backdropScaffold_testExpandAction_whenRevealed() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        advanceClock()

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    fun backdropScaffold_testOffset_whenRevealed_backContentTooLarge() {
        rule.setContent {
            BackdropScaffold(
                scaffoldState = rememberBackdropScaffoldState(Revealed),
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
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
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
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
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - headerHeight)
    }

    @Test
    @LargeTest
    fun backdropScaffold_revealAndConceal_manually(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(Concealed)
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)

        scaffoldState.reveal()

        advanceClock()

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight + contentHeight)

        scaffoldState.conceal()

        advanceClock()

        rule.onNodeWithTag(frontLayer)
            .assertTopPositionInRootIsEqualTo(peekHeight)
    }

    @Test
    fun backdropScaffold_revealBySwiping() {
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(Concealed)
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Concealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }
    }

    @Test
    fun backdropScaffold_respectsConfirmStateChange() {
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(
                Concealed,
                confirmStateChange = {
                    it != Revealed
                }
            )
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Concealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Concealed)
        }
    }

    /**
     * Tests that the state and offset of [swipeable] are updated when swiping.
     */
    @Test
    fun backdropScaffold_syncThresholdUpdate() {
        val increasedAnchor = mutableStateOf(false)
        val scaffoldState = BackdropScaffoldState(Revealed)
        rule.setContent {
            BackdropScaffold(
                scaffoldState = scaffoldState,
                frontLayerScrimColor = Color.Red,
                appBar = { },
                backLayerContent = {
                    Box(
                        Modifier
                            .height(if (increasedAnchor.value) 400.dp else 200.dp)
                            .background(Color.Blue)
                    )
                },
                frontLayerContent = {
                    Box(Modifier.height(1000.dp).testTag(frontLayer).background(Color.Yellow))
                }
            )
        }

        val revealedOffset = rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(BackdropValue.Revealed)
            // state change changes the anchors, causing the recalculation
            increasedAnchor.value = true
            scaffoldState.offset.value
        }

        rule.runOnIdle {
            assertThat(scaffoldState.offset.value).isNotEqualTo(revealedOffset)
            // swap back, causing threshold update during update-caused settle
            increasedAnchor.value = false
        }

        rule.runOnIdle {
            // no crash and assert passes
            assertThat(scaffoldState.offset.value).isEqualTo(revealedOffset)
        }
    }

    @Test
    fun backdropScaffold_animatesAsSideEffect() {

        val bottomSheetState = ModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

        @Composable
        fun BottomSheet(message: String?) {
            Text(
                text = message ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colors.primary)
                    .padding(vertical = 50.dp),
                color = MaterialTheme.colors.onPrimary
            )
        }

        @Composable
        fun BottomSheetScreen(message: String?) {

            LaunchedEffect(bottomSheetState) {
                bottomSheetState.show()
            }

            ModalBottomSheetLayout(
                modifier = Modifier.fillMaxSize(),
                sheetContent = {
                    BottomSheet(message = message)
                },
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Should the modal be visible: ${message != null}")
                }
            }
        }

        rule.setContent {
            BottomSheetScreen(message = "")
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }
    }

    @Test
    fun backdropScaffold_animatesAsSideEffect_fromNull() {

        @Composable
        fun BottomSheet(message: String?) {
            Text(
                text = message ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colors.primary)
                    .padding(vertical = 50.dp),
                color = MaterialTheme.colors.onPrimary
            )
        }

        @Composable
        fun BottomSheetScreen(message: String?) {
            val bottomSheetState =
                rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

            LaunchedEffect(bottomSheetState, message != null) {
                if (message != null) {
                    try {
                        bottomSheetState.show()
                    } catch (ex: CancellationException) {
                        assertWithMessage("shouldn't cancel").fail()
                    }
                }
            }

            ModalBottomSheetLayout(
                modifier = Modifier.fillMaxSize(),
                sheetContent = {
                    BottomSheet(message = message)
                },
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Should the modal be visible: ${message != null}")
                }
            }
        }

        val stringState = mutableStateOf<String?>(null)

        rule.setContent {
            BottomSheetScreen(message = stringState.value)
        }

        rule.runOnIdle {
            stringState.value = "line 1 \n line2 \n line 3"
        }
        rule.waitForIdle()
    }

    @Test
    fun backdropScaffold_concealByTapingOnFrontLayer() {
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(Revealed)
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                frontLayerScrimColor = Color.Red,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Concealed)
        }
    }

    @Test
    fun backdropScaffold_concealByTapingOnFrontLayer_withUnspecifiedColorScrim() {
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(Revealed)
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                frontLayerScrimColor = Color.Unspecified,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        // still revealed if the color is unspecified
        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }
    }

    @Test
    fun backdropScaffold_tapOnFrontLayerScrim_respectsVeto() {
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(
                Revealed,
                confirmStateChange = {
                    it != Concealed
                }
            )
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                frontLayerScrimColor = Color.Red,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
                frontLayerContent = { Box(Modifier.fillMaxSize().testTag(frontLayer)) }
            )
        }

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        rule.runOnIdle {
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }
    }

    @Test
    fun backdropScaffold_scrimIsDisabledWhenUnspecified() {
        var frontLayerClicks = 0
        lateinit var scaffoldState: BackdropScaffoldState
        rule.setContent {
            scaffoldState = rememberBackdropScaffoldState(Revealed)
            BackdropScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                headerHeight = headerHeight,
                frontLayerScrimColor = Color.Unspecified,
                appBar = { Box(Modifier.height(peekHeight)) },
                backLayerContent = { Box(Modifier.height(contentHeight)) },
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
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }

        rule.onNodeWithTag(frontLayer)
            .performGesture { click() }

        advanceClock()

        rule.runOnIdle {
            assertThat(frontLayerClicks).isEqualTo(1)
            assertThat(scaffoldState.currentValue).isEqualTo(Revealed)
        }
    }
}