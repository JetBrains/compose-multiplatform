package com.google.r4a.adapters

import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import java.util.regex.Pattern

internal val View.metrics: DisplayMetrics get() = resources.displayMetrics

/**
 * This function will take in a string and pass back a valid resource identifier for View.setTag(...). We should eventually move this
 * to a resource id that's actually generated via AAPT but doing that in this project is proving to be complicated, so for now I'm
 * just doing this as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}

internal object Utils {



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

    internal fun stringToIntPx(dimension: String, metrics: DisplayMetrics): Int {
        val dim = dimFromString(dimension)
        val value = dim.value
        val f = TypedValue.applyDimension(dim.unit, value, metrics)
        val res = (f + 0.5f).toInt()
        if (res != 0) return res
        if (value == 0f) return 0
        return if (value > 0) 1 else -1
    }

    internal fun stringToFloatPx(dimension: String, metrics: DisplayMetrics): Float {
        val dim = dimFromString(dimension)
        return TypedValue.applyDimension(dim.unit, dim.value, metrics)
    }

    private fun dimFromString(dimension: String): Dim {
        val matcher = DIMENSION_PATTERN.matcher(dimension)

        if (!matcher.matches()) throw NumberFormatException()
        val value = matcher.group(1).toFloat()
        val unit = matcher.group(3).toLowerCase()
        val dimensionUnit = DIMENSION_UNIT[unit]
        return if (dimensionUnit == null) {
            throw NumberFormatException(dimension)
        } else {
            Dim(value, dimensionUnit)
        }
    }

    private class Dim(val value: Float, val unit: Int)

}