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

package androidx.compose.foundation.text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@Suppress("DEPRECATION")
@RunWith(Parameterized::class)
@MediumTest
class TextStyleInvalidationTest(private val config: Config) {

    class Config(
        private val description: String,
        val updateStyle: (TextStyle) -> TextStyle,
        val initializeStyle: (TextStyle) -> TextStyle = { it },
        val invalidatesMeasure: Boolean = false,
        val invalidatesPlacement: Boolean = false,
        val invalidatesDraw: Boolean = false,
    ) {
        override fun toString(): String = buildString {
            append(description)
            listOfNotNull(
                "measure".takeIf { invalidatesMeasure },
                "placement".takeIf { invalidatesPlacement },
                "draw".takeIf { invalidatesDraw },
            ).joinTo(this, prefix = " ", separator = ", ") { "invalidates $it" }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    companion object {
        @Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            Config("nothing", { it }),
            Config(
                "color",
                { it.copy(color = Color.Blue) },
                invalidatesDraw = true,
            ),
            Config(
                "to brush",
                { it.copy(brush = Brush.verticalGradient(0f to Color.Blue, 1f to Color.Magenta)) },
                invalidatesDraw = true,
            ),
            Config(
                "from brush to brush",
                initializeStyle = {
                    it.copy(brush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Magenta))
                },
                updateStyle = {
                    it.copy(brush = Brush.verticalGradient(0f to Color.Blue, 1f to Color.Magenta))
                },
                invalidatesDraw = true,
            ),
            Config(
                "alpha",
                initializeStyle = {
                    it.copy(
                        alpha = 1f,
                        brush = Brush.verticalGradient(0f to Color.Blue, 1f to Color.Magenta)
                    )
                },
                updateStyle = { it.copy(alpha = 0.5f, brush = it.brush) },
                invalidatesDraw = true,
            ),
            Config(
                "fontSize",
                { it.copy(fontSize = it.fontSize * 2) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "fontWeight",
                { it.copy(fontWeight = FontWeight(it.fontWeight!!.weight * 2)) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "fontStyle",
                { it.copy(fontStyle = FontStyle.Italic) },
                invalidatesMeasure = true,
                invalidatesDraw = true
            ),
            Config(
                "fontSynthesis",
                { it.copy(fontSynthesis = FontSynthesis.All) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "fontFamily",
                { it.copy(fontFamily = FontFamily.Cursive) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "fontFeatureSettings",
                initializeStyle = { it.copy(fontFeatureSettings = "a") },
                updateStyle = { it.copy(fontFeatureSettings = "b") },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "letterSpacing",
                { it.copy(letterSpacing = it.letterSpacing * 2) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "baselineShift",
                { it.copy(baselineShift = BaselineShift.Superscript) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "textGeometricTransform",
                { it.copy(textGeometricTransform = TextGeometricTransform(scaleX = 2f)) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "localeList",
                initializeStyle = { it.copy(localeList = LocaleList("en-US")) },
                updateStyle = { it.copy(localeList = LocaleList("en-GB")) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "background",
                { it.copy(background = Color.Blue) },
                invalidatesDraw = true,
            ),
            Config(
                "textDecoration",
                { it.copy(textDecoration = TextDecoration.LineThrough) },
                invalidatesDraw = true,
            ),
            Config(
                "to shadow",
                { it.copy(shadow = Shadow(Color.Black, blurRadius = 4f)) },
                invalidatesDraw = true,
            ),
            Config(
                "from shadow to shadow",
                initializeStyle = { it.copy(shadow = Shadow(Color.Black, blurRadius = 1f)) },
                updateStyle = { it.copy(shadow = Shadow(Color.Black, blurRadius = 4f)) },
                invalidatesDraw = true,
            ),
            Config(
                "to drawStyle",
                { it.copy(drawStyle = Stroke(width = 1f)) },
                invalidatesDraw = true,
            ),
            Config(
                "from drawStyle to drawStyle",
                initializeStyle = { it.copy(drawStyle = Stroke(width = 0f)) },
                updateStyle = { it.copy(drawStyle = Stroke(width = 1f)) },
                invalidatesDraw = true,
            ),
            Config(
                "textAlign",
                { it.copy(textAlign = TextAlign.Justify) },
                invalidatesDraw = true,
            ),
            Config(
                "textDirection",
                { it.copy(textDirection = TextDirection.Rtl) },
                invalidatesDraw = true,
            ),
            Config(
                "lineHeight",
                { it.copy(lineHeight = it.lineHeight * 2) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "textIndent",
                { it.copy(textIndent = TextIndent(firstLine = 5.sp)) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "platformStyle",
                initializeStyle = {
                    it.copy(platformStyle = PlatformTextStyle(includeFontPadding = true))
                },
                updateStyle = {
                    it.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
                },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "lineHeightStyle",
                {
                    it.copy(
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.FirstLineTop
                        )
                    )
                },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "lineBreak",
                { it.copy(lineBreak = LineBreak.Heading) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
            Config(
                "hyphens",
                { it.copy(hyphens = Hyphens.Auto) },
                invalidatesMeasure = true,
                invalidatesDraw = true,
            ),
        )
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun changing() {
        // Don't leave any TextUnits Unspecified so test cases can double them to invalidate.
        var style by mutableStateOf(
            TextStyle(
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = null,
                fontSynthesis = null,
                fontFamily = TEST_FONT_FAMILY,
                fontFeatureSettings = null,
                letterSpacing = 12.sp,
                baselineShift = null,
                textGeometricTransform = null,
                localeList = null,
                background = Color.White,
                textDecoration = null,
                shadow = null,
                textAlign = TextAlign.Start,
                textDirection = TextDirection.Ltr,
                lineHeight = 12.sp,
                textIndent = null,
            ).let(config.initializeStyle)
        )
        var measures = 0
        var placements = 0
        var draws = 0

        rule.setContent {
            BasicText(
                "a",
                style = style,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        measures++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placements++
                            placeable.place(IntOffset.Zero)
                        }
                    }
                    .drawBehind {
                        draws++
                    }
            )
        }

        rule.waitForIdle()
        val initialMeasures = measures
        val initialPlacements = placements
        val initialDraws = draws

        style = config.updateStyle(style)

        rule.runOnIdle {
            if (config.invalidatesMeasure) {
                assertThat(measures).isGreaterThan(initialMeasures)
            }
            if (config.invalidatesPlacement) {
                assertThat(placements).isGreaterThan(initialPlacements)

                // If measure is invalidated, placement will also always be invalidated, so ensure
                // that placement was also invalidated separately from measurement.
                if (config.invalidatesMeasure) {
                    assertThat(placements).isGreaterThan(measures)
                }
            }
            if (config.invalidatesDraw) {
                assertThat(draws).isGreaterThan(initialDraws)
            }
        }
    }
}