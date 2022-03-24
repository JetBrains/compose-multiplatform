/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class HSV(
    val hue: Float,
    val saturation: Float,
    val value: Float
)

private inline val Color.max get() = maxOf(red, green, blue)
private inline val Color.min get() = minOf(red, green, blue)

val Color.hsvHue
    get() = when {
        red > blue && green > blue -> 60 * (green - blue) / (max - min) + 0
        red < blue && green < blue -> 60 * (green - blue) / (max - min) + 360
        green == 1f -> 60 * (blue - red) / (max - min) + 120
        blue == 1f -> 60 * (red - green) / (max - min) + 240
        else -> 0f
    }

val Color.hsvSaturation
    get() = when {
        max == 0f -> 0f
        else -> 1 - min / max
    }

val Color.hsvValue get() = max

/**
 * https://www.rapidtables.com/convert/color/rgb-to-hsv.html
 */
fun Color.toHsv(): HSV {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == red -> 60 * ((green - blue) / delta).mod(6f)
        max == green -> 60 * ((blue - red) / delta + 2)
        max == blue -> 60 * ((red - green) / delta + 4)
        else -> 0f
    }
    val s = when {
        max == 0f -> 0f
        else -> delta / max
    }
    val v = max
    return HSV(
        hue = h,
        saturation = s,
        value = v
    )
}

/**
 * https://www.rapidtables.com/convert/color/hsv-to-rgb.html
 */
fun HSV.toRgb(): Color {
    val c = value * saturation
    val x = minOf(c * (1 - abs((hue / 60).mod(2f) - 1)), 1f)
    if (x.isNaN()) {
        println("x.isNaN()")
    }
    val m = value - c
    val tempColor = when {
        hue >= 0 && hue < 60 -> Color(c, x, 0f)
        hue >= 60 && hue < 120 -> Color(x, c, 0f)
        hue >= 120 && hue < 180 -> Color(0f, c, x)
        hue >= 180 && hue < 240 -> Color(0f, x, c)
        hue >= 240 && hue < 300 -> Color(x, 0f, c)
        else -> Color(c, 0f, x)
    }
    return Color(minOf(m + tempColor.red, 1f), minOf(m + tempColor.green, 1f), minOf( m + tempColor.blue, 1f))
}

