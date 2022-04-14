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
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1

/**
 * A color model is required by a [ColorSpace] to describe the
 * way colors can be represented as tuples of numbers. A common color
 * model is the [RGB][Rgb] color model which defines a color
 * as represented by a tuple of 3 numbers (red, green and blue).
 */
@Immutable
@kotlin.jvm.JvmInline
value class ColorModel internal constructor(
    /**
     * pack both the number of components and an ordinal value to distinguish between
     * different ColorModel types that have the same number of components
     */
    internal val packedValue: Long
) {
    /**
     * Returns the number of components for this color model.
     *
     * @return An integer between 1 and 4
     */
    /*@IntRange(from = 1, to = 4)*/
    @Stable
    val componentCount: Int
        get() {
            return unpackInt1(packedValue)
        }

    companion object {
        /**
         * The RGB model is a color model with 3 components that
         * refer to the three additive primiaries: red, green
         * and blue.
         */
        val Rgb = ColorModel(packInts(3, 0))

        /**
         * The XYZ model is a color model with 3 components that
         * are used to model human color vision on a basic sensory
         * level.
         */
        val Xyz = ColorModel(packInts(3, 1))

        /**
         * The Lab model is a color model with 3 components used
         * to describe a color space that is more perceptually
         * uniform than XYZ.
         */
        val Lab = ColorModel(packInts(3, 2))

        /**
         * The CMYK model is a color model with 4 components that
         * refer to four inks used in color printing: cyan, magenta,
         * yellow and black (or key). CMYK is a subtractive color
         * model.
         */
        val Cmyk = ColorModel(packInts(4, 3))
    }

    override fun toString() = when (this) {
        Rgb -> "Rgb"
        Xyz -> "Xyz"
        Lab -> "Lab"
        Cmyk -> "Cmyk"
        else -> "Unknown"
    }
}