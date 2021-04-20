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
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Declare the preferred width of the content to be exactly [width]dp. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * For a modifier that sets the width of the content regardless of the incoming constraints see
 * [Modifier.requiredWidth]. See [height] or [size] to set other preferred dimensions.
 * See [widthIn], [heightIn] or [sizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWidthModifier
 */
@Stable
fun Modifier.width(width: Dp) = this.then(
    SizeModifier(
        minWidth = width,
        maxWidth = width,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "width"
            value = width
        }
    )
)

/**
 * Declare the preferred height of the content to be exactly [height]dp. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * For a modifier that sets the height of the content regardless of the incoming constraints see
 * [Modifier.requiredHeight]. See [width] or [size] to set other preferred dimensions.
 * See [widthIn], [heightIn] or [sizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleHeightModifier
 */
@Stable
fun Modifier.height(height: Dp) = this.then(
    SizeModifier(
        minHeight = height,
        maxHeight = height,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "height"
            value = height
        }
    )
)

/**
 * Declare the preferred size of the content to be exactly [size]dp square. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * For a modifier that sets the size of the content regardless of the incoming constraints, see
 * [Modifier.requiredSize]. See [width] or [height] to set width or height alone.
 * See [widthIn], [heightIn] or [sizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleSizeModifier
 */
@Stable
fun Modifier.size(size: Dp) = this.then(
    SizeModifier(
        minWidth = size,
        maxWidth = size,
        minHeight = size,
        maxHeight = size,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "size"
            value = size
        }
    )
)

/**
 * Declare the preferred size of the content to be exactly [width]dp by [height]dp. The incoming
 * measurement [Constraints] may override this value, forcing the content to be either smaller or
 * larger.
 *
 * For a modifier that sets the size of the content regardless of the incoming constraints, see
 * [Modifier.requiredSize]. See [width] or [height] to set width or height alone.
 * See [widthIn], [heightIn] or [sizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleSizeModifier
 */
@Stable
fun Modifier.size(width: Dp, height: Dp) = this.then(
    SizeModifier(
        minWidth = width,
        maxWidth = width,
        minHeight = height,
        maxHeight = height,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "size"
            properties["width"] = width
            properties["height"] = height
        }
    )
)

/**
 * Constrain the width of the content to be between [min]dp and [max]dp as permitted
 * by the incoming measurement [Constraints]. If the incoming constraints are more restrictive
 * the requested size will obey the incoming constraints and attempt to be as close as possible
 * to the preferred size.
 */
@Stable
fun Modifier.widthIn(
    min: Dp = Dp.Unspecified,
    max: Dp = Dp.Unspecified
) = this.then(
    SizeModifier(
        minWidth = min,
        maxWidth = max,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "widthIn"
            properties["min"] = min
            properties["max"] = max
        }
    )
)

/**
 * Constrain the height of the content to be between [min]dp and [max]dp as permitted
 * by the incoming measurement [Constraints]. If the incoming constraints are more restrictive
 * the requested size will obey the incoming constraints and attempt to be as close as possible
 * to the preferred size.
 */
@Stable
fun Modifier.heightIn(
    min: Dp = Dp.Unspecified,
    max: Dp = Dp.Unspecified
) = this.then(
    SizeModifier(
        minHeight = min,
        maxHeight = max,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "heightIn"
            properties["min"] = min
            properties["max"] = max
        }
    )
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp and the height
 * of the content to be between [minHeight]dp and [maxHeight]dp as permitted by the incoming
 * measurement [Constraints]. If the incoming constraints are more restrictive the requested size
 * will obey the incoming constraints and attempt to be as close as possible to the preferred size.
 */
@Stable
fun Modifier.sizeIn(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = this.then(
    SizeModifier(
        minWidth = minWidth,
        minHeight = minHeight,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        enforceIncoming = true,
        inspectorInfo = debugInspectorInfo {
            name = "sizeIn"
            properties["minWidth"] = minWidth
            properties["minHeight"] = minHeight
            properties["maxWidth"] = maxWidth
            properties["maxHeight"] = maxHeight
        }
    )
)

/**
 * Declare the width of the content to be exactly [width]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [requiredWidthIn] and [requiredSizeIn] to set a size range.
 * See [width] to set a preferred width, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleRequiredWidthModifier
 */
@Stable
fun Modifier.requiredWidth(width: Dp) = this.then(
    SizeModifier(
        minWidth = width,
        maxWidth = width,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredWidth"
            value = width
        }
    )
)

