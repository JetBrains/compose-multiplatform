package com.google.r4a.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.support.v7.widget.CardView
import android.view.View

import android.util.TypedValue
import android.util.DisplayMetrics
import android.view.Gravity
import android.widget.*
import com.bumptech.glide.Glide
import com.google.r4a.adapters.AttributeAdapter
import com.google.r4a.adapters.LocalUtils.displayMetrics
import com.google.r4a.adapters.LocalUtils.stringToFloatPx
import com.google.r4a.adapters.LocalUtils.stringToIntPx
import com.google.r4a.annotations.ConflictsWith
import java.util.regex.Pattern

fun View.setElevation(elevation: Dimension) = this.setElevation(elevation.toFloatPixels(displayMetrics(this)))

fun ImageView.setUri(uri: String) {
    Glide
        .with(getContext())
        .load(uri)
        .centerCrop()
        .into(this)
}

fun ImageView.setSrc(src: String) = setUri(src)

fun LinearLayout.setOrientation(orientation: String) {
    when(orientation) {
        "vertical" -> setOrientation(LinearLayout.VERTICAL)
        "horizontal" -> setOrientation(LinearLayout.HORIZONTAL)
    }
}