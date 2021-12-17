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
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
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
@OptIn(ExperimentalTestApi::class)
class SnackbarScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val snackbarTestTag = "snackbarTestTag"

    @Test
    fun snackbar_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            TestSnackbar()
        }
        assertAgainstGolden("snackbar_lightTheme")
    }

    @Test
    fun snackbar_withAction_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            TestSnackbar(showAction = true)
        }
        assertAgainstGolden("snackbar_withAction_lightTheme")
    }

    @Test
    fun snackbar_withDismiss_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            TestSnackbar(showAction = true, duration = SnackbarDuration.Indefinite)
        }
        assertAgainstGolden("snackbar_withDismiss_lightTheme")
    }

    @Test
    fun snackbar_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            TestSnackbar()
        }
        assertAgainstGolden("snackbar_darkTheme")
    }

    @Test
    fun snackbar_withAction_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            TestSnackbar(showAction = true)
        }
        assertAgainstGolden("snackbar_withAction_darkTheme")
    }

    @Test
    fun snackbar_withDismiss_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            TestSnackbar(showAction = true, duration = SnackbarDuration.Indefinite)
        }
        assertAgainstGolden("snackbar_withDismiss_darkTheme")
    }

    @Composable
    private fun TestSnackbar(
        showAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        Snackbar(
            snackbarData = object : SnackbarData {
                override val visuals: SnackbarVisuals = object : SnackbarVisuals {
                    override val message: String = "Snackbar message"
                    override val actionLabel: String? = if (showAction) "Undo" else null
                    override val withDismissAction: Boolean =
                        duration == SnackbarDuration.Indefinite
                    override val duration: SnackbarDuration = duration
                }

                override fun performAction() {
                    // no-op
                }

                override fun dismiss() {
                    // no-op
                }
            },
            modifier = Modifier.testTag(snackbarTestTag),
        )
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(snackbarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
