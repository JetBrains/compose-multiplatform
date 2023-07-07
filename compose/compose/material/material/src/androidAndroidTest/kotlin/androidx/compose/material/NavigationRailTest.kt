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

package androidx.compose.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.samples.CompactNavigationRailSample
import androidx.compose.material.samples.NavigationRailSample
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * Test for [NavigationRail] and [NavigationRailItem].
 */
class NavigationRailTest {
    @get:Rule
    val rule = createComposeRule()

    private val NavigationRailPadding = 8.dp

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent {
            NavigationRail {
                NavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null)
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()

        rule.onNodeWithTag("item")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent {
            NavigationRail {
                NavigationRailItem(
                    enabled = false,
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null)
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun navigationRail_width() {
        val defaultWidth = 72.dp
        rule.setMaterialContentForSizeAssertions {
            NavigationRailSample()
        }.assertWidthIsEqualTo(defaultWidth)
    }

    @Test
    fun navigationRail_compactWidth() {
        val compactWidth = 56.dp
        rule.setMaterialContentForSizeAssertions {
            CompactNavigationRailSample()
        }.assertWidthIsEqualTo(compactWidth)
    }

    @Test
    fun navigationRailItem_sizeAndPositions() {
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent {
            Box {
                NavigationRail {
                    repeat(4) { index ->
                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned { coords ->
                                itemCoords[index] = coords
                            }
                        )
                    }
                }
            }
        }

        assertDimension(itemCoords, 72.dp, 72.dp)
    }

    @Test
    fun navigationRailItem_compactSizeAndPositions() {
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent {
            Box {
                NavigationRail {
                    repeat(4) { index ->
                        // No label will create a compact size item
                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            selected = index == 0,
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned { coords ->
                                itemCoords[index] = coords
                            }
                        )
                    }
                }
            }
        }

        assertDimension(itemCoords, 56.dp, 56.dp)
    }

    @Test
    fun navigationRailItem_customSizeAndPositions() {
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent {
            Box {
                NavigationRail {
                    repeat(4) { index ->
                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier = Modifier.width(96.dp)
                                .onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        assertDimension(itemCoords, 96.dp, 72.dp)
    }

    private fun assertDimension(
        itemCoords: MutableMap<Int, LayoutCoordinates>,
        expectedItemWidth: Dp,
        expectedItemHeight: Dp
    ) {
        rule.runOnIdleWithDensity {
            val expectedItemWidthPx = expectedItemWidth.roundToPx()
            val expectedItemHeightPx = expectedItemHeight.roundToPx()
            val navigationRailPadding = NavigationRailPadding.roundToPx()

            Truth.assertThat(itemCoords.size).isEqualTo(4)

            itemCoords.forEach { (index, coord) ->
                Truth.assertThat(coord.size.width).isEqualTo(expectedItemWidthPx)
                Truth.assertThat(coord.size.height).isEqualTo(expectedItemHeightPx)
                Truth.assertThat(coord.positionInWindow().y)
                    .isEqualTo((expectedItemHeightPx * index + navigationRailPadding).toFloat())
            }
        }
    }

    @Test
    fun navigationRailItemContent_withLabel_sizeAndPosition() {
        rule.setMaterialContent {
            Box {
                NavigationRail {
                    NavigationRailItem(
                        modifier = Modifier.testTag("item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                        },
                        label = {
                            Text("ItemText")
                        },
                        selected = true,
                        onClick = {}
                    )
                }
            }
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithText("ItemText").getUnclippedBoundsInRoot()

        // The space between the text label's baseline and the bottom of the container.
        val textBaseline = 16.dp

        // Relative position of the baseline to the top of text
        val relativeTextBaseline = rule.onNodeWithText("ItemText").getLastBaselinePosition()
        // Absolute y position of the text baseline
        val absoluteTextBaseline = textBounds.top + relativeTextBaseline

        val itemBottom = itemBounds.height + itemBounds.top
        // Text baseline should be 16.dp from the bottom of the item
        absoluteTextBaseline.assertIsEqualTo(itemBottom - textBaseline)

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be centered in the item
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The top of the icon is 14.dp from the top of the item's container +
            // the NavigationRailPadding.
            .assertTopPositionInRootIsEqualTo(14.dp + NavigationRailPadding)
    }

    @Test
    fun navigationRailItemContent_withLabel_unselected_sizeAndPosition() {
        rule.setMaterialContent {
            Box {
                NavigationRail {
                    NavigationRailItem(
                        modifier = Modifier.testTag("item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                        },
                        label = {
                            Text("ItemText")
                        },
                        selected = false,
                        onClick = {},
                        alwaysShowLabel = false
                    )
                }
            }
        }

        // The text should not be placed, since the item is not selected and alwaysShowLabels
        // is false
        rule.onNodeWithText("ItemText", useUnmergedTree = true).assertIsNotDisplayed()

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The item's container has an [NavigationRailPadding] top-padding (8dp)
            .assertTopPositionInRootIsEqualTo(
                NavigationRailPadding +
                    (itemBounds.height - iconBounds.height) / 2
            )
    }

    @Test
    fun navigationRailItemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent {
            Box {
                NavigationRail {
                    NavigationRailItem(
                        modifier = Modifier.testTag("item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                        },
                        label = null,
                        selected = false,
                        onClick = {}
                    )
                }
            }
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // The icon should be centered in the item, as there is no text placeable provided
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The item's container has an [NavigationRailPadding] top-padding (8dp)
            .assertTopPositionInRootIsEqualTo(
                NavigationRailPadding +
                    (itemBounds.height - iconBounds.height) / 2
            )
    }

    @Test
    fun navigationRail_selectNewItem() {
        rule.setMaterialContent {
            NavigationRailSample()
        }

        // Find all items and ensure there are 3
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            // Ensure semantics match for selected state of the items
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }
            // Click the last item
            .apply {
                get(2).performClick()
            }
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent {
            NavigationRail {
                NavigationRailItem(
                    enabled = false,
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null)
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = { clicks++ }
                )
            }
        }

        rule.onNodeWithTag("item")
            .performClick()

        rule.runOnIdle {
            Truth.assertThat(clicks).isEqualTo(0)
        }
    }
}