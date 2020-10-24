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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SliderScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    val wrap = Modifier.width(70.dp).wrapContentSize(Alignment.TopStart)

    private val wrapperTestTag = "sliderWrapper"

    @Test
    fun sliderTest_origin() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                var position by remember { mutableStateOf(0f) }
                Slider(position, { position = it })
            }
        }
        assertSliderAgainstGolden("slider_origin")
    }

    @Test
    fun sliderTest_middle() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                var position by remember { mutableStateOf(0.5f) }
                Slider(position, { position = it })
            }
        }
        assertSliderAgainstGolden("slider_middle")
    }

    @Test
    fun sliderTest_end() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                var position by remember { mutableStateOf(1f) }
                Slider(position, { position = it })
            }
        }
        assertSliderAgainstGolden("slider_end")
    }

    @Test
    fun sliderTest_middle_steps() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                var position by remember { mutableStateOf(0.5f) }
                Slider(position, { position = it }, steps = 5)
            }
        }
        assertSliderAgainstGolden("slider_middle_steps")
    }

    @Test
    fun sliderTest_customColors() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                var position by remember { mutableStateOf(0.5f) }
                Slider(
                    value = position,
                    onValueChange = { position = it },
                    steps = 5,
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Blue,
                    activeTickColor = Color.Yellow,
                    inactiveTickColor = Color.Magenta
                )
            }
        }
        assertSliderAgainstGolden("slider_customColors")
    }

    private fun assertSliderAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}