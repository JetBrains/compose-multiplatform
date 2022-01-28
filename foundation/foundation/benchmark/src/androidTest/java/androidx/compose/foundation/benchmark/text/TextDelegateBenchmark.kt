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

package androidx.compose.foundation.benchmark.text

import android.content.Context
import android.util.TypedValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.benchmark.RandomTextGenerator
import androidx.compose.ui.text.benchmark.TextBenchmarkTestRule
import androidx.compose.ui.text.benchmark.cartesian
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.roundToInt

@OptIn(InternalFoundationTextApi::class)
@LargeTest
@RunWith(Parameterized::class)
class TextDelegateBenchmark(
    private val textLength: Int,
    private val styleCount: Int
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "length={0} styleCount={1}"
        )
        fun initParameters() =
            cartesian(
                arrayOf(32, 512),
                arrayOf(0, 32)
            )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkTestRule = TextBenchmarkTestRule()

    private val layoutDirection = LayoutDirection.Ltr

    lateinit var instrumentationContext: Context
    // Width is initialized in setup().
    private var width: Int = 0
    private val fontSize = textBenchmarkTestRule.fontSizeSp.sp
    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            textBenchmarkTestRule.widthDp,
            instrumentationContext.resources.displayMetrics
        ).roundToInt()
    }

    @OptIn(ExperimentalTextApi::class)
    private fun textDelegate(textGenerator: RandomTextGenerator): TextDelegate {
        val text = textGenerator.nextAnnotatedString(
            length = textLength,
            styleCount = styleCount,
            hasMetricAffectingStyle = true
        )

        return TextDelegate(
            text = text,
            density = Density(density = instrumentationContext.resources.displayMetrics.density),
            style = TextStyle(fontSize = fontSize),
            fontFamilyResolver = createFontFamilyResolver(instrumentationContext)
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun constructor() {
        textBenchmarkTestRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val text = runWithTimingDisabled {
                    textGenerator.nextAnnotatedString(
                        length = textLength,
                        styleCount = styleCount,
                        hasMetricAffectingStyle = true
                    )
                }

                TextDelegate(
                    text = text,
                    density = Density(density = 1f),
                    style = TextStyle(fontSize = fontSize),
                    fontFamilyResolver = createFontFamilyResolver(instrumentationContext)
                )
            }
        }
    }

    /**
     * Measure the time taken by the first time layout of TextDelegate.
     */
    @Test
    fun first_layout() {
        textBenchmarkTestRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val textDelegate = runWithTimingDisabled {
                    textDelegate(textGenerator)
                }
                textDelegate.layout(Constraints(maxWidth = width), layoutDirection)
            }
        }
    }

    /**
     * Measure the time taken by re-layout text with different constraints.
     */
    @Test
    fun layout() {
        textBenchmarkTestRule.generator { textGenerator ->
            val textDelegate = textDelegate(textGenerator)

            val offset = 20
            var sign = 1
            benchmarkRule.measureRepeated {
                val maxWidth = width + sign * offset
                sign *= -1
                textDelegate.layout(Constraints(maxWidth = maxWidth), layoutDirection)
            }
        }
    }

    /**
     * Measure the time taken by the first time painting a TextDelegate on Canvas.
     */
    @Test
    fun first_paint() {
        textBenchmarkTestRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val (canvas, layoutResult) = runWithTimingDisabled {
                    textDelegate(textGenerator).let {
                        val layoutResult = it.layout(
                            Constraints(maxWidth = width),
                            layoutDirection
                        )
                        val canvas = Canvas(
                            ImageBitmap(
                                layoutResult.size.width,
                                layoutResult.size.height
                            )
                        )
                        Pair(canvas, layoutResult)
                    }
                }
                TextDelegate.paint(canvas, layoutResult)
            }
        }
    }

    /**
     * Measure the time taken by painting a TextDelegate on Canvas.
     */
    @Test
    fun paint() {
        textBenchmarkTestRule.generator { textGenerator ->
            val textDelegate = textDelegate(textGenerator)
            val layoutResult = textDelegate.layout(
                Constraints(maxWidth = width),
                layoutDirection
            )
            val canvas = Canvas(
                ImageBitmap(layoutResult.size.width, layoutResult.size.height)
            )

            benchmarkRule.measureRepeated {
                TextDelegate.paint(canvas, layoutResult)
            }
        }
    }
}
