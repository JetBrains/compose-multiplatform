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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.util.lerp

/**
 * We encode the unit information and float value into the single 64-bit long integer.
 * The higher 32bit represents the metadata of this value and lower 32bit represents the bit
 * representation of the float value. Currently lower 8bits in the metadata bits are used for
 * unit information.
 *
 * Bits
 * |-------|-------|-------|-------|-------|-------|-------|-------|
 *                                  FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF: Float Value
 *                          UUUUUUUU                                : Unit Information
 *  XXXXXXXXXXXXXXXXXXXXXXXX                                        : Unused bits
 */
private const val UNIT_MASK = 0xFFL shl 32 // 0xFF_0000_0000
private const val UNIT_TYPE_UNSPECIFIED = 0x00L shl 32 // 0x00_0000_0000
private const val UNIT_TYPE_SP = 0x01L shl 32 // 0x01_0000_0000
private const val UNIT_TYPE_EM = 0x02L shl 32 // 0x2_0000_0000

/**
 * An enum class defining for type of [TextUnit].
 */
enum class TextUnitType {
    Unspecified,
    Sp,
    Em
}

/**
 * The unit used for text related dimension value.
 *
 * This unit can hold either scaled pixels (SP), relative font size (EM) and special unit
 * Unspecified for indicating inheriting from other style or using the default value. It can be
 * created with [sp] or [em]. (e.g. 15.sp or 18.em) which can be applied to [Int], [Double],
 * and [Float].
 *
 * Note that do not store this value in your persistent storage or send to another process since
 * the internal representation may be changed in future.
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class TextUnit(val packedValue: Long) {
    @Deprecated(
        message = "This function will be removed. Please perform the arithmetic operations " +
            "before creating the TextUnit.",
        replaceWith = ReplaceWith(
            "if(isSp) (value + other.value).sp else (value + other.value).em"
        )
    )
    /**
     * Add two [TextUnit]s together.
     *
     * This operation works only if all the operands are the same unit type and not they are not
     * equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit] and the [other] has different
     * [TextUnitType]s or either of the two has the [TextUnitType] equals to
     * [TextUnitType.Unspecified].
     */
    inline operator fun plus(other: TextUnit): TextUnit {
        checkArithmetic(this, other)
        return pack(rawType, value + other.value)
    }

    @Deprecated(
        message = "This function will be removed. Please perform the arithmetic operations " +
            "before creating the TextUnit.",
        replaceWith = ReplaceWith(
            "if(isSp) (value - other.value).sp else (value - other.value).em"
        )
    )
    /**
     * Subtract a [TextUnit] from another one.

     * This operation works only if all the operands are the same unit type and not they are not
     * equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit] and the [other] has different
     * [TextUnitType]s or either of the two has the [TextUnitType] equals to
     * [TextUnitType.Unspecified].
     */
    inline operator fun minus(other: TextUnit): TextUnit {
        checkArithmetic(this, other)
        return pack(rawType, value - other.value)
    }

    /**
     * This is the same as multiplying the [TextUnit] by -1.0.
     *
     * This operation works only if the operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun unaryMinus(): TextUnit {
        checkArithmetic(this)
        return pack(rawType, -value)
    }

    /**
     * Divide a [TextUnit] by a scalar.
     *
     * This operation works only if the left operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun div(other: Float): TextUnit {
        checkArithmetic(this)
        return pack(rawType, value / other)
    }

    /**
     * Divide a [TextUnit] by a scalar.
     *
     * This operation works only if the left operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun div(other: Double): TextUnit {
        checkArithmetic(this)
        return pack(rawType, (value / other).toFloat())
    }

    /**
     * Divide a [TextUnit] by a scalar.
     *
     * This operation works only if the left operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun div(other: Int): TextUnit {
        checkArithmetic(this)
        return pack(rawType, value / other)
    }

    @Deprecated(
        message = "This function will be removed. Please perform the arithmetic operations " +
            "before creating the TextUnit.",
        replaceWith = ReplaceWith(
            "value / other.value"
        )
    )
    /**
     * Divide by another [TextUnit] to get a scalar.
     *
     * This operation works only if all the operands are the same unit type and they are not
     * equal to [TextUnit.Unspecified].
     *
     * @throws IllegalArgumentException if this [TextUnit] and the [other] has different
     * [TextUnitType]s or either of the two has the [TextUnitType] equals to
     * [TextUnitType.Unspecified].
     */
    inline operator fun div(other: TextUnit): Float {
        checkArithmetic(this, other)
        return value / other.value
    }

    /**
     * Multiply a [TextUnit] by a scalar.
     *
     * This operation works only if the left operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun times(other: Float): TextUnit {
        checkArithmetic(this)
        return pack(rawType, value * other)
    }

    /**
     * Multiply a [TextUnit] by a scalar.
     *
     * This operation works only if the left operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun times(other: Double): TextUnit {
        checkArithmetic(this)
        return pack(rawType, (value * other).toFloat())
    }

    /**
     * Multiply a [TextUnit] by a scalar.
     *
     * This operation works only if the left operand is not equal to [TextUnit.Unspecified].
     * The result of this operation is the same unit type of the given one.
     *
     * @throws IllegalArgumentException if this [TextUnit]'s type is [TextUnitType.Unspecified].
     */
    inline operator fun times(other: Int): TextUnit {
        checkArithmetic(this)
        return pack(rawType, value * other)
    }

    /**
     * Support comparing Dimensions with comparison operators.
     *
     * @return 0 if this [TextUnit] equals to the [other], a negative number if it's less than the
     * [other], or a positive number if it's greater than the [other].
     * @throws IllegalArgumentException if this [TextUnit] and the [other] has different
     * [TextUnitType]s or either of the two has the [TextUnitType] equals to
     * [TextUnitType.Unspecified].
     */
    inline operator fun compareTo(other: TextUnit): Int {
        checkArithmetic(this, other)
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return when (type) {
            TextUnitType.Unspecified -> "Unspecified"
            TextUnitType.Sp -> "$value.sp"
            TextUnitType.Em -> "$value.em"
        }
    }

    companion object {
        internal val TextUnitTypes =
            arrayOf(TextUnitType.Unspecified, TextUnitType.Sp, TextUnitType.Em)

        @Deprecated(
            message = "This function will be removed. Please use Int.sp instead.",
            replaceWith = ReplaceWith(
                "value.sp",
                "androidx.compose.ui.unit.sp"
            )
        )
        /**
         * Creates a SP unit [TextUnit].
         */
        fun Sp(value: Int) = pack(UNIT_TYPE_SP, value.toFloat())

        @Deprecated(
            message = "This function will be removed. Please use Float.sp instead.",
            replaceWith = ReplaceWith(
                "value.sp",
                "androidx.compose.ui.unit.sp"
            )
        )
        /**
         * Creates a SP unit [TextUnit].
         */
        fun Sp(value: Float) = pack(UNIT_TYPE_SP, value)

        @Deprecated(
            message = "This function will be removed. Please use Double.sp instead.",
            replaceWith = ReplaceWith(
                "value.sp",
                "androidx.compose.ui.unit.sp"
            )
        )
        /**
         * Creates a SP unit [TextUnit].
         */
        fun Sp(value: Double) = pack(UNIT_TYPE_SP, value.toFloat())

        @Deprecated(
            message = "This function will be removed. Please use Int.em instead.",
            replaceWith = ReplaceWith(
                "value.em",
                "androidx.compose.ui.unit.em"
            )
        )
        /**
         * Creates an EM unit [TextUnit].
         */
        fun Em(value: Int) = pack(UNIT_TYPE_EM, value.toFloat())

        @Deprecated(
            message = "This function will be removed. Please use Float.em instead.",
            replaceWith = ReplaceWith(
                "value.em",
                "androidx.compose.ui.unit.em"
            )
        )
        /**
         * Creates an EM unit [TextUnit].
         */
        fun Em(value: Float) = pack(UNIT_TYPE_EM, value)

        @Deprecated(
            message = "This function will be removed. Please use Double.em instead.",
            replaceWith = ReplaceWith(
                "value.em",
                "androidx.compose.ui.unit.em"
            )
        )
        /**
         * Creates an EM unit [TextUnit].
         */
        fun Em(value: Double) = pack(UNIT_TYPE_EM, value.toFloat())

        /**
         * A special [TextUnit] instance for representing inheriting from parent value.
         *
         * Notice that performing arithmetic operations on [Unspecified] may result in an
         * [IllegalArgumentException].
         */
        @Stable
        val Unspecified = pack(UNIT_TYPE_UNSPECIFIED, Float.NaN)
    }

    /**
     * A helper function for getting underlying type information in raw bits.
     *
     * Use [TextUnit.type] in public places.
     */
    @PublishedApi
    internal val rawType: Long get() = packedValue and UNIT_MASK

    /**
     * A type information of this TextUnit.
     */
    val type: TextUnitType get() = TextUnitTypes[(rawType ushr 32).toInt()]

    /**
     * True if this is a SP unit type.
     */
    val isSp get() = rawType == UNIT_TYPE_SP

    /**
     * True if this is a EM unit type.
     */
    val isEm get() = rawType == UNIT_TYPE_EM

    /**
     * Returns the value of this [TextUnit].
     *
     * For example, the value of 3.sp equals to 3, and value of 5.em equals to 5. The value of
     * [TextUnit]s whose [TextUnitType] is [TextUnitType.Unspecified] is undefined.
     */
    val value get() = Float.fromBits((packedValue and 0xFFFF_FFFFL).toInt())
}

