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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.toSize
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.swipeLeft
import androidx.ui.test.swipeRight
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ScaffoldTest {

    @get:Rule
    val rule = createComposeRule(disableTransitions = true)

    private val scaffoldTag = "Scaffold"

    @Test
    fun scaffold_onlyContent_takesWholeScreen() {
        rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 100.dp,
            parentMaxHeight = 100.dp
        ) {
            Scaffold {
                Text("Scaffold body")
            }
        }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun scaffold_onlyContent_stackSlot() {
        var child1: Offset = Offset.Zero
        var child2: Offset = Offset.Zero
        rule.setMaterialContent {
            Scaffold {
                Text(
                    "One",
                    Modifier.onGloballyPositioned { child1 = it.positionInParent }
                )
                Text(
                    "Two",
                    Modifier.onGloballyPositioned { child2 = it.positionInParent }
                )
            }
        }
        assertThat(child1.y).isEqualTo(child2.y)
        assertThat(child1.x).isEqualTo(child2.x)
    }

    @Test
    fun scaffold_AppbarAndContent_inColumn() {
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        rule.setMaterialContent {
            Scaffold(
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
        assertThat(appbarPosition.y + appbarSize.height.toFloat())
            .isEqualTo(contentPosition.y)
    }

    @Test
    fun scaffold_bottomBarAndContent_inStack() {
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        var contentSize: IntSize = IntSize.Zero
        rule.setMaterialContent {
            Scaffold(
                bottomBar = {
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.positionInParent
                                appbarSize = positioned.size
                            }
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Red)
                    )
                }
            ) {
                Box(
                    Modifier
                        .onGloballyPositioned { positioned: LayoutCoordinates ->
                            contentPosition = positioned.positionInParent
                            contentSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(50.dp)
                        .background(color = Color.Blue)
                )
            }
        }
        val appBarBottom = appbarPosition.y + appbarSize.height
        val contentBottom = contentPosition.y + contentSize.height
        assertThat(appBarBottom).isEqualTo(contentBottom)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_gestures() {
        var drawerChildPosition: Offset = Offset.Zero
        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState(isDrawerGesturesEnabled = false)
            Box(Modifier.testTag(scaffoldTag)) {
                Scaffold(
                    scaffoldState = scaffoldState,
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
        assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.onNodeWithTag(scaffoldTag).performGesture {
            swipeRight()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.onNodeWithTag(scaffoldTag).performGesture {
            swipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)

        rule.runOnUiThread {
            scaffoldState.isDrawerGesturesEnabled = true
        }

        rule.onNodeWithTag(scaffoldTag).performGesture {
            swipeRight()
        }
        assertThat(drawerChildPosition.x).isEqualTo(0f)
        rule.onNodeWithTag(scaffoldTag).performGesture {
            swipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_manualControl() {
        var drawerChildPosition: Offset = Offset.Zero
        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState()
            Box(Modifier.testTag(scaffoldTag)) {
                Scaffold(
                    scaffoldState = scaffoldState,
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
        assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.runOnUiThread {
            scaffoldState.drawerState.open()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.runOnUiThread {
            scaffoldState.drawerState.close()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
    }

    @Test
    fun scaffold_centerDockedFab_position() {
        var fabPosition: Offset = Offset.Zero
        var fabSize: IntSize = IntSize.Zero
        var bottomBarPosition: Offset = Offset.Zero
        rule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier.onGloballyPositioned { positioned ->
                            fabSize = positioned.size
                            fabPosition = positioned.localToGlobal(positioned.positionInParent)
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite)
                    }
                },
                floatingActionButtonPosition = FabPosition.Center,
                isFloatingActionButtonDocked = true,
                bottomBar = {
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarPosition =
                                    positioned.localToGlobal(positioned.positionInParent)
                            }
                            .fillMaxWidth()
                            .preferredHeight(100.dp)
                            .background(color = Color.Red)
                    )
                }
            ) {
                Text("body")
            }
        }
        val expectedFabY = bottomBarPosition.y - (fabSize.height / 2)
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }

    @Test
    fun scaffold_endDockedFab_position() {
        var fabPosition: Offset = Offset.Zero
        var fabSize: IntSize = IntSize.Zero
        var bottomBarPosition: Offset = Offset.Zero
        rule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier.onGloballyPositioned { positioned ->
                            fabSize = positioned.size
                            fabPosition = positioned.localToGlobal(positioned.positionInParent)
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite)
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
                isFloatingActionButtonDocked = true,
                bottomBar = {
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarPosition =
                                    positioned.localToGlobal(positioned.positionInParent)
                            }
                            .fillMaxWidth()
                            .preferredHeight(100.dp)
                            .background(color = Color.Red)
                    )
                }
            ) {
                Text("body")
            }
        }
        val expectedFabY = bottomBarPosition.y - (fabSize.height / 2)
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_topAppBarIsDrawnOnTopOfContent() {
        rule.setContent {
            Box(
                Modifier
                    .size(10.dp, 20.dp)
                    .semantics(mergeAllDescendants = true) {}
                    .testTag("Scaffold")
            ) {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier.size(10.dp)
                                .drawShadow(4.dp)
                                .background(color = Color.White)
                        )
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
                assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @Test
    fun scaffold_geometry_fabSize() {
        var fabSize: IntSize = IntSize.Zero
        val showFab = mutableStateOf(true)
        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState()
            val fab: @Composable (() -> Unit)? = if (showFab.value) {
                @Composable {
                    FloatingActionButton(
                        modifier = Modifier.onGloballyPositioned { positioned ->
                            fabSize = positioned.size
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite)
                    }
                }
            } else {
                null
            }
            Scaffold(
                scaffoldState = scaffoldState,
                floatingActionButton = fab,
                floatingActionButtonPosition = FabPosition.End
            ) {
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(scaffoldState.scaffoldGeometry.fabBounds?.size)
                .isEqualTo(fabSize.toSize())
            showFab.value = false
        }

        rule.runOnIdle {
            assertThat(scaffoldState.scaffoldGeometry.fabBounds?.size).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_bottomBarSize() {
        var bottomBarSize: IntSize = IntSize.Zero
        val showBottom = mutableStateOf(true)
        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState()
            val bottom: @Composable (() -> Unit)? = if (showBottom.value) {
                @Composable {
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarSize = positioned.size
                            }
                            .fillMaxWidth()
                            .preferredHeight(100.dp)
                            .background(color = Color.Red)
                    )
                }
            } else {
                null
            }
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = bottom
            ) {
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(scaffoldState.scaffoldGeometry.bottomBarBounds?.size)
                .isEqualTo(bottomBarSize.toSize())
            showBottom.value = false
        }

        rule.runOnIdle {
            assertThat(scaffoldState.scaffoldGeometry.bottomBarBounds?.size)
                .isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_topBarSize() {
        var topBarSize: IntSize = IntSize.Zero
        val showTop = mutableStateOf(true)
        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState()
            val top: @Composable (() -> Unit)? = if (showTop.value) {
                @Composable {
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                topBarSize = positioned.size
                            }
                            .fillMaxWidth()
                            .preferredHeight(100.dp)
                            .background(color = Color.Red)
                    )
                }
            } else {
                null
            }
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = top
            ) {
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(scaffoldState.scaffoldGeometry.topBarBounds?.size)
                .isEqualTo(topBarSize.toSize())
            showTop.value = false
        }

        rule.runOnIdle {
            assertThat(scaffoldState.scaffoldGeometry.topBarBounds?.size)
                .isEqualTo(null)
        }
    }

    @Test
    fun scaffold_innerPadding_lambdaParam() {
        var bottomBarSize: IntSize = IntSize.Zero
        lateinit var innerPadding: PaddingValues

        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState()
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = {
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarSize = positioned.size
                            }
                            .fillMaxWidth()
                            .preferredHeight(100.dp)
                            .background(color = Color.Red)
                    )
                }
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            with(rule.density) {
                assertThat(innerPadding.bottom).isEqualTo(bottomBarSize.toSize().height.toDp())
            }
        }
    }

    @Test
    fun scaffold_bottomBar_geometryPropagation() {
        var bottomBarSize: IntSize = IntSize.Zero
        lateinit var geometry: ScaffoldGeometry

        lateinit var scaffoldState: ScaffoldState
        rule.setContent {
            scaffoldState = rememberScaffoldState()
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = {
                    geometry = AmbientScaffoldGeometry.current
                    Box(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarSize = positioned.size
                            }
                            .fillMaxWidth()
                            .preferredHeight(100.dp)
                            .background(color = Color.Red)
                    )
                }
            ) {
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(geometry.bottomBarBounds?.size).isEqualTo(bottomBarSize.toSize())
            assertThat(geometry.bottomBarBounds?.width).isNotEqualTo(0f)
        }
    }
}
