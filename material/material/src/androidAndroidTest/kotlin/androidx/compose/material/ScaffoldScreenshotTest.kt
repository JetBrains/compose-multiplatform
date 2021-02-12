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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ScaffoldScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun onlyContent() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches(
            "scaffold_onlyContent"
        )
    }

    @Test
    fun topAppBar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches(
            "scaffold_topAppBar"
        )
    }

    @Test
    fun bottomAppBar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar")
    }

    @Test
    fun topAndBottomAppBar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar")
    }

    @Test
    fun centerFab() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = true,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_centerFab")
    }

    @Test
    fun endFab_ltr() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = true,
                    fabPosition = FabPosition.End,
                    rtl = false
                )
            }
        }

        assertScaffoldMatches("scaffold_endFab_ltr")
    }

    @Test
    fun endFab_rtl() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = true,
                    fabPosition = FabPosition.End,
                    rtl = true
                )
            }
        }

        assertScaffoldMatches("scaffold_endFab_rtl")
    }

    @Test
    fun topAppBar_centerFab() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = true,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_topAppBar_centerFab")
    }

    @Test
    fun topAppBar_endFab_ltr() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = true,
                    fabPosition = FabPosition.End,
                    rtl = false
                )
            }
        }

        assertScaffoldMatches("scaffold_topAppBar_endFab_ltr")
    }

    @Test
    fun topAppBar_endFab_rtl() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = false,
                    showSnackbar = false,
                    showFab = true,
                    fabPosition = FabPosition.End,
                    rtl = true
                )
            }
        }

        assertScaffoldMatches("scaffold_topAppBar_endFab_rtl")
    }

    @Test
    fun bottomAppBar_centerFab_floating() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_centerFab_floating")
    }

    @Test
    fun bottomAppBar_endFab_floating_ltr() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.End,
                    rtl = false
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_endFab_floating_ltr")
    }

    @Test
    fun bottomAppBar_endFab_floating_rtl() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.End,
                    rtl = true
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_endFab_floating_rtl")
    }

    @Test
    fun bottomAppBar_centerFab_docked() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_centerFab_docked")
    }

    @Test
    fun bottomAppBar_centerFab_docked_noCutout() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabCutout = false,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_centerFab_docked_noCutout")
    }

    @Test
    fun bottomAppBar_endFab_docked_ltr() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.End,
                    rtl = false
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_endFab_docked_ltr")
    }

    @Test
    fun bottomAppBar_endFab_docked_rtl() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.End,
                    rtl = true
                )
            }
        }

        assertScaffoldMatches("scaffold_bottomAppBar_endFab_docked_rtl")
    }

    @Test
    fun topAndBottomAppBar_centerFab_floating() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar_centerFab_floating")
    }

    @Test
    fun topAndBottomAppBar_endFab_floating_ltr() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.End,
                    rtl = false
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar_endFab_floating_ltr")
    }

    @Test
    fun topAndBottomAppBar_endFab_floating_rtl() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.End,
                    rtl = true
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar_endFab_floating_rtl")
    }

    @Test
    fun topAndBottomAppBar_centerFab_docked() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar_centerFab_docked")
    }

    @Test
    fun topAndBottomAppBar_endFab_docked_ltr() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.End,
                    rtl = false
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar_endFab_docked_ltr")
    }

    @Test
    fun topAndBottomAppBar_endFab_docked_rtl() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = false,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.End,
                    rtl = true
                )
            }
        }

        assertScaffoldMatches("scaffold_topAndBottomAppBar_endFab_docked_rtl")
    }

    @Test
    fun snackbar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = false,
                    showSnackbar = true,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches("snackbar")
    }

    @Test
    fun topAppBar_snackbar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = false,
                    showSnackbar = true,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches("topAppBar_snackbar")
    }

    @Test
    fun bottomAppBar_snackbar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = false,
                    showBottomAppBar = true,
                    showSnackbar = true,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches("bottomAppBar_snackbar")
    }

    @Test
    fun topAndBottomAppBar_snackbar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = true,
                    showFab = false
                )
            }
        }

        assertScaffoldMatches("topAndBottomAppBar_snackbar")
    }

    @Test
    fun topAndBottomAppBar_floatingFab_snackbar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = true,
                    showFab = true,
                    dockedFab = false,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("topAndBottomAppBar_floatingFab_snackbar")
    }

    @Test
    fun topAndBottomAppBar_dockedFab_snackbar() {
        composeTestRule.setContent {
            MaterialTheme {
                ScreenshotScaffold(
                    showTopAppBar = true,
                    showBottomAppBar = true,
                    showSnackbar = true,
                    showFab = true,
                    dockedFab = true,
                    fabPosition = FabPosition.Center
                )
            }
        }

        assertScaffoldMatches("topAndBottomAppBar_dockedFab_snackbar")
    }

    /**
     * Asserts that the Scaffold matches the screenshot with identifier [goldenIdentifier].
     *
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertScaffoldMatches(
        goldenIdentifier: String
    ) {
        // Capture and compare screenshots
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

/**
 * Scaffold with typical app bar / FAB content, and a light background color for the main content.
 *
 * @param showTopAppBar whether to show a [TopAppBar] or not
 * @param showBottomAppBar whether to show a [BottomAppBar] or not
 * @param showFab whether to show a [FloatingActionButton] or not
 * @param dockedFab whether the FAB (if present) is docked to the [BottomAppBar] or not
 * @param fabCutout whether the [BottomAppBar] (if present) should draw a cutout where the FAB
 * (if present) is, when docked to the [BottomAppBar].
 * @param fabPosition the [FabPosition] of the FAB (if present)
 * @param rtl whether to set [LayoutDirection.Rtl] as the [LayoutDirection] for this Scaffold and
 * its content
 */
@Composable
private fun ScreenshotScaffold(
    showTopAppBar: Boolean,
    showBottomAppBar: Boolean,
    showSnackbar: Boolean,
    showFab: Boolean,
    dockedFab: Boolean = false,
    fabCutout: Boolean = true,
    fabPosition: FabPosition = FabPosition.End,
    rtl: Boolean = false
) {
    val topAppBar = @Composable {
        if (showTopAppBar) {
            TopAppBar(title = { Text("Scaffold") })
        }
    }

    val bottomAppBar = @Composable {
        if (showBottomAppBar) {
            val cutoutShape = if (fabCutout) CircleShape else null
            BottomAppBar(cutoutShape = cutoutShape) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    }

    val snackbar = @Composable {
        if (showSnackbar) {
            val snackbarData = object : SnackbarData {
                override val message = "Snackbar"
                override val actionLabel = "Click me"
                override fun dismiss() {}
                override fun performAction() {}
                override val duration = SnackbarDuration.Indefinite
            }
            Snackbar(snackbarData)
        }
    }

    val fab = @Composable {
        if (showFab) {
            FloatingActionButton(
                content = { Icon(Icons.Filled.Favorite, null) },
                onClick = {}
            )
        }
    }

    val layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            Modifier
                .fillMaxSize(0.5f)
                .wrapContentSize()
                .semantics(mergeDescendants = true) {}
                .testTag(Tag)
        ) {
            Scaffold(
                topBar = topAppBar,
                bottomBar = bottomAppBar,
                snackbarHost = { snackbar() },
                floatingActionButton = fab,
                floatingActionButtonPosition = fabPosition,
                isFloatingActionButtonDocked = dockedFab,
                content = { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(MaterialTheme.colors.secondary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Scaffold Content",
                            modifier = Modifier.fillMaxSize().wrapContentSize()
                        )
                    }
                }
            )
        }
    }
}

private const val Tag = "Scaffold"
