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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.enforce
import androidx.compose.ui.Alignment
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.platform.InspectableParameter
import androidx.compose.ui.platform.ParameterElement
import androidx.compose.ui.util.annotation.FloatRange
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Declare the preferred width of the content to be exactly [width]dp. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * For a modifier that sets the width of the content regardless of the incoming constraints see
 * [Modifier.width]. See [preferredHeight] or [preferredSize] to set other preferred dimensions.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimplePreferredWidthModifier
 */
@Stable
fun Modifier.preferredWidth(width: Dp) = preferredSizeIn(minWidth = width, maxWidth = width)

/**
 * Declare the preferred height of the content to be exactly [height]dp. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * For a modifier that sets the height of the content regardless of the incoming constraints see
 * [Modifier.height]. See [preferredWidth] or [preferredSize] to set other preferred dimensions.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimplePreferredHeightModifier
 */
@Stable
fun Modifier.preferredHeight(height: Dp) = preferredSizeIn(minHeight = height, maxHeight = height)

/**
 * Declare the preferred size of the content to be exactly [size]dp square. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * For a modifier that sets the size of the content regardless of the incoming constraints, see
 * [Modifier.size]. See [preferredWidth] or [preferredHeight] to set width or height alone.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimplePreferredSizeModifier
 */
@Stable
fun Modifier.preferredSize(size: Dp) = preferredSizeIn(size, size, size, size)

/**
 * Declare the preferred size of the content to be exactly [width]dp by [height]dp. The incoming
 * measurement [Constraints] may override this value, forcing the content to be either smaller or
 * larger.
 *
 * For a modifier that sets the size of the content regardless of the incoming constraints, see
 * [Modifier.size]. See [preferredWidth] or [preferredHeight] to set width or height alone.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimplePreferredSizeModifier
 */
