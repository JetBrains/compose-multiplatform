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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
                            .fillMaxSize(0.6f)
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
                content = { Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(contentTag)) },
                sheetContent = { Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)) }
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
                confirmValueChange = { newState ->
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
                content = { Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(contentTag)) },
                sheetContent = { Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)) }
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
                content = { Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(contentTag)) },
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                            .onGloballyPositioned {
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
        var showShortContent by mutableStateOf(false)
        val sheetState = ModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
        lateinit var scope: CoroutineScope
        rule.setMaterialContent {
            scope = rememberCoroutineScope()
            ModalBottomSheetLayout(
                modifier = Modifier.testTag(topTag),
                sheetState = sheetState,
                content = { Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(contentTag)) },
                sheetContent = {
                    if (showShortContent) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .testTag(sheetTag)
                        )
                    }
                }
            )
        }

        scope.launch { sheetState.show() } // We can't use LaunchedEffect with Swipeable in tests
        // yet, so we're invoking this outside of composition. See b/254115946.
        rule.waitForIdle()

        rule.onNodeWithTag(topTag).performTouchInput {
            swipeDown()
            swipeDown()
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        }

        showShortContent = true
        scope.launch { sheetState.show() } // We can't use LaunchedEffect with Swipeable in tests
        // yet, so we're invoking this outside of composition. See b/254115946.

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_nestedScroll_consumesWithinBounds_scrollsOutsideBounds() {
        lateinit var sheetState: ModalBottomSheetState
        lateinit var scrollState: ScrollState
        val sheetContentTag = "sheetContent"
        rule.setContent {
            sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.HalfExpanded
            )
            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetContent = {
                    scrollState = rememberScrollState()
                    Column(
                        Modifier
                            .verticalScroll(scrollState)
                            .testTag(sheetContentTag)
                    ) {
                        repeat(100) {
                            Text(it.toString(), Modifier.requiredHeight(50.dp))
                        }
                    }
                },
                content = { Box(Modifier.fillMaxSize()) }
            )
        }

        rule.waitForIdle()

        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)

        rule.onNodeWithTag(sheetContentTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)

        rule.onNodeWithTag(sheetContentTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = top)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isGreaterThan(0)
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)

        rule.onNodeWithTag(sheetContentTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)

        rule.onNodeWithTag(sheetContentTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)

        rule.onNodeWithTag(sheetContentTag)
            .performTouchInput {
                swipeDown(startY = bottom / 2, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
    }

    @Test
    fun modalBottomSheet_anchorsChange_retainsCurrentValue() {
        lateinit var state: ModalBottomSheetState
        var amountOfItems by mutableStateOf(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            ModalBottomSheetLayout(
                sheetState = state,
                sheetContent = {
                    scope = rememberCoroutineScope()
                    LazyColumn {
                        items(amountOfItems) {
                            ListItem(text = { Text("$it") })
                        }
                    }
                },
                content = { Box(Modifier.fillMaxSize()) }
            )
        }

        assertThat(state.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)

        amountOfItems = 50
        rule.waitForIdle()
        scope.launch {
            state.show()
        }
        // The anchors should now be {Hidden, HalfExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)

        amountOfItems = 100 // The anchors should now be {Hidden, HalfExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded) // We should
        // retain the current value if possible
        assertThat(state.swipeableState.anchors).containsKey(ModalBottomSheetValue.Hidden)
        assertThat(state.swipeableState.anchors).containsKey(ModalBottomSheetValue.HalfExpanded)
        assertThat(state.swipeableState.anchors).containsKey(ModalBottomSheetValue.Expanded)

        amountOfItems = 0 // When the sheet height is 0, we should only have a hidden anchor
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        assertThat(state.swipeableState.anchors).containsKey(ModalBottomSheetValue.Hidden)
        assertThat(state.swipeableState.anchors)
            .doesNotContainKey(ModalBottomSheetValue.HalfExpanded)
        assertThat(state.swipeableState.anchors).doesNotContainKey(ModalBottomSheetValue.Expanded)
    }

    @Test
    fun modalBottomSheet_emptySheet_expandDoesNotAnimate() {
        lateinit var state: ModalBottomSheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            scope = rememberCoroutineScope()
            ModalBottomSheetLayout(
                sheetState = state,
                sheetContent = { },
                content = { Box(Modifier.fillMaxSize()) }
            )
        }

        assertThat(state.swipeableState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        scope.launch { state.expand() }
        rule.waitForIdle()

        assertThat(state.swipeableState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
    }

    @Test
    fun modalBottomSheetState_notRestoredWhenInitialValueChangesBeforeRestoration() {
        lateinit var state: ModalBottomSheetState
        val restorationTester = StateRestorationTester(rule)
        var tallSheet = true // Not backed by state as we only care about its value when composing
        var compositionCount = 0
        restorationTester.setContent {
            compositionCount++
            val initialValue = if (tallSheet) ModalBottomSheetValue.HalfExpanded else
                ModalBottomSheetValue.Expanded
            state = rememberModalBottomSheetState(initialValue)
            ModalBottomSheetLayout(
                sheetState = state,
                sheetContent = {
                    Box(if (tallSheet) Modifier.fillMaxSize() else Modifier.height(56.dp))
                },
                content = { Box(Modifier.fillMaxSize()) },
            )
        }

        assertThat(state.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)
        assertThat(compositionCount).isEqualTo(1)

        tallSheet = false
        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertThat(compositionCount).isEqualTo(2)

        assertThat(state.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
    }

    @Test
    fun modalBottomSheet_shortSheet_sizeChanges_snapsToNewTarget() {
        var size by mutableStateOf(56.dp)
        val expectedExpandedAnchor by derivedStateOf {
            with(rule.density) { (rule.rootHeight() - size).toPx() }
        }
        lateinit var state: ModalBottomSheetState
        rule.setContent {
            state = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)
            ModalBottomSheetLayout(
                sheetState = state,
                sheetContent = {
                    Box(Modifier.height(size))
                },
                content = { Box(Modifier.fillMaxSize()) },
            )
        }

        assertThat(state.requireOffset()).isWithin(0.5f).of(expectedExpandedAnchor)

        size = 100.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(0.5f).of(expectedExpandedAnchor)

        size = 30.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(0.5f).of(expectedExpandedAnchor)
    }

    @Test
    fun modalBottomSheet_narrowScreen_sheetRespectsMaxWidth() {
        val layoutTag = "msbl"
        val sheetTag = "sheet"
        val simulatedRootWidth = 600.dp
        val simulatedRootHeight = 1080.dp
        rule.setContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                modifier = Modifier
                    .testTag(layoutTag)
                    .requiredSize(simulatedRootWidth, simulatedRootHeight),
                sheetContent = {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                },
                content = { Box(Modifier.fillMaxSize()) }
            )
        }

        rule.onNodeWithTag(layoutTag)
            .assertWidthIsEqualTo(simulatedRootWidth)

        val maxSheetWidth = 640.dp
        val expectedSheetWidth = maxSheetWidth.coerceAtMost(simulatedRootWidth)
        // Our sheet should be max 640 dp but fill the width if the container is less wide
        val expectedSheetLeft = if (simulatedRootWidth <= expectedSheetWidth) {
            0.dp
        } else {
            (simulatedRootWidth - expectedSheetWidth) / 2
        }

        // We are requiring a size on the layout that might be wider than the root width
        // In that case, our "actual" left might be outside the rule's bounds
        val simulatedLeft = with(rule.density) {
            rule.onNodeWithTag(layoutTag).fetchSemanticsNode().positionInRoot.x.toDp()
        }

        val simulatedExpectedLeft = simulatedLeft + expectedSheetLeft

        rule.onNodeWithTag(sheetTag)
            .onParent()
            .assertLeftPositionInRootIsEqualTo(
                expectedLeft = simulatedExpectedLeft
            )
            .assertWidthIsEqualTo(expectedSheetWidth)
    }

    @Test
    fun modalBottomSheet_wideScreen_sheetRespectsMaxWidthAndIsCentered() {
        val layoutTag = "msbl"
        val sheetTag = "sheet"
        val simulatedRootWidth = 1920.dp
        val simulatedRootHeight = 980.dp
        rule.setContent {
            ModalBottomSheetLayout(
                sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
                modifier = Modifier
                    .testTag(layoutTag)
                    .requiredSize(simulatedRootWidth, simulatedRootHeight),
                sheetContent = {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                },
                content = { Box(Modifier.fillMaxSize()) }
            )
        }

        rule.onNodeWithTag(layoutTag)
            .assertWidthIsEqualTo(simulatedRootWidth)

        val maxSheetWidth = 640.dp
        val expectedSheetWidth = maxSheetWidth.coerceAtMost(simulatedRootWidth)
        // Our sheet should be max 640 dp but fill the width if the container is less wide
        val expectedSheetLeft = if (simulatedRootWidth <= expectedSheetWidth) {
            0.dp
        } else {
            (simulatedRootWidth - expectedSheetWidth) / 2
        }

        // We are requiring a size on the layout that might be wider than the root width
        // In that case, our "actual" left might be outside the rule's bounds
        val simulatedLeft = with(rule.density) {
            rule.onNodeWithTag(layoutTag).fetchSemanticsNode().positionInRoot.x.toDp()
        }

        val simulatedExpectedLeft = simulatedLeft + expectedSheetLeft

        rule.onNodeWithTag(sheetTag)
            .onParent()
            .assertLeftPositionInRootIsEqualTo(
                expectedLeft = simulatedExpectedLeft
            )
            .assertWidthIsEqualTo(expectedSheetWidth)
    }

    @Test
    fun modalBottomSheet_shortSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        val sheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden)
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetContent = {
                    if (hasSheetContent) {
                        Box(Modifier.fillMaxHeight(0.4f))
                    }
                },
                content = {}
            )
        }

        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        assertThat(sheetState.swipeableState.hasAnchorForValue(ModalBottomSheetValue.HalfExpanded))
            .isFalse()
        assertThat(sheetState.swipeableState.hasAnchorForValue(ModalBottomSheetValue.Expanded))
            .isFalse()

        scope.launch { sheetState.show() }
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.currentValue).isEqualTo(sheetState.targetValue)

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Expanded)
    }

    @Test
    fun modalBottomSheet_tallSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        val sheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden)
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetContent = {
                    if (hasSheetContent) {
                        Box(Modifier.fillMaxHeight(0.6f))
                    }
                },
                content = {}
            )
        }

        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.Hidden)
        assertThat(sheetState.swipeableState.hasAnchorForValue(ModalBottomSheetValue.HalfExpanded))
            .isFalse()
        assertThat(sheetState.swipeableState.hasAnchorForValue(ModalBottomSheetValue.Expanded))
            .isFalse()

        scope.launch { sheetState.show() }
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.currentValue).isEqualTo(sheetState.targetValue)

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(ModalBottomSheetValue.HalfExpanded)
    }
}
