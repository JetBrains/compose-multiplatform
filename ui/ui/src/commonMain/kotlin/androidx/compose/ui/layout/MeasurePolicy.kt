/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastMap

/**
 * Defines the measure and layout behavior of a [Layout]. [Layout] and [MeasurePolicy] are the way
 * Compose layouts (such as `Box`, `Column`, etc.) are built, and they can also be used to achieve
 * custom layouts.
 *
 * See [Layout] samples for examples of how to use [MeasurePolicy].
 *
 * Intrinsic measurement methods define the intrinsic size of the layout. These can be queried
 * by the layout's parent in order to obtain, in specific cases, more information about
 * the size of the layout in the absence of specific constraints:
 * - [minIntrinsicWidthMeasureBlock] defines the minimum width this layout can take, given
 *   a specific height, such that the content of the layout will be painted correctly
 * - [minIntrinsicHeightMeasureBlock] defines the minimum height this layout can take, given
 *   a specific width, such that the content of the layout will be painted correctly
 * - [maxIntrinsicWidthMeasureBlock] defines the minimum width such that increasing it further
 *   will not decrease the minimum intrinsic height
 * - [maxIntrinsicHeightMeasureBlock] defines the minimum height such that increasing it further
 *   will not decrease the minimum intrinsic width
 * Most layout scenarios do not require querying intrinsic measurements. Therefore, when writing
 * a custom layout, it is common to only define the actual measurement, as most of the times
 * the intrinsic measurements of the layout will not be queried. Moreover, intrinsic measurement
 * methods have default implementations that make a best effort attempt to calculate the intrinsic
 * measurements by reusing the [measure] method. Note this will not be correct for all layouts,
 * but can be a convenient approximation.
 * Intrinsic measurements can be useful when the layout system enforcement of no more than one
 * measurement per child is limiting. Layouts that use them are the `preferredWidth(IntrinsicSize)`
 * and `preferredHeight(IntrinsicSize)` modifiers. See their samples for when they can be useful.
 *
 * @see Layout
 */
@Stable
fun interface MeasurePolicy {
    /**
     * The function that defines the measurement and layout. Each [Measurable] in the [measurables]
     * list corresponds to a layout child of the layout, and children can be measured using the
     * [Measurable.measure] method. Measuring a child returns a [Placeable], which can then
     * be positioned in the [MeasureResult.placeChildren] of the returned [MeasureResult].
     * Usually [MeasureResult] objects are created using the [MeasureScope.layout] factory, which
     * takes the calculated size of this layout, its alignment lines, and a block defining
     * the positioning of the children layouts.
     */
    fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth]. It represents
     * the minimum width this layout can take, given a specific height, such that the content
     * of the layout can be painted correctly.
     */
    fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        val mapped = measurables.fastMap {
            DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
        }
        val constraints = Constraints(maxHeight = height)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.width
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicHeight]. It represents
     * defines the minimum height this layout can take, given  a specific width, such
     * that the content of the layout will be painted correctly.
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        val mapped = measurables.fastMap {
            DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
        }
        val constraints = Constraints(maxWidth = width)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.height
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth]. It represents the
     * minimum width such that increasing it further will not decrease the minimum intrinsic height.
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        val mapped = measurables.fastMap {
            DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
        }
        val constraints = Constraints(maxHeight = height)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.width
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicHeight]. It represents the
     * minimum height such that increasing it further will not decrease the minimum intrinsic width.
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        val mapped = measurables.fastMap {
            DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
        }
        val constraints = Constraints(maxWidth = width)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.height
    }
}