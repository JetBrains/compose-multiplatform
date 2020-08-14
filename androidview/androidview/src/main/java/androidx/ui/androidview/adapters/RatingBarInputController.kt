/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.androidview.adapters

import android.widget.RatingBar

class RatingBarInputController(
    view: RatingBar
) : RatingBar.OnRatingBarChangeListener, InputController<RatingBar, Float>(view) {

    override fun getValue(): Float = view.rating

    override fun setValue(value: Float) {
        view.rating = value
    }

    var onRatingChange: Function1<Float, Unit>? = null

    override fun onRatingChanged(ratingBar: RatingBar?, rating: Float, fromUser: Boolean) {
        prepareForChange(rating)
        onRatingChange?.invoke(rating)
    }
}
