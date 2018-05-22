package com.google.r4a.adapters

import android.widget.ImageView

@Suppress("MemberVisibilityCanBePrivate", "unused")
object ImageVIewAttributeAdapter: AttributeAdapter() {

    fun setScaleType(view: ImageView, scaleType: String) {
        // NOTE(lmr): the xml version of these enums is a bit different... ie, "centerCrop", not "center_crop"
        view.scaleType = when (scaleType) {
            "center_crop" -> ImageView.ScaleType.CENTER_CROP
            "center" -> ImageView.ScaleType.CENTER
            "center_inside" -> ImageView.ScaleType.CENTER_INSIDE
            "fit_center" -> ImageView.ScaleType.FIT_CENTER
            "fit_end" -> ImageView.ScaleType.FIT_END
            "fit_start" -> ImageView.ScaleType.FIT_START
            "fit_xy" -> ImageView.ScaleType.FIT_XY
            "matrix" -> ImageView.ScaleType.MATRIX
            else -> throw IllegalArgumentException("unknown scaletype: $scaleType")
        }
    }

}