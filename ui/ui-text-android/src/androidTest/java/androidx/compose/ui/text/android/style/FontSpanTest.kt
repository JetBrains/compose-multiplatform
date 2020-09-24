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
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalPlatformTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class FontSpanTest {
    @Test
    fun updatePaint() {
        val textPaint = TextPaint()
        val typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
        val span = FontSpan { _, _ -> typeface }

        assertThat(textPaint.typeface).isNotSameInstanceAs(typeface)
        span.updatePaint(textPaint)
        assertThat(textPaint.typeface).isSameInstanceAs(typeface)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun updatePaint_bold_belowAPI27() {
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        val getFont = mock<(Int, Boolean) -> Typeface>()
        val span = FontSpan(getFont)

        span.updatePaint(textPaint)
        verify(getFont, times(1)).invoke(FontStyle.FONT_WEIGHT_BOLD, false)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun updatePaint_italic_belowAPI27() {
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
        val getFont = mock<(Int, Boolean) -> Typeface>()
        val span = FontSpan(getFont)

        span.updatePaint(textPaint)
        verify(getFont, times(1)).invoke(FontStyle.FONT_WEIGHT_NORMAL, true)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_extraLight_aboveAPI28() {
        val textPaint = TextPaint()
        val fontWeight = FontStyle.FONT_WEIGHT_EXTRA_LIGHT
        textPaint.typeface = Typeface.create(null, fontWeight, false)
        val getFont = mock<(Int, Boolean) -> Typeface>()
        val span = FontSpan(getFont)

        span.updatePaint(textPaint)
        verify(getFont, times(1)).invoke(fontWeight, false)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun updatePaint_italic_aboveAPI28() {
        val textPaint = TextPaint()
        val italic = true
        textPaint.typeface = Typeface.create(null, FontStyle.FONT_WEIGHT_NORMAL, italic)
        val getFont = mock<(Int, Boolean) -> Typeface>()
        val span = FontSpan(getFont)

        span.updatePaint(textPaint)
        verify(getFont, times(1)).invoke(FontStyle.FONT_WEIGHT_NORMAL, italic)
    }
}