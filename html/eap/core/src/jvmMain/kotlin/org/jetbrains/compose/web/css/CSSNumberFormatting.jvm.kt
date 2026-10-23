/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// Use exact rounding near ties and on older JDKs.
private val hasShortestFloatFormatting =
    (System.getProperty("java.specification.version").substringAfterLast('.').toIntOrNull() ?: 0) >=
        19

private val decimalPowers = DoubleArray(121) { java.lang.Double.parseDouble("1e${it - 60}") }

internal actual fun formatBinary32(value: Double): String {
    val float = value.toFloat()
    if (!float.isFinite()) return float.toString()
    if (float == 0f) return "0"
    if (!hasShortestFloatFormatting) return formatBinary32Exact(value)
    val magnitude = kotlin.math.abs(float.toDouble())
    var exponent = kotlin.math.floor(kotlin.math.log10(magnitude)).toInt()
    while (magnitude < decimalPowers[exponent + 60]) exponent--
    while (magnitude >= decimalPowers[exponent + 61]) exponent++
    // Start from JDK precision, keeping CSS rounding and spelling.
    val shortest = java.lang.Float.toString(float)
    var digitsSeen = 0
    var significantDigits = 0
    var canonicalDigits = 0L
    var started = false
    for (char in shortest) {
        if (char == 'E') break
        if (char !in '0'..'9') continue
        if (char != '0') started = true
        if (started) {
            digitsSeen++
            canonicalDigits = canonicalDigits * 10 + (char - '0')
            if (char != '0') significantDigits = digitsSeen
        }
    }
    while (canonicalDigits % 10L == 0L) canonicalDigits /= 10
    // Tiny values may need one digit instead of the JDK's two.
    val firstPrecision = if (significantDigits <= 2) 1 else significantDigits
    for (precision in firstPrecision..9) {
        val scale = exponent - precision + 1
        val scaled = magnitude / decimalPowers[scale + 60]
        val floor = kotlin.math.floor(scaled)
        // Allow four ulps for scaling error near a tie.
        if (kotlin.math.abs(scaled - floor - 0.5) <= 4 * java.lang.Math.ulp(scaled)) {
            return formatBinary32Exact(value)
        }
        val digits = (floor + if (scaled - floor >= 0.5) 1 else 0).toLong()
        var normalized = digits
        var normalizedScale = scale
        while (normalized % 10L == 0L) {
            normalized /= 10
            normalizedScale++
        }
        if (normalized == canonicalDigits && normalizedScale == exponent - significantDigits + 1) {
            // This decimal already round-trips. Only CSS notation may differ.
            if ('E' !in shortest && !shortest.endsWith(".0")) return shortest
            return decimalCssString(normalized, normalizedScale, float < 0f)
        }
        val candidate = decimalCssString(digits, scale, float < 0f)
        // Parse as Float to avoid rounding twice.
        if (java.lang.Float.parseFloat(candidate) == float) return candidate
    }
    return formatBinary32Exact(value)
}

private fun decimalCssString(unscaled: Long, scale: Int, negative: Boolean): String {
    var digits = unscaled
    var power = scale
    while (digits % 10L == 0L) {
        digits /= 10
        power++
    }
    val text = digits.toString()
    val exponent = text.length + power - 1
    return buildString {
        if (negative) append('-')
        if (exponent in -6..20) {
            val point = text.length + power
            when {
                point <= 0 -> {
                    append("0.")
                    repeat(-point) { append('0') }
                    append(text)
                }
                point >= text.length -> {
                    append(text)
                    repeat(power) { append('0') }
                }
                else -> {
                    append(text, 0, point)
                    append('.')
                    append(text, point, text.length)
                }
            }
        } else {
            append(text[0])
            if (text.length > 1) {
                append('.')
                append(text, 1, text.length)
            }
            append('e')
            if (exponent >= 0) append('+')
            append(exponent)
        }
    }
}

private fun formatBinary32Exact(value: Double): String {
    val float = value.toFloat()
    // NaN, infinities and values beyond Float range lack CSS syntax but share a spelling.
    if (!float.isFinite()) return float.toString()

    // Use the exact binary value, avoiding JDK Double/Float formatting (not shortest before 19).
    val exact = BigDecimal(float.toDouble())
    for (precision in 1..9) {
        // HALF_UP matches Number.prototype.toPrecision, which rounds ties away from zero.
        val candidate = exact.round(MathContext(precision, RoundingMode.HALF_UP))
        // BigDecimal.toFloat() is correctly rounded through exact-float division or decimal
        // parsing, so it matches parsing this candidate.
        if (candidate.toFloat() == float) return candidate.toCssString()
    }
    return exact.toCssString()
}

/** Lays a decimal out the way `Number.prototype.toString` lays out a number. */
private fun BigDecimal.toCssString(): String {
    val decimal = stripTrailingZeros()
    val exponent = decimal.precision() - decimal.scale() - 1
    if (exponent in -6..20) return decimal.toPlainString()

    val digits = decimal.unscaledValue().abs().toString()
    val sign = if (decimal.signum() < 0) "-" else ""
    val fraction = if (digits.length > 1) "." + digits.substring(1) else ""
    return "$sign${digits[0]}${fraction}e${if (exponent >= 0) "+" else ""}$exponent"
}

internal actual fun narrowToFloat(value: Number): Float = value.toFloat()
