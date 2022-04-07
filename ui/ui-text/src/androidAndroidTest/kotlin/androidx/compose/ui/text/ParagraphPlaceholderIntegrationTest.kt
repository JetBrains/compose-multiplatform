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

package androidx.compose.ui.text

import androidx.compose.ui.text.android.style.ceilToInt
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParagraphPlaceholderIntegrationTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val defaultDensity = Density(density = 1f, fontScale = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun placeHolder_alignAboveBaseLine_lessThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)
        // Height won't be increased. Notice: in fontFamilyMeasureFont lineHeight = 1.2 * fontSize
        assertThat(paragraph.getLineHeight(0)).isEqualTo(fontSize)

        val bound = placeholderRects[0]!!
        assertThat(bound.bottom).isEqualTo(paragraph.firstBaseline)
        assertThat(bound.top).isEqualTo(paragraph.firstBaseline - height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignAboveBaseLine_greaterThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 2.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        // In the measure font, descent = 0.2 * fontSize, ascent = 1 * fontSize.
        // In this case, ascent is pushed by placeholder to 2 * fontSize, so that
        // lineHeight  = 0.2 * fontSize + 2 * fontSize.
        assertThat(paragraph.getLineHeight(0))
            .isEqualTo(0.2f * fontSize + fontSize * height.value)

        val bound = placeholderRects[0]!!
        assertThat(bound.bottom).isEqualTo(paragraph.firstBaseline)
        assertThat(bound.top).isEqualTo(paragraph.firstBaseline - height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignBottom_lessThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.Bottom)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        assertThat(paragraph.getLineHeight(0)).isEqualTo(fontSize)

        val bound = placeholderRects[0]!!
        assertThat(bound.bottom).isEqualTo(paragraph.getLineBottom(0))
        assertThat(bound.top)
            .isEqualTo(paragraph.getLineBottom(0) - height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignBottom_greaterThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 2.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.Bottom)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        assertThat(paragraph.getLineHeight(0)).isEqualTo(height.value * fontSize)

        val bound = placeholderRects[0]!!
        assertThat(bound.bottom).isEqualTo(paragraph.getLineBottom(0))
        assertThat(bound.top)
            .isEqualTo(paragraph.getLineBottom(0) - height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignTop_lessThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.Top)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        assertThat(paragraph.getLineHeight(0)).isEqualTo(fontSize)

        val bound = placeholderRects[0]!!
        // TODO(haoyuchang): use getLineTop instead
        assertThat(bound.top).isEqualTo(paragraph.getLineBottom(-1))
        assertThat(bound.bottom)
            .isEqualTo(paragraph.getLineBottom(-1) + height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignTop_greaterThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 2.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.Top)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        assertThat(paragraph.getLineHeight(0)).isEqualTo(height.value * fontSize)

        val bound = placeholderRects[0]!!
        // TODO(haoyuchang): use getLineTop instead
        assertThat(bound.top).isEqualTo(paragraph.getLineBottom(-1))
        assertThat(bound.bottom)
            .isEqualTo(paragraph.getLineBottom(-1) + height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignCenter_lessThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.Center)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        assertThat(paragraph.getLineHeight(0)).isEqualTo(fontSize)

        val bound = placeholderRects[0]!!
        // TODO(haoyuchang): We need getLineTop(0).
        val lineCenter = (paragraph.getLineBottom(-1) + paragraph.getLineBottom(0)) / 2

        assertThat(bound.top).isEqualTo(lineCenter - height.value * fontSize / 2)
        assertThat(bound.bottom).isEqualTo(lineCenter + height.value * fontSize / 2)

        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignCenter_greaterThanOriginalHeight() {
        val text = "AAA"
        val fontSize = 20
        val height = 2.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.Center)

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize.sp,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        assertThat(paragraph.getLineHeight(0)).isEqualTo(fontSize * height.value)

        val bound = placeholderRects[0]!!
        // TODO(haoyuchang): We need getLineTop(0).
        val lineCenter = (paragraph.getLineBottom(-1) + paragraph.getLineBottom(0)) / 2

        assertThat(bound.top).isEqualTo(lineCenter - height.value * fontSize / 2)
        assertThat(bound.bottom).isEqualTo(lineCenter + height.value * fontSize / 2)

        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignTextTop() {
        val text = "AAB"
        val fontSize = 20
        val fontSizeSpan = 30

        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.TextTop)
        val paragraph = simpleParagraph(
            text = text,
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(fontSize = fontSizeSpan.sp), 2, 3)
            ),
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            fontSize = fontSize.sp,
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        val bound = placeholderRects[0]!!
        // TextTop aligns the inline element to top of the proceeding text.
        // In the measurement font, the top equals to fontSize pixels above baseline.
        val expectedTop = paragraph.firstBaseline - fontSize * 0.8f
        assertThat(bound.top).isEqualTo(expectedTop)
        assertThat(bound.bottom).isEqualTo(expectedTop + height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignTextBottom() {
        val text = "AAB"
        val fontSize = 20
        val fontSizeSpan = 30

        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.TextBottom)
        val paragraph = simpleParagraph(
            text = text,
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(fontSize = fontSizeSpan.sp), 2, 3)
            ),
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            fontSize = fontSize.sp,
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        val bound = placeholderRects[0]!!
        // TextBottom aligns the inline element to bottom of the proceeding text.
        // In the measurement font, the bottom equals to fontSize * 0.2 pixels below baseline.
        val expectedBottom = paragraph.firstBaseline + fontSize * 0.2f
        assertThat(bound.bottom).isEqualTo(expectedBottom)
        assertThat(bound.top).isEqualTo(expectedBottom - height.value * fontSize)
        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_alignTextCenter() {
        val text = "AAB"
        val fontSize = 20
        val fontSizeSpan = 30

        val height = 0.5.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.TextCenter)
        val paragraph = simpleParagraph(
            text = text,
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(fontSize = fontSizeSpan.sp), 2, 3)
            ),
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            fontSize = fontSize.sp,
            width = Float.MAX_VALUE
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)

        val bound = placeholderRects[0]!!
        // TextCenter aligns the inline element to center of the proceeding text.
        // In the measurement font, the center equals to fontSize * 0.3 pixels above baseline.
        val expectedCenter = paragraph.firstBaseline - fontSize * 0.3f
        assertThat(bound.top).isEqualTo(expectedCenter - height.value * fontSize / 2)
        assertThat(bound.bottom).isEqualTo(expectedCenter + height.value * fontSize / 2)

        // There is one character to the left of this placeholder.
        assertThat(bound.left).isEqualTo(fontSize.toFloat())
        assertThat(bound.right).isEqualTo(fontSize + fontSize * width.value)
    }

    @Test
    fun placeHolder_withRtlText() {
        val text = "\u05D0\u05D0\u05D0\u05D0"
        val fontSize = 20f
        val paragraphWidth = fontSize * (text.length + 3)

        val height = 1.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline)
        val paragraph = simpleParagraph(
            text = text,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            fontSize = fontSize.sp,
            width = paragraphWidth
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)
        assertThat(paragraph.lineCount).isEqualTo(1)

        val bound = placeholderRects[0]!!
        val expectedBottom = paragraph.firstBaseline
        assertThat(bound.top).isEqualTo(expectedBottom - height.value * fontSize)
        assertThat(bound.bottom).isEqualTo(expectedBottom)

        // Text is alight right: |   RR_R|
        assertThat(bound.left).isEqualTo(paragraphWidth - 2 * fontSize)
        assertThat(bound.right).isEqualTo(paragraphWidth - 1 * fontSize)
    }

    @Test
    fun placeHolder_withBiDiText_coversRtlChar() {
        val text = "\u05D0\u05D0AA"
        val fontSize = 20f
        val paragraphWidth = fontSize * (text.length + 3)

        val height = 1.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline)
        val paragraph = simpleParagraph(
            text = text,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 2)),
            fontSize = fontSize.sp,
            width = paragraphWidth
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)
        assertThat(paragraph.lineCount).isEqualTo(1)

        val bound = placeholderRects[0]!!
        val expectedBottom = paragraph.firstBaseline
        assertThat(bound.top).isEqualTo(expectedBottom - height.value * fontSize)
        assertThat(bound.bottom).isEqualTo(expectedBottom)

        // Text is align right: |   LL_R|
        assertThat(bound.left).isEqualTo(paragraphWidth - 2 * fontSize)
        assertThat(bound.right).isEqualTo(paragraphWidth - 1 * fontSize)
    }

    @Test
    fun placeHolder_withBiDiText_coversLtrChar() {
        val text = "\u05D0\u05D0AA"
        val fontSize = 20f
        val paragraphWidth = fontSize * (text.length + 3)

        val height = 1.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline)
        val paragraph = simpleParagraph(
            text = text,
            placeholders = listOf(AnnotatedString.Range(placeholder, 2, 3)),
            fontSize = fontSize.sp,
            width = paragraphWidth
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)
        assertThat(paragraph.lineCount).isEqualTo(1)

        val bound = placeholderRects[0]!!
        val expectedBottom = paragraph.firstBaseline
        assertThat(bound.top).isEqualTo(expectedBottom - height.value * fontSize)
        assertThat(bound.bottom).isEqualTo(expectedBottom)

        // Text is align right: |   L_RR|
        assertThat(bound.left).isEqualTo(paragraphWidth - 3 * fontSize)
        assertThat(bound.right).isEqualTo(paragraphWidth - 2 * fontSize)
    }

    @Test
    fun placeHolder_withBiDiText_coversBiDiStr() {
        val text = "\u05D0\u05D0AA"
        val fontSize = 20f
        val paragraphWidth = fontSize * (text.length + 3)

        val height = 1.em
        val width = 1.em
        val placeholder = Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline)
        val paragraph = simpleParagraph(
            text = text,
            placeholders = listOf(AnnotatedString.Range(placeholder, 1, 3)),
            fontSize = fontSize.sp,
            width = paragraphWidth
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(1)
        assertThat(paragraph.lineCount).isEqualTo(1)

        val bound = placeholderRects[0]!!
        val expectedBottom = paragraph.firstBaseline
        assertThat(bound.top).isEqualTo(expectedBottom - height.value * fontSize)
        assertThat(bound.bottom).isEqualTo(expectedBottom)

        // Text is align right, 2 chars are covered: |   L_R|
        assertThat(bound.left).isEqualTo(paragraphWidth - 2 * fontSize)
        assertThat(bound.right).isEqualTo(paragraphWidth - 1 * fontSize)
    }

    @Test
    fun placeHolderRects_ellipsized() {
        val text = "ABC"
        val fontSize = 20f

        val placeholder = Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)
        val placeholders = listOf(
            AnnotatedString.Range(placeholder, 0, 1),
            AnnotatedString.Range(placeholder, 2, 3)
        )
        val paragraph = simpleParagraph(
            text = text,
            placeholders = placeholders,
            fontSize = fontSize.sp,
            width = 2 * fontSize,
            maxLines = 1,
            ellipsis = true
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(placeholders.size)
        assertThat(placeholderRects[0]).isNotNull()
        // The second placeholder should be ellipsized.
        assertThat(placeholderRects[1]).isNull()
    }

    @Test
    fun placeHolderRects_withLimitedHeight_ellipsized() {
        val text = "ABC"
        val fontSize = 20f

        val placeholder = Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)
        val placeholders = listOf(
            AnnotatedString.Range(placeholder, 0, 1),
            AnnotatedString.Range(placeholder, 2, 3)
        )
        val paragraph = simpleParagraph(
            text = text,
            placeholders = placeholders,
            fontSize = fontSize.sp,
            width = 2 * fontSize,
            height = 1.3f * fontSize,
            ellipsis = true
        )
        val placeholderRects = paragraph.placeholderRects
        assertThat(placeholderRects.size).isEqualTo(placeholders.size)
        assertThat(placeholderRects[0]).isNotNull()
        // The second placeholder should be ellipsized.
        assertThat(placeholderRects[1]).isNull()
    }

    private fun simpleParagraph(
        text: String = "",
        fontSize: TextUnit = TextUnit.Unspecified,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
        width: Float = Float.MAX_VALUE,
        height: Float = Float.MAX_VALUE,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false
    ): Paragraph {
        return Paragraph(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontFamily = fontFamilyMeasureFont
            ),
            spanStyles = spanStyles,
            placeholders = placeholders,
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = Constraints(maxWidth = width.ceilToInt(), maxHeight = height.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }
}