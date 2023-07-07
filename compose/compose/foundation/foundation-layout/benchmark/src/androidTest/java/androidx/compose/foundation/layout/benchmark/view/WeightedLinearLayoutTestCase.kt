/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCase

class WeightedLinearLayoutTestCase(
    private val subLayouts: Int,
    private val numberOfBoxes: Int
) : AndroidTestCase, ToggleableTestCase {

    private val rows = mutableListOf<LinearLayout>()
    private var isToggled = false
    private var linearLayout: LinearLayout? = null
    override fun getContent(activity: Activity): ViewGroup {
        val mainLayout = LinearLayout(activity).also { linearLayout = it }
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        repeat(subLayouts) {
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1 / subLayouts.toFloat()
            )

            repeat(numberOfBoxes) {
                val box = View(activity)
                box.setBackgroundColor(Color.BLUE)
                box.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1 / numberOfBoxes.toFloat()
                )
                row.addView(box)
            }
            rows += row
            mainLayout.addView(row)
        }
        return mainLayout
    }

    override fun toggleState() {
        if (!isToggled) {
            linearLayout?.removeView(rows.last())
            isToggled = true
        } else {
            linearLayout?.addView(rows.last())
            isToggled = false
        }
    }
}