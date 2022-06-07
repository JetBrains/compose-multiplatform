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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.tokens.SwitchTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
        rule.setMaterialContent(lightColorScheme()) {
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
    fun switch_materialSizes_whenChecked_minimumTouchTarget() {
        materialSizesTestForValue(
            checked = true,
            clickable = true,
            minimumTouchTarget = true
        )
    }

    @Test
    fun switch_materialSizes_whenChecked_withoutMinimumTouchTarget() {
        materialSizesTestForValue(
            checked = true,
            clickable = true,
            minimumTouchTarget = false
        )
    }

    @Test
    fun switch_materialSizes_whenUnchecked_minimumTouchTarget() {
        materialSizesTestForValue(
            checked = false,
            clickable = true,
            minimumTouchTarget = true
        )
    }

    @Test
    fun switch_materialSizes_whenUnchecked_withoutMinimumTouchTarget() {
        materialSizesTestForValue(
            checked = false,
            clickable = true,
            minimumTouchTarget = false
        )
    }

    @Test
    fun switch_materialSizes_whenChecked_notClickable_minimumTouchTarget() {
        materialSizesTestForValue(
            checked = true,
            clickable = false,
            minimumTouchTarget = true
        )
    }

    @Test
    fun switch_materialSizes_whenChecked_notClickable_withoutMinimumTouchTarget() {
        materialSizesTestForValue(
            checked = true,
            clickable = false,
            minimumTouchTarget = false
        )
    }

    @Test
    fun switch_materialSizes_whenUnchecked_notClickable_minimumTouchTarget() {
        materialSizesTestForValue(
            checked = false,
            clickable = false,
            minimumTouchTarget = true
        )
    }

    @Test
    fun switch_materialSizes_whenUnchecked_notClickable_withoutMinimumTouchTarget() {
        materialSizesTestForValue(
            checked = false,
            clickable = false,
            minimumTouchTarget = false
        )
    }

    @Test
    fun switch_stateChange_movesThumb() {
        var checked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            val spacer = @Composable { Spacer(Modifier.size(16.dp).testTag("spacer")) }
            Switch(
                modifier = Modifier.testTag(defaultSwitchTag),
                checked = checked,
                thumbContent = spacer,
                onCheckedChange = { checked = it },
            )
        }

        rule.onNodeWithTag("spacer", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp)

        rule.runOnIdle { checked = true }

        rule.onNodeWithTag("spacer", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(28.dp)

        rule.runOnIdle { checked = false }

        rule.onNodeWithTag("spacer", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp)
    }

    // regression test for b/191375128
    @Test
    fun switch_stateRestoration_stateChangeWhileSaved() {
        val screenTwo = mutableStateOf(false)
        var items by mutableStateOf(listOf(1 to false, 2 to true))
        rule.setContent {
            Column {
                Button(onClick = { screenTwo.value = !screenTwo.value }) {
                    Text("switch screen")
                }
                val holder = rememberSaveableStateHolder()
                holder.SaveableStateProvider(screenTwo.value) {
                    if (screenTwo.value) {
                        // second screen, just some random content
                        Text("Second screen")
                    } else {
                        Column {
                            Text("screen one")
                            items.forEachIndexed { index, item ->
                                Row {
                                    Text("Item ${item.first}")
                                    Switch(
                                        modifier = Modifier.testTag(item.first.toString()),
                                        checked = item.second,
                                        onCheckedChange = {
                                            items = items.toMutableList().also {
                                                it[index] = item.first to !item.second
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("1").assertIsOff()
        rule.onNodeWithTag("2").assertIsOn()
        rule.runOnIdle {
            screenTwo.value = true
        }
        rule.runOnIdle {
            items = items.toMutableList().also {
                it[0] = items[0].first to !items[0].second
                it[1] = items[1].first to !items[1].second
            }
        }
        rule.runOnIdle {
            screenTwo.value = false
        }
        rule.onNodeWithTag("1").assertIsOn()
        rule.onNodeWithTag("2").assertIsOff()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun materialSizesTestForValue(
        checked: Boolean,
        clickable: Boolean,
        minimumTouchTarget: Boolean
    ) = with(rule.density) {
        rule.setMaterialContentForSizeAssertions {
            CompositionLocalProvider(
                LocalMinimumTouchTargetEnforcement provides minimumTouchTarget
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = if (clickable) { {} } else null,
                    enabled = false
                )
            }
        }.run {
            if (clickable && minimumTouchTarget) {
                assertWidthIsAtLeast(48.dp)
                assertHeightIsAtLeast(48.dp)
            } else {
                assertWidthIsEqualTo(SwitchTokens.TrackWidth)
                assertHeightIsEqualTo(SwitchTokens.TrackHeight)
            }
        }
    }

    /**
     * A switch should have a minimum touch target of 48 DP x 48 DP and the reported size
     * should match that, despite the fact that we force the size to be smaller.
     */
    @Test
    fun switch_minTouchTargetArea(): Unit = with(rule.density) {
        var checked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            // Box is needed because otherwise the control will be expanded to fill its parent
            Box(Modifier.fillMaxSize()) {
                Switch(
                    modifier = Modifier.align(Alignment.Center)
                        .testTag(defaultSwitchTag)
                        .requiredSize(2.dp),
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
            }
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertIsOff()
            .assertWidthIsEqualTo(2.dp)
            .assertHeightIsEqualTo(2.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performTouchInput {
                click(position = Offset(-1f, -1f))
            }.assertIsOn()
    }
}
