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
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
class AppBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun smallAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                SmallTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "smallAppBar_lightTheme")
    }

    @Test
    fun smallAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                SmallTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "smallAppBar_darkTheme")
    }

    @Test
    fun centerAlignedAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "centerAlignedAppBar_lightTheme")
    }

    @Test
    fun centerAlignedAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "centerAlignedAppBar_darkTheme")
    }

    @Test
    fun mediumAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "mediumAppBar_lightTheme")
    }

    @Test
    fun mediumAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "mediumAppBar_darkTheme")
    }

    @Test
    fun largeAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "largeAppBar_lightTheme")
    }

    @Test
    fun largeAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text("Title")
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Like"
                            )
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "largeAppBar_darkTheme")
    }

    @Test
    fun bottomAppBarWithFAB_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                BottomAppBar(
                    icons = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    floatingActionButton = {
                        BottomAppBarDefaults.FloatingActionButton(
                            onClick = { /* do something */ }
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarWithFAB_lightTheme",
            testTag = BottomAppBarTestTag
        )
    }

    @Test
    fun bottomAppBarWithFAB_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                BottomAppBar(
                    icons = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    floatingActionButton = {
                        BottomAppBarDefaults.FloatingActionButton(
                            onClick = { /* do something */ }
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarWithFAB_darkTheme",
            testTag = BottomAppBarTestTag
        )
    }

    private fun assertAppBarAgainstGolden(
        goldenIdentifier: String,
        testTag: String = TopAppBarTestTag
    ) {
        composeTestRule.onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }

    private val TopAppBarTestTag = "topAppBar"
    private val BottomAppBarTestTag = "bottomAppBar"
}
