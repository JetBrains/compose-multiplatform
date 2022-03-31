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

package androidx.compose.ui.graphics.colorspace

import androidx.compose.runtime.Immutable

/**
 * A render intent determines how a [connector][Connector]
 * maps colors from one color space to another. The choice of mapping is
 * important when the source color space has a larger color gamut than the
 * destination color space.
 *
 * @see ColorSpace.connect
 */
@Immutable
@kotlin.jvm.JvmInline
value class RenderIntent internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Compresses the source gamut into the destination gamut.
         * This render intent affects all colors, inside and outside
         * of destination gamut. The goal of this render intent is
         * to preserve the visual relationship between colors.
         *
         * This render intent is currently not
         * implemented and behaves like [Relative].
         */
        val Perceptual = RenderIntent(0)

        /**
         * Similar to the [Absolute] render intent, this render
         * intent matches the closest color in the destination gamut
         * but makes adjustments for the destination white point.
         */
        val Relative = RenderIntent(1)

        /**
         * Attempts to maintain the relative saturation of colors
         * from the source gamut to the destination gamut, to keep
         * highly saturated colors as saturated as possible.
         *
         * This render intent is currently not
         * implemented and behaves like [Relative].
         */
        val Saturation = RenderIntent(2)

        /**
         * Colors that are in the destination gamut are left unchanged.
         * Colors that fall outside of the destination gamut are mapped
         * to the closest possible color within the gamut of the destination
         * color space (they are clipped).
         */
        val Absolute = RenderIntent(3)
    }

    override fun toString() = when (this) {
        Perceptual -> "Perceptual"
        Relative -> "Relative"
        Saturation -> "Saturation"
        Absolute -> "Absolute"
        else -> "Unknown"
    }
}