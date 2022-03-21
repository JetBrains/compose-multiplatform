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

package androidx.compose.ui.text.font

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AndroidFontResolverInterceptorTest {
    private lateinit var subject: AndroidFontResolveInterceptor

    private fun createSubject(adjustment: Int) {
        subject = AndroidFontResolveInterceptor(adjustment)
    }

    @Test
    fun fontWeightDoesNotChange_whenBoldTextAccessibilityIsNotEnabled() {
        val testedWeight = 400
        val adjustment = 0
        val expected = 400
        createSubject(adjustment)
        assertThat(subject.interceptFontWeight(FontWeight(testedWeight)).weight)
            .isEqualTo(expected)
    }

    @Test
    fun fontWeightIncreases_whenBoldTextAccessibilityIsEnabled() {
        val testedWeight = 400
        val adjustment = 300
        val expected = 700
        createSubject(adjustment)
        assertThat(subject.interceptFontWeight(FontWeight(testedWeight)).weight)
            .isEqualTo(expected)
    }

    @Test
    fun fontWeightNeverExceeds1000_whenBoldTextAccessibilityIsEnabled() {
        val testedWeight = 500
        val adjustment = 600
        createSubject(adjustment)
        assertThat(subject.interceptFontWeight(FontWeight(testedWeight)).weight)
            .isEqualTo(android.graphics.fonts.FontStyle.FONT_WEIGHT_MAX)
    }

    @Test
    fun fontWeightWontBeZero_whenBoldTextAccessibilityIsEnabled() {
        val testedWeight = 500
        val adjustment = -600
        createSubject(adjustment)
        assertThat(subject.interceptFontWeight(FontWeight(testedWeight)).weight)
            .isEqualTo(android.graphics.fonts.FontStyle.FONT_WEIGHT_MIN)
    }

    @Test
    fun fontWeightWontBeNegative_whenBoldTextAccessibilityIsEnabled() {
        val testedWeight = 500
        val adjustment = -500
        createSubject(adjustment)
        assertThat(subject.interceptFontWeight(FontWeight(testedWeight)).weight)
            .isEqualTo(android.graphics.fonts.FontStyle.FONT_WEIGHT_MIN)
    }

    @Test
    fun fontWeightWontOverflow_whenBoldTextAccessibilityIsEnabled() {
        val testedWeight = 500
        val adjustment = Int.MAX_VALUE
        createSubject(adjustment)
        assertThat(subject.interceptFontWeight(FontWeight(testedWeight)).weight)
            .isEqualTo(testedWeight)
    }

    @Test
    fun otherFontArgumentsWontChange_whenBoldTextAccessibilityIsEnabled() {
        val adjustment = 300
        createSubject(adjustment)
        assertThat(subject.interceptFontFamily(FontFamily.SansSerif))
            .isEqualTo(FontFamily.SansSerif)

        assertThat(subject.interceptFontStyle(FontStyle.Normal))
            .isEqualTo(FontStyle.Normal)

        assertThat(subject.interceptFontSynthesis(FontSynthesis.Weight))
            .isEqualTo(FontSynthesis.Weight)
    }
}