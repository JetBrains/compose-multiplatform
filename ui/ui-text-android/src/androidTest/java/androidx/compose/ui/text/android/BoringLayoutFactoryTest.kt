/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text.android

import android.text.BoringLayout
import android.text.BoringLayout.Metrics
import android.text.Layout.Alignment
import android.text.SpannableString
import android.text.Spanned
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import androidx.compose.ui.text.android.BoringLayoutFactory.create
import androidx.compose.ui.text.android.BoringLayoutFactory.measure
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class BoringLayoutFactoryTest {
    @Test
    fun measure_plainText_returnBoringMetrics() {
        assertThat(measure("abc", TextPaint(), TextDirectionHeuristics.FIRSTSTRONG_LTR))
            .isNotNull()
    }

    @Test
    fun measure_BiDiText_returnNull() {
        assertThat(measure("abc\u05D0", TextPaint(), TextDirectionHeuristics.FIRSTSTRONG_LTR))
            .isNull()
    }

    @Test
    fun measure_textWithCharacterStyle_returnBoringMetrics() {
        val text = SpannableString("HelloWorld")
        text.setSpan(
            ForegroundColorSpan(0xFF00FF00.toInt()),
            0,
            5,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        assertThat(measure(text, TextPaint(), TextDirectionHeuristics.FIRSTSTRONG_LTR))
            .isNotNull()
    }

    @Test
    fun measure_textWithParagraphStyle_returnNull() {
        val text = SpannableString("HelloWorld")
        text.setSpan(
            LeadingMarginSpan.Standard(20),
            0,
            5,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        assertThat(measure(text, TextPaint(), TextDirectionHeuristics.FIRSTSTRONG_LTR)).isNull()
    }

    @Test
    fun create_returnsGivenValues() {
        val text = "abc"
        val paint = TextPaint()
        val width = 100
        val metrics = BoringLayout.isBoring(text, paint)
        val boringLayout = create(text, paint, width, metrics)

        assertThat(boringLayout.text).isEqualTo(text)
        assertThat(boringLayout.paint).isEqualTo(paint)
        // The width and height of the boringLayout is the same in metrics, indicating metrics is 
        // passed correctly.
        assertThat(boringLayout.getLineWidth(0).toInt()).isEqualTo(metrics.width)
        assertThat(boringLayout.getLineBottom(0) - boringLayout.getLineTop(0))
            .isEqualTo(metrics.bottom - metrics.top)
        assertThat(boringLayout.width).isEqualTo(width)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_width_negative_throwsIAE() {
        create("", TextPaint(), -1, Metrics())
    }

    @Test
    fun create_defaultAlignment_isAlignNormal() {
        val boringLayout = create("", TextPaint(), 0, Metrics())

        assertThat(boringLayout.alignment).isEqualTo(Alignment.ALIGN_NORMAL)
    }

    @Test
    fun create_includePad_true_useTopAndBottomAsAscendAndDescend() {
        val text: CharSequence = "abcdefghijk"
        val paint = TextPaint()
        val metrics = BoringLayout.isBoring(text, paint)
        val boringLayout = create(
            text = text,
            paint = paint,
            width = metrics.width,
            metrics = metrics,
            includePadding = true
        )
        assertThat(boringLayout.getLineAscent(0)).isEqualTo(metrics.top)
        assertThat(boringLayout.getLineDescent(0)).isEqualTo(metrics.bottom)
    }

    @Test
    fun create_includePad_false_useTopAndBottomAsAscendAndDescend() {
        val text: CharSequence = "abcdefghijk"
        val paint = TextPaint()
        val metrics = BoringLayout.isBoring(text, paint)
        val boringLayout = create(
            text = text,
            paint = paint,
            width = metrics.width,
            metrics = metrics,
            includePadding = false
        )

        assertThat(boringLayout.getLineAscent(0)).isEqualTo(metrics.ascent)
        assertThat(boringLayout.getLineDescent(0)).isEqualTo(metrics.descent)
    }

    @Test
    fun create_defaultIncludePad_isTrue() {
        val text: CharSequence = "abcdefghijk"
        val paint = TextPaint()
        val metrics = BoringLayout.isBoring(text, paint)
        val boringLayout = create(
            text = text,
            paint = paint,
            width = metrics.width,
            metrics = metrics
        )

        val topPad = boringLayout.topPadding
        val bottomPad = boringLayout.bottomPadding
        // Top and bottom padding are not 0 at the same time, indicating includePad is true.
        assertThat(topPad * topPad + bottomPad * bottomPad).isGreaterThan(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_setEllipsizedWidth_withNegative_throwsIAE() {
        create(
            text = "",
            paint = TextPaint(),
            width = 0,
            metrics = Metrics(),
            ellipsizedWidth = -1
        )
    }

    @Test
    fun create_setEllipsize_withShortText_isNotEllipsized() {
        val text: CharSequence = "abcdefghijk"
        val paint = TextPaint()
        val metrics = BoringLayout.isBoring(text, paint)
        val width = metrics.width
        val boringLayout = create(
            text = text,
            paint = paint,
            width = width,
            metrics = metrics,
            ellipsize = TextUtils.TruncateAt.END,
            ellipsizedWidth = width
        )

        assertThat(boringLayout.getEllipsisCount(0)).isEqualTo(0)
    }

    @Test
    fun create_setEllipsize_withLongText_isEllipsized() {
        val text: CharSequence = "abcdefghijk"
        val paint = TextPaint()
        val metrics = BoringLayout.isBoring(text, paint)
        val width = metrics.width

        val boringLayout = create(
            text = text,
            paint = paint,
            width = width,
            metrics = metrics,
            ellipsize = TextUtils.TruncateAt.END,
            ellipsizedWidth = width / 2
        )

        assertThat(boringLayout.getEllipsisCount(0)).isGreaterThan(0)
    }

    @Test
    fun create_defaultEllipsize_isNull() {
        val text: CharSequence = "abcdefghijk"
        val paint = TextPaint()
        val metrics = BoringLayout.isBoring(text, paint)
        // Don't give enough space, but boringLayout won't cut the text either.
        val width = metrics.width / 2
        val boringLayout = create(text, paint, width, metrics)
        // EllipsisCount should be 0 indicating ellipsize is null.
        assertThat(boringLayout.getEllipsisCount(0)).isEqualTo(0)
    }
}