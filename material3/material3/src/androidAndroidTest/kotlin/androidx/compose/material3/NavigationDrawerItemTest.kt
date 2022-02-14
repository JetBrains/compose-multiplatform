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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class NavigationDrawerItemTest {

    @get:Rule
    val rule = createComposeRule()

    val DrawerItemTag = "drawer_item_tag"

    @Test
    fun navigationDrawerItem_sizes() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.width(264.dp)) {
                NavigationDrawerItem(
                    icon = {},
                    label = {},
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag(DrawerItemTag)
                )
            }
        }

        rule.onNodeWithTag(DrawerItemTag)
            .assertWidthIsEqualTo(264.dp)
            .assertHeightIsAtLeast(NavigationDrawerTokens.ActiveIndicatorHeight)
    }

    @Test
    fun navigationDrawerItem_paddings() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.width(264.dp)) {
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.testTag("icon")
                        )
                    },
                    label = {
                        Text("Label")
                    },
                    badge = {
                        Text("Badge")
                    },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag(DrawerItemTag)
                )
            }
        }

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // should be 16dp padding from the start
            .assertLeftPositionInRootIsEqualTo(16.dp)

        val iconLabelPadding =
            rule.onNodeWithText("Label", useUnmergedTree = true).getBoundsInRoot().left -
                rule.onNodeWithTag("icon", useUnmergedTree = true).getBoundsInRoot().right
        // should be 12dp padding between an icon and a label, also rounding error
        assertThat(iconLabelPadding - 12.dp).isLessThan(0.5.dp)

        val badgePadding =
            rule.onRoot().getBoundsInRoot().right -
                rule.onNodeWithText("Badge", useUnmergedTree = true).getBoundsInRoot().right
        // 24 at the end
        assertThat(badgePadding).isEqualTo(24.dp)
    }

    @Test
    fun navigationDrawerItem_labelOnly_paddings() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.width(264.dp)) {
                NavigationDrawerItem(
                    label = {
                        Text("Label")
                    },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag(DrawerItemTag)
                )
            }
        }

        rule.onNodeWithText("Label", useUnmergedTree = true)
            // should be 16dp padding from the start
            .assertLeftPositionInRootIsEqualTo(16.dp)
    }

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    NavigationDrawerItem(
                        modifier = Modifier.testTag("selected_item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null)
                        },
                        label = {
                            Text("ItemText")
                        },
                        selected = true,
                        onClick = {}
                    )
                    NavigationDrawerItem(
                        modifier = Modifier.testTag("unselected_item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null)
                        },
                        label = {
                            Text("ItemText")
                        },
                        selected = false,
                        onClick = {}
                    )
                }
            ) {
            }
        }

        rule.onNodeWithTag("selected_item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertHasClickAction()
        rule.onNodeWithTag("unselected_item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsNotSelected()
            .assertHasClickAction()
    }
}