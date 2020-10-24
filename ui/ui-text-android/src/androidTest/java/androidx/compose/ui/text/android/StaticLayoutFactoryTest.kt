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
package androidx.compose.ui.text.android

import android.app.Instrumentation
import android.graphics.Typeface
import android.text.Layout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.text.font.test.R
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.floor

@RunWith(AndroidJUnit4::class)
@SmallTest
class StaticLayoutFactoryTest {
    private var sampleFont: Typeface? = null

    @Before
    fun setUp() {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        sampleFont = ResourcesCompat.getFont(instrumentation.context, R.font.sample_font)!!
    }

    @Test
    fun create_withText_returnsGiven() {
        val text = "hello"
        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        assertThat(staticLayout.text).isEqualTo(text)
    }

    @Test
    fun create_withDefaultStartAndEnd_returnsWholeText() {
        val text = "ABCDEF"
        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        // width Int.MAX_VALUE therefore should be only one line
        // getLineStart/End will return the index relative to the input text.
        assertThat(staticLayout.getLineStart(0)).isEqualTo(0)
        assertThat(staticLayout.getLineEnd(0)).isEqualTo(text.length)
    }

    @Test
    fun create_withStartAndEnd_returnsTextInRange() {
        val text = "ABCDEF"
        val start = 2
        val end = 5
        val staticLayout = StaticLayoutFactory.create(
            text = text,
            start = start,
            end = end,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        // width Int.MAX_VALUE therefore should be only one line
        // getLineStart/End will return the index relative to the input text.
        assertThat(staticLayout.getLineStart(0)).isEqualTo(start)
        assertThat(staticLayout.getLineEnd(0)).isEqualTo(end)
    }

    @Test
    fun create_withPaint_returnsGiven() {
        val paint = TextPaint().apply { color = 0xFF00FF00.toInt() }
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = paint,
            width = Int.MAX_VALUE
        )

        assertThat(staticLayout.paint).isEqualTo(paint)
    }

    @Test
    fun create_withWidth_returnsGiven() {
        val width = 200
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = width
        )