/**
 * `false` when this is [TextUnit.Unspecified].
 */
@Stable
inline val TextUnit.isSpecified: Boolean
    get() = !isUnspecified

/**
 * `true` when this is [TextUnit.Unspecified].
 */
@Stable
val TextUnit.isUnspecified: Boolean
    get() = rawType == UNIT_TYPE_UNSPECIFIED

/**
 * If this [TextUnit] [isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun TextUnit.takeOrElse(block: () -> TextUnit): TextUnit =
    if (isSpecified) this else block()

/**
 * Creates a SP unit [TextUnit]
 */
@Stable
val Float.sp: TextUnit get() = pack(UNIT_TYPE_SP, this)

/**
 * Creates an EM unit [TextUnit]
 */
@Stable
val Float.em: TextUnit get() = pack(UNIT_TYPE_EM, this)

/**
 * Creates a SP unit [TextUnit]
 */
@Stable
val Double.sp: TextUnit get() = pack(UNIT_TYPE_SP, this.toFloat())

/**
 * Creates an EM unit [TextUnit]
 */
@Stable
val Double.em: TextUnit get() = pack(UNIT_TYPE_EM, this.toFloat())

/**
 * Creates a SP unit [TextUnit]
 */
@Stable
val Int.sp: TextUnit get() = pack(UNIT_TYPE_SP, this.toFloat())

