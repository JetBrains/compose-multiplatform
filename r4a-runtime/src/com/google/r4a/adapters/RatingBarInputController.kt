package com.google.r4a.adapters

import android.widget.RatingBar

class RatingBarInputController(view: RatingBar) : RatingBar.OnRatingBarChangeListener, InputController<RatingBar, Float>(view) {

    override fun getValue(): Float = view.rating

    override fun setValue(value: Float) {
        view.rating = value
    }

    var onRatingChange: Function1<Float, Unit>? = null

    override fun onRatingChanged(ratingBar: RatingBar?, rating: Float, fromUser: Boolean) {
        onRatingChange?.invoke(rating)
        afterChangeEvent(rating)
    }
}
