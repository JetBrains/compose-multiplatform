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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.packFloats
import kotlin.math.pow

/**
 * Implementation of the CIE L*a*b* color space. Its PCS is CIE XYZ
 * with a white point of D50.
 */
internal class Lab(
    name: String,
    id: Int
) : ColorSpace(
    name,
    ColorModel.Lab, id
) {

    override val isWideGamut: Boolean
        get() = true

    override fun getMinValue(component: Int): Float {
        return if (component == 0) 0.0f else -128.0f
    }

    override fun getMaxValue(component: Int): Float {
        return if (component == 0) 100.0f else 128.0f
    }

    override fun toXyz(v: FloatArray): FloatArray {
        v[0] = v[0].coerceIn(0.0f, 100.0f)
        v[1] = v[1].coerceIn(-128.0f, 128.0f)
        v[2] = v[2].coerceIn(-128.0f, 128.0f)

        val fy = (v[0] + 16.0f) / 116.0f
        val fx = fy + (v[1] * 0.002f)
        val fz = fy - (v[2] * 0.005f)
        val x = if (fx > D) fx * fx * fx else (1.0f / B) * (fx - C)
        val y = if (fy > D) fy * fy * fy else (1.0f / B) * (fy - C)
        val z = if (fz > D) fz * fz * fz else (1.0f / B) * (fz - C)

        v[0] = x * Illuminant.D50Xyz[0]
        v[1] = y * Illuminant.D50Xyz[1]
        v[2] = z * Illuminant.D50Xyz[2]

        return v
    }

    override fun toXy(v0: Float, v1: Float, v2: Float): Long {
        val v00 = v0.coerceIn(0.0f, 100.0f)
        val v10 = v0.coerceIn(-128.0f, 128.0f)

        val fy = (v00 + 16.0f) / 116.0f
        val fx = fy + (v10 * 0.002f)
        val x = if (fx > D) fx * fx * fx else (1.0f / B) * (fx - C)
        val y = if (fy > D) fy * fy * fy else (1.0f / B) * (fy - C)

        return packFloats(x * Illuminant.D50Xyz[0], y * Illuminant.D50Xyz[1])
    }

    override fun toZ(v0: Float, v1: Float, v2: Float): Float {
        val v00 = v0.coerceIn(0.0f, 100.0f)
        val v20 = v2.coerceIn(-128.0f, 128.0f)
        val fy = (v00 + 16.0f) / 116.0f
        val fz = fy - (v20 * 0.005f)
        val z = if (fz > D) fz * fz * fz else (1.0f / B) * (fz - C)
        return z * Illuminant.D50Xyz[2]
    }

    override fun xyzaToColor(
        x: Float,
        y: Float,
        z: Float,
        a: Float,
        colorSpace: ColorSpace
    ): Color {
        val x1 = x / Illuminant.D50Xyz[0]
        val y1 = y / Illuminant.D50Xyz[1]
        val z1 = z / Illuminant.D50Xyz[2]

        val fx = if (x1 > A) x1.pow(1f / 3f) else B * x1 + C
        val fy = if (y1 > A) y1.pow(1f / 3f) else B * y1 + C
        val fz = if (z1 > A) z1.pow(1f / 3f) else B * z1 + C

        val l = 116.0f * fy - 16.0f
        val a1 = 500.0f * (fx - fy)
        val b = 200.0f * (fy - fz)

        return Color(
            l.coerceIn(0.0f, 100.0f),
            a1.coerceIn(-128.0f, 128.0f),
            b.coerceIn(-128.0f, 128.0f),
            a,
            colorSpace
        )
    }

    override fun fromXyz(v: FloatArray): FloatArray {
        val x = v[0] / Illuminant.D50Xyz[0]
        val y = v[1] / Illuminant.D50Xyz[1]
        val z = v[2] / Illuminant.D50Xyz[2]

        val fx = if (x > A) x.pow(1f / 3f) else B * x + C
        val fy = if (y > A) y.pow(1f / 3f) else B * y + C
        val fz = if (z > A) z.pow(1f / 3f) else B * z + C

        val l = 116.0f * fy - 16.0f
        val a = 500.0f * (fx - fy)
        val b = 200.0f * (fy - fz)

        v[0] = l.coerceIn(0.0f, 100.0f)
        v[1] = a.coerceIn(-128.0f, 128.0f)
        v[2] = b.coerceIn(-128.0f, 128.0f)

        return v
    }

    internal companion object {
        private const val A = 216.0f / 24389.0f
        private const val B = 841.0f / 108.0f
        private const val C = 4.0f / 29.0f
        private const val D = 6.0f / 29.0f
    }
}