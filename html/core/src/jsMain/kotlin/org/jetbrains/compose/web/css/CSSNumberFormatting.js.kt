/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

private fun fround(value: Double): Double = js("Math.fround(value)")

private fun toPrecision(value: Double, digits: Int): String = js("value.toPrecision(digits)")

internal actual fun formatBinary32(value: Double): String {
    // Kotlin/JS retains Float values at double precision; narrow explicitly.
    val float = fround(value)
    if (!float.isFinite()) return float.toString()

    for (precision in 1..9) {
        val candidate = toPrecision(float, precision)
        // Parsing restores JS Number layout, which the JVM implementation mirrors.
        if (fround(candidate.toDouble()) == float) return candidate.toDouble().toString()
    }
    return float.toString()
}

// Kotlin/JS toFloat() does not narrow; use Math.fround.
internal actual fun narrowToFloat(value: Number): Float = fround(value.toDouble()).toFloat()
