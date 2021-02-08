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

package androidx.compose.ui.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextRangeTest {
    @Test
    fun substring() {
        val text = "abcdef"
        assertThat(text.substring(TextRange(0, 3))).isEqualTo(text.substring(0, 3))
    }

    @Test
    fun substring_not_start_larger_than_end() {
        val text = "abcdef"
        assertThat(text.substring(TextRange(3, 0))).isEqualTo(text.substring(0, 3))
    }

    @Test
    fun min_max() {
        val textRange = TextRange(9, 8)
        assertThat(textRange.min).isEqualTo(8)
        assertThat(textRange.max).isEqualTo(9)
    }

    @Test
    fun equality() {
        assertThat(TextRange(0, 0)).isEqualTo(TextRange(0, 0))
        assertThat(TextRange(0, 1)).isEqualTo(TextRange(0, 1))

        assertThat(TextRange(0, 1)).isNotEqualTo(TextRange(0, 0))
    }

    @Test
    fun test_range_collapsed() {
        assertThat(TextRange(0, 0).collapsed).isTrue()
        assertThat(TextRange(1, 1).collapsed).isTrue()

        assertThat(TextRange(0, 1).collapsed).isFalse()
        assertThat(TextRange(1, 2).collapsed).isFalse()
    }

    @Test
    fun test_range_reversed() {
        assertThat(TextRange(0, 0).reversed).isFalse()
        assertThat(TextRange(0, 1).reversed).isFalse()

        assertThat(TextRange(1, 0).reversed).isTrue()
        assertThat(TextRange(3, 2).reversed).isTrue()
    }

    @Test
    fun test_intersects() {
        // same values intersect
        assertThat(TextRange(0, 1).intersects(TextRange(0, 1))).isTrue()
        // first one covers second one
        assertThat(TextRange(0, 2).intersects(TextRange(1, 2))).isTrue()
        // first one covers second one
        assertThat(TextRange(0, 2).intersects(TextRange(0, 1))).isTrue()
        // second one covers first one
        assertThat(TextRange(0, 1).intersects(TextRange(0, 2))).isTrue()
        // regular intersection
        assertThat(TextRange(0, 3).intersects(TextRange(1, 4))).isTrue()
        // opposite of regular intersection
        assertThat(TextRange(1, 4).intersects(TextRange(0, 3))).isTrue()

        assertThat(TextRange(0, 1).intersects(TextRange(1, 2))).isFalse()
        assertThat(TextRange(0, 1).intersects(TextRange(2, 3))).isFalse()
        assertThat(TextRange(1, 2).intersects(TextRange(0, 1))).isFalse()
        assertThat(TextRange(2, 3).intersects(TextRange(0, 1))).isFalse()
    }

    @Test
    fun test_contains_range() {
        assertThat(TextRange(0, 2).contains(TextRange(0, 1))).isTrue()
        assertThat(TextRange(0, 2).contains(TextRange(0, 2))).isTrue()

        assertThat(TextRange(0, 2).contains(TextRange(0, 3))).isFalse()
        assertThat(TextRange(0, 2).contains(TextRange(1, 3))).isFalse()
    }

    @Test
    fun test_contains_range_operator() {
        assertThat(TextRange(0, 1) in TextRange(0, 2)).isTrue()
        assertThat(TextRange(0, 2) in TextRange(0, 2)).isTrue()

        assertThat(TextRange(0, 3) in TextRange(0, 2)).isFalse()
        assertThat(TextRange(1, 3) in TextRange(0, 2)).isFalse()
    }

    @Test
    fun test_contains_offset() {
        assertThat(TextRange(0, 2).contains(0)).isTrue()
        assertThat(TextRange(0, 2).contains(1)).isTrue()

        // end is exclusive therefore won't contain
        assertThat(TextRange(0, 2).contains(2)).isFalse()
        assertThat(TextRange(0, 2).contains(3)).isFalse()
    }

    @Test
    fun test_contains_offset_operator() {
        assertThat(0 in TextRange(0, 2)).isTrue()
        assertThat(1 in TextRange(0, 2)).isTrue()

        // end is exclusive therefore won't contain
        assertThat(2 in TextRange(0, 2)).isFalse()
        assertThat(3 in TextRange(0, 2)).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_does_not_accept_negative_value_for_start() {
        TextRange(-1, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_does_not_accept_negative_value_for_end() {
        TextRange(1, -1)
    }

    @Test
    fun constrain_updates_start_end_if_required() {
        assertThat(TextRange(0, 4).constrain(1, 3)).isEqualTo(TextRange(1, 3))
    }

    @Test
    fun constrain_with_collapsed_min_max_returns_collapsed_values() {
        assertThat(TextRange(1, 2).constrain(2, 2)).isEqualTo(TextRange(2, 2))
        assertThat(TextRange(2, 3).constrain(2, 2)).isEqualTo(TextRange(2, 2))
    }

    @Test
    fun constrain_min_max_greater_than_TextRange_values() {
        assertThat(TextRange(0, 4).constrain(5, 6)).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun constrain_min_smaller_than_TextRange_values() {
        assertThat(TextRange(5, 6).constrain(0, 4)).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun to_string() {
        assertThat(TextRange(0, 1).toString()).isEqualTo("TextRange(0, 1)")
        assertThat(TextRange(1, 1).toString()).isEqualTo("TextRange(1, 1)")
    }
}