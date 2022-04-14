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

import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphIntrinsicIntegrationTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)

    @Test
    fun maxIntrinsicWidth_empty_string_is_zero() {
        val paragraphIntrinsics = paragraphIntrinsics(text = "")

        assertThat(paragraphIntrinsics.maxIntrinsicWidth).isZero()
    }

    @Test
    fun maxIntrinsicWidth_with_rtl_string() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp

            val paragraphIntrinsics = paragraphIntrinsics(
                text = text,
                fontSize = fontSize
            )

            assertThat(paragraphIntrinsics.maxIntrinsicWidth)
                .isEqualTo(text.length * fontSize.toPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_with_ltr_string() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp

            val paragraphIntrinsics = paragraphIntrinsics(
                text = text,
                fontSize = fontSize
            )

            assertThat(paragraphIntrinsics.maxIntrinsicWidth)
                .isEqualTo(text.length * fontSize.toPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_with_line_feed() {
        with(defaultDensity) {
            val text = "abc\nabc"
            val fontSize = 50.sp
            val paragraphIntrinsics = paragraphIntrinsics(
                text = text,
                fontSize = fontSize
            )

            assertThat(paragraphIntrinsics.maxIntrinsicWidth)
                .isEqualTo(text.indexOf('\n') * fontSize.toPx())
        }
    }

    @Test
    fun maxInstrinsicWidth_withStyledText() {
        with(defaultDensity) {
            val text = "a bb ccc"
            val fontSize = 12.sp
            val styledFontSize = 24.sp
            val paragraph = paragraphIntrinsics(
                text = text,
                fontSize = fontSize,
                spanStyles = listOf(
                    Range(
                        item = SpanStyle(fontSize = styledFontSize),
                        start = "a ".length,
                        end = "a bb ".length
                    )
                )
            )

            // since "bb " is double font size, the whole width should be the text size, and the
            // additional width resulting from the "bb " length.
            val expectedWidth = (text.length + "bb ".length) * fontSize.toPx()
            assertThat(paragraph.maxIntrinsicWidth).isEqualTo(expectedWidth)
        }
    }

    @Test
    fun minIntrinsicWidth_empty_string_is_zero() {
        val paragraphIntrinsics = paragraphIntrinsics(text = "")

        assertThat(paragraphIntrinsics.minIntrinsicWidth).isZero()
    }

    @Test
    fun minIntrinsicWidth_with_rtl_string() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp

            val paragraphIntrinsics = paragraphIntrinsics(
                text = text,
                fontSize = fontSize
            )

            assertThat(paragraphIntrinsics.minIntrinsicWidth)
                .isEqualTo(text.length * fontSize.toPx())
        }
    }

    @Test
    fun minIntrinsicWidth_with_ltr_string() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp

            val paragraphIntrinsics = paragraphIntrinsics(
                text = text,
                fontSize = fontSize
            )

            assertThat(paragraphIntrinsics.minIntrinsicWidth)
                .isEqualTo(text.length * fontSize.toPx())
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
            val paragraph = paragraphIntrinsics(
                text = text,
                fontSize = fontSize
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
            val styledFontSize = 24.sp
            val paragraph = paragraphIntrinsics(
                text = text,
                fontSize = fontSize,
                spanStyles = listOf(
                    Range(
                        item = SpanStyle(fontSize = styledFontSize),
                        start = "a".length,
                        end = "a bb ".length
                    )
                )
            )

            val expectedWidth = "bb ".length * styledFontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualTo(expectedWidth)
        }
    }

    private fun paragraphIntrinsics(
        text: String = "",
        style: TextStyle? = null,
        fontSize: TextUnit = 14.sp,
        spanStyles: List<Range<SpanStyle>> = listOf()
    ): ParagraphIntrinsics {
        return ParagraphIntrinsics(
            text = text,
            spanStyles = spanStyles,
            placeholders = listOf(),
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize
            ).merge(style),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }
}