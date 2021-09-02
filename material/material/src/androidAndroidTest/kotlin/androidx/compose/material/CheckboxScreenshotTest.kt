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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class CheckboxScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    val wrap = Modifier.wrapContentSize(Alignment.TopStart)

    // TODO: this test tag as well as Boxes inside testa are temporarty, remove then b/157687898
    //  is fixed
    private val wrapperTestTag = "checkboxWrapper"

    @Test
    fun checkBoxTest_checked() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(checked = true, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("checkbox_checked")
    }

    @Test
    fun checkBoxTest_unchecked() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("checkbox_unchecked")
    }

    @Test
    fun checkBoxTest_pressed() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = false, onCheckedChange = { })
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performTouchInput {
            down(center)
        }
        assertToggeableAgainstGolden("checkbox_pressed")
    }

    @Test
    fun checkBoxTest_indeterminate() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                TriStateCheckbox(
                    state = ToggleableState.Indeterminate,
                    modifier = wrap,
                    onClick = {}
                )
            }
        }
        assertToggeableAgainstGolden("checkbox_indeterminate")
    }

    @Test
    fun checkBoxTest_disabled_checked() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = true, enabled = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("checkbox_disabled_checked")
    }

    @Test
    fun checkBoxTest_disabled_unchecked() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = false, enabled = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("checkbox_disabled_unchecked")
    }

    @Test
    fun checkBoxTest_disabled_indeterminate() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                TriStateCheckbox(
                    state = ToggleableState.Indeterminate,
                    enabled = false,
                    modifier = wrap,
                    onClick = {}
                )
            }
        }
        assertToggeableAgainstGolden("checkbox_disabled_indeterminate")
    }

    @Test
    fun checkBoxTest_unchecked_animateToChecked() {
        rule.setMaterialContent {
            val isChecked = remember { mutableStateOf(false) }
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap,
                    checked = isChecked.value,
                    onCheckedChange = { isChecked.value = it }
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNode(isToggleable())
            // split click into (down) and (move, up) to enforce a composition in between
            .performTouchInput { down(center) }
            .performTouchInput { move(); up() }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 80)

        assertToggeableAgainstGolden("checkbox_animateToChecked")
    }

    @Test
    fun checkBoxTest_checked_animateToUnchecked() {
        rule.setMaterialContent {
            val isChecked = remember { mutableStateOf(true) }
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap,
                    checked = isChecked.value,
                    onCheckedChange = { isChecked.value = it }
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNode(isToggleable())
            // split click into (down) and (move, up) to enforce a composition in between
            .performTouchInput { down(center) }
            .performTouchInput { move(); up() }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 80)

        assertToggeableAgainstGolden("checkbox_animateToUnchecked")
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        // TODO: replace with find(isToggeable()) after b/157687898 is fixed
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
