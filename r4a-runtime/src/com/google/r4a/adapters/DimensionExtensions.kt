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

interface Dimension {
    fun toIntPixels(metrics: DisplayMetrics): Int
    fun toFloatPixels(metrics: DisplayMetrics): Float
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

inline val Int.px get() = IntDimension(this, UNIT_TYPE_PX)
inline val Int.dp get() = IntDimension(this, UNIT_TYPE_DIP)
inline val Int.dip get() = IntDimension(this, UNIT_TYPE_DIP)
inline val Int.sp get() = IntDimension(this, UNIT_TYPE_SP)
inline val Int.pt get() = IntDimension(this, UNIT_TYPE_PT)
inline val Int.inches get() = IntDimension(this, UNIT_TYPE_IN)
inline val Int.mm get() = IntDimension(this, UNIT_TYPE_MM)

inline val Float.px get() = FloatDimension(this, UNIT_TYPE_PX)
inline val Float.dp get() = FloatDimension(this, UNIT_TYPE_DIP)
inline val Float.dip get() = FloatDimension(this, UNIT_TYPE_DIP)
inline val Float.sp get() = FloatDimension(this, UNIT_TYPE_SP)
inline val Float.pt get() = FloatDimension(this, UNIT_TYPE_PT)
inline val Float.inches get() = FloatDimension(this, UNIT_TYPE_IN)
inline val Float.mm get() = FloatDimension(this, UNIT_TYPE_MM)

inline val Double.px get() = FloatDimension(this.toFloat(), UNIT_TYPE_PX)
inline val Double.dp get() = FloatDimension(this.toFloat(), UNIT_TYPE_DIP)
inline val Double.dip get() = FloatDimension(this.toFloat(), UNIT_TYPE_DIP)
inline val Double.sp get() = FloatDimension(this.toFloat(), UNIT_TYPE_SP)
inline val Double.pt get() = FloatDimension(this.toFloat(), UNIT_TYPE_PT)
inline val Double.inches get() = FloatDimension(this.toFloat(), UNIT_TYPE_IN)
inline val Double.mm get() = FloatDimension(this.toFloat(), UNIT_TYPE_MM)
