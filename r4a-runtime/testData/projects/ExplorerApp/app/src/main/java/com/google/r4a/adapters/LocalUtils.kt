package com.google.r4a.adapters

import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import com.google.r4a.CompositionContext
import java.util.regex.Pattern

object LocalUtils {

    private val DIMENSION_PATTERN = Pattern.compile("^\\s*(\\d+(\\.\\d+)*)\\s*([a-zA-Z]+)\\s*$")
    private val DIMENSION_UNIT = mapOf(
            "px" to TypedValue.COMPLEX_UNIT_PX,
            "dip" to TypedValue.COMPLEX_UNIT_DIP,
            "dp" to TypedValue.COMPLEX_UNIT_DIP,
            "sp" to TypedValue.COMPLEX_UNIT_SP,
            "pt" to TypedValue.COMPLEX_UNIT_PT,
            "in" to TypedValue.COMPLEX_UNIT_IN,
            "mm" to TypedValue.COMPLEX_UNIT_MM
    )

    fun stringToIntPx(dimension: String, metrics: DisplayMetrics): Int {
        val dim = dimFromString(dimension)
        val value = dim.value
        val f = TypedValue.applyDimension(dim.unit, value, metrics)
        val res = (f + 0.5f).toInt()
        if (res != 0) return res
        if (value == 0f) return 0
        return if (value > 0) 1 else -1
    }

    fun stringToIntPx(dimension: String): Int {
        val ctxt = CompositionContext.current.context
        val metrics = ctxt.getResources().getDisplayMetrics()
        return stringToIntPx(dimension, metrics)
    }

    fun stringToFloatPx(dimension: String, metrics: DisplayMetrics): Float {
        val dim = dimFromString(dimension)
        return TypedValue.applyDimension(dim.unit, dim.value, metrics)
    }

    fun stringToFloatPx(dimension: String): Float {
        val ctxt = CompositionContext.current.context
        val metrics = ctxt.getResources().getDisplayMetrics()
        return stringToFloatPx(dimension, metrics)
    }

    private fun dimFromString(dimension: String): Dim {
        val matcher = DIMENSION_PATTERN.matcher(dimension)

        if (!matcher.matches()) throw NumberFormatException()
        val value = matcher.group(1).toFloat()
        val unit = matcher.group(3).toLowerCase()
        val dimensionUnit = DIMENSION_UNIT[unit]
        return if (dimensionUnit == null) {
            // can't throw when compiling with IR
//            throw NumberFormatException(dimension)
            return Dim(0f, 0)
        } else {
            Dim(value, dimensionUnit)
        }
    }

    private class Dim(val value: Float, val unit: Int)
    fun displayMetrics(view: View): DisplayMetrics {
        return view.getResources().getDisplayMetrics()
    }
}