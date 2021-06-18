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

package androidx.compose.ui.graphics

import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.SweepGradient
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastForEachIndexed

actual typealias Shader = android.graphics.Shader

internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    val numTransparentColors = countTransparentColors(colors)
    return LinearGradient(
        from.x,
        from.y,
        to.x,
        to.y,
        makeTransparentColors(colors, numTransparentColors),
        makeTransparentStops(colorStops, colors, numTransparentColors),
        tileMode.toAndroidTileMode()
    )
}

internal actual fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    val numTransparentColors = countTransparentColors(colors)
    return RadialGradient(
        center.x,
        center.y,
        radius,
        makeTransparentColors(colors, numTransparentColors),
        makeTransparentStops(colorStops, colors, numTransparentColors),
        tileMode.toAndroidTileMode()
    )
}

internal actual fun ActualSweepGradientShader(
    center: Offset,
    colors: List<Color>,
    colorStops: List<Float>?
): Shader {
    validateColorStops(colors, colorStops)
    val numTransparentColors = countTransparentColors(colors)
    return SweepGradient(
        center.x,
        center.y,
        makeTransparentColors(colors, numTransparentColors),
        makeTransparentStops(colorStops, colors, numTransparentColors),
    )
}

internal actual fun ActualImageShader(
    image: ImageBitmap,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader {
    return BitmapShader(
        image.asAndroidBitmap(),
        tileModeX.toAndroidTileMode(),
        tileModeY.toAndroidTileMode()
    )
}

/**
 * Returns the number of transparent (alpha = 0) values that aren't at the beginning or
 * end of the gradient so that the color stops can be added. On O and newer devices,
 * this always returns 0 because no stops need to be added.
 */
@VisibleForTesting
internal fun countTransparentColors(colors: List<Color>): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return 0
    }
    var numTransparentColors = 0
    // Don't count the first and last value because we don't add stops for those
    for (i in 1 until colors.lastIndex) {
        if (colors[i].alpha == 0f) {
            numTransparentColors++
        }
    }
    return numTransparentColors
}

/**
 * There was a change in behavior between Android N and O with how
 * transparent colors are interpolated with skia gradients. More specifically
 * Android O treats all fully transparent colors the same regardless of the
 * rgb channels, however, Android N and older releases interpolated between
 * the color channels as well. Because Color.Transparent is transparent black,
 * this would introduce some muddy colors as part of gradients with transparency
 * for Android N and below.
 * In order to make gradient rendering consistent and match the behavior of Android O+,
 * detect whenever Color.Transparent is used and a stop matching the color of the previous
 * value, but alpha = 0 is added and another stop at the same point with the same color
 * as the following value, but with alpha = 0 is used.
 */
@VisibleForTesting
internal fun makeTransparentColors(
    colors: List<Color>,
    numTransparentColors: Int
): IntArray {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // No change for Android O+, map the colors directly to their argb equivalent
        return IntArray(colors.size) { i -> colors[i].toArgb() }
    }
    val values = IntArray(colors.size + numTransparentColors)
    var valuesIndex = 0
    val lastIndex = colors.lastIndex
    colors.fastForEachIndexed { index, color ->
        if (color.alpha == 0f) {
            if (index == 0) {
                values[valuesIndex++] = colors[1].copy(alpha = 0f).toArgb()
            } else if (index == lastIndex) {
                values[valuesIndex++] = colors[index - 1].copy(alpha = 0f).toArgb()
            } else {
                val previousColor = colors[index - 1]
                values[valuesIndex++] = previousColor.copy(alpha = 0f).toArgb()

                val nextColor = colors[index + 1]
                values[valuesIndex++] = nextColor.copy(alpha = 0f).toArgb()
            }
        } else {
            values[valuesIndex++] = color.toArgb()
        }
    }
    return values
}

/**
 * See [makeTransparentColors].
 *
 * On N and earlier devices that have transparent values, we must duplicate the color stops for
 * fully transparent values so that the color value before and after can be interpolated.
 */
@VisibleForTesting
internal fun makeTransparentStops(
    stops: List<Float>?,
    colors: List<Color>,
    numTransparentColors: Int
): FloatArray? {
    if (numTransparentColors == 0) {
        return stops?.toFloatArray()
    }
    val newStops = FloatArray(colors.size + numTransparentColors)
    newStops[0] = stops?.get(0) ?: 0f
    var newStopsIndex = 1
    for (i in 1 until colors.lastIndex) {
        val color = colors[i]
        val stop = stops?.get(i) ?: i.toFloat() / colors.lastIndex
        newStops[newStopsIndex++] = stop
        if (color.alpha == 0f) {
            newStops[newStopsIndex++] = stop
        }
    }
    newStops[newStopsIndex] = stops?.get(colors.lastIndex) ?: 1f
    return newStops
}

private fun validateColorStops(colors: List<Color>, colorStops: List<Float>?) {
    if (colorStops == null) {
        if (colors.size < 2) {
            throw IllegalArgumentException(
                "colors must have length of at least 2 if colorStops " +
                    "is omitted."
            )
        }
    } else if (colors.size != colorStops.size) {
        throw IllegalArgumentException(
            "colors and colorStops arguments must have" +
                " equal length."
        )
    }
}