@Stable
fun Modifier.preferredSize(width: Dp, height: Dp) = preferredSizeIn(
    minWidth = width,
    maxWidth = width,
    minHeight = height,
    maxHeight = height
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp as permitted
 * by the incoming measurement [Constraints]. If the incoming constraints are more restrictive
 * the requested size will obey the incoming constraints and attempt to be as close as possible
 * to the preferred size.
 */
@Stable
fun Modifier.preferredWidthIn(
    minWidth: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified
) = preferredSizeIn(minWidth = minWidth, maxWidth = maxWidth)

/**
 * Constrain the height of the content to be between [minHeight]dp and [maxHeight]dp as permitted
 * by the incoming measurement [Constraints]. If the incoming constraints are more restrictive
 * the requested size will obey the incoming constraints and attempt to be as close as possible
 * to the preferred size.
 */
@Stable
fun Modifier.preferredHeightIn(
    minHeight: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = preferredSizeIn(minHeight = minHeight, maxHeight = maxHeight)

/**
 * Constrain the size of the content to be within [constraints] as permitted by the incoming
 * measurement [Constraints]. If the incoming measurement constraints are more restrictive the
 * requested size will obey the incoming constraints and attempt to be as close as possible to
 * the preferred size.
 */
@Stable
fun Modifier.preferredSizeIn(constraints: DpConstraints) = preferredSizeIn(
    constraints.minWidth,
    constraints.minHeight,
    constraints.maxWidth,
    constraints.maxHeight
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp and the height
 * of the content to be between [minHeight] and [maxHeight] as permitted by the incoming
 * measurement [Constraints]. If the incoming constraints are more restrictive the requested size
 * will obey the incoming constraints and attempt to be as close as possible to the preferred size.
 */
@Stable
fun Modifier.preferredSizeIn(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = this.then(SizeModifier(minWidth, minHeight, maxWidth, maxHeight, true))

/**
 * Declare the width of the content to be exactly [width]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [widthIn] and [sizeIn] to set a size range.
 * See [preferredWidth] to set a preferred width, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWidthModifier
 */
@Stable
fun Modifier.width(width: Dp) = sizeIn(minWidth = width, maxWidth = width)

/**
 * Declare the height of the content to be exactly [height]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [heightIn] and [sizeIn] to set a size range.
 * See [preferredHeight] to set a preferred height, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleHeightModifier
 */
@Stable
fun Modifier.height(height: Dp) = sizeIn(minHeight = height, maxHeight = height)

/**
 * Declare the size of the content to be exactly [size]dp width and height. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [sizeIn] to set a size range.
 * See [preferredSize] to set a preferred size, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleSizeModifier
 */
@Stable
fun Modifier.size(size: Dp) = sizeIn(size, size, size, size)

/**
 * Declare the size of the content to be exactly [width]dp and [height]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [sizeIn] to set a size range.
 * See [preferredSize] to set a preferred size, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWidthModifier
 */
@Stable
fun Modifier.size(width: Dp, height: Dp) = sizeIn(
    minWidth = width,
    maxWidth = width,
    minHeight = height,
    maxHeight = height
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
@Stable
fun Modifier.widthIn(
    minWidth: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified
) = sizeIn(minWidth = minWidth, maxWidth = maxWidth)

/**
 * Constrain the height of the content to be between [minHeight]dp and [maxHeight]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
@Stable
fun Modifier.heightIn(
    minHeight: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = sizeIn(minHeight = minHeight, maxHeight = maxHeight)

/**
 * Constrain the size of the content to be within [constraints].
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
@Stable
fun Modifier.sizeIn(constraints: DpConstraints) = sizeIn(
    constraints.minWidth,
    constraints.minHeight,
    constraints.maxWidth,
    constraints.maxHeight
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp, and the
 * height of the content to be between [minHeight]dp and [maxHeight]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
@Stable
fun Modifier.sizeIn(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = this.then(SizeModifier(minWidth, minHeight, maxWidth, maxHeight, false))

/**
 * Have the content fill (possibly only partially) the [Constraints.maxWidth] of the incoming
 * measurement constraints, by setting the [minimum width][Constraints.minWidth] and the
 * [maximum width][Constraints.maxWidth] to be equal to the [maximum width][Constraints.maxWidth]
 * multiplied by [fraction]. Note that, by default, the [fraction] is 1, so the modifier will
 * make the content fill the whole available width. If the incoming maximum width is
 * [Constraints.Infinity] this modifier will have no effect.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleFillWidthModifier
 * @sample androidx.compose.foundation.layout.samples.FillHalfWidthModifier
 */
@Stable
fun Modifier.fillMaxWidth(@FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f) =
    this.then(FillModifier(Direction.Horizontal, fraction))

/**
 * Have the content fill (possibly only partially) the [Constraints.maxHeight] of the incoming
 * measurement constraints, by setting the [minimum height][Constraints.minHeight] and the
 * [maximum height][Constraints.maxHeight] to be equal to the
 * [maximum height][Constraints.maxHeight] multiplied by [fraction]. Note that, by default,
 * the [fraction] is 1, so the modifier will make the content fill the whole available height.
 * If the incoming maximum height is [Constraints.Infinity] this modifier will have no effect.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleFillHeightModifier
 * @sample androidx.compose.foundation.layout.samples.FillHalfHeightModifier
 */
@Stable
fun Modifier.fillMaxHeight(@FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f) =
    this.then(FillModifier(Direction.Vertical, fraction))

/**
 * Have the content fill (possibly only partially) the [Constraints.maxWidth] and
 * [Constraints.maxHeight] of the incoming measurement constraints, by setting the
 * [minimum width][Constraints.minWidth] and the [maximum width][Constraints.maxWidth] to be
 * equal to the [maximum width][Constraints.maxWidth] multiplied by [fraction], as well as
 * the [minimum height][Constraints.minHeight] and the [maximum height][Constraints.minHeight]
 * to be equal to the [maximum height][Constraints.maxHeight] multiplied by [fraction].
 * Note that, by default, the [fraction] is 1, so the modifier will make the content fill
 * the whole available space.
 * If the incoming maximum width or height is [Constraints.Infinity] this modifier will have no
 * effect in that dimension.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleFillModifier
 * @sample androidx.compose.foundation.layout.samples.FillHalfSizeModifier
 */
@Stable
fun Modifier.fillMaxSize(@FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f) =
    this.then(FillModifier(Direction.Both, fraction))

/**
 * Allow the content to measure at its desired width without regard for the incoming measurement
 * [minimum width constraint][Constraints.minWidth]. If the content's measured size is smaller
 * than the minimum width constraint, [align] it within that minimum width space.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWrapContentHorizontallyAlignedModifier
 */
@Stable
// TODO(popam): avoid recreating modifier for common align
fun Modifier.wrapContentWidth(align: Alignment.Horizontal = Alignment.CenterHorizontally) =
    this.then(AlignmentModifier(Direction.Horizontal, align) { size, layoutDirection ->
        IntOffset(align.align(size.width, layoutDirection), 0)
    })

/**
 * Allow the content to measure at its desired height without regard for the incoming measurement
 * [minimum height constraint][Constraints.minHeight]. If the content's measured size is smaller
 * than the minimum height constraint, [align] it within that minimum height space.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWrapContentVerticallyAlignedModifier
 */
// TODO(popam): avoid recreating modifier for common align
@Stable
fun Modifier.wrapContentHeight(align: Alignment.Vertical = Alignment.CenterVertically) =
    this.then(AlignmentModifier(Direction.Vertical, align) { size, _ ->
        IntOffset(0, align.align(size.height))
    })

/**
 * Allow the content to measure at its desired size without regard for the incoming measurement
 * [minimum width][Constraints.minWidth] or [minimum height][Constraints.minHeight] constraints.
 * If the content's measured size is smaller than the minimum size constraint, [align] it
 * within that minimum sized space.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWrapContentAlignedModifier
 */
@Stable
fun Modifier.wrapContentSize(align: Alignment = Alignment.Center) =
    this.then(AlignmentModifier(Direction.Both, align) { size, layoutDirection ->
        align.align(size, layoutDirection)
    })

/**
 * Constrain the size of the wrapped layout only when it would be otherwise unconstrained:
 * the [minWidth] and [minHeight] constraints are only applied when the incoming corresponding
 * constraint is `0`.
 * The modifier can be used, for example, to define a default min size of a component,
 * while still allowing it to be overidden with smaller min sizes across usages.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.DefaultMinSizeConstraintsSample
 */
@Stable
fun Modifier.defaultMinSizeConstraints(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified
) = this.then(UnspecifiedConstraintsModifier(minWidth, minHeight))

private data class FillModifier(
    private val direction: Direction,
    private val scale: Float
) : LayoutModifier, InspectableParameter {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val minWidth: Int
        val maxWidth: Int
        if (constraints.hasBoundedWidth && direction != Direction.Vertical) {
            val width = (constraints.maxWidth * scale).roundToInt()
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            minWidth = width
            maxWidth = width
        } else {
            minWidth = constraints.minWidth
            maxWidth = constraints.maxWidth
        }
        val minHeight: Int
        val maxHeight: Int
        if (constraints.hasBoundedHeight && direction != Direction.Horizontal) {
            val height = (constraints.maxHeight * scale).roundToInt()
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            minHeight = height
            maxHeight = height
        } else {
            minHeight = constraints.minHeight
            maxHeight = constraints.maxHeight
        }
        val placeable = measurable.measure(
            Constraints(minWidth, maxWidth, minHeight, maxHeight)
        )

        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override val nameFallback: String =
        when (direction) {
            Direction.Vertical -> "fillMaxHeight"
            Direction.Horizontal -> "fillMaxWidth"
            Direction.Both -> "fillMaxSize"
        }

    override val inspectableElements: Sequence<ParameterElement> = emptySequence()
}

private data class SizeModifier(
    private val minWidth: Dp = Dp.Unspecified,
    private val minHeight: Dp = Dp.Unspecified,
    private val maxWidth: Dp = Dp.Unspecified,
    private val maxHeight: Dp = Dp.Unspecified,
    private val enforceIncoming: Boolean
) : LayoutModifier, InspectableParameter {
    private val Density.targetConstraints
        get() = Constraints(
            minWidth = if (minWidth != Dp.Unspecified) minWidth.toIntPx() else 0,
            minHeight = if (minHeight != Dp.Unspecified) minHeight.toIntPx() else 0,
            maxWidth = if (maxWidth != Dp.Unspecified) maxWidth.toIntPx() else Constraints.Infinity,
            maxHeight = if (maxHeight != Dp.Unspecified) {
                maxHeight.toIntPx()
            } else {
                Constraints.Infinity
            }
        )

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val wrappedConstraints = targetConstraints.let { targetConstraints ->
            if (enforceIncoming) {
                targetConstraints.enforce(constraints)
            } else {
                val resolvedMinWidth = if (minWidth != Dp.Unspecified) {
                    targetConstraints.minWidth
                } else {
                    constraints.minWidth.coerceAtMost(targetConstraints.maxWidth)
                }
                val resolvedMaxWidth = if (maxWidth != Dp.Unspecified) {
                    targetConstraints.maxWidth
                } else {
                    constraints.maxWidth.coerceAtLeast(targetConstraints.minWidth)
                }
                val resolvedMinHeight = if (minHeight != Dp.Unspecified) {
                    targetConstraints.minHeight
                } else {
                    constraints.minHeight.coerceAtMost(targetConstraints.maxHeight)
                }
                val resolvedMaxHeight = if (maxHeight != Dp.Unspecified) {
                    targetConstraints.maxHeight
                } else {
                    constraints.maxHeight.coerceAtLeast(targetConstraints.minHeight)
                }
                Constraints(
                    resolvedMinWidth,
                    resolvedMaxWidth,
                    resolvedMinHeight,
                    resolvedMaxHeight
                )
            }
        }
        val placeable = measurable.measure(wrappedConstraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height).let {
        val constraints = targetConstraints
        constraints.constrainWidth(it)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height).let {
        val constraints = targetConstraints
        constraints.constrainWidth(it)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width).let {
        val constraints = targetConstraints
        constraints.constrainHeight(it)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width).let {
        val constraints = targetConstraints
        constraints.constrainHeight(it)
    }

    override val nameFallback: String
        get() {
            val name = simpleName
            return if (enforceIncoming) "preferred${simpleName.capitalize()}" else name
        }

    override val valueOverride: Any?
        get() = when (simpleName) {
            "width" -> minWidth
            "height" -> minHeight
            "size" -> if (minWidth == minHeight) minWidth else null
            else -> null
        }

    override val inspectableElements: Sequence<ParameterElement>
        get() = when (simpleName) {
            "width" -> sequenceOf(ParameterElement("width", minWidth))
            "height" -> sequenceOf(ParameterElement("height", minHeight))
            "size" ->
                if (minWidth == minHeight) {
                    sequenceOf(ParameterElement("size", minWidth))
                } else sequenceOf(
                    ParameterElement("width", minWidth),
                    ParameterElement("height", minHeight)
                )
            "widthIn" -> sequenceOf(
                ParameterElement("minWidth", minWidth),
                ParameterElement("maxWidth", maxWidth)
            )
            "heightIn" -> sequenceOf(
                ParameterElement("minHeight", minHeight),
                ParameterElement("maxHeight", maxHeight)
            )
            else -> sequenceOf(
                ParameterElement("minWidth", minWidth),
                ParameterElement("minHeight", minHeight),
                ParameterElement("maxWidth", maxWidth),
                ParameterElement("maxHeight", maxHeight)
            )
        }

    private val simpleName: String =
        if (minWidth == maxWidth && minHeight == maxHeight) {
            when {
                minHeight == Dp.Unspecified -> "width"
                minWidth == Dp.Unspecified -> "height"
                else -> "size"
            }
        } else {
            when {
                minHeight == Dp.Unspecified && maxHeight == Dp.Unspecified -> "widthIn"
                minWidth == Dp.Unspecified && maxWidth == Dp.Unspecified -> "heightIn"
                else -> "sizeIn"
            }
        }
}

private data class AlignmentModifier(
    private val direction: Direction,
    private val alignment: Any,
    private val alignmentCallback: (IntSize, LayoutDirection) -> IntOffset
) : LayoutModifier, InspectableParameter {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val wrappedConstraints = when (direction) {
            Direction.Both -> constraints.copy(minWidth = 0, minHeight = 0)
            Direction.Horizontal -> constraints.copy(minWidth = 0)
            Direction.Vertical -> constraints.copy(minHeight = 0)
        }
        val placeable = measurable.measure(wrappedConstraints)
        val wrapperWidth = max(constraints.minWidth, placeable.width)
        val wrapperHeight = max(constraints.minHeight, placeable.height)
        return layout(
            wrapperWidth,
            wrapperHeight
        ) {
            val position = alignmentCallback(
                IntSize(wrapperWidth - placeable.width, wrapperHeight - placeable.height),
                layoutDirection
            )
            placeable.place(position)
        }
    }

    override val nameFallback: String =
        when (direction) {
            Direction.Vertical -> "wrapContentHeight"
            Direction.Horizontal -> "wrapContentWidth"
            Direction.Both -> "wrapContentSize"
        }

    override val inspectableElements: Sequence<ParameterElement>
        get() = sequenceOf(ParameterElement("alignment", alignment))
}

private data class UnspecifiedConstraintsModifier(
    val minWidth: Dp = Dp.Unspecified,
    val minHeight: Dp = Dp.Unspecified
) : LayoutModifier, InspectableParameter {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val wrappedConstraints = Constraints(
            if (minWidth != Dp.Unspecified && constraints.minWidth == 0) {
                minWidth.toIntPx().coerceAtMost(constraints.maxWidth)
            } else {
                constraints.minWidth
            },
            constraints.maxWidth,
            if (minHeight != Dp.Unspecified && constraints.minHeight == 0) {
                minHeight.toIntPx().coerceAtMost(constraints.maxHeight)
            } else {
                constraints.minHeight
            },
            constraints.maxHeight
        )
        val placeable = measurable.measure(wrappedConstraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height).coerceAtLeast(
        if (minWidth != Dp.Unspecified) minWidth.toIntPx() else 0
    )

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height).coerceAtLeast(
        if (minWidth != Dp.Unspecified) minWidth.toIntPx() else 0
    )

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width).coerceAtLeast(
        if (minHeight != Dp.Unspecified) minHeight.toIntPx() else 0
    )

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width).coerceAtLeast(
        if (minHeight != Dp.Unspecified) minHeight.toIntPx() else 0
    )

    override val nameFallback = "defaultMinSizeConstraints"

    override val inspectableElements: Sequence<ParameterElement>
        get() = sequenceOf(
            ParameterElement("minWidth", minWidth),
            ParameterElement("minHeight", minHeight)
        )
}

internal enum class Direction {
    Vertical, Horizontal, Both
}
