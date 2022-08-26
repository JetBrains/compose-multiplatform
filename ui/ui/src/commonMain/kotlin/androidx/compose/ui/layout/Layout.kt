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
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

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

/**
 * [Layout] is the main core component for layout for "leaf" nodes. It can be used to measure and
 * position zero children.
 *
 * This overload accepts a list of multiple composable content lambdas, which allows to threat
 * measurables put into different content lambdas differently - measure policy will provide
 * a list of lists of Measurables, not just a single list. Such list has the same size
 * as the list of contents passed into [Layout] and contains the list of measurables
 * of the corresponding content lambda in the same order.
 *
 * Note that layouts emitted as part of all [contents] lambdas will be added as a direct children
 * for this [Layout]. This means that if you set a custom z index on some children, the drawing
 * order will be calculated as if they were all provided as part of one lambda.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.LayoutWithMultipleContentsUsage
 *
 * @param contents The list of children composable contents to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 *
 * @see Layout for a simpler use case when you have only one content lambda.
 */
@ExperimentalComposeUiApi
@Suppress("ComposableLambdaParameterPosition", "NOTHING_TO_INLINE")
@UiComposable
@Composable
inline fun Layout(
    contents: List<@Composable @UiComposable () -> Unit>,
    modifier: Modifier = Modifier,
    measurePolicy: MultiContentMeasurePolicy
) {
    Layout(
        content = combineAsVirtualLayouts(contents),
        modifier = modifier,
        measurePolicy = remember(measurePolicy) { createMeasurePolicy(measurePolicy) }
    )
}

@PublishedApi
internal fun combineAsVirtualLayouts(
    contents: List<@Composable @UiComposable () -> Unit>
): @Composable @UiComposable () -> Unit = {
    contents.fastForEach { content ->
        ReusableComposeNode<ComposeUiNode, Applier<Any>>(
            factory = ComposeUiNode.VirtualConstructor,
            update = {},
            content = content
        )
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
