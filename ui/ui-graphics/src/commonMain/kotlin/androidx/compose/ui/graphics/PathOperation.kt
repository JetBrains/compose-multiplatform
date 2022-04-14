/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable

/**
 * Strategies for combining paths.
 *
 * See also:
 *
 * * [Path.combine], which uses this enum to decide how to combine two paths.
 */
// Must be kept in sync with SkPathOp
@Immutable
@kotlin.jvm.JvmInline
value class PathOperation internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Subtract the second path from the first path.
         *
         * For example, if the two paths are overlapping circles of equal diameter
         * but differing centers, the result would be a crescent portion of the
         * first circle that was not overlapped by the second circle.
         *
         * See also:
         *
         *  * [ReverseDifference], which is the same but subtracting the first path
         *    from the second.
         */
        val Difference = PathOperation(0)
        /**
         * Create a new path that is the intersection of the two paths, leaving the
         * overlapping pieces of the path.
         *
         * For example, if the two paths are overlapping circles of equal diameter
         * but differing centers, the result would be only the overlapping portion
         * of the two circles.
         *
         * See also:
         *  * [Xor], which is the inverse of this operation
         */
        val Intersect = PathOperation(1)

        /**
         * Create a new path that is the union (inclusive-or) of the two paths.
         *
         * For example, if the two paths are overlapping circles of equal diameter
         * but differing centers, the result would be a figure-eight like shape
         * matching the outer boundaries of both circles.
         */
        val Union = PathOperation(2)

        /**
         * Create a new path that is the exclusive-or of the two paths, leaving
         * everything but the overlapping pieces of the path.
         *
         * For example, if the two paths are overlapping circles of equal diameter
         * but differing centers, the figure-eight like shape less the overlapping parts
         *
         * See also:
         *  * [Intersect], which is the inverse of this operation
         */
        val Xor = PathOperation(3)

        /**
         * Subtract the first path from the second path.
         *
         * For example, if the two paths are overlapping circles of equal diameter
         * but differing centers, the result would be a crescent portion of the
         * second circle that was not overlapped by the first circle.
         *
         * See also:
         *
         *  * [Difference], which is the same but subtracting the second path
         *    from the first.
         */
        val ReverseDifference = PathOperation(4)
    }

    override fun toString() = when (this) {
        Difference -> "Difference"
        Intersect -> "Intersect"
        Union -> "Union"
        Xor -> "Xor"
        ReverseDifference -> "ReverseDifference"
        else -> "Unknown"
    }
}

@Deprecated(
    message = "Use PathOperation.Difference instead",
    ReplaceWith(
        "PathOperation.Difference",
        "androidx.compose.ui.graphics.PathOperation.Difference"
    )
)
val PathOperation.Companion.difference: PathOperation
    get() = Difference

@Deprecated(
    message = "Use PathOperation.Intersect instead",
    ReplaceWith(
        "PathOperation.Intersect",
        "androidx.compose.ui.graphics.PathOperation.Intersect"
    )
)
val PathOperation.Companion.intersect: PathOperation
    get() = Intersect

@Deprecated(
    message = "Use PathOperation.Union instead",
    ReplaceWith(
        "PathOperation.Union",
        "androidx.compose.ui.graphics.PathOperation.Union"
    )
)
val PathOperation.Companion.union: PathOperation
    get() = Union

@Deprecated(
    message = "Use PathOperation.ReverseDifference instead",
    ReplaceWith(
        "PathOperation.ReverseDifference",
        "androidx.compose.ui.graphics.PathOperation.ReverseDifference"
    )
)
val PathOperation.Companion.reverseDifference: PathOperation
    get() = ReverseDifference

@Deprecated(
    message = "Use PathOperation.Xor instead",
    ReplaceWith(
        "PathOperation.Xor",
        "androidx.compose.ui.graphics.PathOperation.Xor"
    )
)
val PathOperation.Companion.xor: PathOperation
    get() = Xor