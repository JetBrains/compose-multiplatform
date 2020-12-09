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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.DpPropKey
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.transition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabDefaults.tabIndicatorOffset
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun TextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("TAB 1", "TAB 2", "TAB 3 WITH LOTS OF TEXT")
    Column {
        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = state == index,
                    onClick = { state = index }
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Text tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun IconTabs() {
    var state by remember { mutableStateOf(0) }
    val icons = listOf(Icons.Filled.Favorite, Icons.Filled.Favorite, Icons.Filled.Favorite)
    Column {
        TabRow(selectedTabIndex = state) {
            icons.forEachIndexed { index, icon ->
                Tab(
                    icon = { Icon(icon) },
                    selected = state == index,
                    onClick = { state = index }
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Icon tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun TextAndIconTabs() {
    var state by remember { mutableStateOf(0) }
    val titlesAndIcons = listOf(
        "TAB 1" to Icons.Filled.Favorite,
        "TAB 2" to Icons.Filled.Favorite,
        "TAB 3 WITH LOTS OF TEXT" to Icons.Filled.Favorite
    )
    Column {
        TabRow(selectedTabIndex = state) {
            titlesAndIcons.forEachIndexed { index, (title, icon) ->
                Tab(
                    text = { Text(title) },
                    icon = { Icon(icon) },
                    selected = state == index,
                    onClick = { state = index }
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Text and icon tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun ScrollingTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf(
        "TAB 1",
        "TAB 2",
        "TAB 3 WITH LOTS OF TEXT",
        "TAB 4",
        "TAB 5",
        "TAB 6 WITH LOTS OF TEXT",
        "TAB 7",
        "TAB 8",
        "TAB 9 WITH LOTS OF TEXT",
        "TAB 10"
    )
    Column {
        ScrollableTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = state == index,
                    onClick = { state = index }
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling text tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Sampled
@Composable
fun FancyTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("TAB 1", "TAB 2", "TAB 3")
    Column {
        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                FancyTab(title = title, onClick = { state = index }, selected = (index == state))
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Sampled
@Composable
fun FancyIndicatorTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("TAB 1", "TAB 2", "TAB 3")

    // Reuse the default offset animation modifier, but use our own indicator
    val indicator = @Composable { tabPositions: List<TabPosition> ->
        FancyIndicator(Color.White, Modifier.tabIndicatorOffset(tabPositions[state]))
    }

    Column {
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
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy indicator tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Sampled
@Composable
fun FancyIndicatorContainerTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("TAB 1", "TAB 2", "TAB 3")

    val indicator = @Composable { tabPositions: List<TabPosition> ->
        FancyAnimatedIndicator(tabPositions = tabPositions, selectedTabIndex = state)
    }

    Column {
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
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy transition tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun ScrollingFancyIndicatorContainerTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf(
        "TAB 1",
        "TAB 2",
        "TAB 3 WITH LOTS OF TEXT",
        "TAB 4",
        "TAB 5",
        "TAB 6 WITH LOTS OF TEXT",
        "TAB 7",
        "TAB 8",
        "TAB 9 WITH LOTS OF TEXT",
        "TAB 10"
    )
    val indicator = @Composable { tabPositions: List<TabPosition> ->
        FancyAnimatedIndicator(tabPositions = tabPositions, selectedTabIndex = state)
    }

    Column {
        ScrollableTabRow(
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
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling fancy transition tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Sampled
@Composable
fun FancyTab(title: String, onClick: () -> Unit, selected: Boolean) {
    Tab(selected, onClick) {
        Column(
            Modifier.padding(10.dp).preferredHeight(50.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.preferredSize(10.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(color = if (selected) Color.Red else Color.White)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Sampled
@Composable
fun FancyIndicator(color: Color, modifier: Modifier = Modifier) {
    // Draws a rounded rectangular with border around the Tab, with a 5.dp padding from the edges
    // Color is passed in as a parameter [color]
    Box(
        modifier
            .padding(5.dp)
            .fillMaxSize()
            .border(BorderStroke(2.dp, color), RoundedCornerShape(5.dp))
    )
}

@Sampled
@Composable
fun FancyAnimatedIndicator(tabPositions: List<TabPosition>, selectedTabIndex: Int) {
    val indicatorStart = remember { DpPropKey() }
    val indicatorEnd = remember { DpPropKey() }
    val indicatorColor = remember { ColorPropKey() }

    val colors = listOf(Color.Yellow, Color.Red, Color.Green)
    val transitionDefinition = remember(tabPositions) {
        transitionDefinition<Int> {
            tabPositions.forEachIndexed { index, position ->
                state(index) {
                    this[indicatorStart] = position.left
                    this[indicatorEnd] = position.right
                    this[indicatorColor] = colors[index % colors.size]
                }
            }
            repeat(tabPositions.size) { from ->
                repeat(tabPositions.size) { to ->
                    if (from != to) {
                        transition(fromState = from, toState = to) {
                            // Handle directionality here, if we are moving to the right, we
                            // want the right side of the indicator to move faster, if we are
                            // moving to the left, we want the left side to move faster.
                            val startStiffness = if (from < to) 50f else 1000f
                            val endStiffness = if (from < to) 1000f else 50f
                            indicatorStart using spring(
                                dampingRatio = 1f,
                                stiffness = startStiffness
                            )
                            indicatorEnd using spring(
                                dampingRatio = 1f,
                                stiffness = endStiffness
                            )
                        }
                    }
                }
            }
        }
    }

    val state = transition(transitionDefinition, selectedTabIndex)
    val offset = state[indicatorStart]
    val width = state[indicatorEnd] - state[indicatorStart]

    FancyIndicator(
        // Pass the current color to the indicator
        state[indicatorColor],
        modifier = Modifier
            // Fill up the entire TabRow, and place the indicator at the start
            .fillMaxSize()
            .wrapContentSize(align = Alignment.BottomStart)
            // Apply an offset from the start to correctly position the indicator around the tab
            .offset(x = offset)
            // Make the width of the indicator follow the animated width as we move between tabs
            .preferredWidth(width)
    )
}
