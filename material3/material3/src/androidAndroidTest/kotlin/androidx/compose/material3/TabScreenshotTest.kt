/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun lightTheme() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme"
        )
    }

    @Test
    fun lightTheme_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_lightTheme_pressed"
        )
    }

    @Test
    fun darkTheme() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme"
        )
    }

    @Test
    fun darkTheme_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) {
                DefaultTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_darkTheme_pressed"
        )
    }

    @Test
    fun leadingIconTabs_lightTheme() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) {
                DefaultLeadingIconTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_lightTheme"
        )
    }

    @Test
    fun leadingIconTabs_darkTheme() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) {
                DefaultLeadingIconTabs(interactionSource)
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_darkTheme"
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
 * @param containerColor the containerColor of the [TabRow]
 * @param selectedContentColor the content color for a selected [Tab] (first tab)
 * @param unselectedContentColor the content color for an unselected [Tab] (second and third tabs)
 */
@Composable
private fun CustomTabs(
    interactionSource: MutableInteractionSource,
    containerColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        TabRow(selectedTabIndex = 0,
            containerColor = containerColor,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[0]),
                    color = selectedContentColor
                )
            }) {
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
