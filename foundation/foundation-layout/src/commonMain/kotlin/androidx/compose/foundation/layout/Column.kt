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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A layout composable that places its children in a vertical sequence. For a layout composable
 * that places its children in a horizontal sequence, see [Row]. For a layout that places children
 * in a vertical sequence and is also scrollable, see `ScrollableColumn`. For a vertically
 * scrollable list that only composes and lays out the currently visible items see `LazyColumn`.
 *
 * The [Column] layout is able to assign children heights according to their weights provided
 * using the [ColumnScope.weight] modifier. If a child is not provided a weight, it will be
 * asked for its preferred height before the sizes of the children with weights are calculated
 * proportionally to their weight based on the remaining available space.
 *
 * When none of its children have weights, a [Column] will be as small as possible to fit its
 * children one on top of the other. In order to change the height of the [Column], use the
 * [Modifier.height] modifiers; e.g. to make it fill the available height [Modifier.fillMaxHeight]
 * can be used. If at least one child of a [Column] has a [weight][ColumnScope.weight],
 * the [Column] will fill the available height, so there is no need for [Modifier.fillMaxHeight].
 * However, if [Column]'s size should be limited, the [Modifier.height] or [Modifier.size] layout
 * modifiers should be applied.
 *
 * When the size of the [Column] is larger than the sum of its children sizes, a
 * [verticalArrangement] can be specified to define the positioning of the children inside the
 * [Column]. See [Arrangement] for available positioning behaviors; a custom arrangement can also
 * be defined using the constructor of [Arrangement].
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleColumn
 *
 * @param modifier The modifier to be applied to the Column.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalAlignment The horizontal alignment of the layout's children.
 *
 * @see Row
 * @see [androidx.compose.foundation.ScrollableColumn]
 * @see [androidx.compose.foundation.lazy.LazyColumn]
 */
@Composable
inline fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val measureBlocks = columnMeasureBlocks(verticalArrangement, horizontalAlignment)
    Layout(
        content = { ColumnScope.content() },
        measureBlocks = measureBlocks,
        modifier = modifier
    )
}

@PublishedApi
internal val DefaultColumnMeasureBlocks = rowColumnMeasureBlocks(
    orientation = LayoutOrientation.Vertical,
    arrangement = { totalSize, size, _, density, outPosition ->
        Arrangement.Top.arrange(totalSize, size, density, outPosition)
    },
    arrangementSpacing = Arrangement.Top.spacing,
    crossAxisAlignment = CrossAxisAlignment.horizontal(Alignment.Start),
    crossAxisSize = SizeMode.Wrap
)

@PublishedApi
@Composable
internal fun columnMeasureBlocks(
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal
) = remember(verticalArrangement, horizontalAlignment) {
    if (verticalArrangement == Arrangement.Top && horizontalAlignment == Alignment.Start) {
        DefaultColumnMeasureBlocks
    } else {
        rowColumnMeasureBlocks(
            orientation = LayoutOrientation.Vertical,
            arrangement = { totalSize, size, _, density, outPosition ->
                verticalArrangement.arrange(totalSize, size, density, outPosition)
            },
            arrangementSpacing = verticalArrangement.spacing,
            crossAxisAlignment = CrossAxisAlignment.horizontal(horizontalAlignment),
            crossAxisSize = SizeMode.Wrap
        )
    }
}

/**
 * Scope for the children of [Column].
 */
@LayoutScopeMarker
@Immutable
interface ColumnScope {
    /**
     * Align the element horizontally within the [Column]. This alignment will have priority over
     * the [Column]'s `horizontalAlignment` parameter.
     *
     * Example usage:
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignInColumn
     */
    @Stable
    fun Modifier.align(alignment: Alignment.Horizontal) = this.then(
        HorizontalAlignModifier(
            horizontal = alignment,
            inspectorInfo = debugInspectorInfo {
                name = "align"
                value = alignment
            }
        )
    )

