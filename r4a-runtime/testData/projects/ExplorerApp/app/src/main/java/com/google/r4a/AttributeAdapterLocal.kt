package com.google.r4a

import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import android.util.TypedValue
import android.util.DisplayMetrics
import java.util.regex.Pattern


//annotation class StringDimension
//@Retention(AnnotationRetention.SOURCE)
//annotation class ConflictsWith(val properties: Array<String>)

object AttributeAdapterLocal: AttributeAdapter() {

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

    private fun stringToIntPx(dimension: String, metrics: DisplayMetrics): Int {
        val dim = dimFromString(dimension)
        val value = dim.value
        val f = TypedValue.applyDimension(dim.unit, value, metrics)
        val res = (f + 0.5f).toInt()
        if (res != 0) return res
        if (value == 0f) return 0
        return if (value > 0) 1 else -1
    }

    private fun stringToFloatPx(dimension: String, metrics: DisplayMetrics): Float {
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
//            throw NumberFormatException()
            Dim(0f, TypedValue.COMPLEX_UNIT_PX)
        } else {
            Dim(value, dimensionUnit)
        }
    }

    private class Dim(val value: Float, val unit: Int)

    private fun displayMetrics(view: View): DisplayMetrics {
        return view.getResources().getDisplayMetrics()
    }






    // NOTE: these attributes are added to every view/component so that we can have a "key" attribute
    // that the users define to preserve state across composes. In the long run, we may decide to
    // use namespaces for these attributes, but for now, the grammar does not allow that and this
    // is a simple stop-gap solution.
    fun setKey(view: View, key: Any) {}
    fun setKey(component: Component, key: Any) {}

    fun setElevation(view: View, elevation: String) {
        view.setElevation(stringToFloatPx(elevation, displayMetrics(view)))
    }

    fun setTextSize(view: TextView, textSize: String) {
        view.setTextSize(stringToFloatPx(textSize, displayMetrics(view)))
    }

//    @ConflictsWith(arrayOf("paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "paddingHorizontal", "paddingHorizontal"))
    fun setPadding(view: View, padding: Int) {
        view.setPadding(padding, padding, padding, padding)
    }

    fun setPadding(view: View, padding: String) {
        setPadding(view, stringToIntPx(padding, displayMetrics(view)))
    }

//    @ConflictsWith(arrayOf("paddingLeft", "paddingRight"))
    fun setPaddingHorizontal(view: View, padding: Int) {
        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom())
    }

    fun setPaddingHorizontal(view: View, padding: String) {
        setPaddingHorizontal(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingVertical(view: View, padding: Int) {
        view.setPadding(view.getPaddingLeft(), padding, view.getPaddingRight(), padding)
    }

    fun setPaddingVertical(view: View, padding: String) {
        setPaddingVertical(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingLeft(view: View, padding: Int) {
        view.setPadding(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom())
    }

    fun setPaddingLeft(view: View, padding: String) {
        setPaddingLeft(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingTop(view: View, padding: Int) {
        view.setPadding(view.getPaddingLeft(), padding, view.getPaddingRight(), view.getPaddingBottom())
    }

    fun setPaddingTop(view: View, padding: String) {
        setPaddingTop(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingRight(view: View, padding: Int) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), padding, view.getPaddingBottom())
    }

    fun setPaddingRight(view: View, padding: String) {
        setPaddingRight(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingBottom(view: View, padding: Int) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), padding)
    }

    fun setPaddingBottom(view: View, padding: String) {
        setPaddingBottom(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setBackgroundColor(view: View, color: String) {
        view.setBackgroundColor(Color.parseColor(color))
    }

    fun setBufferType(view: TextView, bufferType: TextView.BufferType) {
        // TODO(lmr): this goes with setText. Not quite sure how to represent this. Wonder if
        // we should expose a bufferType property on TextView
    }



    // bufferType????

    //    fun setOnClick(view: Button, handler: Function0<Unit>) {
//        view.setOnClickListener(object: View.OnClickListener {
//            override fun onClick(v: View?) {
//                handler.invoke()
//            }
//        })
//    }
//
//    fun setWidth(view: View, width: Int) {
//        val current = view.layoutParams
//        view.layoutParams = LinearLayout.LayoutParams(width, current.height, 1.0f)
//    }
//
//    fun setHeight(view: View, height: Int) {
//        val current = view.layoutParams
//        view.layoutParams = LinearLayout.LayoutParams(current.width, height, 1.0f)
//    }
//
//    fun setWeight(view: View, weight: Float) {
//        val current = view.layoutParams
//        view.layoutParams = LinearLayout.LayoutParams(current.width, current.height, weight)
//    }
//
    fun setOrientation(view: LinearLayout, orientation: String) {
        when (orientation) {
            "vertical" -> view.setOrientation(LinearLayout.VERTICAL)
            "horizontal" -> view.setOrientation(LinearLayout.HORIZONTAL)
        }
    }

}
