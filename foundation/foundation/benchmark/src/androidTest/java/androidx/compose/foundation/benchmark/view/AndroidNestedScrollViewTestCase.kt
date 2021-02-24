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

package androidx.compose.foundation.benchmark.view

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.benchmark.NestedScrollerTestCase
import kotlin.random.Random

/**
 * Version of [NestedScrollerTestCase] using Android views.
 */
class AndroidNestedScrollViewTestCase : AndroidTestCase, ToggleableTestCase {
    lateinit var firstScrollView: HorizontalScrollView

    override fun getContent(activity: Activity): ViewGroup {
        val scrollView = activity.layoutInflater
            .inflate(androidx.compose.foundation.benchmark.R.layout.simple_store, null) as ViewGroup
        visitImages(scrollView) { view ->
            val color = Color(
                red = Random.nextInt(256),
                green = Random.nextInt(256),
                blue = Random.nextInt(256)
            ).toArgb()
            view.setBackgroundColor(color)
        }
        firstScrollView = scrollView
            .findViewById(androidx.compose.foundation.benchmark.R.id.first_row)
        return scrollView
    }

    fun visitImages(viewGroup: ViewGroup, block: (View) -> Unit) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                visitImages(child, block)
            } else if (child.id == androidx.compose.foundation.benchmark.R.id.item_image) {
                block(child)
            }
        }
    }

    /**
     * This scrolls the first HorizontalScrollView. Views are well optimized for this operation,
     * so it is good to have a metric to compare against. Compose UI does more during scrolling,
     * and it is important that it doesn't explode. This View example helps compare
     * measure/layout/draw times for scrolling operations.
     */
    override fun toggleState() {
        firstScrollView.scrollX = if (firstScrollView.scrollX == 0) 5 else 0
    }
}
