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
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
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
 * Negative padding is not permitted. See [offset].
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
        rtlAware = true
    )
)

/**
 * Apply [horizontal] dp space along the left and right edges of the content, and [vertical] dp
 * space along the top and bottom edges.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
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
        rtlAware = true
    )
)

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right and bottom.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.PaddingAllModifier
 */
@Stable
fun Modifier.padding(all: Dp) =
    this.then(PaddingModifier(start = all, top = all, end = all, bottom = all, rtlAware = true))

/**
 * Apply [PaddingValues] to the component as additional space along each edge of the content's left,
 * top, right and bottom. Padding is applied before content measurement and takes precedence;
 * content may only be as large as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.PaddingValuesModifier
 */
fun Modifier.padding(padding: PaddingValues) =
    this.then(
        PaddingModifier(
            start = padding.start,
            top = padding.top,
            end = padding.end,
            bottom = padding.bottom,
            rtlAware = true
        )
    )

/**
 * Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom]. These paddings are applied without regard to the current [LayoutDirection], see
 * [padding] to apply relative paddings. Padding is applied before content measurement and takes
 * precedence; content may only be as large as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
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
        rtlAware = false
    )
)

private data class PaddingModifier(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val rtlAware: Boolean
) : LayoutModifier, InspectableValue {
    init {
        require(start.value >= 0f && top.value >= 0f && end.value >= 0f && bottom.value >= 0f) {
            "Padding must be non-negative"
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val horizontal = start.toIntPx() + end.toIntPx()
        val vertical = top.toIntPx() + bottom.toIntPx()

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) {
            if (rtlAware) {
                placeable.placeRelative(start.toIntPx(), top.toIntPx())
            } else {
                placeable.place(start.toIntPx(), top.toIntPx())
            }
        }
    }

    override val nameFallback = if (rtlAware) "padding" else "absolutePadding"

    override val valueOverride: Any?
        get() = if (start == top && top == end && end == bottom) start else null

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(
            ValueElement(if (rtlAware) "start" else "left", start),
            ValueElement("top", top),
            ValueElement(if (rtlAware) "end" else "right", end),
            ValueElement("bottom", bottom)
        )
}

/**
 * Describes a padding to be applied along the edges inside a box.
 */
@Immutable
data class PaddingValues(
    @Stable
    val start: Dp = 0.dp,
    @Stable
    val top: Dp = 0.dp,
    @Stable
    val end: Dp = 0.dp,
    @Stable
    val bottom: Dp = 0.dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}

@Deprecated("InnerPadding was renamed to PaddingValues.", ReplaceWith("PaddingValues"))
typealias InnerPadding = PaddingValues
