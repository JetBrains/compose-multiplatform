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


object AttributeAdapterLocal: AttributeAdapter() {

    // does this requires a support lib?
    fun setElevation(view: View, elevation: String) {
        view.setElevation(stringToFloatPx(elevation, displayMetrics(view)))
    }

    fun setUri(view: ImageView, uri: String) {
        Glide
                .with(view.getContext())
                .load(uri)
                .centerCrop()
                .into(view)
    }

    fun setSrc(view: ImageView, src: String) {
        setUri(view, src)
    }

    fun setOrientation(view: LinearLayout, orientation: String) {
        when (orientation) {
            "vertical" -> view.setOrientation(LinearLayout.VERTICAL)
            "horizontal" -> view.setOrientation(LinearLayout.HORIZONTAL)
        }
    }

}
