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
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrain

/**
 * Declare the preferred width of the content to be the same as the min or max intrinsic width of
 * the content. The incoming measurement [Constraints] may override this value, forcing the content
 * to be either smaller or larger.
 *
 * See [height] for options of sizing to intrinsic height.
 * Also see [width] and [widthIn] for other options to set the preferred width.
 *
 * Example usage for min intrinsic:
 * @sample androidx.compose.foundation.layout.samples.SameWidthBoxes
 *
 * Example usage for max intrinsic:
 * @sample androidx.compose.foundation.layout.samples.SameWidthTextBoxes
 */
@Stable
fun Modifier.width(intrinsicSize: IntrinsicSize) = when (intrinsicSize) {
    IntrinsicSize.Min -> this.then(MinIntrinsicWidthModifier)
    IntrinsicSize.Max -> this.then(MaxIntrinsicWidthModifier)
}

/**
 * Declare the preferred height of the content to be the same as the min or max intrinsic height of
 * the content. The incoming measurement [Constraints] may override this value, forcing the content
 * to be either smaller or larger.
 *
 * See [width] for other options of sizing to intrinsic width.
 * Also see [height] and [heightIn] for other options to set the preferred height.
 *
 * Example usage for min intrinsic:
 * @sample androidx.compose.foundation.layout.samples.MatchParentDividerForText
 *
 * Example usage for max intrinsic:
 * @sample androidx.compose.foundation.layout.samples.MatchParentDividerForAspectRatio
 */
@Stable
fun Modifier.height(intrinsicSize: IntrinsicSize) = when (intrinsicSize) {
    IntrinsicSize.Min -> this.then(MinIntrinsicHeightModifier)
    IntrinsicSize.Max -> this.then(MaxIntrinsicHeightModifier)
}

/**
 * Declare the width of the content to be exactly the same as the min or max intrinsic width of
 * the content. The incoming measurement [Constraints] will not override this value. If the content
 * intrinsic width does not satisfy the incoming [Constraints], the parent layout will be
 * reported a size coerced in the [Constraints], and the position of the content will be
 * automatically offset to be centered on the space assigned to the child by the parent layout under
 * the assumption that [Constraints] were respected.
 *
 * See [height] for options of sizing to intrinsic height.
 * See [width] and [widthIn] for options to set the preferred width.
 * See [requiredWidth] and [requiredWidthIn] for other options to set the required width.
 */
@Stable
fun Modifier.requiredWidth(intrinsicSize: IntrinsicSize) = when (intrinsicSize) {
    IntrinsicSize.Min -> this.then(RequiredMinIntrinsicWidthModifier)
    IntrinsicSize.Max -> this.then(RequiredMaxIntrinsicWidthModifier)
}

/**
 * Declare the height of the content to be exactly the same as the min or max intrinsic height of
 * the content. The incoming measurement [Constraints] will not override this value. If the content
 * intrinsic height does not satisfy the incoming [Constraints], the parent layout will be
 * reported a size coerced in the [Constraints], and the position of the content will be
 * automatically offset to be centered on the space assigned to the child by the parent layout under
 * the assumption that [Constraints] were respected.
 *
 * See [width] for options of sizing to intrinsic width.
 * See [height] and [heightIn] for options to set the preferred height.
 * See [requiredHeight] and [requiredHeightIn] for other options to set the required height.
 */
@Stable
fun Modifier.requiredHeight(intrinsicSize: IntrinsicSize) = when (intrinsicSize) {
    IntrinsicSize.Min -> this.then(RequiredMinIntrinsicHeightModifier)
    IntrinsicSize.Max -> this.then(RequiredMaxIntrinsicHeightModifier)
}

/**
 * Intrinsic size used in [width] or [height] which can refer to width or height.
 */
enum class IntrinsicSize { Min, Max }

private object MinIntrinsicWidthModifier : IntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val width = measurable.minIntrinsicWidth(constraints.maxHeight)
        return Constraints.fixedWidth(width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)
}

private object MinIntrinsicHeightModifier : IntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val height = measurable.minIntrinsicHeight(constraints.maxWidth)
        return Constraints.fixedHeight(height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)
}

private object MaxIntrinsicWidthModifier : IntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val width = measurable.maxIntrinsicWidth(constraints.maxHeight)
        return Constraints.fixedWidth(width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)
}

private object MaxIntrinsicHeightModifier : IntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val height = measurable.maxIntrinsicHeight(constraints.maxWidth)
        return Constraints.fixedHeight(height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}

private object RequiredMinIntrinsicWidthModifier : IntrinsicSizeModifier {
    override val enforceIncoming: Boolean = false

    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val width = measurable.minIntrinsicWidth(constraints.maxHeight)
        return Constraints.fixedWidth(width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)
}

private object RequiredMinIntrinsicHeightModifier : IntrinsicSizeModifier {
    override val enforceIncoming: Boolean = false

    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val height = measurable.minIntrinsicHeight(constraints.maxWidth)
        return Constraints.fixedHeight(height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)
}

private object RequiredMaxIntrinsicWidthModifier : IntrinsicSizeModifier {
    override val enforceIncoming: Boolean = false

    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val width = measurable.maxIntrinsicWidth(constraints.maxHeight)
        return Constraints.fixedWidth(width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)
}

private object RequiredMaxIntrinsicHeightModifier : IntrinsicSizeModifier {
    override val enforceIncoming: Boolean = false

    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val height = measurable.maxIntrinsicHeight(constraints.maxWidth)
        return Constraints.fixedHeight(height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}

private interface IntrinsicSizeModifier : LayoutModifier {
    val enforceIncoming: Boolean get() = true

    fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val contentConstraints = calculateContentConstraints(measurable, constraints)
        val placeable = measurable.measure(
            if (enforceIncoming) constraints.constrain(contentConstraints) else contentConstraints
        )
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(IntOffset.Zero)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}
