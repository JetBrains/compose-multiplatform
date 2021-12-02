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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AppBarTest {

    @get:Rule
    val rule = createComposeRule()

    private val appBarHeight = 56.dp

    @Test
    fun topAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                TopAppBar(title = { Text("Title") })
            }
            .assertHeightIsEqualTo(appBarHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun topAppBar_withTitle() {
        val title = "Title"
        rule.setMaterialContent {
            TopAppBar(title = { Text(title) })
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun topAppBar_default_positioning() {
        rule.setMaterialContent {
            Box(Modifier.testTag("bar")) {
                TopAppBar(
                    navigationIcon = {
                        FakeIcon(Modifier.testTag("navigationIcon"))
                    },
                    title = {
                        Text("title", Modifier.testTag("title"))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag("action"))
                    }
                )
            }
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag("title").getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule.onNodeWithTag("navigationIcon")
            // Navigation icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Navigation icon should be 4.dp from the bottom
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - AppBarStartAndEndPadding - FakeIconSize
            )

        rule.onNodeWithTag("title")
            // Title should be 72.dp from the start
            // 4.dp padding for the whole app bar + 68.dp inset
            .assertLeftPositionInRootIsEqualTo(4.dp + 68.dp)
            // Title should be vertically centered
            .assertTopPositionInRootIsEqualTo((appBarBounds.height - titleBounds.height) / 2)

        rule.onNodeWithTag("action")
            // Action should be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
            // Action should be 4.dp from the bottom
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - AppBarStartAndEndPadding - FakeIconSize
            )
    }

    @Test
    fun topAppBar_noNavigationIcon_positioning() {
        rule.setMaterialContent {
            Box(Modifier.testTag("bar")) {
                TopAppBar(
                    title = {
                        Text("title", Modifier.testTag("title"))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag("action"))
                    }
                )
            }
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()

        rule.onNodeWithTag("title")
            // Title should now be placed 16.dp from the start, as there is no navigation icon
            // 4.dp padding for the whole app bar + 12.dp inset
            .assertLeftPositionInRootIsEqualTo(4.dp + 12.dp)

        rule.onNodeWithTag("action")
            // Action should still be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
    }

    @Test
    fun topAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var h6Style: TextStyle? = null
        rule.setMaterialContent {
            Box {
                TopAppBar(
                    title = {
                        Text("App Bar Title")
                        textStyle = LocalTextStyle.current
                        h6Style = MaterialTheme.typography.h6
                    }
                )
            }
        }
        assertThat(textStyle!!.fontSize).isEqualTo(h6Style!!.fontSize)
        assertThat(textStyle!!.fontFamily).isEqualTo(h6Style!!.fontFamily)
    }

    @Test
    fun topAppBar_contentAlpha() {
        var titleAlpha: Float? = null
        var navigationIconAlpha: Float? = null
        var actionAlpha: Float? = null
        var high: Float? = null
        var medium: Float? = null
        rule.setMaterialContent {
            Box {
                TopAppBar(
                    title = {
                        titleAlpha = LocalContentAlpha.current
                        high = ContentAlpha.high
                        medium = ContentAlpha.medium
                    },
                    navigationIcon = { navigationIconAlpha = LocalContentAlpha.current },
                    actions = { actionAlpha = LocalContentAlpha.current }
                )
            }
        }
        assertThat(titleAlpha).isNotNull()
        assertThat(navigationIconAlpha).isNotNull()
        assertThat(actionAlpha).isNotNull()
        assertThat(high).isNotNull()
        assertThat(medium).isNotNull()
        assertThat(titleAlpha).isEqualTo(high)
        assertThat(navigationIconAlpha).isEqualTo(high)
        assertThat(actionAlpha).isEqualTo(medium)
    }

    @Test
    fun topAppBar_genericContent_contentAlpha() {
        var alpha: Float? = null
        var medium: Float? = null
        rule.setMaterialContent {
            Box {
                TopAppBar {
                    alpha = LocalContentAlpha.current
                    medium = ContentAlpha.medium
                }
            }
        }
        assertThat(alpha).isNotNull()
        assertThat(medium).isNotNull()
        assertThat(alpha).isEqualTo(medium)
    }

    @Test
    fun bottomAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                BottomAppBar {}
            }
            .assertHeightIsEqualTo(appBarHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun bottomAppBar_default_positioning() {
        rule.setMaterialContent {
            BottomAppBar(Modifier.testTag("bar")) {
                FakeIcon(Modifier.testTag("icon"))
            }
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule.onNodeWithTag("icon")
            // Child icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Child icon should be 4.dp from the bottom
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - AppBarStartAndEndPadding - FakeIconSize
            )
    }

    @Test
    fun bottomAppBar_contentAlpha() {
        var alpha: Float? = null
        var medium: Float? = null
        rule.setMaterialContent {
            Box {
                BottomAppBar {
                    alpha = LocalContentAlpha.current
                    medium = ContentAlpha.medium
                }
            }
        }
        assertThat(alpha).isNotNull()
        assertThat(medium).isNotNull()
        assertThat(alpha).isEqualTo(medium)
    }

    /**
     * A 48.dp box with a red box inside, to simulate an [IconButton] with a real icon inside for
     * testing positions.
     */
    private val FakeIcon = @Composable { modifier: Modifier ->
        Box(Modifier.size(48.dp)) {
            Icon(ColorPainter(Color.Red), null, modifier)
        }
    }

    private fun expectedActionPosition(appBarWidth: Dp): Dp =
        appBarWidth - AppBarStartAndEndPadding - FakeIconSize

    private val AppBarStartAndEndPadding = 4.dp

    private val FakeIconSize = 48.dp
}
