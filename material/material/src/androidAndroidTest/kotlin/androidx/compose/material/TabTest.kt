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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.samples.LeadingIconTabs
import androidx.compose.material.samples.ScrollingTextTabs
import androidx.compose.material.samples.TextTabs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
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
        rule.setMaterialContent {
            TabRow(0) {
                Tab(
                    text = { Text("Text") },
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
    fun disabledSemantics() {
        rule.setMaterialContent {
            Box {
                Tab(
                    enabled = false,
                    text = { Text("Text") },
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

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun leadingIconTab_defaultSemantics() {
        rule.setMaterialContent {
            TabRow(0) {
                LeadingIconTab(
                    text = { Text("Text") },
                    icon = { Icon(icon, null) },
                    modifier = Modifier.testTag("leadingIconTab"),
                    selected = true,
                    onClick = {}
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

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun leadingIconTab_disabledSemantics() {
        rule.setMaterialContent {
            Box {
                LeadingIconTab(
                    enabled = false,
                    text = { Text("Text") },
                    icon = { Icon(icon, null) },
                    modifier = Modifier.testTag("leadingIconTab"),
                    selected = true,
                    onClick = {}
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
                Tab(text = { Text("Text") }, selected = true, onClick = {})
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun iconTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Tab(icon = { Icon(icon, null) }, selected = true, onClick = {})
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun textAndIconTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Surface {
                    Tab(
                        text = { Text("Text and Icon") },
                        icon = { Icon(icon, null) },
                        selected = true,
                        onClick = {}
                    )
                }
            }
            .assertHeightIsEqualTo(ExpectedLargeTabHeight)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun leadingIconTab_height() {
        rule
            .setMaterialContentForSizeAssertions {
                Surface {
                    LeadingIconTab(
                        text = { Text("Text") },
                        icon = { Icon(icon, null) },
                        selected = true,
                        onClick = {}
                    )
                }
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun fixedTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp

        rule.setMaterialContent {
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
                            text = { Text(title) },
                            selected = state == index,
                            onClick = { state = index }
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
    fun singleLineTab_textPosition() {
        rule.setMaterialContent {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            text = {
                                Text(title, Modifier.testTag("text"))
                            },
                            selected = state == index,
                            onClick = { state = index }
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
        rule.setMaterialContent {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            text = {
                                Text(title, Modifier.testTag("text"))
                            },
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            selected = state == index,
                            onClick = { state = index }
                        )
                    }
                }
            }
        }

        val expectedBaseline = 14.dp
        val indicatorHeight = 2.dp
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
        rule.setMaterialContent {
            var state by remember { mutableStateOf(0) }
            val titles = listOf("Two line \n text")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = state
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            text = {
                                Text(title, Modifier.testTag("text"), maxLines = 2)
                            },
                            selected = state == index,
                            onClick = { state = index }
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

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun LeadingIconTab_textAndIconPosition() {
        rule.setMaterialContent {
            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    selectedTabIndex = 0
                ) {
                    LeadingIconTab(
                        text = {
                            Text("TAB", Modifier.testTag("text"))
                        },
                        icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                        selected = true,
                        onClick = {}
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

        rule.setMaterialContent {
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
                            text = { Text(title) },
                            selected = state == index,
                            onClick = { state = index }
                        )
                    }
                }
            }
        }

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()

        // Indicator should be placed in the bottom left of the first tab
        rule.onNodeWithTag("indicator", true)
            .assertPositionInRootIsEqualTo(
                // Tabs in a scrollable tab row are offset 52.dp from each end
                expectedLeft = TabRowDefaults.ScrollableTabRowPadding,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        rule.onAllNodes(isSelectable())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        rule.onNodeWithTag("indicator", true)
            .assertPositionInRootIsEqualTo(
                expectedLeft = TabRowDefaults.ScrollableTabRowPadding + minimumTabWidth,
                expectedTop = tabRowBounds.height - indicatorHeight
            )
    }

    @Test
    fun fixedTabRow_initialTabSelected() {
        rule
            .setMaterialContent {
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
            .setMaterialContent {
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
            .setMaterialContent {
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
    fun LeadingIconTabRow_selectNewTab() {
        rule
            .setMaterialContent {
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
            .setMaterialContent {
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
            .setMaterialContent {
                var state by remember { mutableStateOf(9) }
                val titles = List(10) { "Tab ${it + 1}" }
                ScrollableTabRow(selectedTabIndex = state) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = state == index,
                            onClick = { state = index }
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
            .setMaterialContent {
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
        rule.setMaterialContent {
            Box {
                Tab(
                    enabled = false,
                    text = { Text("Text") },
                    modifier = Modifier.testTag("tab"),
                    selected = true,
                    onClick = { clicks++ }
                )
            }
        }

        rule.onNodeWithTag("tab")
            .performClick()

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun leadingIconTab_disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent {
            Box {
                LeadingIconTab(
                    enabled = false,
                    text = { Text("Text") },
                    icon = { Icon(icon, null) },
                    modifier = Modifier.testTag("tab"),
                    selected = true,
                    onClick = { clicks++ }
                )
            }
        }

        rule.onNodeWithTag("tab")
            .performClick()

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
        }
    }
}
