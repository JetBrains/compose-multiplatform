/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * The `Float16` class is a wrapper and a utility class to manipulate half-precision 16-bit
 * [IEEE 754](https://en.wikipedia.org/wiki/Half-precision_floating-point_format)
 * floating point data types (also called fp16 or binary16). A half-precision float can be
 * created from or converted to single-precision floats, and is stored in a short data type.
 * To distinguish short values holding half-precision floats from regular short values,
 * it is recommended to use the `@HalfFloat` annotation.
 *
 * The IEEE 754 standard specifies an fp16 as having the following format:
 *
 *  * Sign bit: 1 bit
 *  * Exponent width: 5 bits
 *  * Significand: 10 bits
 *
 * The format is laid out as follows:
 *     1   11111   1111111111
 *     ^   --^--   -----^----
 *     sign  |          |_______ significand
 *     |
 *     -- exponent
 *
 * Half-precision floating points can be useful to save memory and/or
 * bandwidth at the expense of range and precision when compared to single-precision
 * floating points (fp32).
 *
 * To help you decide whether fp16 is the right storage type for you need, please
 * refer to the table below that shows the available precision throughout the range of
 * possible values. The *precision* column indicates the step size between two
 * consecutive numbers in a specific part of the range.
 *
 * <table summary="Precision of fp16 across the range">
 * <tr><th>Range start</th><th>Precision</th></tr>
 * <tr><td>0</td><td>1  16,777,216</td></tr>
 * <tr><td>1  16,384</td><td>1  16,777,216</td></tr>
 * <tr><td>1  8,192</td><td>1  8,388,608</td></tr>
 * <tr><td>1  4,096</td><td>1  4,194,304</td></tr>
 * <tr><td>1  2,048</td><td>1  2,097,152</td></tr>
 * <tr><td>1  1,024</td><td>1  1,048,576</td></tr>
 * <tr><td>1  512</td><td>1  524,288</td></tr>
 * <tr><td>1  256</td><td>1  262,144</td></tr>
 * <tr><td>1  128</td><td>1  131,072</td></tr>
 * <tr><td>1  64</td><td>1  65,536</td></tr>
 * <tr><td>1  32</td><td>1  32,768</td></tr>
 * <tr><td>1  16</td><td>1  16,384</td></tr>
 * <tr><td>1  8</td><td>1  8,192</td></tr>
 * <tr><td>1  4</td><td>1  4,096</td></tr>
 * <tr><td>1  2</td><td>1  2,048</td></tr>
 * <tr><td>1</td><td>1  1,024</td></tr>
 * <tr><td>2</td><td>1  512</td></tr>
 * <tr><td>4</td><td>1  256</td></tr>
 * <tr><td>8</td><td>1  128</td></tr>
 * <tr><td>16</td><td>1  64</td></tr>
 * <tr><td>32</td><td>1  32</td></tr>
 * <tr><td>64</td><td>1  16</td></tr>
 * <tr><td>128</td><td>1  8</td></tr>
 * <tr><td>256</td><td>1  4</td></tr>
 * <tr><td>512</td><td>1  2</td></tr>
 * <tr><td>1,024</td><td>1</td></tr>
 * <tr><td>2,048</td><td>2</td></tr>
 * <tr><td>4,096</td><td>4</td></tr>
 * <tr><td>8,192</td><td>8</td></tr>
 * <tr><td>16,384</td><td>16</td></tr>
 * <tr><td>32,768</td><td>32</td></tr>
 * </table>
 *
 *
 * This table shows that numbers higher than 1024 lose all fractional precision.
 */
@kotlin.jvm.JvmInline
internal value class Float16(val halfValue: Short) : Comparable<Float16> {

    /**
     * Constructs a newly allocated `Float16` object that represents the
     * argument converted to a half-precision float.
     *
     * @param value The value to be represented by the `Float16`
     */
    constructor(value: Float) : this(
        floatToHalf(
            value
        )
    )

    /**
     * Constructs a newly allocated `Float16` object that
     * represents the argument converted to a half-precision float.
     *
     * @param value The value to be represented by the `Float16`
     */
    constructor(value: Double) : this(value.toFloat())

    /**
     * Returns the value of this `Float16` as a `Byte` after
     * a narrowing primitive conversion.
     *
     * @return The half-precision float value represented by this object
     * converted to type `Byte`
     */
    fun toByte(): Byte {
        return toFloat().toInt().toByte()
    }

    /**
     * Returns the value of this `Float16` as a `Short` after
     * a narrowing primitive conversion.
     *
     * @return The half-precision float value represented by this object
     * converted to type `Short`
     */
    fun toShort(): Short {
        return toFloat().toInt().toShort()
    }

    /**
     * Returns the value of this `Float16` as a `Int` after
     * a narrowing primitive conversion.
     *
     * @return The half-precision float value represented by this object
     * converted to type `Int`
     */
    fun toInt(): Int {
        return toFloat().toInt()
    }

    /**
     * Returns the value of this `Float16` as a `Long` after
     * a narrowing primitive conversion.
     *
     * @return The half-precision float value represented by this object
     * converted to type `Long`
     */
    fun toLong(): Long {
        return toFloat().toLong()
    }

    /**
     * Returns the value of this `Float16` as a `Float` after
     * a widening primitive conversion.
     *
     * @return The half-precision float value represented by this object
     * converted to type `Float`
     */
    fun toFloat(): Float {
        val bits = halfValue.toInt() and 0xffff
        val s = bits and FP16_SIGN_MASK
        val e = bits.ushr(FP16_EXPONENT_SHIFT) and FP16_EXPONENT_MASK
        val m = bits and FP16_SIGNIFICAND_MASK

        var outE = 0
        var outM = 0

        if (e == 0) { // Denormal or 0
            if (m != 0) {
                // Convert denorm fp16 into normalized fp32
                var o = Float.fromBits(FP32_DENORMAL_MAGIC + m)
                o -= FP32_DENORMAL_FLOAT
                return if (s == 0) o else -o
            }
        } else {
            outM = m shl 13
            if (e == 0x1f) { // Infinite or NaN
                outE = 0xff
                if (outM != 0) { // SNaNs are quieted
                    outM = outM or FP32_QNAN_MASK
                }
            } else {
                outE = e - FP16_EXPONENT_BIAS + FP32_EXPONENT_BIAS
            }
        }

        val out = s shl 16 or (outE shl FP32_EXPONENT_SHIFT) or outM
        return Float.fromBits(out)
    }

    /**
     * Returns the value of this `Float16` as a `Double` after
     * a widening primitive conversion.
     *
     * @return The half-precision float value represented by this object
     * converted to type `Double`
     */
    fun toDouble(): Double {
        return toFloat().toDouble()
    }

    /**
     * Returns a representation of the half-precision float value
     * according to the bit layout described in [Float16].
     *
     * Unlike [toRawBits], this method collapses all
     * possible Not-a-Number values to a single canonical Not-a-Number value
     * defined by [NaN].
     *
     * @return The bits that represent the half-precision float value
     */
    fun toBits(): Int {
        return if (isNaN()) {
            NaN.halfValue.toInt()
        } else {
            halfValue.toInt() and 0xffff
        }
    }

    /**
     * Returns a representation of the half-precision float value
     * according to the bit layout described in [Float16].
     *
     * @return The bits that represent the half-precision float value
     */
    fun toRawBits(): Int {
        return halfValue.toInt() and 0xffff
    }

    /**
     * Returns a string representation of the specified half-precision
     * float value. See [toString] for more information.
     *
     * @return A string representation of this `Float16` object
     */
    override fun toString(): String {
        return toFloat().toString()
    }

    /**
     * Compares to another half-precision float value. The following
     * conditions apply during the comparison:
     *
     *  * [NaN] is considered by this method to be equal to itself and greater
     * than all other half-precision float values (including `#PositiveInfinity`)
     *  * [PositiveZero] is considered by this method to be greater than
     * [NegativeZero].
     *
     * @param other The half-precision float value to compare to the half-precision value
     * represented by this `Float16` object
     *
     * @return The value `0` if `this` is numerically equal to `h`; a
     * value less than `0` if `this` is numerically less than `h`;
     * and a value greater than `0` if `this` is numerically greater
     * than `h`
     */
    override operator fun compareTo(other: Float16): Int {
        if (isNaN()) {
            return if (other.isNaN()) 0 else 1
        } else if (other.isNaN()) {
            return -1
        }
        return toCompareValue(halfValue).compareTo(
            toCompareValue(other.halfValue)
        )
    }

    /**
     * Returns the sign of this half-precision float value.
     * * `-1.0` if the value is negative,
     * * zero if the value is zero,
     * * `1.0` if the value is positive
     *
     * Special case:
     * * `NaN.sign` is `NaN`
     */
    val sign: Float16
        get() {
            if (isNaN()) {
                return NaN
            }
            when {
                this < NegativeZero -> return NegativeOne
                this > PositiveZero -> return One
                else -> return this // this is zero, either positive or negative
            }
        }

    /**
     * Returns a [Float16] with the magnitude of this and the sign of [sign]
     */
    fun withSign(sign: Float16): Float16 =
        Float16(
            (
                sign.halfValue.toInt() and FP16_SIGN_MASK or
                    (halfValue.toInt() and FP16_COMBINED)
                ).toShort()
        )

    /**
     * Returns the absolute value of the half-precision float.
     * Special values are handled in the following ways:
     *
     *  * If the specified half-precision float is [NaN], the result is [NaN]
     *  * If the specified half-precision float is zero (negative or positive),
     * the result is positive zero (see [PositiveZero])
     *  * If the specified half-precision float is infinity (negative or positive),
     * the result is positive infinity (see [PositiveInfinity])
     */
    fun absoluteValue(): Float16 {
        return Float16((halfValue.toInt() and FP16_COMBINED).toShort())
    }

    /**
     * Returns the closest integral half-precision float value to the this
     * half-precision float value. Special values are handled in the
     * following ways:
     *
     *  * If the specified half-precision float is [NaN], the result is [NaN]
     *  * If the specified half-precision float is infinity (negative or positive),
     * the result is infinity (with the same sign)
     *  * If the specified half-precision float is zero (negative or positive),
     * the result is zero (with the same sign)
     *
     * @return The value of the specified half-precision float rounded to the nearest
     * half-precision float value
     */
    fun round(): Float16 {
        val bits = halfValue.toInt() and 0xffff
        var e = bits and 0x7fff
        var result = bits

        if (e < 0x3c00) {
            result = result and FP16_SIGN_MASK
            result = result or (0x3c00 and if (e >= 0x3800) 0xffff else 0x0)
        } else if (e < 0x6400) {
            e = 25 - (e shr 10)
            val mask = (1 shl e) - 1
            result += 1 shl e - 1
            result = result and mask.inv()
        }

        return Float16(result.toShort())
    }

    /**
     * Returns the smallest half-precision float value toward negative infinity
     * greater than or equal to this half-precision float value.
     * Special values are handled in the following ways:
     *
     *  * If the specified half-precision float is [NaN], the result is [NaN]
     *  * If the specified half-precision float is infinity (negative or positive),
     * the result is infinity (with the same sign)
     *  * If the specified half-precision float is zero (negative or positive),
     * the result is zero (with the same sign)
     *
     * @return The smallest half-precision float value toward negative infinity
     * greater than or equal to the half-precision float value
     */
    fun ceil(): Float16 {
        val bits = halfValue.toInt() and 0xffff
        var e = bits and 0x7fff
        var result = bits

        if (e < 0x3c00) {
            result = result and FP16_SIGN_MASK
            result = result or (0x3c00 and -((bits shr 15).inv() and if (e != 0) 1 else 0))
        } else if (e < 0x6400) {
            e = 25 - (e shr 10)
            val mask = (1 shl e) - 1
            result += mask and (bits shr 15) - 1
            result = result and mask.inv()
        }

        return Float16(result.toShort())
    }

    /**
     * Returns the largest half-precision float value toward positive infinity
     * less than or equal to this half-precision float value.
     * Special values are handled in the following ways:
     *
     *  * If the specified half-precision float is [NaN], the result is [NaN]
     *  * If the specified half-precision float is infinity (negative or positive),
     * the result is infinity (with the same sign)
     *  * If the specified half-precision float is zero (negative or positive),
     * the result is zero (with the same sign)
     *
     * @return The largest half-precision float value toward positive infinity
     * less than or equal to the half-precision float value
     */
    fun floor(): Float16 {
        val bits = halfValue.toInt() and 0xffff
        var e = bits and 0x7fff
        var result = bits

        if (e < 0x3c00) {
            result = result and FP16_SIGN_MASK
            result = result or (0x3c00 and if (bits > 0x8000) 0xffff else 0x0)
        } else if (e < 0x6400) {
            e = 25 - (e shr 10)
            val mask = (1 shl e) - 1
            result += mask and -(bits shr 15)
            result = result and mask.inv()
        }

        return Float16(result.toShort())
    }

    /**
     * Returns the truncated half-precision float value of this
     * half-precision float value. Special values are handled in the following ways:
     *
     *  * If the specified half-precision float is NaN, the result is NaN
     *  * If the specified half-precision float is infinity (negative or positive),
     * the result is infinity (with the same sign)
     *  * If the specified half-precision float is zero (negative or positive),
     * the result is zero (with the same sign)
     *
     * @return The truncated half-precision float value of the
     * half-precision float value
     */
    fun trunc(): Float16 {
        val bits = halfValue.toInt() and 0xffff
        var e = bits and 0x7fff
        var result = bits

        if (e < 0x3c00) {
            result = result and FP16_SIGN_MASK
        } else if (e < 0x6400) {
            e = 25 - (e shr 10)
            val mask = (1 shl e) - 1
            result = result and mask.inv()
        }

        return Float16(result.toShort())
    }

    /**
     * The unbiased exponent used in the representation of
     * the specified  half-precision float value. if the value is NaN
     * or infinite, this* method returns [MaxExponent] + 1.
     * If the argument is 0 or a subnormal representation, this method
     * returns [MinExponent] - 1.
     */
    val exponent: Int
        get() {
            return (halfValue.toInt().ushr(FP16_EXPONENT_SHIFT) and FP16_EXPONENT_MASK) -
                FP16_EXPONENT_BIAS
        }

    /**
     * The significand, or mantissa, used in the representation
     * of this half-precision float value.
     */
    val significand: Int
        get() {
            return halfValue.toInt() and FP16_SIGNIFICAND_MASK
        }

    /**
     * Returns true if this `Float16` value represents a Not-a-Number,
     * false otherwise.
     *
     * @return True if the value is a NaN, false otherwise
     */
    fun isNaN(): Boolean {
        return halfValue.toInt() and FP16_COMBINED > FP16_EXPONENT_MAX
    }

    /**
     * Returns true if the half-precision float value represents
     * infinity, false otherwise.
     *
     * @return True if the value is positive infinity or negative infinity,
     * false otherwise
     */
    fun isInfinite(): Boolean {
        return halfValue.toInt() and FP16_COMBINED == FP16_EXPONENT_MAX
    }

    /**
     * Returns false if the half-precision float value represents
     * infinity, true otherwise.
     *
     * @return False if the value is positive infinity or negative infinity,
     * true otherwise
     */
    fun isFinite(): Boolean {
        return halfValue.toInt() and FP16_COMBINED != FP16_EXPONENT_MAX
    }

    /**
     * Returns true if the half-precision float value is normalized
     * (does not have a subnormal representation). If the specified value is
     * [PositiveInfinity], [NegativeInfinity],
     * [PositiveZero], [NegativeZero], [NaN] or any subnormal
     * number, this method returns false.
     *
     * @return True if the value is normalized, false otherwise
     */
    fun isNormalized(): Boolean {
        return halfValue.toInt() and FP16_EXPONENT_MAX != 0 &&
            halfValue.toInt() and FP16_EXPONENT_MAX != FP16_EXPONENT_MAX
    }

    /**
     *
     * Returns a hexadecimal string representation of the half-precision
     * float value. If the value is a NaN, the result is `"NaN"`,
     * otherwise the result follows this format:
     *
     *  * If the sign is positive, no sign character appears in the result
     *  * If the sign is negative, the first character is `'-'`
     *  * If the value is inifinity, the string is `"Infinity"`
     *  * If the value is 0, the string is `"0x0.0p0"`
     *  * If the value has a normalized representation, the exponent and
     * significand are represented in the string in two fields. The significand
     * starts with `"0x1."` followed by its lowercase hexadecimal
     * representation. Trailing zeroes are removed unless all digits are 0, then
     * a single zero is used. The significand representation is followed by the
     * exponent, represented by `"p"`, itself followed by a decimal
     * string of the unbiased exponent
     *  * If the value has a subnormal representation, the significand starts
     * with `"0x0."` followed by its lowercase hexadecimal
     * representation. Trailing zeroes are removed unless all digits are 0, then
     * a single zero is used. The significand representation is followed by the
     * exponent, represented by `"p-14"`
     *
     * @return A hexadecimal string representation of the specified value
     */
    fun toHexString(): String {
        val o = StringBuilder()

        val bits = halfValue.toInt() and 0xffff
        val s = bits.ushr(FP16_SIGN_SHIFT)
        val e = bits.ushr(FP16_EXPONENT_SHIFT) and FP16_EXPONENT_MASK
        val m = bits and FP16_SIGNIFICAND_MASK

        if (e == 0x1f) { // Infinite or NaN
            if (m == 0) {
                if (s != 0) o.append('-')
                o.append("Infinity")
            } else {
                o.append("NaN")
            }
        } else {
            if (s == 1) o.append('-')
            if (e == 0) {
                if (m == 0) {
                    o.append("0x0.0p0")
                } else {
                    o.append("0x0.")
                    val significand = m.toString(16)
                    o.append(significand.replaceFirst("0{2,}$".toRegex(), ""))
                    o.append("p-14")
                }
            } else {
                o.append("0x1.")
                val significand = m.toString(16)
                o.append(significand.replaceFirst("0{2,}$".toRegex(), ""))
                o.append('p')
                o.append((e - FP16_EXPONENT_BIAS).toString())
            }
        }

        return o.toString()
    }

    companion object {
        /**
         * The number of bits used to represent a half-precision float value.
         */
        const val Size = 16

        /**
         * Epsilon is the difference between 1.0 and the next value representable
         * by a half-precision floating-point.
         */
        val Epsilon = Float16(0x1400.toShort())

        /**
         * Maximum exponent a finite half-precision float may have.
         */
        const val MaxExponent = 15
        /**
         * Minimum exponent a normalized half-precision float may have.
         */
        const val MinExponent = -14

        /**
         * Smallest negative value a half-precision float may have.
         */
        val LowestValue = Float16(0xfbff.toShort())
        /**
         * Maximum positive finite value a half-precision float may have.
         */
        val MaxValue = Float16(0x7bff.toShort())
        /**
         * Smallest positive normal value a half-precision float may have.
         */
        val MinNormal = Float16(0x0400.toShort())
        /**
         * Smallest positive non-zero value a half-precision float may have.
         */
        val MinValue = Float16(0x0001.toShort())
        /**
         * A Not-a-Number representation of a half-precision float.
         */
        val NaN = Float16(0x7e00.toShort())
        /**
         * Negative infinity of type half-precision float.
         */
        val NegativeInfinity = Float16(0xfc00.toShort())
        /**
         * Negative 0 of type half-precision float.
         */
        val NegativeZero = Float16(0x8000.toShort())
        /**
         * Positive infinity of type half-precision float.
         */
        val PositiveInfinity = Float16(0x7c00.toShort())
        /**
         * Positive 0 of type half-precision float.
         */
        val PositiveZero = Float16(0x0000.toShort())

        private val One = Float16(1f)
        private val NegativeOne = Float16(-1f)

        private const val FP16_SIGN_SHIFT = 15
        private const val FP16_SIGN_MASK = 0x8000
        private const val FP16_EXPONENT_SHIFT = 10
        private const val FP16_EXPONENT_MASK = 0x1f
        private const val FP16_SIGNIFICAND_MASK = 0x3ff
        private const val FP16_EXPONENT_BIAS = 15
        private const val FP16_COMBINED = 0x7fff
        private const val FP16_EXPONENT_MAX = 0x7c00

        private const val FP32_SIGN_SHIFT = 31
        private const val FP32_EXPONENT_SHIFT = 23
        private const val FP32_EXPONENT_MASK = 0xff
        private const val FP32_SIGNIFICAND_MASK = 0x7fffff
        private const val FP32_EXPONENT_BIAS = 127
        private const val FP32_QNAN_MASK = 0x400000

        private const val FP32_DENORMAL_MAGIC = 126 shl 23
        private val FP32_DENORMAL_FLOAT = Float.fromBits(FP32_DENORMAL_MAGIC)

        private fun toCompareValue(value: Short): Int {
            return if (value.toInt() and FP16_SIGN_MASK != 0) {
                0x8000 - (value.toInt() and 0xffff)
            } else {
                value.toInt() and 0xffff
            }
        }

        private fun floatToHalf(f: Float): Short {
            val bits = f.toRawBits()
            val s = bits.ushr(FP32_SIGN_SHIFT)
            var e = bits.ushr(FP32_EXPONENT_SHIFT) and FP32_EXPONENT_MASK
            var m = bits and FP32_SIGNIFICAND_MASK

            var outE = 0
            var outM = 0

            if (e == 0xff) { // Infinite or NaN
                outE = 0x1f
                outM = if (m != 0) 0x200 else 0
            } else {
                e = e - FP32_EXPONENT_BIAS + FP16_EXPONENT_BIAS
                if (e >= 0x1f) { // Overflow
                    outE = 0x31
                } else if (e <= 0) { // Underflow
                    if (e < -10) {
                        // The absolute fp32 value is less than MIN_VALUE, flush to +/-0
                    } else {
                        // The fp32 value is a normalized float less than MIN_NORMAL,
                        // we convert to a denorm fp16
                        m = m or 0x800000 shr 1 - e
                        if (m and 0x1000 != 0) m += 0x2000
                        outM = m shr 13
                    }
                } else {
                    outE = e
                    outM = m shr 13
                    if (m and 0x1000 != 0) {
                        // Round to nearest "0.5" up
                        var out = outE shl FP16_EXPONENT_SHIFT or outM
                        out++
                        return (out or (s shl FP16_SIGN_SHIFT)).toShort()
                    }
                }
            }

            return (s shl FP16_SIGN_SHIFT or (outE shl FP16_EXPONENT_SHIFT) or outM).toShort()
        }
    }
}

/**
 * Returns the smaller of two half-precision float values (the value closest
 * to negative infinity). Special values are handled in the following ways:
 *
 *  * If either value is [Float16.NaN], the result is [Float16.NaN]
 *  * [Float16.NegativeZero] is smaller than [Float16.PositiveZero]
 *
 * @param x The first half-precision value
 * @param y The second half-precision value
 * @return The smaller of the two specified half-precision values
 */
internal fun min(x: Float16, y: Float16): Float16 {
    if (x.isNaN() || y.isNaN()) {
        return Float16.NaN
    }

    return if (x <= y) x else y
}

/**
 * Returns the larger of two half-precision float values (the value closest
 * to positive infinity). Special values are handled in the following ways:
 *
 *  * If either value is [Float16.NaN], the result is [Float16.NaN]
 *  * [Float16.PositiveZero] is greater than [Float16.NegativeZero]
 *
 * @param x The first half-precision value
 * @param y The second half-precision value
 *
 * @return The larger of the two specified half-precision values
 */
internal fun max(x: Float16, y: Float16): Float16 {
    if (x.isNaN() || y.isNaN()) {
        return Float16.NaN
    }
    return if (x >= y) x else y
}