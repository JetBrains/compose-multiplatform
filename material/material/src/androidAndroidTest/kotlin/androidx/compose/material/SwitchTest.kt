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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            .assertIsOn()
        rule.onNodeWithTag("unchecked")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            .assertIsOff()
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
            .assertHasClickAction()
    }

    @Test
    fun switch_untoggleable_whenEmptyLambda() {
        val parentTag = "parent"

        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(parentTag)) {
                Switch(
                    checked,
                    {},
                    enabled = false,
                    modifier = Modifier.testTag(defaultSwitchTag).semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .assertHasClickAction()

        // Check not merged into parent
        rule.onNodeWithTag(parentTag)
            .assert(isNotFocusable())
    }

    @Test
    fun switch_untoggleableAndMergeable_whenNullLambda() {
        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(defaultSwitchTag)) {
                Switch(
                    checked,
                    null,
                    modifier = Modifier.semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .assertHasNoClickAction()
            .assert(isFocusable()) // Check merged into parent
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
