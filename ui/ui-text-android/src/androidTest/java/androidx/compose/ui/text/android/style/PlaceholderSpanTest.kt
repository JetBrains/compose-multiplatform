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

import android.graphics.Paint
import android.text.TextPaint
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class PlaceholderSpanTest {
    @Test
    fun width_isSp_equalsGiven() {
        val width = 1f
        val pxPerSp = 10f
        val placeholderSpan = PlaceholderSpan(
            width = width,
            widthUnit = PlaceholderSpan.UNIT_SP,
            height = 0f,
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        assertThat(placeholderSpan.getSize(Paint(), "ab", 1, 2, null))
            .isEqualTo((width * pxPerSp).toInt())
        assertThat(placeholderSpan.widthPx).isEqualTo((width * pxPerSp).toInt())
    }

    @Test
    fun width_isEm_equalsGiven() {
        val width = 1f
        val fontSize = 24f
        val paint = Paint().apply { textSize = fontSize }
        val placeholderSpan = PlaceholderSpan(
            width = width,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = 0f,
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        assertThat(placeholderSpan.getSize(paint, "ab", 1, 2, null))
            .isEqualTo((width * fontSize).toInt())
        assertThat(placeholderSpan.widthPx).isEqualTo((width * fontSize).toInt())
    }

    @Test(expected = IllegalArgumentException::class)
    fun width_isInherit() {
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_INHERIT,
            height = 0f,
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        placeholderSpan.getSize(TextPaint(), "ab", 0, 2, null)
    }

    @Test
    fun height_isSp_alignAboveBaseLine_smallerThanAscent() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to ascent / 2 converted into Sp.
        val height = abs(fontMetricsInt.ascent) / 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        // Since the abs(ascent) is larger placeHolder's height, there already is enough space
        // for placeHolder. Thus resultFontMetricsInt is same as the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isEm_alignAboveBaseLine_smallerThanAscent() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to ascent / 2 converted into EM.
        val height = abs(fontMetricsInt.ascent) / 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        // Since the abs(ascent) is larger placeHolder's height, there already is enough space
        // for placeHolder. Thus resultFontMetricsInt is same as the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isSp_alignAboveBaseLine_greaterThanAscent() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to 2 * ascent converted into SP.
        val height = abs(fontMetricsInt.ascent) * 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        // Bottom and descent should not be changed.
        assertThat(resultFontMetricsInt.descent).isEqualTo(fontMetricsInt.descent)
        assertThat(resultFontMetricsInt.bottom).isEqualTo(fontMetricsInt.bottom)
        // Ascent expands to be just enough for the placeHolder placed on baseline.
        assertThat(resultFontMetricsInt.ascent).isEqualTo(-placeholderSpan.heightPx)
    }

    @Test
    fun height_isEm_alignAboveBaseLine_greaterThanAscent() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to 2 * ascent converted into EM.
        val height = abs(fontMetricsInt.ascent) * 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        // Bottom and descent should not be changed.
        assertThat(resultFontMetricsInt.descent).isEqualTo(fontMetricsInt.descent)
        assertThat(resultFontMetricsInt.bottom).isEqualTo(fontMetricsInt.bottom)
        // Ascent expands to be just enough for the placeHolder placed on baseline.
        assertThat(resultFontMetricsInt.ascent).isEqualTo(-placeholderSpan.heightPx)
    }

    @Test
    fun height_isSp_alignBottom_smallerThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to lineHeight / 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) / 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_BOTTOM
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        // Since original line height is enough for the placeHolder resultFontMetricsInt is same as
        // the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isSp_alignTop_smallerThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to lineHeight / 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) / 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_TOP
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        // Since original line height is enough for the placeHolder resultFontMetricsInt is same as
        // the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isEm_alignTop_smallerThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to lineHeight / 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) / 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_TOP
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        // Since original line height is enough for the placeHolder resultFontMetricsInt is same as
        // the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isSp_alignTop_greaterThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to lineHeight * 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) * 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_TOP
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        assertThat(resultFontMetricsInt.ascent).isEqualTo(fontMetricsInt.ascent)
        assertThat(resultFontMetricsInt.top).isEqualTo(fontMetricsInt.top)
        // Descent expands to be just enough for the placeHolder placed on top.
        assertThat(resultFontMetricsInt.descent)
            .isEqualTo(fontMetricsInt.ascent + placeholderSpan.heightPx)
    }

    @Test
    fun height_isEm_alignTop_greaterThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to lineHeight * 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) * 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_TOP
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        assertThat(resultFontMetricsInt.ascent).isEqualTo(fontMetricsInt.ascent)
        assertThat(resultFontMetricsInt.top).isEqualTo(fontMetricsInt.top)
        // Descent expands to be just enough for the placeHolder placed on top.
        assertThat(resultFontMetricsInt.descent)
            .isEqualTo(fontMetricsInt.ascent + placeholderSpan.heightPx)
    }

    @Test
    fun height_isEm_alignBottom_smallerThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to lineHeight / 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) / 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_BOTTOM
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        // Since original line height is enough for the placeHolder resultFontMetricsInt is same as
        // the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isSp_alignBottom_greaterThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to lineHeight * 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) * 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_BOTTOM
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        assertThat(resultFontMetricsInt.descent).isEqualTo(fontMetricsInt.descent)
        assertThat(resultFontMetricsInt.bottom).isEqualTo(fontMetricsInt.bottom)
        // Ascent expands to be just enough for the placeHolder placed on bottom.
        assertThat(resultFontMetricsInt.ascent)
            .isEqualTo(fontMetricsInt.descent - placeholderSpan.heightPx)
    }

    @Test
    fun height_isEm_alignBottom_greaterThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to lineHeight * 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) * 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_BOTTOM
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        assertThat(resultFontMetricsInt.descent).isEqualTo(fontMetricsInt.descent)
        assertThat(resultFontMetricsInt.bottom).isEqualTo(fontMetricsInt.bottom)
        // Ascent expands to be just enough for the placeHolder placed on bottom.
        assertThat(resultFontMetricsInt.ascent)
            .isEqualTo(fontMetricsInt.descent - placeholderSpan.heightPx)
    }

    @Test
    fun height_isSp_alignCenter_smallerThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to lineHeight / 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) / 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_BOTTOM
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)
        // Since original line height is enough for the placeHolder resultFontMetricsInt is same as
        // the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isEm_alignCenter_smallerThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to lineHeight / 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) / 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_BOTTOM
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)
        // Since original line height is enough for the placeHolder resultFontMetricsInt is same as
        // the one on Paint.
        // Notice: FontMetricsInt doesn't override equals(), using toString() to compare.
        assertThat(resultFontMetricsInt.toString()).isEqualTo(fontMetricsInt.toString())
    }

    @Test
    fun height_isSp_alignCenter_greaterThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        val pxPerSp = 2
        // Height equals to lineHeight * 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) * 2 / pxPerSp
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_SP,
            pxPerSp = pxPerSp.toFloat(),
            verticalAlign = PlaceholderSpan.ALIGN_CENTER
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        val increasedHeight = height * pxPerSp - (fontMetricsInt.descent - fontMetricsInt.ascent)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * pxPerSp)

        assertThat(resultFontMetricsInt.ascent)
            .isEqualTo(fontMetricsInt.ascent - increasedHeight / 2)
        assertThat(resultFontMetricsInt.descent)
            .isEqualTo(fontMetricsInt.descent + increasedHeight / 2)
    }

    @Test
    fun height_isEm_alignCenter_greaterThanOriginalHeight() {
        val fontSize = 24
        val paint = Paint().apply { textSize = fontSize.toFloat() }
        val fontMetricsInt = paint.fontMetricsInt
        // Height equals to lineHeight * 2 converted into EM.
        val height = abs(fontMetricsInt.descent - fontMetricsInt.ascent) * 2 / fontSize
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = height.toFloat(),
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_CENTER
        )
        val resultFontMetricsInt = Paint.FontMetricsInt()
        placeholderSpan.getSize(paint, "ab", 1, 2, resultFontMetricsInt)

        val increasedHeight = height * fontSize - (fontMetricsInt.descent - fontMetricsInt.ascent)

        assertThat(placeholderSpan.heightPx).isEqualTo(height * fontSize)

        assertThat(resultFontMetricsInt.ascent)
            .isEqualTo(fontMetricsInt.ascent - increasedHeight / 2)
        assertThat(resultFontMetricsInt.descent)
            .isEqualTo(fontMetricsInt.descent + increasedHeight / 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun height_isInherit() {
        val placeholderSpan = PlaceholderSpan(
            width = 0f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = 0f,
            heightUnit = PlaceholderSpan.UNIT_INHERIT,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        placeholderSpan.getSize(TextPaint(), "ab", 0, 2, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun align_isIllegal() {
        val placeholderSpan = PlaceholderSpan(
            width = 1f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = 1f,
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = 7
        )
        placeholderSpan.getSize(TextPaint(), "ab", 0, 2, Paint.FontMetricsInt())
    }

    @Test(expected = IllegalStateException::class)
    fun widthPx_accessBeforeGetSize() {
        val placeholderSpan = PlaceholderSpan(
            width = 1f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = 1f,
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        placeholderSpan.widthPx
    }

    @Test(expected = IllegalStateException::class)
    fun heightPx_accessBeforeGetSize() {
        val placeholderSpan = PlaceholderSpan(
            width = 1f,
            widthUnit = PlaceholderSpan.UNIT_EM,
            height = 1f,
            heightUnit = PlaceholderSpan.UNIT_EM,
            pxPerSp = 1f,
            verticalAlign = PlaceholderSpan.ALIGN_ABOVE_BASELINE
        )
        placeholderSpan.heightPx
    }
}