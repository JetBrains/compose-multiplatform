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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end] and
 * [bottom]. The start and end edges will be determined by the current [LayoutDirection].
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 * See [Modifier.offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.PaddingModifier
 */
@Stable
fun Modifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this.then(
    PaddingModifier(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
        rtlAware = true,
        inspectorInfo = debugInspectorInfo {
            name = "padding"
            properties["start"] = start
            properties["top"] = top
            properties["end"] = end
            properties["bottom"] = bottom
        }
    )
)

/**
 * Apply [horizontal] dp space along the left and right edges of the content, and [vertical] dp
 * space along the top and bottom edges.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 * See [Modifier.offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SymmetricPaddingModifier
 */
@Stable
fun Modifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp
) = this.then(
    PaddingModifier(
        start = horizontal,
        top = vertical,
        end = horizontal,
        bottom = vertical,
        rtlAware = true,
        inspectorInfo = debugInspectorInfo {
            name = "padding"
            properties["horizontal"] = horizontal
            properties["vertical"] = vertical
        }
    )
)

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right and bottom.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 * See [Modifier.offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.PaddingAllModifier
 */
@Stable
fun Modifier.padding(all: Dp) =
    this.then(
        PaddingModifier(
            start = all,
            top = all,
            end = all,
            bottom = all,
            rtlAware = true,
            inspectorInfo = debugInspectorInfo {
                name = "padding"
                value = all
            }
        )
    )

/**
 * Apply [PaddingValues] to the component as additional space along each edge of the content's left,
 * top, right and bottom. Padding is applied before content measurement and takes precedence;
 * content may only be as large as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 * See [Modifier.offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.PaddingValuesModifier
 */
@Stable
fun Modifier.padding(paddingValues: PaddingValues) =
    this.then(
        PaddingValuesModifier(
            paddingValues = paddingValues,
            inspectorInfo = debugInspectorInfo {
                name = "padding"
                properties["paddingValues"] = paddingValues
            }
        )
    )

/**
 * Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom]. These paddings are applied without regard to the current [LayoutDirection], see
 * [padding] to apply relative paddings. Padding is applied before content measurement and takes
 * precedence; content may only be as large as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 * See [Modifier.offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.AbsolutePaddingModifier
 */
@Stable
fun Modifier.absolutePadding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this.then(
    PaddingModifier(
        start = left,
        top = top,
        end = right,
        bottom = bottom,
        rtlAware = false,
        inspectorInfo = debugInspectorInfo {
            name = "absolutePadding"
            properties["left"] = left
            properties["top"] = top
            properties["right"] = right
            properties["bottom"] = bottom
        }
    )
)

/**
 * Describes a padding to be applied along the edges inside a box.
 * See the [PaddingValues] factories and [Absolute] for convenient ways to
 * build [PaddingValues].
 */
@Stable
interface PaddingValues {
    /**
     * The padding to be applied along the left edge inside a box.
     */
    fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp
    /**
     * The padding to be applied along the top edge inside a box.
     */
    fun calculateTopPadding(): Dp
    /**
     * The padding to be applied along the right edge inside a box.
     */
    fun calculateRightPadding(layoutDirection: LayoutDirection): Dp
    /**
     * The padding to be applied along the bottom edge inside a box.
     */
    fun calculateBottomPadding(): Dp

    /**
     * Describes an absolute (RTL unaware) padding to be applied along the edges inside a box.
     */
    @Immutable
    class Absolute(
        @Stable
        private val left: Dp = 0.dp,
        @Stable
        private val top: Dp = 0.dp,
        @Stable
        private val right: Dp = 0.dp,
        @Stable
        private val bottom: Dp = 0.dp
    ) : PaddingValues {
        override fun calculateLeftPadding(layoutDirection: LayoutDirection) = left

        override fun calculateTopPadding() = top

        override fun calculateRightPadding(layoutDirection: LayoutDirection) = right

        override fun calculateBottomPadding() = bottom

        override fun equals(other: Any?): Boolean {
            if (other !is Absolute) return false
            return left == other.left &&
                top == other.top &&
                right == other.right &&
                bottom == other.bottom
        }

        override fun hashCode() =
            ((left.hashCode() * 31 + top.hashCode()) * 31 + right.hashCode()) *
                31 + bottom.hashCode()

        override fun toString() =
            "PaddingValues.Absolute(left=$left, top=$top, right=$right, bottom=$bottom"
    }
}

/**
 * The padding to be applied along the start edge inside a box: along the left edge if
 * the layout direction is LTR, or along the right edge for RTL.
 */
@Stable
fun PaddingValues.calculateStartPadding(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) {
        calculateLeftPadding(layoutDirection)
    } else {
        calculateRightPadding(layoutDirection)
    }

/**
 * The padding to be applied along the end edge inside a box: along the right edge if
 * the layout direction is LTR, or along the left edge for RTL.
 */
@Stable
fun PaddingValues.calculateEndPadding(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) {
        calculateRightPadding(layoutDirection)
    } else {
        calculateLeftPadding(layoutDirection)
    }

/**
 * Creates a padding of [all] dp along all 4 edges.
 */
@Stable
fun PaddingValues(all: Dp): PaddingValues = PaddingValuesImpl(all, all, all, all)

/**
 * Creates a padding of [horizontal] dp along the left and right edges, and of [vertical]
 * dp along the top and bottom edges.
 */
@Stable
fun PaddingValues(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): PaddingValues =
    PaddingValuesImpl(horizontal, vertical, horizontal, vertical)

/**
 * Creates a padding to be applied along the edges inside a box. In LTR contexts [start] will
 * be applied along the left edge and [end] will be applied along the right edge. In RTL contexts,
 * [start] will correspond to the right edge and [end] to the left.
 */
@Stable
fun PaddingValues(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
): PaddingValues = PaddingValuesImpl(start, top, end, bottom)

@Immutable
internal class PaddingValuesImpl(
    @Stable
    val start: Dp = 0.dp,
    @Stable
    val top: Dp = 0.dp,
    @Stable
    val end: Dp = 0.dp,
    @Stable
    val bottom: Dp = 0.dp
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) start else end

    override fun calculateTopPadding() = top

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) end else start

    override fun calculateBottomPadding() = bottom

    override fun equals(other: Any?): Boolean {
        if (other !is PaddingValuesImpl) return false
        return start == other.start &&
            top == other.top &&
            end == other.end &&
            bottom == other.bottom
    }

    override fun hashCode() =
        ((start.hashCode() * 31 + top.hashCode()) * 31 + end.hashCode()) * 31 + bottom.hashCode()

    override fun toString() = "PaddingValues(start=$start, top=$top, end=$end, bottom=$bottom"
}

