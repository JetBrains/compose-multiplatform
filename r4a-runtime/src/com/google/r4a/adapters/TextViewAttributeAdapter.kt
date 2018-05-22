package com.google.r4a.adapters

import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import com.google.r4a.adapters.Utils.displayMetrics
import com.google.r4a.adapters.Utils.stringToFloatPx
import com.google.r4a.annotations.DimensionString

@Suppress("MemberVisibilityCanBePrivate", "unused")
object TextViewAttributeAdapter: AttributeAdapter() {

    @DimensionString
    fun setTextSize(view: TextView, textSize: String) {
        view.textSize = stringToFloatPx(textSize, displayMetrics(view))
    }

    fun setFontFamily(view: TextView, fontFamily: String) {
        view.typeface = Typeface.create(fontFamily, view.typeface.style)
    }

    fun setBufferType(view: TextView, bufferType: TextView.BufferType) {
        // TODO(lmr): this goes with setText. Not quite sure how to represent this. Wonder if
        // we should expose a bufferType property on TextView
    }

    fun setGravity(view: TextView, gravity: String) {
        view.gravity = when (gravity) {
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
}