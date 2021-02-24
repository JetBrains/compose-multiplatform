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

package androidx.compose.foundation.layout.benchmark.view

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.testutils.benchmark.android.AndroidTestCase

/**
 * Version of [CheckboxesInRowsTestCase] using Android views.
 */
class AndroidRectsInLinearLayoutTestCase(private val numberOfRectangles: Int) : AndroidTestCase {

    private val rectangles = mutableListOf<View>()
    var isBlue = true

    override fun getContent(activity: Activity): ViewGroup {
        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL
        column.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        repeat(numberOfRectangles) {
            val rectangle = View(activity)
            rectangle.setBackgroundColor(Color.BLUE)
            rectangle.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            column.addView(rectangle)
            rectangles += rectangle
        }
        return column
    }

    fun toggleState() {
        rectangles.forEach {
            isBlue = !isBlue
            val color = if (isBlue) Color.BLUE else Color.RED
            it.setBackgroundColor(color)
        }
    }
}