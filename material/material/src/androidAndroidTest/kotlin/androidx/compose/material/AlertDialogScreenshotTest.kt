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
import androidx.compose.foundation.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.isDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
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
                }
            )
        }

        rule.onNode(isDialog())
            .captureToBitmap()
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
                }
            )
        }

        rule.onNode(isDialog())
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "dialog_stackedButtons")
    }
}