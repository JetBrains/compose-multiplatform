@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.RatingBar
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("RatingBarInputController")

private val RatingBar.controller: RatingBarInputController
    get() {
        var controller = getTag(key) as? RatingBarInputController
        if (controller == null) {
            controller = RatingBarInputController(this)
            setTag(key, controller)
            onRatingBarChangeListener = controller
        }
        return controller
    }

@RequiresOneOf("controlledRating")
@ConflictsWith("onRatingBarChangeListener")
fun RatingBar.setOnRatingChange(onRatingChange: Function1<Float, Unit>) {
    controller.onRatingChange = onRatingChange
}

@RequiresOneOf("onRatingChange")
fun RatingBar.setControlledRating(rating: Float) {
    controller.setValueIfNeeded(rating)
}