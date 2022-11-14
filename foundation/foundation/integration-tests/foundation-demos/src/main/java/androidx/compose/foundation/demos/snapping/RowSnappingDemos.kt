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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val RowSnappingDemos = listOf(
    ComposableDemo("Single Page - Same Size Pages") { SinglePageSnapping() },
    ComposableDemo("Multi Page - Animation Based Offset") { MultiPageSnappingDemo() },
    ComposableDemo("Multi Page - View Port Based Offset") { ViewPortBasedSnappingDemo() },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SinglePageSnapping() {
    val scrollState = rememberScrollState()
    val layoutSizeState = remember { mutableStateOf(IntSize.Zero) }
    val layoutInfoProvider = rememberRowSnapLayoutInfoProvider(scrollState) {
        layoutSizeState.value.width.toFloat()
    }
    val snapFlingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfoProvider)
    RowSnappingMainLayout(snapFlingBehavior, scrollState) { layoutSizeState.value = it }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiPageSnappingDemo() {
    val scrollState = rememberScrollState()
    val layoutSizeState = remember { mutableStateOf(IntSize.Zero) }
    val layoutInfoProvider = rememberMultiPageRowSnapLayoutInfoProvider(scrollState) {
        layoutSizeState.value.width.toFloat()
    }
    val snapFlingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfoProvider)
    RowSnappingMainLayout(snapFlingBehavior, scrollState) { layoutSizeState.value = it }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViewPortBasedSnappingDemo() {
    val scrollState = rememberScrollState()
    val layoutSizeState = remember { mutableStateOf(IntSize.Zero) }
    val layoutInfoProvider = rememberViewPortRowSnapLayoutInfoProvider(scrollState) {
        layoutSizeState.value.width.toFloat()
    }
    val snapFlingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfoProvider)
    RowSnappingMainLayout(snapFlingBehavior, scrollState) { layoutSizeState.value = it }
}

@Composable
private fun RowSnappingMainLayout(
    snapFlingBehavior: FlingBehavior,
    scrollState: ScrollState,
    onLayoutSizeChanged: (IntSize) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                drawAnchor(CenterAnchor)
            }, contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged(onLayoutSizeChanged)
                .fillMaxHeight(0.7f)
                .horizontalScroll(scrollState, flingBehavior = snapFlingBehavior)
        ) {
            repeat(100) {
                RowSnappingItem(position = it)
            }
        }
    }
}

@Composable
private fun RowSnappingItem(position: Int) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(RowItemSize)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberRowSnapLayoutInfoProvider(
    scrollState: ScrollState,
    layoutSize: Density.() -> Float
): SnapLayoutInfoProvider {
    return remember(scrollState, layoutSize) {
        SnapLayoutInfoProvider(
            scrollState = scrollState,
            itemSize = { RowItemSize.toPx() },
            layoutSize = layoutSize
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberMultiPageRowSnapLayoutInfoProvider(
    scrollState: ScrollState,
    layoutSize: Density.() -> Float
): SnapLayoutInfoProvider {
    val animation: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(scrollState, layoutSize) {
        MultiPageSnappingLayoutInfoProvider(
            SnapLayoutInfoProvider(
                scrollState = scrollState,
                itemSize = { RowItemSize.toPx() },
                layoutSize = layoutSize
            ),
            animation
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberViewPortRowSnapLayoutInfoProvider(
    scrollState: ScrollState,
    layoutSize: Density.() -> Float
): SnapLayoutInfoProvider {
    val density = LocalDensity.current
    val decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(scrollState, density, layoutSize) {
        ViewPortBasedSnappingLayoutInfoProvider(
            SnapLayoutInfoProvider(
                scrollState = scrollState,
                itemSize = { RowItemSize.toPx() },
                layoutSize = layoutSize
            ),
            decayAnimationSpec
        ) { with(density, layoutSize) }
    }
}

private val RowItemSize = 250.dp