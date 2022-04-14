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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * Tests for icon buttons.
 */
class IconButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun iconButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun iconButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun iconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun iconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            IconButton(onClick = {}, enabled = false) {}
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun iconButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconButton(onClick = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun iconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconButton(onClick = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun iconToggleButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                IconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun iconToggleButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    IconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun iconToggleButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun iconToggleButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            IconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun iconToggleButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconToggleButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun iconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconToggleButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun iconToggleButton_clickInMinimumTouchTarget(): Unit = with(rule.density) {
        val tag = "iconToggleButton"
        var checked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
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

    @Test
    fun filledIconButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                FilledIconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun filledIconButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    FilledIconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun filledIconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledIconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun filledIconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledIconButton(onClick = {}, enabled = false) {}
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun filledIconButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconButton(onClick = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun filledIconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconButton(onClick = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the FilledIconButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun filledIconToggleButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                FilledIconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun filledIconToggleButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    FilledIconToggleButton(
                        checked = true,
                        onCheckedChange = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun filledIconToggleButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            FilledIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun filledIconToggleButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledIconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun filledIconToggleButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the FilledIconToggleButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun filledIconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the FilledIconToggleButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun outlinedIconButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedIconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun outlinedIconButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    OutlinedIconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun outlinedIconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedIconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun outlinedIconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedIconButton(onClick = {}, enabled = false) {}
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun outlinedIconButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconButton(onClick = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun outlinedIconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconButton(onClick = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the OutlinedIconButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun outlinedIconToggleButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedIconToggleButton(
                    checked = true,
                    onCheckedChange = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description"
                    )
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun outlinedIconToggleButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    OutlinedIconToggleButton(
                        checked = true,
                        onCheckedChange = { /* doSomething() */ }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun outlinedIconToggleButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            OutlinedIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Localized description")
            }
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun outlinedIconToggleButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedIconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun outlinedIconToggleButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the OutlinedIconToggleButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun outlinedIconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the OutlinedIconToggleButton
        rule.onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    private val IconButtonAccessibilitySize = 48.0.dp
    private val IconButtonSize = 40.0.dp
    private val IconSize = 24.0.dp
    private val IconTestTag = "icon"
}
