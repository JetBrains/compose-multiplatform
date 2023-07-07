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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class)
class TabScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun lightTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColors()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme_defaultColors"
        )
    }

    @Test
    fun lightTheme_defaultColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColors()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_lightTheme_defaultColors_pressed"
        )
    }

    @Test
    fun lightTheme_surfaceColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                scope = rememberCoroutineScope()
                CustomTabs(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme_surfaceColors"
        )
    }

    @Test
    fun lightTheme_surfaceColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                scope = rememberCoroutineScope()
                CustomTabs(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_lightTheme_surfaceColors_pressed"
        )
    }

    @Test
    fun darkTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColors()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_defaultColors"
        )
    }

    @Test
    fun darkTheme_defaultColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColors()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_darkTheme_defaultColors_pressed"
        )
    }

    // Dark theme by default uses `surface` as the background color, but the selectedContentColor
    // defaults to `onSurface`, whereas a typical use case is for it to be `primary`. This test
    // matches that use case.
    @Test
    fun darkTheme_surfaceColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomTabs(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_surfaceColors"
        )
    }

    @Test
    fun darkTheme_surfaceColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomTabs(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_darkTheme_surfaceColors_pressed"
        )
    }

    @Test
    fun darkTheme_primaryColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomTabs(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.primary,
                    selectedContentColor = MaterialTheme.colors.onPrimary,
                    unselectedContentColor = MaterialTheme.colors.onPrimary
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_primaryColors"
        )
    }

    @Test
    fun darkTheme_primaryColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomTabs(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.primary,
                    selectedContentColor = MaterialTheme.colors.onPrimary,
                    unselectedContentColor = MaterialTheme.colors.onPrimary
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_darkTheme_primaryColors_pressed"
        )
    }

    @Test
    fun leadingIconTabs_lightTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColors()) {
                DefaultLeadingIconTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_lightTheme_defaultColors"
        )
    }

    @Test
    fun leadingIconTabs_darkTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColors()) {
                DefaultLeadingIconTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_darkTheme_defaultColors"
        )
    }

    /**
     * Asserts that the tabs match the screenshot with identifier [goldenIdentifier].
     *
     * @param interactionSource the [MutableInteractionSource] used for the first Tab
     * @param interaction the [Interaction] to assert for, or `null` if no [Interaction].
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertTabsMatch(
        scope: CoroutineScope,
        interactionSource: MutableInteractionSource,
        interaction: Interaction? = null,
        goldenIdentifier: String
    ) {
        if (interaction != null) {
            composeTestRule.runOnIdle {
                // Start ripple
                scope.launch {
                    interactionSource.emit(interaction)
                }
            }

            composeTestRule.waitForIdle()
            // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't
            // properly wait for synchronization. Instead just wait until after the ripples are
            // finished animating.
            Thread.sleep(300)
        }

        // Capture and compare screenshots
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

/**
 * Default colored [TabRow] with three [Tab]s. The first [Tab] is selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 * visual state.
 */
@Composable
private fun DefaultTabs(
    interactionSource: MutableInteractionSource
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        TabRow(selectedTabIndex = 0) {
            Tab(
                text = { Text("TAB") },
                selected = true,
                interactionSource = interactionSource,
                onClick = {}
            )
            Tab(
                text = { Text("TAB") },
                selected = false,
                onClick = {}
            )
            Tab(
                text = { Text("TAB") },
                selected = false,
                onClick = {}
            )
        }
    }
}

/**
 * Custom colored [TabRow] with three [Tab]s. The first [Tab] is selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 * visual state.
 * @param backgroundColor the backgroundColor of the [TabRow]
 * @param selectedContentColor the content color for a selected [Tab] (first tab)
 * @param unselectedContentColor the content color for an unselected [Tab] (second and third tabs)
 */
@Composable
private fun CustomTabs(
    interactionSource: MutableInteractionSource,
    backgroundColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color
) {
    // Apply default emphasis
    @Suppress("NAME_SHADOWING")
    val unselectedContentColor = unselectedContentColor.copy(alpha = ContentAlpha.medium)
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        TabRow(selectedTabIndex = 0, backgroundColor = backgroundColor) {
            Tab(
                text = { Text("TAB") },
                selected = true,
                interactionSource = interactionSource,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                onClick = {}
            )
            Tab(
                text = { Text("TAB") },
                selected = false,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                onClick = {}
            )
            Tab(
                text = { Text("TAB") },
                selected = false,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                onClick = {}
            )
        }
    }
}

/**
 * Default colored [TabRow] with three [LeadingIconTab]s. The first [LeadingIconTab] is selected,
 * and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [LeadingIconTab], to control its
 * visual state.
 */
@Composable
private fun DefaultLeadingIconTabs(
    interactionSource: MutableInteractionSource
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        TabRow(selectedTabIndex = 0) {
            LeadingIconTab(
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
                selected = true,
                interactionSource = interactionSource,
                onClick = {}
            )
            LeadingIconTab(
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
                selected = false,
                onClick = {}
            )
            LeadingIconTab(
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
                selected = false,
                onClick = {}
            )
        }
    }
}

private const val Tag = "Tab"
