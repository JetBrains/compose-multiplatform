/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.css

private fun toPrecision(value: Double, digits: Int): String = js("value.toPrecision(digits)")

private fun toJsNumberString(value: Double): String = js("value.toString()")

internal actual fun formatBinary32(value: Double): String {
    val float = value.toFloat()
    val double = float.toDouble()
    if (!float.isFinite()) return toJsNumberString(double)

    for (precision in 1..9) {
        val candidate = toPrecision(double, precision)
        if (candidate.toDouble().toFloat() == float) {
            return toJsNumberString(candidate.toDouble())
        }
    }
    return toJsNumberString(double)
}

internal actual fun narrowToFloat(value: Number): Float = value.toFloat()
