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
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.tokens.BottomAppBarTokens
import androidx.compose.material3.tokens.TopAppBarLargeTokens
import androidx.compose.material3.tokens.TopAppBarMediumTokens
import androidx.compose.material3.tokens.TopAppBarSmallCenteredTokens
import androidx.compose.material3.tokens.TopAppBarSmallTokens
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AppBarTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun smallTopAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                SmallTopAppBar(title = { Text("Title") })
            }
            .assertHeightIsEqualTo(TopAppBarSmallTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun smallTopAppBar_withTitle() {
        val title = "Title"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                SmallTopAppBar(title = { Text(title) })
            }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun smallTopAppBar_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
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
        assertSmallDefaultPositioning()
    }

    @Test
    fun smallTopAppBar_noNavigationIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
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
        assertSmallPositioningWithoutNavigation()
    }

    @Test
    fun smallTopAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            SmallTopAppBar(title = {
                Text("Title")
                textStyle = LocalTextStyle.current
                expectedTextStyle =
                    MaterialTheme.typography.fromToken(TopAppBarSmallTokens.HeadlineFont)
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

        rule.setMaterialContent(lightColorScheme()) {
            SmallTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                navigationIcon = {
                    FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    navigationIconColor = LocalContentColor.current
                    expectedNavigationIconColor =
                        TopAppBarDefaults.smallTopAppBarColors()
                            .navigationIconContentColor(scrollFraction = 0f).value
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
                        .titleContentColor(scrollFraction = 0f)
                        .value
                },
                actions = {
                    FakeIcon(Modifier.testTag(ActionsTestTag))
                    actionsColor = LocalContentColor.current
                    expectedActionsColor = TopAppBarDefaults
                        .smallTopAppBarColors()
                        .actionIconContentColor(scrollFraction = 0f)
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

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_scrolledContentColor() {
        var expectedScrolledContainerColor: Color = Color.Unspecified
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState())
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
        rule.runOnIdle {
            scrollBehavior.state.contentOffset = -100f
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag).captureToImage()
            .assertContainsColor(expectedScrolledContainerColor)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallTopAppBar_scrolledPositioning() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val scrollOffsetDp = 20.dp
        var scrollOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior =
                TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarScrollState())
            scrollOffsetPx = with(LocalDensity.current) { scrollOffsetDp.toPx() }
            SmallTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                scrollBehavior = scrollBehavior
            )
        }

        // Simulate scrolled content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -scrollOffsetPx
            scrollBehavior.state.contentOffset = -scrollOffsetPx
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(TopAppBarSmallTokens.ContainerHeight - scrollOffsetDp)
    }

    @Test
    fun centerAlignedTopAppBar_expandsToScreen() {
        rule.setMaterialContentForSizeAssertions {
            CenterAlignedTopAppBar(title = { Text("Title") })
        }
            .assertHeightIsEqualTo(TopAppBarSmallCenteredTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun centerAlignedTopAppBar_withTitle() {
        val title = "Title"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(title = { Text(title) })
            }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun centerAlignedTopAppBar_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
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
        assertSmallDefaultPositioning(isCenteredTitle = true)
    }

    @Test
    fun centerAlignedTopAppBar_noNavigationIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text("Title", Modifier.testTag(TitleTestTag))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag(ActionsTestTag))
                    }
                )
            }
        }
        assertSmallPositioningWithoutNavigation(isCenteredTitle = true)
    }

    @Test
    fun centerAlignedTopAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            CenterAlignedTopAppBar(
                title = {
                    Text("Title")
                    textStyle = LocalTextStyle.current
                    expectedTextStyle =
                        MaterialTheme.typography.fromToken(
                            TopAppBarSmallCenteredTokens.HeadlineFont
                        )
                }
            )
        }
        assertThat(textStyle).isNotNull()
        assertThat(textStyle).isEqualTo(expectedTextStyle)
    }

    @Test
    fun centerAlignedTopAppBar_measureWithNonZeroMinWidth() {
        var appBarSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
            CenterAlignedTopAppBar(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = constraints.maxWidth)
                    )
                    appBarSize = IntSize(placeable.width, placeable.height)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
                title = {
                    Text("Title")
                }
            )
        }

        assertThat(appBarSize).isNotEqualTo(IntSize.Zero)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun centerAlignedTopAppBar_contentColor() {
        var titleColor: Color = Color.Unspecified
        var navigationIconColor: Color = Color.Unspecified
        var actionsColor: Color = Color.Unspecified
        var expectedTitleColor: Color = Color.Unspecified
        var expectedNavigationIconColor: Color = Color.Unspecified
        var expectedActionsColor: Color = Color.Unspecified
        var expectedContainerColor: Color = Color.Unspecified

        rule.setMaterialContent(lightColorScheme()) {
            CenterAlignedTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                navigationIcon = {
                    FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    navigationIconColor = LocalContentColor.current
                    expectedNavigationIconColor =
                        TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .navigationIconContentColor(scrollFraction = 0f).value
                    // scrollFraction = 0f to indicate no scroll.
                    expectedContainerColor =
                        TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .containerColor(scrollFraction = 0f).value
                },
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    titleColor = LocalContentColor.current
                    expectedTitleColor =
                        TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .titleContentColor(scrollFraction = 0f).value
                },
                actions = {
                    FakeIcon(Modifier.testTag(ActionsTestTag))
                    actionsColor = LocalContentColor.current
                    expectedActionsColor =
                        TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .actionIconContentColor(scrollFraction = 0f).value
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

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun centerAlignedTopAppBar_scrolledContentColor() {
        var expectedScrolledContainerColor: Color = Color.Unspecified
        lateinit var scrollBehavior: TopAppBarScrollBehavior

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState())
            CenterAlignedTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    // scrollFraction = 1f to indicate a scroll.
                    expectedScrolledContainerColor =
                        TopAppBarDefaults.centerAlignedTopAppBarColors()
                            .containerColor(scrollFraction = 1f).value
                },
                scrollBehavior = scrollBehavior
            )
        }

        // Simulate scrolled content.
        rule.runOnIdle {
            scrollBehavior.state.contentOffset = -100f
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag).captureToImage()
            .assertContainsColor(expectedScrolledContainerColor)
    }

    @Test
    fun mediumTopAppBar_expandsToScreen() {
        rule.setMaterialContentForSizeAssertions {
            MediumTopAppBar(title = { Text("Medium Title") })
        }
            .assertHeightIsEqualTo(TopAppBarMediumTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun mediumTopAppBar_expanded_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
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

        // The bottom text baseline should be 24.dp from the bottom of the app bar.
        assertMediumOrLargeDefaultPositioning(
            expectedAppBarHeight = TopAppBarMediumTokens.ContainerHeight,
            bottomTextPadding = 24.dp
        )
    }

    @Test
    fun mediumTopAppBar_scrolled_positioning() {
        val content = @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = {
                        FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    },
                    title = {
                        Text("Title", Modifier.testTag(TitleTestTag))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag(ActionsTestTag))
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
        assertMediumOrLargeScrolledHeight(
            TopAppBarMediumTokens.ContainerHeight,
            TopAppBarSmallTokens.ContainerHeight,
            content
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumTopAppBar_scrolledContainerColor() {
        val content = @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
            MediumTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                },
                scrollBehavior = scrollBehavior
            )
        }

        assertMediumOrLargeScrolledColors(
            TopAppBarMediumTokens.ContainerHeight,
            TopAppBarSmallTokens.ContainerHeight,
            content
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumTopAppBar_semantics() {
        val content = @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
            MediumTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                },
                scrollBehavior = scrollBehavior
            )
        }

        assertMediumOrLargeScrolledSemantics(
            TopAppBarMediumTokens.ContainerHeight,
            TopAppBarSmallTokens.ContainerHeight,
            content
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeTopAppBar_semantics() {
        val content = @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
            LargeTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                },
                scrollBehavior = scrollBehavior
            )
        }
        assertMediumOrLargeScrolledSemantics(
            TopAppBarLargeTokens.ContainerHeight,
            TopAppBarSmallTokens.ContainerHeight,
            content
        )
    }

    @Test
    fun largeTopAppBar_expandsToScreen() {
        rule.setMaterialContentForSizeAssertions {
            LargeTopAppBar(title = { Text("Large Title") })
        }
            .assertHeightIsEqualTo(TopAppBarLargeTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun largeTopAppBar_expanded_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
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

        // The bottom text baseline should be 28.dp from the bottom of the app bar.
        assertMediumOrLargeDefaultPositioning(
            expectedAppBarHeight = TopAppBarLargeTokens.ContainerHeight,
            bottomTextPadding = 28.dp
        )
    }

    @Test
    fun largeTopAppBar_scrolled_positioning() {
        val content = @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = {
                        FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    },
                    title = {
                        Text("Title", Modifier.testTag(TitleTestTag))
                    },
                    actions = {
                        FakeIcon(Modifier.testTag(ActionsTestTag))
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
        assertMediumOrLargeScrolledHeight(
            TopAppBarLargeTokens.ContainerHeight,
            TopAppBarSmallTokens.ContainerHeight,
            content
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeTopAppBar_scrolledContainerColor() {
        val content = @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
            LargeTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                },
                scrollBehavior = scrollBehavior
            )
        }
        assertMediumOrLargeScrolledColors(
            TopAppBarLargeTokens.ContainerHeight,
            TopAppBarSmallTokens.ContainerHeight,
            content
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_enterAlways_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(
                TopAppBarDefaults.enterAlwaysScrollBehavior(
                    rememberTopAppBarScrollState()
                ), state
            )
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_exitUntilCollapsed_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    rememberSplineBasedDecay(),
                    rememberTopAppBarScrollState()
                ), state
            )
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_pinned_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(
                TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState()),
                state
            )
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun state_restoresTopAppBarScrollState() {
        val restorationTester = StateRestorationTester(rule)
        var topAppBarScrollState: TopAppBarScrollState? = null
        restorationTester.setContent {
            topAppBarScrollState = rememberTopAppBarScrollState()
        }

        rule.runOnIdle {
            topAppBarScrollState!!.offsetLimit = -350f
            topAppBarScrollState!!.offset = -300f
            topAppBarScrollState!!.contentOffset = -550f
        }

        topAppBarScrollState = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(topAppBarScrollState!!.offsetLimit).isEqualTo(-350f)
            assertThat(topAppBarScrollState!!.offset).isEqualTo(-300f)
            assertThat(topAppBarScrollState!!.contentOffset).isEqualTo(-550f)
        }
    }

    @Test
    fun bottomAppBarWithFAB_heightIsFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                BottomAppBar(
                    icons = {},
                    floatingActionButton = {
                        BottomAppBarDefaults.FloatingActionButton(
                            onClick = { /* do something */ }
                        ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                })
            }
            .assertHeightIsEqualTo(BottomAppBarTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun bottomAppBar_widthExpandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                BottomAppBar {}
            }
            .assertHeightIsEqualTo(BottomAppBarTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun bottomAppBar_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            BottomAppBar(Modifier.testTag("bar")) {
                FakeIcon(Modifier.testTag("icon"))
            }
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule.onNodeWithTag("icon")
            // Child icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Child icon should be 10.dp from the top
            .assertTopPositionInRootIsEqualTo(
                BottomAppBarTopPadding +
                    (appBarBottomEdgeY - BottomAppBarTopPadding - FakeIconSize) / 2
            )
    }

    @Test
    fun bottomAppBarWithFAB_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            BottomAppBar(
                icons = {},
                Modifier.testTag("bar"),
                floatingActionButton = {
                    BottomAppBarDefaults.FloatingActionButton(
                        modifier = Modifier.testTag("FAB"),
                        onClick = { /* do something */ },
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                })
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()

        val fabBounds = rule.onNodeWithTag("FAB").getUnclippedBoundsInRoot()

        rule.onNodeWithTag("FAB")
            // FAB should be 16.dp from the end
            .assertLeftPositionInRootIsEqualTo(appBarBounds.width - 16.dp - fabBounds.width)
            // FAB should be 12.dp from the top
            .assertTopPositionInRootIsEqualTo(12.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MultiPageContent(scrollBehavior: TopAppBarScrollBehavior, state: LazyListState) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SmallTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text(text = "Title") },
                    scrollBehavior = scrollBehavior,
                )
            }
        ) { contentPadding ->
            LazyRow(
                Modifier
                    .fillMaxSize()
                    .testTag(LazyListTag), state
            ) {
                items(2) { page ->
                    LazyColumn(
                        modifier = Modifier.fillParentMaxSize(),
                        contentPadding = contentPadding
                    ) {
                        items(50) {
                            Text(
                                modifier = Modifier.fillParentMaxWidth(),
                                text = "Item #$page x $it"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks the app bar's components positioning when it's a [SmallTopAppBar], a
     * [CenterAlignedTopAppBar], or a larger app bar that is scrolled up and collapsed into a small
     * configuration and there is no navigation icon.
     */
    private fun assertSmallPositioningWithoutNavigation(isCenteredTitle: Boolean = false) {
        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()

        val titleNode = rule.onNodeWithTag(TitleTestTag)
        // Title should be vertically centered
        titleNode.assertTopPositionInRootIsEqualTo((appBarBounds.height - titleBounds.height) / 2)
        if (isCenteredTitle) {
            // Title should be horizontally centered
            titleNode.assertLeftPositionInRootIsEqualTo(
                (appBarBounds.width - titleBounds.width) / 2
            )
        } else {
            // Title should now be placed 16.dp from the start, as there is no navigation icon
            // 4.dp padding for the whole app bar + 12.dp inset
            titleNode.assertLeftPositionInRootIsEqualTo(4.dp + 12.dp)
        }

        rule.onNodeWithTag(ActionsTestTag)
            // Action should still be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
    }

    /**
     * Checks the app bar's components positioning when it's a [SmallTopAppBar] or a
     * [CenterAlignedTopAppBar].
     */
    private fun assertSmallDefaultPositioning(isCenteredTitle: Boolean = false) {
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

        val titleNode = rule.onNodeWithTag(TitleTestTag)
        // Title should be vertically centered
        titleNode.assertTopPositionInRootIsEqualTo((appBarBounds.height - titleBounds.height) / 2)
        if (isCenteredTitle) {
            // Title should be horizontally centered
            titleNode.assertLeftPositionInRootIsEqualTo(
                (appBarBounds.width - titleBounds.width) / 2
            )
        } else {
            // Title should be 56.dp from the start
            // 4.dp padding for the whole app bar + 48.dp icon size + 4.dp title padding.
            titleNode.assertLeftPositionInRootIsEqualTo(4.dp + FakeIconSize + 4.dp)
        }

        rule.onNodeWithTag(ActionsTestTag)
            // Action should be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
            // Action should be 8.dp from the top
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - AppBarTopAndBottomPadding - FakeIconSize
            )
    }

    /**
     * Checks the app bar's components positioning when it's a [MediumTopAppBar] or a
     * [LargeTopAppBar].
     */
    private fun assertMediumOrLargeDefaultPositioning(
        expectedAppBarHeight: Dp,
        bottomTextPadding: Dp
    ) {
        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        appBarBounds.height.assertIsEqualTo(expectedAppBarHeight, "top app bar height")

        // Expecting the title composable to be reused for the top and bottom rows of the top app
        // bar, so obtaining the node with the title tag should return two nodes, one for each row.
        val allTitleNodes = rule.onAllNodesWithTag(TitleTestTag, true)
        allTitleNodes.assertCountEquals(2)
        val topTitleNode = allTitleNodes.onFirst()
        val bottomTitleNode = allTitleNodes.onLast()

        val topTitleBounds = topTitleNode.getUnclippedBoundsInRoot()
        val bottomTitleBounds = bottomTitleNode.getUnclippedBoundsInRoot()
        val topAppBarBottomEdgeY = appBarBounds.top + TopAppBarSmallTokens.ContainerHeight
        val bottomAppBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule.onNodeWithTag(NavigationIconTestTag)
            // Navigation icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Navigation icon should be centered within the height of the top part of the app bar.
            .assertTopPositionInRootIsEqualTo(
                topAppBarBottomEdgeY - AppBarTopAndBottomPadding - FakeIconSize
            )

        rule.onNodeWithTag(ActionsTestTag)
            // Action should be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
            // Action should be 8.dp from the top
            .assertTopPositionInRootIsEqualTo(
                topAppBarBottomEdgeY - AppBarTopAndBottomPadding - FakeIconSize
            )

        topTitleNode
            // Top title should be 56.dp from the start
            // 4.dp padding for the whole app bar + 48.dp icon size + 4.dp title padding.
            .assertLeftPositionInRootIsEqualTo(4.dp + FakeIconSize + 4.dp)
            // Title should be vertically centered in the top part, which has a height of a small
            // app bar.
            .assertTopPositionInRootIsEqualTo((topAppBarBottomEdgeY - topTitleBounds.height) / 2)

        bottomTitleNode
            // Bottom title should be 16.dp from the start.
            .assertLeftPositionInRootIsEqualTo(16.dp)

        // Check if the bottom text baseline is at the expected distance from the bottom of the
        // app bar.
        val bottomTextBaselineY = bottomTitleBounds.top + bottomTitleNode.getLastBaselinePosition()
        (bottomAppBarBottomEdgeY - bottomTextBaselineY).assertIsEqualTo(
            bottomTextPadding,
            "text baseline distance from the bottom"
        )
    }

    /**
     * Checks that changing values at a [MediumTopAppBar] or a [LargeTopAppBar] scroll behavior
     * affects the height of the app bar.
     *
     * This check partially and fully collapses the app bar to test its height.
     *
     * @param appBarMaxHeight the max height of the app bar [content]
     * @param appBarMinHeight the min height of the app bar [content]
     * @param content a Composable that adds a MediumTopAppBar or a LargeTopAppBar
     */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun assertMediumOrLargeScrolledHeight(
        appBarMaxHeight: Dp,
        appBarMinHeight: Dp,
        content: @Composable (TopAppBarScrollBehavior?) -> Unit
    ) {
        val fullyCollapsedOffsetDp = appBarMaxHeight - appBarMinHeight
        val partiallyCollapsedOffsetDp = fullyCollapsedOffsetDp / 3
        var partiallyCollapsedOffsetPx = 0f
        var fullyCollapsedOffsetPx = 0f
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    rememberSplineBasedDecay(),
                    rememberTopAppBarScrollState()
                )
            with(LocalDensity.current) {
                partiallyCollapsedOffsetPx = partiallyCollapsedOffsetDp.toPx()
                fullyCollapsedOffsetPx = fullyCollapsedOffsetDp.toPx()
            }

            content(scrollBehavior)
        }

        // Simulate a partially collapsed app bar.
        rule.runOnIdle {
            scrollBehavior.state.offset = -partiallyCollapsedOffsetPx
            scrollBehavior.state.contentOffset = -partiallyCollapsedOffsetPx
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(appBarMaxHeight - partiallyCollapsedOffsetDp)

        // Simulate a fully collapsed app bar.
        rule.runOnIdle {
            scrollBehavior.state.offset = -fullyCollapsedOffsetPx
            // Simulate additional content scroll beyond the max offset scroll.
            scrollBehavior.state.contentOffset =
                -fullyCollapsedOffsetPx - partiallyCollapsedOffsetPx
        }
        rule.waitForIdle()
        // Check that the app bar collapsed to its min height.
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(appBarMinHeight)
    }

    /**
     * Checks that changing values at a [MediumTopAppBar] or a [LargeTopAppBar] scroll behavior
     * affects the container color and the title's content color of the app bar.
     *
     * This check partially and fully collapses the app bar to test its colors.
     *
     * @param appBarMaxHeight the max height of the app bar [content]
     * @param appBarMinHeight the min height of the app bar [content]
     * @param content a Composable that adds a MediumTopAppBar or a LargeTopAppBar
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun assertMediumOrLargeScrolledColors(
        appBarMaxHeight: Dp,
        appBarMinHeight: Dp,
        content: @Composable (TopAppBarScrollBehavior?) -> Unit
    ) {
        val fullyCollapsedOffsetDp = appBarMaxHeight - appBarMinHeight
        val oneThirdCollapsedOffsetDp = fullyCollapsedOffsetDp / 3
        var fullyCollapsedOffsetPx = 0f
        var oneThirdCollapsedOffsetPx = 0f
        var fullyCollapsedContainerColor: Color = Color.Unspecified
        var oneThirdCollapsedContainerColor: Color = Color.Unspecified
        var titleContentColor: Color = Color.Unspecified
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    rememberSplineBasedDecay(),
                    rememberTopAppBarScrollState()
                )
            // Using the mediumTopAppBarColors for both Medium and Large top app bars, as the
            // current content color settings are the same.
            oneThirdCollapsedContainerColor =
                TopAppBarDefaults.mediumTopAppBarColors()
                    .containerColor(scrollFraction = 1 / 3f).value
            fullyCollapsedContainerColor =
                TopAppBarDefaults.mediumTopAppBarColors()
                    .containerColor(scrollFraction = 1f).value

            // Resolve the title's content color. The default implementation returns the same color
            // regardless of the scrollFraction, and the color is applied later with alpha.
            titleContentColor =
                TopAppBarDefaults.mediumTopAppBarColors()
                    .titleContentColor(scrollFraction = 1f).value

            with(LocalDensity.current) {
                oneThirdCollapsedOffsetPx = oneThirdCollapsedOffsetDp.toPx()
                fullyCollapsedOffsetPx = fullyCollapsedOffsetDp.toPx()
            }

            content(scrollBehavior)
        }

        // Expecting the title composable to be reused for the top and bottom rows of the top app
        // bar, so obtaining the node with the title tag should return two nodes, one for each row.
        val allTitleNodes = rule.onAllNodesWithTag(TitleTestTag, true)
        allTitleNodes.assertCountEquals(2)
        val topTitleNode = allTitleNodes.onFirst()
        val bottomTitleNode = allTitleNodes.onLast()

        // Simulate 1/3 collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -oneThirdCollapsedOffsetPx
            scrollBehavior.state.contentOffset = -oneThirdCollapsedOffsetPx
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag).captureToImage()
            .assertContainsColor(oneThirdCollapsedContainerColor)

        // Both top and bottom titles should be visible. The top should have the title text color
        // with ~33.333% alpha, and the bottom with ~66.666% alpha.
        topTitleNode.captureToImage()
            .assertContainsColor(
                titleContentColor.copy(alpha = 1 / 3f)
                    .compositeOver(oneThirdCollapsedContainerColor)
            )
        bottomTitleNode.captureToImage()
            .assertContainsColor(
                titleContentColor.copy(alpha = 2 / 3f)
                    .compositeOver(oneThirdCollapsedContainerColor)
            )

        // Simulate fully collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -fullyCollapsedOffsetPx
            scrollBehavior.state.contentOffset = -fullyCollapsedOffsetPx
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TopAppBarTestTag).captureToImage()
            .assertContainsColor(fullyCollapsedContainerColor)
        // Only the top title should be visible in the collapsed form.
        topTitleNode.captureToImage().assertContainsColor(titleContentColor)
        bottomTitleNode.assertIsNotDisplayed()
    }

    /**
     * Checks that changing values at a [MediumTopAppBar] or a [LargeTopAppBar] scroll behavior
     * affects the title's semantics.
     *
     * This check partially and fully collapses the app bar to test the semantics.
     *
     * @param appBarMaxHeight the max height of the app bar [content]
     * @param appBarMinHeight the min height of the app bar [content]
     * @param content a Composable that adds a MediumTopAppBar or a LargeTopAppBar
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun assertMediumOrLargeScrolledSemantics(
        appBarMaxHeight: Dp,
        appBarMinHeight: Dp,
        content: @Composable (TopAppBarScrollBehavior?) -> Unit
    ) {
        val fullyCollapsedOffsetDp = appBarMaxHeight - appBarMinHeight
        val oneThirdCollapsedOffsetDp = fullyCollapsedOffsetDp / 3
        var fullyCollapsedOffsetPx = 0f
        var oneThirdCollapsedOffsetPx = 0f
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    rememberSplineBasedDecay(),
                    rememberTopAppBarScrollState()
                )
            with(LocalDensity.current) {
                oneThirdCollapsedOffsetPx = oneThirdCollapsedOffsetDp.toPx()
                fullyCollapsedOffsetPx = fullyCollapsedOffsetDp.toPx()
            }

            content(scrollBehavior)
        }

        // Asserting that only one semantic title node is returned after the clearAndSetSemantics is
        // applied to the merged tree according to the alpha values of the titles.
        assertSingleTitleSemanticNode()

        // Simulate 1/3 collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -oneThirdCollapsedOffsetPx
            scrollBehavior.state.contentOffset = -oneThirdCollapsedOffsetPx
        }
        rule.waitForIdle()

        // Assert that only one semantic title node is available while scrolling the app bar.
        assertSingleTitleSemanticNode()

        // Simulate fully collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -fullyCollapsedOffsetPx
            scrollBehavior.state.contentOffset = -fullyCollapsedOffsetPx
        }
        rule.waitForIdle()

        // Assert that only one semantic title node is available.
        assertSingleTitleSemanticNode()
    }

    /**
     * Asserts that only one semantic node exists at app bar title when the tree is merged.
     */
    private fun assertSingleTitleSemanticNode() {
        val unmergedTitleNodes = rule.onAllNodesWithTag(TitleTestTag, useUnmergedTree = true)
        unmergedTitleNodes.assertCountEquals(2)

        val mergedTitleNodes = rule.onAllNodesWithTag(TitleTestTag, useUnmergedTree = false)
        mergedTitleNodes.assertCountEquals(1)
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
    private val AppBarTopAndBottomPadding =
        (TopAppBarSmallTokens.ContainerHeight - FakeIconSize) / 2
    private val BottomAppBarTopPadding = 16.dp - 12.dp

    private val LazyListTag = "lazyList"
    private val TopAppBarTestTag = "bar"
    private val NavigationIconTestTag = "navigationIcon"
    private val TitleTestTag = "title"
    private val ActionsTestTag = "actions"
}