        assertThat(staticLayout.width).isEqualTo(width)
    }

    @Test
    fun create_withTextDirection_returnsGiven() {
        val textDir = TextDirectionHeuristics.RTL
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            textDir = textDir
        )

        assertThat(staticLayout.getParagraphDirection(0)).isEqualTo(Layout.DIR_RIGHT_TO_LEFT)
    }

    @Test
    fun create_defaultTextDirection_isFirstStrongLTR() {
        val staticLayoutEmpty = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        assertThat(staticLayoutEmpty.getParagraphDirection(0))
            .isEqualTo(Layout.DIR_LEFT_TO_RIGHT)

        val staticLayoutFirstRTL = StaticLayoutFactory.create(
            text = "\u05D0",
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        assertThat(staticLayoutFirstRTL.getParagraphDirection(0))
            .isEqualTo(Layout.DIR_RIGHT_TO_LEFT)
    }

    @Test
    fun create_withAlign_returnsGiven() {
        val align = Layout.Alignment.ALIGN_OPPOSITE
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            alignment = align
        )

        assertThat(staticLayout.alignment).isEqualTo(align)
    }

    @Test
    fun create_defaultAlign_isAlignNormal() {
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        assertThat(staticLayout.alignment).isEqualTo(Layout.Alignment.ALIGN_NORMAL)
    }

    @Test
    fun create_with_ellipsizeEnd_maxLinesOne_shortText_isNotEllipsized() {
        val text = "abc"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val width = floor(charWidth * text.length).toInt() + 10
        val ellipsizedWidth = width
        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width,
            ellipsize = TextUtils.TruncateAt.END,
            ellipsizedWidth = ellipsizedWidth
        )

        // Ellipsized char in the first line should be zero
        assertThat(staticLayout.getEllipsisCount(0)).isEqualTo(0)
    }

    @Test
    fun create_with_ellipsizeEnd_maxLinesOne_longText_isEllipsized() {
        val text = "abc"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val width = floor(charWidth * text.length).toInt()
        val ellipsizedWidth = width - 1

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width,
            ellipsize = TextUtils.TruncateAt.END,
            ellipsizedWidth = ellipsizedWidth,
            maxLines = 1
        )

        assertThat(staticLayout.getEllipsisCount(0)).isGreaterThan(0)
    }

    @Test
    fun create_withLineSpacingMultiplier_returnsGiven() {
        val lineSpacingMultiplier = 1.5f
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            lineSpacingMultiplier = lineSpacingMultiplier
        )

        assertThat(staticLayout.spacingMultiplier).isEqualTo(lineSpacingMultiplier)
    }

    @Test
    fun create_defaultLineSpacingMultiplier_isOne() {
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        assertThat(staticLayout.spacingMultiplier).isEqualTo(1f)
    }

    @Test
    fun create_withLineSpacingExtra_returnsGiven() {
        val lineSpacingExtra = 10f
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            lineSpacingExtra = lineSpacingExtra
        )

        assertThat(staticLayout.spacingAdd).isEqualTo(lineSpacingExtra)
    }

    @Test
    fun create_defaultLineSpacingExtra_isZero() {
        val staticLayout = StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )

        assertThat(staticLayout.spacingAdd).isZero()
    }

    @Test
    fun create_withJustificationModeNone_isNotJustified() {
        val text = "a b c"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val extra = charWidth / 2
        val width = floor("a b".length * charWidth + extra).toInt()

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width,
            justificationMode = Layout.JUSTIFICATION_MODE_NONE
        )

        // Last line won't be justified, need two lines.
        assertThat(staticLayout.getLineCount()).isGreaterThan(1)
        // The line right is exactly the space needed by "a b"
        assertThat(staticLayout.getLineRight(0)).isEqualTo("a b".length * charWidth)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun create_withJustificationModeInterWord_isJustified() {
        val text = "a b c"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val extra = charWidth / 2
        val width = floor("a b".length * charWidth + extra).toInt()

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width,
            justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
        )

        // Last line won't be justified, need two lines.
        assertThat(staticLayout.getLineCount()).isGreaterThan(1)
        // The line right must be greater than text length when justification is on.
        assertThat(staticLayout.getLineRight(0)).isGreaterThan("a b".length * charWidth)
        // The line right ideally should be width. But before API 28, justification shows an extra
        // space at the end of each line. So we tolerate those cases by make sure light right is
        // bigger than width - sizeOfSpace, where sizeOfSpace equals extra / spaceNum.
        val spaceNum = "a b".split(" ").size - 1
        val lineRightLowerBoundary = width - extra / (spaceNum + 1)
        assertThat(staticLayout.getLineRight(0)).isAtLeast(lineRightLowerBoundary)
    }

    @Test
    fun create_defaultJustificationMode_isNone() {
        val text = "a b c"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val extra = charWidth / 2
        val width = floor("a b".length * charWidth + extra).toInt()

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width
        )

        // Last line won't be justified, need two lines.
        assertThat(staticLayout.getLineCount()).isGreaterThan(1)
        // The line right is exactly the space needed by "a b"
        assertThat(staticLayout.getLineRight(0)).isEqualTo("a b".length * charWidth)
    }

    @Test
    fun create_withIncludePadding_true() {
        val text = "a b c"
        val charWidth = 20f
        val paint = getPaintWithCharWidth(charWidth)

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = Int.MAX_VALUE,
            includePadding = true
        )

        val fontMetrics = paint.fontMetricsInt
        assertThat(staticLayout.height).isEqualTo(fontMetrics.bottom - fontMetrics.top)
    }

    @Test
    fun create_withIncludePadding_false() {
        val text = "a b c"
        val charWidth = 20f
        val paint = getPaintWithCharWidth(charWidth)

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = Int.MAX_VALUE,
            includePadding = false
        )

        val fontMetrics = paint.fontMetricsInt
        assertThat(staticLayout.height).isEqualTo(fontMetrics.descent - fontMetrics.ascent)
    }

    @Test
    fun create_defaultIncludePadding_isTrue() {
        val text = "a b c"
        val charWidth = 20f
        val paint = getPaintWithCharWidth(charWidth)

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = Int.MAX_VALUE
        )

        val fontMetrics = paint.fontMetricsInt
        assertThat(staticLayout.height).isEqualTo(fontMetrics.bottom - fontMetrics.top)
    }

    // Testing of BreakStrategy is non-trivial, only test if it will crash.
    @Test
    fun create_withBreakStrategySimple_notCrash() {
        StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = 0,
            breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        )
    }

    @Test
    fun create_withBreakStrategyHighQuality_notCrash() {
        StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = 0,
            breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
        )
    }

    @Test
    fun create_withBreakStrategyBalanced_notCrash() {
        StaticLayoutFactory.create(
            text = "",
            paint = TextPaint(),
            width = 0,
            breakStrategy = Layout.BREAK_STRATEGY_BALANCED
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun create_withHyphenationFrequencyNone_isNotHyphenated() {
        val text = "writing"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val width = ("writ".length + 2) * charWidth

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width.toInt(),
            hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        )

        assertThat(staticLayout.getLineCount()).isEqualTo(2)
        // If hyphenation is off, "writing" will become "writin" + "\n" +"g".
        // The second line should start with 'g'.
        assertThat(staticLayout.getLineStart(1)).isEqualTo(text.indexOf('g'))
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun create_withHyphenationFrequencyNormal_isHyphenated() {
        val text = "writing"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val width = ("writ".length + 2) * charWidth

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width.toInt(),
            hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NORMAL
        )

        assertThat(staticLayout.getLineCount()).isEqualTo(2)
        // If hyphenation is on, "writing" will become "writ-" + "\n" + "ing".
        // The second line should start with second 'i'.
        assertThat(staticLayout.getLineStart(1)).isEqualTo("writ".length)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun create_withHyphenationFrequencyFull_isHyphenated() {
        val text = "writing"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val width = ("writ".length + 2) * charWidth

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width.toInt(),
            hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_FULL
        )

        assertThat(staticLayout.getLineCount()).isEqualTo(2)
        // If hyphenation is on, "writing" will become "writ-" + "\n" + "ing".
        // The second line should start with second 'i'.
        assertThat(staticLayout.getLineStart(1)).isEqualTo("writ".length)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun create_defaultHyphenationFrequency_isNone() {
        val text = "writing"
        val charWidth = 20.0f
        val paint = getPaintWithCharWidth(charWidth)

        val width = ("writ".length + 2) * charWidth

        val staticLayout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width.toInt()
        )

        assertThat(staticLayout.getLineCount()).isEqualTo(2)
        // If hyphenation is off, "writing" will become "writin" + "\n" +"g".
        // The second line should start with 'g'.
        assertThat(staticLayout.getLineStart(1)).isEqualTo(text.indexOf('g'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withStartNegative_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            start = -1,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withStartGreaterThanLength_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            start = "abc".length + 1,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withEndNegative_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            end = -1,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withEndGreaterThanLength_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            end = "abc".length + 1,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withStartGreaterThanEnd_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            start = 2,
            end = 1,
            paint = TextPaint(),
            width = Int.MAX_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withMaxLinesNegative_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            maxLines = -1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withWidthNegative_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            paint = TextPaint(),
            width = -1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withEllipsizedWidthNegative_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            ellipsizedWidth = -1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withLineSpacingMultiplierNegative_throwsIAE() {
        StaticLayoutFactory.create(
            text = "abc",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            lineSpacingMultiplier = -1f
        )
    }

    @Test
    fun create_withLineSpacingExtraNegative_notCrash() {
        StaticLayoutFactory.create(
            text = "abc",
            paint = TextPaint(),
            width = Int.MAX_VALUE,
            lineSpacingExtra = -1f
        )
    }

    fun getPaintWithCharWidth(width: Float) = TextPaint().apply {
        textSize = width
        typeface = sampleFont
    }
}