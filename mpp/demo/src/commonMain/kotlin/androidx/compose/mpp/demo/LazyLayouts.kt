/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

val LazyLayouts = Screen.List(
    "LazyLayouts",
    Screen.Example("LazyColumn") { ExampleLazyColumn() },
    Screen.Example("LazyGrid") { ExampleLazyGrid() },
    Screen.Example("StaggeredGrid") { ExampleStaggeredGrid() },
)

@Composable
private fun ExampleLazyColumn() {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { }
            state.scrollBy(2f)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = state) {
        items(100) {
            Box(Modifier.size(100.dp).background(remember { Color(Random.nextInt()) }))
        }
    }
}

@Composable
private fun ExampleLazyGrid() {
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize()) {
        items(100) {
            Box(
                Modifier.fillMaxWidth()
                    .aspectRatio(1f)
                    .background(remember { Color(Random.nextInt()) })
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExampleStaggeredGrid() {
    LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(3), Modifier.fillMaxSize()) {
        items(100) {
            Box(
                Modifier.fillMaxSize()
                    .height(remember { Random.nextInt(100, 200).dp })
                    .background(remember { Color(Random.nextInt()) })
            )
        }
    }
}
