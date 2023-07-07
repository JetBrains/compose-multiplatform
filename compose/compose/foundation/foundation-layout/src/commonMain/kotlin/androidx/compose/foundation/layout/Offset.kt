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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Offset the content by ([x] dp, [y] dp). The offsets can be positive as well as non-positive.
 * Applying an offset only changes the position of the content, without interfering with
 * its size measurement.
 *
 * This modifier will automatically adjust the horizontal offset according to the layout direction:
 * when the layout direction is LTR, positive [x] offsets will move the content to the right and
 * when the layout direction is RTL, positive [x] offsets will move the content to the left.
 * For a modifier that offsets without considering layout direction, see [absoluteOffset].
 *
 * @see absoluteOffset
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.OffsetModifier
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
 * Applying an offset only changes the position of the content, without interfering with
 * its size measurement.
 *
 * This modifier will not consider layout direction when calculating the position of the content:
 * a positive [x] offset will always move the content to the right.
 * For a modifier that considers the layout direction when applying the offset, see [offset].
 *
 * @see offset
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.AbsoluteOffsetModifier
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
 * Offset the content by [offset] px. The offsets can be positive as well as non-positive.
 * Applying an offset only changes the position of the content, without interfering with
 * its size measurement.
 *
 * This modifier is designed to be used for offsets that change, possibly due to user interactions.
 * It avoids recomposition when the offset is changing, and also adds a graphics layer that
 * prevents unnecessary redrawing of the context when the offset is changing.
 *
 * This modifier will automatically adjust the horizontal offset according to the layout direction:
 * when the LD is LTR, positive horizontal offsets will move the content to the right and
 * when the LD is RTL, positive horizontal offsets will move the content to the left.
 * For a modifier that offsets without considering layout direction, see [absoluteOffset].
 *
 * @see [absoluteOffset]
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.OffsetPxModifier
 */
fun Modifier.offset(offset: Density.() -> IntOffset) = this.then(
    OffsetPxModifier(
        offset = offset,
        rtlAware = true,
        inspectorInfo = debugInspectorInfo {
            name = "offset"
            properties["offset"] = offset
        }
    )
)

/**
 * Offset the content by [offset] px. The offsets can be positive as well as non-positive.
 * Applying an offset only changes the position of the content, without interfering with
 * its size measurement.
 *
 * This modifier is designed to be used for offsets that change, possibly due to user interactions.
 * It avoids recomposition when the offset is changing, and also adds a graphics layer that
 * prevents unnecessary redrawing of the context when the offset is changing.
 *
 * This modifier will not consider layout direction when calculating the position of the content:
 * a positive horizontal offset will always move the content to the right.
 * For a modifier that considers layout direction when applying the offset, see [offset].
 *
 * @see offset
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.AbsoluteOffsetPxModifier
 */
fun Modifier.absoluteOffset(
    offset: Density.() -> IntOffset
) = this.then(
    OffsetPxModifier(
        offset = offset,
        rtlAware = false,
        inspectorInfo = debugInspectorInfo {
            name = "absoluteOffset"
            properties["offset"] = offset
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
                placeable.placeRelative(x.roundToPx(), y.roundToPx())
            } else {
                placeable.place(x.roundToPx(), y.roundToPx())
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
    val offset: Density.() -> IntOffset,
    val rtlAware: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val offsetValue = offset()
            if (rtlAware) {
                placeable.placeRelativeWithLayer(offsetValue.x, offsetValue.y)
            } else {
                placeable.placeWithLayer(offsetValue.x, offsetValue.y)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? OffsetPxModifier ?: return false

        return offset == otherModifier.offset &&
            rtlAware == otherModifier.rtlAware
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + rtlAware.hashCode()
        return result
    }

    override fun toString(): String = "OffsetPxModifier(offset=$offset, rtlAware=$rtlAware)"
}
