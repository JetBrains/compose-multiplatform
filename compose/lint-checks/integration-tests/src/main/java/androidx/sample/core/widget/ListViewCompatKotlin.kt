/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.sample.core.widget

import android.os.Build
import android.widget.ListView

@Suppress("unused")
object ListViewCompatKotlin {
    /**
     * Scrolls the list items within the view by a specified number of pixels.
     *
     * @param listView the list to scroll
     * @param y the amount of pixels to scroll by vertically
     */
    fun scrollListBy(listView: ListView, y: Int) {
        if (Build.VERSION.SDK_INT >= 19) {
            // Call the framework version directly
            listView.scrollListBy(y)
        } else {
            // provide backport on earlier versions
            val firstPosition = listView.firstVisiblePosition
            if (firstPosition == ListView.INVALID_POSITION) {
                return
            }
            val firstView = listView.getChildAt(0) ?: return
            val newTop = firstView.top - y
            listView.setSelectionFromTop(firstPosition, newTop)
        }
    }

    /**
     * Check if the items in the list can be scrolled in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check
     * scrolling down.
     * @return true if the list can be scrolled in the specified direction,
     * false otherwise.
     * @see .scrollListBy
     */
    fun canScrollList(listView: ListView, direction: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= 19) {
            // Call the framework version directly
            listView.canScrollList(direction)
        } else {
            // provide backport on earlier versions
            val childCount = listView.childCount
            if (childCount == 0) {
                return false
            }
            val firstPosition = listView.firstVisiblePosition
            if (direction > 0) {
                val lastBottom = listView.getChildAt(childCount - 1).bottom
                val lastPosition = firstPosition + childCount
                lastPosition < listView.count ||
                    lastBottom > listView.height - listView.listPaddingBottom
            } else {
                val firstTop = listView.getChildAt(0).top
                firstPosition > 0 || firstTop < listView.listPaddingTop
            }
        }
    }
}
