package com.google.r4a.examples.explorerapp.common.adapters

import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.google.r4a.CompositionContext
import com.google.r4a.adapters.Dimension
import com.makeramen.roundedimageview.RoundedImageView

// TODO(lmr): This provides a way for people to get the int/float value of dimensions without immediate
// access to a DisplayMetrics instance. Ideally, we never need this... but it is a useful stop-gap solution for now
// until we figure out a better way to separate styling from view instances
val Dimension.int get() = toIntPixels(CompositionContext.current.context.resources.displayMetrics)
val Dimension.float get() = toFloatPixels(CompositionContext.current.context.resources.displayMetrics)

fun View.setElevation(elevation: Dimension) = setElevation(elevation.toFloatPixels(metrics))
fun ImageView.setUri(uri: String) {
    Glide
            .with(context)
            .load(uri)
            .into(this)
}