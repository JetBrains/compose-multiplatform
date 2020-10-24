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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.ui.test.captureToBitmap
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.test.move
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.up
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class RadioButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)

    // TODO: this test tag as well as Boxes inside tests are temporarty, remove then b/157687898
    //  is fixed
    private val wrapperTestTag = "radioButtonWrapper"

    @Test
    fun radioButtonTest_selected() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_selected")
    }

    @Test
    fun radioButtonTest_notSelected() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_notSelected")
    }

    @Test
    fun radioButtonTest_pressed() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performGesture {
            down(center)
        }
        assertSelectableAgainstGolden("radioButton_pressed")
    }

    @Test
    fun radioButtonTest_disabled_selected() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_disabled_selected")
    }

    @Test
    fun radioButtonTest_disabled_notSelected() {
        rule.setMaterialContent {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_disabled_notSelected")
    }

    @Test
    fun radioButton_notSelected_animateToSelected() {
        rule.setMaterialContent {
            val isSelected = remember { mutableStateOf(false) }
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = isSelected.value,
                    onClick = { isSelected.value = !isSelected.value }
                )
            }
        }

        rule.clockTestRule.pauseClock()

        rule.onNode(isInMutuallyExclusiveGroup())
            // split click into (down) and (move, up) to enforce a composition in between
            .performGesture { down(center) }
            .performGesture { move(); up() }

        rule.waitForIdle()

        rule.clockTestRule.advanceClock(60)

        assertSelectableAgainstGolden("radioButton_animateToSelected")
    }

    @Test
    fun radioButton_selected_animateToNotSelected() {
        rule.setMaterialContent {
            val isSelected = remember { mutableStateOf(true) }
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = isSelected.value,
                    onClick = { isSelected.value = !isSelected.value }
                )
            }
        }

        rule.clockTestRule.pauseClock()

        rule.onNode(isInMutuallyExclusiveGroup())
            // split click into (down) and (move, up) to enforce a composition in between
            .performGesture { down(center) }
            .performGesture { move(); up() }

        rule.waitForIdle()

        rule.clockTestRule.advanceClock(60)

        assertSelectableAgainstGolden("radioButton_animateToNotSelected")
    }

    private fun assertSelectableAgainstGolden(goldenName: String) {
        // TODO: replace with find(isInMutuallyExclusiveGroup()) after b/157687898 is fixed
        rule.onNodeWithTag(wrapperTestTag)
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}