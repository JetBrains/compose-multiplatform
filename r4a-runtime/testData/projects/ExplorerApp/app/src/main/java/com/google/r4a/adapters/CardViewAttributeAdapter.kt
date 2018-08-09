package com.google.r4a.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.support.v7.widget.CardView
import com.google.r4a.adapters.LocalUtils.displayMetrics
import com.google.r4a.adapters.LocalUtils.stringToFloatPx
import com.google.r4a.adapters.LocalUtils.stringToIntPx

fun CardView.setCardBackgroundColor(color: String) {
    setCardBackgroundColor(Color.parseColor(color))
}

fun CardView.setRadius(radius: Dimension) {
    setRadius(radius.toFloatPixels(displayMetrics(this)))
}

fun CardView.setCardElevation(elevation: Dimension) {
    setCardElevation(elevation.toFloatPixels(displayMetrics(this)))
}

fun CardView.setMaxCardElevation(elevation: Dimension) {
    setMaxCardElevation(elevation.toFloatPixels(displayMetrics(this)))
}

fun CardView.setContentPadding(padding: Int) {
    setContentPadding(padding, padding, padding, padding)
}

fun CardView.setContentPadding(padding: Dimension) {
    setContentPadding(padding.toIntPixels(displayMetrics(this)))
}

fun CardView.setContentPaddingHorizontal(padding: Int) {
    setContentPadding(padding, getContentPaddingTop(), padding, getContentPaddingBottom())
}

fun CardView.setContentPaddingHorizontal(padding: Dimension) {
    setContentPaddingHorizontal(padding.toIntPixels(displayMetrics(this)))
}

fun CardView.setContentPaddingVertical(padding: Int) {
    setContentPadding(getContentPaddingLeft(), padding, getContentPaddingRight(), padding)
}

fun CardView.setContentPaddingVertical(padding: Dimension) {
    setContentPaddingVertical(padding.toIntPixels(displayMetrics(this)))
}

fun CardView.setContentPaddingLeft(padding: Int) {
    setContentPadding(padding, getContentPaddingTop(), getContentPaddingRight(), getContentPaddingBottom())
}

fun CardView.setContentPaddingLeft(padding: Dimension) {
    setContentPaddingLeft(padding.toIntPixels(displayMetrics(this)))
}

fun CardView.setContentPaddingTop(padding: Int) {
    setContentPadding(getContentPaddingLeft(), padding, getContentPaddingRight(), getContentPaddingBottom())
}

fun CardView.setContentPaddingTop(padding: Dimension) {
    setContentPaddingTop(padding.toIntPixels(displayMetrics(this)))
}

fun CardView.setContentPaddingRight(padding: Int) {
    setContentPadding(getContentPaddingLeft(), getContentPaddingTop(), padding, getContentPaddingBottom())
}

fun CardView.setContentPaddingRight(padding: Dimension) {
    setContentPaddingRight(padding.toIntPixels(displayMetrics(this)))
}

fun CardView.setContentPaddingBottom(padding: Int) {
    setContentPadding(getContentPaddingLeft(), getContentPaddingTop(), getContentPaddingRight(), padding)
}

fun CardView.setContentPaddingBottom(padding: Dimension) {
    setContentPaddingBottom(padding.toIntPixels(displayMetrics(this)))
}