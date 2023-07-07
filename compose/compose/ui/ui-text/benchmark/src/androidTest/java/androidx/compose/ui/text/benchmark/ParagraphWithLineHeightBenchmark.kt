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

package androidx.compose.ui.text.benchmark

import android.content.Context
import android.util.TypedValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.ceil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ParagraphWithLineHeightBenchmark(
    private val textLength: Int,
    private val addNewLine: Boolean,
    private val applyLineHeight: Boolean,
    private val lineHeightStyle: LineHeightStyle?
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "length={0} newLine={1} applyLineHeight={2} lineHeightStyle={3}"
        )
        fun initParameters(): List<Array<Any?>> = cartesian(
            arrayOf(16),
            // add new line
            arrayOf(true),
            // apply line height
            arrayOf(false, true),
            arrayOf(LineHeightStyle.Default)
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule(Alphabet.Latin)

    private lateinit var instrumentationContext: Context

    // Width initialized in setup().
    private var width: Float = 0f
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            textBenchmarkRule.widthDp,
            instrumentationContext.resources.displayMetrics
        )
    }

    private fun text(textGenerator: RandomTextGenerator): String {
        return textGenerator.nextParagraph(textLength) + if (addNewLine) "\n" else ""
    }

    private fun paragraph(
        text: String,
        width: Float
    ): Paragraph {
        return Paragraph(
            paragraphIntrinsics = paragraphIntrinsics(text),
            constraints = Constraints(maxWidth = ceil(width).toInt())
        )
    }

    private fun paragraphIntrinsics(text: String): ParagraphIntrinsics {
        @Suppress("DEPRECATION")
        val style = if (applyLineHeight) {
            TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize * 2,
                lineHeightStyle = lineHeightStyle,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        } else {
            TextStyle(
                fontSize = fontSize,
                lineHeightStyle = lineHeightStyle,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        }

        return ParagraphIntrinsics(
            text = text,
            density = Density(density = instrumentationContext.resources.displayMetrics.density),
            style = style,
            fontFamilyResolver = createFontFamilyResolver(instrumentationContext)
        )
    }

    @Test
    fun construct() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val text = runWithTimingDisabled {
                    // create a new paragraph and use a smaller width to get
                    // some line breaking in the result
                    text(textGenerator)
                }

                paragraph(text = text, width = width)
            }
        }
    }
}