/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.SkippableUpdater
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.MeasureBlocks
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.simpleIdentityToString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastMap

/**
 * [Layout] is the main core component for layout. It can be used to measure and position
 * zero or more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined
 * by the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * For a composable able to define its content according to the incoming constraints,
 * see [androidx.compose.foundation.layout.BoxWithConstraints].
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.LayoutUsage
 *
 * Example usage with custom intrinsic measurements:
 * @sample androidx.compose.ui.samples.LayoutWithProvidedIntrinsicsUsage
 *
 * @param content The children composable to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 *
 * @see Layout
 * @see MeasurePolicy
 * @see androidx.compose.foundation.layout.BoxWithConstraints
 */
@Suppress("ComposableLambdaParameterPosition")
@UiComposable
@Composable inline fun Layout(
    content: @Composable @UiComposable () -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
        },
        skippableUpdate = materializerOf(modifier),
        content = content
    )
}

/**
 * [Layout] is the main core component for layout for "leaf" nodes. It can be used to measure and
 * position zero children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined
 * by the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * For a composable able to define its content according to the incoming constraints,
 * see [androidx.compose.foundation.layout.BoxWithConstraints].
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.LayoutUsage
 *
 * Example usage with custom intrinsic measurements:
 * @sample androidx.compose.ui.samples.LayoutWithProvidedIntrinsicsUsage
 *
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 *
 * @see Layout
 * @see MeasurePolicy
 * @see androidx.compose.foundation.layout.BoxWithConstraints
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
@UiComposable
inline fun Layout(
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    val materialized = currentComposer.materialize(modifier)
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
            set(materialized, ComposeUiNode.SetModifier)
        },
    )
}

@Suppress("ComposableLambdaParameterPosition")
@Composable
@UiComposable
@Deprecated(
    "This composable was deprecated. Please use the alternative Layout overloads instead."
)
internal fun Layout(
    content: @Composable @UiComposable () -> Unit,
    minIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    minIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    maxIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    maxIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    modifier: Modifier = Modifier,
    measureBlock: MeasureBlock
) {
    val measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ) = measureBlock(this, measurables, constraints)

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = minIntrinsicWidthMeasureBlock(this, measurables, height)

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = minIntrinsicHeightMeasureBlock(this, measurables, width)

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = maxIntrinsicWidthMeasureBlock(this, measurables, height)

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = maxIntrinsicHeightMeasureBlock(this, measurables, width)
    }

    Layout(content, modifier, measurePolicy)
}

@Deprecated(
    "MeasureBlocks was deprecated. Please use MeasurePolicy and the Layout overloads using " +
        "it instead."
)
internal fun measureBlocksOf(
    minIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    minIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    maxIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    maxIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    measureBlock: MeasureBlock
): MeasureBlocks {
    return object : MeasureBlocks {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ) = measureScope.measureBlock(measurables, constraints)
        override fun minIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ) = intrinsicMeasureScope.minIntrinsicWidthMeasureBlock(measurables, h)
        override fun minIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ) = intrinsicMeasureScope.minIntrinsicHeightMeasureBlock(measurables, w)
        override fun maxIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ) = intrinsicMeasureScope.maxIntrinsicWidthMeasureBlock(measurables, h)
        override fun maxIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ) = intrinsicMeasureScope.maxIntrinsicHeightMeasureBlock(measurables, w)
    }
}

@PublishedApi
internal fun materializerOf(
    modifier: Modifier
): @Composable SkippableUpdater<ComposeUiNode>.() -> Unit = {
    val materialized = currentComposer.materialize(modifier)
    update {
        set(materialized, ComposeUiNode.SetModifier)
    }
}

@Suppress("ComposableLambdaParameterPosition")
@Composable
@UiComposable
@Deprecated(
    "This API is unsafe for UI performance at scale - using it incorrectly will lead " +
        "to exponential performance issues. This API should be avoided whenever possible."
)
fun MultiMeasureLayout(
    modifier: Modifier = Modifier,
    content: @Composable @UiComposable () -> Unit,
    measurePolicy: MeasurePolicy
) {
    val materialized = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current

    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(materialized, ComposeUiNode.SetModifier)
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
            @Suppress("DEPRECATION")
            init { this.canMultiMeasure = true }
        },
        content = content
    )
}

/**
 * Used to return a fixed sized item for intrinsics measurements in [Layout]
 */
