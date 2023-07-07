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
@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text.style

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LineHeightStyleTest {

    @Test
    fun equals_returns_false_for_different_alignment() {
        val lineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.None
        )
        val otherLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Bottom,
            trim = Trim.None
        )
        assertThat(lineHeightStyle.equals(otherLineHeightStyle)).isFalse()
    }

    @Test
    fun equals_returns_false_for_different_trim() {
        val lineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.None
        )
        val otherLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.Both
        )
        assertThat(lineHeightStyle.equals(otherLineHeightStyle)).isFalse()
    }

    @Test
    fun equals_returns_true_for_same_attributes() {
        val lineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.FirstLineTop
        )
        val otherLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.FirstLineTop
        )
        assertThat(lineHeightStyle.equals(otherLineHeightStyle)).isTrue()
    }

    @Test
    fun hashCode_is_different_for_different_alignment() {
        val lineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.None
        )
        val otherLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Bottom,
            trim = Trim.Both
        )
        assertThat(lineHeightStyle.hashCode()).isNotEqualTo(otherLineHeightStyle.hashCode())
    }

    @Test
    fun hashCode_is_different_for_different_trim() {
        val lineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.None
        )
        val otherLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.Both
        )
        assertThat(lineHeightStyle.hashCode()).isNotEqualTo(otherLineHeightStyle.hashCode())
    }

    @Test
    fun hashCode_is_same_for_same_attributes() {
        val lineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.Both
        )
        val otherLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.Both
        )
        assertThat(lineHeightStyle.hashCode()).isEqualTo(otherLineHeightStyle.hashCode())
    }
}