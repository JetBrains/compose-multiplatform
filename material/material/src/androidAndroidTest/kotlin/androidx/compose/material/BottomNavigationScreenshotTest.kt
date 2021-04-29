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
@OptIn(ExperimentalMaterialApi::class, ExperimentalTestApi::class)
class BottomNavigationScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun lightTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                scope = rememberCoroutineScope()
                DefaultBottomNavigation(interactionSource)
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "bottomNavigation_lightTheme_defaultColors"
        )
    }

    @Test
    fun lightTheme_defaultColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                scope = rememberCoroutineScope()
                DefaultBottomNavigation(interactionSource)
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "bottomNavigation_lightTheme_defaultColors_pressed"
        )
    }

    @Test
    fun lightTheme_surfaceColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                scope = rememberCoroutineScope()
                CustomBottomNavigation(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "bottomNavigation_lightTheme_surfaceColors"
        )
    }

    @Test
    fun lightTheme_surfaceColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                scope = rememberCoroutineScope()
                CustomBottomNavigation(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "bottomNavigation_lightTheme_surfaceColors_pressed"
        )
    }

    @Test
    fun darkTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                DefaultBottomNavigation(interactionSource)
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "bottomNavigation_darkTheme_defaultColors"
        )
    }

    @Test
    fun darkTheme_defaultColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                DefaultBottomNavigation(interactionSource)
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "bottomNavigation_darkTheme_defaultColors_pressed"
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
                CustomBottomNavigation(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "bottomNavigation_darkTheme_surfaceColors"
        )
    }

    @Test
    fun darkTheme_surfaceColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomBottomNavigation(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.surface,
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface
                )
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "bottomNavigation_darkTheme_surfaceColors_pressed"
        )
    }

    @Test
    fun darkTheme_primaryColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomBottomNavigation(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.primary,
                    selectedContentColor = MaterialTheme.colors.onPrimary,
                    unselectedContentColor = MaterialTheme.colors.onPrimary
                )
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "bottomNavigation_darkTheme_primaryColors"
        )
    }

    @Test
    fun darkTheme_primaryColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                scope = rememberCoroutineScope()
                CustomBottomNavigation(
                    interactionSource,
                    backgroundColor = MaterialTheme.colors.primary,
                    selectedContentColor = MaterialTheme.colors.onPrimary,
                    unselectedContentColor = MaterialTheme.colors.onPrimary
                )
            }
        }

        assertBottomNavigationMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "bottomNavigation_darkTheme_primaryColors_pressed"
        )
    }

    /**
     * Asserts that the BottomNavigation matches the screenshot with identifier [goldenIdentifier].
     *
     * @param scope [CoroutineScope] used to interact with [MutableInteractionSource]
     * @param interactionSource the [MutableInteractionSource] used for the first
     * BottomNavigationItem
     * @param interaction the [Interaction] to assert for, or `null` if no [Interaction].
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertBottomNavigationMatches(
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
 * Default colored [BottomNavigation] with three [BottomNavigationItem]s. The first
 * [BottomNavigationItem] is selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [BottomNavigationItem], to
 * control its visual state.
 */
@Composable
private fun DefaultBottomNavigation(
    interactionSource: MutableInteractionSource
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        BottomNavigation {
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                selected = true,
                onClick = {},
                interactionSource = interactionSource
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                selected = false,
                onClick = {}
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                selected = false,
                onClick = {}
            )
        }
    }
}

/**
 * Custom colored [BottomNavigation] with three [BottomNavigationItem]s. The first
 * [BottomNavigationItem] is selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [BottomNavigationItem], to
 * control its visual state.
 * @param backgroundColor the backgroundColor of the [BottomNavigation]
 * @param selectedContentColor the content color for a selected [BottomNavigationItem] (first item)
 * @param unselectedContentColor the content color for an unselected [BottomNavigationItem] (second
 * and third items)
 */
@Composable
private fun CustomBottomNavigation(
    interactionSource: MutableInteractionSource,
    backgroundColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color
) {
    // Apply default emphasis
    @Suppress("NAME_SHADOWING")
    val unselectedContentColor = unselectedContentColor.copy(alpha = ContentAlpha.medium)
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        BottomNavigation(backgroundColor = backgroundColor) {
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                selected = true,
                onClick = {},
                interactionSource = interactionSource,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                selected = false,
                onClick = {},
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                selected = false,
                onClick = {},
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
        }
    }
}

private const val Tag = "BottomNavigation"
