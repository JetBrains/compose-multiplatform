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

import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max

/**
 * A [Modifier] that can add padding to position the content according to specified distances
 * from its bounds to an [alignment line][AlignmentLine]. Whether the positioning is vertical
 * or horizontal is defined by the orientation of the given [alignmentLine] (if the line is
 * horizontal, [before] and [after] will refer to distances from top and bottom, otherwise they
 * will refer to distances from start and end). The opposite axis sizing and positioning will
 * remain unaffected.
 * The modified layout will try to include the required padding, subject to the incoming max
 * layout constraints, such that the distance from its bounds to the [alignmentLine] of the
 * content will be [before] and [after], respectively. When the max constraints do not allow
 * this, satisfying the [before] requirement will have priority over [after]. When the modified
 * layout is min constrained in the affected layout direction and the padded layout is smaller
 * than the constraint, the modified layout will satisfy the min constraint and the content will
 * be positioned to satisfy the [before] requirement if specified, or the [after] requirement
 * otherwise.
 *
 * @param alignmentLine the alignment line relative to which the padding is defined
 * @param before the distance between the container's top edge and the horizontal alignment line, or
 * the container's start edge and the vertical alignment line
 * @param after the distance between the container's bottom edge and the horizontal alignment line,
 * or the container's end edge and the vertical alignment line
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.RelativePaddingFromSample
 */
@Stable
fun Modifier.relativePaddingFrom(
    alignmentLine: AlignmentLine,
    before: Dp = Dp.Unspecified,
    after: Dp = Dp.Unspecified
): Modifier = this.then(AlignmentLineOffset(alignmentLine, before, after))

private data class AlignmentLineOffset(
    val alignmentLine: AlignmentLine,
    val before: Dp,
    val after: Dp
) : LayoutModifier {
    init {
        require(
            (before.value >= 0f || before == Dp.Unspecified) &&
                (after.value >= 0f || after == Dp.Unspecified)
        ) {
            "Padding from alignment line must be a non-negative number"
        }
    }
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(
            // Loose constraints perpendicular on the alignment line.
            if (alignmentLine.horizontal) constraints.copy(minHeight = 0)
            else constraints.copy(minWidth = 0)
        )
        val linePosition = placeable[alignmentLine].let {
            if (it != AlignmentLine.Unspecified) it else 0
        }
        val axis = if (alignmentLine.horizontal) placeable.height else placeable.width
        val axisMax = if (alignmentLine.horizontal) constraints.maxHeight else constraints.maxWidth
        // Compute padding required to satisfy the total before and after offsets.
        val paddingBefore =
            ((if (before != Dp.Unspecified) before.toIntPx() else 0) - linePosition)
                .coerceIn(0, axisMax - axis)
        val paddingAfter =
            ((if (after != Dp.Unspecified) after.toIntPx() else 0) - axis + linePosition)
                .coerceIn(0, axisMax - axis - paddingBefore)

        val width = if (alignmentLine.horizontal) {
            placeable.width
        } else {
            max(paddingBefore + placeable.width + paddingAfter, constraints.minWidth)
        }
        val height = if (alignmentLine.horizontal) {
            max(paddingBefore + placeable.height + paddingAfter, constraints.minHeight)
        } else {
            placeable.height
        }
        return layout(width, height) {
            val x = when {
                alignmentLine.horizontal -> 0
                before != Dp.Unspecified -> paddingBefore
                else -> width - paddingAfter - placeable.width
            }
            val y = when {
                !alignmentLine.horizontal -> 0
                before != Dp.Unspecified -> paddingBefore
                else -> height - paddingAfter - placeable.height
            }
            placeable.placeRelative(x, y)
        }
    }
}

private val AlignmentLine.horizontal: Boolean get() = this is HorizontalAlignmentLine
