/*
 * Copyright 2020 The Android Open Source Project
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
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import kotlin.math.roundToInt

class AndroidTextViewTestCase(
    private val texts: List<String>
) : AndroidTestCase {

    private var fontSize = 8f

    override fun getContent(activity: Activity): ViewGroup {
        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL
        column.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        for (text in texts) {
            val textView = TextView(activity)
            textView.text = text
            textView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            textView.width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160f,
                activity.resources.displayMetrics
            ).roundToInt()

            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            column.addView(textView)
        }
        return column
    }
}