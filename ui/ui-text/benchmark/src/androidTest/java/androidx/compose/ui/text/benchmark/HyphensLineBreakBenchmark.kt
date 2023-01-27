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

package androidx.compose.ui.text.benchmark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.text.TextPaint
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.LayoutCompat
import androidx.compose.ui.text.android.TextLayout
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalTextApi::class, InternalPlatformTextApi::class)
class HyphensLineBreakBenchmark(
    private val textLength: Int,
    private val hyphensString: String,
    private val lineBreakString: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} hyphens={1} lineBreak={2}")
        fun initParameters(): List<Array<Any?>> {
            return cartesian(
                arrayOf(32, 128, 512),
                arrayOf(
                    Hyphens.None.toTestString(),
                    Hyphens.Auto.toTestString()
                ),
                arrayOf(
                    LineBreak.Paragraph.toTestString(),
                    LineBreak.Simple.toTestString(),
                    LineBreak.Heading.toTestString()
                )
            )
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule()

    private val width = 100
    private val textSize: Float = 10F
    private val hyphenationFrequency = toLayoutHyphenationFrequency(hyphensString.toHyphens())
    private val lineBreakStyle = toLayoutLineBreakStyle(lineBreakString.toLineBreak().strictness)
    private val breakStrategy = toLayoutBreakStrategy(lineBreakString.toLineBreak().strategy)
    private val lineBreakWordStyle =
        toLayoutLineBreakWordStyle(lineBreakString.toLineBreak().wordBreak)

    @Test
    fun constructLayout() {
        textBenchmarkRule.generator { textGenerator ->
            val text = textGenerator.nextParagraph(textLength)
            val textPaint = TextPaint()
            textPaint.textSize = textSize
            benchmarkRule.measureRepeated {
                TextLayout.constructStaticLayout(text, width = width,
                    textPaint = textPaint,
                    hyphenationFrequency = hyphenationFrequency,
                    lineBreakStyle = lineBreakStyle,
                    breakStrategy = breakStrategy,
                    lineBreakWordStyle = lineBreakWordStyle
                )
            }
        }
    }

    @Test
    fun constructLayoutDraw() {
        textBenchmarkRule.generator { textGenerator ->
            val text = textGenerator.nextParagraph(textLength)
            val textPaint = TextPaint()
            textPaint.textSize = textSize
            val canvas = Canvas(Bitmap.createBitmap(width, 1000, Bitmap.Config.ARGB_8888))
            benchmarkRule.measureRepeated {
                val layout = TextLayout.constructStaticLayout(text, width = width,
                    textPaint = textPaint,
                    hyphenationFrequency = hyphenationFrequency,
                    lineBreakStyle = lineBreakStyle,
                    breakStrategy = breakStrategy,
                    lineBreakWordStyle = lineBreakWordStyle
                )
                layout.draw(canvas)
            }
        }
    }

    private fun toLayoutHyphenationFrequency(hyphens: Hyphens?): Int = when (hyphens) {
        Hyphens.Auto -> if (Build.VERSION.SDK_INT <= 32) {
            LayoutCompat.HYPHENATION_FREQUENCY_NORMAL
        } else {
            LayoutCompat.HYPHENATION_FREQUENCY_NORMAL_FAST
        }
        Hyphens.None -> LayoutCompat.HYPHENATION_FREQUENCY_NONE
        else -> LayoutCompat.HYPHENATION_FREQUENCY_NONE
    }

    private fun toLayoutBreakStrategy(breakStrategy: LineBreak.Strategy?): Int =
        when (breakStrategy) {
            LineBreak.Strategy.Simple -> LayoutCompat.BREAK_STRATEGY_SIMPLE
            LineBreak.Strategy.HighQuality -> LayoutCompat.BREAK_STRATEGY_HIGH_QUALITY
            LineBreak.Strategy.Balanced -> LayoutCompat.BREAK_STRATEGY_BALANCED
            else -> LayoutCompat.BREAK_STRATEGY_SIMPLE
        }

    private fun toLayoutLineBreakStyle(lineBreakStrictness: LineBreak.Strictness?): Int =
        when (lineBreakStrictness) {
            LineBreak.Strictness.Default -> LayoutCompat.LINE_BREAK_STYLE_NONE
            LineBreak.Strictness.Loose -> LayoutCompat.LINE_BREAK_STYLE_LOOSE
            LineBreak.Strictness.Normal -> LayoutCompat.LINE_BREAK_STYLE_NORMAL
            LineBreak.Strictness.Strict -> LayoutCompat.LINE_BREAK_STYLE_STRICT
            else -> LayoutCompat.LINE_BREAK_STYLE_NONE
        }

    private fun toLayoutLineBreakWordStyle(lineBreakWordStyle: LineBreak.WordBreak?): Int =
        when (lineBreakWordStyle) {
            LineBreak.WordBreak.Default -> LayoutCompat.LINE_BREAK_WORD_STYLE_NONE
            LineBreak.WordBreak.Phrase -> LayoutCompat.LINE_BREAK_WORD_STYLE_PHRASE
            else -> LayoutCompat.LINE_BREAK_WORD_STYLE_NONE
        }
}

/**
 * Required to make this test work due to a bug with value classes and Parameterized JUnit tests.
 * https://youtrack.jetbrains.com/issue/KT-35523
 *
 * However, it's not enough to use a wrapper because wrapper makes the test name unnecessarily
 * long which causes Perfetto to be unable to create output files with a very long name in some
 * file systems.
 *
 * Using a String instead of an Integer gives us a better test naming.
 */
private fun String.toLineBreak(): LineBreak = when (this) {
    "Simple" -> LineBreak.Simple
    "Heading" -> LineBreak.Heading
    "Paragraph" -> LineBreak.Paragraph
    else -> throw IllegalArgumentException("Unrecognized LineBreak value for this test")
}

private fun LineBreak.toTestString(): String = when (this) {
    LineBreak.Simple -> "Simple"
    LineBreak.Heading -> "Heading"
    LineBreak.Paragraph -> "Paragraph"
    else -> throw IllegalArgumentException("Unrecognized LineBreak value for this test")
}

private fun String.toHyphens(): Hyphens = when (this) {
    "None" -> Hyphens.None
    "Auto" -> Hyphens.Auto
    else -> throw IllegalArgumentException("Unrecognized Hyphens value for this test")
}

private fun Hyphens.toTestString(): String = when (this) {
    Hyphens.None -> "None"
    Hyphens.Auto -> "Auto"
    else -> throw IllegalArgumentException("Unrecognized Hyphens value for this test")
}