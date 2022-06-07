/*
 * Copyright 2021 The Android Open Source Project
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

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Implementation of the Oklab color space. Oklab uses
 * a D65 white point.
 */
internal class Oklab(
    name: String,
    id: Int
) : ColorSpace(
    name,
    ColorModel.Lab, id
) {

    override val isWideGamut: Boolean
        get() = true

    override fun getMinValue(component: Int): Float {
        return if (component == 0) 0f else -0.5f
    }

    override fun getMaxValue(component: Int): Float {
        return if (component == 0) 1f else 0.5f
    }

    override fun toXyz(v: FloatArray): FloatArray {
        v[0] = v[0].coerceIn(0f, 1f)
        v[1] = v[1].coerceIn(-0.5f, 0.5f)
        v[2] = v[2].coerceIn(-0.5f, 0.5f)

        mul3x3Float3(InverseM2, v)
        v[0] = v[0] * v[0] * v[0]
        v[1] = v[1] * v[1] * v[1]
        v[2] = v[2] * v[2] * v[2]
        mul3x3Float3(InverseM1, v)

        return v
    }

    override fun fromXyz(v: FloatArray): FloatArray {
        mul3x3Float3(M1, v)

        v[0] = sign(v[0]) * abs(v[0]).pow(1.0f / 3.0f)
        v[1] = sign(v[1]) * abs(v[1]).pow(1.0f / 3.0f)
        v[2] = sign(v[2]) * abs(v[2]).pow(1.0f / 3.0f)

        mul3x3Float3(M2, v)
        return v
    }

    internal companion object {
        /**
         * This is the matrix applied before the nonlinear transform for (D50) XYZ-to-Oklab.
         * This combines the D50-to-D65 white point transform with the normal transform matrix
         * because this is always done together in [fromXyz].
         */
        private val M1 = mul3x3(
            floatArrayOf(
                0.8189330101f, 0.0329845436f, 0.0482003018f,
                0.3618667424f, 0.9293118715f, 0.2643662691f,
                -0.1288597137f, 0.0361456387f, 0.6338517070f
            ),
            chromaticAdaptation(
                matrix = Adaptation.Bradford.transform,
                srcWhitePoint = Illuminant.D50.toXyz(),
                dstWhitePoint = Illuminant.D65.toXyz()
            )
        )

        /**
         * Matrix applied after the nonlinear transform.
         */
        private val M2 = floatArrayOf(
            0.2104542553f, 1.9779984951f, 0.0259040371f,
            0.7936177850f, -2.4285922050f, 0.7827717662f,
            -0.0040720468f, 0.4505937099f, -0.8086757660f
        )

        /**
         * The inverse of the [M1] matrix, transforming back to XYZ (D50)
         */
        private val InverseM1 = inverse3x3(M1)

        /**
         * The inverse of the [M2] matrix, doing the first linear transform in the
         * Oklab-to-XYZ before doing the nonlinear transform.
         */
        private val InverseM2 = inverse3x3(M2)
    }
}
