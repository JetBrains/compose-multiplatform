/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScaffoldTest {

    @get:Rule
    val rule = createComposeRule()

    private val scaffoldTag = "Scaffold"
    private val roundingError = 0.5.dp

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
        rule.setMaterialContent(lightColorScheme()) {
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
        var scaffoldSize: IntSize = IntSize.Zero
        var appbarPosition: Offset = Offset.Zero
        var contentPosition: Offset = Offset.Zero
        var contentSize: IntSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
            Scaffold(
                topBar = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.localToWindow(Offset.Zero)
                            }
                    )
                },
                modifier = Modifier
                    .onGloballyPositioned { positioned: LayoutCoordinates ->
                        scaffoldSize = positioned.size
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Blue)
                        .onGloballyPositioned { positioned: LayoutCoordinates ->
                            contentPosition = positioned.positionInParent()
                            contentSize = positioned.size
                        }
                )
            }
        }
        assertThat(appbarPosition.y).isEqualTo(contentPosition.y)
        assertThat(scaffoldSize).isEqualTo(contentSize)
    }

    @Test
    fun scaffold_bottomBarAndContent_inStack() {
        var scaffoldSize: IntSize = IntSize.Zero
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        var contentSize: IntSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
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
                },
                modifier = Modifier
                    .onGloballyPositioned { positioned: LayoutCoordinates ->
                        scaffoldSize = positioned.size
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
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
        assertThat(scaffoldSize).isEqualTo(contentSize)
    }

    @Test
    fun scaffold_innerPadding_lambdaParam() {
        var topBarSize: IntSize = IntSize.Zero
        var bottomBarSize: IntSize = IntSize.Zero
        lateinit var innerPadding: PaddingValues

        rule.setContent {
            Scaffold(
                topBar = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                topBarSize = positioned.size
                            }
                    )
                },
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
                assertThat(innerPadding.calculateTopPadding())
                    .isEqualTo(topBarSize.toSize().height.toDp())
                assertThat(innerPadding.calculateBottomPadding())
                    .isEqualTo(bottomBarSize.toSize().height.toDp())
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_topAppBarIsDrawnOnTopOfContent() {
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(10.dp, 20.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag(scaffoldTag)
            ) {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier
                                .requiredSize(10.dp)
                                .shadow(4.dp)
                                .zIndex(4f)
                                .background(color = Color.White)
                        )
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

        rule.onNodeWithTag(scaffoldTag)
            .captureToImage().asAndroidBitmap().apply {
                // asserts the appbar(top half part) has the shadow
                val yPos = height / 2 + 2
                assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_providesInsets_respectTopAppBar() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 20.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    topBar = {
                        Box(Modifier.requiredSize(10.dp))
                    }
                ) { paddingValues ->
                    // top is like top app bar + rounding error
                    assertThat(paddingValues.calculateTopPadding() - 10.dp)
                        .isLessThan(roundingError)
                    // bottom is like the insets
                    assertThat(paddingValues.calculateBottomPadding() - 30.dp).isLessThan(
                        roundingError
                    )
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_providesInsets_respectCollapsedTopAppBar() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 20.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    topBar = {
                        Box(Modifier.requiredSize(0.dp))
                    }
                ) { paddingValues ->
                    // top is like the collapsed top app bar (i.e. 0dp) + rounding error
                    assertThat(paddingValues.calculateTopPadding()).isLessThan(roundingError)
                    // bottom is like the insets
                    assertThat(paddingValues.calculateBottomPadding() - 30.dp).isLessThan(
                        roundingError
                    )
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_providesInsets_respectsBottomAppBar() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 20.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    bottomBar = {
                        Box(Modifier.requiredSize(10.dp))
                    }
                ) { paddingValues ->
                    // bottom is like bottom app bar + rounding error
                    assertThat(paddingValues.calculateBottomPadding() - 10.dp).isLessThan(
                        roundingError
                    )
                    // top is like the insets
                    assertThat(paddingValues.calculateTopPadding() - 5.dp).isLessThan(roundingError)
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_insetsTests_snackbarRespectsInsets() {
        val hostState = SnackbarHostState()
        var snackbarSize: IntSize? = null
        var snackbarPosition: Offset? = null
        var density: Density? = null
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 20.dp)) {
                density = LocalDensity.current
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    snackbarHost = {
                        SnackbarHost(hostState = hostState,
                            modifier = Modifier
                                .onGloballyPositioned {
                                    snackbarSize = it.size
                                    snackbarPosition = it.positionInRoot()
                                })
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
        val snackbarBottomOffsetDp =
            with(density!!) { (snackbarPosition!!.y.roundToInt() + snackbarSize!!.height).toDp() }
        assertThat(rule.rootHeight() - snackbarBottomOffsetDp - 3.dp).isLessThan(1.dp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_insetsTests_FabRespectsInsets() {
        var fabSize: IntSize? = null
        var fabPosition: Offset? = null
        var density: Density? = null
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 20.dp)) {
                density = LocalDensity.current
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    floatingActionButton = {
                        FloatingActionButton(onClick = {},
                            modifier = Modifier
                                .onGloballyPositioned {
                                    fabSize = it.size
                                    fabPosition = it.positionInRoot()
                                }) {
                            Text("Fab")
                        }
                    },
                ) {
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }
        val fabBottomOffsetDp =
            with(density!!) { (fabPosition!!.y.roundToInt() + fabSize!!.height).toDp() }
        assertThat(rule.rootHeight() - fabBottomOffsetDp - 3.dp).isLessThan(1.dp)
    }
}
