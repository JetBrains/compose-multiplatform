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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
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
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.sign

val SnappingDemos = listOf(
    DemoCategory("Lazy List Snapping", LazyListSnappingDemos),
    DemoCategory("Scrollable Row Snapping", RowSnappingDemos),
    DemoCategory("Lazy Grid Snapping", LazyGridSnappingDemos),
)

@OptIn(ExperimentalFoundationApi::class)
internal class MultiPageSnappingLayoutInfoProvider(
    private val baseSnapLayoutInfoProvider: SnapLayoutInfoProvider,
    private val decayAnimationSpec: DecayAnimationSpec<Float>
) : SnapLayoutInfoProvider by baseSnapLayoutInfoProvider {
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        val offset = decayAnimationSpec.calculateTargetValue(0f, initialVelocity)
        val finalDecayedOffset = (offset.absoluteValue - calculateSnapStepSize()).coerceAtLeast(0f)
        return finalDecayedOffset * initialVelocity.sign
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class ViewPortBasedSnappingLayoutInfoProvider(
    private val baseSnapLayoutInfoProvider: SnapLayoutInfoProvider,
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val viewPortStep: () -> Float
) : SnapLayoutInfoProvider by baseSnapLayoutInfoProvider {
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        val offset = decayAnimationSpec.calculateTargetValue(0f, initialVelocity)
        val viewPortOffset = viewPortStep()
        return offset.coerceIn(-viewPortOffset, viewPortOffset)
    }
}

@Composable
internal fun SnappingDemoMainLayout(
    lazyListState: LazyListState,
    flingBehavior: FlingBehavior,
    contentPaddingValues: PaddingValues = PaddingValues(8.dp),
    content: @Composable (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .drawWithContent {
                drawContent()
                drawAnchor(
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
internal fun DefaultSnapDemoItem(position: Int) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(500.dp)
            .padding(8.dp)
            .background(Color.White)
            .drawWithContent {
                drawContent()
                drawAnchor(CenterAnchor)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = position.toString(), fontSize = 40.sp)
    }
}

@Composable
internal fun ResizableSnapDemoItem(width: Dp, height: Dp, position: Int) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .padding(8.dp)
            .background(Color.White)
            .drawWithContent {
                drawContent()
                drawAnchor(CenterAnchor)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = position.toString(), fontSize = 40.sp)
    }
}

internal fun ContentDrawScope.drawAnchor(
    anchor: Float,
    contentPaddingValues: PaddingValues = PaddingValues(0.dp),
    shouldDrawPadding: Boolean = false,
    mainLineStrokeWidth: Float = Stroke.HairlineWidth,
    paddingLineStrokeWidth: Float = Stroke.HairlineWidth
) {
    val beforePadding = contentPaddingValues.calculateStartPadding(LayoutDirection.Rtl).toPx()
    val afterPadding = contentPaddingValues.calculateEndPadding(LayoutDirection.Rtl).toPx()

    val center = (size.width - beforePadding - afterPadding) * anchor + beforePadding

    drawLine(
        Color.Red,
        start = Offset(center, 0f),
        end = Offset(center, size.height),
        strokeWidth = mainLineStrokeWidth
    )

    if (shouldDrawPadding) {
        drawLine(
            Color.Magenta,
            start = Offset(beforePadding, 0f),
            end = Offset(beforePadding, size.height),
            strokeWidth = paddingLineStrokeWidth
        )

        drawLine(
            Color.Magenta,
            start = Offset(size.width - afterPadding, 0f),
            end = Offset(size.width - afterPadding, size.height),
            strokeWidth = paddingLineStrokeWidth
        )
    }
}

internal const val CenterAnchor = 0.5f
internal const val ItemNumber = 200
