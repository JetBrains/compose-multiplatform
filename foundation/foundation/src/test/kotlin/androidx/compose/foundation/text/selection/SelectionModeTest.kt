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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionModeTest {
    private val left = 0.0f
    private val right = 100.0f
    private val top = 0.0f
    private val bottom = 100.0f

    private val shift = 10.0f

    private val smallerThanLeft = left - shift
    private val betweenLeftAndRight = left + (right - left) / 2
    private val largerThanRight = right + shift

    private val smallerThanTop = top - shift
    private val betweenTopAndBottom = top + (bottom - top) / 2
    private val largerThanBottom = bottom + shift

    private val bounds = Rect(left = left, top = top, right = right, bottom = bottom)

    @Test
    fun isSelected_Vertical_contains_start_return_true() {
        val start = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)
        val end = Offset(x = largerThanRight, y = largerThanBottom)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isTrue()
    }

    @Test
    fun isSelected_Vertical_contains_end_return_true() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isTrue()
    }

    @Test
    fun isSelected_Vertical_contains_start_and_end_return_true() {
        val start = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)
        val end = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isTrue()
    }

    @Test
    fun isSelected_Vertical_smaller_than_top_return_false() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = largerThanRight, y = smallerThanTop)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Vertical_larger_than_bottom_return_false() {
        val start = Offset(x = smallerThanLeft, y = largerThanBottom)
        val end = Offset(x = largerThanRight, y = largerThanBottom)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Vertical_same_row_smaller_than_left_return_false() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = smallerThanLeft, y = betweenTopAndBottom)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Vertical_same_row_larger_than_right_return_false() {
        val start = Offset(x = largerThanRight, y = betweenTopAndBottom)
        val end = Offset(x = largerThanRight, y = largerThanBottom)

        val result = SelectionMode.Vertical.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Horizontal_contains_start_return_true() {
        val start = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)
        val end = Offset(x = largerThanRight, y = largerThanBottom)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isTrue()
    }

    @Test
    fun isSelected_Horizontal_contains_end_return_true() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isTrue()
    }

    @Test
    fun isSelected_Horizontal_contains_start_and_end_return_true() {
        val start = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)
        val end = Offset(x = betweenLeftAndRight, y = betweenTopAndBottom)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isTrue()
    }

    @Test
    fun isSelected_Horizontal_smaller_than_left_return_false() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = smallerThanLeft, y = largerThanBottom)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Horizontal_larger_than_right_return_false() {
        val start = Offset(x = largerThanRight, y = smallerThanTop)
        val end = Offset(x = largerThanRight, y = largerThanBottom)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Horizontal_same_column_smaller_than_top_return_false() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = betweenLeftAndRight, y = smallerThanTop)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun isSelected_Horizontal_same_column_larger_than_bottom_return_false() {
        val start = Offset(x = betweenLeftAndRight, y = largerThanBottom)
        val end = Offset(x = largerThanRight, y = largerThanBottom)

        val result = SelectionMode.Horizontal.isSelected(bounds = bounds, start = start, end = end)

        assertThat(result).isFalse()
    }

    @Test
    fun areHandlesCrossed_Vertical_same_row_not_crossed() {
        val start = Offset(x = smallerThanLeft, y = betweenTopAndBottom)
        val end = Offset(x = largerThanRight, y = betweenTopAndBottom)

        val result =
            SelectionMode.Vertical.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isFalse()
    }

    @Test
    fun areHandlesCrossed_Vertical_same_row_crossed() {
        val start = Offset(x = largerThanRight, y = betweenTopAndBottom)
        val end = Offset(x = smallerThanLeft, y = betweenTopAndBottom)

        val result =
            SelectionMode.Vertical.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isTrue()
    }

    @Test
    fun areHandlesCrossed_Vertical_different_rows_not_crossed() {
        val start = Offset(x = smallerThanLeft, y = smallerThanTop)
        val end = Offset(x = smallerThanLeft, y = largerThanBottom)

        val result =
            SelectionMode.Vertical.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isFalse()
    }

    @Test
    fun areHandlesCrossed_Vertical_different_rows_crossed() {
        val start = Offset(x = smallerThanLeft, y = largerThanBottom)
        val end = Offset(x = largerThanRight, y = smallerThanTop)

        val result =
            SelectionMode.Vertical.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isTrue()
    }

    @Test
    fun areHandlesCrossed_Horizontal_same_column_not_crossed() {
        val start = Offset(x = betweenLeftAndRight, y = smallerThanTop)
        val end = Offset(x = betweenLeftAndRight, y = largerThanBottom)

        val result =
            SelectionMode.Horizontal.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isFalse()
    }

    @Test
    fun areHandlesCrossed_Horizontal_same_column_crossed() {
        val start = Offset(x = betweenLeftAndRight, y = largerThanBottom)
        val end = Offset(x = betweenLeftAndRight, y = smallerThanTop)

        val result =
            SelectionMode.Horizontal.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isTrue()
    }

    @Test
    fun areHandlesCrossed_Horizontal_different_columns_not_crossed() {
        val start = Offset(x = smallerThanLeft, y = largerThanBottom)
        val end = Offset(x = largerThanRight, y = smallerThanTop)

        val result =
            SelectionMode.Horizontal.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isFalse()
    }

    @Test
    fun areHandlesCrossed_Horizontal_different_columns_crossed() {
        val start = Offset(x = largerThanRight, y = smallerThanTop)
        val end = Offset(x = smallerThanLeft, y = largerThanBottom)

        val result =
            SelectionMode.Horizontal.areHandlesCrossed(
                bounds = bounds,
                start = start,
                end = end
            )

        assertThat(result).isTrue()
    }
}
