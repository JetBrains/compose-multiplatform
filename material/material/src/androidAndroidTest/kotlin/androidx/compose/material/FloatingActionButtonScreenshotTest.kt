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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
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
class FloatingActionButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun icon() {
        rule.setMaterialContent {
            FloatingActionButton(onClick = { }) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fab_icon")
    }

    @Test
    fun text() {
        rule.setMaterialContent {
            ExtendedFloatingActionButton(
                text = { Text("EXTENDED") },
                onClick = {}
            )
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fab_text")
    }

    @Test
    fun textAndIcon() {
        rule.setMaterialContent {
            ExtendedFloatingActionButton(
                text = { Text("EXTENDED") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                onClick = {}
            )
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fab_textAndIcon")
    }

    @Test
    fun ripple() {
        rule.setMaterialContent {
            Box(Modifier.requiredSize(100.dp, 100.dp).wrapContentSize()) {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                }
            }
        }

        // Start ripple
        rule.onNode(hasClickAction())
            .performTouchInput { down(center) }

        rule.waitForIdle()
        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't
        // properly wait for synchronization. Instead just wait until after the ripples are
        // finished animating.
        Thread.sleep(300)

        rule.onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fab_ripple")
    }

    @Test
    fun hover() {
        rule.setMaterialContent {
            Box(Modifier.requiredSize(100.dp, 100.dp).wrapContentSize()) {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                }
            }
        }

        rule.onNode(hasClickAction())
            .performMouseInput { enter(center) }

        rule.waitForIdle()

        rule.onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fab_hover")
    }

    @Test
    fun focus() {
        val focusRequester = FocusRequester()

        rule.setMaterialContent {
            Box(Modifier.requiredSize(100.dp, 100.dp).wrapContentSize()) {
                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.waitForIdle()

        rule.onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fab_focus")
    }
}
