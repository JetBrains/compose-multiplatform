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
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.TabConstants.defaultTabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.samples.ScrollingTextTabs
import androidx.compose.material.samples.TextTabs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.filters.LargeTest
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertIsNotSelected
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.getUnclippedBoundsInRoot
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class TabTest {

    private val ExpectedSmallTabHeight = 48.dp
    private val ExpectedLargeTabHeight = 72.dp

    private val icon = Icons.Filled.Favorite

    @get:Rule
    val rule = createComposeRule()

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
                Tab(icon = { Icon(icon) }, selected = true, onClick = {})
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
                        icon = { Icon(icon) },
                        selected = true,
                        onClick = {}
                    )
                }
            }
            .assertHeightIsEqualTo(ExpectedLargeTabHeight)
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
                        .defaultTabIndicatorOffset(tabPositions[state])
                        .fillMaxWidth()
                        .preferredHeight(indicatorHeight)
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

        rule.onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                expectedLeft = 0.dp,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        rule.onAllNodes(isInMutuallyExclusiveGroup())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        rule.onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                expectedLeft = (tabRowBounds.width / 2),
                expectedTop = tabRowBounds.height - indicatorHeight
            )
    }

    @Test
    fun singleLineTab_textBaseline() {
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

        val expectedBaseline = 18.dp
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
                            icon = { Icon(Icons.Filled.Favorite) },
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
    fun twoLineTab_textBaseline() {
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

        val expectedBaseline = 10.dp
        val indicatorHeight = 2.dp

        val tabRowBounds = rule.onNodeWithTag("tabRow").getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val textBaselinePos =
            rule.onNodeWithTag("text", useUnmergedTree = true).getLastBaselinePosition()

        val expectedBaselineDistance = expectedBaseline + indicatorHeight

        val baselinePositionY = textBounds.top + textBaselinePos
        val expectedPositionY = (tabRowBounds.height - expectedBaselineDistance)
        baselinePositionY.assertIsEqualTo(expectedPositionY, "baseline y-position")
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
                        .defaultTabIndicatorOffset(tabPositions[state])
                        .fillMaxWidth()
                        .preferredHeight(indicatorHeight)
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
        rule.onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                // Tabs in a scrollable tab row are offset 52.dp from each end
                expectedLeft = TabConstants.DefaultScrollableTabRowPadding,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        rule.onAllNodes(isInMutuallyExclusiveGroup())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        rule.onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                expectedLeft = TabConstants.DefaultScrollableTabRowPadding + minimumTabWidth,
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
        rule.onAllNodes(isInMutuallyExclusiveGroup())
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
        rule.onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }

        // Click the last tab
        rule.onAllNodes(isInMutuallyExclusiveGroup())[2].performClick()

        // Now only the last tab should be selected
        rule.onAllNodes(isInMutuallyExclusiveGroup())
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
        rule.onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsNotSelected()
                }
            }
    }

    @Test
    fun scrollableTabRow_selectNewTab() {
        rule
            .setMaterialContent {
                ScrollingTextTabs()
            }

        // Only the first tab should be selected
        rule.onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsNotSelected()
                }
            }

        // Click the second tab
        rule.onAllNodes(isInMutuallyExclusiveGroup())[1].performClick()

        // Now only the second tab should be selected
        rule.onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsSelected()
                (2..9).forEach {
                    get(it).assertIsNotSelected()
                }
            }
    }
}
