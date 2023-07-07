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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.samples.LeadingIconTabs
import androidx.compose.material3.samples.ScrollingTextTabs
import androidx.compose.material3.samples.TextTabs
import androidx.compose.material3.tokens.PrimaryNavigationTabTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TabTest {

    private val ExpectedSmallTabHeight = 48.dp
    private val ExpectedLargeTabHeight = 72.dp

    private val icon = Icons.Filled.Favorite

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            TabRow(0) {
                Tab(
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag("tab"),
                    text = { Text("Text") }
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
    fun disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Tab(
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag("tab"),
                    enabled = false,
                    text = { Text("Text") }
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
    fun leadingIconTab_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            TabRow(0) {
                LeadingIconTab(
                    selected = true,
                    onClick = {},
                    text = { Text("Text") },
                    icon = { Icon(icon, null) },
                    modifier = Modifier.testTag("leadingIconTab")
                )
            }
        }

        rule.onNodeWithTag("leadingIconTab")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()

        rule.onNodeWithTag("leadingIconTab")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun leadingIconTab_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                LeadingIconTab(
                    selected = true,
                    onClick = {},
                    text = { Text("Text") },
                    icon = { Icon(icon, null) },
                    modifier = Modifier.testTag("leadingIconTab"),
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag("leadingIconTab")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun textTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Tab(selected = true, onClick = {}, text = { Text("Text") })
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun iconTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Tab(selected = true, onClick = {}, icon = { Icon(icon, null) })
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun textAndIconTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Surface {
                    Tab(
                        selected = true,
                        onClick = {},
                        text = { Text("Text and Icon") },
                        icon = { Icon(icon, null) }
                    )
                }
            }
            .assertHeightIsEqualTo(ExpectedLargeTabHeight)
    }

    @Test
    fun leadingIconTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Surface {
                    LeadingIconTab(
                        selected = true,
                        onClick = {},
                        text = { Text("Text") },
                        icon = { Icon(icon, null) }
                    )
                }
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun fixedTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp

        rule.setMaterialContent(lightColorScheme()) {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB 1", "TAB 2")

            val indicator = @Composable { tabPositions: List<TabPosition> ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[state])
                        .fillMaxWidth()
                        .height(indicatorHeight)
                        .background(color = Color.Red)
                        .testTag("indicator")
                )
            }

            Box(Modifier.testTag("tabRow")) {
                TabRow(
                    selectedTabIndex = state,
                    indicator = indicator
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()

        rule.onNodeWithTag("indicator", true)
            .assertPositionInRootIsEqualTo(
                expectedLeft = 0.dp,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        rule.onAllNodes(isSelectable())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        rule.onNodeWithTag("indicator", true)
            .assertPositionInRootIsEqualTo(
                expectedLeft = (tabRowBounds.width / 2),
                expectedTop = tabRowBounds.height - indicatorHeight
            )
    }

    @Test
    fun fixedTabRow_dividerHeight() {
        rule.setMaterialContent(lightColorScheme()) {
            val titles = listOf("TAB 1", "TAB 2")
            val tabRowHeight = 100.dp

            val divider = @Composable { Divider(Modifier.testTag("divider")) }

            Box(Modifier.testTag("tabRow")) {
                TabRow(
                    modifier = Modifier.height(tabRowHeight),
                    selectedTabIndex = 0,
                    divider = divider
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = index == 0,
                            onClick = {},
                            modifier = Modifier.height(tabRowHeight),
                            text = { Text(title) }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getBoundsInRoot()

        rule.onNodeWithTag("divider", true)
            .assertPositionInRootIsEqualTo(
                expectedLeft = 0.dp,
                expectedTop = tabRowBounds.height - PrimaryNavigationTabTokens.DividerHeight
            )
            .assertHeightIsEqualTo(PrimaryNavigationTabTokens.DividerHeight)
    }

    @Test
    fun singleLineTab_textPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = {
                                Text(title, Modifier.testTag("text"))
                            }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val expectedPositionY = (tabRowBounds.height - textBounds.height) / 2
        textBounds.top.assertIsEqualTo(expectedPositionY, "text bounds top y-position")
    }

    @Test
    fun singleLineTab_withIcon_textBaseline() {
        rule.setMaterialContent(lightColorScheme()) {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = {
                                Text(title, Modifier.testTag("text"))
                            },
                            icon = { Icon(Icons.Filled.Favorite, null) }
                        )
                    }
                }
            }
        }

        val expectedBaseline = 14.dp
        val indicatorHeight = 3.dp
        val expectedBaselineDistance = expectedBaseline + indicatorHeight

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val textBaselinePos =
            rule.onNodeWithTag("text", useUnmergedTree = true).getLastBaselinePosition()

        val baselinePositionY = textBounds.top + textBaselinePos
        val expectedPositionY = tabRowBounds.height - expectedBaselineDistance
        baselinePositionY.assertIsEqualTo(expectedPositionY, "baseline y-position")
    }

    @Test
    fun twoLineTab_textPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("Two line \n text")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = {
                                Text(title, Modifier.testTag("text"), maxLines = 2)
                            }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()

        val expectedPositionY = (tabRowBounds.height - textBounds.height) / 2
        textBounds.top.assertIsEqualTo(expectedPositionY, "text bounds top y-position")
    }

    @Test
    fun LeadingIconTab_textAndIconPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = 0
                ) {
                    LeadingIconTab(
                        selected = true,
                        onClick = {},
                        text = {
                            Text("TAB", Modifier.testTag("text"))
                        },
                        icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) }
                    )
                }
            }
        }

        val tabRowBounds =
            rule.onNodeWithTag("tabRow", useUnmergedTree = true).getUnclippedBoundsInRoot()

        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()

        val textDistanceFromIcon = 8.dp

        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        textBounds.left.assertIsEqualTo(
            iconBounds.right + textDistanceFromIcon,
            "textBounds left-position"
        )

        val iconOffset =
            (tabRowBounds.width - iconBounds.width - textBounds.width - textDistanceFromIcon) / 2
        iconBounds.left.assertIsEqualTo(iconOffset, "iconBounds left-position")
    }

    @Test
    fun scrollableTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp
        val minimumTabWidth = 90.dp

        rule.setMaterialContent(lightColorScheme()) {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB 1", "TAB 2")

            val indicator = @Composable { tabPositions: List<TabPosition> ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[state])
                        .fillMaxWidth()
                        .height(indicatorHeight)
                        .background(color = Color.Red)
                        .testTag("indicator")
                )
            }

            Box {
                ScrollableTabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state,
                    indicator = indicator
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()
        val tabRowPadding = 52.dp
        // Indicator should be placed in the bottom left of the first tab
        rule.onNodeWithTag("indicator", true)
            .assertPositionInRootIsEqualTo(
                // Tabs in a scrollable tab row are offset 52.dp from each end
                expectedLeft = tabRowPadding,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        rule.onAllNodes(isSelectable())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        rule.onNodeWithTag("indicator", true)
            .assertPositionInRootIsEqualTo(
                expectedLeft = tabRowPadding + minimumTabWidth,
                expectedTop = tabRowBounds.height - indicatorHeight
            )
    }

    @Test
    fun scrollableTabRow_dividerHeight() {
        rule.setMaterialContent(lightColorScheme()) {
            val titles = listOf("TAB 1", "TAB 2")
            val tabRowHeight = 100.dp

            val divider = @Composable { Divider(Modifier.testTag("divider")) }

            Box(Modifier.testTag("tabRow")) {
                ScrollableTabRow(
                    modifier = Modifier.height(tabRowHeight),
                    selectedTabIndex = 0,
                    divider = divider
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = index == 0,
                            onClick = {},
                            modifier = Modifier.height(tabRowHeight),
                            text = { Text(title) }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getBoundsInRoot()

        rule.onNodeWithTag("divider", true)
            .assertPositionInRootIsEqualTo(
                expectedLeft = 0.dp,
                expectedTop = tabRowBounds.height - PrimaryNavigationTabTokens.DividerHeight,
            )
            .assertHeightIsEqualTo(PrimaryNavigationTabTokens.DividerHeight)
    }

    @Test
    fun fixedTabRow_initialTabSelected() {
        rule
            .setMaterialContent(lightColorScheme()) {
                TextTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }
    }

    @Test
    fun fixedTabRow_selectNewTab() {
        rule
            .setMaterialContent(lightColorScheme()) {
                TextTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }

        // Click the last tab
        rule.onAllNodes(isSelectable())[2].performClick()

        // Now only the last tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun fixedLeadingIconTabRow_initialTabSelected() {
        rule
            .setMaterialContent(lightColorScheme()) {
                LeadingIconTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }
    }

    @Test
    fun leadingIconTabRow_selectNewTab() {
        rule
            .setMaterialContent(lightColorScheme()) {
                LeadingIconTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }

        // Click the last tab
        rule.onAllNodes(isSelectable())[2].performClick()

        // Now only the last tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun scrollableTabRow_initialTabSelected() {
        rule
            .setMaterialContent(lightColorScheme()) {
                ScrollingTextTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsNotSelected()
                }
            }
    }

    @Test
    fun scrollableTabRow_offScreenTabInitiallySelected() {
        rule
            .setMaterialContent(lightColorScheme()) {
                var state by remember { mutableStateOf(9) }
                val titles = List(10) { "Tab ${it + 1}" }
                ScrollableTabRow(selectedTabIndex = state) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

        rule.onAllNodes(isSelectable())
            .assertCountEquals(10)
            .apply {
                // The last tab should be selected and displayed (scrolled to)
                get(9)
                    .assertIsSelected()
                    .assertIsDisplayed()
            }
    }

    @Test
    fun scrollableTabRow_selectNewTab() {
        rule
            .setMaterialContent(lightColorScheme()) {
                ScrollingTextTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsNotSelected()
                }
            }

        // Click the second tab
        rule.onAllNodes(isSelectable())[1].performClick()

        // Now only the second tab should be selected
        rule.onAllNodes(isSelectable())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsSelected()
                (2..9).forEach {
                    get(it).assertIsNotSelected()
                }
            }
    }

    @Test
    fun tabRowIndicator_animatesWidthChange() {
        rule.mainClock.autoAdvance = false

        rule.setMaterialContent(lightColorScheme()) {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB 1", "TAB 2", "TAB 3 WITH LOTS OF TEXT")

            val indicator = @Composable { tabPositions: List<TabPosition> ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[state])
                        .testTag("indicator")
                )
            }

            Box {
                ScrollableTabRow(
                    selectedTabIndex = state,
                    indicator = indicator
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }

        val initialWidth = rule.onNodeWithTag("indicator").getUnclippedBoundsInRoot().width

        // Click the third tab, which is wider than the first
        rule.onAllNodes(isSelectable())[2].performClick()

        // Ensure animation starts
        rule.mainClock.advanceTimeBy(50)

        val midAnimationWidth = rule.onNodeWithTag("indicator").getUnclippedBoundsInRoot().width
        assertThat(initialWidth).isLessThan(midAnimationWidth)

        // Allow animation to complete
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        val finalWidth = rule.onNodeWithTag("indicator").getUnclippedBoundsInRoot().width
        assertThat(midAnimationWidth).isLessThan(finalWidth)
    }

    @Test
    fun testInspectorValue() {
        val pos = TabPosition(10.0.dp, 200.0.dp)
        rule.setContent {
            val modifier = Modifier.tabIndicatorOffset(pos) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("tabIndicatorOffset")
            assertThat(modifier.valueOverride).isEqualTo(pos)
            assertThat(modifier.inspectableElements.asIterable()).isEmpty()
        }
    }

    @Test
    fun disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Tab(
                    selected = true,
                    onClick = { clicks++ },
                    modifier = Modifier.testTag("tab"),
                    enabled = false,
                    text = { Text("Text") }
                )
            }
        }

        rule.onNodeWithTag("tab")
            .performClick()

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
        }
    }

    @Test
    fun leadingIconTab_disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                LeadingIconTab(
                    selected = true,
                    onClick = { clicks++ },
                    text = { Text("Text") },
                    icon = { Icon(icon, null) },
                    modifier = Modifier.testTag("tab"),
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag("tab")
            .performClick()

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
        }
    }

    @Test
    fun fontScaleChange_height() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(
                    LocalDensity provides
                        Density(
                            density = LocalDensity.current.density,
                            fontScale = 2.0f
                        )
                ) {
                    Surface {
                        Tab(
                            selected = true,
                            onClick = {},
                            text = { Text("Text") },
                            icon = { Icon(icon, null) }
                        )
                    }
                }
            }
            .assertHeightIsAtLeast(100.dp)
    }

    @Test
    fun tabRow_layoutHeightRespected() {
        var height by mutableStateOf(0.dp)
        rule
            .setMaterialContent(lightColorScheme()) {
                var state by remember { mutableStateOf(0) }
                val titles = listOf("Tab 1", "Tab 2", "Tab 3")
                Column(
                    Modifier
                        .heightIn(max = height)
                        .testTag("Tabs")
                ) {
                    TabRow(selectedTabIndex = state) {
                        titles.forEachIndexed { index, title ->
                            Tab(
                                selected = state == index,
                                onClick = { state = index },
                                text = { Text(text = title) }
                            )
                        }
                    }
                }
            }

        rule.onNodeWithTag("Tabs").assertHeightIsEqualTo(height)

        height = 40.dp
        rule.waitForIdle()

        rule.onNodeWithTag("Tabs").assertHeightIsEqualTo(height)
    }

    @Test
    fun scrollableTabRow_layoutHeightRespected() {
        var height by mutableStateOf(0.dp)
        rule
            .setMaterialContent(lightColorScheme()) {
                var state by remember { mutableStateOf(0) }
                val titles = listOf("Tab 1", "Tab 2", "Tab 3")
                Column(
                    Modifier
                        .heightIn(max = height)
                        .testTag("Tabs")
                ) {
                    ScrollableTabRow(selectedTabIndex = state) {
                        titles.forEachIndexed { index, title ->
                            Tab(
                                selected = state == index,
                                onClick = { state = index },
                                text = { Text(text = title) }
                            )
                        }
                    }
                }
            }

        rule.onNodeWithTag("Tabs").assertHeightIsEqualTo(height)

        height = 40.dp
        rule.waitForIdle()

        rule.onNodeWithTag("Tabs").assertHeightIsEqualTo(height)
    }
}
