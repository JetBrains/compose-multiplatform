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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
@OptIn(ExperimentalTestApi::class)
class IconButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "iconButtonWrapper"

    @Test
    fun iconButton_lightTheme() {
        rule.setMaterialContent {
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
        rule.setContent {
            MaterialTheme(darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(wrap.testTag(wrapperTestTag)) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                }
            }
        }
        assertAgainstGolden("iconButton_darkTheme")
    }

    @Test
    fun iconButton_lightTheme_disabled() {

        rule.setMaterialContent {
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
        rule.setMaterialContent {
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
        rule.setMaterialContent {
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

        rule.setMaterialContent {
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

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}