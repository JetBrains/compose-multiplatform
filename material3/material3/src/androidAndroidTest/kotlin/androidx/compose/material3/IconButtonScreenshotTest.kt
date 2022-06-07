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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
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
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class IconButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "iconButtonWrapper"

    @Test
    fun iconButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("iconButton_lightTheme")
    }

    @Test
    fun iconButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("iconButton_darkTheme")
    }

    @Test
    fun iconButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconButton(onClick = { /* doSomething() */ }, enabled = false) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("iconButton_lightTheme_disabled")
    }

    @Test
    fun iconButton_lightTheme_pressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(hasClickAction())
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("iconButton_lightTheme_pressed")
    }

    @Test
    fun iconButton_lightTheme_hovered() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performMouseInput {
            enter(center)
        }

        assertAgainstGolden("iconButton_lightTheme_hovered")
    }

    @Test
    fun iconButton_lightTheme_focused() {
        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconButton(onClick = { /* doSomething() */ },
                    modifier = Modifier
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        assertAgainstGolden("iconButton_lightTheme_focused")
    }

    @Test
    fun iconToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconToggleButton(checked = false, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("iconToggleButton_lightTheme")
    }

    @Test
    fun iconToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconToggleButton(checked = false, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("iconToggleButton_darkTheme")
    }

    @Test
    fun iconToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("iconToggleButton_checked_lightTheme")
    }

    @Test
    fun iconToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                IconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("iconToggleButton_checked_darkTheme")
    }

    @Test
    fun filledIconButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledIconButton_lightTheme")
    }

    @Test
    fun filledIconButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("filledIconButton_darkTheme")
    }

    @Test
    fun filledIconToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconToggleButton(checked = false, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledIconToggleButton_lightTheme")
    }

    @Test
    fun filledIconToggleButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconToggleButton(
                    checked = false,
                    onCheckedChange = { /* doSomething() */ },
                    enabled = false
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledIconToggleButton_lightTheme_disabled")
    }

    @Test
    fun filledIconToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconToggleButton(checked = false, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledIconToggleButton_darkTheme")
    }

    @Test
    fun filledIconToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledIconToggleButton_checked_lightTheme")
    }

    @Test
    fun filledIconToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledIconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledIconToggleButton_checked_darkTheme")
    }

    @Test
    fun filledTonalIconButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledTonalIconButton_lightTheme")
    }

    @Test
    fun filledTonalIconButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconButton(
                    onClick = { /* doSomething() */ },
                    enabled = false
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledTonalIconButton_lightTheme_disabled")
    }

    @Test
    fun filledTonalIconButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("filledTonalIconButton_darkTheme")
    }

    @Test
    fun filledTonalIconToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconToggleButton(
                    checked = false,
                    onCheckedChange = { /* doSomething() */ }
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledTonalIconToggleButton_lightTheme")
    }

    @Test
    fun filledTonalIconToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconToggleButton(
                    checked = false,
                    onCheckedChange = { /* doSomething() */ }
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledTonalIconToggleButton_darkTheme")
    }

    @Test
    fun filledTonalIconToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconToggleButton(
                    checked = true,
                    onCheckedChange = { /* doSomething() */ }
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledTonalIconToggleButton_checked_lightTheme")
    }

    @Test
    fun filledTonalIconToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledTonalIconToggleButton(
                    checked = true,
                    onCheckedChange = { /* doSomething() */ }
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("filledTonalIconToggleButton_checked_darkTheme")
    }

    @Test
    fun outlinedIconButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("outlinedIconButton_lightTheme")
    }

    @Test
    fun outlinedButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconButton(onClick = { /* doSomething() */ }, enabled = false) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("outlinedButton_lightTheme_disabled")
    }

    @Test
    fun outlinedIconButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("outlinedIconButton_darkTheme")
    }

    @Test
    fun outlinedIconToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconToggleButton(
                    checked = false,
                    onCheckedChange = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("outlinedIconToggleButton_lightTheme")
    }

    @Test
    fun outlinedIconToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconToggleButton(
                    checked = false,
                    onCheckedChange = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        assertAgainstGolden("outlinedIconToggleButton_darkTheme")
    }

    @Test
    fun outlinedIconToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconToggleButton(
                    checked = true,
                    onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("outlinedIconToggleButton_checked_lightTheme")
    }

    @Test
    fun outlinedIconToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedIconToggleButton(
                    checked = true,
                    onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertAgainstGolden("outlinedIconToggleButton_checked_darkTheme")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}