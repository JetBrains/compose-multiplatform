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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
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
class AlertDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun alertDialog_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(text = "Title")
                },
                text = {
                    Text(
                        "This area typically contains the supportive text " +
                            "which presents the details regarding the Dialog's purpose."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        assertAppBarAgainstGolden(goldenIdentifier = "alertDialog_lightTheme")
    }

    @Test
    fun alertDialog_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(text = "Title")
                },
                text = {
                    Text(
                        "This area typically contains the supportive text " +
                            "which presents the details regarding the Dialog's purpose."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        assertAppBarAgainstGolden(goldenIdentifier = "alertDialog_darkTheme")
    }

    @Test
    fun alertDialog_withIcon_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                title = {
                    Text(text = "Title")
                },
                text = {
                    Text(
                        "This area typically contains the supportive text " +
                            "which presents the details regarding the Dialog's purpose."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        assertAppBarAgainstGolden(goldenIdentifier = "alertDialog_withIcon_lightTheme")
    }

    @Test
    fun alertDialog_withIcon_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                title = {
                    Text(text = "Title")
                },
                text = {
                    Text(
                        "This area typically contains the supportive text " +
                            "which presents the details regarding the Dialog's purpose."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        assertAppBarAgainstGolden(goldenIdentifier = "alertDialog_withIcon_darkTheme")
    }

    private fun assertAppBarAgainstGolden(goldenIdentifier: String) {
        composeTestRule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
