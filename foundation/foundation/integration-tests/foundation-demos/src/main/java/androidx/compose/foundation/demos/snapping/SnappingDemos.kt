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
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SnappingDemos = listOf(
    DemoCategory("Single Page Snapping", SinglePageSnappingDemos),
    ComposableDemo("Multi Page Snapping") { MultiPageDemo() },
    ComposableDemo("View Port Based Snapping") { ViewPortBasedDemo() },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiPageDemo() {
    val lazyListState = rememberLazyListState()
    val animation: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    val snappingLayout = remember(lazyListState) {
        MultiPageSnappingLayoutInfoProvider(
            animation,
            SnapLayoutInfoProvider(lazyListState = lazyListState)
        )
    }

    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)
    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
class MultiPageSnappingLayoutInfoProvider(
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val lazySnapLayoutInfoProvider: SnapLayoutInfoProvider
) : SnapLayoutInfoProvider by lazySnapLayoutInfoProvider {
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        return decayAnimationSpec.calculateTargetValue(0f, initialVelocity) / 2f
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewPortBasedDemo() {
    val lazyListState = rememberLazyListState()
    val snappingLayout = remember(lazyListState) {
        ViewPortBasedSnappingLayoutInfoProvider(
            SnapLayoutInfoProvider(lazyListState = lazyListState),
            lazyListState
        )
    }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
class ViewPortBasedSnappingLayoutInfoProvider(
    private val lazySnapLayoutInfoProvider: SnapLayoutInfoProvider,
    private val lazyLayoutState: LazyListState
) : SnapLayoutInfoProvider by lazySnapLayoutInfoProvider {
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        return lazyLayoutState.layoutInfo.visibleItemsInfo.sumOf { it.size }.toFloat()
    }
}

@Composable
fun SnappingDemoMainLayout(
    lazyListState: LazyListState,
    flingBehavior: FlingBehavior,
    contentPaddingValues: PaddingValues = PaddingValues(8.dp),
    content: @Composable (Int) -> Unit
) {
    val layoutCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .onPlaced { layoutCoordinates.value = it }
            .background(Color.LightGray)
            .drawWithContent {
                drawContent()
                drawAnchor(
                    layoutCoordinates.value,
                    CenterAnchor,
                    contentPaddingValues,
                    true,
                    4.0f,
                    4.0f
                )
            },
        contentPadding = contentPaddingValues,
        verticalAlignment = Alignment.CenterVertically,
        state = lazyListState,
        flingBehavior = flingBehavior
    ) {
        items(ItemNumber) {
            content(it)
        }
    }
}

@Composable
fun DefaultSnapDemoItem(position: Int) {
    val innerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(500.dp)
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

internal fun ContentDrawScope.drawAnchor(
    layoutCoordinates: LayoutCoordinates?,
    anchor: Float,
    contentPaddingValues: PaddingValues = PaddingValues(0.dp),
    shouldDrawPadding: Boolean = false,
    mainLineStrokeWidth: Float = Stroke.HairlineWidth,
    paddingLineStrokeWidth: Float = Stroke.HairlineWidth
) {
    val beforePadding = contentPaddingValues.calculateStartPadding(LayoutDirection.Rtl).toPx()
    val afterPadding = contentPaddingValues.calculateEndPadding(LayoutDirection.Rtl).toPx()

    layoutCoordinates?.let {
        val center = (it.size.width - beforePadding - afterPadding) * anchor + beforePadding

        drawLine(
            Color.Red,
            start = Offset(center, 0f),
            end = Offset(center, it.size.height.toFloat()),
            strokeWidth = mainLineStrokeWidth
        )

        if (shouldDrawPadding) {
            drawLine(
                Color.Magenta,
                start = Offset(beforePadding, 0f),
                end = Offset(beforePadding, it.size.height.toFloat()),
                strokeWidth = paddingLineStrokeWidth
            )

            drawLine(
                Color.Magenta,
                start = Offset(it.size.width - afterPadding, 0f),
                end = Offset(it.size.width - afterPadding, it.size.height.toFloat()),
                strokeWidth = paddingLineStrokeWidth
            )
        }
    }
}

internal const val CenterAnchor = 0.5f
internal const val ItemNumber = 200