/**
 * Creates an EM unit [TextUnit]
 */
@Stable
val Int.em: TextUnit get() = pack(UNIT_TYPE_EM, this.toFloat())

/**
 * Multiply a [TextUnit] by a scalar.
 *
 * This operation works only if the right operand is not equal to [TextUnit.Unspecified].
 * The result of this operation is the same unit type of the given one.
 */
@Stable
inline operator fun Float.times(other: TextUnit): TextUnit {
    checkArithmetic(other)
    return pack(other.rawType, this * other.value)
}

/**
 * Multiply a [TextUnit] by a scalar.
 *
 * This operation works only if the right operand is not equal to [TextUnit.Unspecified].
 * The result of this operation is the same unit type of the given one.
 */
@Stable
inline operator fun Double.times(other: TextUnit): TextUnit {
    checkArithmetic(other)
    return pack(other.rawType, this.toFloat() * other.value)
}

/**
 * Multiply a [TextUnit] by a scalar.
 *
 * This operation works only if the right operand is not equal to [TextUnit.Unspecified].
 * The result of this operation is the same unit type of the given one.
 */
@Stable
inline operator fun Int.times(other: TextUnit): TextUnit {
    checkArithmetic(other)
    return pack(other.rawType, this * other.value)
}

@Deprecated(
    message = "This function will be removed, please perform the arithmetic operations " +
        "before creating the TextUnit.",
    replaceWith = ReplaceWith(
        "if (a.value < b.value) a else b",
        "import kotlin.math.min"
    )
)
/**
 * Returns the smaller value from the given values.
 *
 * This operation works only if all the operands are the same unit type and they are not
 * equal to [TextUnit.Unspecified].
 * The result of this operation is the same unit type of the given one.
 */
@Stable
inline fun min(a: TextUnit, b: TextUnit): TextUnit {
    checkArithmetic(a, b)
    return if (a.value < b.value) a else b
}

@Deprecated(
    message = "This function will be removed, please perform the arithmetic operations " +
        "before creating the TextUnit.",
    replaceWith = ReplaceWith(
        "if (a.value < b.value) b else a",
        "import kotlin.math.min"
    )
)
/**
 * Returns the smaller value from the given values.
 *
 * This operation works only if all the operands are the same unit type and they are not
 * equal to [TextUnit.Unspecified].
 * The result of this operation is the same unit type of the given one.
 */
@Stable
inline fun max(a: TextUnit, b: TextUnit): TextUnit {
    checkArithmetic(a, b)
    return if (a.value < b.value) b else a
}

