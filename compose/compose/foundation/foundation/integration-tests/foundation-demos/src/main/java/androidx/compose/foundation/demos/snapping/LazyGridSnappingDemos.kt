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

package androidx.compose.foundation.demos.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LazyGridSnappingDemos = listOf(
    ComposableDemo("Single Page - Same Size Pages") { GridSinglePageSnapping() },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridSinglePageSnapping() {
    val lazyGridState = rememberLazyGridState()
    val snappingLayout = remember(lazyGridState) { SnapLayoutInfoProvider(lazyGridState) }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = snappingLayout)
    GridSnappingDemoMainLayout(flingBehavior, lazyGridState)
}

@Composable
private fun GridSnappingDemoMainLayout(flingBehavior: FlingBehavior, lazyGridState: LazyGridState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                drawAnchor(CenterAnchor)
            }, contentAlignment = Alignment.Center
    ) {
        LazyHorizontalGrid(
            modifier = Modifier
                .fillMaxHeight(0.7f),
            rows = GridCells.Fixed(3),
            state = lazyGridState,
            flingBehavior = flingBehavior
        ) {
            items(100) {
                GridSinglePageSnappingItem(it)
            }
        }
    }
}

@Composable
private fun GridSinglePageSnappingItem(position: Int) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .background(Color.Gray)
            .drawWithContent {
                drawContent()
                drawAnchor(CenterAnchor)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = position.toString(), fontSize = 40.sp)
    }
}
