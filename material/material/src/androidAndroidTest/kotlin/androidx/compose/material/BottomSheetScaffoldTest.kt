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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class BottomSheetScaffoldTest {

    @get:Rule
    val rule = createComposeRule()

    private val peekHeight = 75.dp

    private val sheetTestTag = "sheetTag"
    private val fabTestTag = "fabTag"
    private val snackbarTag = "snackbarTag"

    @Test
    fun bottomSheetScaffold_testOffset_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)
    }

    @Test
    fun bottomSheetScaffold_testExpandAction_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTestTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)
    }

    @Test
    fun bottomSheetScaffold_testCollapseAction_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTestTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .performSemanticsAction(SemanticsActions.Collapse)

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testNoCollapseExpandAction_whenPeekHeightIsSheetHeight() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BottomSheetScaffold(
                    scaffoldState = rememberBottomSheetScaffoldState(
                        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                    ),
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(peekHeight)
                                .testTag(sheetTestTag)
                        )
                    },
                    sheetPeekHeight = peekHeight
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithTag(sheetTestTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
    }

    @Test
    fun bottomSheetScaffold_revealAndConceal_manually(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)

        bottomSheetState.expand()

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)

        bottomSheetState.collapse()

        rule.onNodeWithTag(sheetTestTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping() {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetTestTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTestTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    fun bottomSheetScaffold_respectsConfirmStateChange() {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(
                BottomSheetValue.Collapsed,
                confirmStateChange = {
                    it != BottomSheetValue.Expanded
                }
            )
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState,
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetTestTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetTestTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping_gesturesDisabled() {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetTestTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun bottomSheetScaffold_drawer_manualControl() = runBlocking {
        var drawerChildPosition: Offset = Offset.Zero
        lateinit var scaffoldState: BottomSheetScaffoldState
        rule.setContent {
            scaffoldState = rememberBottomSheetScaffoldState()
            Box {
                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(100.dp))
                    },
                    drawerContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(color = Color.Blue)
                                .onGloballyPositioned { positioned: LayoutCoordinates ->
                                    drawerChildPosition = positioned.positionInParent()
                                }
                        )
                    }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Blue)
                    )
                }
            }
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        scaffoldState.drawerState.open()
        assertThat(drawerChildPosition.x).isLessThan(0f)
        scaffoldState.drawerState.close()
        assertThat(drawerChildPosition.x).isLessThan(0f)
    }

    @Test
    fun bottomSheetScaffold_AppbarAndContent_inColumn() {
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        rule.setMaterialContent {
            BottomSheetScaffold(
                topBar = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.localToWindow(Offset.Zero)
                                appbarSize = positioned.size
                            }
                    )
                },
                sheetContent = {
                    Box(Modifier.requiredSize(10.dp))
                }
            ) {
                Box(
                    Modifier
                        .onGloballyPositioned { contentPosition = it.localToWindow(Offset.Zero) }
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Blue)
                )
            }
        }
        assertThat(appbarPosition.y + appbarSize.height.toFloat())
            .isEqualTo(contentPosition.y)
    }

    @Test
    fun bottomSheetScaffold_fab_position(): Unit = runBlocking(AutoTestFrameClock()) {
        val fabTag = "fab"
        var fabSize: IntSize = IntSize.Zero
        lateinit var scaffoldState: BottomSheetScaffoldState
        rule.setContent {
            scaffoldState = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier
                            .onGloballyPositioned { positioned ->
                                fabSize = positioned.size
                            }
                            .testTag(fabTag),
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                },
                content = { Text("Content") }
            )
        }
        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - peekHeight - fabSize.height.toDp() / 2
            )
        }
        scaffoldState.bottomSheetState.expand()

        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - 300.dp - fabSize.height.toDp() / 2
            )
        }
    }

    @Test
    fun bottomSheetScaffold_snackbarPosition_fab_sheetCollapsed() {
        lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
        rule.setContent {
            bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                scaffoldState = bottomSheetScaffoldState,
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                sheetPeekHeight = peekHeight,
                snackbarHost = { state -> SnackbarHost(state, Modifier.testTag(snackbarTag)) },
                floatingActionButton = { TestFab() }
            ) {
                Text("Content")
            }
        }

        assertThat(bottomSheetScaffoldState.bottomSheetState.currentValue)
            .isEqualTo(BottomSheetValue.Collapsed)

        val expectedTop = rule.onNodeWithTag(fabTestTag).getUnclippedBoundsInRoot().top
        rule.onNodeWithTag(snackbarTag).assertTopPositionInRootIsEqualTo(expectedTop)
    }

    @Test
    fun bottomSheetScaffold_snackbarPosition_fab_sheetExpanded() {
        val snackbarHostHeight = 32.dp
        lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
        rule.setContent {
            val bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
            bottomSheetScaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
            BottomSheetScaffold(
                scaffoldState = bottomSheetScaffoldState,
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTestTag))
                },
                snackbarHost = { state ->
                    SnackbarHost(
                        state,
                        Modifier
                            .testTag(snackbarTag)
                            .requiredHeight(snackbarHostHeight)
                    )
                },
                floatingActionButton = { TestFab() }
            ) {
                Text("Content")
            }
        }

        assertThat(bottomSheetScaffoldState.bottomSheetState.currentValue)
            .isEqualTo(BottomSheetValue.Expanded)

        val expectedTop = rule.rootHeight() - snackbarHostHeight
        rule.onNodeWithTag(snackbarTag).assertTopPositionInRootIsEqualTo(expectedTop)
    }

    @Test
    fun bottomSheetScaffold_snackbarPosition_noFab(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
            rule.setContent {
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
                BottomSheetScaffold(
                    scaffoldState = bottomSheetScaffoldState,
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(300.dp)
                                .testTag(sheetTestTag))
                    },
                    sheetPeekHeight = peekHeight,
                    snackbarHost = { state -> SnackbarHost(state, Modifier.testTag(snackbarTag)) },
                ) {
                    Text("Content")
                }
            }

            val expectedTop = rule.onNodeWithTag(sheetTestTag).getUnclippedBoundsInRoot().top
            rule.onNodeWithTag(snackbarTag).assertTopPositionInRootIsEqualTo(expectedTop)
        }

    @Test
    fun bottomSheetScaffold_innerPadding() {
        lateinit var innerPadding: PaddingValues

        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(100.dp))
                },
                sheetPeekHeight = peekHeight
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(innerPadding.calculateBottomPadding()).isEqualTo(peekHeight)
        }
    }

    @Test
    fun bottomSheetScaffold_testOnlyCollapsedState_whenPeekHeightIsSheetHeight() {
        lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
        rule.setContent {
            bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            )
            BottomSheetScaffold(
                scaffoldState = bottomSheetScaffoldState,
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(peekHeight)
                            .testTag(sheetTestTag)
                    )
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        assertThat(bottomSheetScaffoldState.bottomSheetState.anchors.values)
            .containsExactly(BottomSheetValue.Collapsed)

        assertThat(bottomSheetScaffoldState.bottomSheetState.hasExpandedState)
            .isFalse()
    }

    @Composable
    private fun TestFab(
        modifier: Modifier = Modifier,
        testTag: String = fabTestTag,
        onClick: () -> Unit = {},
        content: @Composable () -> Unit = { Icon(Icons.Filled.Favorite, null) },
    ) {
        FloatingActionButton(
            modifier = modifier.testTag(testTag),
            onClick = onClick,
            content = content
        )
    }
}
