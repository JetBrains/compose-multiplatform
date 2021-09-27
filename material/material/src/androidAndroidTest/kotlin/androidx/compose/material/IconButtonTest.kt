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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.samples.IconButtonSample
import androidx.compose.material.samples.IconToggleButtonSample
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * Test for [IconButton] and [IconToggleButton].
 */
class IconButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun iconButton_size() {
        val width = 48.dp
        val height = 48.dp
        rule
            .setMaterialContentForSizeAssertions {
                IconButtonSample()
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun iconButton_size_withoutMinimumTouchTarget() {
        val width = 24.dp
        val height = 24.dp
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    IconButtonSample()
                }
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun iconButton_defaultSemantics() {
        rule.setMaterialContent {
            IconButtonSample()
        }
        rule.onNode(hasClickAction()).apply {
            assertIsEnabled()
        }
    }

    @Test
    fun iconButton_disabledSemantics() {
        rule.setMaterialContent {
            IconButton(onClick = {}, enabled = false) {}
        }
        rule.onNode(hasClickAction()).apply {
            assertIsNotEnabled()
        }
    }

    @Test
    fun iconButton_materialIconSize_iconPositioning() {
        val diameter = 24.dp
        rule.setMaterialContent {
            Box {
                IconButton(onClick = {}) {
                    Box(Modifier.size(diameter).testTag("icon"))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(24.dp / 2)
            .assertTopPositionInRootIsEqualTo(24.dp / 2)
    }

    @Test
    fun iconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent {
            Box {
                IconButton(onClick = {}) {
                    Box(Modifier.size(width, height).testTag("icon"))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((48.dp - width) / 2)
            .assertTopPositionInRootIsEqualTo((48.dp - height) / 2)
    }

    @Test
    fun iconButton_contentAlpha() {
        var enabled by mutableStateOf(true)
        var contentAlpha: Float? = null
        var highContentAlpha: Float? = null
        var disabledContentAlpha: Float? = null
        rule.setMaterialContent {
            IconButton(onClick = {}, enabled = enabled) {
                contentAlpha = LocalContentAlpha.current
                highContentAlpha = ContentAlpha.high
                disabledContentAlpha = ContentAlpha.disabled
            }
        }

        rule.runOnIdle {
            Truth.assertThat(contentAlpha!!).isEqualTo(highContentAlpha!!)
        }

        rule.runOnIdle {
            enabled = false
        }

        rule.runOnIdle {
            Truth.assertThat(contentAlpha!!).isEqualTo(disabledContentAlpha!!)
        }
    }

    @Test
    fun iconToggleButton_size() {
        val width = 48.dp
        val height = 48.dp
        rule
            .setMaterialContentForSizeAssertions {
                IconToggleButtonSample()
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun iconToggleButton_size_withoutMinimumTouchTarget() {
        val width = 24.dp
        val height = 24.dp
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    IconToggleButtonSample()
                }
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun iconToggleButton_defaultSemantics() {
        rule.setMaterialContent {
            IconToggleButtonSample()
        }
        rule.onNode(isToggleable()).apply {
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun iconToggleButton_disabledSemantics() {
        rule.setMaterialContent {
            IconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun iconToggleButton_materialIconSize_iconPositioning() {
        val diameter = 24.dp
        rule.setMaterialContent {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(diameter).testTag("icon"))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(24.dp / 2)
            .assertTopPositionInRootIsEqualTo(24.dp / 2)
    }

    @Test
    fun iconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag("icon"))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((48.dp - width) / 2)
            .assertTopPositionInRootIsEqualTo((48.dp - height) / 2)
    }

    @Test
    fun iconToggleButton_contentAlpha() {
        var enabled by mutableStateOf(true)
        var contentAlpha: Float? = null
        var highContentAlpha: Float? = null
        var disabledContentAlpha: Float? = null
        rule.setMaterialContent {
            IconToggleButton(checked = false, onCheckedChange = {}, enabled = enabled) {
                contentAlpha = LocalContentAlpha.current
                highContentAlpha = ContentAlpha.high
                disabledContentAlpha = ContentAlpha.disabled
            }
        }

        rule.runOnIdle {
            Truth.assertThat(contentAlpha!!).isEqualTo(highContentAlpha!!)
        }

        rule.runOnIdle {
            enabled = false
        }

        rule.runOnIdle {
            Truth.assertThat(contentAlpha!!).isEqualTo(disabledContentAlpha!!)
        }
    }

    @Test
    fun iconToggleButton_clickInMinimumTouchTarget(): Unit = with(rule.density) {
        val tag = "iconToggleButton"
        var checked by mutableStateOf(false)
        rule.setMaterialContent {
            // Box is needed because otherwise the control will be expanded to fill its parent
            Box(Modifier.fillMaxSize()) {
                IconToggleButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.align(Alignment.Center).requiredSize(2.dp).testTag(tag)
                ) {
                    Box(Modifier.size(2.dp))
                }
            }
        }
        rule.onNodeWithTag(tag)
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
