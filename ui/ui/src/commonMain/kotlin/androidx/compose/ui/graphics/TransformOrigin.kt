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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs a [TransformOrigin] from the given fractional values from the Layer's
 * width and height
 */
fun TransformOrigin(pivotFractionX: Float, pivotFractionY: Float): TransformOrigin =
    TransformOrigin(packFloats(pivotFractionX, pivotFractionY))

/**
 * A two-dimensional position represented as a fraction of the Layer's width and height
 */
@Immutable
@kotlin.jvm.JvmInline
value class TransformOrigin internal constructor(@PublishedApi internal val packedValue: Long) {

    /**
     * Return the position along the x-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the width of the content. A value of 0.5f represents the midpoint between the left
     * and right bounds of the content
     */
    val pivotFractionX: Float
        get() = unpackFloat1(packedValue)

    /**
     * Return the position along the y-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the height of the content. A value of 0.5f represents the midpoint between the top
     * and bottom bounds of the content
     */
    val pivotFractionY: Float
        get() = unpackFloat2(packedValue)

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component1(): Float = pivotFractionX

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component2(): Float = pivotFractionY

    /**
     * Returns a copy of this TransformOrigin instance optionally overriding the
     * pivotFractionX or pivotFractionY parameter
     */
    fun copy(
        pivotFractionX: Float = this.pivotFractionX,
        pivotFractionY: Float = this.pivotFractionY
    ) = TransformOrigin(pivotFractionX, pivotFractionY)

    companion object {

        /**
         * [TransformOrigin] constant to indicate that the center of the content should
         * be used for rotation and scale transformations
         */
        val Center = TransformOrigin(0.5f, 0.5f)
    }
}