/**
 * Declare the height of the content to be exactly [height]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [requiredHeightIn] and [requiredSizeIn] to set a size range.
 * See [height] to set a preferred height, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleRequiredHeightModifier
 */
@Stable
fun Modifier.requiredHeight(height: Dp) = this.then(
    SizeModifier(
        minHeight = height,
        maxHeight = height,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredHeight"
            value = height
        }
    )
)

/**
 * Declare the size of the content to be exactly [size]dp width and height. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [requiredSizeIn] to set a size range.
 * See [size] to set a preferred size, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleRequiredSizeModifier
 */
@Stable
fun Modifier.requiredSize(size: Dp) = this.then(
    SizeModifier(
        minWidth = size,
        maxWidth = size,
        minHeight = size,
        maxHeight = size,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredSize"
            value = size
        }
    )
)

/**
 * Declare the size of the content to be exactly [width]dp and [height]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [requiredSizeIn] to set a size range.
 * See [size] to set a preferred size, which is only respected when the incoming
 * constraints allow it.
 */
@Stable
fun Modifier.requiredSize(width: Dp, height: Dp) = this.then(
    SizeModifier(
        minWidth = width,
        maxWidth = width,
        minHeight = height,
        maxHeight = height,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredSize"
            properties["width"] = width
            properties["height"] = height
        }
    )
)

/**
 * Constrain the width of the content to be between [min]dp and [max]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
@Stable
fun Modifier.requiredWidthIn(
    min: Dp = Dp.Unspecified,
    max: Dp = Dp.Unspecified
) = this.then(
    SizeModifier(
        minWidth = min,
        maxWidth = max,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredWidthIn"
            properties["min"] = min
            properties["max"] = max
        }
    )
)

/**
 * Constrain the height of the content to be between [min]dp and [max]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
@Stable
fun Modifier.requiredHeightIn(
    min: Dp = Dp.Unspecified,
    max: Dp = Dp.Unspecified
) = this.then(
    SizeModifier(
        minHeight = min,
        maxHeight = max,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredHeightIn"
            properties["min"] = min
            properties["max"] = max
        }
    )
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
fun Modifier.requiredSizeIn(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = this.then(
    SizeModifier(
        minWidth = minWidth,
        minHeight = minHeight,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        enforceIncoming = false,
        inspectorInfo = debugInspectorInfo {
            name = "requiredSizeIn"
            properties["minWidth"] = minWidth
            properties["minHeight"] = minHeight
            properties["maxWidth"] = maxWidth
            properties["maxHeight"] = maxHeight
        }
    )
)

/**
 * Have the content fill (possibly only partially) the [Constraints.maxWidth] of the incoming
 * measurement constraints, by setting the [minimum width][Constraints.minWidth] and the
 * [maximum width][Constraints.maxWidth] to be equal to the [maximum width][Constraints.maxWidth]
 * multiplied by [fraction]. Note that, by default, the [fraction] is 1, so the modifier will
 * make the content fill the whole available width. If the incoming maximum width is
 * [Constraints.Infinity] this modifier will have no effect.
 *
 * @param fraction The fraction of the maximum width to use, between `0` and `1`, inclusive.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleFillWidthModifier
 * @sample androidx.compose.foundation.layout.samples.FillHalfWidthModifier
 */
@Stable
@Suppress("ModifierInspectorInfo")
fun Modifier.fillMaxWidth(/*@FloatRange(from = 0.0, to = 1.0)*/ fraction: Float = 1f) =
    this.then(if (fraction == 1f) FillWholeMaxWidth else createFillWidthModifier(fraction))

private val FillWholeMaxWidth = createFillWidthModifier(1f)

/**
 * Have the content fill (possibly only partially) the [Constraints.maxHeight] of the incoming
 * measurement constraints, by setting the [minimum height][Constraints.minHeight] and the
 * [maximum height][Constraints.maxHeight] to be equal to the
 * [maximum height][Constraints.maxHeight] multiplied by [fraction]. Note that, by default,
 * the [fraction] is 1, so the modifier will make the content fill the whole available height.
 * If the incoming maximum height is [Constraints.Infinity] this modifier will have no effect.
 *
 * @param fraction The fraction of the maximum height to use, between `0` and `1`, inclusive.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleFillHeightModifier
 * @sample androidx.compose.foundation.layout.samples.FillHalfHeightModifier
 */
