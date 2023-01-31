/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetScaffoldTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val restorationTester = StateRestorationTester(rule)

    private val sheetHeight = 256.dp
    private val dragHandleHeight = 44.dp
    private val peekHeight = 75.dp
    private val sheetTag = "sheetContentTag"

    @Test
    fun test_stateSavedAndRestored() {
        val initialValue = SheetValue.Expanded
        lateinit var state: BottomSheetScaffoldState
        restorationTester.setContent {
            state = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(initialValue),
            )
        }
        assertThat(state.bottomSheetState.currentValue).isEqualTo(initialValue)
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state.bottomSheetState.currentValue).isEqualTo(initialValue)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight,
                sheetDragHandle = null
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Expanded)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight))
                },
                sheetDragHandle = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(dragHandleHeight)
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - (sheetHeight + dragHandleHeight))
    }

    @Test
    fun bottomSheetScaffold_testExpandAction_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = null,
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - sheetHeight)
    }

    @Test
    fun bottomSheetScaffold_testCollapseAction_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Expanded)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = null,
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .performSemanticsAction(SemanticsActions.Collapse)

        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testNoCollapseExpandAction_whenPeekHeightIsSheetHeight() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BottomSheetScaffold(
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(peekHeight)
                                .testTag(sheetTag)
                        )
                    },
                    sheetDragHandle = null,
                    sheetPeekHeight = peekHeight
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
    }

    @Test
    fun backdropScaffold_revealAndConceal_manually(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight))
                },
                sheetDragHandle = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(dragHandleHeight)
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }
        val expectedHeight = sheetHeight + dragHandleHeight

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)

        bottomSheetState.expand()
        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - expectedHeight)

        bottomSheetState.partialExpand()
        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping() {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = null,
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
    }

    @Test
    fun bottomSheetScaffold_respectsConfirmStateChange() {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                confirmValueChange = {
                    it != SheetValue.Expanded
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
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping_gesturesDisabled() {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTag))
                },
                sheetSwipeEnabled = false,
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
    }

    @Test
    fun bottomSheetScaffold_AppbarAndContent_inColumn() {
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        rule.setContent {
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
        assertThat(appbarPosition.y + appbarSize.height.toFloat()).isEqualTo(contentPosition.y)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun bottomSheetScaffold_topAppBarIsDrawnOnTopOfContent() {
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(10.dp, 20.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("Scaffold")
            ) {
                BottomSheetScaffold(
                    topBar = {
                        Box(
                            Modifier
                                .requiredSize(10.dp)
                                .shadow(4.dp)
                                .zIndex(4f)
                                .background(color = Color.White)
                        )
                    },
                    sheetContent = {
                        Box(Modifier.requiredSize(0.dp))
                    }
                ) {
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }

        rule.onNodeWithTag("Scaffold")
            .captureToImage().asAndroidBitmap().apply {
                // asserts the appbar(top half part) has the shadow
                val yPos = height / 2 + 2
                assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @Test
    fun bottomSheetScaffold_innerPadding_lambdaParam() {
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
    fun bottomSheetScaffold_landscape_sheetRespectsMaxWidthAndIsCentered() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(p0: Configuration) {
                latch.countDown()
            }

            override fun onLowMemory() {
                // NO-OP
            }

            override fun onTrimMemory(p0: Int) {
                // NO-OP
            }
        })

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            rule.setContent {
                BottomSheetScaffold(sheetContent = {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                }) {
                    Text("body")
                }
            }
            val rootWidth = rule.rootWidth()
            val maxSheetWidth = 640.dp
            val expectedSheetWidth = maxSheetWidth.coerceAtMost(rootWidth)
            // Our sheet should be max 640 dp but fill the width if the container is less wide
            val expectedSheetLeft = if (rootWidth <= expectedSheetWidth) {
                0.dp
            } else {
                (rootWidth - expectedSheetWidth) / 2
            }

            rule.onNodeWithTag(sheetTag)
                .onParent()
                .assertLeftPositionInRootIsEqualTo(
                    expectedLeft = expectedSheetLeft
                )
                .assertWidthIsEqualTo(expectedSheetWidth)
        } catch (e: InterruptedException) {
            TestCase.fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}