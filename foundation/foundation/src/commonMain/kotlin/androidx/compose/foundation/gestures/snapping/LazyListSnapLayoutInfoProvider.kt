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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy

/**
 * A [SnapLayoutInfoProvider] for LazyLists.
 *
 * @param lazyListState The [LazyListState] with information about the current state of the list
 * @param positionInLayout The desired positioning of the snapped item within the main layout.
 * This position should be considered with regard to the start edge of the item and the placement
 * within the viewport.
 *
 * @return A [SnapLayoutInfoProvider] that can be used with [SnapFlingBehavior]
 */
@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyListState: LazyListState,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float =
        { layoutSize, itemSize -> (layoutSize / 2f - itemSize / 2f) }
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyListLayoutInfo
        get() = lazyListState.layoutInfo

    // Single page snapping is the default
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float = 0f

    override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset =
                calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return lowerBoundOffset.rangeTo(upperBoundOffset)
    }

    override fun Density.snapStepSize(): Float = with(layoutInfo) {
        if (visibleItemsInfo.isNotEmpty()) {
            visibleItemsInfo.fastSumBy { it.size } / visibleItemsInfo.size.toFloat()
        } else {
            0f
        }
    }
}

internal fun Density.calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyListLayoutInfo,
    item: LazyListItemInfo,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float
): Float {
    val containerSize =
        with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

    val desiredDistance =
        positionInLayout(containerSize.toFloat(), item.size.toFloat())

    val itemCurrentPosition = item.offset
    return itemCurrentPosition - desiredDistance
}

private val LazyListLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width