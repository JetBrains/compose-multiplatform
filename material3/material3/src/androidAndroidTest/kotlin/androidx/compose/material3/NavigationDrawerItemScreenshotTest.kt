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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
class NavigationDrawerItemScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun lightTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultDrawerItems(interactionSource)
        }

        assertDrawerItemMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "drawerItem_lightTheme_defaultColors"
        )
    }

    @Test
    fun lightTheme_defaultColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultDrawerItems(interactionSource)
        }

        assertDrawerItemMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "drawerItem_lightTheme_defaultColors_pressed"
        )
    }

    @Test
    fun darkTheme_defaultColors() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultDrawerItems(interactionSource)
        }

        assertDrawerItemMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "drawerItem_darkTheme_defaultColors"
        )
    }

    @Test
    fun darkTheme_defaultColors_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultDrawerItems(interactionSource)
        }

        assertDrawerItemMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "drawerItem_darkTheme_defaultColors_pressed"
        )
    }

    /**
     * Asserts that the NavigationDrawerItem matches the screenshot with identifier
     * [goldenIdentifier].
     *
     * @param scope [CoroutineScope] used to interact with [MutableInteractionSource]
     * @param interactionSource the [MutableInteractionSource] used for the first
     * [NavigationDrawerItem]
     * @param interaction the [Interaction] to assert for, or `null` if no [Interaction].
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertDrawerItemMatches(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultDrawerItems(
    interactionSource: MutableInteractionSource,
) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Favorite, null) },
            label = { Text("Favorites") },
            selected = true,
            onClick = {},
            interactionSource = interactionSource
        )
        NavigationDrawerItem(
            label = { Text("Favorites") },
            selected = true,
            onClick = {},
            interactionSource = interactionSource
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Face, null) },
            label = { Text("Face") },
            selected = false,
            onClick = {},
            interactionSource = interactionSource
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Face, null) },
            label = { Text("Face") },
            badge = { Text("100+") },
            selected = false,
            onClick = {},
            interactionSource = interactionSource
        )
    }
}

private const val Tag = "NavigationDrawerItem"