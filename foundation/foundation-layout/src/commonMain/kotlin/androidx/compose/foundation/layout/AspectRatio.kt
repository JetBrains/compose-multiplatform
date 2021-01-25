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
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.isSatisfiedBy
import kotlin.math.roundToInt

/**
 * Attempts to size the content to match a specified aspect ratio by trying to match one of the
 * incoming constraints in the following order: [Constraints.maxWidth], [Constraints.maxHeight],
 * [Constraints.minWidth], [Constraints.minHeight] if [matchHeightConstraintsFirst] is `false`
 * (which is the default), or [Constraints.maxHeight], [Constraints.maxWidth],
 * [Constraints.minHeight], [Constraints.minWidth] if [matchHeightConstraintsFirst] is `true`.
 * The size in the other dimension is determined by the aspect ratio. The combinations will be
 * tried in this order until one non-empty is found to satisfy the constraints. If no valid
 * size is obtained this way, it means that there is no non-empty size satisfying both
 * the constraints and the aspect ratio, so the constraints will not be respected
 * and the content will be sized such that the [Constraints.maxWidth] or [Constraints.maxHeight]
 * is matched (depending on [matchHeightConstraintsFirst]).
 *
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.SimpleAspectRatio
 *
 * @param ratio the desired width/height positive ratio
 */
@Stable
fun Modifier.aspectRatio(
    /*@FloatRange(from = 0.0, fromInclusive = false)*/
    ratio: Float,
    matchHeightConstraintsFirst: Boolean = false
) = this.then(
    AspectRatioModifier(
        ratio,
        matchHeightConstraintsFirst,
        debugInspectorInfo {
            name = "aspectRatio"
            properties["ratio"] = ratio
            properties["matchHeightConstraintsFirst"] = matchHeightConstraintsFirst
        }
    )
)

private class AspectRatioModifier(
    val aspectRatio: Float,
    val matchHeightConstraintsFirst: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {
    init {
        require(aspectRatio > 0) { "aspectRatio $aspectRatio must be > 0" }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val size = constraints.findSize()
        val wrappedConstraints = if (size != IntSize.Zero) {
            Constraints.fixed(size.width, size.height)
        } else {
            constraints
        }
        val placeable = measurable.measure(wrappedConstraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = if (height != Constraints.Infinity) {
        (height * aspectRatio).roundToInt()
    } else {
        measurable.minIntrinsicWidth(height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = if (height != Constraints.Infinity) {
        (height * aspectRatio).roundToInt()
    } else {
        measurable.maxIntrinsicWidth(height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = if (width != Constraints.Infinity) {
        (width / aspectRatio).roundToInt()
    } else {
        measurable.minIntrinsicHeight(width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = if (width != Constraints.Infinity) {
        (width / aspectRatio).roundToInt()
    } else {
        measurable.maxIntrinsicHeight(width)
    }

    private fun Constraints.findSize(): IntSize {
        if (!matchHeightConstraintsFirst) {
            tryMaxWidth().also { if (it != IntSize.Zero) return it }
            tryMaxHeight().also { if (it != IntSize.Zero) return it }
            tryMinWidth().also { if (it != IntSize.Zero) return it }
            tryMinHeight().also { if (it != IntSize.Zero) return it }
            tryMaxWidth(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
            tryMaxHeight(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
            tryMinWidth(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
            tryMinHeight(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
        } else {
            tryMaxHeight().also { if (it != IntSize.Zero) return it }
            tryMaxWidth().also { if (it != IntSize.Zero) return it }
            tryMinHeight().also { if (it != IntSize.Zero) return it }
            tryMinWidth().also { if (it != IntSize.Zero) return it }
            tryMaxHeight(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
            tryMaxWidth(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
            tryMinHeight(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
            tryMinWidth(enforceConstraints = false).also { if (it != IntSize.Zero) return it }
        }
        return IntSize.Zero
    }

    private fun Constraints.tryMaxWidth(enforceConstraints: Boolean = true): IntSize {
        val maxWidth = this.maxWidth
        if (maxWidth != Constraints.Infinity) {
            val height = (maxWidth / aspectRatio).roundToInt()
            if (height > 0) {
                val size = IntSize(maxWidth, height)
                if (!enforceConstraints || isSatisfiedBy(size)) {
                    return size
                }
            }
        }
        return IntSize.Zero
    }

    private fun Constraints.tryMaxHeight(enforceConstraints: Boolean = true): IntSize {
        val maxHeight = this.maxHeight
        if (maxHeight != Constraints.Infinity) {
            val width = (maxHeight * aspectRatio).roundToInt()
            if (width > 0) {
                val size = IntSize(width, maxHeight)
                if (!enforceConstraints || isSatisfiedBy(size)) {
                    return size
                }
            }
        }
        return IntSize.Zero
    }

    private fun Constraints.tryMinWidth(enforceConstraints: Boolean = true): IntSize {
        val minWidth = this.minWidth
        val height = (minWidth / aspectRatio).roundToInt()
        if (height > 0) {
            val size = IntSize(minWidth, height)
            if (!enforceConstraints || isSatisfiedBy(size)) {
                return size
            }
        }
        return IntSize.Zero
    }

    private fun Constraints.tryMinHeight(enforceConstraints: Boolean = true): IntSize {
        val minHeight = this.minHeight
        val width = (minHeight * aspectRatio).roundToInt()
        if (width > 0) {
            val size = IntSize(width, minHeight)
            if (!enforceConstraints || isSatisfiedBy(size)) {
                return size
            }
        }
        return IntSize.Zero
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? AspectRatioModifier ?: return false
        return aspectRatio == otherModifier.aspectRatio &&
            matchHeightConstraintsFirst == other.matchHeightConstraintsFirst
    }

    override fun hashCode(): Int =
        aspectRatio.hashCode() * 31 + matchHeightConstraintsFirst.hashCode()

    override fun toString(): String = "AspectRatioModifier(aspectRatio=$aspectRatio)"
}
