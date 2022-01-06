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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
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
class ButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun default_button_light_theme() {
        rule.setMaterialContent(lightColorScheme()) {
            Button(onClick = { }) {
                Text("Button")
            }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_light_theme")
    }

    @Test
    fun default_button_dark_theme() {
        rule.setMaterialContent(darkColorScheme()) {
            Button(onClick = { }) {
                Text("Button")
            }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_dark_theme")
    }

    @Test
    fun disabled_button_light_theme() {
        rule.setMaterialContent(lightColorScheme()) {
            Button(onClick = { }, enabled = false) {
                Text("Button")
            }
        }

        rule.onNodeWithText("Button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_disabled_light_theme")
    }

    @Test
    fun disabled_button_dark_theme() {
        rule.setMaterialContent(darkColorScheme()) {
            Button(onClick = { }, enabled = false) {
                Text("Button")
            }
        }

        rule.onNodeWithText("Button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_disabled_dark_theme")
    }

    @Test
    fun elevated_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(
                Modifier.requiredSize(200.dp, 100.dp).wrapContentSize().testTag("elevated button")
            ) {
                ElevatedButton(onClick = {}) { Text("Elevated Button") }
            }
        }

        rule.onNodeWithTag("elevated button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "elevated_button_light_theme")
    }

    @Test
    fun disabled_elevated_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(
                Modifier.requiredSize(200.dp, 100.dp).wrapContentSize().testTag("elevated button")
            ) {
                ElevatedButton(onClick = {}, enabled = false) { Text("Elevated Button") }
            }
        }

        rule
            .onNodeWithTag("elevated button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "elevated_button_disabled_light_theme")
    }

    @Test
    fun filled_tonal_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledTonalButton(onClick = {}) { Text("Filled tonal Button") }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "filled_tonal_button_light_theme")
    }

    @Test
    fun disabled_filled_tonal_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledTonalButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("button")
            ) {
                Text("Filled tonal Button")
            }
        }

        rule.onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "filled_tonal_button_disabled_light_theme")
    }

    @Test
    fun outlined_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedButton(onClick = {}) { Text("Outlined Button") }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlined_button_light_theme")
    }

    @Test
    fun disabled_outlined_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.testTag("button")) {
                Text("Outlined Button")
            }
        }

        rule.onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlined_button_disabled_light_theme")
    }

    @Test
    fun text_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            TextButton(onClick = {}) {
                Text("Text Button")
            }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_button_light_theme")
    }

    @Test
    fun disabled_text_button_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            TextButton(onClick = {}, enabled = false, modifier = Modifier.testTag("button")) {
                Text("Text Button")
            }
        }

        rule.onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_button_disabled_light_theme")
    }
}
