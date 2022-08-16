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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

@ExperimentalComposeUiApi
interface LayoutModifierNode : Remeasurement, DelegatableNode {
    // NOTE(lmr): i guess RemeasurementModifier was created because there are some use
    //  cases where we want to call forceRemeasure but we don't want to implement MeasureNode.
    //  I think maybe we should just add this as an API on DelegatingNode. I don't think we need
    //  to burn a NodeType on this...
    override fun forceRemeasure() = requireLayoutNode().forceRemeasure()

    /**
     * The function used to measure the modifier. The [measurable] corresponds to the
     * wrapped content, and it can be measured with the desired constraints according
     * to the logic of the [LayoutModifierNode]. The modifier needs to choose its own
     * size, which can depend on the size chosen by the wrapped content (the obtained
     * [Placeable]), if the wrapped content was measured. The size needs to be returned
     * as part of a [MeasureResult], alongside the placement logic of the
     * [Placeable], which defines how the wrapped content should be positioned inside
     * the [LayoutModifierNode]. A convenient way to create the [MeasureResult]
     * is to use the [MeasureScope.layout] factory function.
     *
     * A [LayoutModifierNode] uses the same measurement and layout concepts and principles as a
     * [Layout], the only difference is that they apply to exactly one child. For a more detailed
     * explanation of measurement and layout, see [MeasurePolicy].
     */
    fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int

    /**
     * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int

    /**
     * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int
}

@ExperimentalComposeUiApi
internal fun LayoutModifierNode.invalidateLayer() =
    requireCoordinator(Nodes.Layout).invalidateLayer()

@ExperimentalComposeUiApi
internal fun LayoutModifierNode.requestRelayout() = requireLayoutNode().requestRelayout()

@ExperimentalComposeUiApi
internal fun LayoutModifierNode.requestRemeasure() = requireLayoutNode().requestRemeasure()

@ExperimentalComposeUiApi
interface IntermediateLayoutModifierNode : LayoutModifierNode {
    var targetSize: IntSize
}
