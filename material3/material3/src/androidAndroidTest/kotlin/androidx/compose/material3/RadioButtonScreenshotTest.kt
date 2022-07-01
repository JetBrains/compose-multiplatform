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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
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
class RadioButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "radioButtonWrapper"

    @Test
    fun radioButton_lightTheme_selected() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_lightTheme_selected")
    }

    @Test
    fun radioButton_darkTheme_selected() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_darkTheme_selected")
    }

    @Test
    fun radioButton_lightTheme_notSelected() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_lightTheme_notSelected")
    }

    @Test
    fun radioButton_darkTheme_notSelected() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_darkTheme_notSelected")
    }

    @Test
    fun radioButton_lightTheme_pressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isSelectable())
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertSelectableAgainstGolden("radioButton_lightTheme_pressed")
    }

    @Test
    fun radioButton_darkTheme_pressed() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isSelectable())
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertSelectableAgainstGolden("radioButton_darkTheme_pressed")
    }

    @Test
    fun radioButton_lightTheme_hovered() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performMouseInput {
            enter(center)
        }

        assertSelectableAgainstGolden("radioButton_lightTheme_hovered")
    }

    @Test
    fun radioButton_darkTheme_hovered() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performMouseInput {
            enter(center)
        }

        assertSelectableAgainstGolden("radioButton_darkTheme_hovered")
    }

    @Test
    fun radioButton_lightTheme_focused() {
        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = false,
                    onClick = {},
                    modifier = Modifier
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                )
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        assertSelectableAgainstGolden("radioButton_lightTheme_focused")
    }

    @Test
    fun radioButton_darkTheme_focused() {
        val focusRequester = FocusRequester()

        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = false,
                    onClick = {},
                    modifier = Modifier
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                )
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        assertSelectableAgainstGolden("radioButton_darkTheme_focused")
    }

    @Test
    fun radioButton_lightTheme_disabled_selected() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_lightTheme_disabled_selected")
    }

    @Test
    fun radioButton_darkTheme_disabled_selected() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_darkTheme_disabled_selected")
    }

    @Test
    fun radioButton_lightTheme_disabled_notSelected() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_lightTheme_disabled_notSelected")
    }

    @Test
    fun radioButton_darkTheme_disabled_notSelected() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_darkTheme_disabled_notSelected")
    }

    @Test
    fun radioButton_lightTheme_notSelected_animateToSelected() {
        rule.setMaterialContent(lightColorScheme()) {
            val isSelected = remember { mutableStateOf(false) }
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = isSelected.value,
                    onClick = { isSelected.value = !isSelected.value }
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNode(isSelectable())
            // split click into (down) and (move, up) to enforce a composition in between
            .performTouchInput { down(center) }
            .performTouchInput { move(); up() }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 80)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertSelectableAgainstGolden("radioButton_lightTheme_notSelected_animateToSelected")
    }

    @Test
    fun radioButton_darkTheme_notSelected_animateToSelected() {
        rule.setMaterialContent(darkColorScheme()) {
            val isSelected = remember { mutableStateOf(false) }
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = isSelected.value,
                    onClick = { isSelected.value = !isSelected.value }
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNode(isSelectable())
            // split click into (down) and (move, up) to enforce a composition in between
            .performTouchInput { down(center) }
            .performTouchInput { move(); up() }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 80)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertSelectableAgainstGolden("radioButton_darkTheme_notSelected_animateToSelected")
    }

    @Test
    fun radioButton_lightTheme_selected_animateToNotSelected() {
        rule.setMaterialContent(lightColorScheme()) {
            val isSelected = remember { mutableStateOf(true) }
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = isSelected.value,
                    onClick = { isSelected.value = !isSelected.value }
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNode(isSelectable())
            // split click into (down) and (move, up) to enforce a composition in between
            .performTouchInput { down(center) }
            .performTouchInput { move(); up() }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 80)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertSelectableAgainstGolden("radioButton_lightTheme_selected_animateToNotSelected")
    }

    @Test
    fun radioButton_darkTheme_selected_animateToNotSelected() {
        rule.setMaterialContent(darkColorScheme()) {
            val isSelected = remember { mutableStateOf(true) }
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = isSelected.value,
                    onClick = { isSelected.value = !isSelected.value }
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNode(isSelectable())
            // split click into (down) and (move, up) to enforce a composition in between
            .performTouchInput { down(center) }
            .performTouchInput { move(); up() }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 80)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertSelectableAgainstGolden("radioButton_darkTheme_selected_animateToNotSelected")
    }

    private fun assertSelectableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}