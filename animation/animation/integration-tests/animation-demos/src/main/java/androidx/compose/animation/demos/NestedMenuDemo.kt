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

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope.SlideDirection
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Preview
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NestedMenuDemo() {
    var nestedMenuState by remember { mutableStateOf(NestedMenuState.Level1) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = { nestedMenuState = nestedMenuState.previous() },
                enabled = nestedMenuState != NestedMenuState.Level1
            ) {
                Text("Previous")
            }
            Button(
                onClick = { nestedMenuState = nestedMenuState.next() },
                enabled = nestedMenuState != NestedMenuState.Level3
            ) {
                Text("Next")
            }
        }
        AnimatedContent(
            targetState = nestedMenuState,
            modifier = Modifier.background(menuColor),
            transitionSpec = {
                if (initialState < targetState) {
                    // Going from parent menu to child menu, slide towards left
                    slideIntoContainer(towards = SlideDirection.Left) with
                        slideOutOfContainer(
                            towards = SlideDirection.Left,
                            targetOffset = { offsetForFullSlide -> offsetForFullSlide / 2 }
                        )
                } else {
                    // Going from child menu to parent menu, slide towards right
                    slideIntoContainer(
                        towards = SlideDirection.Right,
                        initialOffset = { offsetForFullSlide -> offsetForFullSlide / 2 }
                    ) with
                        slideOutOfContainer(towards = SlideDirection.Right)
                }.apply {
                    targetContentZIndex = when (targetState) {
                        NestedMenuState.Level1 -> 1f
                        NestedMenuState.Level2 -> 2f
                        NestedMenuState.Level3 -> 3f
                    }
                }
            }
        ) {
            when (it) {
                NestedMenuState.Level1 -> MenuLevel1()
                NestedMenuState.Level2 -> MenuLevel2()
                NestedMenuState.Level3 -> MenuLevel3()
            }
        }
    }
}

private enum class NestedMenuState { Level1, Level2, Level3 }

private fun NestedMenuState.next(): NestedMenuState =
    NestedMenuState.values()[min(this.ordinal + 1, 2)]

private fun NestedMenuState.previous(): NestedMenuState =
    NestedMenuState.values()[max(this.ordinal - 1, 0)]

@Composable
fun MenuLevel1() {
    Box(Modifier.size(100.dp, 200.dp).border(2.dp, turquoiseColors[0]).background(menuColor)) {
        Text("Menu\nLevel 1", Modifier.align(Alignment.Center))
    }
}

@Composable
fun MenuLevel2() {
    Box(Modifier.size(60.dp, 100.dp).border(2.dp, turquoiseColors[1]).background(menuColor)) {
        Text("Menu\nLevel 2", Modifier.align(Alignment.Center))
    }
}

@Composable
fun MenuLevel3() {
    Box(Modifier.size(300.dp, 240.dp).border(2.dp, turquoiseColors[2]).background(menuColor)) {
        Text("Menu\nLevel 3", Modifier.align(Alignment.Center))
    }
}

private val menuColor = turquoiseColors[4]