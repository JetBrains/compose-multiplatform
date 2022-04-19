/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
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
@OptIn(ExperimentalTestApi::class)
class SwitchScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)
    // TODO: this test tag as well as Boxes inside testa are temporarty, remove then b/157687898
    //  is fixed
    private val wrapperTestTag = "switchWrapper"

    private val wrapperModifier = Modifier
        .wrapContentSize(Alignment.TopStart)
        .testTag(wrapperTestTag)

    @Test
    fun switchTest_checked() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrapperModifier) {
                Switch(checked = true, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_checked")
    }

    @Test
    fun switchTest_checked_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrapperModifier) {
                Switch(checked = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_unchecked")
    }

    @Test
    fun switchTest_unchecked_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrapperModifier) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Switch(checked = false, onCheckedChange = { })
                }
            }
        }
        assertToggeableAgainstGolden("switch_unchecked_rtl")
    }

    @Test
    fun switchTest_pressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrapperModifier) {
                Switch(checked = false, enabled = true, onCheckedChange = { })
            }
        }

        rule.onNode(isToggleable()).performTouchInput {
            down(center)
        }

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertToggeableAgainstGolden("switch_pressed")
    }

    @Test
    fun switchTest_disabled_checked() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrapperModifier) {
                Switch(checked = true, enabled = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_disabled_checked")
    }

    @Test
    fun switchTest_disabled_unchecked() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrapperModifier) {
                Switch(checked = false, enabled = false, onCheckedChange = { })
            }
        }
        assertToggeableAgainstGolden("switch_disabled_unchecked")
    }

    @Test
    fun switchTest_unchecked_animateToChecked() {
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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

    @Test
    fun switchTest_checked_icon() {
        rule.setMaterialContent(lightColorScheme()) {
            val icon: @Composable () -> Unit = {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    thumbContent = icon
                )
            }
        }

        assertToggeableAgainstGolden("switch_checked_icon")
    }

    @Test
    fun switchTest_unchecked_icon() {
        rule.setMaterialContent(lightColorScheme()) {
            val icon: @Composable () -> Unit = {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
            Box(wrapperModifier) {
                Switch(
                    checked = false,
                    onCheckedChange = { },
                    thumbContent = icon
                )
            }
        }

        assertToggeableAgainstGolden("switch_unchecked_icon")
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}