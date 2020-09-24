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
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.samples.IconButtonSample
import androidx.compose.material.samples.IconToggleButtonSample
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.isToggleable
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

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

    @Test
    fun iconButton_materialIconSize_iconPositioning() {
        val diameter = 24.dp
        rule.setMaterialContent {
            Box {
                IconButton(onClick = {}) {
                    Box(Modifier.preferredSize(diameter).testTag("icon"))
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
                    Box(Modifier.preferredSize(width, height).testTag("icon"))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((48.dp - width) / 2)
            .assertTopPositionInRootIsEqualTo((48.dp - height) / 2)
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

    @Test
    fun iconToggleButton_materialIconSize_iconPositioning() {
        val diameter = 24.dp
        rule.setMaterialContent {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.preferredSize(diameter).testTag("icon"))
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
                    Box(Modifier.preferredSize(width, height).testTag("icon"))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((48.dp - width) / 2)
            .assertTopPositionInRootIsEqualTo((48.dp - height) / 2)
    }

    @Test
    fun iconToggleButton_semantics() {
        rule.setMaterialContent {
            IconToggleButtonSample()
        }
        rule.onNode(isToggleable()).apply {
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }
}
