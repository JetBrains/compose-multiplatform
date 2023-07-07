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

package androidx.compose.animation.demos.visualinspection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Right
import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Up
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Preview
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideInContentVariedSizes() {
    Column {
        var contentAlignment by remember { mutableStateOf(Alignment.TopStart) }
        var clip by remember { mutableStateOf(true) }
        var horizontal by remember { mutableStateOf(true) }
        AlignmentMenu(contentAlignment, { contentAlignment = it })
        Row(Modifier.clickable { clip = !clip }.padding(20.dp)) {
            Checkbox(clip, { clip = it })
            Text("Clip")
        }

        Row(Modifier.clickable { horizontal = !horizontal }.padding(20.dp)) {
            Checkbox(horizontal, { horizontal = it })
            Text("Slide horizontally")
        }

        var contentState by remember { mutableStateOf(PaneState.Pane1) }
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                contentState,
                modifier = Modifier.padding(top = 120.dp, start = 100.dp)
                    .border(3.dp, Color(0xff79e9de)),
                contentAlignment = contentAlignment,
                transitionSpec = {
                    if (targetState < initialState) {
                        if (horizontal) {
                            slideIntoContainer(towards = Right) with slideOutOfContainer(
                                towards = Right
                            )
                        } else {
                            slideIntoContainer(towards = Down) with slideOutOfContainer(
                                towards = Down
                            )
                        }
                    } else {
                        if (horizontal) {
                            slideIntoContainer(towards = Left).with(
                                slideOutOfContainer(towards = Left)
                            )
                        } else {
                            slideIntoContainer(towards = Up).with(
                                slideOutOfContainer(towards = Up)
                            )
                        }
                    }.using(SizeTransform(clip = clip)).apply {
                        targetContentZIndex = when (targetState) {
                            PaneState.Pane1 -> 1f
                            PaneState.Pane2 -> 2f
                            PaneState.Pane3 -> 3f
                        }
                    }
                }
            ) {
                when (it) {
                    PaneState.Pane1 -> Pane1()
                    PaneState.Pane2 -> Pane2()
                    PaneState.Pane3 -> Pane3()
                }
            }
            Row(
                Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Default.ArrowBack, contentDescription = null,
                    Modifier.clickable {
                        contentState = PaneState.values()[max(0, contentState.ordinal - 1)]
                    }.padding(top = 300.dp, bottom = 300.dp, end = 60.dp)
                )
                Icon(
                    Icons.Default.ArrowForward, contentDescription = null,
                    Modifier.clickable {
                        contentState = PaneState.values()[min(2, contentState.ordinal + 1)]
                    }.padding(top = 300.dp, bottom = 300.dp, start = 60.dp)
                )
            }
        }
    }
}

@Composable
fun AlignmentMenu(contentAlignment: Alignment, onAlignmentChanged: (alignment: Alignment) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text("Current alignment: $contentAlignment")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.TopStart) }) {
                Text("TopStart")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.TopEnd) }) {
                Text("TopEnd")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.BottomStart) }) {
                Text("BottomStart")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.BottomEnd) }) {
                Text("BottomEnd")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.Center) }) {
                Text("Center")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.TopCenter) }) {
                Text("TopCenter")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.BottomCenter) }) {
                Text("BottomCenter")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.CenterStart) }) {
                Text("CenterStart")
            }
            DropdownMenuItem(onClick = { onAlignmentChanged(Alignment.CenterEnd) }) {
                Text("CenterEnd")
            }
        }
    }
}

@Composable
fun Pane1() {
    Column(Modifier.background(Color(0xFFe3ffd9)).padding(10.dp)) {
        for (id in 1..4) {
            Text("Range from ${(id - 1) * 10} to ${id * 10 - 1}:", fontSize = 20.sp)
        }
    }
}

@Composable
fun Pane3() {
    Column(Modifier.background(Color(0xFFffe9d6)).padding(10.dp)) {
        for (id in 1..10) {
            Text("Line #$id ", fontSize = 20.sp)
        }
    }
}

@Composable
fun Pane2() {
    Column(Modifier.background(Color(0xFFfffbd0)).padding(10.dp)) {
        Text("Yes", fontSize = 20.sp)
        Text("No", fontSize = 20.sp)
    }
}

private enum class PaneState {
    Pane1, Pane2, Pane3
}
