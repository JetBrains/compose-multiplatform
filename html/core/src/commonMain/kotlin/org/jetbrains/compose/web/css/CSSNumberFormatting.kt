/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.math.abs
import kotlin.math.floor

// 2^53; above this, a Double skips integers.
private const val EXACT_INTEGER_LIMIT = 9007199254740992.0

/**
 * Produces identical CSS text on all platforms for server/browser hydration.
 *
 * Keeps every [Long] and other integers below 2^53 exact. Other values use the shortest decimal
 * that round-trips to the same binary32 [Float]. Kotlin/JS leaves [Float] values and arithmetic
 * at double precision (`Double.toFloat()` is a no-op), so binary32 is the only shared precision.
 *
 * Trade-offs:
 *
 * - [Double] digits beyond binary32 are lost: `0.1234567890123456` becomes `0.12345679`.
 *   Custom properties follow this rule, so `--var` cannot retain high precision.
 * - Non-[Long] integers from 2^53 are rounded: `9007199254740992.0` becomes
 *   `9007199000000000`; use a [Long] to keep them exact.
 * - Values beyond the [Float] range become `Infinity` or `-Infinity`; values below its smallest
 *   denormal, such as `1e-46`, become `0`. These are invalid CSS, but both targets spell them
 *   identically.
 */
internal fun formatCssNumber(value: Number): String {
    // Longs remain exact beyond Double's consecutive-integer range.
    if (value is Long) return value.toString()

    val double = value.toDouble()
    if (double == floor(double) && abs(double) < EXACT_INTEGER_LIMIT) {
        // Check by value: Kotlin/JS reports whole Doubles as Int; rounding loses JVM-kept digits.
        return double.toLong().toString()
    }

    return formatBinary32(double)
}

/**
 * Narrows [value] to binary32, then returns its shortest 1–9-significant-digit round-trip using
 * `Number.prototype.toString` layout.
 */
internal expect fun formatBinary32(value: Double): String

/**
 * Rounds [value] to binary32, which `Number.toFloat()` does not do on Kotlin/JS.
 *
 * Callers remain responsible for [Float] arithmetic done before CSS APIs.
 */
internal expect fun narrowToFloat(value: Number): Float
