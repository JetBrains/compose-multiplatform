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
 * Defines what happens at the edge of the gradient.
 * A gradient is defined along a finite inner area. In the case of a linear
 * gradient, it's between the parallel lines that are orthogonal to the line
 * drawn between two points. In the case of radial gradients, it's the disc
 * that covers the circle centered on a particular point up to a given radius.
 *
 * This enum is used to define how the gradient should paint the regions
 * outside that defined inner area.
 *
 * See also:
 * [LinearGradientShader], [RadialGradientShader] which works in
 * relative coordinates and can create a [Shader] representing the gradient
 * for a particular [Rect] on demand.
 */
@Immutable
@kotlin.jvm.JvmInline
value class TileMode internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Edge is clamped to the final color.
         *
         * The gradient will paint the all the regions outside the inner area with
         * the color of the point closest to that region.
         */
        val Clamp = TileMode(0)

        /**
         * Edge is repeated from first color to last.
         *
         * This is as if the stop points from 0.0 to 1.0 were then repeated from 1.0
         * to 2.0, 2.0 to 3.0, and so forth (and for linear gradients, similarly from
         * -1.0 to 0.0, -2.0 to -1.0, etc).
         */
        val Repeated = TileMode(1)

        /**
         * Edge is mirrored from last color to first.
         * This is as if the stop points from 0.0 to 1.0 were then repeated backwards
         * from 2.0 to 1.0, then forwards from 2.0 to 3.0, then backwards again from
         * 4.0 to 3.0, and so forth (and for linear gradients, similarly from in the
         * negative direction).
         */
        val Mirror = TileMode(2)

        /**
         * Render the shader's image pixels only within its original bounds. If the shader
         * draws outside of its original bounds, transparent black is drawn instead.
         */
        val Decal = TileMode(3)
    }

    override fun toString() = when (this) {
        Clamp -> "Clamp"
        Repeated -> "Repeated"
        Mirror -> "Mirror"
        Decal -> "Decal"
        else -> "Unknown"
    }
}

/**
 * Capability query to determine if the particular platform supports the [TileMode]. Not
 * all platforms support all tile mode algorithms, however, [TileMode.Clamp], [TileMode.Repeated]
 * and [TileMode.Mirror] are guaranteed to be supported. If a [TileMode] that is not supported is
 * used, the default of [TileMode.Clamp] is consumed instead.
 */
expect fun TileMode.isSupported(): Boolean