@Stable
@Suppress("ModifierInspectorInfo")
fun Modifier.fillMaxHeight(/*@FloatRange(from = 0.0, to = 1.0)*/ fraction: Float = 1f) =
    this.then(if (fraction == 1f) FillWholeMaxHeight else createFillHeightModifier(fraction))

private val FillWholeMaxHeight = createFillHeightModifier(1f)

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
 * @param fraction The fraction of the maximum size to use, between `0` and `1`, inclusive.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleFillModifier
 * @sample androidx.compose.foundation.layout.samples.FillHalfSizeModifier
 */
@Stable
@Suppress("ModifierInspectorInfo")
fun Modifier.fillMaxSize(/*@FloatRange(from = 0.0, to = 1.0)*/ fraction: Float = 1f) =
    this.then(if (fraction == 1f) FillWholeMaxSize else createFillSizeModifier(fraction))

private val FillWholeMaxSize = createFillSizeModifier(1f)

/**
 * Allow the content to measure at its desired width without regard for the incoming measurement
 * [minimum width constraint][Constraints.minWidth], and, if [unbounded] is true, also without
 * regard for the incoming measurement [maximum width constraint][Constraints.maxWidth]. If
 * the content's measured size is smaller than the minimum width constraint, [align]
 * it within that minimum width space. If the content's measured size is larger than the maximum
 * width constraint (only possible when [unbounded] is true), [align] over the maximum
 * width space.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWrapContentHorizontallyAlignedModifier
 */
@Stable
@Suppress("ModifierInspectorInfo")
fun Modifier.wrapContentWidth(
    align: Alignment.Horizontal = Alignment.CenterHorizontally,
    unbounded: Boolean = false
) = this.then(
    if (align == Alignment.CenterHorizontally && !unbounded) {
        WrapContentWidthCenter
    } else if (align == Alignment.Start && !unbounded) {
        WrapContentWidthStart
    } else {
        createWrapContentWidthModifier(align, unbounded)
    }
)

private val WrapContentWidthCenter =
    createWrapContentWidthModifier(Alignment.CenterHorizontally, false)
private val WrapContentWidthStart = createWrapContentWidthModifier(Alignment.Start, false)

/**
 * Allow the content to measure at its desired height without regard for the incoming measurement
 * [minimum height constraint][Constraints.minHeight], and, if [unbounded] is true, also without
 * regard for the incoming measurement [maximum height constraint][Constraints.maxHeight]. If the
 * content's measured size is smaller than the minimum height constraint, [align] it within
 * that minimum height space. If the content's measured size is larger than the maximum height
 * constraint (only possible when [unbounded] is true), [align] over the maximum height space.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWrapContentVerticallyAlignedModifier
 */
@Stable
@Suppress("ModifierInspectorInfo")
fun Modifier.wrapContentHeight(
    align: Alignment.Vertical = Alignment.CenterVertically,
    unbounded: Boolean = false
) = this.then(
    if (align == Alignment.CenterVertically && !unbounded) {
        WrapContentHeightCenter
    } else if (align == Alignment.Top && !unbounded) {
        WrapContentHeightTop
    } else {
        createWrapContentHeightModifier(align, unbounded)
    }
)

private val WrapContentHeightCenter =
    createWrapContentHeightModifier(Alignment.CenterVertically, false)
private val WrapContentHeightTop = createWrapContentHeightModifier(Alignment.Top, false)

/**
 * Allow the content to measure at its desired size without regard for the incoming measurement
 * [minimum width][Constraints.minWidth] or [minimum height][Constraints.minHeight] constraints,
 * and, if [unbounded] is true, also without regard for the incoming maximum constraints.
 * If the content's measured size is smaller than the minimum size constraint, [align] it
 * within that minimum sized space. If the content's measured size is larger than the maximum
 * size constraint (only possible when [unbounded] is true), [align] within the maximum space.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleWrapContentAlignedModifier
 */
@Stable
@Suppress("ModifierInspectorInfo")
fun Modifier.wrapContentSize(
    align: Alignment = Alignment.Center,
    unbounded: Boolean = false
) = this.then(
    if (align == Alignment.Center && !unbounded) {
        WrapContentSizeCenter
    } else if (align == Alignment.TopStart && !unbounded) {
        WrapContentSizeTopStart
    } else {
        createWrapContentSizeModifier(align, unbounded)
    }
)

private val WrapContentSizeCenter = createWrapContentSizeModifier(Alignment.Center, false)
private val WrapContentSizeTopStart = createWrapContentSizeModifier(Alignment.TopStart, false)

