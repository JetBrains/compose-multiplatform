/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class ButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun default_button() {
        rule.setMaterialContent {
            Button(onClick = { }) {
                Text("Button")
            }
        }

        rule.onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_default")
    }

    @Test
    fun disabled_button() {
        rule.setMaterialContent {
            Button(onClick = { }, enabled = false) {
                Text("Button")
            }
        }

        rule.onNodeWithText("Button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_disabled")
    }

    @Test
    fun ripple() {
        rule.mainClock.autoAdvance = false

        rule.setMaterialContent {
            Box(Modifier.requiredSize(200.dp, 100.dp).wrapContentSize()) {
                Button(onClick = { }) { }
            }
        }

        // Start ripple
        rule.onNode(hasClickAction())
            .performTouchInput { down(center) }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(100)

        rule.waitForIdle()
        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't
        // properly wait for synchronization. Instead just wait until after the ripples are
        // finished animating.
        Thread.sleep(300)

        rule.onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_ripple")
    }

    @Test
    fun hover() {
        rule.setMaterialContent {
            Box(Modifier.requiredSize(200.dp, 100.dp).wrapContentSize()) {
                Button(onClick = { }) { }
            }
        }

        rule.onNode(hasClickAction())
            .performMouseInput { enter(center) }

        rule.waitForIdle()

        rule.onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_hover")
    }
}