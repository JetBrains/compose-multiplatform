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

@file:Suppress("unused")

package androidx.ui.androidview.adapters

import android.widget.RatingBar
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

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