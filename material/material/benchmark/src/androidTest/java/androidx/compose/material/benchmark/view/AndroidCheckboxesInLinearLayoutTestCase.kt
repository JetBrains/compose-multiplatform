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

package androidx.compose.material.benchmark.view

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.testutils.benchmark.android.AndroidTestCase

/**
 * Version of [CheckboxesInRowsTestCase] using Android views.
 */
class AndroidCheckboxesInLinearLayoutTestCase(
    private val amountOfCheckboxes: Int
) : AndroidTestCase {

    private val checkboxes = mutableListOf<CheckBox>()

    override fun getContent(activity: Activity): ViewGroup {
        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL
        column.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        repeat(amountOfCheckboxes) {
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val text = TextView(activity)
            text.text = "Check Me!"
            text.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val checkbox = CheckBox(activity)
            checkbox.isChecked = false
            checkbox.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val space = View(activity)
            val layoutParams = LinearLayout.LayoutParams(0, 1)
            layoutParams.weight = 1f
            space.layoutParams = layoutParams
            row.addView(text)
            row.addView(space)
            row.addView(checkbox)
            column.addView(row)
        }
        return column
    }

    fun toggleState() {
        val checkbox = checkboxes.first()
        checkbox.isChecked = !checkbox.isChecked
    }
}