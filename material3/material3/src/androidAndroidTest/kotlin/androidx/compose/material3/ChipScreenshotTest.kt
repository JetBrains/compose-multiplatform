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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class ChipScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun assistChip_flat_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            AssistChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Assist chip") }
            )
        }
        assertChipAgainstGolden("assistChip_flat_lightTheme")
    }

    @Test
    fun assistChip_flat_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            AssistChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Assist chip") }
            )
        }
        assertChipAgainstGolden("assistChip_flat_darkTheme")
    }

    @Test
    fun assistChip_elevated_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            ElevatedAssistChip(
                onClick = {},
                modifier = Modifier.testTag(TestTag),
                label = { Text("Assist chip") }
            )
        }
        assertChipAgainstGolden("assistChip_elevated_lightTheme")
    }

    @Test
    fun assistChip_elevated_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            ElevatedAssistChip(
                onClick = {},
                modifier = Modifier.testTag(TestTag),
                label = { Text("Assist chip") }
            )
        }
        assertChipAgainstGolden("assistChip_elevated_darkTheme")
    }

    @Test
    fun assistChip_flat_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            AssistChip(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Assist chip") }
            )
        }
        assertChipAgainstGolden("assistChip_flat_disabled_lightTheme")
    }

    @Test
    fun assistChip_elevated_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            ElevatedAssistChip(
                onClick = {},
                modifier = Modifier.testTag(TestTag),
                enabled = false,
                label = { Text("Assist chip") }
            )
        }
        assertChipAgainstGolden("assistChip_elevated_disabled_lightTheme")
    }

    @Test
    fun inputChip_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            InputChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Input chip") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(InputChipDefaults.IconSize)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(InputChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("inputChip_lightTheme")
    }

    @Test
    fun inputChip_withAvatar_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            InputChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Input chip") },
                avatar = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(InputChipDefaults.AvatarSize)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(InputChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("inputChip_withAvatar_lightTheme")
    }

    @Test
    fun inputChip_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            InputChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Input chip") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(InputChipDefaults.IconSize)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(InputChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("inputChip_darkTheme")
    }

    @Test
    fun inputChip_disabled_lightTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            InputChip(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Input chip") }
            )
        }
        assertChipAgainstGolden("inputChip_disabled_lightTheme")
    }

    @Test
    fun inputChip_disabled_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            InputChip(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Input chip") }
            )
        }
        assertChipAgainstGolden("inputChip_disabled_darkTheme")
    }

    @Test
    fun filterChip_flat_selected_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Filter Chip") },
                modifier = Modifier.testTag(TestTag),
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("filterChip_flat_selected_lightTheme")
    }

    @Test
    fun filterChip_flat_withLeadingIcon_selected_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Filter Chip") },
                modifier = Modifier.testTag(TestTag),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Localized Description"
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("filterChip_flat_withLeadingIcon_selected_lightTheme")
    }

    @Test
    fun filterChip_flat_withLeadingIcon_selected_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Filter Chip") },
                modifier = Modifier.testTag(TestTag),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Localized Description"
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("filterChip_flat_withLeadingIcon_selected_darkTheme")
    }

    @Test
    fun filterChip_flat_notSelected() {
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Filter Chip") },
                modifier = Modifier.testTag(TestTag)
            )
        }
        assertChipAgainstGolden("filterChip_flat_notSelected")
    }

    @Test
    fun filterChip_flat_disabled_selected() {
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Filter Chip") },
                enabled = false,
                modifier = Modifier.testTag(TestTag),
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        tint = LocalContentColor.current,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("filterChip_flat_disabled_selected")
    }

    @Test
    fun filterChip_flat_withLeadingIcon_disabled_selected() {
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Filter Chip") },
                enabled = false,
                modifier = Modifier.testTag(TestTag),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Localized Description"
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("filterChip_flat_withLeadingIcon_disabled_selected")
    }

    @Test
    fun filterChip_flat_disabled_notSelected() {
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Filter Chip") },
                enabled = false,
                modifier = Modifier.testTag(TestTag)
            )
        }
        assertChipAgainstGolden("filterChip_flat_disabled_notSelected")
    }

    @Test
    fun filterChip_elevated_selected_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Filter Chip") },
                modifier = Modifier.testTag(TestTag),
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        assertChipAgainstGolden("filterChip_elevated_selected_darkTheme")
    }

    @Test
    fun suggestionChip_flat_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Suggestion Chip") }
            )
        }
        assertChipAgainstGolden("suggestionChip_flat_lightTheme")
    }

    @Test
    fun suggestionChip_flat_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            SuggestionChip(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Suggestion Chip") }
            )
        }
        assertChipAgainstGolden("suggestionChip_flat_darkTheme")
    }

    @Test
    fun suggestionChip_elevated_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            ElevatedSuggestionChip(
                onClick = {},
                modifier = Modifier.testTag(TestTag),
                label = { Text("Suggestion Chip") }
            )
        }
        assertChipAgainstGolden("suggestionChip_elevated_lightTheme")
    }

    @Test
    fun suggestionChip_elevated_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            ElevatedSuggestionChip(
                onClick = {},
                modifier = Modifier.testTag(TestTag),
                label = { Text("Suggestion Chip") }
            )
        }
        assertChipAgainstGolden("suggestionChip_elevated_darkTheme")
    }

    @Test
    fun suggestionChip_flat_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TestTag),
                label = { Text("Suggestion Chip") }
            )
        }
        assertChipAgainstGolden("suggestionChip_flat_disabled_lightTheme")
    }

    @Test
    fun suggestionChip_elevated_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            ElevatedSuggestionChip(
                onClick = {},
                modifier = Modifier.testTag(TestTag),
                enabled = false,
                label = { Text("Suggestion Chip") }
            )
        }
        assertChipAgainstGolden("suggestionChip_elevated_disabled_lightTheme")
    }

    private fun assertChipAgainstGolden(goldenIdentifier: String) {
        rule.onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

private const val TestTag = "chip"
