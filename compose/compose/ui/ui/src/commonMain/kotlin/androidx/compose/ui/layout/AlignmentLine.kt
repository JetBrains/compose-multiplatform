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

package androidx.compose.ui.layout

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.min

/**
 * Defines an offset line that can be used by parent layouts to align and position their children.
 * Text baselines are representative examples of [AlignmentLine]s. For example, they can be used
 * by `Row`, to align its children by baseline, or by `paddingFrom` to achieve a layout
 * with a specific distance from the top to the baseline of the text content. [AlignmentLine]s
 * can be understood as an abstraction over text baselines.
 *
 * When a layout provides a value for a particular [AlignmentLine], this can be read by the
 * parents of the layout after measuring, using the [Placeable.get] operator on the corresponding
 * [Placeable] instance. Based on the position of the [AlignmentLine], the parents can then decide
 * the positioning of the children.
 *
 * Note that when a layout provides a value for an [AlignmentLine], this will be automatically
 * inherited by the layout's parent, which will offset the value by the position of the child
 * within itself. This way, nested layout hierarchies are able to preserve the [AlignmentLine]s
 * defined for deeply nested children, making it possible for non-direct parents to use these for
 * positioning and alignment. When a layout inherits multiple values for the same [AlignmentLine]
 * from different children, the position of the line within the layout will be computed by merging
 * the children values using the provided [merger]. If a layout provides a value for an
 * [AlignmentLine], this will always be the position of the line, regardless of the values
 * provided by children for the same line.
 *
 * [AlignmentLine]s cannot be created directly, please create [VerticalAlignmentLine] or
 * [HorizontalAlignmentLine] instances instead.
 *
 * @sample androidx.compose.ui.samples.AlignmentLineSample
 *
 * @see VerticalAlignmentLine
 * @see HorizontalAlignmentLine
 */
@Immutable
sealed class AlignmentLine(
    internal val merger: (Int, Int) -> Int
) {
    companion object {
        /**
         * Constant representing that an [AlignmentLine] has not been provided.
         */
        const val Unspecified = Int.MIN_VALUE
    }
}

/**
 * Merges two values of the current [alignment line][AlignmentLine].
 * This is used when a layout inherits multiple values for the same [AlignmentLine]
 * from different children, so the position of the line within the layout will be computed
 * by merging the children values using the provided [AlignmentLine.merger].
 */
internal fun AlignmentLine.merge(position1: Int, position2: Int) = merger(position1, position2)

/**
 * A vertical [AlignmentLine]. Defines a vertical offset line that can be used by parent layouts
 * usually to align or position their children horizontally. The positions of the alignment lines
 * will be automatically inherited by parent layouts from their content, and the [merger] will
 * be used to merge multiple line positions when more than one child provides a specific
 * [AlignmentLine]. See [AlignmentLine] for more details.
 *
 * @param merger How to merge two alignment line values defined by different children
 */
class VerticalAlignmentLine(merger: (Int, Int) -> Int) : AlignmentLine(merger)

/**
 * A horizontal [AlignmentLine]. Defines an horizontal offset line that can be used by parent
 * layouts usually to align or position their children vertically. Text baselines (`FirstBaseline`
 * and `LastBaseline`) are representative examples of [HorizontalAlignmentLine]s. For example,
 * they can be used by `Row`, to align its children by baseline, or by `paddingFrom` to
 * achieve a layout with a specific from the top to the baseline of the text content.
 * The positions of the alignment lines will be automatically inherited by parent layouts from
 * their content, and the [merger] will be used to merge multiple line positions when more
 * than one child provides a specific [HorizontalAlignmentLine]. See [AlignmentLine]
 * for more details.
 *
 * @param merger How to merge two alignment line values defined by different children
 */
class HorizontalAlignmentLine(merger: (Int, Int) -> Int) : AlignmentLine(merger)

/**
 * [AlignmentLine] defined by the baseline of a first line of a [androidx.foundation.text.CoreText]
 */
val FirstBaseline = HorizontalAlignmentLine(::min)

/**
 * [AlignmentLine] defined by the baseline of the last line of a [androidx.foundation.text.CoreText]
 */
val LastBaseline = HorizontalAlignmentLine(::max)
