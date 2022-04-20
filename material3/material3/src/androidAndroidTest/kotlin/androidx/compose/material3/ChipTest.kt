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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.tokens.AssistChipTokens
import androidx.compose.material3.tokens.FilterChipTokens
import androidx.compose.material3.tokens.InputChipTokens
import androidx.compose.material3.tokens.SuggestionChipTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ChipTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultSemantics_assistChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                AssistChip(
                    onClick = {},
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text(TestChipTag) })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun disabledSemantics_assistChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                AssistChip(
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = {},
                    label = { Text(TestChipTag) },
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun onClick_assistChip() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                AssistChip(
                    onClick = onClick,
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text("Test chip") })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun heightIsFromSpec_assistChip() {
        // This test can be reasonable failing on the non default font scales
        // so lets skip it.
        Assume.assumeTrue(rule.density.fontScale <= 1f)
        rule.setMaterialContent(lightColorScheme()) {
            AssistChip(onClick = {}, label = { Text("Test chip") })
        }

        rule.onNode(hasClickAction())
            .assertHeightIsEqualTo(AssistChipDefaults.Height)
    }

    @Test
    fun horizontalPadding_assistChip() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            AssistChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Assist chip",
                        Modifier.testTag(TestChipTag)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(16.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - 16.dp)
    }

    @Test
    fun horizontalPadding_assistChip_withLeadingIcon() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            AssistChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Assist chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp + AssistChipDefaults.IconSize + 8.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - AssistChipDefaults.IconSize - 16.dp)
    }

    @Test
    fun labelContentColor_assistChip() {
        var expectedLabelColor = Color.Unspecified
        var contentColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            expectedLabelColor = AssistChipTokens.LabelTextColor.toColor()
            AssistChip(onClick = {}, label = {
                contentColor = LocalContentColor.current
            })
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(expectedLabelColor)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun elevatedDisabled_assistChip() {
        var containerColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            containerColor = AssistChipTokens.ElevatedDisabledContainerColor.toColor()
                .copy(alpha = AssistChipTokens.ElevatedDisabledContainerOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface)
            ElevatedAssistChip(
                modifier = Modifier.testTag(TestChipTag),
                onClick = {},
                label = {},
                enabled = false,
                shape = RectangleShape
            )
        }

        rule.onNodeWithTag(TestChipTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                backgroundColor = containerColor,
                shapeColor = containerColor
            )
    }

    @Test
    fun unselectedSemantics_filterChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilterChip(
                    selected = false,
                    onClick = {},
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text(TestChipTag) })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
            .assertHasClickAction()
            .assertIsNotSelected()
    }

    @Test
    fun selectedSemantics_filterChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilterChip(
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text(TestChipTag) })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
            .assertHasClickAction()
            .assertIsSelected()
    }

    @Test
    fun disabledSemantics_filterChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilterChip(
                    selected = false,
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = {},
                    label = { Text(TestChipTag) },
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsNotEnabled()
            .assertHasClickAction()
            .assertIsNotSelected()
    }

    @Test
    fun toggle_filterChip() {
        rule.setMaterialContent(lightColorScheme()) {
            val selected = remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = selected.value,
                    onClick = { selected.value = !selected.value },
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text("Test chip") })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun horizontalPadding_unselected_filterChip() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = false,
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Filter chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }
        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(16.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - 16.dp)
    }

    @Test
    fun horizontalPadding_selected_filterChip() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = true,
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Filter chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }
        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp + FilterChipDefaults.IconSize + 8.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - FilterChipDefaults.IconSize - 16.dp)
    }

    @Test
    fun horizontalPadding_filterChip_withIcons() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(
                selected = false,
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Filter chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }
        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp + FilterChipDefaults.IconSize + 8.dp)
            .assertWidthIsEqualTo(
                chipWidth - 16.dp - FilterChipDefaults.IconSize -
                    FilterChipDefaults.IconSize - 16.dp
            )
    }

    @Test
    fun heightIsFromSpec_filterChip() {
        // This test can be reasonable failing on the non default font scales
        // so lets skip it.
        Assume.assumeTrue(rule.density.fontScale <= 1f)
        rule.setMaterialContent(lightColorScheme()) {
            FilterChip(selected = false, onClick = {}, label = { Text("Test chip") })
        }

        rule.onNode(hasClickAction())
            .assertHeightIsEqualTo(FilterChipDefaults.Height)
    }

    @Test
    fun labelContentColor_unselectedFilterChip() {
        var expectedLabelColor = Color.Unspecified
        var contentColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            expectedLabelColor = FilterChipTokens.UnselectedLabelTextColor.toColor()
            FilterChip(selected = false, onClick = {}, label = {
                contentColor = LocalContentColor.current
            })
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(expectedLabelColor)
        }
    }

    @Test
    fun labelContentColor_selectedFilterChip() {
        var expectedLabelColor = Color.Unspecified
        var contentColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            expectedLabelColor = FilterChipTokens.SelectedLabelTextColor.toColor()
            FilterChip(selected = true, onClick = {}, label = {
                contentColor = LocalContentColor.current
            })
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(expectedLabelColor)
        }
    }

    @Test
    fun defaultSemantics_inputChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                InputChip(
                    onClick = {},
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text(TestChipTag) })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun disabledSemantics_inputChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                InputChip(
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = {},
                    label = { Text(TestChipTag) },
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun onClick_inputChip() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                InputChip(
                    onClick = onClick,
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text("Test chip") })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun heightIsFromSpec_inputChip() {
        // This test can be reasonable failing on the non default font scales
        // so lets skip it.
        Assume.assumeTrue(rule.density.fontScale <= 1f)
        rule.setMaterialContent(lightColorScheme()) {
            InputChip(onClick = {}, label = { Text("Test chip") })
        }

        rule.onNode(hasClickAction())
            .assertHeightIsEqualTo(InputChipDefaults.Height)
    }

    @Test
    fun horizontalPadding_inputChip() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            InputChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Input chip",
                        Modifier.testTag(TestChipTag)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(12.dp)
            .assertWidthIsEqualTo(chipWidth - 12.dp - 12.dp)
    }

    @Test
    fun horizontalPadding_inputChip_withLeadingIcon() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            InputChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Input chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        // Note that InputChip has slightly different padding than the other Chips.
        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp + InputChipDefaults.IconSize + 8.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - InputChipDefaults.IconSize - 12.dp)
    }

    @Test
    fun horizontalPadding_inputChip_withAvatar() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            InputChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Input chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                avatar = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(InputChipDefaults.AvatarSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        // Note that InputChip has slightly different padding than the other Chips.
        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(4.dp + InputChipDefaults.AvatarSize + 8.dp)
            .assertWidthIsEqualTo(chipWidth - 12.dp - InputChipDefaults.AvatarSize - 12.dp)
    }

    @Test
    fun labelContentColor_inputChip() {
        var expectedLabelColor = Color.Unspecified
        var contentColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            expectedLabelColor = InputChipTokens.LabelTextColor.toColor()
            InputChip(onClick = {}, label = {
                contentColor = LocalContentColor.current
            })
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(expectedLabelColor)
        }
    }

    @Test
    fun defaultSemantics_suggestionChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                SuggestionChip(
                    onClick = {},
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text(TestChipTag) })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun disabledSemantics_suggestionChip() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = {},
                    label = { Text(TestChipTag) },
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun onClick_suggestionChip() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                SuggestionChip(
                    onClick = onClick,
                    modifier = Modifier.testTag(TestChipTag),
                    label = { Text("Test chip") })
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun heightIsFromSpec_suggestionChip() {
        // This test can be reasonable failing on the non default font scales
        // so lets skip it.
        Assume.assumeTrue(rule.density.fontScale <= 1f)
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(onClick = {}, label = { Text("Test chip") })
        }

        rule.onNode(hasClickAction())
            .assertHeightIsEqualTo(SuggestionChipDefaults.Height)
    }

    @Test
    fun horizontalPadding_suggestionChip() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Suggestion chip",
                        Modifier.testTag(TestChipTag)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(16.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - 16.dp)
    }

    @Test
    fun horizontalPadding_suggestionChip_withLeadingIcon() {
        var chipCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned { chipCoordinates = it },
                label = {
                    Text(
                        "Suggestion chip",
                        Modifier.testTag(TestChipTag)
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(SuggestionChipDefaults.IconSize)
                    )
                })
        }

        var chipWidth = 0.dp
        rule.runOnIdle {
            chipWidth = with(rule.density) {
                chipCoordinates!!.boundsInWindow().width.toDp()
            }
        }

        rule.onNodeWithTag(TestChipTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(8.dp + SuggestionChipDefaults.IconSize + 8.dp)
            .assertWidthIsEqualTo(chipWidth - 16.dp - SuggestionChipDefaults.IconSize - 16.dp)
    }

    @Test
    fun labelContentColor_suggestionChip() {
        var expectedLabelColor = Color.Unspecified
        var contentColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            expectedLabelColor = SuggestionChipTokens.LabelTextColor.toColor()
            SuggestionChip(onClick = {}, label = {
                contentColor = LocalContentColor.current
            })
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(expectedLabelColor)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun elevatedDisabled_suggestionChip() {
        var containerColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            containerColor = SuggestionChipTokens.ElevatedDisabledContainerColor.toColor()
                .copy(alpha = SuggestionChipTokens.ElevatedDisabledContainerOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface)
            ElevatedSuggestionChip(
                modifier = Modifier.testTag(TestChipTag),
                onClick = {},
                label = {},
                enabled = false,
                shape = RectangleShape
            )
        }

        rule.onNodeWithTag(TestChipTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                backgroundColor = containerColor,
                shapeColor = containerColor
            )
    }

    @Test
    fun canBeDisabled() {
        rule.setMaterialContent(lightColorScheme()) {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = onClick,
                    label = { Text("Hello") },
                    enabled = enabled
                )
            }
        }
        rule.onNodeWithTag(TestChipTag)
            // Confirm the chip starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun withLargeFontSizeIsLargerThenHeight() {
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(onClick = {}, label = {
                Text(
                    text = "Test chip",
                    fontSize = 50.sp
                )
            })
        }

        rule.onNode(hasClickAction())
            .assertHeightIsAtLeast(SuggestionChipDefaults.Height + 1.dp)
    }

    @Test
    fun propagateDefaultTextStyle() {
        var textStyle: TextStyle? = null
        var body2TextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(onClick = {}, label = {
                textStyle = LocalTextStyle.current
                body2TextStyle =
                    MaterialTheme.typography.fromToken(SuggestionChipTokens.LabelTextFont)
            })
        }

        rule.runOnIdle { assertThat(textStyle).isEqualTo(body2TextStyle) }
    }

    @Test
    fun contentIsRow() {
        var chipBounds = Rect(0f, 0f, 0f, 0f)
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var item2Bounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent(lightColorScheme()) {
            SuggestionChip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned {
                    chipBounds = it.boundsInRoot()
                },
                label = {
                    Spacer(
                        Modifier.requiredSize(10.dp).onGloballyPositioned {
                            item1Bounds = it.boundsInRoot()
                        }
                    )
                    Spacer(
                        Modifier.requiredWidth(10.dp).requiredHeight(5.dp)
                            .onGloballyPositioned {
                                item2Bounds = it.boundsInRoot()
                            }
                    )
                }
            )
        }

        assertThat(item1Bounds.center.y).isWithin(1f).of(chipBounds.center.y)
        assertThat(item2Bounds.center.y).isWithin(1f).of(chipBounds.center.y)
        assertThat(item1Bounds.right).isWithin(1f).of(chipBounds.center.x)
        assertThat(item2Bounds.left).isWithin(1f).of(chipBounds.center.x)
    }

    @Test
    fun clickableInMinimumTouchTarget() {
        var clicked = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                SuggestionChip(
                    modifier = Modifier.align(Alignment.Center).testTag(TestChipTag)
                        .requiredSize(10.dp),
                    onClick = { clicked = !clicked },
                    label = { Box(Modifier.size(10.dp)) }
                )
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performTouchInput {
                click(Offset(-1f, -1f))
            }

        assertThat(clicked).isTrue()
    }
}

private const val TestChipTag = "chip"