/**
 * Constrain the size of the wrapped layout only when it would be otherwise unconstrained:
 * the [minWidth] and [minHeight] constraints are only applied when the incoming corresponding
 * constraint is `0`.
 * The modifier can be used, for example, to define a default min size of a component,
 * while still allowing it to be overidden with smaller min sizes across usages.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.DefaultMinSizeSample
 */
@Stable
fun Modifier.defaultMinSize(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified
) = this.then(
    UnspecifiedConstraintsModifier(
        minWidth = minWidth,
        minHeight = minHeight,
        inspectorInfo = debugInspectorInfo {
            name = "defaultMinSize"
            properties["minWidth"] = minWidth
            properties["minHeight"] = minHeight
        }
    )
)

private class FillModifier(
    private val direction: Direction,
    private val fraction: Float,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val minWidth: Int
        val maxWidth: Int
        if (constraints.hasBoundedWidth && direction != Direction.Vertical) {
            val width = (constraints.maxWidth * fraction).roundToInt()
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
            val height = (constraints.maxHeight * fraction).roundToInt()
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

    override fun equals(other: Any?) =
        other is FillModifier && direction == other.direction && fraction == other.fraction

    override fun hashCode() = direction.hashCode() * 31 + fraction.hashCode()
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createFillWidthModifier(fraction: Float) = FillModifier(
    direction = Direction.Horizontal,
    fraction = fraction,
    inspectorInfo = {
        name = "fillMaxWidth"
        properties["fraction"] = fraction
    }
)

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createFillHeightModifier(fraction: Float) = FillModifier(
    direction = Direction.Vertical,
    fraction = fraction,
    inspectorInfo = {
        name = "fillMaxHeight"
        properties["fraction"] = fraction
    }
)

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createFillSizeModifier(fraction: Float) = FillModifier(
    direction = Direction.Both,
    fraction = fraction,
    inspectorInfo = {
        name = "fillMaxSize"
        properties["fraction"] = fraction
    }
)

private class SizeModifier(
    private val minWidth: Dp = Dp.Unspecified,
    private val minHeight: Dp = Dp.Unspecified,
    private val maxWidth: Dp = Dp.Unspecified,
    private val maxHeight: Dp = Dp.Unspecified,
    private val enforceIncoming: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    private val Density.targetConstraints: Constraints
        get() {
            val maxWidth = if (maxWidth != Dp.Unspecified) {
                maxWidth.coerceAtLeast(0.dp).roundToPx()
            } else {
                Constraints.Infinity
            }
            val maxHeight = if (maxHeight != Dp.Unspecified) {
                maxHeight.coerceAtLeast(0.dp).roundToPx()
            } else {
                Constraints.Infinity
            }
            val minWidth = if (minWidth != Dp.Unspecified) {
                minWidth.roundToPx().coerceAtMost(maxWidth).coerceAtLeast(0).let {
                    if (it != Constraints.Infinity) it else 0
                }
            } else {
                0
            }
            val minHeight = if (minHeight != Dp.Unspecified) {
                minHeight.roundToPx().coerceAtMost(maxHeight).coerceAtLeast(0).let {
                    if (it != Constraints.Infinity) it else 0
                }
            } else {
                0
            }
            return Constraints(
                minWidth = minWidth,
                minHeight = minHeight,
                maxWidth = maxWidth,
                maxHeight = maxHeight
            )
        }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val wrappedConstraints = targetConstraints.let { targetConstraints ->
            if (enforceIncoming) {
                constraints.constrain(targetConstraints)
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
    ): Int {
        val constraints = targetConstraints
        return if (constraints.hasFixedWidth) {
            constraints.maxWidth
        } else {
            constraints.constrainWidth(measurable.minIntrinsicWidth(height))
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val constraints = targetConstraints
        return if (constraints.hasFixedHeight) {
            constraints.maxHeight
        } else {
            constraints.constrainHeight(measurable.minIntrinsicHeight(width))
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val constraints = targetConstraints
        return if (constraints.hasFixedWidth) {
            constraints.maxWidth
        } else {
            constraints.constrainWidth(measurable.maxIntrinsicWidth(height))
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val constraints = targetConstraints
        return if (constraints.hasFixedHeight) {
            constraints.maxHeight
        } else {
            constraints.constrainHeight(measurable.maxIntrinsicHeight(width))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SizeModifier) return false
        return minWidth == other.minWidth &&
            minHeight == other.minHeight &&
            maxWidth == other.maxWidth &&
            maxHeight == other.maxHeight &&
            enforceIncoming == other.enforceIncoming
    }

    override fun hashCode() =
        (
            (
                (((minWidth.hashCode() * 31 + minHeight.hashCode()) * 31) + maxWidth.hashCode()) *
                    31
                ) + maxHeight.hashCode()
            ) * 31
}

private class WrapContentModifier(
    private val direction: Direction,
    private val unbounded: Boolean,
    private val alignmentCallback: (IntSize, LayoutDirection) -> IntOffset,
    private val align: Any, // only used for equals and hashcode
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val wrappedConstraints = Constraints(
            minWidth = if (direction != Direction.Vertical) 0 else constraints.minWidth,
            minHeight = if (direction != Direction.Horizontal) 0 else constraints.minHeight,
            maxWidth = if (direction != Direction.Vertical && unbounded) {
                Constraints.Infinity
            } else {
                constraints.maxWidth
            },
            maxHeight = if (direction != Direction.Horizontal && unbounded) {
                Constraints.Infinity
            } else {
                constraints.maxHeight
            }
        )
        val placeable = measurable.measure(wrappedConstraints)
        val wrapperWidth = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val wrapperHeight = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)
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

    override fun equals(other: Any?): Boolean {
        if (other !is WrapContentModifier) return false
        return direction == other.direction && unbounded == other.unbounded && align == other.align
    }

    override fun hashCode() =
        (direction.hashCode() * 31 + unbounded.hashCode()) * 31 + align.hashCode()
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createWrapContentWidthModifier(
    align: Alignment.Horizontal,
    unbounded: Boolean
) = WrapContentModifier(
    direction = Direction.Horizontal,
    unbounded = unbounded,
    alignmentCallback = { size, layoutDirection ->
        IntOffset(align.align(0, size.width, layoutDirection), 0)
    },
    align,
    inspectorInfo = {
        name = "wrapContentWidth"
        properties["align"] = align
        properties["unbounded"] = unbounded
    }
)

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createWrapContentHeightModifier(
    align: Alignment.Vertical,
    unbounded: Boolean
) = WrapContentModifier(
    direction = Direction.Vertical,
    unbounded = unbounded,
    alignmentCallback = { size, _ ->
        IntOffset(0, align.align(0, size.height))
    },
    align,
    inspectorInfo = {
        name = "wrapContentHeight"
        properties["align"] = align
        properties["unbounded"] = unbounded
    }
)

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createWrapContentSizeModifier(
    align: Alignment,
    unbounded: Boolean
) = WrapContentModifier(
    direction = Direction.Both,
    unbounded = unbounded,
    alignmentCallback = { size, layoutDirection ->
        align.align(IntSize.Zero, size, layoutDirection)
    },
    align,
    inspectorInfo = {
        name = "wrapContentSize"
        properties["align"] = align
        properties["unbounded"] = unbounded
    }
)

private class UnspecifiedConstraintsModifier(
    val minWidth: Dp = Dp.Unspecified,
    val minHeight: Dp = Dp.Unspecified,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val wrappedConstraints = Constraints(
            if (minWidth != Dp.Unspecified && constraints.minWidth == 0) {
                minWidth.roundToPx().coerceAtMost(constraints.maxWidth).coerceAtLeast(0)
            } else {
                constraints.minWidth
            },
            constraints.maxWidth,
            if (minHeight != Dp.Unspecified && constraints.minHeight == 0) {
                minHeight.roundToPx().coerceAtMost(constraints.maxHeight).coerceAtLeast(0)
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
        if (minWidth != Dp.Unspecified) minWidth.roundToPx() else 0
    )

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height).coerceAtLeast(
        if (minWidth != Dp.Unspecified) minWidth.roundToPx() else 0
    )

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width).coerceAtLeast(
        if (minHeight != Dp.Unspecified) minHeight.roundToPx() else 0
    )

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width).coerceAtLeast(
        if (minHeight != Dp.Unspecified) minHeight.roundToPx() else 0
    )

    override fun equals(other: Any?): Boolean {
        if (other !is UnspecifiedConstraintsModifier) return false
        return minWidth == other.minWidth && minHeight == other.minHeight
    }

    override fun hashCode() = minWidth.hashCode() * 31 + minHeight.hashCode()
}

internal enum class Direction {
    Vertical, Horizontal, Both
}
