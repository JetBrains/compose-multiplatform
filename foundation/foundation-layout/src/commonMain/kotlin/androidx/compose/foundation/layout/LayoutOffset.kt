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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Offset the content by ([x] dp, [y] dp). The offsets can be positive as well as non-positive.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.LayoutOffsetModifier
 */
@Stable
fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp) = this.then(
    OffsetModifier(
        x = x,
        y = y,
        rtlAware = true,
        inspectorInfo = debugInspectorInfo {
            name = "offset"
            properties["x"] = x
            properties["y"] = y
        }
    )
)

/**
 * Offset the content by ([x] dp, [y] dp). The offsets can be positive as well as non-positive.
 * The offsets are applied without regard to the current [LayoutDirection], see [Modifier
 * .offset] to apply relative offsets.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.LayoutAbsoluteOffsetModifier
 */
@Stable
fun Modifier.absoluteOffset(x: Dp = 0.dp, y: Dp = 0.dp) = this.then(
    OffsetModifier(
        x = x,
        y = y,
        rtlAware = false,
        inspectorInfo = debugInspectorInfo {
            name = "absoluteOffset"
            properties["x"] = x
            properties["y"] = y
        }
    )
)

/**
 * Offset the content by ([x] px, [y] px). The offsets can be positive as well as non-positive.
 * This modifier is designed to be used for offsets that change, possibly due to user interactions.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.LayoutOffsetPxModifier
 */
fun Modifier.offsetPx(
    x: State<Float> = mutableStateOf(0f),
    y: State<Float> = mutableStateOf(0f)
) = this.then(
    OffsetPxModifier(
        x = x,
        y = y,
        rtlAware = true,
        inspectorInfo = debugInspectorInfo {
            name = "offsetPx"
            properties["x"] = x
            properties["y"] = y
        }
    )
)

/**
 * Offset the content by ([x] px, [y] px). The offsets can be positive as well as non-positive.
 * This modifier is designed to be used for offsets that change, possibly due to user interactions.
 *
 * The offsets are applied without regard to the current [LayoutDirection]. To apply relative
 * offsets, use [Modifier.offsetPx] instead.
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.LayoutAbsoluteOffsetPxModifier
 */
fun Modifier.absoluteOffsetPx(
    x: State<Float> = mutableStateOf(0f),
    y: State<Float> = mutableStateOf(0f)
) = this.then(
    OffsetPxModifier(
        x = x,
        y = y,
        rtlAware = false,
        inspectorInfo = debugInspectorInfo {
            name = "absoluteOffsetPx"
            properties["x"] = x
            properties["y"] = y
        }
    )
)

private class OffsetModifier(
    val x: Dp,
    val y: Dp,
    val rtlAware: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (rtlAware) {
                placeable.placeRelative(x.toIntPx(), y.toIntPx())
            } else {
                placeable.place(x.toIntPx(), y.toIntPx())
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? OffsetModifier ?: return false

        return x == otherModifier.x &&
            y == otherModifier.y &&
            rtlAware == otherModifier.rtlAware
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + rtlAware.hashCode()
        return result
    }

    override fun toString(): String = "OffsetModifier(x=$x, y=$y, rtlAware=$rtlAware)"
}

private class OffsetPxModifier(
    val x: State<Float>,
    val y: State<Float>,
    val rtlAware: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (rtlAware) {
                placeable.placeRelative(x.value.roundToInt(), y.value.roundToInt())
            } else {
                placeable.place(x.value.roundToInt(), y.value.roundToInt())
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? OffsetPxModifier ?: return false

        return x == otherModifier.x &&
            y == otherModifier.y &&
            rtlAware == otherModifier.rtlAware
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + rtlAware.hashCode()
        return result
    }

    override fun toString(): String = "OffsetPxModifier(x=$x, y=$y, rtlAware=$rtlAware)"
}
