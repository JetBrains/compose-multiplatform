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

package androidx.compose.ui.text.android.style

import android.graphics.Typeface
import android.graphics.fonts.FontStyle
import android.text.TextPaint
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalPlatformTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class FontWeightStyleSpanTest {
    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_withFontWeightBold() {
        val textPaint = TextPaint()
        val weight = FontStyle.FONT_WEIGHT_BOLD
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.weight).isEqualTo(weight)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_withFontWeightLight() {
        val textPaint = TextPaint()
        val weight = FontStyle.FONT_WEIGHT_LIGHT
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.weight).isEqualTo(weight)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_withFontWeightBoldToNormal() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.create(null, FontStyle.FONT_WEIGHT_BOLD, false)
        }
        val weight = FontStyle.FONT_WEIGHT_NORMAL
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface.isBold).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_withFontWeightNull() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.create(null, FontStyle.FONT_WEIGHT_BOLD, true)
        }
        val span = FontWeightStyleSpan(0, FontWeightStyleSpan.STYLE_NORMAL)
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface.weight).isEqualTo(FontStyle.FONT_WEIGHT_BOLD)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_withItalic() {
        val textPaint = TextPaint()
        val span = FontWeightStyleSpan(0, FontWeightStyleSpan.STYLE_ITALIC)
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.isItalic).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_withItalicNull() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.create(null, FontStyle.FONT_WEIGHT_BOLD, true)
        }
        val span = FontWeightStyleSpan(
            weight = FontStyle.FONT_WEIGHT_NORMAL,
            style = FontWeightStyleSpan.STYLE_NONE
        )
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface.isItalic).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_noChange() {
        val typeface = Typeface.create(null, FontStyle.FONT_WEIGHT_BOLD, true)
        val textPaint = TextPaint().apply { this.typeface = typeface }
        val span = FontWeightStyleSpan(FontStyle.FONT_WEIGHT_BOLD, FontWeightStyleSpan.STYLE_ITALIC)
        span.updatePaint(textPaint)

        assertThat(textPaint.typeface).isSameInstanceAs(typeface)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_withFontWeightBold() {
        val textPaint = TextPaint()
        val weight = FontStyle.FONT_WEIGHT_BOLD
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.isBold).isTrue()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_withFontWeightLight() {
        val textPaint = TextPaint()
        val weight = FontStyle.FONT_WEIGHT_LIGHT
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.isBold).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_withFontWeightBoldToNormal() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }
        val weight = FontStyle.FONT_WEIGHT_NORMAL
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface.isBold).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_withFontWeightNull() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
        }
        val span = FontWeightStyleSpan(0, FontWeightStyleSpan.STYLE_NORMAL)
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface.isBold).isTrue()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_withItalic() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }
        val weight = FontStyle.FONT_WEIGHT_NORMAL
        val span = FontWeightStyleSpan(weight, FontWeightStyleSpan.STYLE_NONE)
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.isBold).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_withItalicNull() {
        val textPaint = TextPaint().apply {
            typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
        }
        val span = FontWeightStyleSpan(0, FontWeightStyleSpan.STYLE_NORMAL)
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface).isNotNull()
        assertThat(textPaint.typeface.isItalic).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun legacyUpdatePaint_noChange() {
        val typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
        val textPaint = TextPaint().apply { this.typeface = typeface }
        val span = FontWeightStyleSpan(
            weight = FontStyle.FONT_WEIGHT_BOLD,
            style = FontWeightStyleSpan.STYLE_ITALIC
        )
        span.legacyUpdatePaint(textPaint)

        assertThat(textPaint.typeface).isSameInstanceAs(typeface)
    }
}