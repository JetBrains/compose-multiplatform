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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.FontTestData.Companion.BASIC_KERN_FONT
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.FontTestData.Companion.FONT_100_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_200_REGULAR
import androidx.compose.ui.text.android.style.lineHeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.matchers.isZero
import androidx.compose.ui.text.platform.AndroidParagraph
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphIntegrationTest {
    private val fontFamilyMeasureFont = BASIC_MEASURE_FONT.toFontFamily()
    private val fontFamilyKernFont = BASIC_KERN_FONT.toFontFamily()
    private val fontFamilyCustom100 = FONT_100_REGULAR.toFontFamily()
    private val fontFamilyCustom200 = FONT_200_REGULAR.toFontFamily()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val ltrLocaleList = LocaleList("en")

    private val resourceLoader = UncachedFontFamilyResolver(context)

    @Test
    fun empty_string() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val text = ""
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = 100.0f
            )

            assertThat(paragraph.width).isEqualTo(100.0f)

            assertThat(paragraph.height).isEqualTo(fontSizeInPx)
            // defined in sample_font
            assertThat(paragraph.firstBaseline).isEqualTo(fontSizeInPx * 0.8f)
            assertThat(paragraph.lastBaseline).isEqualTo(fontSizeInPx * 0.8f)
            assertThat(paragraph.maxIntrinsicWidth).isZero()
            assertThat(paragraph.minIntrinsicWidth).isZero()
        }
    }

    @Test
    fun single_line_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("xyz", "\u05D0\u05D1\u05D2")) {
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    // width greater than text width - 150
                    width = 200.0f
                )

                assertWithMessage(text).that(paragraph.width).isEqualTo(200.0f)
                assertWithMessage(text).that(paragraph.height).isEqualTo(fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline).isEqualTo(fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.lastBaseline).isEqualTo(fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.length)
                assertWithMessage(text).that(paragraph.minIntrinsicWidth)
                    .isEqualTo(text.length * fontSizeInPx)
            }
        }
    }

    @Test
    fun line_break_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("abcdef", "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5")) {
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    // 3 chars width
                    width = 3 * fontSizeInPx
                )

                // 3 chars
                assertWithMessage(text).that(paragraph.width)
                    .isEqualTo(3 * fontSizeInPx)
                // 2 lines, 1 line gap
                assertWithMessage(text).that(paragraph.height)
                    .isEqualTo(2 * fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline)
                    .isEqualTo(fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.lastBaseline)
                    .isEqualTo(fontSizeInPx + fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.length)
                assertWithMessage(text).that(paragraph.minIntrinsicWidth)
                    .isEqualTo(text.length * fontSizeInPx)
            }
        }
    }

    @Test
    fun newline_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    // 3 chars width
                    width = 3 * fontSizeInPx
                )

                // 3 chars
                assertWithMessage(text).that(paragraph.width).isEqualTo(3 * fontSizeInPx)
                // 2 lines, 1 line gap
                assertWithMessage(text).that(paragraph.height)
                    .isEqualTo(2 * fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline).isEqualTo(fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.lastBaseline)
                    .isEqualTo(fontSizeInPx + fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
                assertWithMessage(text).that(paragraph.minIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
            }
        }
    }

    @Test
    fun newline_and_line_break_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    // 2 chars width
                    width = 2 * fontSizeInPx
                )

                // 2 chars
                assertWithMessage(text).that(paragraph.width).isEqualTo(2 * fontSizeInPx)
                // 4 lines, 3 line gaps
                assertWithMessage(text).that(paragraph.height)
                    .isEqualTo(4 * fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline)
                    .isEqualTo(fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.lastBaseline)
                    .isEqualTo(3 * fontSizeInPx + fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
                assertWithMessage(text).that(paragraph.minIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars 0, 1, 2 ...
            for (i in 0..text.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage("offset at index $i, position $position does not match")
                    .that(offset).isEqualTo(i)
            }
        }
    }

    @Test
    fun getOffsetForPosition_rtl() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars .., 2, 1, 0
            for (i in 0..text.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage("offset at index $i, position $position does not match")
                    .that(offset).isEqualTo(text.length - i)
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr_multiline() {
        with(defaultDensity) {
            val firstLine = "abc"
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = firstLine.length * fontSizeInPx
            )

            // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in 0..secondLine.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx * 1.5f))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage(
                    "offset at index $i, position $position, second line does not match"
                ).that(offset).isEqualTo(i + firstLine.length)
            }
        }
    }

    @Test
    fun getOffsetForPosition_rtl_multiline() {
        with(defaultDensity) {
            val firstLine = "\u05D0\u05D1\u05D2"
            val secondLine = "\u05D3\u05D4\u05D5"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = firstLine.length * fontSizeInPx
            )

            // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
            // which maps to chars 5, 4, 3
            for (i in 0..secondLine.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx * 1.5f))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage(
                    "offset at index $i, position $position, second line does not match"
                ).that(offset).isEqualTo(text.length - i)
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr_width_outOfBounds() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            // greater than width
            var position = Offset((fontSizeInPx * text.length * 2), (fontSizeInPx / 2))
            var offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isEqualTo(text.length)

            // negative
            position = Offset((-1 * fontSizeInPx), (fontSizeInPx / 2))
            offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isZero()
        }
    }

    @Test
    fun getOffsetForPosition_ltr_height_outOfBounds() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            // greater than height
            var position = Offset((fontSizeInPx / 2), (fontSizeInPx * text.length * 2))
            var offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isZero()

            // negative
            position = Offset((fontSizeInPx / 2), (-1 * fontSizeInPx))
            offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isZero()
        }
    }

    @Test
    fun getLineForVerticalPosition_ltr() {
        with(defaultDensity) {
            val text = "abcdefgh"
            val fontSize = 20f
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSize / 4
            val lineHeight = 30f

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp
                ),
                width = layoutWidth
            )

            assertThat(paragraph.lineCount).isEqualTo(4)
            // test positions are 1, lineHeight+1, 2lineHeight+1, 3lineHeight + 1 which map to line
            // 0, 1, 2, 3
            for (i in 0 until paragraph.lineCount) {
                val position = i * lineHeight.sp.toPx() + 1
                val line = paragraph.getLineForVerticalPosition(position)
                assertWithMessage(
                    "Line at line index $i, position $position does not match"
                ).that(line).isEqualTo(i)
            }
        }
    }

    @Test
    fun getLineForVerticalPosition_rtl() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7"
            val fontSize = 20f
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSize / 4
            val lineHeight = 30f

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp
                ),
                width = layoutWidth
            )

            assertThat(paragraph.lineCount).isEqualTo(4)
            // test positions are 1, lineHeight+1, 2lineHeight+1, 3lineHeight + 1 which map to line
            // 0, 1, 2, 3
            for (i in 0 until paragraph.lineCount) {
                val position = i * lineHeight.sp.toPx() + 1
                val line = paragraph.getLineForVerticalPosition(position)
                assertWithMessage(
                    "Line at line index $i, position $position does not match"
                ).that(line).isEqualTo(i)
            }
        }
    }

    @Test
    fun getLineForVerticalPosition_ltr_height_outOfBounds() {
        with(defaultDensity) {
            val text = "abcdefgh"
            val fontSize = 20f
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSize / 4
            val lineHeight = 30f

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp
                ),
                width = layoutWidth
            )

            assertThat(paragraph.lineCount).isEqualTo(4)
            // greater than height
            var position = lineHeight.sp.toPx() * paragraph.lineCount * 2
            var line = paragraph.getLineForVerticalPosition(position)
            assertThat(line).isEqualTo(paragraph.lineCount - 1)

            // negative
            position = -1 * lineHeight.sp.toPx()
            line = paragraph.getLineForVerticalPosition(position)
            assertThat(line).isZero()
        }
    }

    @Test
    fun getBoundingBox_ltr_singleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            // test positions that are 0, 1, 2 ... which maps to chars 0, 1, 2 ...
            for (i in 0..text.length - 1) {
                val box = paragraph.getBoundingBox(i)
                assertThat(box.left).isEqualTo(i * fontSizeInPx)
                assertThat(box.right).isEqualTo((i + 1) * fontSizeInPx)
                assertThat(box.top).isZero()
                assertThat(box.bottom).isEqualTo(fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_ltr_multiLines() {
        with(defaultDensity) {
            val firstLine = "abc"
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = firstLine.length * fontSizeInPx
            )

            // test positions are 3, 4, 5 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in secondLine.indices) {
                val textPosition = i + firstLine.length
                val box = paragraph.getBoundingBox(textPosition)
                assertThat(box.left).isEqualTo(i * fontSizeInPx)
                assertThat(box.right).isEqualTo((i + 1) * fontSizeInPx)
                assertThat(box.top).isEqualTo(fontSizeInPx)
                assertThat(box.bottom).isEqualTo(2f * fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_ltr_textPosition_negative() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            val textPosition = -1
            val box = paragraph.getBoundingBox(textPosition)
            assertThat(box.left).isZero()
            assertThat(box.right).isZero()
            assertThat(box.top).isZero()
            assertThat(box.bottom).isEqualTo(fontSizeInPx)
        }
    }

    @Test(expected = java.lang.IndexOutOfBoundsException::class)
    @SdkSuppress(minSdkVersion = 26)
    fun getBoundingBox_ltr_textPosition_larger_than_length_throw_exception() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            val textPosition = text.length + 1
            paragraph.getBoundingBox(textPosition)
        }
    }

    @Test(expected = java.lang.AssertionError::class)
    fun getCursorRect_larger_than_length_throw_exception() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            paragraph.getCursorRect(text.length + 1)
        }
    }

    @Test(expected = java.lang.AssertionError::class)
    fun getCursorRect_negative_throw_exception() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            paragraph.getCursorRect(-1)
        }
    }

    @Test
    fun getCursorRect_ltr_singleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            for (i in text.indices) {
                val cursorRect = paragraph.getCursorRect(i)
                val cursorXOffset = i * fontSizeInPx
                assertThat(cursorRect).isEqualTo(
                    Rect(
                        left = cursorXOffset,
                        top = 0f,
                        right = cursorXOffset,
                        bottom = fontSizeInPx
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_ltr_multiLines() {
        with(defaultDensity) {
            val text = "abcdef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val charsPerLine = 3
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = charsPerLine * fontSizeInPx
            )

            for (i in 0 until charsPerLine) {
                val cursorXOffset = i * fontSizeInPx
                assertThat(paragraph.getCursorRect(i)).isEqualTo(
                    Rect(
                        left = cursorXOffset,
                        top = 0f,
                        right = cursorXOffset,
                        bottom = fontSizeInPx
                    )
                )
            }

            for (i in charsPerLine until text.length) {
                val cursorXOffset = (i % charsPerLine) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i)).isEqualTo(
                    Rect(
                        left = cursorXOffset,
                        top = fontSizeInPx,
                        right = cursorXOffset,
                        bottom = fontSizeInPx * 2f
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_ltr_newLine() {
        with(defaultDensity) {
            val text = "abc\ndef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3)).isEqualTo(
                Rect(
                    left = 3 * fontSizeInPx,
                    top = 0f,
                    right = 3 * fontSizeInPx,
                    bottom = fontSizeInPx
                )
            )

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4)).isEqualTo(
                Rect(
                    left = 0f,
                    top = fontSizeInPx,
                    right = 0f,
                    bottom = fontSizeInPx * 2f
                )
            )
        }
    }

    @Test
    fun getCursorRect_ltr_newLine_last_char() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList)
            )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3)).isEqualTo(
                Rect(
                    left = 3 * fontSizeInPx,
                    top = 0f,
                    right = 3 * fontSizeInPx,
                    bottom = fontSizeInPx
                )
            )

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4)).isEqualTo(
                Rect(
                    left = 0f,
                    top = fontSizeInPx,
                    right = 0f,
                    bottom = fontSizeInPx * 2f
                )
            )
        }
    }

    @Test
    fun getCursorRect_rtl_singleLine() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            for (i in text.indices) {
                val cursorXOffset = (text.length - i) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i)).isEqualTo(
                    Rect(
                        left = cursorXOffset,
                        top = 0f,
                        right = cursorXOffset,
                        bottom = fontSizeInPx
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_rtl_multiLines() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val charsPerLine = 3
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = charsPerLine * fontSizeInPx
            )

            for (i in 0 until charsPerLine) {
                val cursorXOffset = (charsPerLine - i) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i)).isEqualTo(
                    Rect(
                        left = cursorXOffset,
                        top = 0f,
                        right = cursorXOffset,
                        bottom = fontSizeInPx
                    )
                )
            }

            for (i in charsPerLine until text.length) {
                val cursorXOffset = (charsPerLine - i % charsPerLine) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i)).isEqualTo(
                    Rect(
                        left = cursorXOffset,
                        top = fontSizeInPx,
                        right = cursorXOffset,
                        bottom = fontSizeInPx * 2f
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_rtl_newLine() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = 3 * fontSizeInPx
            )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3)).isEqualTo(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = 0f,
                    bottom = fontSizeInPx
                )
            )

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4)).isEqualTo(
                Rect(
                    left = 3 * fontSizeInPx,
                    top = fontSizeInPx,
                    right = 3 * fontSizeInPx,
                    bottom = fontSizeInPx * 2f
                )
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun getCursorRect_rtl_newLine_last_char() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                width = 3 * fontSizeInPx
            )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3)).isEqualTo(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = 0f,
                    bottom = fontSizeInPx
                )
            )

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4)).isEqualTo(
                Rect(
                    left = 0f,
                    top = fontSizeInPx,
                    right = 0f,
                    bottom = fontSizeInPx * 2f
                )
            )
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(fontSizeInPx * i)
            }

            for (i in 1 until rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualTo(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualTo(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(0, true)).isEqualTo(width)

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(0, true)).isZero()

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualTo(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in 0..ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(fontSizeInPx * i)
            }

            for (i in 1 until rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualTo(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualTo(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(0, true)).isEqualTo(width)

            for (i in 1 until ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(rtlText.length * fontSizeInPx + i * fontSizeInPx)
            }

            for (i in 0..rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualTo(rtlText.length * fontSizeInPx - i * fontSizeInPx)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    // The behavior of getPrimaryHorizontal on API 19 to API 22 was wrong. Suppress this test.
    @SdkSuppress(minSdkVersion = 23)
    fun getHorizontalPositionForOffset_primary_rtl_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_newLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualTo(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_rtl_newLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = text.length * fontSizeInPx
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(fontSizeInPx * i)
            }

            for (i in 0..rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualTo(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(0, false)).isZero()

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualTo(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(0, false)).isEqualTo(width)

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(fontSizeInPx * i)
            }

            for (i in rtlText.indices) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualTo(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualTo(width - rtlText.length * fontSizeInPx)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(0, false))
                .isEqualTo(width - ltrText.length * fontSizeInPx)

            for (i in 1..ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(rtlText.length * fontSizeInPx + i * fontSizeInPx)
            }

            for (i in 1..rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualTo(rtlText.length * fontSizeInPx - i * fontSizeInPx)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    // The behavior of getSecondaryHorizontal on API 19 to API 22 was wrong. Suppress this test.
    fun getHorizontalPositionForOffset_notPrimary_rtl_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_newLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualTo(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_rtl_newLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    fun getParagraphDirection_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getParagraphDirection_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in text.indices) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getParagraphDirection_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getBidiRunDirection_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getBidiRunDirection_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in text.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in 0 until text.length - 1) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
            assertThat(
                paragraph.getBidiRunDirection(text.length - 1)
            ).isEqualTo(ResolvedTextDirection.Ltr)
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = width
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun locale_withCJK_shouldNotDrawSame() {
        with(defaultDensity) {
            val text = "\u82B1"
            val fontSize = 10.sp
            val fontSizeInPx = fontSize.toPx()
            val locales = arrayOf(
                // duplicate ja is on purpose
                LocaleList("ja"),
                LocaleList("ja"),
                LocaleList("zh-CN"),
                LocaleList("zh-TW")
            )

            val bitmaps = locales.map { localeList ->
                val paragraph = Paragraph(
                    text = text,
                    spanStyles = listOf(),
                    style = TextStyle(
                        fontSize = fontSize,
                        localeList = localeList
                    ),
                    density = defaultDensity,
                    fontFamilyResolver = resourceLoader,
                    // just have 10x font size to have a bitmap
                    constraints = Constraints(maxWidth = (fontSizeInPx * 10).ceilToInt())
                )

                paragraph.bitmap()
            }

            assertThat(bitmaps[0]).isEqualToBitmap(bitmaps[1])
            assertThat(bitmaps[1]).isNotEqualToBitmap(bitmaps[2])
            assertThat(bitmaps[1]).isNotEqualToBitmap(bitmaps[3])
            // this does not work on API 21
            // assertThat(bitmaps[2], not(equalToBitmap(bitmaps[3])))
        }
    }

    @Test
    fun lineCount_withMaxLineSmallerThanTextLines() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val lineCount = text.lines().size
        val maxLines = lineCount - 1
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize),
            maxLines = maxLines
        )

        assertThat(paragraph.lineCount).isEqualTo(maxLines)
    }

    @Test
    fun lineCount_withMaxLineGreaterThanTextLines() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val lineCount = text.lines().size
        val maxLines = lineCount + 1
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize),
            maxLines = maxLines
        )

        assertThat(paragraph.lineCount).isEqualTo(lineCount)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun maxLines_withMaxLineEqualsZero_throwsException() {
        simpleParagraph(
            text = "",
            maxLines = 0
        )
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun maxLines_withMaxLineNegative_throwsException() {
        simpleParagraph(
            text = "",
            maxLines = -1
        )
    }

    @Test
    fun maxLines_withMaxLineSmallerThanTextLines_clipHeight() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount - 1
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines
            )

            val expectHeight = maxLines * fontSizeInPx
            assertThat(paragraph.height).isEqualTo(expectHeight)
        }
    }

    @Test
    fun maxLines_withMaxLineSmallerThanTextLines_haveCorrectBaselines() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount - 1
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines
            )

            val expectFirstBaseline = 0.8f * fontSizeInPx
            assertThat(paragraph.firstBaseline).isEqualTo(expectFirstBaseline)
            val expectLastBaseline = (maxLines - 1) * fontSizeInPx + 0.8f * fontSizeInPx
            assertThat(paragraph.lastBaseline).isEqualTo(expectLastBaseline)
        }
    }

    @Test
    fun maxLines_withMaxLineEqualsTextLine() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = text.lines().size
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines
            )

            val expectHeight = maxLines * fontSizeInPx
            assertThat(paragraph.height).isEqualTo(expectHeight)
        }
    }

    @Test
    fun maxLines_withMaxLineGreaterThanTextLines() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount + 1
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
                width = 200f
            )

            val expectHeight = lineCount * fontSizeInPx
            assertThat(paragraph.height).isEqualTo(expectHeight)
        }
    }

    @Test
    fun maxLines_paintDifferently() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = 1

            val paragraphWithMaxLine = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
                width = fontSizeInPx
            )

            val paragraphNoMaxLine = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = fontSizeInPx
            )

            // Make sure the maxLine is applied correctly
            assertThat(paragraphNoMaxLine.height).isGreaterThan(paragraphWithMaxLine.height)

            val imageNoMaxLine = ImageBitmap(
                paragraphNoMaxLine.width.roundToInt(),
                paragraphNoMaxLine.height.roundToInt(),
                ImageBitmapConfig.Argb8888
            )
            // Same size with imageNoMaxLine for comparison
            val imageWithMaxLine = ImageBitmap(
                paragraphNoMaxLine.width.roundToInt(),
                paragraphNoMaxLine.height.roundToInt(),
                ImageBitmapConfig.Argb8888
            )

            paragraphNoMaxLine.paint(Canvas(imageNoMaxLine))
            paragraphWithMaxLine.paint(Canvas(imageWithMaxLine))
            assertThat(imageNoMaxLine.asAndroidBitmap()).isNotEqualToBitmap(
                imageWithMaxLine
                    .asAndroidBitmap()
            )
        }
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_returnsTrue() {
        val text = "aaa\naa"
        val maxLines = text.lines().size - 1
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines
        )

        assertThat(paragraph.didExceedMaxLines).isTrue()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines
        )

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size + 1
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines
        )

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_withLineWrap_returnsTrue() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = 1
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
                // One line can only contain 1 character
                width = fontSizeInPx
            )

            assertThat(paragraph.didExceedMaxLines).isTrue()
        }
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_withLineWrap_returnsFalse() {
        val text = "a"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines
        )

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_withLineWrap_returnsFalse() {
        with(defaultDensity) {
            val text = "aa"
            val maxLines = 3
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
                // One line can only contain 1 character
                width = fontSizeInPx
            )

            assertThat(paragraph.didExceedMaxLines).isFalse()
        }
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesSmallerThanTextLines_returnsTrue() {
        val text = "aaa\naa"
        val maxLines = text.lines().size - 1
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines,
            ellipsis = true
        )

        assertThat(paragraph.didExceedMaxLines).isTrue()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesEqualToTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines,
            ellipsis = true
        )

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesGreaterThanTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size + 1
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines,
            ellipsis = true
        )

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesSmallerThanTextLines_withLineWrap_returnsTrue() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = 1
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
                ellipsis = true,
                // One line can only contain 1 character
                width = fontSizeInPx
            )

            assertThat(paragraph.didExceedMaxLines).isTrue()
        }
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesEqualToTextLines_withLineWrap_returnsFalse() {
        val text = "a"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(
            text = text,
            maxLines = maxLines,
            ellipsis = true
        )

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesGreaterThanTextLines_withLineWrap_returnsFalse() {
        with(defaultDensity) {
            val text = "aa"
            val maxLines = 3
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
                ellipsis = true,
                // One line can only contain 1 character
                width = fontSizeInPx
            )

            assertThat(paragraph.didExceedMaxLines).isFalse()
        }
    }

    @Test
    fun textAlign_defaultValue_alignsStart() {
        with(defaultDensity) {
            val textLTR = "aa"
            val textRTL = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val layoutLTRWidth = (textLTR.length + 2) * fontSizeInPx
            val paragraphLTR = simpleParagraph(
                text = textLTR,
                style = TextStyle(fontSize = fontSize),
                width = layoutLTRWidth
            )

            val layoutRTLWidth = (textRTL.length + 2) * fontSizeInPx
            val paragraphRTL = simpleParagraph(
                text = textRTL,
                style = TextStyle(fontSize = fontSize),
                width = layoutRTLWidth
            )

            // When textAlign is TextAlign.start, LTR aligns to left, RTL aligns to right.
            assertThat(paragraphLTR.getLineLeft(0)).isZero()
            assertThat(paragraphRTL.getLineRight(0)).isEqualTo(layoutRTLWidth)
        }
    }

    @Test
    fun textAlign_whenAlignLeft_returnsZeroForGetLineLeft() {
        with(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize,
                        textAlign = TextAlign.Left
                    ),
                    width = layoutWidth
                )

                assertThat(paragraph.getLineLeft(0)).isZero()
            }
        }
    }

    @Test
    fun textAlign_whenAlignRight_returnsLayoutWidthForGetLineRight() {
        with(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize,
                        textAlign = TextAlign.Right
                    ),

                    width = layoutWidth
                )

                assertThat(paragraph.getLineRight(0)).isEqualTo(layoutWidth)
            }
        }
    }

    @Test
    fun textAlign_whenAlignCenter_textIsCentered() {
        with(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize,
                        textAlign = TextAlign.Center
                    ),
                    width = layoutWidth
                )

                val textWidth = text.length * fontSizeInPx
                assertThat(paragraph.getLineLeft(0)).isEqualTo(layoutWidth / 2 - textWidth / 2)
                assertThat(paragraph.getLineRight(0)).isEqualTo(layoutWidth / 2 + textWidth / 2)
            }
        }
    }

    @Test
    fun textAlign_whenAlignStart_withLTR_returnsZeroForGetLineLeft() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.Start
                ),
                width = layoutWidth
            )

            assertThat(paragraph.getLineLeft(0)).isZero()
        }
    }

    @Test
    fun textAlign_whenAlignEnd_withLTR_returnsLayoutWidthForGetLineRight() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.End
                ),
                width = layoutWidth
            )

            assertThat(paragraph.getLineRight(0)).isEqualTo(layoutWidth)
        }
    }

    @Test
    fun textAlign_whenAlignStart_withRTL_returnsLayoutWidthForGetLineRight() {
        with(defaultDensity) {
            val text = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.Start
                ),
                width = layoutWidth
            )

            assertThat(paragraph.getLineRight(0)).isEqualTo(layoutWidth)
        }
    }

    @Test
    fun textAlign_whenAlignEnd_withRTL_returnsZeroForGetLineLeft() {
        with(defaultDensity) {
            val text = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,

                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.End
                ),
                width = layoutWidth
            )

            assertThat(paragraph.getLineLeft(0)).isZero()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    // We have to test justification above API 28 because of this bug b/68009059, where devices
    // before API 28 may have an extra space at the end of line.
    fun textAlign_whenAlignJustify_justifies() {
        with(defaultDensity) {
            val text = "a a a"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = ("a a".length + 1) * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.Justify
                ),
                width = layoutWidth
            )

            assertThat(paragraph.getLineLeft(0)).isZero()
            assertThat(paragraph.getLineRight(0)).isEqualTo(layoutWidth)
            // Last line should align start
            assertThat(paragraph.getLineLeft(1)).isZero()
        }
    }

    @Test
    fun textDirection_whenLTR_dotIsOnRight() {
        with(defaultDensity) {
            val text = "a.."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = layoutWidth
            )

            // The position of the last character in display order.
            val position = Offset(("a.".length * fontSizeInPx + 1), (fontSizeInPx / 2))
            val charIndex = paragraph.getOffsetForPosition(position)
            assertThat(charIndex).isEqualTo(2)
        }
    }

    @Test
    fun textDirection_whenRTL_dotIsOnLeft() {
        with(defaultDensity) {
            val text = "a.."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Rtl
                ),
                width = layoutWidth
            )

            // The position of the first character in display order.
            val position = Offset((fontSizeInPx / 2 + 1), (fontSizeInPx / 2))
            val charIndex = paragraph.getOffsetForPosition(position)
            assertThat(charIndex).isEqualTo(2)
        }
    }

    @Test
    fun textDirection_whenDefault_withoutStrongChar_directionIsLTR() {
        with(defaultDensity) {
            val text = "..."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                width = layoutWidth
            )

            for (i in 0..text.length) {
                // The position of the i-th character in display order.
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val charIndex = paragraph.getOffsetForPosition(position)
                assertThat(charIndex).isEqualTo(i)
            }
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharLTR_directionIsLTR() {
        with(defaultDensity) {
            val text = "a\u05D0."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = layoutWidth
            )

            for (i in text.indices) {
                // The position of the i-th character in display order.
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val charIndex = paragraph.getOffsetForPosition(position)
                assertThat(charIndex).isEqualTo(i)
            }
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharRTL_directionIsRTL() {
        with(defaultDensity) {
            val text = "\u05D0a."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = layoutWidth
            )

            // The first character in display order should be '.'
            val position = Offset((fontSizeInPx / 2 + 1), (fontSizeInPx / 2))
            val index = paragraph.getOffsetForPosition(position)
            assertThat(index).isEqualTo(2)
        }
    }

    @Test
    fun getLineTop() {
        with(defaultDensity) {
            val text = "aaa\nbbb"

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )
            assertThat(paragraph.getLineTop(0)).isZero()
            assertThat(paragraph.getLineTop(1)).isEqualTo(fontSizeInPx)
        }
    }

    @Test
    fun getLineBottom() {
        with(defaultDensity) {
            val text = "aaa\nbbb"

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )
            assertThat(paragraph.getLineBottom(0)).isEqualTo(fontSizeInPx)
            assertThat(paragraph.getLineBottom(1)).isEqualTo(fontSize.value * 2f)
        }
    }

    @Test
    fun getLineForOffset_withNewline() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )
        for (i in 0..2) {
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(0)
        }
        for (i in 4..6) {
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(1)
        }
    }

    @Test
    fun getLineForOffset_newline_belongsToPreviousLine() {
        val text = "aaa\nbbb\n"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )
        assertThat(paragraph.getLineForOffset(3)).isEqualTo(0)
        assertThat(paragraph.getLineForOffset(7)).isEqualTo(1)
    }

    @Test
    fun getLineForOffset_outOfBoundary() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )
        assertThat(paragraph.getLineForOffset(-1)).isEqualTo(0)
        assertThat(paragraph.getLineForOffset(-2)).isEqualTo(0)

        assertThat(paragraph.getLineForOffset(text.length)).isEqualTo(1)
        assertThat(paragraph.getLineForOffset(text.length + 1)).isEqualTo(1)
    }

    @Test
    fun getLineForOffset_ellipsisApplied() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(
            text = text,
            maxLines = 1,
            ellipsis = true,
            style = TextStyle(),
            width = Float.MAX_VALUE
        )

        for (i in 0..2) {
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(0)
        }
        assertThat(paragraph.getLineForOffset(3)).isEqualTo(0)
        for (i in 4..6) {
            // It returns 0 because the second line(index 1) is ellipsized
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(0)
        }
        // It returns 0 since the paragraph actually has 1 line
        assertThat(paragraph.getLineForOffset(text.length + 1)).isEqualTo(0)

        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(text.length)
    }

    @Test
    fun getLineStart_linebreak() {
        val text = "aaabbb"
        val fontSize = 50f

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize.sp
            ),
            width = fontSize * 3
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineStart(1)).isEqualTo(3)
    }

    @Test
    fun getLineStart_newline() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(text.lines().size)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        // First char after '\n'
        assertThat(paragraph.getLineStart(1))
            .isEqualTo(text.indexOfFirst { ch -> ch == '\n' } + 1)
    }

    @Test
    fun getLineStart_emptyLine() {
        val text = "aaa\n"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineStart(1)).isEqualTo(4)
    }

    @Test
    fun getLineEnd_linebreak() {
        val text = "aaabbb"
        val fontSize = 50f

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize.sp
            ),
            width = fontSize * 3,
            density = defaultDensity
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineStart(1)).isEqualTo(3)
    }

    @Test
    fun getLineEnd_newline() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(text.lines().size)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(text.indexOfFirst { ch -> ch == '\n' } + 1)
        assertThat(paragraph.getLineEnd(1)).isEqualTo(text.length)
    }

    @Test
    fun getLineEnd_emptyLine() {
        val text = "aaa\n"

        val paragraph = simpleParagraph(
            text = text,
            width = Float.MAX_VALUE
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(4)
        assertThat(paragraph.getLineEnd(1)).isEqualTo(4)
    }

    @Test
    fun getLineEllipsisOffset() {
        val text = "aaa\nbbb\nccc"

        val paragraph = simpleParagraph(
            text = text,
            maxLines = 2,
            ellipsis = true,
            width = Float.MAX_VALUE
        )

        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(4)
        assertThat(paragraph.getLineEnd(0, true)).isEqualTo(3) // "\n" is excluded
        assertThat(paragraph.isLineEllipsized(0)).isFalse()

        assertThat(paragraph.getLineEnd(1)).isEqualTo(text.length)
        assertThat(paragraph.getLineEnd(1, true)).isEqualTo(7) // "\n" is excluded
        assertThat(paragraph.isLineEllipsized(1)).isTrue()
    }

    @Test
    fun getLineEllipsisCount() {
        val text = "aaaaabbbbbccccc"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 10.sp
            ),
            maxLines = 2,
            ellipsis = true,
            width = 50f
        )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)

        assertThat(paragraph.isLineEllipsized(0)).isFalse()
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(5)
        assertThat(paragraph.getLineEnd(0, true)).isEqualTo(5)

        assertThat(paragraph.isLineEllipsized(1)).isTrue()
        assertThat(paragraph.getLineStart(1)).isEqualTo(5)
        assertThat(paragraph.getLineEnd(1)).isEqualTo(text.length)
        // The ellipsizer may reserve multiple characters for drawing HORIZONTAL ELLIPSIS
        // character (U+2026). We can only expect the visible end is not the end of the line.
        assertThat(paragraph.getLineEnd(1, true)).isNotEqualTo(text.length)
    }

    @Test
    fun lineHeight_inSp() {
        val text = "abcdefgh"
        val fontSize = 20f
        // Make the layout 4 lines
        val layoutWidth = text.length * fontSize / 4
        val lineHeight = 30f

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                lineHeight = lineHeight.sp
            ),
            width = layoutWidth
        )

        assertThat(paragraph.lineCount).isEqualTo(4)
        // First/last line is influenced by top/bottom padding
        for (i in 1 until paragraph.lineCount - 1) {
            val actualHeight = paragraph.getLineHeight(i)
            // In the sample_font.ttf, the height of the line should be
            // fontSize + 0.2f * fontSize(line gap)
            assertWithMessage("line number $i").that(actualHeight).isEqualTo(lineHeight)
        }
    }

    @Test
    fun lineHeight_InEm() {
        val text = "abcdefgh"
        val fontSize = 20f
        // Make the layout 4 lines
        val layoutWidth = text.length * fontSize / 4
        val lineHeight = 1.5f

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.em),
            width = layoutWidth
        )

        assertThat(paragraph.lineCount).isEqualTo(4)
        // First/last line is influenced by top/bottom padding
        for (i in 1 until paragraph.lineCount - 1) {
            val actualHeight = paragraph.getLineHeight(i)
            // In the sample_font.ttf, the height of the line should be
            // fontSize + 0.2f * fontSize(line gap)
            assertWithMessage("line number $i")
                .that(actualHeight).isEqualTo(lineHeight * fontSize)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun lineHeight_InEm_when_includeFontPadding_is_false() {
        val text = "abcdefgh"
        val fontSize = 20f
        // Make the layout 4 lines
        val layoutWidth = text.length * fontSize / 4
        val lineHeight = 2f

        @Suppress("DEPRECATION")
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                lineHeight = lineHeight.em,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            width = layoutWidth
        ) as AndroidParagraph

        val fontMetrics = paragraph.paragraphIntrinsics.textPaint.fontMetricsInt
        val ascentToLineHeightRatio = abs(fontMetrics.ascent.toFloat()) / fontMetrics.lineHeight()
        val extraLineHeight = (lineHeight * fontSize) - fontSize
        val ascentExtra = extraLineHeight * ascentToLineHeightRatio
        val descentExtra = extraLineHeight - ascentExtra

        assertThat(paragraph.lineCount).isEqualTo(4)
        assertThat(paragraph.getLineHeight(0)).isEqualTo(fontSize + descentExtra)
        assertThat(paragraph.getLineHeight(1)).isEqualTo(fontSize * lineHeight)
        assertThat(paragraph.getLineHeight(2)).isEqualTo(fontSize * lineHeight)
        assertThat(paragraph.getLineHeight(3)).isEqualTo(fontSize + ascentExtra)
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalTextApi::class)
    @Test
    fun lineHeight_IsAppliedToFirstLine_when_includeFontPadding_is_true() {
        // values such as text or TextStyle attributes are from the b/227095468
        val text = "AAAAAA ".repeat(20)
        val fontSize = 12.sp
        val lineHeight = 16.052.sp
        val maxLines = 4
        val textStyle = TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            platformStyle = PlatformTextStyle(includeFontPadding = true)
        )

        val paragraph = simpleParagraph(
            text = text,
            style = textStyle,
            maxLines = maxLines,
            ellipsis = true,
            width = 480f // px
        ) as AndroidParagraph

        // In LineHeightSpan line height is being ceiled and ratio calculated accordingly.
        // Then LineHeightSpan changes the descent and ascent, but Android ignores the ascent
        // change for the first line.
        // Therefore the descent changes and that's what caused the 1px diff in b/227095468
        // Here in order to stabilize the behavior we do the same calculation
        val lineHeightInPx = ceil(with(defaultDensity) { lineHeight.toPx() })
        val fontMetrics = paragraph.paragraphIntrinsics.textPaint.fontMetricsInt
        val ratio = lineHeightInPx / (fontMetrics.descent - fontMetrics.ascent)
        val expectedDescent = ceil(fontMetrics.descent * ratio.toDouble()).toInt()
        val expectedAscent = expectedDescent - lineHeightInPx

        val expectedFirstLineHeight = expectedDescent - fontMetrics.ascent
        val expectedSecondLineHeight = expectedDescent - expectedAscent
        assertThat(paragraph.getLineHeight(0)).isEqualTo(expectedFirstLineHeight)
        for (i in 1..3) {
            assertThat(paragraph.getLineHeight(i)).isEqualTo(expectedSecondLineHeight)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)
            val paragraphWidth = fontSizeInPx * text.length

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = paragraphWidth
            )

            // Make sure there is only one line, so that we can use getLineRight to test fontSize.
            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            assertThat(paragraph.getLineWidth(0)).isEqualTo(fontSizeInPx * text.length)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnPartOfText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyleFontSize = 30.sp
            val spanStyleFontSizeInPx = spanStyleFontSize.toPx()
            val spanStyle = SpanStyle(fontSize = spanStyleFontSize)
            val paragraphWidth = spanStyleFontSizeInPx * text.length

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                style = TextStyle(fontSize = fontSize),
                width = paragraphWidth
            )

            // Make sure there is only one line, so that we can use getLineRight to test fontSize.
            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedLineRight = "abc".length * spanStyleFontSizeInPx +
                "de".length * fontSizeInPx
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedLineRight)
        }
    }

    @Test
    fun testAnnotatedString_seFontSizeTwice_lastOneOverwrite() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)

            val fontSizeOverwrite = 30.sp
            val fontSizeOverwriteInPx = fontSizeOverwrite.toPx()
            val spanStyleOverwrite = SpanStyle(fontSize = fontSizeOverwrite)
            val paragraphWidth = fontSizeOverwriteInPx * text.length

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(spanStyle, 0, text.length),
                    AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                ),
                width = paragraphWidth
            )

            // Make sure there is only one line, so that we can use getLineRight to test fontSize.
            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = "abc".length * fontSizeOverwriteInPx + "de".length * fontSizeInPx
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScale() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val em = 0.5.em
            val spanStyle = SpanStyle(fontSize = em)

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                style = TextStyle(fontSize = fontSize)
            )

            assertThat(paragraph.getLineRight(0))
                .isEqualTo(text.length * fontSizeInPx * em.value)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleNested() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val em = 0.5f.em
            val spanStyle = SpanStyle(fontSize = em)

            val emNested = 2f.em
            val spanStyleNested = SpanStyle(fontSize = emNested)

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(spanStyle, 0, text.length),
                    AnnotatedString.Range(spanStyleNested, 0, text.length)
                ),
                style = TextStyle(fontSize = fontSize)
            )

            assertThat(paragraph.getLineRight(0))
                .isEqualTo(text.length * fontSizeInPx * em.value * emNested.value)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeFirst() {
        with(defaultDensity) {
            val text = "abcde"
            val paragraphFontSize = 20.sp

            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSize)

            val em = 0.5f.em
            val fontSizeScaleStyle = SpanStyle(fontSize = em)

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(fontSizeStyle, 0, text.length),
                    AnnotatedString.Range(fontSizeScaleStyle, 0, text.length)
                ),
                style = TextStyle(fontSize = paragraphFontSize)
            )

            assertThat(paragraph.getLineRight(0))
                .isEqualTo(text.length * fontSizeInPx * em.value)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeSecond() {
        with(defaultDensity) {
            val text = "abcde"
            val paragraphFontSize = 20.sp

            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSize)

            val em = 0.5f.em
            val fontSizeScaleStyle = SpanStyle(fontSize = em)

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(fontSizeScaleStyle, 0, text.length),
                    AnnotatedString.Range(fontSizeStyle, 0, text.length)
                ),
                style = TextStyle(fontSize = paragraphFontSize)
            )

            assertThat(paragraph.getLineRight(0)).isEqualTo(text.length * fontSizeInPx)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeNested() {
        with(defaultDensity) {
            val text = "abcde"
            val paragraphFontSize = 20.sp

            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSize)

            val em1 = 0.5f.em
            val fontSizeScaleStyle1 = SpanStyle(fontSize = em1)

            val em2 = 2f.em
            val fontSizeScaleStyle2 = SpanStyle(fontSize = em2)

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(fontSizeScaleStyle1, 0, text.length),
                    AnnotatedString.Range(fontSizeStyle, 0, text.length),
                    AnnotatedString.Range(fontSizeScaleStyle2, 0, text.length)
                ),
                style = TextStyle(fontSize = paragraphFontSize)
            )

            assertThat(paragraph.getLineRight(0))
                .isEqualTo(text.length * fontSizeInPx * em2.value)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inEm_OnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            assertThat(paragraph.getLineWidth(0))
                .isEqualTo(fontSizeInPx * text.length * (1 + letterSpacing))
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_OnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.sp)

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            assertThat(paragraph.getLineWidth(0))
                .isEqualTo((fontSizeInPx + letterSpacing) * text.length)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnPartText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = ("abc".length * letterSpacing + text.length) * fontSizeInPx
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacingTwice_lastOneOverwrite() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val letterSpacingOverwrite = 10.0f
            val spanStyleOverwrite = SpanStyle(letterSpacing = letterSpacingOverwrite.em)

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(
                    AnnotatedString.Range(spanStyle, 0, text.length),
                    AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                ),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = "abc".length * (1 + letterSpacingOverwrite) * fontSizeInPx +
                "de".length * (1 + letterSpacing) * fontSizeInPx
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inEm_withFontSize() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 2f
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val fontSizeOverwrite = 30.sp
            val fontSizeOverwriteInPx = fontSizeOverwrite.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSizeOverwrite)

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(
                    AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                    AnnotatedString.Range(fontSizeStyle, 0, "abc".length)
                ),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = (1 + letterSpacing) *
                ("abc".length * fontSizeOverwriteInPx + "de".length * fontSizeInPx)
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inEm_withScaleX() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 2f
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val scaleX = 1.5f
            val scaleXStyle = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX))

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(
                    AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                    AnnotatedString.Range(scaleXStyle, 0, "abc".length)
                ),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = (1 + letterSpacing) *
                ("abc".length * fontSizeInPx * scaleX + "de".length * fontSizeInPx)
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_withFontSize() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 10.sp
            val letterSpacingInPx = letterSpacing.toPx()
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing)

            val fontSizeOverwrite = 30.sp
            val fontSizeOverwriteInPx = fontSizeOverwrite.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSizeOverwrite)

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(
                    AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                    AnnotatedString.Range(fontSizeStyle, 0, "abc".length)
                ),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = text.length * letterSpacingInPx +
                ("abc".length * fontSizeOverwriteInPx + "de".length * fontSizeInPx)
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_withScaleX() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 10.sp
            val letterSpacingInPx = letterSpacing.toPx()
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing)

            val scaleX = 1.5f
            val scaleXStyle = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX))

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(
                    AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                    AnnotatedString.Range(scaleXStyle, 0, "abc".length)
                ),
                width = Float.MAX_VALUE
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = text.length * letterSpacingInPx +
                ("abc".length * fontSizeInPx * scaleX + "de".length * fontSizeInPx)
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_after_inEm() {
        val text = "abcde"
        val fontSize = 20f

        val letterSpacingEm = 1f
        val letterSpacingEmStyle = SpanStyle(letterSpacing = letterSpacingEm.em)

        val letterSpacingSp = 10f
        val letterSpacingSpStyle = SpanStyle(letterSpacing = letterSpacingSp.sp)

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            spanStyles = listOf(
                AnnotatedString.Range(letterSpacingEmStyle, 0, text.length),
                AnnotatedString.Range(letterSpacingSpStyle, 0, "abc".length)
            ),
            width = Float.MAX_VALUE
        )

        assertThat(paragraph.lineCount).isEqualTo(1)
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth = fontSize * text.length + "abc".length * letterSpacingSp +
            "de".length * fontSize * letterSpacingEm
        assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inEm_after_inSp() {
        val text = "abcde"
        val fontSize = 20f

        val letterSpacingEm = 1f
        val letterSpacingEmStyle = SpanStyle(letterSpacing = letterSpacingEm.em)

        val letterSpacingSp = 10f
        val letterSpacingSpStyle = SpanStyle(letterSpacing = letterSpacingSp.sp)

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            spanStyles = listOf(
                AnnotatedString.Range(letterSpacingSpStyle, 0, "abc".length),
                AnnotatedString.Range(letterSpacingEmStyle, 0, text.length)
            ),
            width = 500f
        )

        assertThat(paragraph.lineCount).isEqualTo(1)
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth = fontSize * text.length * (1 + letterSpacingEm)
        assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
    }

    @Test
    fun textIndent_inSp_onSingleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val indent = 20.sp
            val indentInPx = indent.toPx()

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textIndent = TextIndent(firstLine = indent),
                    fontFamily = fontFamilyMeasureFont
                )
            )

            // This position should point to the first character 'a' if indent is applied.
            // Otherwise this position will point to the second character 'b'.
            val position = Offset((indentInPx + 1), (fontSizeInPx / 2))
            // The offset corresponding to the position should be the first char 'a'.
            assertThat(paragraph.getOffsetForPosition(position)).isZero()
        }
    }

    @Test
    fun textIndent_inSp_onFirstLine() {
        val text = "abcdef"
        val fontSize = 20f
        val indent = 15f
        val paragraphWidth = "abcd".length * fontSize

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                textIndent = TextIndent(firstLine = indent.sp),
                fontFamily = fontFamilyMeasureFont
            ),
            width = paragraphWidth
        )

        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getHorizontalPosition(0, true)).isEqualTo(indent)
    }

    @Test
    fun textIndent_inSp_onRestLine() {
        val text = "abcde"
        val fontSize = 20f
        val indent = 20f
        val paragraphWidth = "abc".length * fontSize

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                textIndent = TextIndent(restLine = indent.sp),
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            width = paragraphWidth
        )

        // check the position of the first character in second line: "d" should be indented
        assertThat(paragraph.getHorizontalPosition(3, true)).isEqualTo(indent)
    }

    @Test
    fun textIndent_inEm_onSingleLine() {
        val text = "abc"
        val fontSize = 20f
        val indent = 1.5f

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                textIndent = TextIndent(firstLine = indent.em),
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            )
        )

        assertThat(paragraph.getHorizontalPosition(0, true)).isEqualTo(indent * fontSize)
    }

    @Test
    fun textIndent_inEm_onFirstLine() {
        val text = "abcdef"
        val fontSize = 20f
        val indent = 1.5f

        val paragraphWidth = "abcd".length * fontSize

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                textIndent = TextIndent(firstLine = indent.em),
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            width = paragraphWidth
        )

        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getHorizontalPosition(0, true)).isEqualTo(indent * fontSize)
    }

    @Test
    fun textIndent_inEm_onRestLine() {
        val text = "abcdef"
        val fontSize = 20f
        val indent = 1.5f

        val paragraphWidth = "abcd".length * fontSize

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                textIndent = TextIndent(restLine = indent.em),
                fontSize = fontSize.sp,
                fontFamily = fontFamilyMeasureFont
            ),
            width = paragraphWidth
        )

        assertThat(paragraph.lineCount).isEqualTo(2)
        // check the position of the first character in second line: "e" should be indented
        assertThat(paragraph.getHorizontalPosition(4, true)).isEqualTo(indent * fontSize)
    }

    @Test
    fun testAnnotatedString_fontFamily_changesMeasurement() {
        with(defaultDensity) {
            val text = "ad"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            // custom 100 regular font has b as the wide glyph
            // custom 200 regular font has d as the wide glyph
            val spanStyle = SpanStyle(fontFamily = fontFamilyCustom200)
            // a is rendered in paragraphStyle font (custom 100), it will not have wide glyph
            // d is rendered in defaultSpanStyle font (custom 200), and it will be wide glyph
            val expectedWidth = fontSizeInPx + fontSizeInPx * 3

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(spanStyle, "a".length, text.length)
                ),
                style = TextStyle(
                    fontSize = fontSize,
                    fontFamily = fontFamilyCustom100
                )
            )

            assertThat(paragraph.lineCount).isEqualTo(1)
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_fontFeature_turnOffKern() {
        with(defaultDensity) {
            val text = "AaAa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            // This fontFeatureSetting turns off the kerning
            val spanStyle = SpanStyle(fontFeatureSettings = "\"kern\" 0")

            val paragraph = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(spanStyle, 0, "aA".length)
                ),
                style = TextStyle(
                    fontSize = fontSize,
                    fontFamily = fontFamilyKernFont
                )
            )

            // Two characters are kerning, so minus 0.4 * fontSize
            val expectedWidth = text.length * fontSizeInPx - 0.4f * fontSizeInPx
            assertThat(paragraph.lineCount).isEqualTo(1)
            assertThat(paragraph.getLineWidth(0)).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_shadow() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length

            val spanStyle = SpanStyle(
                shadow = Shadow(
                    Color(0xFF00FF00),
                    Offset(1f, 2f),
                    3.0f
                )
            )

            val paragraphShadow = simpleParagraph(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(spanStyle, 0, text.length)
                ),
                width = paragraphWidth
            )

            val paragraph = simpleParagraph(
                text = text,
                width = paragraphWidth
            )

            assertThat(paragraphShadow.bitmap()).isNotEqualToBitmap(paragraph.bitmap())
        }
    }

    @Test
    fun testDefaultSpanStyle_setColor() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length

            val paragraphWithoutColor = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = paragraphWidth
            )

            val paragraphWithColor = simpleParagraph(
                text = text,
                style = TextStyle(
                    color = Color.Red,
                    fontSize = fontSize
                ),
                width = paragraphWidth
            )

            assertThat(paragraphWithColor.bitmap())
                .isNotEqualToBitmap(paragraphWithoutColor.bitmap())
        }
    }

    @Test
    fun testDefaultSpanStyle_setLetterSpacing() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 1f

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    letterSpacing = letterSpacing.em,
                    fontSize = fontSize
                )
            )

            assertThat(paragraph.getLineRight(0))
                .isEqualTo(fontSizeInPx * (1 + letterSpacing) * text.length)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testDefaultSpanStyle_setBrush() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length

            val paragraphWithoutBrush = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = paragraphWidth
            )

            val paragraphWithBrush = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
                ),
                width = paragraphWidth
            )

            assertThat(paragraphWithBrush.bitmap())
                .isNotEqualToBitmap(paragraphWithoutBrush.bitmap())
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testDefaultSpanStyle_setBrushAlpha() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length
            val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))

            val paragraphWithoutAlpha = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, brush = brush),
                width = paragraphWidth
            )

            val paragraphWithAlpha = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = brush,
                    alpha = 0.5f
                ),
                width = paragraphWidth
            )

            assertThat(paragraphWithoutAlpha.bitmap())
                .isNotEqualToBitmap(paragraphWithAlpha.bitmap())
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testDefaultSpanStyle_overrideAlphaDuringDraw() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length
            val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))

            val paragraphWithoutAlpha = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, brush = brush),
                width = paragraphWidth
            )

            val paragraphWithAlpha = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = brush,
                    alpha = 0.5f
                ),
                width = paragraphWidth
            )

            assertThat(paragraphWithoutAlpha.bitmap(brush, 0.5f))
                .isEqualToBitmap(paragraphWithAlpha.bitmap())
        }
    }

    @Test
    fun testGetPathForRange_singleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(
                Rect(
                    lineLeft,
                    0f,
                    lineRight - fontSizeInPx,
                    fontSizeInPx
                )
            )

            // Select "ab"
            val actualPath = paragraph.getPathForRange(0, 2)

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_multiLines() {
        with(defaultDensity) {
            val text = "abc\nabc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            val expectedPath = Path()
            val firstLineLeft = paragraph.getLineLeft(0)
            val secondLineLeft = paragraph.getLineLeft(1)
            val firstLineRight = paragraph.getLineRight(0)
            val secondLineRight = paragraph.getLineRight(1)
            expectedPath.addRect(
                Rect(
                    firstLineLeft + fontSizeInPx,
                    0f,
                    firstLineRight,
                    fontSizeInPx
                )
            )
            expectedPath.addRect(
                Rect(
                    secondLineLeft,
                    fontSizeInPx,
                    secondLineRight - fontSizeInPx,
                    paragraph.height
                )
            )

            // Select "bc\nab"
            val actualPath = paragraph.getPathForRange(1, 6)

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Bidi() {
        with(defaultDensity) {
            val textLTR = "Hello"
            val textRTL = "שלום"
            val text = textLTR + textRTL
            val selectionLTRStart = 2
            val selectionRTLEnd = 2
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(
                Rect(
                    lineLeft + selectionLTRStart * fontSizeInPx,
                    0f,
                    lineLeft + textLTR.length * fontSizeInPx,
                    fontSizeInPx
                )
            )
            expectedPath.addRect(
                Rect(
                    lineRight - selectionRTLEnd * fontSizeInPx,
                    0f,
                    lineRight,
                    fontSizeInPx
                )
            )

            // Select "llo..של"
            val actualPath =
                paragraph.getPathForRange(selectionLTRStart, textLTR.length + selectionRTLEnd)

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Start_Equals_End_Returns_Empty_Path() {
        val text = "abc"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            )
        )

        val actualPath = paragraph.getPathForRange(1, 1)

        assertThat(actualPath.getBounds()).isEqualTo(Rect.Zero)
    }

    @Test
    fun testGetPathForRange_Empty_Text() {
        val text = ""
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            )
        )

        val actualPath = paragraph.getPathForRange(0, 0)

        assertThat(actualPath.getBounds()).isEqualTo(Rect.Zero)
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_Second_Character_Selected() {
        with(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            // Try to select "\uDD1E\uD834\uDD1F", only "\uD834\uDD1F" is selected.
            val actualPath = paragraph.getPathForRange(1, text.length)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, 2 * fontSizeInPx, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_End_Middle_Second_Character_Selected() {
        with(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            // Try to select "\uDD1E\uD834", actually "\uD834\uDD1F" is selected.
            val actualPath = paragraph.getPathForRange(1, text.length - 1)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, fontSizeInPx, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_End_Same_Character_Returns_Line_Segment() {
        with(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            // Try to select "\uDD1E", get vertical line segment after this character.
            val actualPath = paragraph.getPathForRange(1, 2)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, fontSizeInPx, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Emoji_Sequence() {
        with(defaultDensity) {
            val text = "\uD83D\uDE00\uD83D\uDE03\uD83D\uDE04\uD83D\uDE06"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            // Select "\u1F603\u1F604"
            val actualPath = paragraph.getPathForRange(1, text.length - 1)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, fontSizeInPx * 3, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Unicode_200D_Return_Line_Segment() {
        with(defaultDensity) {
            val text = "\u200D"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSizeInPx))

            val actualPath = paragraph.getPathForRange(0, 1)

            assertThat(lineLeft).isEqualTo(lineRight)
            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Unicode_2066_Return_Line_Segment() {
        with(defaultDensity) {
            val text = "\u2066"
            val fontSize = 20f.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                )
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSizeInPx))

            val actualPath = paragraph.getPathForRange(0, 1)

            assertThat(lineLeft).isEqualTo(lineRight)
            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetWordBoundary() {
        val text = "abc def"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            )
        )

        val result = paragraph.getWordBoundary(text.indexOf('a'))

        assertThat(result.start).isEqualTo(text.indexOf('a'))
        assertThat(result.end).isEqualTo(text.indexOf(' '))
    }

    @Test
    fun testGetWordBoundary_spaces() {
        val text = "ab cd  e"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            )
        )

        // end of word (length+1) will select word
        val singleSpaceStartResult = paragraph.getWordBoundary(text.indexOf('b') + 1)
        assertThat(singleSpaceStartResult.start).isEqualTo(text.indexOf('a'))
        assertThat(singleSpaceStartResult.end).isEqualTo(text.indexOf('b') + 1)

        // beginning of word will select word
        val singleSpaceEndResult = paragraph.getWordBoundary(text.indexOf('c'))
        assertThat(singleSpaceEndResult.start).isEqualTo(text.indexOf('c'))
        assertThat(singleSpaceEndResult.end).isEqualTo(text.indexOf('d') + 1)

        // between spaces ("_ | _") where | is the requested offset and _ is the space, will
        // return the exact collapsed range at offset/offset
        val doubleSpaceResult = paragraph.getWordBoundary(text.indexOf('d') + 2)
        assertThat(doubleSpaceResult.start).isEqualTo(text.indexOf('d') + 2)
        assertThat(doubleSpaceResult.end).isEqualTo(text.indexOf('d') + 2)
    }

    @Test
    fun testGetWordBoundary_Bidi() {
        val text = "abc \u05d0\u05d1\u05d2 def"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            )
        )

        val resultEnglish = paragraph.getWordBoundary(text.indexOf('a'))
        val resultHebrew = paragraph.getWordBoundary(text.indexOf('\u05d1'))

        assertThat(resultEnglish.start).isEqualTo(text.indexOf('a'))
        assertThat(resultEnglish.end).isEqualTo(text.indexOf(' '))
        assertThat(resultHebrew.start).isEqualTo(text.indexOf('\u05d0'))
        assertThat(resultHebrew.end).isEqualTo(text.indexOf('\u05d2') + 1)
    }

    @Test
    fun test_finalFontSizeChangesWithDensity() {
        val text = "a"
        val fontSize = 20.sp
        val densityMultiplier = 2f

        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize),
            density = Density(density = 1f, fontScale = 1f)
        )

        val doubleFontSizeParagraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = fontSize),
            density = Density(density = 1f, fontScale = densityMultiplier)
        )

        assertThat(doubleFontSizeParagraph.maxIntrinsicWidth)
            .isEqualTo(paragraph.maxIntrinsicWidth * densityMultiplier)
        assertThat(doubleFontSizeParagraph.height).isEqualTo(paragraph.height * densityMultiplier)
    }

    @Test
    fun minInstrinsicWidth_includes_white_space() {
        with(defaultDensity) {
            val fontSize = 12.sp
            val text = "b "
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )

            val expectedWidth = text.length * fontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun minInstrinsicWidth_returns_longest_word_width() {
        with(defaultDensity) {
            // create words with length 1, 2, 3... 50; and append all with space.
            val maxWordLength = 50
            val text = (1..maxWordLength).fold("") { string, next ->
                string + "a".repeat(next) + " "
            }
            val fontSize = 12.sp
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )

            // +1 is for the white space
            val expectedWidth = (maxWordLength + 1) * fontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun minInstrinsicWidth_withStyledText() {
        with(defaultDensity) {
            val text = "a bb ccc"
            val fontSize = 12.sp
            val styledFontSize = fontSize * 2
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                spanStyles = listOf(
                    AnnotatedString.Range(
                        SpanStyle(fontSize = styledFontSize), "a".length, "a bb ".length
                    )
                )
            )

            val expectedWidth = "bb ".length * styledFontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualTo(expectedWidth)
        }
    }

    @Test(expected = AssertionError::class)
    fun getPathForRange_throws_exception_if_start_larger_than_end() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleParagraph(text = text)

        paragraph.getPathForRange(textEnd, textStart)
    }

    @Test(expected = AssertionError::class)
    fun getPathForRange_throws_exception_if_start_is_smaller_than_zero() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleParagraph(text = text)

        paragraph.getPathForRange(textStart - 2, textEnd - 1)
    }

    @Test(expected = AssertionError::class)
    fun getPathForRange_throws_exception_if_end_is_larger_than_text_length() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleParagraph(text = text)

        paragraph.getPathForRange(textStart, textEnd + 1)
    }

    @Test
    fun createParagraph_with_ParagraphIntrinsics() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 14.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraphIntrinsics = ParagraphIntrinsics(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    fontFamily = fontFamilyMeasureFont
                ),
                spanStyles = listOf(),
                density = defaultDensity,
                fontFamilyResolver = UncachedFontFamilyResolver(context)
            )

            val paragraph = Paragraph(
                paragraphIntrinsics = paragraphIntrinsics,
                constraints = Constraints(maxWidth = (fontSizeInPx * text.length).ceilToInt())
            )

            assertThat(paragraph.maxIntrinsicWidth).isEqualTo(paragraphIntrinsics.maxIntrinsicWidth)
            assertThat(paragraph.width).isEqualTo(fontSizeInPx * text.length)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeMaxLines_throwsException() {
        simpleParagraph(
            text = "",
            maxLines = -1,
            width = Float.MAX_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWidth_throwsException() {
        simpleParagraph(
            text = "",
            width = -1f
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testSolidBrushColorIsSameAsColor() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length

            val paragraphWithColor = simpleParagraph(
                text = text,
                style = TextStyle(color = Color.Red, fontSize = fontSize),
                width = paragraphWidth
            )

            val paragraphWithSolidColor = simpleParagraph(
                text = text,
                style = TextStyle(brush = SolidColor(Color.Red), fontSize = fontSize),
                width = paragraphWidth
            )

            assertThat(paragraphWithColor.bitmap())
                .isEqualToBitmap(paragraphWithSolidColor.bitmap())
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testSpanBrush_overridesDefaultBrush() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * text.length

            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
                ),
                width = paragraphWidth
            )

            val paragraphWithSpan = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
                ),
                spanStyles = listOf(
                    AnnotatedString.Range(
                        item = SpanStyle(
                            brush = Brush.linearGradient(listOf(Color.Yellow, Color.Green))
                        ),
                        start = 0,
                        end = text.length
                    )
                ),
                width = paragraphWidth
            )

            assertThat(paragraph.bitmap())
                .isNotEqualToBitmap(paragraphWithSpan.bitmap())
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testBrush_notEffectedBy_TextDirection() {
        with(defaultDensity) {
            val ltrText = "aaa"
            val rtlText = "\u05D0\u05D0\u05D0"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraphWidth = fontSizeInPx * ltrText.length

            val ltrParagraph = simpleParagraph(
                text = ltrText,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
                ),
                width = paragraphWidth
            )

            val rtlParagraph = simpleParagraph(
                text = rtlText,
                style = TextStyle(
                    fontSize = fontSize,
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
                ),
                width = paragraphWidth
            )

            assertThat(ltrParagraph.bitmap())
                .isNotEqualToBitmap(rtlParagraph.bitmap())

            // Color on the same pixel should be the same since they used the same brush.
            assertThat(ltrParagraph.bitmap().getPixel(50, 50))
                .isEqualTo(rtlParagraph.bitmap().getPixel(50, 50))
        }
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        density: Density? = null,
        width: Float = Float.MAX_VALUE
    ): Paragraph {
        return Paragraph(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont
            ).merge(style),
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = density ?: defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }
}
