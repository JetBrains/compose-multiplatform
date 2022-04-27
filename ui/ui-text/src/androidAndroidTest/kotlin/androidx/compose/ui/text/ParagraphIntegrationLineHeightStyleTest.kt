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

import android.graphics.Paint.FontMetricsInt
import androidx.compose.ui.text.android.style.lineHeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.platform.AndroidParagraph
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlin.math.ceil

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalTextApi::class)
class ParagraphIntegrationLineHeightStyleTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val fontSize = 10.sp
    private val lineHeight = 20.sp
    private val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
    private val lineHeightInPx = with(defaultDensity) { lineHeight.toPx() }

    /* single line even */

    @Test
    fun singleLine_even_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_even_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_even_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_even_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    /* single line top */

    @Test
    fun singleLine_top_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_top_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(defaultFontMetrics.descent)
        }
    }

    @Test
    fun singleLine_top_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_top_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    /* single line bottom */

    @Test
    fun singleLine_bottom_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_bottom_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent - diff)
            assertThat(getLineDescent(0)).isEqualTo(defaultFontMetrics.descent)
        }
    }

    @Test
    fun singleLine_bottom_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_bottom_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    /* single line proportional */

    @Test
    fun singleLine_proportional_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_proportional_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - descentDiff)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_proportional_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - ascentDiff)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    @Test
    fun singleLine_proportional_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)
        }
    }

    /* multi line even */

    @Test
    fun multiLine_even_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            for (line in 0 until lineCount) {
                assertThat(getLineHeight(line)).isEqualTo(lineHeightInPx)
                assertThat(getLineAscent(line)).isEqualTo(expectedAscent)
                assertThat(getLineDescent(line)).isEqualTo(expectedDescent)
            }

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_even_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_even_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_even_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    /* multi line top */

    @Test
    fun multiLine_top_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_top_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_top_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_top_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    /* multi line bottom */

    @Test
    fun multiLine_bottom_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_bottom_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_bottom_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_bottom_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    /* multi line proportional */

    @Test
    fun multiLine_proportional_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_proportional_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(0)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx - descentDiff)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_proportional_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - ascentDiff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_proportional_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isEqualTo(lineHeightInPx - ascentDiff)
            assertThat(getLineAscent(0)).isEqualTo(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(1)).isEqualTo(lineHeightInPx)
            assertThat(getLineAscent(1)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(1)).isEqualTo(expectedDescent)

            assertThat(getLineHeight(2)).isEqualTo(lineHeightInPx - descentDiff)
            assertThat(getLineAscent(2)).isEqualTo(expectedAscent)
            assertThat(getLineDescent(2)).isEqualTo(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isEqualTo(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isEqualTo(lineHeightInPx)
        }
    }

    private fun singleLineParagraph(
        lineHeightTrim: Trim,
        lineHeightAlignment: Alignment,
    ): AndroidParagraph {
        val text = "AAA"
        val textStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                trim = lineHeightTrim,
                alignment = lineHeightAlignment
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            style = textStyle,
            width = text.length * fontSizeInPx
        ) as AndroidParagraph

        assertThat(paragraph.lineCount).isEqualTo(1)

        return paragraph
    }

    @Suppress("DEPRECATION")
    private fun multiLineParagraph(
        lineHeightTrim: Trim,
        lineHeightAlignment: Alignment,
    ): AndroidParagraph {
        val lineCount = 3
        val word = "AAA"
        val text = "AAA".repeat(lineCount)

        val textStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                trim = lineHeightTrim,
                alignment = lineHeightAlignment
            ),
            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                includeFontPadding = false
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            style = textStyle,
            width = word.length * fontSizeInPx
        ) as AndroidParagraph

        assertThat(paragraph.lineCount).isEqualTo(lineCount)

        return paragraph
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        width: Float = Float.MAX_VALUE
    ): Paragraph {
        return Paragraph(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                lineHeight = lineHeight,
                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                    includeFontPadding = false
                )
            ).merge(style),
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    private fun defaultFontMetrics(): FontMetricsInt {
        return (simpleParagraph() as AndroidParagraph).paragraphIntrinsics.textPaint.fontMetricsInt
    }

    private fun proportionalDescentDiff(fontMetrics: FontMetricsInt): Int {
        val ascent = abs(fontMetrics.ascent.toFloat())
        val ascentRatio = ascent / fontMetrics.lineHeight()
        return ceil(fontMetrics.lineHeight() * (1f - ascentRatio)).toInt()
    }
}