private class PaddingModifier(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val rtlAware: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    init {
        require(
            (start.value >= 0f || start == Dp.Unspecified) &&
                (top.value >= 0f || top == Dp.Unspecified) &&
                (end.value >= 0f || end == Dp.Unspecified) &&
                (bottom.value >= 0f || bottom == Dp.Unspecified)
        ) {
            "Padding must be non-negative"
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val horizontal = start.roundToPx() + end.roundToPx()
        val vertical = top.roundToPx() + bottom.roundToPx()

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) {
            if (rtlAware) {
                placeable.placeRelative(start.roundToPx(), top.roundToPx())
            } else {
                placeable.place(start.roundToPx(), top.roundToPx())
            }
        }
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + rtlAware.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? PaddingModifier ?: return false
        return start == otherModifier.start &&
            top == otherModifier.top &&
            end == otherModifier.end &&
            bottom == otherModifier.bottom &&
            rtlAware == otherModifier.rtlAware
    }
}

private class PaddingValuesModifier(
    val paddingValues: PaddingValues,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        require(
            paddingValues.calculateLeftPadding(LayoutDirection.Ltr) >= 0.dp &&
                paddingValues.calculateTopPadding() >= 0.dp &&
                paddingValues.calculateRightPadding(LayoutDirection.Ltr) >= 0.dp &&
                paddingValues.calculateBottomPadding() >= 0.dp
        ) {
            "Padding must be non-negative"
        }
        val horizontal = paddingValues.calculateLeftPadding(LayoutDirection.Ltr).roundToPx() +
            paddingValues.calculateRightPadding(layoutDirection).roundToPx()
        val vertical = paddingValues.calculateTopPadding().roundToPx() +
            paddingValues.calculateBottomPadding().roundToPx()

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) {
            placeable.place(
                paddingValues.calculateLeftPadding(layoutDirection).roundToPx(),
                paddingValues.calculateTopPadding().roundToPx()
            )
        }
    }

    override fun hashCode() = paddingValues.hashCode()

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? PaddingValuesModifier ?: return false
        return paddingValues == otherModifier.paddingValues
    }
}
