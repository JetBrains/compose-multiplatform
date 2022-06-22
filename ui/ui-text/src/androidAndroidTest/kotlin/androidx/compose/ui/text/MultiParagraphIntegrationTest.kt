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
package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.matchers.isZero
import androidx.compose.ui.text.platform.AndroidParagraph
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.filters.Suppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphIntegrationTest {
    private val fontFamilyMeasureFont = BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val ltrLocaleList = LocaleList("en")
    private val rtlLocaleList = LocaleList("ar")

    @Test
    fun didExceedMaxLines_withLineFeed() {
        // The text should be rendered with 3 lines:
        //     a
        //     b
        //     c
        val text = createAnnotatedString("a\nb", "c")
        // maxLines be 1 or 2, smaller than the line count 3
        for (i in 1..2) {
            val paragraph = simpleMultiParagraph(
                text = text,
                maxLines = i
            )
            assertWithMessage("text has 3 lines, maxLines = $i")
                .that(paragraph.didExceedMaxLines).isTrue()
        }

        // maxLines be 3, 4, 5 larger than the line count 3
        for (i in 3..5) {
            val paragraph = simpleMultiParagraph(
                text = text,
                maxLines = i
            )
            assertWithMessage("text has 3 lines, maxLines = $i")
                .that(paragraph.didExceedMaxLines).isFalse()
        }
    }

    @Test
    fun didExceedMaxLines_withLineWrap() {
        with(defaultDensity) {
            val fontSize = 50.sp
            // Each line has the space only for 1 character
            val width = fontSize.toPx()
            // The text should be rendered with 3 lines:
            //     a
            //     b
            //     c
            val text = createAnnotatedString("ab", "c")

            for (i in 1..2) {
                val paragraph = simpleMultiParagraph(
                    text = text,
                    fontSize = fontSize,
                    maxLines = i,
                    width = width
                )
                assertWithMessage("text has 3 lines, maxLines = $i")
                    .that(paragraph.didExceedMaxLines).isTrue()
            }

            for (i in 3..5) {
                val paragraph = simpleMultiParagraph(
                    text = text,
                    fontSize = fontSize,
                    maxLines = i
                )
                assertWithMessage("text has 3 lines, maxLines = $i")
                    .that(paragraph.didExceedMaxLines).isFalse()
            }
        }
    }

    @Test
    fun textOverflow_exceedMaxLines_singleParagraph() {
        val text = createAnnotatedString("a\nb")
        val paragraph = simpleMultiParagraph(text = text, maxLines = 1)

        assertThat(paragraph.paragraphInfoList[0].paragraph.didExceedMaxLines).isTrue()
    }

    @Test
    fun textOverflow_exceedMaxLinesInMiddle_multiParagraph() {
        val text = createAnnotatedString("a\nb", "a\nb")
        val paragraph = simpleMultiParagraph(text = text, maxLines = 3)

        assertThat(paragraph.paragraphInfoList[1].paragraph.didExceedMaxLines).isTrue()
    }

    @Test
    fun textOverflow_exceedMaxLinesInGap_multiParagraph() {
        val text = createAnnotatedString("a\nb", "a")
        val paragraph = simpleMultiParagraph(text = text, maxLines = 2)

        assertThat(paragraph.paragraphInfoList.size).isEqualTo(1)
    }

    @Test
    fun getPathForRange() {
        with(defaultDensity) {
            val text = createAnnotatedString("ab", "c", "de")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize
            )

            // Select "bcd"
            val actualPath = paragraph.getPathForRange(1, 4)

            val expectedPath = Path()
            // path covering "b"
            expectedPath.addRect(
                Rect(fontSizeInPx, 0f, fontSizeInPx * 2, fontSizeInPx)
            )
            // path covering "c"
            expectedPath.addRect(
                Rect(0f, fontSizeInPx, fontSizeInPx, fontSizeInPx * 2)
            )
            // path covering "d"
            expectedPath.addRect(
                Rect(0f, fontSizeInPx * 2, fontSizeInPx, fontSizeInPx * 3)
            )

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPathForRange_throws_exception_if_start_larger_than_end() {
        val text = "abc"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getPathForRange(textEnd, textStart)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPathForRange_throws_exception_if_start_is_smaller_than_zero() {
        val text = "abc"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getPathForRange(textStart - 2, textEnd - 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPathForRange_throws_exception_if_end_is_larger_than_text_length() {
        val text = "abc"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getPathForRange(textStart, textEnd + 1)
    }

    @Test
    fun getOffsetForPosition() {
        with(defaultDensity) {
            val lineLength = 2
            val text = createAnnotatedString(List(3) { "a".repeat(lineLength) })

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.roundToPx()
            // each line contains 2 character
            val width = 2 * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                width = width.toFloat()
            )
            // The text should be rendered as:
            //     aa
            //     aa
            //     aa
            for (i in 0 until text.length) {
                val row = i / lineLength
                val y = fontSizeInPx / 2 + fontSizeInPx * row
                val col = i % lineLength
                val x = fontSizeInPx * col

                val actualOffset = paragraph.getOffsetForPosition(
                    Offset(x.toFloat(), y.toFloat())
                )
                assertWithMessage("getOffsetForPosition($x, $y) failed")
                    .that(actualOffset).isEqualTo(i)
            }
        }
    }

    @Test
    fun getBoundingBox() {
        with(defaultDensity) {
            val lineLength = 2
            val text = createAnnotatedString(List(3) { "a".repeat(lineLength) })

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                width = text.length * fontSizeInPx
            )
            // The text should be rendered as:
            //     aa
            //     aa
            //     aa
            for (i in 0 until text.length) {
                val row = i / lineLength
                val col = i % lineLength

                val expectedBox = Rect(
                    left = col * fontSizeInPx,
                    right = (col + 1) * fontSizeInPx,
                    top = row * fontSizeInPx,
                    bottom = (row + 1) * fontSizeInPx
                )
                val actualBox = paragraph.getBoundingBox(i)

                assertWithMessage("getBoundingBox($i) failed")
                    .that(actualBox).isEqualTo(expectedBox)
            }
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getBoundingBox_offset_negative() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)
        paragraph.getBoundingBox(-1)
    }

    @Suppress
    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getBoundingBox_offset_larger_than_length_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)
        paragraph.getBoundingBox(text.length + 1)
    }

    @Test
    fun getHorizontalPosition() {
        with(defaultDensity) {
            val paragraphCount = 3
            val lineLength = 2
            val text = createAnnotatedString(List(paragraphCount) { "a".repeat(lineLength) })

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize
            )

            for (i in 0 until text.length) {
                val col = i % lineLength
                val expectPos = fontSizeInPx * col
                val actualPos = paragraph.getHorizontalPosition(i, true)
                assertWithMessage("getHorizontalPosition($i) failed")
                    .that(actualPos).isEqualTo(expectPos)
            }

            val expectPos = fontSizeInPx * lineLength
            val actualPos = paragraph.getHorizontalPosition(text.length, true)
            assertWithMessage("getHorizontalPosition(${text.length}) failed")
                .that(actualPos).isEqualTo(expectPos)
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getHorizontalPosition_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getHorizontalPosition(-1, true)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getHorizontalPosition_larger_than_length_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getHorizontalPosition(text.length + 1, true)
    }

    @Test
    fun getParagraphDirection_textDirection_Default() {
        val text = createAnnotatedString("a", "\u05D0", " ")
        val paragraph = simpleMultiParagraph(text = text, localeList = ltrLocaleList)

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
        assertThat(paragraph.getParagraphDirection(1)).isEqualTo(ResolvedTextDirection.Rtl)
        assertThat(paragraph.getParagraphDirection(2)).isEqualTo(ResolvedTextDirection.Ltr)
        assertThat(paragraph.getParagraphDirection(3)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test
    fun getParagraphDirection_textDirection_Content_withLtrLocale() {
        val text = createAnnotatedString("a", "\u05D0", " ")
        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(
                textDirection = TextDirection.Content,
                localeList = ltrLocaleList
            )
        )
        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
        assertThat(paragraph.getParagraphDirection(1)).isEqualTo(ResolvedTextDirection.Rtl)
        assertThat(paragraph.getParagraphDirection(2)).isEqualTo(ResolvedTextDirection.Ltr)
        assertThat(paragraph.getParagraphDirection(3)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test
    fun getParagraphDirection_textDirection_Content_withRtlLocale() {
        val text = createAnnotatedString("a", "\u05D0", " ")
        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(
                textDirection = TextDirection.Content,
                localeList = rtlLocaleList
            )
        )
        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
        assertThat(paragraph.getParagraphDirection(1)).isEqualTo(ResolvedTextDirection.Rtl)
        assertThat(paragraph.getParagraphDirection(2)).isEqualTo(ResolvedTextDirection.Rtl)
        assertThat(paragraph.getParagraphDirection(3)).isEqualTo(ResolvedTextDirection.Rtl)
    }

    @Test
    fun getParagraphDirection_textDirection_ForceLtr() {
        val text = createAnnotatedString("a", "\u05D0", " ")
        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(
                textDirection = TextDirection.Ltr
            )
        )

        for (i in 0..text.length) {
            assertWithMessage("getParagraphDirection($i) failed")
                .that(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
        }
    }

    @Test
    fun getParagraphDirection_textDirection_ForceRtl() {
        val text = createAnnotatedString("a", "\u05D0", " ")
        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(
                textDirection = TextDirection.Rtl
            )
        )

        for (i in 0..text.length) {
            assertWithMessage("getParagraphDirection($i) failed")
                .that(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getParagraphDirection_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getParagraphDirection(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getParagraphDirection_larger_than_length_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getParagraphDirection(text.length + 1)
    }

    @Test
    fun getBidiRunDirection() {
        val text = createAnnotatedString("a\u05D0", "\u05D0a")
        val paragraph = simpleMultiParagraph(text = text)

        assertThat(paragraph.getBidiRunDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
        assertThat(paragraph.getBidiRunDirection(1)).isEqualTo(ResolvedTextDirection.Rtl)

        assertThat(paragraph.getBidiRunDirection(2)).isEqualTo(ResolvedTextDirection.Rtl)
        assertThat(paragraph.getBidiRunDirection(3)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getBidiRunDirection_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getBidiRunDirection(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getBidiRunDirection_larger_than_length_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getBidiRunDirection(text.length + 1)
    }

    @Test
    fun getWordBoundary() {
        val text = createAnnotatedString("ab cd", "e f")
        val paragraph = simpleMultiParagraph(text = text)

        val textString = text.text
        assertThat(paragraph.getWordBoundary(textString.indexOf('a')))
            .isEqualTo(
                TextRange(
                    textString.indexOf('a'),
                    textString.indexOf('b') + 1
                )
            )

        assertThat(paragraph.getWordBoundary(textString.indexOf('d')))
            .isEqualTo(
                TextRange(
                    textString.indexOf('c'),
                    textString.indexOf('d') + 1
                )
            )

        assertThat(paragraph.getWordBoundary(textString.indexOf('e')))
            .isEqualTo(
                TextRange(
                    textString.indexOf('e'),
                    textString.indexOf('e') + 1
                )
            )

        assertThat(paragraph.getWordBoundary(textString.indexOf('f')))
            .isEqualTo(
                TextRange(
                    textString.indexOf('f'),
                    textString.indexOf('f') + 1
                )
            )
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getWordBoundary_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getWordBoundary(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getWordBoundary_larger_than_length_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getWordBoundary(text.length + 1)
    }

    @Test
    fun getCursorRect() {
        with(defaultDensity) {
            val paragraphCount = 3
            val lineLength = 2
            // A text with 3 lines and each line has 2 characters.
            val text = createAnnotatedString(List(paragraphCount) { "a".repeat(lineLength) })

            val fontSize = 10.sp
            val fontSizeInPx = fontSize.toPx()
            val width = 2 * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                width = width
            )

            for (i in 0 until text.length) {
                val row = i / lineLength
                val col = i % lineLength
                val top = row * fontSizeInPx
                val cursorXOffset = col * fontSizeInPx

                val expectRect = Rect(
                    left = cursorXOffset,
                    top = top,
                    right = cursorXOffset,
                    bottom = top + fontSizeInPx
                )
                val actualRect = paragraph.getCursorRect(i)

                assertWithMessage("getCursorRect($i) failed")
                    .that(actualRect).isEqualTo(expectRect)
            }

            // Last cursor position is the end of the last line.
            val row = paragraph.lineCount - 1
            val col = lineLength
            val top = row * fontSizeInPx
            val cursorXOffset = col * fontSizeInPx

            val expectRect = Rect(
                left = cursorXOffset,
                top = top,
                right = cursorXOffset,
                bottom = top + fontSizeInPx
            )
            val actualRect = paragraph.getCursorRect(text.length)
            assertWithMessage("getCursorRect(${text.length}) failed")
                .that(actualRect).isEqualTo(expectRect)
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getCursorRect_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getCursorRect(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getCursorRect_larger_than_length_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getCursorRect(text.length + 1)
    }

    @Test
    fun getLineForOffset() {
        val text = createAnnotatedString("a", "a\na")
        val paragraph = simpleMultiParagraph(text = text)

        assertThat(paragraph.getLineForOffset(0)).isEqualTo(0)
        assertThat(paragraph.getLineForOffset(1)).isEqualTo(1)
        // '\n' is not checked because it's Paragraph's implementation
        assertThat(paragraph.getLineForOffset(3)).isEqualTo(2)
    }

    @Test
    fun getLineForOffset_negative_returnsZero() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        assertThat(paragraph.getLineForOffset(-1)).isZero()
    }

    @Test
    fun getLineForOffset_larger_than_length_returnsLastLine() {
        val text = "abc\ndef"
        val paragraph = simpleMultiParagraph(text = text)

        assertThat(paragraph.getLineForOffset(text.length + 1))
            .isEqualTo(1)
    }

    @Test
    fun getLineLeft() {
        with(defaultDensity) {
            val text = createAnnotatedString("aa", "\u05D0\u05D0")

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth * 2

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                width = width
            )

            assertThat(paragraph.getLineLeft(0)).isEqualTo(0)
            assertThat(paragraph.getLineLeft(1)).isEqualTo(width - 2 * fontSizeInPx)
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineLeft_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineLeft(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineLeft_greaterThanOrEqual_lineCount_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineLeft(paragraph.lineCount)
    }

    @Test
    fun lineContentForEmptyText() {
        val text = ""
        val paragraph = simpleMultiParagraph(text = text)

        assertThat(paragraph.lineCount).isEqualTo(1)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(0)
        assertThat(paragraph.getLineLeft(0)).isEqualTo(0)
        assertThat(paragraph.getLineRight(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0, true)).isEqualTo(0)
        assertThat(paragraph.isLineEllipsized(0)).isFalse()
    }

    @Test
    fun getLineRight() {
        with(defaultDensity) {
            val text = createAnnotatedString("aa", "\u05D0\u05D0")

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth * 2

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                width = width
            )

            assertThat(paragraph.getLineRight(0)).isEqualTo(2 * fontSizeInPx)
            assertThat(paragraph.getLineRight(1)).isEqualTo(width)
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineRight_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineRight(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineRight_greaterThanOrEqual_lineCount_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineRight(paragraph.lineCount)
    }

    @Test
    fun getLineTop() {
        with(defaultDensity) {
            val text = createAnnotatedString("a", "a", "a")

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize
            )

            for (i in 0 until paragraph.lineCount) {
                assertWithMessage("bottom of line $i doesn't match")
                    .that(paragraph.getLineTop(i))
                    .isEqualTo(fontSizeInPx * i)
            }
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineTop_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineTop(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineTop_greaterThanOrEqual_lineCount_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineTop(paragraph.lineCount)
    }

    @Test
    fun getLineBottom() {
        with(defaultDensity) {
            val text = createAnnotatedString("a", "a", "a")

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize
            )

            for (i in 0 until paragraph.lineCount) {
                assertWithMessage("bottom of line $i doesn't match")
                    .that(paragraph.getLineBottom(i))
                    .isEqualTo(fontSizeInPx * (i + 1))
            }
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineBottom_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineBottom(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineBottom_greaterThanOrEqual_lineCount_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineBottom(paragraph.lineCount)
    }

    @Test
    fun getLineHeight() {
        with(defaultDensity) {
            val text = createAnnotatedString("a", "a", "a")

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize
            )

            for (i in 0 until paragraph.lineCount) {
                assertWithMessage("getLineHeight($i) failed")
                    .that(paragraph.getLineHeight(i)).isEqualTo(fontSizeInPx)
            }
        }
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineHeight_negative_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineHeight(-1)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun getLineHeight_greaterThanOrEqual_lineCount_throw_exception() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(text = text)

        paragraph.getLineHeight(paragraph.lineCount)
    }

    @Test
    fun textAlign_defaultValue_alignsStart() {
        val textLtr = "aa"
        val textRtl = "\u05D0\u05D0"
        val text = createAnnotatedString(textLtr, textRtl)

        // Width should be sufficient to make each paragraph one line.
        val width = 2 * simpleMultiParagraphIntrinsics(text).maxIntrinsicWidth

        val paragraph = simpleMultiParagraph(text = text, width = width)
        // When text align to start, Ltr text aligns to left, line left should be 0.
        assertThat(paragraph.getLineLeft(0)).isZero()
        // When text align to start, Rtl text aligns to right, line right should be width.
        assertThat(paragraph.getLineRight(1)).isEqualTo(width)
    }

    @Test
    fun textAlign_left_returnsZeroForGetLineLeft() {
        val textLtr = "aa"
        val textRtl = "\u05D0\u05D0"
        val text = createAnnotatedString(textLtr, textRtl)

        // Width should be sufficient to make each paragraph one line.
        val width = 2 * simpleMultiParagraphIntrinsics(text).maxIntrinsicWidth

        val paragraph = simpleMultiParagraph(
            text = text,
            width = width,
            style = TextStyle(textAlign = TextAlign.Left)
        )

        // When text align to left, line left should be 0 for both Ltr and Rtl text.
        assertThat(paragraph.getLineLeft(0)).isZero()
        assertThat(paragraph.getLineLeft(1)).isZero()
    }

    @Test
    fun textAlign_right_returnsWidthForGetLineRight() {
        val textLtr = "aa"
        val textRtl = "\u05D0\u05D0"
        val text = createAnnotatedString(textLtr, textRtl)

        // Width should be sufficient to make each paragraph one line.
        val width = 2 * simpleMultiParagraphIntrinsics(text).maxIntrinsicWidth

        val paragraph = simpleMultiParagraph(
            text = text,
            width = width,
            style = TextStyle(textAlign = TextAlign.Right)
        )

        // When text align to right, line right should be width for both Ltr and Rtl text.
        assertThat(paragraph.getLineRight(0)).isEqualTo(width)
        assertThat(paragraph.getLineRight(1)).isEqualTo(width)
    }

    @Test
    fun textAlign_center_textIsCentered() {
        with(defaultDensity) {
            val textLtr = "aa"
            val textRtl = "\u05D0\u05D0"
            val text = createAnnotatedString(textLtr, textRtl)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            // Width should be sufficient to make each paragraph one line.
            val width = 2 * simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                width = width,
                style = TextStyle(textAlign = TextAlign.Center)
            )

            val expectedLineLeft = width / 2 - (fontSizeInPx * textLtr.length) / 2
            val expectedLineRight = width / 2 + (fontSizeInPx * textLtr.length) / 2

            assertThat(paragraph.getLineLeft(0)).isEqualTo(expectedLineLeft)
            assertThat(paragraph.getLineRight(0)).isEqualTo(expectedLineRight)
            assertThat(paragraph.getLineLeft(1)).isEqualTo(expectedLineLeft)
            assertThat(paragraph.getLineRight(1)).isEqualTo(expectedLineRight)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    // We have to test strict justification above API 28 because of this bug b/68009059, where
    // devices before API 28 may have an extra space at the end of line.
    fun textAlign_justify_justifies() {
        val textLtr = "a a a"
        val textRtl = "\u05D0 \u05D0 \u05D0"
        val text = createAnnotatedString(textLtr, textRtl)

        // Justify only works for soft wrapped lines, so width is made insufficient.
        val width = simpleMultiParagraphIntrinsics(text).maxIntrinsicWidth - 1f

        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(textAlign = TextAlign.Justify),
            width = width
        )

        // When text is justified, line left is 0 while line right is width
        assertThat(paragraph.getLineLeft(0)).isZero()
        assertThat(paragraph.getLineRight(0)).isEqualTo(width)
        assertThat(paragraph.getLineLeft(2)).isZero()
        assertThat(paragraph.getLineRight(2)).isEqualTo(width)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27, minSdkVersion = 26)
    fun textAlign_justify_justifies_underApi28() {
        with(defaultDensity) {
            val textLtr = "a a a"
            val textRtl = "\u05D0 \u05D0 \u05D0"
            val text = createAnnotatedString(textLtr, textRtl)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            // Justify only works for soft wrapped lines, so width is made insufficient.
            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth - 1f

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(textAlign = TextAlign.Justify),
                width = width
            )

            // When Ltr text is justified, line left is 0.
            assertThat(paragraph.getLineLeft(0)).isZero()
            // When Ltr text is justified, line right is greater than when it's align left. We
            // can only assert a weaker condition due to bug b/68009059, where extra space is
            // added at the end of the line.
            assertThat(paragraph.getLineRight(0))
                .isGreaterThan("a a".length * fontSizeInPx)
            // When Rtl text is justified, line right is width.
            assertThat(paragraph.getLineRight(2)).isEqualTo(width)
            // Similar to Ltr text, when Rtl text is justified, line left is less than when it's
            // align right.
            assertThat(paragraph.getLineLeft(2))
                .isLessThan(width - "\u05D0 \u05D0".length * fontSizeInPx)
        }
    }

    @Test
    fun textAlign_start_alignsStart() {
        val textLtr = "aa"
        val textRtl = "\u05D0\u05D0"
        val text = createAnnotatedString(textLtr, textRtl)

        // Width should be sufficient to make each paragraph one line.
        val width = 2 * simpleMultiParagraphIntrinsics(text).maxIntrinsicWidth

        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(textAlign = TextAlign.Start),
            width = width
        )
        // When text align to start, Ltr text aligns to left, line left should be 0.
        assertThat(paragraph.getLineLeft(0)).isZero()
        // When text align to start, Rtl text aligns to right, line right should be width.
        assertThat(paragraph.getLineRight(1)).isEqualTo(width)
    }

    @Test
    fun textAlign_end_alignsEnd() {
        val textLtr = "aa"
        val textRtl = "\u05D0\u05D0"
        val text = createAnnotatedString(textLtr, textRtl)

        // Width should be sufficient to make each paragraph one line.
        val width = 2 * simpleMultiParagraphIntrinsics(text).maxIntrinsicWidth

        val paragraph = simpleMultiParagraph(
            text = text,
            style = TextStyle(textAlign = TextAlign.End),
            width = width
        )
        // When text align to start, Ltr text aligns to right, line right should be width.
        assertThat(paragraph.getLineRight(0)).isEqualTo(width)
        // When text align to start, Rtl text aligns to left, line left should 0.
        assertThat(paragraph.getLineLeft(1)).isZero()
    }

    @Test
    fun textDirection_content_withLtrLocale() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(
                    textDirection = TextDirection.Content,
                    localeList = ltrLocaleList
                ),
                width = width
            )

            // First paragraph should be rendered as: "a .", dot is visually after "a ".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo("a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: ". א", dot is visually before " א".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo(width - "\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: "  .", dot is visually after "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo("  ".length * fontSizeInPx)
        }
    }

    @Test
    fun textDirection_content_withRtlLocale() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(
                    textDirection = TextDirection.Content,
                    localeList = rtlLocaleList
                ),
                width = width
            )

            // First paragraph should be rendered as: "a .", dot is visually after "a ".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo("a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: ". א", dot is visually before " א".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo(width - "\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: ".  ", dot is visually before "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo(width - "  ".length * fontSizeInPx)
        }
    }

    @Test
    fun textDirection_forceLtr() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(textDirection = TextDirection.Ltr),
                width = width
            )

            // First paragraph should be rendered as: "a .", dot is visually after "a ".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo("a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: "א .", dot is visually after "א ".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo("\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: "  .", dot is visually after "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo("  ".length * fontSizeInPx)
        }
    }

    @Test
    fun textDirection_forceRtl() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = simpleMultiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(textDirection = TextDirection.Rtl),
                width = width
            )

            // First paragraph should be rendered as: ". a", dot is visually before " a".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo(width - "a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: ". א", dot is visually before " א".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo(width - "\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: ".  ", dot is visually before "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo(width - "  ".length * fontSizeInPx)
        }
    }

    @Test
    fun lineHeight_returnsSameAsGiven() {
        with(defaultDensity) {
            val text = createAnnotatedString("a\na\na", "a\na\na")
            // Need to specify font size in case the asserted line height happens to be the default
            // line height corresponding to the font size.
            val fontSize = 50.sp

            val lineHeight = 80.sp
            val lineHeightInPx = lineHeight.toPx()

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(lineHeight = lineHeight)
            )

            // Height of first and last line in each paragraph is influenced by includePadding.
            // So we only assert the inner paragraph lines' height.
            assertThat(paragraph.getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(paragraph.getLineHeight(4)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun textIndent_onFirstLine() {
        with(defaultDensity) {
            val text = createAnnotatedString("aaa", "\u05D0\u05D0\u05D0")
            val indent = 20.sp
            val indentInPx = indent.toPx()

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            // Width is the space needed by 2 characters
            val width = 2 * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                style = TextStyle(textIndent = TextIndent(firstLine = indent)),
                fontSize = fontSize,
                width = width
            )
            // The paragraph should be rendered as:
            //   a
            //  aa
            //  א
            //  אא
            assertThat(paragraph.getHorizontalPosition(0, true)).isEqualTo(indentInPx)
            assertThat(paragraph.getHorizontalPosition(1, true)).isZero()
            assertThat(paragraph.getHorizontalPosition(3, true)).isEqualTo(width - indentInPx)
            assertThat(paragraph.getHorizontalPosition(4, true)).isEqualTo(width)
        }
    }

    @Test
    @MediumTest
    fun textIndent_onRestLine() {
        with(defaultDensity) {
            val text = createAnnotatedString("aaa", "\u05D0\u05D0\u05D0")
            val indent = 20.sp
            val indentInPx = indent.toPx()

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            // Width is the space needed by 2 characters
            val width = 2 * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                style = TextStyle(textIndent = TextIndent(restLine = indent)),
                width = width
            )
            // The paragraph should be rendered as:
            //  aa
            //   a
            //  אא
            //  א
            assertThat(paragraph.getHorizontalPosition(0, true)).isZero()
            assertThat(paragraph.getHorizontalPosition(2, true)).isEqualTo(indentInPx)
            assertThat(paragraph.getHorizontalPosition(3, true)).isEqualTo(width)
            assertThat(paragraph.getHorizontalPosition(5, true)).isEqualTo(width - indentInPx)
        }
    }

    @Test
    fun annotatedString_haveParagraphStyle_withoutTextDirection() {
        // Provide an LTR text
        val text = AnnotatedString(
            text = "ab",
            paragraphStyles = listOf(
                Range(
                    item = ParagraphStyle(textDirection = TextDirection.Content),
                    start = 0,
                    end = "a".length
                ),
                Range(
                    // skip setting [TextDirection] on purpose, should inherit from the
                    // main [ParagraphStyle]
                    item = ParagraphStyle(),
                    start = "a".length,
                    end = "ab".length
                )
            )
        )

        val paragraph = MultiParagraph(
            annotatedString = text,
            style = TextStyle(textDirection = TextDirection.Rtl),
            constraints = Constraints(),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )

        // the first character uses TextDirection.Content, text is Ltr
        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
        // the second character should use TextDirection.Rtl since it should inherit
        // from main [ParagraphStyle]
        assertThat(paragraph.getParagraphDirection(1)).isEqualTo(ResolvedTextDirection.Rtl)
    }

    @Test
    fun maxIntrinsicWidth_withPlaceholder_inEm() {
        val text = AnnotatedString(text = "ab")
        val fontSize = 20
        val width = 2.em
        val placeholders = listOf(
            Range(
                Placeholder(width, 1.em, PlaceholderVerticalAlign.AboveBaseline),
                0,
                1
            )
        )

        val paragraph = MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            placeholders = placeholders,
            constraints = Constraints(),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )

        // Rendered as below:
        //   |  |b
        // The placeholder takes the space of 2 characters
        assertThat(paragraph.maxIntrinsicWidth).isEqualTo(fontSize * width.value + fontSize)
    }

    @Test
    fun maxIntrinsicWidth_withPlaceholder_inSp() {
        val text = AnnotatedString(text = "ab")
        val fontSize = 20
        val width = 30.sp
        val placeholders = listOf(
            Range(
                Placeholder(width, 1.em, PlaceholderVerticalAlign.AboveBaseline),
                0,
                1
            )
        )

        val paragraph = MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            placeholders = placeholders,
            constraints = Constraints(),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )

        // Rendered as below:
        //   |  |b
        // The placeholder takes 30 sp. In default density, it will be 30 pixels
        assertThat(paragraph.maxIntrinsicWidth).isEqualTo(fontSize + width.value)
    }

    @Test
    fun placeholderRects_withSingleParagraph() {
        val text = AnnotatedString(text = "ab")
        val fontSize = 20
        val width = 2.em
        val height = 1.em
        val placeholders = listOf(
            Range(
                Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline),
                0,
                1
            )
        )

        val paragraph = MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            placeholders = placeholders,
            constraints = Constraints(),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )

        assertThat(paragraph.placeholderRects).hasSize(1)
        assertThat(paragraph.placeholderRects[0]).isEqualTo(
            Rect(
                left = 0f,
                top = paragraph.firstBaseline - height.value * fontSize,
                right = width.value * fontSize,
                bottom = paragraph.firstBaseline
            )
        )
    }

    @Test
    fun placeholderRects_withMultipleParagraphs() {
        val text = createAnnotatedString("ab", "cd")
        val fontSize = 20
        val width = 2.em
        val height = 1.em
        val placeholders = listOf(
            Range(
                Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline),
                0,
                1
            ),
            Range(
                Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline),
                2,
                3
            )
        )

        val paragraph = MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            placeholders = placeholders,
            constraints = Constraints(),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )

        assertThat(paragraph.placeholderRects).hasSize(2)
        assertThat(paragraph.placeholderRects[0]).isEqualTo(
            Rect(
                left = 0f,
                top = paragraph.firstBaseline - height.value * fontSize,
                right = width.value * fontSize,
                bottom = paragraph.firstBaseline
            )
        )
        assertThat(paragraph.placeholderRects[1]).isEqualTo(
            Rect(
                left = 0f,
                top = paragraph.lastBaseline - height.value * fontSize,
                right = width.value * fontSize,
                bottom = paragraph.lastBaseline
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun placeholderRects_overlapWithParagraph() {
        val text = createAnnotatedString("ab", "cd")
        val fontSize = 20
        val width = 2.em
        val height = 1.em
        val placeholders = listOf(
            Range(
                Placeholder(width, height, PlaceholderVerticalAlign.AboveBaseline),
                1,
                3
            )
        )

        MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            placeholders = placeholders,
            constraints = Constraints(),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMinWidthConstraints_notSupported() {
        val minWidthConstraints = Constraints(minWidth = 100)
        MultiParagraph(
            annotatedString = AnnotatedString(""),
            style = TextStyle(),
            constraints = minWidthConstraints,
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMinHeightConstraints_notSupported() {
        val minHeightConstraints = Constraints(minHeight = 100)
        MultiParagraph(
            annotatedString = AnnotatedString(""),
            style = TextStyle(),
            constraints = minHeightConstraints,
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    @Test
    fun multiParagraphConstruction_doesNotThrow_ifNotAllParagraghsFitVertically() {
        with(defaultDensity) {
            val fontSize = 20.sp
            val constraints = Constraints(
                maxWidth = 10 * fontSize.roundToPx(),
                maxHeight = fontSize.roundToPx() / 2
            )
            val text = buildAnnotatedString {
                withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
                    append("Lorem")
                }
                withStyle(ParagraphStyle()) {
                    append("Ipsum")
                }
            }

            MultiParagraph(
                annotatedString = text,
                style = TextStyle(fontSize = fontSize, fontFamily = fontFamilyMeasureFont),
                constraints = constraints,
                density = this,
                fontFamilyResolver = UncachedFontFamilyResolver(context)
            )
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun multiParagraph_appliesBrush_toTheWholeText() = with(defaultDensity) {
        val fontSize = 20.sp
        val fontSizeInPx = fontSize.toPx()
        val brush = Brush.verticalGradient(listOf(Color.Blue, Color.Red))
        val multiParagraph = simpleMultiParagraph(
            text = buildAnnotatedString {
                withStyle(ParagraphStyle(textAlign = TextAlign.Right)) {
                    append("Lorem")
                }
                withStyle(ParagraphStyle()) {
                    append("Ipsum")
                }
            },
            style = TextStyle(
                brush = brush,
                fontSize = fontSize
            ),
            width = fontSizeInPx * 5
        ).apply { disableAntialias() }

        val multiParagraph2 = simpleMultiParagraph(
            text = buildAnnotatedString {
                append("Lorem\n")
                append("Ipsum")
            },
            style = TextStyle(
                brush = brush,
                fontSize = fontSize
            ),
            width = fontSizeInPx * 5
        ).apply { disableAntialias() }

        assertThat(multiParagraph.bitmap(brush))
            .isEqualToBitmap(multiParagraph2.bitmap(brush))
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun multiParagraph_overridesAlphaDuringDraw() = with(defaultDensity) {
        val fontSize = 20.sp
        val fontSizeInPx = fontSize.toPx()
        val brush = Brush.verticalGradient(listOf(Color.Blue, Color.Red))
        val multiParagraph = simpleMultiParagraph(
            text = buildAnnotatedString {
                withStyle(ParagraphStyle(textAlign = TextAlign.Right)) {
                    append("Lorem")
                }
                withStyle(ParagraphStyle()) {
                    append("Ipsum")
                }
            },
            style = TextStyle(
                brush = brush,
                alpha = 0.5f,
                fontSize = fontSize
            ),
            width = fontSizeInPx * 5
        ).apply { disableAntialias() }

        val multiParagraph2 = simpleMultiParagraph(
            text = buildAnnotatedString {
                append("Lorem\n")
                append("Ipsum")
            },
            style = TextStyle(
                brush = brush,
                fontSize = fontSize
            ),
            width = fontSizeInPx * 5
        ).apply { disableAntialias() }

        assertThat(multiParagraph.bitmap(brush))
            .isEqualToBitmap(multiParagraph2.bitmap(brush, 0.5f))
    }

    private fun MultiParagraph.disableAntialias() {
        paragraphInfoList.forEach {
            (it.paragraph as AndroidParagraph).textPaint.isAntiAlias = false
        }
    }

    /**
     * Helper function which creates an AnnotatedString where each input string becomes a paragraph.
     */
    private fun createAnnotatedString(vararg paragraphs: String) =
        createAnnotatedString(paragraphs.toList())

    /**
     * Helper function which creates an AnnotatedString where each input string becomes a paragraph.
     */
    private fun createAnnotatedString(paragraphs: List<String>): AnnotatedString {
        return buildAnnotatedString {
            for (paragraph in paragraphs) {
                pushStyle(ParagraphStyle())
                append(paragraph)
                pop()
            }
        }
    }

    private fun simpleMultiParagraphIntrinsics(
        text: AnnotatedString,
        fontSize: TextUnit = TextUnit.Unspecified,
        placeholders: List<Range<Placeholder>> = listOf()
    ): MultiParagraphIntrinsics {
        return MultiParagraphIntrinsics(
            text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize
            ),
            placeholders = placeholders,
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    private fun simpleMultiParagraph(
        text: String,
        style: TextStyle? = null,
        fontSize: TextUnit = TextUnit.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        width: Float = Float.MAX_VALUE
    ): MultiParagraph {
        return MultiParagraph(
            annotatedString = createAnnotatedString(text),
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize
            ).merge(style),
            maxLines = maxLines,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    private fun simpleMultiParagraph(
        text: AnnotatedString,
        style: TextStyle? = null,
        fontSize: TextUnit = TextUnit.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        width: Float = Float.MAX_VALUE,
        localeList: LocaleList? = null
    ): MultiParagraph {
        return MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                localeList = localeList
            ).merge(style),
            maxLines = maxLines,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }
}
