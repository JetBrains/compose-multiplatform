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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
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
class SwitchScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    // TODO: this test tag as well as Boxes inside testa are temporarty, remove then b/157687898
    //  is fixed
    private val wrapperTestTag = "switchWrapper"

    private val wrapperModifier = Modifier
        .wrapContentSize(Alignment.TopStart)
        .testTag(wrapperTestTag)

    @Test
    fun switchTest_checked() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(checked = true, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_checked")
    }

    @Test
    fun switchTest_checked_rtl() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Switch(checked = true, onCheckedChange = { })
                }
            }
        }
        assertToggeableAgainstGolden("switch_checked_rtl")
    }

    @Test
    fun switchTest_checked_customColor() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Red)
                )
            }
        }
        assertToggeableAgainstGolden("switch_checked_customColor")
    }

    @Test
    fun switchTest_unchecked() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(checked = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_unchecked")
    }

    @Test
    fun switchTest_unchecked_rtl() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Switch(checked = false, onCheckedChange = { })
                }
            }
        }
        assertToggeableAgainstGolden("switch_unchecked_rtl")
    }

    @Test
    fun switchTest_bigSizeSpecified() {
        rule.setMaterialContent {
            Box(wrapperModifier.requiredSize(50.dp)) {
                Switch(checked = true, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_bigger_size")
    }

    @Test
    fun switchTest_pressed() {
        rule.mainClock.autoAdvance = false

        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(checked = false, enabled = true, onCheckedChange = { })
            }
        }

        rule.onNode(isToggleable()).performTouchInput {
            down(center)
        }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(100)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertToggeableAgainstGolden("switch_pressed")
    }

    @Test
    fun switchTest_disabled_checked() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(checked = true, enabled = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_disabled_checked")
    }

    @Test
    fun switchTest_disabled_unchecked() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(checked = false, enabled = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_disabled_unchecked")
    }

    @Test
    fun switchTest_unchecked_animateToChecked() {
        rule.setMaterialContent {
            val isChecked = remember { mutableStateOf(false) }
            Box(wrapperModifier) {
                Switch(
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

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(milliseconds = 96)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertToggeableAgainstGolden("switch_animateToChecked")
    }

    @Test
    fun switchTest_checked_animateToUnchecked() {
        rule.setMaterialContent {
            val isChecked = remember { mutableStateOf(true) }
            Box(wrapperModifier) {
                Switch(
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

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(milliseconds = 96)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertToggeableAgainstGolden("switch_animateToUnchecked")
    }

    @Test
    fun switchTest_hover() {
        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { }
                )
            }
        }

        rule.onNode(isToggleable())
            .performMouseInput { enter(center) }

        rule.waitForIdle()

        assertToggeableAgainstGolden("switch_hover")
    }

    @Test
    fun switchTest_focus() {
        val focusRequester = FocusRequester()

        rule.setMaterialContent {
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
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

        rule.waitForIdle()

        assertToggeableAgainstGolden("switch_focus")
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}