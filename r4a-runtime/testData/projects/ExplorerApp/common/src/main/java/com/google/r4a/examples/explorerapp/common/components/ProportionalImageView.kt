package com.google.r4a.examples.explorerapp.common.components

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide


// TODO(lmr): it turns out we really want to set these two properties together.
// once we have some sort of an adapter story figured out, we can do this differently
data class ImageSpec(
        val uri: String,
        val aspectRatio: Float
)

/**
 * An ImageView subclass that is built to enforce an aspect ratio of an image, instead
 * of specifying an explicit width/height.
 */
class CardImageView(context: Context) : ImageView(context) {
    var spec: ImageSpec? = null
        set(value) {
            field = value
            if (value != null) {
                val w = measuredWidth

                val b = Glide.with(context)
                        .load(value.uri)
                        .centerCrop()

                if (w > 0) {
                    b.override(w, (w.toFloat() * value.aspectRatio).toInt())
                }

                b.into(this)
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val d = drawable
        val spec = spec
        if (spec != null && spec.aspectRatio > 0) {
            val w = measuredWidth
            val h = (w.toFloat() * spec.aspectRatio).toInt()
            setMeasuredDimension(w, h)
        } else if (d != null) {
            val w = measuredWidth
            val h = (w.toFloat() * d.intrinsicHeight.toFloat() / d.intrinsicWidth.toFloat()).toInt()
            setMeasuredDimension(w, h)
        }
    }
}