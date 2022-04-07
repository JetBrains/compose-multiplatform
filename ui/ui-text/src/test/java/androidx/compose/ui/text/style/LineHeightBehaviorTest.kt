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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LineHeightBehaviorTest {

    @Test
    fun equals_returns_false_for_different_distribution() {
        val lineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Center
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Bottom
        )
        assertThat(lineHeightBehavior.equals(otherLineHeightBehavior)).isFalse()
    }

    @Test
    fun equals_returns_false_for_different_applyToFirstTop() {
        val lineHeightBehavior = LineHeightBehavior(
            trimFirstLineTop = false
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            trimFirstLineTop = true
        )
        assertThat(lineHeightBehavior.equals(otherLineHeightBehavior)).isFalse()
    }

    @Test
    fun equals_returns_false_for_different_applyToLastBottom() {
        val lineHeightBehavior = LineHeightBehavior(
            trimLastLineBottom = false
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            trimLastLineBottom = true
        )
        assertThat(lineHeightBehavior.equals(otherLineHeightBehavior)).isFalse()
    }

    @Test
    fun equals_returns_true_for_same_attributes() {
        val lineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Center,
            trimLastLineBottom = false,
            trimFirstLineTop = false
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Center,
            trimLastLineBottom = false,
            trimFirstLineTop = false
        )
        assertThat(lineHeightBehavior.equals(otherLineHeightBehavior)).isTrue()
    }

    @Test
    fun hashCode_is_different_for_different_distribution() {
        val lineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Center
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Bottom
        )
        assertThat(lineHeightBehavior.hashCode()).isNotEqualTo(otherLineHeightBehavior.hashCode())
    }

    @Test
    fun hashCode_is_different_for_different_applyToFirstTop() {
        val lineHeightBehavior = LineHeightBehavior(
            trimFirstLineTop = false
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            trimFirstLineTop = true
        )
        assertThat(lineHeightBehavior.hashCode()).isNotEqualTo(otherLineHeightBehavior.hashCode())
    }

    @Test
    fun hashCode_is_different_for_different_applyToLastBottom() {
        val lineHeightBehavior = LineHeightBehavior(
            trimLastLineBottom = false
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            trimLastLineBottom = true
        )
        assertThat(lineHeightBehavior.hashCode()).isNotEqualTo(otherLineHeightBehavior.hashCode())
    }

    @Test
    fun hashCode_is_same_for_same_attributes() {
        val lineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Center,
            trimLastLineBottom = false,
            trimFirstLineTop = false
        )
        val otherLineHeightBehavior = LineHeightBehavior(
            alignment = LineVerticalAlignment.Center,
            trimLastLineBottom = false,
            trimFirstLineTop = false
        )
        assertThat(lineHeightBehavior.hashCode()).isEqualTo(otherLineHeightBehavior.hashCode())
    }
}