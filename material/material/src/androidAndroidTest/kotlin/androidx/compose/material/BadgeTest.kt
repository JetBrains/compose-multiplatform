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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BadgeTest {

    private val icon = Icons.Filled.Favorite

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun badge_noContent_size() {
        rule
            .setMaterialContentForSizeAssertions {
                Badge()
            }
            .assertHeightIsEqualTo(BadgeRadius * 2)
            .assertWidthIsEqualTo(BadgeRadius * 2)
    }

    @Test
    fun badge_shortContent_size() {
        rule
            .setMaterialContentForSizeAssertions {
                Badge { Text("1") }
            }
            .assertHeightIsEqualTo(BadgeWithContentRadius * 2)
            .assertWidthIsEqualTo(BadgeWithContentRadius * 2)
    }

    @Test
    fun badge_longContent_size() {
        rule
            .setMaterialContentForSizeAssertions {
                Badge { Text("999+") }
            }
            .assertHeightIsEqualTo(BadgeWithContentRadius * 2)
            .assertWidthIsAtLeast(BadgeWithContentRadius * 2)
    }

    @Test
    fun badge_shortContent_customSizeModifier_size() {
        val customWidth = 24.dp
        val customHeight = 6.dp
        rule
            .setMaterialContentForSizeAssertions {
                Badge(modifier = Modifier.size(customWidth, customHeight)) {
                    Text("1")
                }
            }
            .assertHeightIsEqualTo(customHeight)
            .assertWidthIsEqualTo(customWidth)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // captureToImage() requires API level 26
    fun badge_noContent_shape() {
        var errorColor = Color.Unspecified
        rule.setMaterialContent {
            errorColor = MaterialTheme.colors.error
            Badge(modifier = Modifier.testTag(TestBadgeTag))
        }

        rule.onNodeWithTag(TestBadgeTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor = errorColor,
                backgroundColor = Color.White
            )
    }

    @Test
    fun badgeBox_noContent_position() {
        rule
            .setMaterialContent {
                BadgedBox(badge = { Badge(Modifier.testTag(TestBadgeTag)) }) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.testTag(TestAnchorTag)
                    )
                }
            }
        val badge = rule.onNodeWithTag(TestBadgeTag)
        val anchorBounds = rule.onNodeWithTag(TestAnchorTag).getUnclippedBoundsInRoot()
        val badgeBounds = badge.getUnclippedBoundsInRoot()
        badge.assertPositionInRootIsEqualTo(
            expectedLeft =
                anchorBounds.right + BadgeHorizontalOffset +
                    max((BadgeRadius - badgeBounds.width) / 2, 0.dp),
            expectedTop = -badgeBounds.height / 2
        )
    }

    @Test
    fun badgeBox_shortContent_position() {
        rule
            .setMaterialContent {
                BadgedBox(badge = { Badge { Text("8") } }) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.testTag(TestAnchorTag)
                    )
                }
            }
        val badge = rule.onNodeWithTag(TestAnchorTag).onSibling()
        val anchorBounds = rule.onNodeWithTag(TestAnchorTag).getUnclippedBoundsInRoot()
        val badgeBounds = badge.getUnclippedBoundsInRoot()
        badge.assertPositionInRootIsEqualTo(
            expectedLeft = anchorBounds.right + BadgeWithContentHorizontalOffset + 4.dp + max
            (
                (
                    BadgeWithContentRadius - badgeBounds.width
                    ) / 2,
                0.dp
            ),
            expectedTop = -badgeBounds.height / 2
        )
    }

    @Test
    fun badgeBox_longContent_position() {
        rule
            .setMaterialContent {
                BadgedBox(badge = { Badge { Text("999+") } }) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.testTag(TestAnchorTag)
                    )
                }
            }
        val badge = rule.onNodeWithTag(TestAnchorTag).onSibling()
        val anchorBounds = rule.onNodeWithTag(TestAnchorTag).getUnclippedBoundsInRoot()
        val badgeBounds = badge.getUnclippedBoundsInRoot()

        val totalBadgeHorizontalOffset = BadgeWithContentHorizontalOffset +
            BadgeWithContentHorizontalPadding
        badge.assertPositionInRootIsEqualTo(
            expectedLeft = anchorBounds.right + totalBadgeHorizontalOffset,
            expectedTop = -badgeBounds.height / 2
        )
    }

    @Test
    fun badge_noClickAction_anchor_clickable() {
        val count = mutableStateOf(0f)
        rule.setMaterialContent {
            Badge(modifier = Modifier.testTag(TestBadgeTag)) {
                Tab(
                    text = { Text("Text") },
                    selected = true,
                    onClick = { count.value += 1 },
                    modifier = Modifier
                        .testTag("tab")
                )
            }
        }
        rule.onNodeWithTag(TestBadgeTag)
            .assertHasNoClickAction()
        Truth.assertThat(count.value).isEqualTo(0)
        rule.onNodeWithTag("tab")
            .performClick()
            .performClick()
        Truth.assertThat(count.value).isEqualTo(2)
    }

    @Test
    fun badge_on_tab_defaultSemantics() {
        rule.setMaterialContent {
            TabRow(0) {
                Tab(
                    text = { Badge { Text("Text") } },
                    modifier = Modifier.testTag("tab"),
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("tab")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()

        rule.onNodeWithTag("tab")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun badge_on_tab_disabledSemantics() {
        rule.setMaterialContent {
            Box {
                Tab(
                    enabled = false,
                    text = { Badge { Text("Text") } },
                    modifier = Modifier.testTag("tab"),
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("tab")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun badge_notMergingDescendants_withOwnContentDescription() {
        rule.setMaterialContent {
            BadgedBox(
                badge = {
                    Badge { Text("99+") }
                },
                modifier = Modifier.testTag(TestBadgeTag).semantics {
                    this.contentDescription = "more than 99 new email"
                }
            ) {
                Text(
                    "inbox",
                    Modifier.semantics {
                        this.contentDescription = "inbox"
                    }.testTag(TestAnchorTag)
                )
            }
        }

        rule.onNodeWithTag(TestBadgeTag).assertContentDescriptionEquals("more than 99 new email")
        rule.onNodeWithTag(TestAnchorTag).assertContentDescriptionEquals("inbox")
    }

    @Test
    fun badgeBox_size() {
        rule.setMaterialContentForSizeAssertions {
            BadgedBox(badge = { Badge { Text("999+") } }) {
                Icon(icon, null)
            }
        }
            .assertWidthIsEqualTo(icon.defaultWidth)
            .assertHeightIsEqualTo(icon.defaultHeight)
    }
}

private const val TestBadgeTag = "badge"
private const val TestAnchorTag = "anchor"
