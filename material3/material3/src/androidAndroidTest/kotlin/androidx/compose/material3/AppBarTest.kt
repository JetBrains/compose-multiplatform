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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.tokens.TopAppBarSmall
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
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
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AppBarTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun smallTopAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                SmallTopAppBar(title = { Text("Title") })
            }
            .assertHeightIsEqualTo(TopAppBarSmall.SmallContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun smallTopAppBar_withTitle() {
        val title = "Title"
        rule.setMaterialContent {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                SmallTopAppBar(title = { Text(title) })
            }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun smallTopAppBar_default_positioning() {
        rule.setMaterialContent {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                SmallTopAppBar(
                    navigationIcon = {
                        FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    },
                    title = {
                        Text("Title", Modifier.testTag(TitleTestTag))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag(ActionsTestTag))
                    }
                )
            }
        }

        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule.onNodeWithTag(NavigationIconTestTag)
            // Navigation icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Navigation icon should be centered within the height of the app bar.
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - AppBarTopAndBottomPadding - FakeIconSize
            )

        rule.onNodeWithTag(TitleTestTag)
            // Title should be 56.dp from the start
            // 4.dp padding for the whole app bar + 48.dp icon size + 4.dp title padding.
            .assertLeftPositionInRootIsEqualTo(4.dp + FakeIconSize + 4.dp)
            // Title should be vertically centered
            .assertTopPositionInRootIsEqualTo((appBarBounds.height - titleBounds.height) / 2)

        rule.onNodeWithTag(ActionsTestTag)
            // Action should be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
            // Action should be 8.dp from the top
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - AppBarTopAndBottomPadding - FakeIconSize
            )
    }

    @Test
    fun smallTopAppBar_noNavigationIcon_positioning() {
        rule.setMaterialContent {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                SmallTopAppBar(
                    title = {
                        Text("Title", Modifier.testTag(TitleTestTag))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag(ActionsTestTag))
                    }
                )
            }
        }

        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()

        rule.onNodeWithTag(TitleTestTag)
            // Title should now be placed 16.dp from the start, as there is no navigation icon
            // 4.dp padding for the whole app bar + 12.dp inset
            .assertLeftPositionInRootIsEqualTo(4.dp + 12.dp)

        rule.onNodeWithTag(ActionsTestTag)
            // Action should still be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
    }

    @Test
    fun smallTopAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent {
            SmallTopAppBar(title = {
                Text("Title")
                textStyle = LocalTextStyle.current
                expectedTextStyle =
                    MaterialTheme.typography.fromToken(TopAppBarSmall.SmallHeadlineFont)
            }
            )
        }
        assertThat(textStyle).isNotNull()
        assertThat(textStyle).isEqualTo(expectedTextStyle)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_contentColor() {
        var titleColor: Color = Color.Unspecified
        var navigationIconColor: Color = Color.Unspecified
        var actionsColor: Color = Color.Unspecified
        var expectedTitleColor: Color = Color.Unspecified
        var expectedNavigationIconColor: Color = Color.Unspecified
        var expectedActionsColor: Color = Color.Unspecified
        var expectedContainerColor: Color = Color.Unspecified

        rule.setMaterialContent {
            SmallTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                navigationIcon = {
                    FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    navigationIconColor = LocalContentColor.current
                    expectedNavigationIconColor =
                        TopAppBarDefaults.smallTopAppBarColors()
                            .navigationIconColor(scrollFraction = 0f).value
                    // scrollFraction = 0f to indicate no scroll.
                    expectedContainerColor = TopAppBarDefaults
                        .smallTopAppBarColors()
                        .containerColor(scrollFraction = 0f)
                        .value
                },
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    titleColor = LocalContentColor.current
                    expectedTitleColor = TopAppBarDefaults
                        .smallTopAppBarColors()
                        .titleColor(scrollFraction = 0f)
                        .value
                },
                actions = {
                    FakeIcon(Modifier.testTag(ActionsTestTag))
                    actionsColor = LocalContentColor.current
                    expectedActionsColor = TopAppBarDefaults
                        .smallTopAppBarColors()
                        .actionIconColor(scrollFraction = 0f)
                        .value
                }
            )
        }
        assertThat(navigationIconColor).isNotNull()
        assertThat(titleColor).isNotNull()
        assertThat(actionsColor).isNotNull()
        assertThat(navigationIconColor).isEqualTo(expectedNavigationIconColor)
        assertThat(titleColor).isEqualTo(expectedTitleColor)
        assertThat(actionsColor).isEqualTo(expectedActionsColor)

        rule.onNodeWithTag(TopAppBarTestTag).captureToImage()
            .assertContainsColor(expectedContainerColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_scrolledContentColor() {
        var expectedScrolledContainerColor: Color = Color.Unspecified
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        rule.setMaterialContent {
            SmallTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    // scrollFraction = 1f to indicate a scroll.
                    expectedScrolledContainerColor =
                        TopAppBarDefaults.smallTopAppBarColors()
                            .containerColor(scrollFraction = 1f).value
                },
                scrollBehavior = scrollBehavior
            )
        }

        // Simulate scrolled content.
        scrollBehavior.contentOffset = -100f
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag).captureToImage()
            .assertContainsColor(expectedScrolledContainerColor)
    }

    /**
     * An [IconButton] with an [Icon] inside for testing positions.
     *
     * An [IconButton] is defaulted to be 48X48dp, while its child [Icon] is defaulted to 24x24dp.
     */
    private val FakeIcon = @Composable { modifier: Modifier ->
        IconButton(
            onClick = { /* doSomething() */ },
            modifier = modifier.semantics(mergeDescendants = true) {}
        ) {
            Icon(ColorPainter(Color.Red), null)
        }
    }

    private fun expectedActionPosition(appBarWidth: Dp): Dp =
        appBarWidth - AppBarStartAndEndPadding - FakeIconSize

    private val FakeIconSize = 48.dp
    private val AppBarStartAndEndPadding = 4.dp
    private val AppBarTopAndBottomPadding = (TopAppBarSmall.SmallContainerHeight - FakeIconSize) / 2

    private val TopAppBarTestTag = "bar"
    private val NavigationIconTestTag = "navigationIcon"
    private val TitleTestTag = "title"
    private val ActionsTestTag = "actions"
}
