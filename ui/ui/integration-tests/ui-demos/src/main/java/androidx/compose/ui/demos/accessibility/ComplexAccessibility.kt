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

package androidx.compose.ui.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.DrawerValue
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LastElementOverLaidColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var yPosition = 0

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                if (placeable != placeables[placeables.lastIndex]) {
                    placeable.placeRelative(x = 0, y = yPosition)
                    yPosition += placeable.height
                } else {
                    // if the element is our last element (our overlaid node)
                    // then we'll put it over the middle of our previous elements
                    placeable.placeRelative(x = 0, y = yPosition / 2)
                }
            }
        }
    }
}

@Preview
@Composable
fun OverlaidNodeLayoutDemo() {
    LastElementOverLaidColumn(modifier = Modifier.padding(8.dp)) {
        Row {
            Column(modifier = Modifier.testTag("Text1")) {
                Row { Text("text1\n") }
                Row { Text("text2\n") }
                Row { Text("text3\n") }
            }
        }
        Row {
            Text("overlaid node")
        }
    }
}

@Composable
fun CardRow(
    modifier: Modifier,
    columnNumber: Int,
    topSampleText: String,
    bottomSampleText: String
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column {
            Text(topSampleText + columnNumber)
            Text(bottomSampleText + columnNumber)
        }
    }
}

@Preview
@Composable
fun NestedContainersFalseDemo() {
    var topSampleText = "Top text in column "
    var bottomSampleText = "Bottom text in column "
    Column(
        Modifier
            .testTag("Test Tag")
            .semantics { isContainer = true }
    ) {
        Row() { Modifier.semantics { isContainer = true }
            CardRow(
                Modifier.semantics { isContainer = false },
                1,
                topSampleText,
                bottomSampleText)
            CardRow(
                Modifier.semantics { isContainer = false },
                2,
                topSampleText,
                bottomSampleText)
        }
    }
}

@Preview
@Composable
fun NestedContainersTrueDemo() {
    var topSampleText = "Top text in column "
    var bottomSampleText = "Bottom text in column "
    Column(
        Modifier
            .testTag("Test Tag")
            .semantics { isContainer = true }
    ) {
        Row() { Modifier.semantics { isContainer = true }
            CardRow(
                Modifier.semantics { isContainer = true },
                1,
                topSampleText,
                bottomSampleText)
            CardRow(
                Modifier.semantics { isContainer = true },
                2,
                topSampleText,
                bottomSampleText)
        }
    }
}

@Composable
fun TopAppBar() {
    val topAppBar = "Top App Bar"
    TopAppBar(
        title = {
            Text(text = topAppBar)
        }
    )
}

@Composable
fun ScrollColumn(padding: PaddingValues) {
    var counter = 0
    var sampleText = "Sample text in column"
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .testTag("Test Tag")
    ) {
        repeat(100) {
            Text(sampleText + counter++)
        }
    }
}

@Preview
@Composable
fun ScaffoldSample() {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { TopAppBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = { FloatingActionButton(onClick = {}) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "fab icon")
        } },
        drawerContent = { Text(text = "Drawer Menu 1") },
        content = { padding -> Text("Content", modifier = Modifier.padding(padding)) },
        bottomBar = { BottomAppBar(backgroundColor = MaterialTheme.colors.primary) {
            Text("Bottom App Bar") } }
    )
}

@Preview
@Composable
fun ScaffoldSampleScroll() {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { TopAppBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = { FloatingActionButton(onClick = {}) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "fab icon")
        } },
        content = { padding -> ScrollColumn(padding) },
        bottomBar = { BottomAppBar(backgroundColor = MaterialTheme.colors.primary) {
            Text("Bottom App Bar") } }
    )
}

@Preview
@Composable
fun ScrollingColumnDemo() {
    var sampleText = "Sample text in column"
    var counter = 0

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .testTag("Test Tag")
    ) {
        TopAppBar()
        repeat(100) {
            Text(sampleText + counter++)
        }
    }
}
