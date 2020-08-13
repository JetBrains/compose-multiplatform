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
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.ui.test.captureToBitmap
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNode
import androidx.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.ui.test.hasClickAction
import androidx.ui.test.down
import androidx.ui.test.onRoot
import androidx.ui.test.waitForIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ButtonScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun default_button() {
        composeTestRule.setMaterialContent {
            Button(onClick = { }) {
                Text("Button")
            }
        }

        onNode(hasClickAction())
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "button_default")
    }

    @Test
    fun disabled_button() {
        composeTestRule.setMaterialContent {
            Button(onClick = { }, enabled = false) {
                Text("Button")
            }
        }

        onNodeWithText("Button")
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "button_disabled")
    }

    @Test
    fun ripple() {
        composeTestRule.setMaterialContent {
            Stack(Modifier.size(200.dp, 100.dp).wrapContentSize()) {
                Button(onClick = { }) { }
            }
        }

        composeTestRule.clockTestRule.pauseClock()

        // Start ripple
        onNode(hasClickAction())
            .performGesture { down(center) }

        // Let ripple propagate
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(50)

        onRoot()
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "button_ripple")
    }
}