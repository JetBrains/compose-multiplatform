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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.samples.BottomNavigationSample
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
import androidx.compose.ui.test.assertHeightIsEqualTo
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
 * Test for [BottomNavigation] and [BottomNavigationItem].
 */
class BottomNavigationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent {
            BottomNavigation {
                BottomNavigationItem(
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
            BottomNavigation {
                BottomNavigationItem(
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
    fun bottomNavigation_size() {
        val height = 56.dp
        rule.setMaterialContentForSizeAssertions {
            BottomNavigationSample()
        }
            .assertWidthIsEqualTo(rule.rootWidth())
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun bottomNavigationItem_sizeAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent(
            Modifier.onGloballyPositioned { coords: LayoutCoordinates ->
                parentCoords = coords
            }
        ) {
            Box {
                BottomNavigation {
                    repeat(4) { index ->
                        BottomNavigationItem(
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

        rule.runOnIdleWithDensity {
            val totalWidth = parentCoords.size.width

            val expectedItemWidth = totalWidth / 4
            val expectedItemHeight = 56.dp.roundToPx()

            Truth.assertThat(itemCoords.size).isEqualTo(4)

            itemCoords.forEach { (index, coord) ->
                Truth.assertThat(coord.size.width).isEqualTo(expectedItemWidth)
                Truth.assertThat(coord.size.height).isEqualTo(expectedItemHeight)
                Truth.assertThat(coord.positionInWindow().x)
                    .isEqualTo((expectedItemWidth * index).toFloat())
            }
        }
    }

    @Test
    fun bottomNavigationItemContent_withLabel_sizeAndPosition() {
        rule.setMaterialContent {
            Box {
                BottomNavigation {
                    BottomNavigationItem(
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

        // Distance from the bottom to the text baseline and from the text baseline to the
        // bottom of the icon
        val textBaseline = 12.dp

        // Relative position of the baseline to the top of text
        val relativeTextBaseline = rule.onNodeWithText("ItemText").getLastBaselinePosition()
        // Absolute y position of the text baseline
        val absoluteTextBaseline = textBounds.top + relativeTextBaseline

        val itemBottom = itemBounds.height + itemBounds.top
        // Text baseline should be 12.dp from the bottom of the item
        absoluteTextBaseline.assertIsEqualTo(itemBottom - textBaseline)

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be centered in the item
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The bottom of the icon is 12.dp above the text baseline
            .assertTopPositionInRootIsEqualTo(absoluteTextBaseline - 12.dp - iconBounds.height)
    }

    @Test
    fun bottomNavigationItemContent_withLabel_unselected_sizeAndPosition() {
        rule.setMaterialContent {
            Box {
                BottomNavigation {
                    BottomNavigationItem(
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
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun bottomNavigationItemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent {
            Box {
                BottomNavigation {
                    BottomNavigationItem(
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
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun bottomNavigation_selectNewItem() {
        rule.setMaterialContent {
            BottomNavigationSample()
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
            BottomNavigation {
                BottomNavigationItem(
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
