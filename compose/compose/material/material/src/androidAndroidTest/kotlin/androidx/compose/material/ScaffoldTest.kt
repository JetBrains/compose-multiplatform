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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScaffoldTest {

    @get:Rule
    val rule = createComposeRule()

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
                    Modifier.onGloballyPositioned { child1 = it.positionInParent() }
                )
                Text(
                    "Two",
                    Modifier.onGloballyPositioned { child2 = it.positionInParent() }
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
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.localToWindow(Offset.Zero)
                                appbarSize = positioned.size
                            }
                    )
                }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Blue)
                        .onGloballyPositioned { contentPosition = it.localToWindow(Offset.Zero) }
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
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.positionInParent()
                                appbarSize = positioned.size
                            }
                    )
                }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .height(50.dp)
                        .background(color = Color.Blue)
                        .onGloballyPositioned { positioned: LayoutCoordinates ->
                            contentPosition = positioned.positionInParent()
                            contentSize = positioned.size
                        }
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
        val drawerGesturedEnabledState = mutableStateOf(false)
        rule.setContent {
            Box(Modifier.testTag(scaffoldTag)) {
                Scaffold(
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
                    },
                    drawerGesturesEnabled = drawerGesturedEnabledState.value
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
        rule.onNodeWithTag(scaffoldTag).performTouchInput {
            swipeRight()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        rule.onNodeWithTag(scaffoldTag).performTouchInput {
            swipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)

        rule.runOnUiThread {
            drawerGesturedEnabledState.value = true
        }

        rule.onNodeWithTag(scaffoldTag).performTouchInput {
            swipeRight()
        }
        assertThat(drawerChildPosition.x).isEqualTo(0f)
        rule.onNodeWithTag(scaffoldTag).performTouchInput {
            swipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_manualControl(): Unit = runBlocking {
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
                            fabPosition = positioned.positionInRoot()
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                },
                floatingActionButtonPosition = FabPosition.Center,
                isFloatingActionButtonDocked = true,
                bottomBar = {
                    BottomAppBar(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarPosition = positioned.positionInRoot()
                            }
                    ) {}
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
                            fabPosition = positioned.positionInRoot()
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
                isFloatingActionButtonDocked = true,
                bottomBar = {
                    BottomAppBar(
                        Modifier
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarPosition = positioned.positionInRoot()
                            }
                    ) {}
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
                    .requiredSize(10.dp, 20.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("Scaffold")
            ) {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier.requiredSize(10.dp)
                                .shadow(4.dp)
                                .zIndex(4f)
                                .background(color = Color.White)
                        )
                    }
                ) {
                    Box(
                        Modifier.requiredSize(10.dp)
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
    fun scaffold_geometry_fabSize() {
        var fabSize: IntSize = IntSize.Zero
        val showFab = mutableStateOf(true)
        var fabPlacement: FabPlacement? = null
        rule.setContent {
            val fab = @Composable {
                if (showFab.value) {
                    FloatingActionButton(
                        modifier = Modifier.onGloballyPositioned { positioned ->
                            fabSize = positioned.size
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                }
            }
            Scaffold(
                floatingActionButton = fab,
                floatingActionButtonPosition = FabPosition.End,
                bottomBar = {
                    fabPlacement = LocalFabPlacement.current
                }
            ) {
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(fabPlacement?.width).isEqualTo(fabSize.width)
            assertThat(fabPlacement?.height).isEqualTo(fabSize.height)
            showFab.value = false
        }

        rule.runOnIdle {
            assertThat(fabPlacement).isEqualTo(null)
            assertThat(fabPlacement).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_animated_fabSize() {
        val fabTestTag = "FAB TAG"
        lateinit var showFab: MutableState<Boolean>
        var actualFabSize: IntSize = IntSize.Zero
        var actualFabPlacement: FabPlacement? = null
        rule.setContent {
            showFab = remember { mutableStateOf(true) }
            val animatedFab = @Composable {
                AnimatedVisibility(visible = showFab.value) {
                    FloatingActionButton(
                        modifier = Modifier.onGloballyPositioned { positioned ->
                            actualFabSize = positioned.size
                        }.testTag(fabTestTag),
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                }
            }
            Scaffold(
                floatingActionButton = animatedFab,
                floatingActionButtonPosition = FabPosition.End,
                bottomBar = {
                    actualFabPlacement = LocalFabPlacement.current
                }
            ) {
                Text("body")
            }
        }

        val fabNode = rule.onNodeWithTag(fabTestTag)

        fabNode.assertIsDisplayed()

        rule.runOnIdle {
            assertThat(actualFabPlacement?.width).isEqualTo(actualFabSize.width)
            assertThat(actualFabPlacement?.height).isEqualTo(actualFabSize.height)
            actualFabSize = IntSize.Zero
            actualFabPlacement = null
            showFab.value = false
        }

        fabNode.assertDoesNotExist()

        rule.runOnIdle {
            assertThat(actualFabPlacement).isNull()
            actualFabSize = IntSize.Zero
            actualFabPlacement = null
            showFab.value = true
        }

        fabNode.assertIsDisplayed()

        rule.runOnIdle {
            assertThat(actualFabPlacement?.width).isEqualTo(actualFabSize.width)
            assertThat(actualFabPlacement?.height).isEqualTo(actualFabSize.height)
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
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarSize = positioned.size
                            }
                    )
                }
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            with(rule.density) {
                assertThat(innerPadding.calculateBottomPadding())
                    .isEqualTo(bottomBarSize.toSize().height.toDp())
            }
        }
    }
}
