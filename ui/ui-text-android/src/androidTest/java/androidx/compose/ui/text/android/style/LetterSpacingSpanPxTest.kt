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

package androidx.compose.ui.text.android.style

import android.text.TextPaint
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalPlatformTextApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class LetterSpacingSpanPxTest {
    @Test
    fun updateDrawState() {
        val letterSpacing = 10f
        val textSize = 10f
        val textScaleX = 2f

        val letterSpacingSpanPx = LetterSpacingSpanPx(letterSpacing)

        val textPaint = mock<TextPaint> {
            on { this.textSize } doReturn textSize
            on { this.textScaleX } doReturn textScaleX
        }

        letterSpacingSpanPx.updateDrawState(textPaint)

        verify(textPaint).letterSpacing = 0.5f
    }

    @Test
    fun updateDrawState_with_invalid_textSize() {
        val letterSpacing = 10f
        val textSize = 0f
        val textScaleX = 2f

        val letterSpacingSpanPx = LetterSpacingSpanPx(letterSpacing)

        val textPaint = mock<TextPaint> {
            on { this.textSize } doReturn textSize
            on { this.textScaleX } doReturn textScaleX
        }

        letterSpacingSpanPx.updateDrawState(textPaint)

        verify(textPaint, never()).letterSpacing = any()
    }

    @Test
    fun updateMeasureState() {
        val letterSpacing = 10f
        val textSize = 10f
        val textScaleX = 2f

        val letterSpacingSpanPx = LetterSpacingSpanPx(letterSpacing)

        val textPaint = mock<TextPaint> {
            on { this.textSize } doReturn textSize
            on { this.textScaleX } doReturn textScaleX
        }

        letterSpacingSpanPx.updateMeasureState(textPaint)

        verify(textPaint).letterSpacing = 0.5f
    }

    @Test
    fun updateMeasureState_with_invalid_textSize() {
        val letterSpacing = 10f
        val textSize = 0f
        val textScaleX = 2f

        val letterSpacingSpanPx = LetterSpacingSpanPx(letterSpacing)

        val textPaint = mock<TextPaint> {
            on { this.textSize } doReturn textSize
            on { this.textScaleX } doReturn textScaleX
        }

        letterSpacingSpanPx.updateMeasureState(textPaint)

        verify(textPaint, never()).letterSpacing = any()
    }
}