private class FixedSizeIntrinsicsPlaceable(width: Int, height: Int) : Placeable() {
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

/**
 * Identifies an [IntrinsicMeasurable] as a min or max intrinsic measurement.
 */
internal enum class IntrinsicMinMax {
    Min, Max
}

/**
 * Identifies an [IntrinsicMeasurable] as a width or height intrinsic measurement.
 */
internal enum class IntrinsicWidthHeight {
    Width, Height
}

/**
 * A wrapper around a [Measurable] for intrinsic measurements in [Layout]. Consumers of
 * [Layout] don't identify intrinsic methods, but we can give a reasonable implementation
 * by using their [measure], substituting the intrinsics gathering method
 * for the [Measurable.measure] call.
 */
internal class DefaultIntrinsicMeasurable(
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
            return FixedSizeIntrinsicsPlaceable(width, constraints.maxHeight)
        }
        val height = if (minMax == IntrinsicMinMax.Max) {
            measurable.maxIntrinsicHeight(constraints.maxWidth)
        } else {
            measurable.minIntrinsicHeight(constraints.maxWidth)
        }
        return FixedSizeIntrinsicsPlaceable(constraints.maxWidth, height)
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

/**
 * Receiver scope for [Layout]'s and [LayoutModifier]'s layout lambda when used in an intrinsics
 * call.
 */
internal class IntrinsicsMeasureScope(
    density: Density,
    override val layoutDirection: LayoutDirection
) : MeasureScope, Density by density

/**
 * Default [MeasureBlocks] object implementation, providing intrinsic measurements
 * that use the measure block replacing the measure calls with intrinsic measurement calls.
 */
@Deprecated("MeasuringIntrinsicsMeasureBlocks was deprecated. Please use MeasurePolicy instead.")
internal fun MeasuringIntrinsicsMeasureBlocks(measureBlock: MeasureBlock) =
    object : MeasureBlocks {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ) = measureScope.measureBlock(measurables, constraints)
        override fun minIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ) = intrinsicMeasureScope.MeasuringMinIntrinsicWidth(
            measureBlock,
            measurables,
            h,
            intrinsicMeasureScope.layoutDirection
        )
        override fun minIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ) = intrinsicMeasureScope.MeasuringMinIntrinsicHeight(
            measureBlock,
            measurables,
            w,
            intrinsicMeasureScope.layoutDirection
        )
        override fun maxIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ) = intrinsicMeasureScope.MeasuringMaxIntrinsicWidth(
            measureBlock,
            measurables,
            h,
            intrinsicMeasureScope.layoutDirection
        )
        override fun maxIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ) = intrinsicMeasureScope.MeasuringMaxIntrinsicHeight(
            measureBlock,
            measurables,
            w,
            intrinsicMeasureScope.layoutDirection
        )

        override fun toString(): String {
            // this calls simpleIdentityToString on measureBlock because it is typically a lambda,
            // which has a useless toString that doesn't hint at the source location
            return simpleIdentityToString(
                this,
                "MeasuringIntrinsicsMeasureBlocks"
            ) + "{ measureBlock=${simpleIdentityToString(measureBlock, null)} }"
        }
    }

/**
 * Default implementation for the min intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private fun Density.MeasuringMinIntrinsicWidth(
    measureBlock: MeasureScope.(List<Measurable>, Constraints) -> MeasureResult,
    measurables: List<IntrinsicMeasurable>,
    h: Int,
    layoutDirection: LayoutDirection
): Int {
    val mapped = measurables.fastMap {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
    }
    val constraints = Constraints(maxHeight = h)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.width
}

/**
 * Default implementation for the min intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private fun Density.MeasuringMinIntrinsicHeight(
    measureBlock: MeasureScope.(List<Measurable>, Constraints) -> MeasureResult,
    measurables: List<IntrinsicMeasurable>,
    w: Int,
    layoutDirection: LayoutDirection
): Int {
    val mapped = measurables.fastMap {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
    }
    val constraints = Constraints(maxWidth = w)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.height
}

/**
 * Default implementation for the max intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private fun Density.MeasuringMaxIntrinsicWidth(
    measureBlock: MeasureScope.(List<Measurable>, Constraints) -> MeasureResult,
    measurables: List<IntrinsicMeasurable>,
    h: Int,
    layoutDirection: LayoutDirection
): Int {
    val mapped = measurables.fastMap {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
    }
    val constraints = Constraints(maxHeight = h)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.width
}

/**
 * Default implementation for the max intrinsic height of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private fun Density.MeasuringMaxIntrinsicHeight(
    measureBlock: MeasureScope.(List<Measurable>, Constraints) -> MeasureResult,
    measurables: List<IntrinsicMeasurable>,
    w: Int,
    layoutDirection: LayoutDirection
): Int {
    val mapped = measurables.fastMap {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
    }
    val constraints = Constraints(maxWidth = w)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.height
}
