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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.IntrinsicsMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A [Modifier.Node] that changes how its wrapped content is measured and laid out.
 * It has the same measurement and layout functionality as the [androidx.compose.ui.layout.Layout]
 * component, while wrapping exactly one layout due to it being a modifier. In contrast,
 * the [androidx.compose.ui.layout.Layout] component is used to define the layout behavior of
 * multiple children.
 *
 * This is the [androidx.compose.ui.Modifier.Node] equivalent of
 * [androidx.compose.ui.layout.LayoutModifier]
 *
 * @sample androidx.compose.ui.samples.LayoutModifierNodeSample
 *
 * @see androidx.compose.ui.layout.Layout
 */
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
     * [androidx.compose.ui.layout.Layout], the only difference is that they apply to exactly one
     * child. For a more detailed explanation of measurement and layout, see
     * [androidx.compose.ui.layout.MeasurePolicy].
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
    ): Int = NodeMeasuringIntrinsics.minWidth(
        this@LayoutModifierNode,
        this,
        measurable,
        height
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = NodeMeasuringIntrinsics.minHeight(
        this@LayoutModifierNode,
        this,
        measurable,
        width
    )

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = NodeMeasuringIntrinsics.maxWidth(
        this@LayoutModifierNode,
        this,
        measurable,
        height
    )

    /**
     * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = NodeMeasuringIntrinsics.maxHeight(
        this@LayoutModifierNode,
        this,
        measurable,
        width
    )
}

/**
 * This will invalidate the current node's layer, and ensure that the layer is redrawn for the next
 * frame.
 */
@ExperimentalComposeUiApi
fun LayoutModifierNode.invalidateLayer() =
    requireCoordinator(Nodes.Layout).invalidateLayer()

/**
 * This will invalidate the current node's layout pass, and ensure that relayout of this node will
 * happen for the next frame.
 */
@ExperimentalComposeUiApi
fun LayoutModifierNode.invalidateLayout() = requireLayoutNode().requestRelayout()

/**
 * This invalidates the current node's measure result, and ensures that a remeasurement of this node
 * will happen for the next frame.
 */
@ExperimentalComposeUiApi
fun LayoutModifierNode.invalidateMeasurements() = requireLayoutNode().invalidateMeasurements()

@ExperimentalComposeUiApi
internal fun LayoutModifierNode.requestRemeasure() = requireLayoutNode().requestRemeasure()

/**
 * IntermediateLayoutModifier is a [LayoutModifierNode] that will be skipped when
 * looking ahead. During measure pass, [measure] will be invoked with the constraints from the
 * look-ahead, as well as the target size.
 */
@ExperimentalComposeUiApi
interface IntermediateLayoutModifierNode : LayoutModifierNode {
    var targetSize: IntSize
}

@ExperimentalComposeUiApi
private object NodeMeasuringIntrinsics {
    internal fun minWidth(
        node: LayoutModifierNode,
        instrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        h: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Min,
            IntrinsicWidthHeight.Width
        )
        val constraints = Constraints(maxHeight = h)
        val layoutResult = with(node) {
            IntrinsicsMeasureScope(instrinsicMeasureScope, instrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.width
    }

    internal fun minHeight(
        node: LayoutModifierNode,
        instrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        w: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Min,
            IntrinsicWidthHeight.Height
        )
        val constraints = Constraints(maxWidth = w)
        val layoutResult = with(node) {
            IntrinsicsMeasureScope(instrinsicMeasureScope, instrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.height
    }

    internal fun maxWidth(
        node: LayoutModifierNode,
        instrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        h: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Max,
            IntrinsicWidthHeight.Width
        )
        val constraints = Constraints(maxHeight = h)
        val layoutResult = with(node) {
            IntrinsicsMeasureScope(instrinsicMeasureScope, instrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.width
    }

    internal fun maxHeight(
        node: LayoutModifierNode,
        instrinsicMeasureScope: IntrinsicMeasureScope,
        intrinsicMeasurable: IntrinsicMeasurable,
        w: Int
    ): Int {
        val measurable = DefaultIntrinsicMeasurable(
            intrinsicMeasurable,
            IntrinsicMinMax.Max,
            IntrinsicWidthHeight.Height
        )
        val constraints = Constraints(maxWidth = w)
        val layoutResult = with(node) {
            IntrinsicsMeasureScope(instrinsicMeasureScope, instrinsicMeasureScope.layoutDirection)
                .measure(measurable, constraints)
        }
        return layoutResult.height
    }

    private class DefaultIntrinsicMeasurable(
        val measurable: IntrinsicMeasurable,
        val minMax: IntrinsicMinMax,
        val widthHeight: IntrinsicWidthHeight
    ) : Measurable {
        override val parentData: Any?
            get() = measurable.parentData

        override fun measure(constraints: Constraints): Placeable {
            if (widthHeight == IntrinsicWidthHeight.Width) {
                val width = if (minMax == IntrinsicMinMax.Max) {
                    measurable.maxIntrinsicWidth(constraints.maxHeight)
                } else {
                    measurable.minIntrinsicWidth(constraints.maxHeight)
                }
                return EmptyPlaceable(width, constraints.maxHeight)
            }
            val height = if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicHeight(constraints.maxWidth)
            } else {
                measurable.minIntrinsicHeight(constraints.maxWidth)
            }
            return EmptyPlaceable(constraints.maxWidth, height)
        }

        override fun minIntrinsicWidth(height: Int): Int {
            return measurable.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: Int): Int {
            return measurable.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: Int): Int {
            return measurable.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: Int): Int {
            return measurable.maxIntrinsicHeight(width)
        }
    }

    private class EmptyPlaceable(width: Int, height: Int) : Placeable() {
        init {
            measuredSize = IntSize(width, height)
        }

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified
        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
        }
    }

    private enum class IntrinsicMinMax { Min, Max }
    private enum class IntrinsicWidthHeight { Width, Height }
}