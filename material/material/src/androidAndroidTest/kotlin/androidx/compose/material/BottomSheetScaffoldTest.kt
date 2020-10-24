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

import android.os.Build
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.swipeDown
import androidx.ui.test.swipeUp
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class BottomSheetScaffoldTest {

    @get:Rule
    val rule = createComposeRule()

    private val peekHeight = 75.dp

    private val sheetContent = "frontLayerTag"

    private lateinit var clock: ManualAnimationClock

    private fun advanceClock() {
        clock.clockTimeMillis += 100000L
    }

    @Before
    fun init() {
        clock = ManualAnimationClock(initTimeMillis = 0L)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(Modifier.fillMaxSize().testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState =
                        rememberBottomSheetState(initialValue = BottomSheetValue.Expanded)
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().height(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)
    }

    @Test
    fun backdropScaffold_revealAndConceal_manually() {
        val bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed, clock = clock)
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState =
                    rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().height(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight,
                bodyContent = { Text("Content") }
            )
        }

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)

        rule.runOnIdle {
            bottomSheetState.expand()
        }

        advanceClock()

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)

        rule.runOnIdle {
            bottomSheetState.collapse()
        }

        advanceClock()

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping() {
        val bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed, clock = clock)
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState =
                    rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().height(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight,
                bodyContent = { Text("Content") }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.value).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetContent)
            .performGesture { swipeUp() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.value).isEqualTo(BottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetContent)
            .performGesture { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.value).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping_gesturesDisabled() {
        val bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed, clock = clock)
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState =
                    rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().height(300.dp).testTag(sheetContent))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                bodyContent = { Text("Content") }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.value).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetContent)
            .performGesture { swipeUp() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.value).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun bottomSheetScaffold_drawer_manualControl() {
        var drawerChildPosition: Offset = Offset.Zero
        lateinit var scaffoldState: BottomSheetScaffoldState
        rule.setContent {
            scaffoldState = rememberBottomSheetScaffoldState()
            Box {
                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetContent = {
                        Box(Modifier.fillMaxWidth().height(100.dp))
                    },
                    drawerContent = {
                        Box(
                            Modifier
                                .onGloballyPositioned { positioned: LayoutCoordinates ->
                                    drawerChildPosition = positioned.positionInParent
                                }
                                .fillMaxWidth()
                                .preferredHeight(50.dp)
                                .background(color = Color.Blue)
                        )
                    }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Blue)
                    )
                }
            }
        }
        Truth.assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.runOnUiThread {
            scaffoldState.drawerState.open()
        }
        Truth.assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.runOnUiThread {
            scaffoldState.drawerState.close()
        }
        Truth.assertThat(drawerChildPosition.x).isLessThan(0f)
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
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.localToGlobal(Offset.Zero)
                                appbarSize = positioned.size
                            }
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Red)
                    )
                },
                sheetContent = {
                    Box(Modifier.size(10.dp))
                }
            ) {
                Box(
                    Modifier
                        .onGloballyPositioned { contentPosition = it.localToGlobal(Offset.Zero) }
                        .fillMaxWidth()
                        .preferredHeight(50.dp)
                        .background(Color.Blue)
                )
            }
        }
        Truth.assertThat(appbarPosition.y + appbarSize.height.toFloat())
            .isEqualTo(contentPosition.y)
    }

    @Test
    fun bottomSheetScaffold_fab_position() {
        val fabTag = "fab"
        var fabSize: IntSize = IntSize.Zero
        lateinit var scaffoldState: BottomSheetScaffoldState
        rule.setContent {
            scaffoldState = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    Box(Modifier.fillMaxWidth().height(300.dp).testTag(sheetContent))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier
                            .onGloballyPositioned { positioned ->
                                fabSize = positioned.size
                            }.testTag(fabTag),
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite)
                    }
                },
                bodyContent = { Text("Content") }
            )
        }
        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - peekHeight - fabSize.height.toDp() / 2
            )
        }
        rule.runOnUiThread {
            scaffoldState.bottomSheetState.expand()
        }
        advanceClock()

        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - 300.dp - fabSize.height.toDp() / 2
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun bottomSheetScaffold_topAppBarIsDrawnOnTopOfContent() {
        rule.setContent {
            Box(
                Modifier
                    .size(10.dp, 20.dp)
                    .semantics(mergeAllDescendants = true) {}
                    .testTag("Scaffold")
            ) {
                BottomSheetScaffold(
                    topBar = {
                        Box(
                            Modifier.size(10.dp)
                                .drawShadow(4.dp)
                                .zIndex(4f)
                                .background(color = Color.White)
                        )
                    },
                    sheetContent = {
                        Box(Modifier.size(0.dp))
                    }
                ) {
                    Box(
                        Modifier.size(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }

        rule.onNodeWithTag("Scaffold")
            .captureToBitmap().apply {
                // asserts the appbar(top half part) has the shadow
                val yPos = height / 2 + 2
                Truth.assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                Truth.assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                Truth.assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @Test
    fun bottomSheetScaffold_innerPadding_lambdaParam() {
        lateinit var innerPadding: PaddingValues

        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(Modifier.fillMaxWidth().height(100.dp))
                },
                sheetPeekHeight = peekHeight
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            Truth.assertThat(innerPadding.bottom).isEqualTo(peekHeight)
        }
    }
}
