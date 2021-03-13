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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.pow

/**
 * A lambda that takes a [ColorSpace] and returns a converter that can both convert a [Color] to
 * a [AnimationVector4D], and convert a [AnimationVector4D]) back to a [Color] in the given
 * [ColorSpace].
 */
private val ColorToVector: (colorSpace: ColorSpace) -> TwoWayConverter<Color, AnimationVector4D> =
    { colorSpace ->
        TwoWayConverter(
            convertToVector = { color ->
                // TODO: use Oklab when it is public API
                val colorXyz = color.convert(ColorSpaces.CieXyz)
                val x = colorXyz.red
                val y = colorXyz.green
                val z = colorXyz.blue

                val l = multiplyColumn(0, x, y, z, M1).pow(1f / 3f)
                val a = multiplyColumn(1, x, y, z, M1).pow(1f / 3f)
                val b = multiplyColumn(2, x, y, z, M1).pow(1f / 3f)
                AnimationVector4D(color.alpha, l, a, b)
            },
            convertFromVector = {
                val l = it.v2.pow(3f)
                val a = it.v3.pow(3f)
                val b = it.v4.pow(3f)

                val x = multiplyColumn(0, l, a, b, InverseM1)
                val y = multiplyColumn(1, l, a, b, InverseM1)
                val z = multiplyColumn(2, l, a, b, InverseM1)

                val colorXyz = Color(
                    alpha = it.v1.coerceIn(0f, 1f),
                    red = x.coerceIn(-2f, 2f),
                    green = y.coerceIn(-2f, 2f),
                    blue = z.coerceIn(-2f, 2f),
                    colorSpace = ColorSpaces.CieXyz // here we have the right color space
                )
                colorXyz.convert(colorSpace)
            }
        )
    }

/**
 * A lambda that takes a [ColorSpace] and returns a converter that can both convert a [Color] to
 * a [AnimationVector4D], and convert a [AnimationVector4D]) back to a [Color] in the given
 * [ColorSpace].
 */
val Color.Companion.VectorConverter:
    (colorSpace: ColorSpace) -> TwoWayConverter<Color, AnimationVector4D>
        get() = ColorToVector

// These are utilities and constants to emulate converting to/from Oklab color space.
// These can be removed when Oklab becomes public and we can use it directly in the conversion.
private val M1 = floatArrayOf(
    0.80405736f, 0.026893456f, 0.04586542f,
    0.3188387f, 0.9319606f, 0.26299807f,
    -0.11419419f, 0.05105356f, 0.83999807f
)

private val InverseM1 = floatArrayOf(
    1.2485008f, -0.032856926f, -0.057883114f,
    -0.48331892f, 1.1044513f, -0.3194066f,
    0.19910365f, -0.07159331f, 1.202023f
)

private fun multiplyColumn(column: Int, x: Float, y: Float, z: Float, matrix: FloatArray): Float {
    return x * matrix[column] + y * matrix[3 + column] + z * matrix[6 + column]
}