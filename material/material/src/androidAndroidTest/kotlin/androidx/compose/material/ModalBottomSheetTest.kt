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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class ModalBottomSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private val sheetHeight = 256.dp
    private val sheetTag = "sheetContentTag"
    private val contentTag = "contentTag"

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    @Test
    fun modalBottomSheet_testOffset_whenHidden() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_testOffset_whenExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height - sheetHeight)
    }

    @Test
    fun modalBottomSheet_testDismissAction_whenExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_testOffset_tallBottomSheet_whenHidden() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_testOffset_tallBottomSheet_whenExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun modalBottomSheet_testCollapseAction_tallBottomSheet_whenExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Collapse)

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height / 2)
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_testOffset_tallBottomSheet_whenHalfExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height / 2)
    }

    @Test
    fun modalBottomSheet_testExpandAction_tallBottomSheet_whenHalfExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Expand)

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenHalfExpanded() {
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_showAndHide_manually(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)

        sheetState.show()

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height - sheetHeight)

        sheetState.hide()

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_showAndHide_manually_tallBottomSheet(): Unit = runBlocking(
        AutoTestFrameClock()
    ) {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        val height = rule.rootHeight()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)

        sheetState.show()

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height / 2)

        sheetState.hide()

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_showAndHide_manually_skipHalfExpanded(): Unit = runBlocking(
        AutoTestFrameClock()
    ) {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            )
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {},
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        assertThat(sheetState.currentValue == ModalBottomSheetValue.Hidden)

        sheetState.show()

        advanceClock()

        assertThat(sheetState.currentValue == ModalBottomSheetValue.Expanded)

        sheetState.hide()

        assertThat(sheetState.currentValue == ModalBottomSheetValue.Hidden)
    }

    @Test
    fun modalBottomSheet_hideBySwiping() {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(contentTag)
                    )
                },
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown(endY = rule.rootHeight().toPx() / 2) }

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_hideBySwiping_skipHalfExpanded() {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Expanded,
                skipHalfExpanded = true
            )
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(contentTag)
                    )
                },
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_scrim_doesNotClickWhenClosed_hasContentDescriptionWhenOpen() {
        val topTag = "ModalBottomSheetLayout"
        val scrimColor = mutableStateOf(Color.Red)
        lateinit var closeSheet: String
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                modifier = Modifier.testTag(topTag),
                scrimColor = scrimColor.value,
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
                content = { Box(Modifier.fillMaxSize().testTag(contentTag)) },
                sheetContent = { Box(Modifier.fillMaxSize().testTag(sheetTag)) }
            )
            closeSheet = getString(Strings.CloseSheet)
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height / 2)
        var topNode = rule.onNodeWithTag(topTag).fetchSemanticsNode()
        assertEquals(3, topNode.children.size)
        rule.onNodeWithContentDescription(closeSheet)
            .assertHasClickAction()

        rule.runOnIdle {
            scrimColor.value = Color.Unspecified
        }

        topNode = rule.onNodeWithTag(topTag).fetchSemanticsNode()
        // only two nodes since there's no scrim
        assertEquals(2, topNode.children.size)
    }

    @Test
    fun modalBottomSheet_hideBySwiping_tallBottomSheet() {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(contentTag)
                    )
                },
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_respectsConfirmStateChange() {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Expanded,
                confirmStateChange = { newState ->
                    newState != ModalBottomSheetValue.Hidden
                }
            )
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(contentTag)
                    )
                },
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_expandBySwiping() {
        lateinit var sheetState: ModalBottomSheetState
        rule.setMaterialContent {
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded)
            ModalBottomSheetLayout(
                sheetState = sheetState,
                content = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(contentTag)
                    )
                },
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            )
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }

        advanceClock()

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_scrimNode_reportToSemanticsWhenShow_tallBottomSheet() {
        val topTag = "ModalBottomSheetLayout"
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                modifier = Modifier.testTag(topTag),
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
                content = { Box(Modifier.fillMaxSize().testTag(contentTag)) },
                sheetContent = { Box(Modifier.fillMaxSize().testTag(sheetTag)) }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height / 2)
        var topNode = rule.onNodeWithTag(topTag).fetchSemanticsNode()
        assertEquals(3, topNode.children.size)
        rule.onNodeWithTag(topTag)
            .onChildAt(1)
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)

        advanceClock()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
        topNode = rule.onNodeWithTag(topTag).fetchSemanticsNode()
        assertEquals(2, topNode.children.size)
    }

    @Test
    fun modalBottomSheet_hiddenOnTheFirstFrame() {
        val topTag = "ModalBottomSheetLayout"
        var lastKnownPosition: Offset? = null
        rule.setMaterialContent {
            ModalBottomSheetLayout(
                modifier = Modifier.testTag(topTag),
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
                content = { Box(Modifier.fillMaxSize().testTag(contentTag)) },
                sheetContent = {
                    Box(
                        Modifier.fillMaxSize().testTag(sheetTag).onGloballyPositioned {
                            if (lastKnownPosition != null) {
                                assertThat(lastKnownPosition).isEqualTo(it.positionInRoot())
                            }
                            lastKnownPosition = it.positionInRoot()
                        }
                    )
                }
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    fun modalBottomSheet_missingAnchors_findsClosest() {
        val topTag = "ModalBottomSheetLayout"
        val showShortContent = mutableStateOf(false)
        val sheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden)
        rule.setMaterialContent {
            LaunchedEffect(showShortContent.value) {
                sheetState.show()
            }
            ModalBottomSheetLayout(
                modifier = Modifier.testTag(topTag),
                sheetState = sheetState,
                content = { Box(Modifier.fillMaxSize().testTag(contentTag)) },
                sheetContent = {
                    if (!showShortContent.value) {
                        Box(Modifier.fillMaxSize().testTag(sheetTag))
                    } else {
                        Box(Modifier.fillMaxWidth().height(100.dp))
                    }
                }
            )
        }

        rule.onNodeWithTag(topTag).performTouchInput {
            swipeDown()
            swipeDown()
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        }

        rule.runOnIdle {
            showShortContent.value = true
        }
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }
    }
}
