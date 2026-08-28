/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal actual fun formatBinary32(value: Double): String {
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
