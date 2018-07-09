@file:Suppress("unused")

package com.google.r4a.adapters

import android.util.DisplayMetrics
import kotlin.math.roundToInt


const val UNIT_TYPE_PX = 1
const val UNIT_TYPE_DIP = 2
const val UNIT_TYPE_SP = 3
const val UNIT_TYPE_PT = 4
const val UNIT_TYPE_IN = 5
const val UNIT_TYPE_MM = 6
// TODO(lmr): degrees/radians ?


private inline val Int.scalar: Dimension get() = IntDimension(this, UNIT_TYPE_PX)
private inline val Float.scalar: Dimension get() = FloatDimension(this, UNIT_TYPE_PX)
private inline val Double.scalar: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_PX)

interface Dimension {
    fun toIntPixels(metrics: DisplayMetrics): Int
    fun toFloatPixels(metrics: DisplayMetrics): Float

    operator fun times(rhs: Int): Dimension = CombinedDimension(this, rhs.scalar, INT_MULT, FLOAT_MULT)
    operator fun div(rhs: Int): Dimension = CombinedDimension(this, rhs.scalar, INT_DIV, FLOAT_DIV)
    operator fun times(rhs: Float): Dimension = CombinedDimension(this, rhs.scalar, INT_MULT, FLOAT_MULT)
    operator fun div(rhs: Float): Dimension = CombinedDimension(this, rhs.scalar, INT_DIV, FLOAT_DIV)
    operator fun times(rhs: Double): Dimension = CombinedDimension(this, rhs.scalar, INT_MULT, FLOAT_MULT)
    operator fun div(rhs: Double): Dimension = CombinedDimension(this, rhs.scalar, INT_DIV, FLOAT_DIV)
    operator fun plus(dim: Dimension): Dimension = CombinedDimension(this, dim, INT_PLUS, FLOAT_PLUS)
}

operator fun Int.times(dim: Dimension): Dimension = CombinedDimension(this.scalar, dim, INT_MULT, FLOAT_MULT)
operator fun Int.div(dim: Dimension): Dimension = CombinedDimension(this.scalar, dim, INT_DIV, FLOAT_DIV)
operator fun Float.times(dim: Dimension): Dimension = CombinedDimension(this.scalar, dim, INT_MULT, FLOAT_MULT)
operator fun Float.div(dim: Dimension): Dimension = CombinedDimension(this.scalar, dim, INT_DIV, FLOAT_DIV)
operator fun Double.times(dim: Dimension): Dimension = CombinedDimension(this.scalar, dim, INT_MULT, FLOAT_MULT)
operator fun Double.div(dim: Dimension): Dimension = CombinedDimension(this.scalar, dim, INT_DIV, FLOAT_DIV)

private val INT_PLUS = { a: Int, b: Int -> a + b }
private val FLOAT_PLUS = { a: Float, b: Float -> a + b }
private val INT_MULT = { a: Int, b: Int -> a * b }
private val FLOAT_MULT = { a: Float, b: Float -> a * b }
private val INT_DIV = { a: Int, b: Int -> a / b }
private val FLOAT_DIV = { a: Float, b: Float -> a / b }
private val INT_MINUS = { a: Int, b: Int -> a - b }
private val FLOAT_MINUS = { a: Float, b: Float -> a - b }

private data class CombinedDimension(
    val left: Dimension,
    val right: Dimension,
    val iop: (Int, Int) -> Int,
    val fop: (Float, Float) -> Float
) : Dimension {
    override fun toIntPixels(metrics: DisplayMetrics) = iop(left.toIntPixels(metrics), right.toIntPixels(metrics))
    override fun toFloatPixels(metrics: DisplayMetrics) = fop(left.toFloatPixels(metrics), right.toFloatPixels(metrics))
}

data class IntDimension(val value: Int, val unit: Int) : Dimension {
    override fun toIntPixels(metrics: DisplayMetrics): Int = toFloatPixels(metrics).roundToInt()
    override fun toFloatPixels(metrics: DisplayMetrics): Float = when (unit) {
        UNIT_TYPE_PX -> value.toFloat()
        UNIT_TYPE_DIP -> value * metrics.density
        UNIT_TYPE_SP -> value * metrics.scaledDensity
        UNIT_TYPE_PT -> value * metrics.xdpi * (1.0f / 72)
        UNIT_TYPE_IN -> value * metrics.xdpi
        UNIT_TYPE_MM -> value * metrics.xdpi * (1.0f / 25.4f)
        else -> error("Unrecognized unit")
    }
}

data class FloatDimension(val value: Float, val unit: Int) : Dimension {
    override fun toIntPixels(metrics: DisplayMetrics): Int = toFloatPixels(metrics).roundToInt()
    override fun toFloatPixels(metrics: DisplayMetrics): Float = when (unit) {
        UNIT_TYPE_PX -> value
        UNIT_TYPE_DIP -> value * metrics.density
        UNIT_TYPE_SP -> value * metrics.scaledDensity
        UNIT_TYPE_PT -> value * metrics.xdpi * (1.0f / 72)
        UNIT_TYPE_IN -> value * metrics.xdpi
        UNIT_TYPE_MM -> value * metrics.xdpi * (1.0f / 25.4f)
        else -> error("Unrecognized unit")
    }
}

inline val Int.px: Dimension get() = IntDimension(this, UNIT_TYPE_PX)
inline val Int.dp: Dimension get() = IntDimension(this, UNIT_TYPE_DIP)
inline val Int.dip: Dimension get() = IntDimension(this, UNIT_TYPE_DIP)
inline val Int.sp: Dimension get() = IntDimension(this, UNIT_TYPE_SP)
inline val Int.pt: Dimension get() = IntDimension(this, UNIT_TYPE_PT)
inline val Int.inches: Dimension get() = IntDimension(this, UNIT_TYPE_IN)
inline val Int.mm: Dimension get() = IntDimension(this, UNIT_TYPE_MM)

inline val Float.px: Dimension get() = FloatDimension(this, UNIT_TYPE_PX)
inline val Float.dp: Dimension get() = FloatDimension(this, UNIT_TYPE_DIP)
inline val Float.dip: Dimension get() = FloatDimension(this, UNIT_TYPE_DIP)
inline val Float.sp: Dimension get() = FloatDimension(this, UNIT_TYPE_SP)
inline val Float.pt: Dimension get() = FloatDimension(this, UNIT_TYPE_PT)
inline val Float.inches: Dimension get() = FloatDimension(this, UNIT_TYPE_IN)
inline val Float.mm: Dimension get() = FloatDimension(this, UNIT_TYPE_MM)

inline val Double.px: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_PX)
inline val Double.dp: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_DIP)
inline val Double.dip: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_DIP)
inline val Double.sp: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_SP)
inline val Double.pt: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_PT)
inline val Double.inches: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_IN)
inline val Double.mm: Dimension get() = FloatDimension(this.toFloat(), UNIT_TYPE_MM)
