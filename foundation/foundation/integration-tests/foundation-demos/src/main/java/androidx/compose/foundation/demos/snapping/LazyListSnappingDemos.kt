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

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

val LazyListSnappingDemos = listOf(
    ComposableDemo("Single Page - Same Size Pages") { SamePageSizeDemo() },
    ComposableDemo("Single Page - Multi-Size Pages") { MultiSizePageDemo() },
    ComposableDemo("Single Page - Large Pages") { LargePageSizeDemo() },
    ComposableDemo("Single Page - List with Content padding") { DifferentContentPaddingDemo() },
    ComposableDemo("Multi Page - Animation Based Offset") { MultiPageSnappingDemo() },
    ComposableDemo("Multi Page - View Port Based Offset") { ViewPortBasedSnappingDemo() },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SamePageSizeDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = remember(lazyListState) {
        SnapLayoutInfoProvider(lazyListState)
    }
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior
    ) {
        DefaultSnapDemoItem(it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiSizePageDemo() {
    val lazyListState = rememberLazyListState()
    val snapLayoutInfoProvider =
        remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        ResizableSnapDemoItem(
            width = PagesSizes[it],
            height = 500.dp,
            position = it
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LargePageSizeDemo() {
    val lazyListState = rememberLazyListState()
    val snappingLayout = remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        ResizableSnapDemoItem(
            width = 350.dp,
            height = 500.dp,
            position = it
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DifferentContentPaddingDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider =
        remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior,
        contentPaddingValues = PaddingValues(start = 20.dp, end = 50.dp)
    ) {
        DefaultSnapDemoItem(position = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiPageSnappingDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberMultiPageSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)
    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViewPortBasedSnappingDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberViewPortSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberMultiPageSnappingLayoutInfoProvider(
    state: LazyListState
): SnapLayoutInfoProvider {
    val animation: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(state) {
        MultiPageSnappingLayoutInfoProvider(
            SnapLayoutInfoProvider(lazyListState = state),
            animation
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberViewPortSnappingLayoutInfoProvider(
    state: LazyListState
): SnapLayoutInfoProvider {
    return remember(state) {
        ViewPortBasedSnappingLayoutInfoProvider(
            SnapLayoutInfoProvider(lazyListState = state)
        ) { state.layoutInfo.visibleItemsInfo.sumOf { it.size }.toFloat() }
    }
}

private val PagesSizes = (0..ItemNumber).toList().map { (50..500).random().dp }