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
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for the Material Menus.
 *
 * Note that currently nodes in a popup cannot be captured to bitmaps. A [DropdownMenu] is
 * displaying its content in a popup, so the tests here focus on the [DropdownMenuContent].
 */
// TODO(b/208991956): Update to include DropdownMenu when popups can be captured into bitmaps.
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class MenuScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val testTag = "dropdown_menu"

    @Test
    fun dropdownMenu_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            TestMenu(enabledItems = true)
        }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_lightTheme")
    }

    @Test
    fun dropdownMenu_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            TestMenu(enabledItems = true)
        }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_darkTheme")
    }

    @Test
    fun dropdownMenu_disabled_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            TestMenu(enabledItems = false)
        }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_disabled_lightTheme")
    }

    @Test
    fun dropdownMenu_disabled_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            TestMenu(enabledItems = false)
        }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_disabled_darkTheme")
    }

    @Composable
    private fun TestMenu(enabledItems: Boolean) {
        Box(Modifier.testTag(testTag).padding(20.dp), contentAlignment = Alignment.Center) {
            DropdownMenuContent(
                expandedStates = MutableTransitionState(initialState = true),
                transformOriginState = mutableStateOf(TransformOrigin.Center)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { },
                    enabled = enabledItems,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null
                        )
                    })
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = { },
                    enabled = enabledItems,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    },
                    trailingIcon = { Text("F11", textAlign = TextAlign.Center) })
                MenuDefaults.Divider()
                DropdownMenuItem(
                    text = { Text("Send Feedback") },
                    onClick = { },
                    enabled = enabledItems,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null
                        )
                    })
            }
        }
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        composeTestRule.onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}