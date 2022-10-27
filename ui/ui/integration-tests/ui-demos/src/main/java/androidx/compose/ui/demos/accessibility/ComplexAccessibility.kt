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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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
            Column {
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
