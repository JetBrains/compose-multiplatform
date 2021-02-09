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
package androidx.compose.ui.window

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.GOLDEN_UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
class DialogScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    @Test
    fun dialogWithNoElevation() {
        rule.setContent {
            Dialog(onDismissRequest = {}) {
                Box(
                    Modifier
                        .graphicsLayer(shape = RoundedCornerShape(percent = 15), clip = true)
                        .size(200.dp)
                        .background(Color(0xFFA896B0))
                )
            }
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialogWithNoElevation")
    }

    @Test
    fun dialogWithElevation() {
        rule.setContent {
            Dialog(onDismissRequest = {}) {
                val elevation = with(LocalDensity.current) { 16.dp.toPx() }
                Box(
                    Modifier
                        .graphicsLayer(
                            shadowElevation = elevation,
                            shape = RoundedCornerShape(percent = 15),
                            clip = true
                        )
                        .size(200.dp)
                        .background(Color(0xFFA896B0))
                )
            }
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dialogWithElevation")
    }
}
