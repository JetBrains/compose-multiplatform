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
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
@OptIn(ExperimentalMaterial3Api::class)
class ListItemScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun oneLine_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                ListItem(headlineText = { Text("One line list item with no icon") })
                Divider()
                ListItem(
                    headlineText = { Text("One line list item with 24x24 icon") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null
                        )
                    }
                )
                Divider()
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "list_oneLine_lightTheme")
    }

    @Test
    fun oneLine_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                ListItem(headlineText = { Text("One line list item with no icon") })
                Divider()
                ListItem(
                    headlineText = { Text("One line list item with 24x24 icon") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null
                        )
                    }
                )
                Divider()
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "list_oneLine_darkTheme")
    }

    @Test
    fun twoLine_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                ListItem(
                    headlineText = { Text("Two line list item") },
                    supportingText = { Text("Secondary text") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Two line list item") },
                    overlineText = { Text("OVERLINE") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Two line list item with 24x24 icon") },
                    supportingText = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null
                        )
                    }
                )
                Divider()
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "list_twoLine_lightTheme")
    }

    @Test
    fun twoLine_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                ListItem(
                    headlineText = { Text("Two line list item") },
                    supportingText = { Text("Secondary text") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Two line list item") },
                    overlineText = { Text("OVERLINE") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Two line list item with 24x24 icon") },
                    supportingText = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null
                        )
                    }
                )
                Divider()
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "list_twoLine_darkTheme")
    }

    @Test
    fun threeLine_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                ListItem(
                    headlineText = { Text("Three line list item") },
                    overlineText = { Text("OVERLINE") },
                    supportingText = { Text("Secondary text") },
                    trailingContent = { Text("meta") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Three line list item") },
                    overlineText = { Text("OVERLINE") },
                    supportingText = { Text("Secondary text") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Three line list item") },
                    overlineText = { Text("OVERLINE") },
                    supportingText = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null
                        )
                    }
                )
                Divider()
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "list_threeLine_lightTheme")
    }

    @Test
    fun threeLine_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                ListItem(
                    headlineText = { Text("Three line list item") },
                    overlineText = { Text("OVERLINE") },
                    supportingText = { Text("Secondary text") },
                    trailingContent = { Text("meta") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Three line list item") },
                    overlineText = { Text("OVERLINE") },
                    supportingText = { Text("Secondary text") }
                )
                Divider()
                ListItem(
                    headlineText = { Text("Three line list item") },
                    overlineText = { Text("OVERLINE") },
                    supportingText = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null
                        )
                    }
                )
                Divider()
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "list_threeLine_darkTheme")
    }
}

private const val Tag = "List"