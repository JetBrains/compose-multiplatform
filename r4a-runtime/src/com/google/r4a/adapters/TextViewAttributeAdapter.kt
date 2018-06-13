@file:Suppress("unused", "UNUSED_PARAMETER", "UsePropertyAccessSyntax")

package com.google.r4a.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.google.r4a.adapters.Utils.stringToFloatPx
import com.google.r4a.annotations.Aesthetic
import com.google.r4a.annotations.ColorString
import com.google.r4a.annotations.DimensionString

@DimensionString
fun TextView.setTextSize(size: String) {
    textSize = stringToFloatPx(size, metrics)
}

fun TextView.setTextSize(size: Dimension) = setTextSize(size.toFloatPixels(metrics))

fun TextView.setFontFamily(fontFamily: String) {
    typeface = Typeface.create(fontFamily, typeface.style)
}


fun TextView.setFontStyle(style: Int) {
    typeface = Typeface.create(typeface, style)
}

fun TextView.setBufferType(bufferType: TextView.BufferType) {
    // TODO(lmr): this goes with setText. Not quite sure how to represent this. Wonder if
    // we should expose a bufferType property on TextView
}

@Aesthetic
fun TextView.setGravity(enumString: String) {
    gravity = when (enumString) {
        "no_gravity" -> Gravity.NO_GRAVITY
        "axis_specified" -> Gravity.AXIS_SPECIFIED
        "axis_pull_before" -> Gravity.AXIS_PULL_BEFORE
        "axis_pull_after" -> Gravity.AXIS_PULL_AFTER
        "axis_clip" -> Gravity.AXIS_CLIP
        "axis_x_shift" -> Gravity.AXIS_X_SHIFT
        "axis_y_shift" -> Gravity.AXIS_Y_SHIFT
        "top" -> Gravity.TOP
        "bottom" -> Gravity.BOTTOM
        "left" -> Gravity.LEFT
        "right" -> Gravity.RIGHT
        "center_vertical" -> Gravity.CENTER_VERTICAL
        "fill_vertical" -> Gravity.FILL_VERTICAL
        "center_horizontal" -> Gravity.CENTER_HORIZONTAL
        "fill_horizontal" -> Gravity.FILL_HORIZONTAL
        "center" -> Gravity.CENTER
        "fill" -> Gravity.FILL
        "clip_vertical" -> Gravity.CLIP_VERTICAL
        "clip_horizontal" -> Gravity.CLIP_HORIZONTAL
        "horizontal_gravity_mask" -> Gravity.HORIZONTAL_GRAVITY_MASK
        "vertical_gravity_mask" -> Gravity.VERTICAL_GRAVITY_MASK
        "display_clip_vertical" -> Gravity.DISPLAY_CLIP_VERTICAL
        "display_clip_horizontal" -> Gravity.DISPLAY_CLIP_HORIZONTAL
        else -> throw IllegalArgumentException("Unknown gravity string: $gravity")
    }
}

@ColorString
fun TextView.setHighlightColor(color: String) = setHighlightColor(Color.parseColor(color))

@ColorString
fun TextView.setHintTextColor(color: String) = setHintTextColor(Color.parseColor(color))

@ColorString
fun TextView.setLinkTextColor(color: String) = setLinkTextColor(Color.parseColor(color))

@ColorString
fun TextView.setTextColor(color: String) = setTextColor(Color.parseColor(color))


fun TextView.setCompoundDrawablePadding(compoundDrawablePadding: Dimension) =
    setCompoundDrawablePadding(compoundDrawablePadding.toIntPixels(metrics))

fun TextView.setHeight(height: Dimension) = setHeight(height.toIntPixels(metrics))
fun TextView.setWidth(width: Dimension) = setWidth(width.toIntPixels(metrics))
fun TextView.setMaxHeight(maxHeight: Dimension) = setMaxHeight(maxHeight.toIntPixels(metrics))
fun TextView.setMaxWidth(maxWidth: Dimension) = setMaxWidth(maxWidth.toIntPixels(metrics))
fun TextView.setMinHeight(minHeight: Dimension) = setMinHeight(minHeight.toIntPixels(metrics))
fun TextView.setMinWidth(minWidth: Dimension) = setMinWidth(minWidth.toIntPixels(metrics))