@Deprecated(
    message = "This function will be removed, please perform the arithmetic operations " +
        "before creating the TextUnit.",
    replaceWith = ReplaceWith(
        "if(isSp) {\n" +
            "    value.coerceIn(minimumValue.value, maximumValue.value).sp\n" +
            "} else {\n" +
            "    value.coerceIn(minimumValue.value, maximumValue.value).em\n" +
            "}",
        "import kotlin.ranges.coerceIn"
    )
)
/**
 * Ensures that the value of [TextUnit] lies in the specified range [minimumValue]..[maximumValue].
 *
 *
 * This operation works only if all the operands are the same unit type and they are not
 * equal to [TextUnit.Unspecified].
 * The result of this operation is the same unit type of the given one.
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 *
 * @throws IllegalArgumentException if this [TextUnit], [minimumValue] and [maximumValue] don't
 * have the same [TextUnitType], or if any of this [TextUnit], [minimumValue] and [maximumValue]
 * has its [TextUnitType] equal to [TextUnitType.Unspecified].
 */
@Stable
inline fun TextUnit.coerceIn(minimumValue: TextUnit, maximumValue: TextUnit): TextUnit {
    checkArithmetic(this, minimumValue, maximumValue)
    return pack(rawType, value.coerceIn(minimumValue.value, maximumValue.value))
}

@Deprecated(
    message = "This function will be removed, please perform the arithmetic operations " +
        "before creating the TextUnit.",
    replaceWith = ReplaceWith(
        "if(isSp) {\n" +
            "    value.coerceAtLeast(minimumValue.value).sp\n" +
            "} else {\n" +
            "    value.coerceAtLeast(minimumValue.value).em\n" +
            "}",
        "import kotlin.ranges.coerceAtLeast"
    )
)
/**
 * Ensures that the value of [TextUnit] is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 *
 * @throws IllegalArgumentException if this [TextUnit] and [minimumValue] don't have the same
 * [TextUnitType], or if either of the two has its [TextUnitType] equal to
 * [TextUnitType.Unspecified].
 */
@Stable
inline fun TextUnit.coerceAtLeast(minimumValue: TextUnit): TextUnit {
    checkArithmetic(this, minimumValue)
    return pack(rawType, value.coerceAtLeast(minimumValue.value))
}

@Deprecated(
    message = "This function will be removed, please perform the arithmetic operations " +
        "before creating the TextUnit.",
    replaceWith = ReplaceWith(
        "if(isSp) {\n" +
            "    value.coerceAtMost(maximumValue.value).sp\n" +
            "} else {\n" +
            "    value.coerceAtMost(maximumValue.value).em\n" +
            "}",
        "import kotlin.ranges.coerceAtMost"
    )
)
/**
 * Ensures that the value of [TextUnit] is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 *
 * @throws IllegalArgumentException if this [TextUnit] and [maximumValue] don't have the same
 * [TextUnitType], or if either of the two has its [TextUnitType] equal to
 * [TextUnitType.Unspecified].
 */
@Stable
inline fun TextUnit.coerceAtMost(maximumValue: TextUnit): TextUnit {
    checkArithmetic(this, maximumValue)
    return pack(rawType, value.coerceAtMost(maximumValue.value))
}

@PublishedApi
internal inline fun pack(unitType: Long, v: Float): TextUnit =
    TextUnit(unitType or (v.toBits().toLong() and 0xFFFF_FFFFL))

@PublishedApi
internal fun checkArithmetic(a: TextUnit) {
    require(!a.isUnspecified) {
        "Cannot perform operation for Unspecified type."
    }
}

@PublishedApi
internal fun checkArithmetic(a: TextUnit, b: TextUnit) {
    require(!a.isUnspecified && !b.isUnspecified) {
        "Cannot perform operation for Unspecified type."
    }
    require(a.type == b.type) {
        "Cannot perform operation for ${a.type} and ${b.type}"
    }
}

@PublishedApi
internal fun checkArithmetic(a: TextUnit, b: TextUnit, c: TextUnit) {
    require(!a.isUnspecified && !b.isUnspecified && !c.isUnspecified) {
        "Cannot perform operation for Unspecified type."
    }
    require(a.type == b.type && b.type == c.type) {
        "Cannot perform operation for ${a.type} and ${b.type}"
    }
}

/**
 * Linearly interpolate between two [TextUnit]s.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 *
 * @throws IllegalArgumentException if [start] and [stop] have different [TextUnitType]s, or
 * either of the two has its [TextUnitType] equal to [TextUnitType.Unspecified].
 */
@Stable
fun lerp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit {
    checkArithmetic(start, stop)
    return pack(start.rawType, lerp(start.value, stop.value, fraction))
}
