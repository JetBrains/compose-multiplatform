/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasuringIntrinsicsMeasureBlocks
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max

/**
 * A layout composable that positions its children relative to its edges.
 * The component is useful for drawing children that overlap. The children will always be
 * drawn in the order they are specified in the body of the [Box].
 * When children are smaller than the parent, by default they will be positioned inside the [Box]
 * according to the [alignment]. If individual alignment of the children is needed, apply the
 * [BoxScope.align] modifier to a child to specify its alignment.
 *
 * Example usage:
 *
 * @2sample androidx.compose.foundation.layout.samples.SimpleBox
 *
 * @param modifier The modifier to be applied to the layout.
 * @param alignment The default alignment inside the Box.
 */
@Composable
@OptIn(ExperimentalLayoutNodeApi::class)
inline fun Box(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    crossinline children: @Composable BoxScope.() -> Unit
) {
    val measureBlocks = rememberMeasureBlocks(alignment)
    Layout(
        children = { BoxScope.children() },
        measureBlocks = measureBlocks,
        modifier = modifier
    )
}

@PublishedApi
@Composable
internal fun rememberMeasureBlocks(
    alignment: Alignment
) = remember(alignment) {
    if (alignment == Alignment.TopStart) {
        DefaultBoxMeasureBlocks
    } else {
        boxMeasureBlocks(alignment)
    }
}

@OptIn(ExperimentalLayoutNodeApi::class)
internal val DefaultBoxMeasureBlocks: LayoutNode.MeasureBlocks =
    boxMeasureBlocks(Alignment.TopStart)

internal fun boxMeasureBlocks(alignment: Alignment) =
    MeasuringIntrinsicsMeasureBlocks { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@MeasuringIntrinsicsMeasureBlocks layout(
                constraints.minWidth,
                constraints.minHeight
            ) {}
        }

        val minRelaxedConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        if (measurables.size == 1) {
            val measurable = measurables[0]
            val boxWidth: Int
            val boxHeight: Int
            val placeable: Placeable
            if (!measurable.matchesParentSize) {
                placeable = measurable.measure(minRelaxedConstraints)
                boxWidth = max(constraints.minWidth, placeable.width)
                boxHeight = max(constraints.minHeight, placeable.height)
            } else {
                boxWidth = constraints.minWidth
                boxHeight = constraints.minHeight
                placeable = measurable.measure(
                    Constraints.fixed(constraints.minWidth, constraints.minHeight)
                )
            }
            return@MeasuringIntrinsicsMeasureBlocks layout(boxWidth, boxHeight) {
                placeInBox(placeable, measurable, layoutDirection, boxWidth, boxHeight, alignment)
            }
        }

        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure non match parent size children to get the size of the Box.
        var hasMatchParentSizeChildren = false
        var boxWidth = constraints.minWidth
        var boxHeight = constraints.minHeight
        measurables.fastForEachIndexed { index, measurable ->
            if (!measurable.matchesParentSize) {
                val placeable = measurable.measure(minRelaxedConstraints)
                placeables[index] = placeable
                boxWidth = max(boxWidth, placeable.width)
                boxHeight = max(boxHeight, placeable.height)
            } else {
                hasMatchParentSizeChildren = true
            }
        }

        // Now measure match parent size children, if any.
        if (hasMatchParentSizeChildren) {
            // The infinity check is needed for default intrinsic measurements.
            val matchParentSizeConstraints = Constraints(
                minWidth = if (boxWidth != Constraints.Infinity) boxWidth else 0,
                minHeight = if (boxHeight != Constraints.Infinity) boxHeight else 0,
                maxWidth = boxWidth,
                maxHeight = boxHeight
            )
            measurables.fastForEachIndexed { index, measurable ->
                if (measurable.matchesParentSize) {
                    placeables[index] = measurable.measure(matchParentSizeConstraints)
                }
            }
        }

        // Specify the size of the Box and position its children.
        layout(boxWidth, boxHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable as Placeable
                val measurable = measurables[index]
                placeInBox(placeable, measurable, layoutDirection, boxWidth, boxHeight, alignment)
            }
        }
    }

private fun Placeable.PlacementScope.placeInBox(
    placeable: Placeable,
    measurable: Measurable,
    layoutDirection: LayoutDirection,
    boxWidth: Int,
    boxHeight: Int,
    alignment: Alignment
) {
    val childAlignment = measurable.boxChildData?.alignment ?: alignment
    val position = childAlignment.align(
        IntSize(boxWidth - placeable.width, boxHeight - placeable.height),
        layoutDirection
    )
    placeable.place(position)
}

/**
 * A convenience box with no content that can participate in layout, drawing, pointer input
 * due to the [modifier] applied to it.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleBox
 *
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
@OptIn(ExperimentalLayoutNodeApi::class)
fun Box(modifier: Modifier) {
    Layout({}, measureBlocks = EmptyBoxMeasureBlocks, modifier = modifier)
}

@OptIn(ExperimentalLayoutNodeApi::class)
internal val EmptyBoxMeasureBlocks = MeasuringIntrinsicsMeasureBlocks { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
}

@Composable
@Deprecated(
    "Stack was renamed to Box.",
    ReplaceWith("Box", "androidx.compose.foundation.layout.Box")
)
fun Stack(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    children: @Composable BoxScope.() -> Unit
) = Box(modifier, alignment, children)

/**
 * A BoxScope provides a scope for the children of a [Box].
 */
@LayoutScopeMarker
@Immutable
interface BoxScope {
    /**
     * Pull the content element to a specific [Alignment] within the [Box]. This alignment will
     * have priority over the [Box]'s `alignment` parameter.
     */
    @Stable
    fun Modifier.align(alignment: Alignment) = this.then(BoxChildData(alignment, false))

    @Stable
    @Deprecated("gravity has been renamed to align.", ReplaceWith("align(align)"))
    fun Modifier.gravity(align: Alignment) = this.then(BoxChildData(align, false))

    /**
     * Size the element to match the size of the [Box] after all other content elements have
     * been measured.
     *
     * The element using this modifier does not take part in defining the size of the [Box].
     * Instead, it matches the size of the [Box] after all other children (not using
     * matchParentSize() modifier) have been measured to obtain the [Box]'s size.
     * In contrast, a general-purpose [Modifier.fillMaxSize] modifier, which makes an element
     * occupy all available space, will take part in defining the size of the [Box]. Consequently,
     * using it for an element inside a [Box] will make the [Box] itself always fill the
     * available space.
     */
    @Stable
    fun Modifier.matchParentSize() = this.then(MatchParentSizeModifier)

    companion object : BoxScope
}

@Deprecated(
    "Stack was renamed to Box.",
    ReplaceWith("BoxScope", "androidx.compose.foundation.layout.BoxScope")
)
typealias StackScope = BoxScope

@Stable
private val MatchParentSizeModifier: ParentDataModifier = BoxChildData(Alignment.Center, true)

private val Measurable.boxChildData: BoxChildData? get() = parentData as? BoxChildData
private val Measurable.matchesParentSize: Boolean get() = boxChildData?.matchParentSize ?: false

private data class BoxChildData(
    var alignment: Alignment,
    var matchParentSize: Boolean = false
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@BoxChildData
}
