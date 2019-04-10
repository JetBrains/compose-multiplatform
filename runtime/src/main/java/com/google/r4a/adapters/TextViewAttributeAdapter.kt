@file:Suppress("unused", "UNUSED_PARAMETER", "UsePropertyAccessSyntax")

package com.google.r4a.adapters

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView

fun TextView.setTextSize(size: Dimension) =
    setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloatPixels(metrics))

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

fun TextView.setCompoundDrawablePadding(compoundDrawablePadding: Dimension) =
    setCompoundDrawablePadding(compoundDrawablePadding.toIntPixels(metrics))

fun TextView.setHeight(height: Dimension) = setHeight(height.toIntPixels(metrics))
fun TextView.setWidth(width: Dimension) = setWidth(width.toIntPixels(metrics))
fun TextView.setMaxHeight(maxHeight: Dimension) = setMaxHeight(maxHeight.toIntPixels(metrics))
fun TextView.setMaxWidth(maxWidth: Dimension) = setMaxWidth(maxWidth.toIntPixels(metrics))
fun TextView.setMinHeight(minHeight: Dimension) = setMinHeight(minHeight.toIntPixels(metrics))
fun TextView.setMinWidth(minWidth: Dimension) = setMinWidth(minWidth.toIntPixels(metrics))