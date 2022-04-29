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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.platform.AndroidParagraph
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.unit.Constraints
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphFillBoundingBoxesTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val fontFamilyResolver = createFontFamilyResolver(
        InstrumentationRegistry.getInstrumentation().context
    )
    private val defaultDensity = Density(density = 1f)
    private val fontSize = 10.sp
    private val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

    @Test(expected = IllegalArgumentException::class)
    fun negativeStart() {
        val paragraph = Paragraph("a")
        paragraph.getBoundingBoxes(TextRange(1, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun startEqualToLength() {
        val paragraph = Paragraph("a")
        paragraph.getBoundingBoxes(TextRange(1, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun endGreaterThanLength() {
        val paragraph = Paragraph("a")
        paragraph.getBoundingBoxes(TextRange(0, 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun endEqualToStart() {
        val paragraph = Paragraph("a")
        paragraph.getBoundingBoxes(TextRange(0, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun arraySizeSmallerThanTextLength() {
        val text = "abc"
        val paragraph = Paragraph(text)
        val array = FloatArray(text.length * 4 - 1)
        paragraph.fillBoundingBoxes(TextRange(0, text.length), array, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun arraySizeSmallerThanRange() {
        val text = "abc"
        val paragraph = Paragraph(text)
        val startIndex = 1
        val endIndex = text.length
        val array = FloatArray((endIndex - startIndex) * 4 - 1)
        paragraph.fillBoundingBoxes(TextRange(startIndex, endIndex), array, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun arraySizeSmallerThanTextLengthWithStart() {
        val text = "abc"
        val paragraph = Paragraph(text)
        val startIndex = 0
        val endIndex = text.length
        val array = FloatArray(text.length * 8)
        val arrayStart = text.length * 4 + 1
        paragraph.fillBoundingBoxes(TextRange(startIndex, endIndex), array, arrayStart)
    }

    @Test
    fun singleCharacter() {
        val text = "a"
        val paragraph = Paragraph(text)

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(ltrCharacterBoundariesForTestFont(text))
    }

    @Test
    fun arrayFillStartsFromStartOffsetEndsAtEndOffset() {
        val text = "abc"
        val paragraph = Paragraph(text)
        val arraySizeToFill = text.length * 4
        // provide 3 times the array, first and last sections should not be filled.
        // fill with min value to check the not-filled indices
        val array = FloatArray(arraySizeToFill * 3) { Float.MIN_VALUE }

        // start filling from arraySizeToFill
        paragraph.fillBoundingBoxes(TextRange(0, text.length), array, arraySizeToFill)

        // first section is not changed
        for (index in 0 until arraySizeToFill) {
            assertThat(array[index]).isEqualTo(Float.MIN_VALUE)
        }

        // data is added to the middle section, and not equal to MIN_VALUE
        for (index in arraySizeToFill until (2 * arraySizeToFill)) {
            assertThat(array[index]).isNotEqualTo(Float.MIN_VALUE)
        }

        // last section is not changed
        for (index in 2 * arraySizeToFill until (3 * arraySizeToFill)) {
            assertThat(array[index]).isEqualTo(Float.MIN_VALUE)
        }
    }

    @Test
    fun overridesArray() {
        val text = "abc"
        val range = TextRange(0, text.length)
        val array = FloatArray(range.length * 4)

        val paragraph1 = Paragraph(text, style = TextStyle(fontSize = fontSize))
        paragraph1.fillBoundingBoxes(range, array, 0)

        assertThat(array.asRectArray()).isEqualToWithTolerance(
            ltrCharacterBoundariesForTestFont(text, fontSizeInPx)
        )

        val paragraph2 = Paragraph(text, style = TextStyle(fontSize = (fontSize * 2)))
        paragraph2.fillBoundingBoxes(range, array, 0)

        // the same array is overridden with new and different values
        assertThat(array.asRectArray()).isEqualToWithTolerance(
            ltrCharacterBoundariesForTestFont(text, fontSizeInPx * 2)
        )
    }

    @Test
    fun singleCharacterLineHeight() {
        val lineHeight = fontSize * 2
        val text = "a"
        val paragraph = Paragraph(text, style = TextStyle(lineHeight = lineHeight))

        // first line line height is ignored, therefore the result is the same as without line
        // height
        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(ltrCharacterBoundariesForTestFont(text))
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun singleCharacterLineHeight_includeFontPaddingIsFalse() {
        val lineHeight = fontSize * 2
        val lineHeightInPx = with(defaultDensity) { lineHeight.toPx() }
        val text = "a"
        val paragraph = Paragraph(
            text,
            style = TextStyle(
                lineHeight = lineHeight,
                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = Alignment.Proportional,
                    trim = Trim.None
                )
            ),
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            ltrCharacterBoundariesForTestFont(
                text = text,
                fontSizeInPx = fontSizeInPx,
                lineHeightInPx = lineHeightInPx
            )
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun multiLineCharacterLineHeight() {
        val lineHeight = fontSize * 2
        val lineHeightInPx = with(defaultDensity) { lineHeight.toPx() }
        val text = "a\na\na"
        @Suppress("DEPRECATION")
        val paragraph = Paragraph(
            text,
            style = TextStyle(
                lineHeight = lineHeight,
                lineHeightStyle = LineHeightStyle(
                    alignment = Alignment.Proportional,
                    trim = Trim.None
                ),
                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            ltrCharacterBoundariesForTestFont(
                text = text,
                fontSizeInPx = fontSizeInPx,
                lineHeightInPx = lineHeightInPx
            )
        )
    }

    @Test
    fun singleCharacterRtl() {
        val text = "\u05D0"
        val width = text.length * 2 * fontSizeInPx // a width wider than text
        val paragraph = Paragraph(
            text = text,
            width = width,
            style = TextStyle(textDirection = TextDirection.Content)
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(rtlCharacterBoundariesForTestFont(text, width))
    }

    @Test
    fun singleLineLtr() {
        val text = "abc"
        val paragraph = Paragraph(text)

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(ltrCharacterBoundariesForTestFont(text))
    }

    @Test
    fun singleLineRtl() {
        val text = "\u05D0\u05D1\u05D2"
        val width = text.length * 2 * fontSizeInPx // a width wider than text
        val paragraph = Paragraph(text, width = width)

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(rtlCharacterBoundariesForTestFont(text, width))
    }

    @Test
    fun bidiLtrLine() {
        val text = "a" + "\u05D0\u05D1" + "b"
        val paragraph = Paragraph(text)

        val expected = ltrCharacterBoundariesForTestFont(text)
        // text with indices 0123 is rendered as 0213
        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(arrayOf(expected[0], expected[2], expected[1], expected[3]))
    }

    @Test
    fun bidiRtlLine() {
        val text = "\u05D0" + "ab" + "\u05D1"
        val width = text.length * 2 * fontSizeInPx // a width wider than text
        val paragraph = Paragraph(width = width, text = text)

        val expected = rtlCharacterBoundariesForTestFont(text, width)
        // text with indices 0123 is rendered as 3120
        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(arrayOf(expected[0], expected[2], expected[1], expected[3]))
    }

    @Test
    fun multiLineLtr() {
        val text = "a\nb\nc"
        val paragraph = Paragraph(text)

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(ltrCharacterBoundariesForTestFont(text))
    }

    @Test
    fun multiLineRtl() {
        val text = "\u05D0\n\u05D1\n\u05D2"
        val width = 3 * fontSizeInPx // a width wider than paragraph
        val paragraph = Paragraph(width = width, text = text)

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(rtlCharacterBoundariesForTestFont(text, width))
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun zwjEmoji() {
        // Emoji 2.0 - family: man, woman, girl, boy
        // 2.0 released in Nov 2015; min version is set to SDK 24 which was released in 2016
        val text = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"
        val paragraph = Paragraph(text)

        val expected = paragraph.getBoundingBoxes(TextRange(0, text.length))

        // since we do not use the test font, the first rect should be non-zero
        // the remaining characters should have 0 width starting from the right of the
        // first character
        val initialRect = expected[0]
        assertThat(initialRect.width).isNonZero()
        for (index in 1 until expected.size) {
            assertThat(expected[index]).isEqualToWithTolerance(
                Rect(initialRect.right, initialRect.top, initialRect.right, initialRect.bottom)
            )
        }
    }

    // API 28 and below adds indent while calculating the right of the character
    // at the line end. should fix in the main code with a behavior switch before and after API 29.
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun withIndent() {
        val firstIndent = fontSize * 2
        val restIndent = fontSize

        val firstIndentInPx = with(defaultDensity) { firstIndent.toPx() }
        val restIndentInPx = with(defaultDensity) { restIndent.toPx() }
        val text = "abcd\ne"
        val paragraph = Paragraph(
            width = 3f * fontSizeInPx, // first indent is 2 char + 1 char will reach line break
            text = text,
            style = TextStyle(
                textIndent = TextIndent(firstLine = firstIndent, restLine = restIndent)
            )
        )

        // will be rendered as
        // _ _ a
        // _ b c
        // _ d \n
        // _ _ e
        val firstLeft = firstIndentInPx
        val restLeft = restIndentInPx

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                // a
                Rect(firstLeft, 0f, firstLeft + fontSizeInPx, fontSizeInPx),
                // b
                Rect(restLeft, fontSizeInPx, restLeft + fontSizeInPx, 2 * fontSizeInPx),
                // c
                Rect(
                    restLeft + fontSizeInPx,
                    fontSizeInPx,
                    restLeft + 2 * fontSizeInPx,
                    2 * fontSizeInPx
                ),
                // d
                Rect(restLeft, 2 * fontSizeInPx, restLeft + fontSizeInPx, 3 * fontSizeInPx),
                // \n
                Rect(
                    restLeft + fontSizeInPx,
                    2 * fontSizeInPx,
                    restLeft + fontSizeInPx,
                    3 * fontSizeInPx
                ),
                // e
                Rect(firstLeft, 3 * fontSizeInPx, firstLeft + fontSizeInPx, 4 * fontSizeInPx),
            )
        )
    }

    @Test
    fun variableFontSize() {
        val doubleFontSize = fontSize * 2
        val doubleFontSizeInPx = with(defaultDensity) { doubleFontSize.toPx() }
        val text = buildAnnotatedString {
            append("a")
            withStyle(SpanStyle(fontSize = doubleFontSize)) {
                append("b")
            }
            append("c")
            toAnnotatedString()
        }
        val paragraph = Paragraph(text)

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                // 1 width for a, height is doubleFontSize since line metrics change
                Rect(0f, 0f, fontSizeInPx, doubleFontSizeInPx),
                // 2 width for b
                Rect(fontSizeInPx, 0f, 3 * fontSizeInPx, doubleFontSizeInPx),
                // 1 width for c
                Rect(3 * fontSizeInPx, 0f, 4 * fontSizeInPx, doubleFontSizeInPx)
            )
        )
    }

    @Test
    fun letterSpacing() {
        val text = "abc\nde"
        val paragraph = Paragraph(
            text = text,
            style = TextStyle(letterSpacing = 1.em)
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                // a
                Rect(0f, 0f, 2 * fontSizeInPx, fontSizeInPx),
                // b
                Rect(2 * fontSizeInPx, 0f, 4 * fontSizeInPx, fontSizeInPx),
                // c
                Rect(4 * fontSizeInPx, 0f, 6 * fontSizeInPx, fontSizeInPx),
                // \n
                Rect(6 * fontSizeInPx, 0f, 6 * fontSizeInPx, fontSizeInPx),
                // c
                Rect(0f, fontSizeInPx, 2 * fontSizeInPx, 2 * fontSizeInPx),
                // d
                Rect(2 * fontSizeInPx, fontSizeInPx, 4 * fontSizeInPx, 2 * fontSizeInPx)
            )
        )
    }

    @Test
    fun textAlignCenter() {
        val text = "ab"
        val paragraph = Paragraph(
            width = text.length * fontSizeInPx * 2,
            text = text,
            style = TextStyle(textAlign = TextAlign.Center)
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                Rect(fontSizeInPx, 0f, 2 * fontSizeInPx, fontSizeInPx),
                Rect(2 * fontSizeInPx, 0f, 3 * fontSizeInPx, fontSizeInPx),
            )
        )
    }

    @Test
    fun textAlignEnd() {
        val text = "ab"
        val paragraph = Paragraph(
            width = text.length * fontSizeInPx * 2,
            text = text,
            style = TextStyle(textAlign = TextAlign.End)
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                Rect(2 * fontSizeInPx, 0f, 3 * fontSizeInPx, fontSizeInPx),
                Rect(3 * fontSizeInPx, 0f, 4 * fontSizeInPx, fontSizeInPx),
            )
        )
    }

    @Test
    fun withTextGeometricTransformScaleX() {
        val text = "ab"
        val paragraph = Paragraph(
            text = text,
            style = TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = 2.0f))
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                Rect(0f, 0f, 2 * fontSizeInPx, fontSizeInPx),
                Rect(2 * fontSizeInPx, 0f, 4 * fontSizeInPx, fontSizeInPx)
            )
        )
    }

    @Test
    fun textGeometricTransformSkewX() {
        val text = "ab"
        val paragraph = Paragraph(
            text = text,
            style = TextStyle(textGeometricTransform = TextGeometricTransform(skewX = -1f))
        )

        // skew does not change the boundary, character glyph goes outside of boundary
        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                Rect(0f, 0f, fontSizeInPx, fontSizeInPx),
                Rect(fontSizeInPx, 0f, 2 * fontSizeInPx, fontSizeInPx)
            )
        )
    }

    @Test
    fun baselineShift() {
        val shiftedFontSize = fontSize / 2f
        val shiftedFontSizeInPx = with(defaultDensity) { shiftedFontSize.toPx() }
        val text = buildAnnotatedString {
            append("a")
            withStyle(
                SpanStyle(
                    baselineShift = BaselineShift.Superscript,
                    fontSize = shiftedFontSize
                )
            ) {
                append("b")
            }
            append("c")
            toAnnotatedString()
        }
        val paragraph = Paragraph(text, width = 6 * fontSizeInPx)

        val shiftedStart = fontSizeInPx
        val shiftedEnd = shiftedStart + shiftedFontSizeInPx
        // shifted bottom and top still points to line bottom and top
        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                // a
                Rect(0f, 0f, fontSizeInPx, fontSizeInPx),
                // b
                Rect(shiftedStart, 0f, shiftedEnd, fontSizeInPx),
                // c
                Rect(shiftedEnd, 0f, shiftedEnd + fontSizeInPx, fontSizeInPx)
            )
        )
    }

    @Test
    fun inlineElement() {
        val doubleFontSize = fontSize * 2
        val doubleFontSizeInPx = with(defaultDensity) { doubleFontSize.toPx() }
        val text = "abc"
        val paragraph = Paragraph(
            text = text,
            placeholders = listOf(
                AnnotatedString.Range(
                    item = Placeholder(
                        width = doubleFontSize,
                        height = doubleFontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Top
                    ),
                    start = 1,
                    end = 2
                )
            )
        )

        assertThat(
            paragraph.getBoundingBoxes(TextRange(0, text.length))
        ).isEqualToWithTolerance(
            arrayOf(
                Rect(0f, 0f, fontSizeInPx, doubleFontSizeInPx),
                Rect(fontSizeInPx, 0f, 3 * fontSizeInPx, doubleFontSizeInPx),
                Rect(3 * fontSizeInPx, 0f, 4 * fontSizeInPx, doubleFontSizeInPx)
            )
        )
    }

    private fun AndroidParagraph.getBoundingBoxes(range: TextRange): Array<Rect> {
        val arraySize = range.length * 4
        val array = FloatArray(arraySize)
        this.fillBoundingBoxes(range, array, 0)
        return array.asRectArray()
    }

    private fun ltrCharacterBoundariesForTestFont(
        text: String,
        fontSizeInPx: Float = this.fontSizeInPx,
        // assumes that the test font is used and fontSize is equal to default line height
        lineHeightInPx: Float = fontSizeInPx,
        initialTop: Float = 0f
    ): Array<Rect> =
        getLtrCharacterBoundariesForTestFont(text, fontSizeInPx, lineHeightInPx, initialTop)

    private fun rtlCharacterBoundariesForTestFont(text: String, width: Float): Array<Rect> =
        getRtlCharacterBoundariesForTestFont(text, width, fontSizeInPx)

    private fun Paragraph(
        text: String,
        style: TextStyle? = null,
        width: Float = Float.MAX_VALUE,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf()
    ): AndroidParagraph = Paragraph(AnnotatedString(text), style, width, placeholders)

    private fun Paragraph(
        text: AnnotatedString,
        style: TextStyle? = null,
        width: Float = Float.MAX_VALUE,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf()
    ): AndroidParagraph {
        return Paragraph(
            text = text.text,
            style = TextStyle(
                fontSize = fontSize,
                fontFamily = fontFamilyMeasureFont,
                textDirection = TextDirection.Content
            ).merge(style),
            spanStyles = text.spanStyles,
            placeholders = placeholders,
            maxLines = Int.MAX_VALUE,
            ellipsis = false,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = fontFamilyResolver
        ) as AndroidParagraph
    }
}