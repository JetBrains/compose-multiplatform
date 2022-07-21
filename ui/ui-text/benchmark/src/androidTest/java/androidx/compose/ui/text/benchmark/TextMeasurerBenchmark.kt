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

import android.content.Context
import android.util.TypedValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTextApi::class)
@LargeTest
@RunWith(Parameterized::class)
class TextMeasurerBenchmark(
    private val textLength: Int,
    private val textType: TextType,
    alphabet: Alphabet
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} type={1} alphabet={2}")
        fun initParameters(): List<Array<Any>> = cartesian(
            arrayOf(8, 32, 128, 512),
            arrayOf(TextType.PlainText, TextType.StyledText),
            arrayOf(Alphabet.Latin, Alphabet.Cjk)
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule(alphabet)

    private lateinit var instrumentationContext: Context

    // Width initialized in setup().
    private var width: Int = 0
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            textBenchmarkRule.widthDp,
            instrumentationContext.resources.displayMetrics
        ).roundToInt()
    }

    private fun text(textGenerator: RandomTextGenerator): AnnotatedString {
        val text = textGenerator.nextParagraph(textLength)
        val spanStyles = if (textType == TextType.StyledText) {
            textGenerator.createStyles(text)
        } else {
            listOf()
        }
        return AnnotatedString(text = text, spanStyles = spanStyles)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun text_measurer_no_cache() {
        textBenchmarkRule.generator { textGenerator ->
            val textMeasurer = TextMeasurer(
                fallbackFontFamilyResolver = createFontFamilyResolver(instrumentationContext),
                fallbackDensity = Density(instrumentationContext),
                fallbackLayoutDirection = LayoutDirection.Ltr,
                cacheSize = 0
            )
            val text = text(textGenerator)
            benchmarkRule.measureRepeated {
                textMeasurer.measure(
                    text,
                    style = TextStyle(color = Color.Red, fontSize = fontSize),
                    size = IntSize(width, Int.MAX_VALUE)
                )
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun text_measurer_cached() {
        textBenchmarkRule.generator { textGenerator ->
            val textMeasurer = TextMeasurer(
                fallbackFontFamilyResolver = createFontFamilyResolver(instrumentationContext),
                fallbackDensity = Density(instrumentationContext),
                fallbackLayoutDirection = LayoutDirection.Ltr,
                cacheSize = 16
            )
            val text = text(textGenerator)
            benchmarkRule.measureRepeated {
                textMeasurer.measure(
                    text,
                    style = TextStyle(color = Color.Red, fontSize = fontSize),
                    size = IntSize(width, Int.MAX_VALUE)
                )
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun drawText_TextLayoutResult_no_change() {
        textBenchmarkRule.generator { textGenerator ->
            val textMeasurer = TextMeasurer(
                fallbackFontFamilyResolver = createFontFamilyResolver(instrumentationContext),
                fallbackDensity = Density(instrumentationContext),
                fallbackLayoutDirection = LayoutDirection.Ltr,
                cacheSize = 16
            )
            val textLayoutResult = textMeasurer.measure(
                text(textGenerator),
                style = TextStyle(color = Color.Red, fontSize = fontSize),
                size = IntSize(width, Int.MAX_VALUE)
            )
            val drawScope = CanvasDrawScope()
            val canvas = Canvas(
                ImageBitmap(textLayoutResult.size.width, textLayoutResult.size.height)
            )
            benchmarkRule.measureRepeated {
                drawScope.draw(
                    Density(instrumentationContext),
                    LayoutDirection.Ltr,
                    canvas,
                    textLayoutResult.size.toSize()
                ) {
                    drawText(textLayoutResult)
                }
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun drawText_TextLayoutResult_color_override() {
        textBenchmarkRule.generator { textGenerator ->
            val textMeasurer = TextMeasurer(
                fallbackFontFamilyResolver = createFontFamilyResolver(instrumentationContext),
                fallbackDensity = Density(instrumentationContext),
                fallbackLayoutDirection = LayoutDirection.Ltr,
                cacheSize = 16
            )
            val textLayoutResult = textMeasurer.measure(
                text(textGenerator),
                style = TextStyle(color = Color.Red, fontSize = fontSize),
                size = IntSize(width, Int.MAX_VALUE)
            )
            val drawScope = CanvasDrawScope()
            val canvas = Canvas(
                ImageBitmap(textLayoutResult.size.width, textLayoutResult.size.height)
            )
            benchmarkRule.measureRepeated {
                drawScope.draw(
                    Density(instrumentationContext),
                    LayoutDirection.Ltr,
                    canvas,
                    textLayoutResult.size.toSize()
                ) {
                    drawText(textLayoutResult, color = Color.Blue)
                }
            }
        }
    }
}