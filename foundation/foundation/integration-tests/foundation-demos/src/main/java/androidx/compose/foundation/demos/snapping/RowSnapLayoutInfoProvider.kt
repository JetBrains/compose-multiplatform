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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.ui.unit.Density
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
fun SnapLayoutInfoProvider(
    scrollState: ScrollState,
    itemSize: Density.() -> Float,
    layoutSize: Density.() -> Float
) = object : SnapLayoutInfoProvider {

    fun Density.nextFullItemCenter(layoutCenter: Float): Float {
        val intItemSize = itemSize().roundToInt()
        return floor((layoutCenter + snapStepSize()) / itemSize().roundToInt()) * intItemSize
    }

    fun Density.previousFullItemCenter(layoutCenter: Float): Float {
        val intItemSize = itemSize().roundToInt()
        return ceil((layoutCenter - snapStepSize()) / itemSize().roundToInt()) * intItemSize
    }

    override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        val layoutCenter = layoutSize() / 2f + scrollState.value + snapStepSize() / 2f
        val lowerBound = nextFullItemCenter(layoutCenter) - layoutCenter
        val upperBound = previousFullItemCenter(layoutCenter) - layoutCenter
        return upperBound.rangeTo(lowerBound)
    }

    override fun Density.snapStepSize(): Float {
        return itemSize()
    }

    override fun Density.calculateApproachOffset(initialVelocity: Float): Float = 0f
}