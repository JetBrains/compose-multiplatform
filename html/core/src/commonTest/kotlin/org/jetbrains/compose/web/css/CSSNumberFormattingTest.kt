package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// Shared JVM/JS expectations: platform-specific output cannot survive hydration.
class CSSNumberFormattingTest {
    @Test
    fun integersKeepTheirExactDigits() {
        assertEquals("7", formatCssNumber(7))
        assertEquals("0", formatCssNumber(-0.0))
        assertEquals("0", formatCssNumber(-0.0f))
        assertEquals("1", formatCssNumber(1.0))
        assertEquals("1", formatCssNumber(1.0f))
        // Beyond binary32 precision, so rounding to a Float would drop digits.
        assertEquals("2147483647", formatCssNumber(2147483647))
        assertEquals("2147483647", formatCssNumber(2147483647.0))
        assertEquals("9007199254740993", formatCssNumber(9_007_199_254_740_993L))
    }

    @Test
    fun fractionalValuesUseCssExponentBoundaries() {
        assertEquals("100000000000000000000", formatCssNumber(1e20))
        assertEquals("1e+21", formatCssNumber(1e21))
        assertEquals("0.000001", formatCssNumber(1e-6))
        assertEquals("1e-7", formatCssNumber(1e-7))
    }

    @Test
    fun nonNumbersKeepTheirSpelling() {
        assertEquals("NaN", formatCssNumber(Double.NaN))
        assertEquals("Infinity", formatCssNumber(Double.POSITIVE_INFINITY))
        assertEquals("-Infinity", formatCssNumber(Double.NEGATIVE_INFINITY))
    }

    // Binary32 edges: invalid or inexact CSS must still match across platforms for hydration.
    @Test
    fun binary32RoundingIsObservableAtTheEdges() {
        // Out of the Float range once narrowed.
        assertEquals("Infinity", formatCssNumber(1e39))
        assertEquals("-Infinity", formatCssNumber(-1e39))
        // Below the smallest denormal.
        assertEquals("0", formatCssNumber(1e-46))
        assertEquals("0", formatCssNumber(-1e-46))
        // Integral, but no longer exact from 2^53 up.
        assertEquals("9007199000000000", formatCssNumber(9007199254740992.0))
        assertEquals("9007199254740992", formatCssNumber(9_007_199_254_740_992L))
        // Integral and below 2^53, so exact even though binary32 cannot represent it.
        assertEquals("16777217", formatCssNumber(16_777_217))
    }

    @Test
    fun fractionalValuesAreSerializedAtBinary32Precision() {
        assertEquals("0.33333334", formatCssNumber(divide(1f, 3f)))
        assertEquals("33.333332", formatCssNumber(divide(100f, 3f)))
        assertEquals("1e-45", formatCssNumber(runtimeValue(Float.MIN_VALUE)))
        assertEquals("3.4028235e+38", formatCssNumber(runtimeValue(Float.MAX_VALUE)))
        // A Double carries no more precision than a Float through CSS.
        assertEquals("0.12345679", formatCssNumber(0.1234567890123456))
    }

    @Test
    fun midpointsRoundAwayFromZero() {
        // Both -2070951.2 and -2070951.3 round-trip to this Float. We deliberately choose the
        // latter on both platforms; JDK Float.toString breaks the tie toward the even digit.
        assertEquals("-2070951.3", formatCssNumber(runtimeValue(-2070951.25f)))
        assertEquals("2070951.3", formatCssNumber(runtimeValue(2070951.25f)))
        assertEquals("1207866.3", formatCssNumber(Float.fromBits(1_234_399_698)))
    }

    @Test
    fun runtimeFloatsKeepBinary32FormattingThroughNumberApis() {
        val alpha = divide(1f, 3f)
        val percentage = divide(100f, 3f)
        val style = StyleScopeBuilder()

        style.property("opacity", alpha)
        style.variable("alpha", alpha)

        assertEquals("opacity: 0.33333334; --alpha: 0.33333334", style.toStyleAttributeValue())
        assertEquals("0.33333334", StylePropertyValue(alpha).toString())
        assertEquals("33.333332%", percentage.percent.toString())
        assertEquals("rgba(0, 0, 0, 0.33333334)", rgba(0, 0, 0, alpha).toString())
        assertEquals("rgb(0.33333334, 0.33333334, 0.33333334)", rgb(alpha, alpha, alpha).toString())
        assertEquals("hsl(90deg, 33.333332%, 33.333332%)", hsl(90, percentage, percentage).toString())
        assertEquals(
            "hsla(90deg, 33.333332%, 33.333332%, 0.33333334)",
            hsla(90, percentage, percentage, alpha).toString(),
        )
        val size: CSSNumericValue<CSSUnit.px> = 10.px
        assertEquals("calc(10px * 0.33333334)", (size * alpha).toString())
        assertEquals("calc(10px / 0.33333334)", (size / alpha).toString())
    }

    @Test
    fun arithmeticOverCssValuesAgreesAcrossPlatforms() {
        val alpha = divide(1f, 3f)

        // CSS storage narrows before arithmetic, aligning values rather than only text.
        // Kotlin/JS previously computed 10 * 0.3333333333333333 and produced 3.3333333px.
        assertEquals("3.3333335px", (10.px * alpha).toString())
        assertEquals("30px", (10.px / alpha).toString())
        assertEquals("33.333332%", (100.percent / 3).toString())
        assertEquals("3.3333333px", (10.px / 3).toString())
        assertEquals("15.5px", (10.px + 5.5.px).toString())
    }

    @OptIn(ExperimentalComposeWebApi::class)
    @Test
    fun runtimeFloatsKeepBinary32FormattingInTransformsAndFilters() {
        val alpha = divide(1f, 3f)
        val style = StyleScopeBuilder()

        style.transform {
            matrix(alpha, alpha, alpha, alpha, alpha, alpha)
            scale(alpha)
            rotate3d(alpha, alpha, alpha, 90.deg)
        }
        style.filter { opacity(alpha) }

        assertEquals(
            "transform: matrix(0.33333334, 0.33333334, 0.33333334, 0.33333334, 0.33333334, 0.33333334) " +
                "scale(0.33333334) rotate3d(0.33333334, 0.33333334, 0.33333334, 90deg); " +
                "filter: opacity(0.33333334)",
            style.toStyleAttributeValue(),
        )
    }

    @Test
    fun formattedValuesRoundTripToTheSameBinary32Value() {
        val random = Random(0)
        repeat(10_000) {
            val value = Float.fromBits(random.nextInt())
            if (value.isFinite()) {
                assertEquals(
                    value.toBits(),
                    formatCssNumber(value).toFloat().toBits(),
                    "Failed to round-trip ${value.toBits().toString(16)}",
                )
            }
        }
    }

    // Runtime computation prevents constant folding from hiding Kotlin/JS's lack of narrowing.
    private fun divide(numerator: Float, denominator: Float): Float = numerator / denominator

    private fun runtimeValue(value: Float): Float = value
}
