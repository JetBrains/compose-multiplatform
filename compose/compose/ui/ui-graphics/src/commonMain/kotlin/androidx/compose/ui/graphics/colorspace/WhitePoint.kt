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

/**
 * Class for constructing white points used in [RGB][Rgb] color space. The value is
 * stored in the CIE xyY color space. The Y component of the white point is assumed
 * to be 1.
 *
 * @see Illuminant
 */
data class WhitePoint(val x: Float, val y: Float) {
    /**
     * Illuminant for CIE XYZ white point
     */
    constructor(x: Float, y: Float, z: Float) : this(x, y, z, x + y + z)

    @Suppress("UNUSED_PARAMETER")
    private constructor(x: Float, y: Float, z: Float, sum: Float) : this(x / sum, y / sum)

    /**
     * Converts a value from CIE xyY to CIE XYZ. Y is assumed to be 1 so the
     * input xyY array only contains the x and y components.
     *
     * @return A new float array of length 3 containing XYZ values
     */
    /*@Size(3)*/
    internal fun toXyz(): FloatArray {
        return floatArrayOf(x / y, 1.0f, (1f - x - y) / y)
    }
}
