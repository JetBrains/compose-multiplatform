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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite

/**
 * Similar to [Constraints], but with constraint values expressed in [Dp].
 */
@Immutable
data class DpConstraints(
    @Stable
    val minWidth: Dp = 0.dp,
    @Stable
    val maxWidth: Dp = Dp.Infinity,
    @Stable
    val minHeight: Dp = 0.dp,
    @Stable
    val maxHeight: Dp = Dp.Infinity
) {
    init {
        require(minWidth.isFinite()) { "Constraints#minWidth should be finite" }
        require(minHeight.isFinite()) { "Constraints#minHeight should be finite" }
        require(!minWidth.value.isNaN()) { "Constraints#minWidth should not be NaN" }
        require(!maxWidth.value.isNaN()) { "Constraints#maxWidth should not be NaN" }
        require(!minHeight.value.isNaN()) { "Constraints#minHeight should not be NaN" }
        require(!maxHeight.value.isNaN()) { "Constraints#maxHeight should not be NaN" }
        require(minWidth <= maxWidth) {
            "Constraints should be satisfiable, but minWidth > maxWidth"
        }
        require(minHeight <= maxHeight) {
            "Constraints should be satisfiable, but minHeight > maxHeight"
        }
        require(minWidth >= 0.dp) { "Constraints#minWidth should be non-negative" }
        require(maxWidth >= 0.dp) { "Constraints#maxWidth should be non-negative" }
        require(minHeight >= 0.dp) { "Constraints#minHeight should be non-negative" }
        require(maxHeight >= 0.dp) { "Constraints#maxHeight should be non-negative" }
    }

    companion object {
        /**
         * Creates constraints tight in both dimensions.
         */
        @Stable
        fun fixed(width: Dp, height: Dp) = DpConstraints(width, width, height, height)

        /**
         * Creates constraints with tight width and loose height.
         */
        @Stable
        fun fixedWidth(width: Dp) = DpConstraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0.dp,
            maxHeight = Dp.Infinity
        )

        /**
         * Creates constraints with tight height and loose width.
         */
        @Stable
        fun fixedHeight(height: Dp) = DpConstraints(
            minWidth = 0.dp,
            maxWidth = Dp.Infinity,
            minHeight = height,
            maxHeight = height
        )
    }
}

/**
 * Whether or not the upper bound on the maximum height.
 * @see hasBoundedWidth
 */
@Stable
val DpConstraints.hasBoundedHeight get() = maxHeight.isFinite()

/**
 * Whether or not the upper bound on the maximum width.
 * @see hasBoundedHeight
 */
@Stable
val DpConstraints.hasBoundedWidth get() = maxWidth.isFinite()

/**
 * Whether there is exactly one width value that satisfies the constraints.
 */
@Stable
val DpConstraints.hasFixedWidth get() = maxWidth == minWidth

/**
 * Whether there is exactly one height value that satisfies the constraints.
 */
@Stable
val DpConstraints.hasFixedHeight get() = maxHeight == minHeight

/**
 * Whether the area of a component respecting these constraints will definitely be 0.
 * This is true when at least one of maxWidth and maxHeight are 0.
 */
@Stable
val DpConstraints.isZero get() = maxWidth == 0.dp || maxHeight == 0.dp

/**
 * Whether there is any size that satisfies the current constraints.
 */
@Stable
val DpConstraints.satisfiable get() = minWidth <= maxWidth && minHeight <= maxHeight

/**
 * Returns the result of coercing the current constraints in a different set of constraints.
 */
@Stable
fun DpConstraints.enforce(otherConstraints: DpConstraints) = DpConstraints(
    minWidth = minWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    maxWidth = maxWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    minHeight = minHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight),
    maxHeight = maxHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight)
)

/**
 * Returns the DpConstraints obtained by offsetting the current instance with the given values.
 */
@Stable
fun DpConstraints.offset(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) = DpConstraints(
    (minWidth + horizontal).coerceAtLeast(0.dp),
    (maxWidth + horizontal).coerceAtLeast(0.dp),
    (minHeight + vertical).coerceAtLeast(0.dp),
    (maxHeight + vertical).coerceAtLeast(0.dp)
)

/**
 * Creates the [Constraints] corresponding to the current [DpConstraints].
 */
@Stable
fun Density.Constraints(dpConstraints: DpConstraints) = Constraints(
    minWidth = dpConstraints.minWidth.toIntPx(),
    maxWidth = dpConstraints.maxWidth.toIntPx(),
    minHeight = dpConstraints.minHeight.toIntPx(),
    maxHeight = dpConstraints.maxHeight.toIntPx()
)

/**
 * Creates the [DpConstraints] corresponding to the current [Constraints].
 */
@Stable
fun Density.DpConstraints(constraints: Constraints) = DpConstraints(
    minWidth = constraints.minWidth.toDp(),
    maxWidth = constraints.maxWidth.toDp(),
    minHeight = constraints.minHeight.toDp(),
    maxHeight = constraints.maxHeight.toDp()
)
