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

import androidx.compose.foundation.Strings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.assertValueEquals
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import androidx.ui.test.performGesture
import androidx.ui.test.swipeLeft
import androidx.ui.test.swipeRight
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class SwitchTest {

    @get:Rule
    val rule = createComposeRule()

    private val defaultSwitchTag = "switch"

    @Test
    fun switch_defaultSemantics() {
        rule.setMaterialContent {
            Column {
                Switch(modifier = Modifier.testTag("checked"), checked = true, onCheckedChange = {})
                Switch(
                    modifier = Modifier.testTag("unchecked"),
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        rule.onNodeWithTag("checked")
            .assertIsEnabled()
            .assertIsOn()
            .assertValueEquals(Strings.Checked)
        rule.onNodeWithTag("unchecked")
            .assertIsEnabled()
            .assertIsOff()
            .assertValueEquals(Strings.Unchecked)
    }

    @Test
    fun switch_toggle() {
        rule.setMaterialContent {
            val (checked, onChecked) = remember { mutableStateOf(false) }

            // Box is needed because otherwise the control will be expanded to fill its parent
            Box {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun switch_toggleTwice() {
        rule.setMaterialContent {
            val (checked, onChecked) = remember { mutableStateOf(false) }

            // Box is needed because otherwise the control will be expanded to fill its parent
            Box {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertIsOff()
            .performClick()
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun switch_uncheckableWithNoLambda() {
        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Switch(
                modifier = Modifier.testTag(defaultSwitchTag),
                checked = checked,
                onCheckedChange = {},
                enabled = false
            )
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertHasNoClickAction()
    }

    @Test
    fun switch_materialSizes_whenChecked() {
        materialSizesTestForValue(true)
    }

    @Test
    fun switch_materialSizes_whenUnchecked() {
        materialSizesTestForValue(false)
    }

    @Test
    fun switch_testDraggable() {
        val state = mutableStateOf(false)
        rule.setMaterialContent {

            // Box is needed because otherwise the control will be expanded to fill its parent
            Box {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = state.value,
                    onCheckedChange = { state.value = it }
                )
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeRight() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(true)
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeLeft() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(false)
        }
    }

    @Test
    fun switch_testDraggable_rtl() {
        val state = mutableStateOf(false)
        rule.setMaterialContent {

            // Box is needed because otherwise the control will be expanded to fill its parent
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Box {
                    Switch(
                        modifier = Modifier.testTag(defaultSwitchTag),
                        checked = state.value,
                        onCheckedChange = { state.value = it }
                    )
                }
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeLeft() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(true)
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeRight() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(false)
        }
    }

    private fun materialSizesTestForValue(checked: Boolean) {
        rule.setMaterialContentForSizeAssertions {
            Switch(checked = checked, onCheckedChange = {}, enabled = false)
        }
            .assertWidthIsEqualTo(34.dp + 2.dp * 2)
            .assertHeightIsEqualTo(20.dp + 2.dp * 2)
    }
}
