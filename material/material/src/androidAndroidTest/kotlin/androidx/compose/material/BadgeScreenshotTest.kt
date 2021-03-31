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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
@OptIn(ExperimentalMaterialApi::class, ExperimentalTestApi::class)
class BadgeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun darkTheme_withContent() {
        composeTestRule.setContent {
            MaterialTheme(darkColors()) {
                Box(
                    Modifier.size(56.dp).semantics(mergeDescendants = true) {}.testTag(TestTag),
                    contentAlignment = Alignment.Center
                ) {
                    BadgeBox(badgeContent = { Text("8") }) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_darkTheme_withContent"
        )
    }

    @Test
    fun lightTheme_noContent_bottomNavigation() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                BottomNavigation(
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                ) {
                    BottomNavigationItem(
                        icon = {
                            BadgeBox {
                                Icon(Icons.Filled.Favorite, null)
                            }
                        },
                        selected = true,
                        onClick = {},
                    )
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_noContent_bottomNavigation"
        )
    }

    @Test
    fun lightTheme_shortContent_bottomNavigation() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                BottomNavigation(
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                ) {
                    BottomNavigationItem(
                        icon = {
                            BadgeBox(
                                badgeContent = {
                                    Text(
                                        "8",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Favorite, null)
                            }
                        },
                        selected = false,
                        onClick = {}
                    )
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_shortContent_bottomNavigation"
        )
    }

    @Test
    fun lightTheme_longContent_bottomNavigation() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                BottomNavigation(
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                ) {
                    BottomNavigationItem(
                        icon = {
                            BadgeBox(
                                badgeContent = {
                                    Text(
                                        "99+",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Favorite, null)
                            }
                        },
                        selected = false,
                        onClick = {}
                    )
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_longContent_bottomNavigation"
        )
    }

    @Test
    fun lightTheme_badge_noContent_tab() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                Tab(
                    text = {
                        BadgeBox {
                            Text("TAB")
                        }
                    },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                )
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_noContent_tab"
        )
    }

    @Test
    fun lightTheme_badge_shortContent_tab() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                // A round badge with the text `8` attached to a tab.
                Tab(
                    text = {
                        BadgeBox(
                            badgeContent = {
                                Text(
                                    "8",
                                    textAlign = TextAlign.Center
                                )
                            }
                        ) {
                            Text("TAB")
                        }
                    },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                )
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_shortContent_tab"
        )
    }

    @Test
    fun lightTheme_badge_longContent_tab() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                // Tab with a pilled shape badge with the text `99+`.
                Tab(
                    text = {
                        BadgeBox(
                            badgeContent = {
                                Text(
                                    "99+",
                                    textAlign = TextAlign.Center
                                )
                            }
                        ) {
                            Text("TAB")
                        }
                    },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                )
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_longContent_tab"
        )
    }

    @Test
    fun lightTheme_badge_shortContent_leadingIconTab() {
        composeTestRule.setContent {
            MaterialTheme(lightColors()) {
                // A round badge with the text `8` attached to a leading icon tab.
                LeadingIconTab(
                    icon = {
                        BadgeBox(
                            badgeContent = {
                                Text(
                                    "8",
                                    textAlign = TextAlign.Center
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Favorite, null)
                        }
                    },
                    text = {
                        Text("TAB")
                    },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)
                )
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_shortContent_leadingIconTab"
        )
    }

    private fun assertBadgeAgainstGolden(goldenIdentifier: String) {
        composeTestRule.onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

private const val TestTag = "badge"
