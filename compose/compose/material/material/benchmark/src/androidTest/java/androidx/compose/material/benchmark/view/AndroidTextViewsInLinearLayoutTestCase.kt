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
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.testutils.benchmark.android.AndroidTestCase

class AndroidTextViewsInLinearLayoutTestCase(
    private val amountOfCheckboxes: Int
) : AndroidTestCase {

    private val textViews = mutableListOf<TextView>()
    private var fontSize = 20f

    override fun getContent(activity: Activity): ViewGroup {
        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL
        column.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        repeat(amountOfCheckboxes) {
            val text = TextView(activity)
            text.text = "Hello World Hello World Hello W"
            text.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text.setTextSize(fontSize)
            column.addView(text)
            textViews += text
        }
        return column
    }

    fun toggleState() {
        fontSize = if (fontSize == 20f) 15f else 20f
        textViews.forEach {
            it.textSize = fontSize
        }
    }
}