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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
@OptIn(ExperimentalComposeUiApi::class)
class AlertDialogScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun sideBySideButtons() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Title") },
                text = { Text("Some content") },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {}) {
                        Text("Dismiss")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_sideBySideButtons")
    }

    @Test
    fun stackedButtons() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Title") },
                text = { Text("Some content") },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Very long confirm button")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {}) {
                        Text("Very long dismiss button")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_stackedButtons")
    }

    @Test
    fun onlyTitle() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Title") },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Ok")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_onlyTitle")
    }

    @Test
    fun onlyText() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                text = { Text("Text") },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Ok")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_onlyText")
    }

    @Test
    fun noTitleOrText() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Ok")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_noTitleOrText")
    }

    @Test
    fun titleWithoutTextBaseline() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Box(
                        Modifier.requiredSize(75.dp, 25.dp).background(MaterialTheme.colors.primary)
                    )
                },
                text = { Text("Text") },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Ok")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_titleWithoutTextBaseline")
    }

    @Test
    fun textWithoutTextBaseline() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Title") },
                text = {
                    Box(
                        Modifier.requiredSize(300.dp, 50.dp)
                            .background(MaterialTheme.colors.primary)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Ok")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_textWithoutTextBaseline")
    }

    @Test
    fun titleAndTextWithoutTextBaselines() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Box(
                        Modifier.requiredSize(75.dp, 25.dp)
                            .background(MaterialTheme.colors.primary)
                    )
                },
                text = {
                    Box(
                        Modifier.requiredSize(300.dp, 50.dp)
                            .background(MaterialTheme.colors.primary)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {}) {
                        Text("Ok")
                    }
                },
                properties = DialogProperties(useDefaultMaxWidth = true)
            )
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialog_titleAndTextWithoutTextBaselines")
    }
}
