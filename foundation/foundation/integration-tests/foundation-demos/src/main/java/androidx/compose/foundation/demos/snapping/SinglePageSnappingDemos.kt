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
import androidx.compose.foundation.gestures.snapping.lazyListSnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SinglePageSnappingDemos = listOf(
    ComposableDemo("Same Size Pages") { SamePageSizeDemo() },
    ComposableDemo("Multi-Size Pages") { MultiSizePageDemo() },
    ComposableDemo("Large Pages") { LargePageSizeDemo() },
    ComposableDemo("List with Content padding") { DifferentContentPaddingDemo() },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SamePageSizeDemo() {
    val lazyListState = rememberLazyListState()
    val snappingLayout = remember(lazyListState) { lazyListSnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior
    ) {
        DefaultSnapDemoItem(it)
    }
}

@Composable
fun ResizableSnapDemoItem(modifier: Modifier, position: Int) {
    val innerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .then(modifier)
            .padding(8.dp)
            .background(Color.White)
            .onPlaced { innerCoordinates.value = it }
            .drawWithContent {
                drawContent()
                drawAnchor(innerCoordinates.value, CenterAnchor)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = position.toString(), fontSize = 40.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LargePageSizeDemo() {
    val lazyListState = rememberLazyListState()
    val snappingLayout = remember(lazyListState) { lazyListSnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        ResizableSnapDemoItem(
            modifier = Modifier
                .width(350.dp)
                .height(500.dp),
            position = it
        )
    }
}

private val PagesSizes = (0..ItemNumber).toList().map { (50..500).random().dp }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiSizePageDemo() {
    val lazyListState = rememberLazyListState()
    val snappingLayout = remember(lazyListState) { lazyListSnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        ResizableSnapDemoItem(
            modifier = Modifier
                .width(PagesSizes[it])
                .height(500.dp),
            position = it
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DifferentContentPaddingDemo() {
    val lazyListState = rememberLazyListState()
    val snappingLayout = remember(lazyListState) { lazyListSnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior,
        contentPaddingValues = PaddingValues(start = 20.dp, end = 50.dp)
    ) {
        DefaultSnapDemoItem(position = it)
    }
}
