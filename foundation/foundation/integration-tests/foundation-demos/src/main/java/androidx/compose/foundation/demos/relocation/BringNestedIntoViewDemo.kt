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

@file:Suppress("SameParameterValue")

package androidx.compose.foundation.demos.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BringNestedIntoViewDemo() {
    Column {
        val rows = 3
        val columns = 3
        val bringIntoViewRequesters = remember { List(rows * columns) { BringIntoViewRequester() } }

        Text(
            "This is a $rows x $columns grid of circles. The entire grid is vertically " +
                "scrollable, and each row in the grid is horizontally scrollable. Click the " +
                "buttons in the smaller grid below to bring the corresponding circle into view."
        )

        ScrollableGrid(rows, columns, bringIntoViewRequesters)
        ControlGrid(rows, columns, bringIntoViewRequesters)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollableGrid(rows: Int, columns: Int, requesters: List<BringIntoViewRequester>) {
    Column(
        Modifier
            .border(3.dp, Color.Blue)
            .size(200.dp, 250.dp)
            .verticalScroll(rememberScrollState())
    ) {
        repeat(rows) { row ->
            Row(
                Modifier
                    // Inner scrollable rows.
                    .border(3.dp, Color.Green)
                    .fillMaxWidth()
                    .height(200.dp)
                    .horizontalScroll(rememberScrollState())
                    .width(600.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circles
                repeat(columns) { column ->
                    val index = row * columns + column
                    TextCircle(
                        index.toString(),
                        Modifier
                            .size(75.dp)
                            .bringIntoViewRequester(requesters[index])
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ControlGrid(rows: Int, columns: Int, requesters: List<BringIntoViewRequester>) {
    val coroutineScope = rememberCoroutineScope()
    Column {
        repeat(rows) { row ->
            Row {
                repeat(columns) { column ->
                    val requester = requesters[row * columns + column]
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                requester.bringIntoView()
                            }
                        }
                    ) {
                        val index = row * columns + column
                        TextCircle(
                            index.toString(),
                            Modifier.size(50.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextCircle(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .background(Color.Red, shape = CircleShape)
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}