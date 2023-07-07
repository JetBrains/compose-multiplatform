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
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.NoInspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max

/**
 * A layout composable with [content].
 * The [Box] will size itself to fit the content, subject to the incoming constraints.
 * When children are smaller than the parent, by default they will be positioned inside
 * the [Box] according to the [contentAlignment]. For individually specifying the alignments
 * of the children layouts, use the [BoxScope.align] modifier.
 * By default, the content will be measured without the [Box]'s incoming min constraints,
 * unless [propagateMinConstraints] is `true`. As an example, setting [propagateMinConstraints] to
 * `true` can be useful when the [Box] has content on which modifiers cannot be specified
 * directly and setting a min size on the content of the [Box] is needed. If
 * [propagateMinConstraints] is set to `true`, the min size set on the [Box] will also be
 * applied to the content, whereas otherwise the min size will only apply to the [Box].
 * When the content has more than one layout child the layout children will be stacked one
 * on top of the other (positioned as explained above) in the composition order.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleBox
 *
 * @param modifier The modifier to be applied to the layout.
 * @param contentAlignment The default alignment inside the Box.
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param content The content of the [Box].
 */
@Composable
inline fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val measurePolicy = rememberBoxMeasurePolicy(contentAlignment, propagateMinConstraints)
    Layout(
        content = { BoxScopeInstance.content() },
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

@PublishedApi
@Composable
internal fun rememberBoxMeasurePolicy(
    alignment: Alignment,
    propagateMinConstraints: Boolean
) = if (alignment == Alignment.TopStart && !propagateMinConstraints) {
    DefaultBoxMeasurePolicy
} else {
    remember(alignment, propagateMinConstraints) {
        boxMeasurePolicy(alignment, propagateMinConstraints)
    }
}

internal val DefaultBoxMeasurePolicy: MeasurePolicy = boxMeasurePolicy(Alignment.TopStart, false)

internal fun boxMeasurePolicy(alignment: Alignment, propagateMinConstraints: Boolean) =
    MeasurePolicy { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@MeasurePolicy layout(
                constraints.minWidth,
                constraints.minHeight
            ) {}
        }

        val contentConstraints = if (propagateMinConstraints) {
            constraints
        } else {
            constraints.copy(minWidth = 0, minHeight = 0)
        }

        if (measurables.size == 1) {
            val measurable = measurables[0]
            val boxWidth: Int
            val boxHeight: Int
            val placeable: Placeable
            if (!measurable.matchesParentSize) {
                placeable = measurable.measure(contentConstraints)
                boxWidth = max(constraints.minWidth, placeable.width)
                boxHeight = max(constraints.minHeight, placeable.height)
            } else {
                boxWidth = constraints.minWidth
                boxHeight = constraints.minHeight
                placeable = measurable.measure(
                    Constraints.fixed(constraints.minWidth, constraints.minHeight)
                )
            }
            return@MeasurePolicy layout(boxWidth, boxHeight) {
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
                val placeable = measurable.measure(contentConstraints)
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
        IntSize(placeable.width, placeable.height),
        IntSize(boxWidth, boxHeight),
        layoutDirection
    )
    placeable.place(position)
}

/**
 * A box with no content that can participate in layout, drawing, pointer input
 * due to the [modifier] applied to it.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleBox
 *
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun Box(modifier: Modifier) {
    Layout({}, measurePolicy = EmptyBoxMeasurePolicy, modifier = modifier)
}

internal val EmptyBoxMeasurePolicy = MeasurePolicy { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
}

/**
 * A BoxScope provides a scope for the children of [Box] and [BoxWithConstraints].
 */
@LayoutScopeMarker
@Immutable
interface BoxScope {
    /**
     * Pull the content element to a specific [Alignment] within the [Box]. This alignment will
     * have priority over the [Box]'s `alignment` parameter.
     */
    @Stable
    fun Modifier.align(alignment: Alignment): Modifier

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
    fun Modifier.matchParentSize(): Modifier
}

internal object BoxScopeInstance : BoxScope {
    @Stable
    override fun Modifier.align(alignment: Alignment) = this.then(
        BoxChildData(
            alignment = alignment,
            matchParentSize = false,
            inspectorInfo = debugInspectorInfo {
                name = "align"
                value = alignment
            }
        )
    )

    @Stable
    override fun Modifier.matchParentSize() = this.then(
        BoxChildData(
            alignment = Alignment.Center,
            matchParentSize = true,
            inspectorInfo = debugInspectorInfo { name = "matchParentSize" }
        )
    )
}

private val Measurable.boxChildData: BoxChildData? get() = parentData as? BoxChildData
private val Measurable.matchesParentSize: Boolean get() = boxChildData?.matchParentSize ?: false

private class BoxChildData(
    var alignment: Alignment,
    var matchParentSize: Boolean = false,
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?) = this@BoxChildData

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? BoxChildData ?: return false

        return alignment == otherModifier.alignment &&
            matchParentSize == otherModifier.matchParentSize
    }

    override fun hashCode(): Int {
        var result = alignment.hashCode()
        result = 31 * result + matchParentSize.hashCode()
        return result
    }

    override fun toString(): String =
        "BoxChildData(alignment=$alignment, matchParentSize=$matchParentSize)"
}
