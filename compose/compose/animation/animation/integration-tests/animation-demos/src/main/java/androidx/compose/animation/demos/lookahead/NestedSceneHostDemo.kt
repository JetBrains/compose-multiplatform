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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.animation.demos.lookahead

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NestedSceneHostDemo() {
    SceneHost {
        Box(Modifier.padding(top = 100.dp).fillMaxSize()
            .intermediateLayout { measurable, constraints, _ ->
                println("SPEC, actually measure parent")
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    println("SPEC, actually place parent")
                    placeable.place(0, 0)
                }
            }) {
            SceneHost {
                Column {
                    Box(
                        Modifier.size(100.dp).background(Color.Red)
                            .intermediateLayout { measurable, constraints, _ ->
                                println("SPEC, actually measure child")
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    println("SPEC, actually place child")
                                    placeable.place(0, 0)
                                }
                            })
                    Box(
                        Modifier.size(100.dp).background(Color.Green)
                    )
                }
            }
        }
    }
}