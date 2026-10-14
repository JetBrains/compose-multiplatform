/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
package org.jetbrains.compose.web.css

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class FractionalCssFormattingTest {
    @Test
    fun matchesExactHalfUpReferenceAcrossBinary32Range() {
        val random = Random(381)
        repeat(100_000) { compare(Float.fromBits(random.nextInt())) }
        for (exponent in -45..38) {
            val center = java.lang.Float.parseFloat("1e$exponent").toRawBits()
            for (offset in -32..32) {
                compare(Float.fromBits(center + offset))
                compare(-Float.fromBits(center + offset))
            }
        }
        listOf(2070951.25f, -2070951.25f, Float.MIN_VALUE, Float.MAX_VALUE,
            1207866.25f, 0f, -0f, Float.NaN, Float.POSITIVE_INFINITY).forEach(::compare)
    }
    @Test
    fun priorityFallbackSerializesEachValueOnce() {
        var calls = 0
        val value = object : StylePropertyValue {
            override fun toString(): String { calls++; return "0.33333334" }
        }
        val style = StyleScopeBuilder().apply {
            property("opacity", value, true)
            variable("fraction", value)
        }
        assertEquals("opacity: 0.33333334 !important; --fraction: 0.33333334", style.toStyleAttributeValue())
        assertEquals(2, calls)
    }

    @Test
    fun emptyAssignmentFallbackReusesEarlierConversions() {
        var calls = 0
        val value = object : StylePropertyValue {
            override fun toString(): String { calls++; return "0.33333334" }
        }
        val style = StyleScopeBuilder().apply {
            property("opacity", value)
            property("removed", "unused")
            property("removed", "")
            variable("fraction", value)
        }
        assertEquals("opacity: 0.33333334; --fraction: 0.33333334", style.toStyleAttributeValue())
        assertEquals(2, calls)
    }

    private fun compare(value: Float) {
        assertEquals(reference(value), formatBinary32(value.toDouble()), "bits=${value.toRawBits()}")
    }
    private fun reference(value: Float): String {
        if (!value.isFinite()) return value.toString()
        if (value == 0f) return "0"
        val exact = BigDecimal(value.toDouble())
        val decimal = (1..9).firstNotNullOfOrNull { precision ->
            exact.round(MathContext(precision, RoundingMode.HALF_UP)).takeIf { it.toFloat() == value }
        }!!.stripTrailingZeros()
        val exponent = decimal.precision() - decimal.scale() - 1
        if (exponent in -6..20) return decimal.toPlainString()
        val digits = decimal.unscaledValue().abs().toString()
        return (if (value < 0f) "-" else "") + digits[0] +
            (if (digits.length > 1) "." + digits.substring(1) else "") +
            "e" + (if (exponent >= 0) "+" else "") + exponent
    }
}