    /**
     * Position the element horizontally such that its [alignmentLine] aligns with sibling elements
     * also configured to [alignBy]. [alignBy] is a form of [align],
     * so both modifiers will not work together if specified for the same layout.
     * Within a [Column], all components with [alignBy] will align horizontally using
     * the specified [VerticalAlignmentLine]s or values provided using the other
     * [alignBy] overload, forming a sibling group.
     * At least one element of the sibling group will be placed as it had [Alignment.Start] align
     * in [Column], and the alignment of the other siblings will be then determined such that
     * the alignment lines coincide. Note that if only one element in a [Column] has the
     * [alignBy] modifier specified the element will be positioned
     * as if it had [Alignment.Start] align.
     *
     * Example usage:
     * @sample androidx.compose.foundation.layout.samples.SimpleRelativeToSiblingsInColumn
     */
    @Stable
    fun Modifier.alignBy(alignmentLine: VerticalAlignmentLine) = this.then(
        SiblingsAlignedModifier.WithAlignmentLine(
            line = alignmentLine,
            inspectorInfo = debugInspectorInfo {
                name = "alignBy"
                value = alignmentLine
            }
        )
    )

    @Deprecated(
        "alignWithSiblings was renamed to alignBy.",
        ReplaceWith("alignBy(alignmentLine)")
    )
    fun Modifier.alignWithSiblings(alignmentLine: VerticalAlignmentLine) = alignBy(alignmentLine)

    /**
     * Size the element's height proportional to its [weight] relative to other weighted sibling
     * elements in the [Column]. The parent will divide the vertical space remaining after measuring
     * unweighted child elements and distribute it according to this weight.
     * When [fill] is true, the element will be forced to occupy the whole height allocated to it.
     * Otherwise, the element is allowed to be smaller - this will result in [Column] being smaller,
     * as the unused allocated height will not be redistributed to other siblings.
     *
     * @param weight The proportional height to give to this element, as related to the total of
     * all weighted siblings. Must be positive.
     * @param fill When `true`, the element will occupy the whole height allocated.
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleColumn
     */
    @Stable
    fun Modifier.weight(
        /*@FloatRange(from = 0.0, fromInclusive = false)*/
        weight: Float,
        fill: Boolean = true
    ): Modifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this.then(
            LayoutWeightImpl(
                weight = weight,
                fill = fill,
                inspectorInfo = debugInspectorInfo {
                    name = "weight"
                    value = weight
                    properties["weight"] = weight
                    properties["fill"] = fill
                }
            )
        )
    }

    /**
     * Position the element horizontally such that the alignment line for the content as
     * determined by [alignmentLineBlock] aligns with sibling elements also configured to
     * [alignBy]. [alignBy] is a form of [align], so both modifiers
     * will not work together if specified for the same layout.
     * Within a [Column], all components with [alignBy] will align horizontally using
     * the specified [VerticalAlignmentLine]s or values obtained from [alignmentLineBlock],
     * forming a sibling group.
     * At least one element of the sibling group will be placed as it had [Alignment.Start] align
     * in [Column], and the alignment of the other siblings will be then determined such that
     * the alignment lines coincide. Note that if only one element in a [Column] has the
     * [alignBy] modifier specified the element will be positioned
     * as if it had [Alignment.Start] align.
     *
     * Example usage:
     * @sample androidx.compose.foundation.layout.samples.SimpleRelativeToSiblings
     */
    @Stable
    fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int) = this.then(
        SiblingsAlignedModifier.WithAlignmentLineBlock(
            block = alignmentLineBlock,
            inspectorInfo = debugInspectorInfo {
                name = "alignBy"
                value = alignmentLineBlock
            }
        )
    )

    @Deprecated(
        "alignWithSiblings was renamed to alignBy.",
        ReplaceWith("alignBy(alignmentLineBlock)")
    )
    fun Modifier.alignWithSiblings(alignmentLineBlock: (Measured) -> Int) =
        alignBy(alignmentLineBlock)

    companion object : ColumnScope
}
