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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.Density

@OptIn(ExperimentalFoundationApi::class)
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: (Float, Float) -> Float = { mainAxisLayoutSize, mainAxisItemSize ->
        mainAxisLayoutSize / 2f - mainAxisItemSize / 2f
    }
) = object : SnapLayoutInfoProvider {
    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    override fun Density.calculateApproachOffset(initialVelocity: Float) = 0f

    // use the first row/column as a baseline for snapping.
    private val singleAxisItems: List<LazyGridItemInfo>
        get() = lazyGridState.layoutInfo.visibleItemsInfo.filter {
            if (lazyGridState.layoutInfo.orientation == Orientation.Horizontal) {
                it.row == 0
            } else {
                it.column == 0
            }
        }

    override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var distanceFromItemBeforeTarget = Float.NEGATIVE_INFINITY
        var distanceFromItemAfterTarget = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.forEach { item ->
            val distance =
                calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            // Find item that is closest to the center
            if (distance <= 0 && distance > distanceFromItemBeforeTarget) {
                distanceFromItemBeforeTarget = distance
            }

            // Find item that is closest to center, but after it
            if (distance >= 0 && distance < distanceFromItemAfterTarget) {
                distanceFromItemAfterTarget = distance
            }
        }

        return distanceFromItemBeforeTarget.rangeTo(distanceFromItemAfterTarget)
    }

    override fun Density.snapStepSize(): Float {
        return if (singleAxisItems.isNotEmpty()) {
            val size = if (layoutInfo.orientation == Orientation.Vertical) {
                singleAxisItems.sumOf { it.size.height }
            } else {
                singleAxisItems.sumOf { it.size.width }
            }
            size / singleAxisItems.size.toFloat()
        } else {
            0f
        }
    }
}

internal fun calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float
): Float {

    val containerSize =
        with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

    val desiredDistance =
        positionInLayout(containerSize, item.sizeOnMainAxis(layoutInfo.orientation))

    val itemCurrentPosition = item.offsetOnMainAxis(layoutInfo.orientation)
    return itemCurrentPosition - desiredDistance
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Float
    get() = if (orientation == Orientation.Vertical) {
        viewportSize.height.toFloat()
    } else {
        viewportSize.width.toFloat()
    }

private fun LazyGridItemInfo.sizeOnMainAxis(orientation: Orientation): Float {
    return if (orientation == Orientation.Vertical) {
        size.height.toFloat()
    } else {
        size.width.toFloat()
    }
}

private fun LazyGridItemInfo.offsetOnMainAxis(orientation: Orientation): Float {
    return if (orientation == Orientation.Vertical) {
        offset.y.toFloat()
    } else {
        offset.x.toFloat()
    }
}
