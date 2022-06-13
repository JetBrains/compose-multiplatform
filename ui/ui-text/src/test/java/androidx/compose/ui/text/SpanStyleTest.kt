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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpanStyleTest {
    @Test
    fun `constructor with default values`() {
        val style = SpanStyle()

        assertThat(style.brush).isNull()
        assertThat(style.color).isEqualTo(Color.Unspecified)
        assertThat(style.fontSize.isUnspecified).isTrue()
        assertThat(style.fontWeight).isNull()
        assertThat(style.fontStyle).isNull()
        assertThat(style.letterSpacing.isUnspecified).isTrue()
        assertThat(style.localeList).isNull()
        assertThat(style.background).isEqualTo(Color.Unspecified)
        assertThat(style.textDecoration).isNull()
        assertThat(style.fontFamily).isNull()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `constructor with customized brush`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush)

        assertThat(style.brush).isEqualTo(brush)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `constructor with customized brush and alpha`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush, alpha = 0.3f)

        assertThat(style.brush).isEqualTo(brush)
        assertThat(style.alpha).isEqualTo(0.3f)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `constructor with gradient brush has unspecified color`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush)

        assertThat(style.color).isEqualTo(Color.Unspecified)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `constructor with SolidColor converts to regular color`() {
        val brush = SolidColor(Color.Red)

        val style = SpanStyle(brush = brush)

        assertThat(style.color).isEqualTo(Color.Red)
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color.Red

        val style = SpanStyle(color = color)

        assertThat(style.color).isEqualTo(color)
    }

    @Test
    fun `constructor with half-transparent color`() {
        val color = Color.Red.copy(alpha = 0.5f)

        val style = SpanStyle(color = color)

        assertThat(style.color).isEqualTo(color)
        assertThat(style.alpha).isWithin(1f / 256f).of(0.5f)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val style = SpanStyle(fontSize = fontSize)

        assertThat(style.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `constructor with customized fontWeight`() {
        val fontWeight = FontWeight.W500

        val style = SpanStyle(fontWeight = fontWeight)

        assertThat(style.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `constructor with customized fontStyle`() {
        val fontStyle = FontStyle.Italic

        val style = SpanStyle(fontStyle = fontStyle)

        assertThat(style.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `constructor with customized letterSpacing`() {
        val letterSpacing = 1.em

        val style = SpanStyle(letterSpacing = letterSpacing)

        assertThat(style.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `constructor with customized baselineShift`() {
        val baselineShift = BaselineShift.Superscript

        val style = SpanStyle(baselineShift = baselineShift)

        assertThat(style.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `constructor with customized locale`() {
        val localeList = LocaleList("en-US")

        val style = SpanStyle(localeList = localeList)

        assertThat(style.localeList).isEqualTo(localeList)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color.Red

        val style = SpanStyle(background = color)

        assertThat(style.background).isEqualTo(color)
    }

    @Test
    fun `constructor with customized textDecoration`() {
        val decoration = TextDecoration.Underline

        val style = SpanStyle(textDecoration = decoration)

        assertThat(style.textDecoration).isEqualTo(decoration)
    }

    @Test
    fun `constructor with customized fontFamily`() {
        val fontFamily = FontFamily.SansSerif

        val style = SpanStyle(fontFamily = fontFamily)

        assertThat(style.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `merge with empty other should return this`() {
        val style = SpanStyle()

        val newSpanStyle = style.merge()

        assertThat(newSpanStyle).isEqualTo(style)
    }

    @Test
    fun `merge with other's color is null should use this color`() {
        val style = SpanStyle(color = Color.Red)

        val newSpanStyle = style.merge(SpanStyle(color = Color.Unspecified))

        assertThat(newSpanStyle.color).isEqualTo(style.color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val style = SpanStyle(color = Color.Red)
        val otherStyle = SpanStyle(color = Color.Green)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.color).isEqualTo(otherStyle.color)
    }

    @Test
    fun `merge with other's fontFamily is unspecified should use this' fontFamily`() {
        val style = SpanStyle(fontFamily = FontFamily.SansSerif)

        val newSpanStyle = style.merge(SpanStyle(fontFamily = null))

        assertThat(newSpanStyle.fontFamily).isEqualTo(style.fontFamily)
    }

    @Test
    fun `merge with other's fontFamily is set should use other's fontFamily`() {
        val style = SpanStyle(fontFamily = FontFamily.SansSerif)
        val otherStyle = SpanStyle(fontFamily = FontFamily.Serif)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontFamily).isEqualTo(otherStyle.fontFamily)
    }

    @Test
    fun `merge with other's fontSize is null should use this' fontSize`() {
        val style = SpanStyle(fontSize = 3.5.sp)

        val newSpanStyle = style.merge(SpanStyle(fontSize = TextUnit.Unspecified))

        assertThat(newSpanStyle.fontSize).isEqualTo(style.fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val style = SpanStyle(fontSize = 3.5.sp)
        val otherStyle = SpanStyle(fontSize = 8.7.sp)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontSize).isEqualTo(otherStyle.fontSize)
    }

    @Test
    fun `merge with other's fontWeight is null should use this' fontWeight`() {
        val style = SpanStyle(fontWeight = FontWeight.W300)

        val newSpanStyle = style.merge(SpanStyle(fontWeight = null))

        assertThat(newSpanStyle.fontWeight).isEqualTo(style.fontWeight)
    }

    @Test
    fun `merge with other's fontWeight is set should use other's fontWeight`() {
        val style = SpanStyle(fontWeight = FontWeight.W300)
        val otherStyle = SpanStyle(fontWeight = FontWeight.W500)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontWeight).isEqualTo(otherStyle.fontWeight)
    }

    @Test
    fun `merge with other's fontStyle is null should use this' fontStyle`() {
        val style = SpanStyle(fontStyle = FontStyle.Italic)

        val newSpanStyle = style.merge(SpanStyle(fontStyle = null))

        assertThat(newSpanStyle.fontStyle).isEqualTo(style.fontStyle)
    }

    @Test
    fun `merge with other's fontStyle is set should use other's fontStyle`() {
        val style = SpanStyle(fontStyle = FontStyle.Normal)
        val otherStyle = SpanStyle(fontStyle = FontStyle.Italic)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontStyle).isEqualTo(otherStyle.fontStyle)
    }

    @Test
    fun `merge with other's fontSynthesis is null should use this' fontSynthesis`() {
        val style = SpanStyle(fontSynthesis = FontSynthesis.Style)

        val newSpanStyle = style.merge(SpanStyle(fontSynthesis = null))

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(style.fontSynthesis)
    }

    @Test
    fun `merge with other's fontSynthesis is set should use other's fontSynthesis`() {
        val style = SpanStyle(fontSynthesis = FontSynthesis.Style)
        val otherStyle = SpanStyle(fontSynthesis = FontSynthesis.Weight)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(otherStyle.fontSynthesis)
    }

    @Test
    fun `merge with other's fontFeature is null should use this' fontSynthesis`() {
        val style = SpanStyle(fontFeatureSettings = "\"kern\" 0")

        val newSpanStyle = style.merge(SpanStyle(fontFeatureSettings = null))

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(style.fontFeatureSettings)
    }

    @Test
    fun `merge with other's fontFeature is set should use other's fontSynthesis`() {
        val style = SpanStyle(fontFeatureSettings = "\"kern\" 0")
        val otherStyle = SpanStyle(fontFeatureSettings = "\"kern\" 1")

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(otherStyle.fontFeatureSettings)
    }

    @Test
    fun `merge with other's letterSpacing is unspecified should use this' letterSpacing`() {
        val style = SpanStyle(letterSpacing = 1.2.em)

        val newSpanStyle = style.merge(SpanStyle(letterSpacing = TextUnit.Unspecified))

        assertThat(newSpanStyle.letterSpacing).isEqualTo(style.letterSpacing)
    }

    @Test
    fun `merge with other's letterSpacing is set should use other's letterSpacing`() {
        val style = SpanStyle(letterSpacing = 1.2.em)
        val otherStyle = SpanStyle(letterSpacing = 1.5.em)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.letterSpacing).isEqualTo(otherStyle.letterSpacing)
    }

    @Test
    fun `merge with other's baselineShift is null should use this' baselineShift`() {
        val style = SpanStyle(baselineShift = BaselineShift.Superscript)

        val newSpanStyle = style.merge(SpanStyle(baselineShift = null))

        assertThat(newSpanStyle.baselineShift).isEqualTo(style.baselineShift)
    }

    @Test
    fun `merge with other's baselineShift is set should use other's baselineShift`() {
        val style = SpanStyle(baselineShift = BaselineShift.Superscript)
        val otherStyle = SpanStyle(baselineShift = BaselineShift.Subscript)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.baselineShift).isEqualTo(otherStyle.baselineShift)
    }

    @Test
    fun `merge with other's background is null should use this' background`() {
        val style = SpanStyle(background = Color.Red)

        val newSpanStyle = style.merge(SpanStyle(background = Color.Unspecified))

        assertThat(newSpanStyle.background).isEqualTo(style.background)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val style = SpanStyle(background = Color.Red)
        val otherStyle = SpanStyle(background = Color.Green)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.background).isEqualTo(otherStyle.background)
    }

    @Test
    fun `merge with other's textDecoration is null should use this' textDecoration`() {
        val style = SpanStyle(textDecoration = TextDecoration.LineThrough)

        val newSpanStyle = style.merge(SpanStyle(textDecoration = null))

        assertThat(newSpanStyle.textDecoration).isEqualTo(style.textDecoration)
    }

    @Test
    fun `merge with other's textDecoration is set should use other's textDecoration`() {
        val style = SpanStyle(textDecoration = TextDecoration.LineThrough)
        val otherStyle = SpanStyle(textDecoration = TextDecoration.Underline)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.textDecoration).isEqualTo(otherStyle.textDecoration)
    }

    @Test
    fun `merge with other's locale is null should use this' locale`() {
        val style = SpanStyle(localeList = LocaleList("en-US"))

        val newSpanStyle = style.merge(SpanStyle(localeList = null))

        assertThat(newSpanStyle.localeList).isEqualTo(style.localeList)
    }

    @Test
    fun `merge with other's locale is set should use other's locale`() {
        val style = SpanStyle(localeList = LocaleList("en-US"))
        val otherStyle = SpanStyle(localeList = LocaleList("ja-JP"))

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.localeList).isEqualTo(otherStyle.localeList)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `merge with null platformStyles has null platformStyle`() {
        val style = SpanStyle(platformStyle = null)
        val otherStyle = SpanStyle(platformStyle = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle).isNull()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `merge with brush has other brush and no color`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(color = Color.Red)
        val otherStyle = SpanStyle(brush = brush)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `merge with unspecified brush has original brush`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush)
        val otherStyle = SpanStyle()

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `merge brush with brush uses other's alpha`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush, alpha = 0.3f)
        val otherStyle = SpanStyle(brush = brush, alpha = 0.6f)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
        assertThat(mergedStyle.alpha).isEqualTo(0.6f)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `merge brush with brush uses current alpha if other's is NaN`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush, alpha = 0.3f)
        val otherStyle = SpanStyle(brush = brush)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
        assertThat(mergedStyle.alpha).isEqualTo(0.3f)
    }

    @Test
    fun `plus operator merges`() {
        val style = SpanStyle(
            color = Color.Red,
            fontWeight = FontWeight.Bold
        ) + SpanStyle(
            color = Color.Green,
            fontFamily = FontFamily.Cursive
        )

        assertThat(style).isEqualTo(
            SpanStyle(
                color = Color.Green, // overridden by RHS
                fontWeight = FontWeight.Bold, // from LHS,
                fontFamily = FontFamily.Cursive // from RHS
            )
        )
    }

    @Test
    fun `lerp color with a and b are not Null`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.3f
        val style1 = SpanStyle(color = color1)
        val style2 = SpanStyle(color = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp color with a and b are unspecified`() {
        val style1 = SpanStyle(color = Color.Unspecified)
        val style2 = SpanStyle(color = Color.Unspecified)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = 0.3f)

        assertThat(newSpanStyle.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `when lerp from Specified to Unspecified color, uses Color lerp logic`() {
        val t = 0.3f
        val color1 = Color.Red
        val color2 = Color.Unspecified
        val style1 = SpanStyle(color = color1)
        val style2 = SpanStyle(color = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is smaller than half`() {
        val fontFamily1 = FontFamily.SansSerif
        val fontFamily2 = FontFamily.Serif
        val t = 0.3f
        val style1 = SpanStyle(fontFamily = fontFamily1)
        val style2 = SpanStyle(fontFamily = fontFamily2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily1)
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is larger than half`() {
        val fontFamily1 = FontFamily.SansSerif
        val fontFamily2 = FontFamily.Serif
        val t = 0.8f
        val style1 = SpanStyle(fontFamily = fontFamily1)
        val style2 = SpanStyle(fontFamily = fontFamily2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily2)
    }

    @Test
    fun `lerp fontSize with a and b are not Null`() {
        val fontSize1 = 8.sp
        val fontSize2 = 16.sp
        val t = 0.8f
        val style1 = SpanStyle(fontSize = fontSize1)
        val style2 = SpanStyle(fontSize = fontSize2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        // a + (b - a) * t = 8.0f + (16.0f  - 8.0f) * 0.8f = 14.4f
        assertThat(newSpanStyle.fontSize).isEqualTo(14.4.sp)
    }

    @Test
    fun `lerp fontWeight with a and b are not Null`() {
        val fontWeight1 = FontWeight.W200
        val fontWeight2 = FontWeight.W500
        val t = 0.8f
        val style1 = SpanStyle(fontWeight = fontWeight1)
        val style2 = SpanStyle(fontWeight = fontWeight2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontWeight).isEqualTo(lerp(fontWeight1, fontWeight2, t))
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is smaller than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.3f
        val style1 = SpanStyle(fontStyle = fontStyle1)
        val style2 = SpanStyle(fontStyle = fontStyle2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle1)
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is larger than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.8f
        val style1 = SpanStyle(fontStyle = fontStyle1)
        val style2 = SpanStyle(fontStyle = fontStyle2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle2)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is smaller than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontSynthesis = fontSynthesis1)
        val style2 = SpanStyle(fontSynthesis = fontSynthesis2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis1)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is larger than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontSynthesis = fontSynthesis1)
        val style2 = SpanStyle(fontSynthesis = fontSynthesis2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis2)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is smaller than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontFeatureSettings = fontFeatureSettings1)
        val style2 = SpanStyle(fontFeatureSettings = fontFeatureSettings2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings1)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is larger than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontFeatureSettings = fontFeatureSettings1)
        val style2 = SpanStyle(fontFeatureSettings = fontFeatureSettings2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings2)
    }

    @Test
    fun `lerp baselineShift with a and b are not Null`() {
        val baselineShift1 = BaselineShift(1.0f)
        val baselineShift2 = BaselineShift(2.0f)
        val t = 0.3f
        val style1 = SpanStyle(baselineShift = baselineShift1)
        val style2 = SpanStyle(baselineShift = baselineShift2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.baselineShift)
            .isEqualTo(lerp(baselineShift1, baselineShift2, t))
    }

    @Test
    fun `lerp textGeometricTransform with a and b are not Null`() {
        val textTransform1 = TextGeometricTransform(scaleX = 1.5f, skewX = 0.1f)
        val textTransform2 = TextGeometricTransform(scaleX = 1.0f, skewX = 0.3f)
        val t = 0.3f
        val style1 = SpanStyle(textGeometricTransform = textTransform1)
        val style2 = SpanStyle(textGeometricTransform = textTransform2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.textGeometricTransform)
            .isEqualTo(lerp(textTransform1, textTransform2, t))
    }

    @Test
    fun `lerp locale with a and b are not Null and t is smaller than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.3f
        val style1 = SpanStyle(localeList = localeList1)
        val style2 = SpanStyle(localeList = localeList2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList1)
    }

    @Test
    fun `lerp locale with a and b are not Null and t is larger than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.8f
        val style1 = SpanStyle(localeList = localeList1)
        val style2 = SpanStyle(localeList = localeList2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList2)
    }

    @Test
    fun `lerp background with a and b are Null and t is smaller than half`() {
        val style1 = SpanStyle(background = Color.Unspecified)
        val style2 = SpanStyle(background = Color.Unspecified)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = 0.1f)

        assertThat(newSpanStyle.background).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `lerp background with a is Null and b is not Null`() {
        val t = 0.1f
        val color1 = Color.Unspecified
        val style1 = SpanStyle(background = color1)
        val color2 = Color.Red
        val style2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a is Not Null and b is Null`() {
        val t = 0.1f
        val color1 = Color.Red
        val style1 = SpanStyle(background = color1)
        val style2 = SpanStyle(background = Color.Unspecified)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, Color.Unspecified, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is smaller than half`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.2f
        val style1 = SpanStyle(background = color1)
        val style2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is larger than half`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.8f
        val style1 = SpanStyle(background = color1)
        val style2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp textDecoration with a and b are not Null and t is smaller than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.2f
        val style1 = SpanStyle(textDecoration = decoration1)
        val style2 = SpanStyle(textDecoration = decoration2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.textDecoration).isEqualTo(decoration1)
    }

    @Test
    fun `lerp textDecoration with a and b are not Null and t is larger than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.8f
        val style1 = SpanStyle(textDecoration = decoration1)
        val style2 = SpanStyle(textDecoration = decoration2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.textDecoration).isEqualTo(decoration2)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `lerp with null platformStyles have null platformStyle`() {
        val style = SpanStyle(platformStyle = null)
        val otherStyle = SpanStyle(platformStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.platformStyle).isNull()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `lerp brush with a specified, b specified and t is smaller than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = SpanStyle(brush = brush)
        val style2 = SpanStyle(color = Color.Red)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.brush).isEqualTo(brush)
        assertThat(newStyle.color).isEqualTo(Color.Unspecified)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `lerp brush with a specified, b specified and t is larger than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = SpanStyle(brush = brush)
        val style2 = SpanStyle(color = Color.Red)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.brush).isEqualTo(null)
        assertThat(newStyle.color).isEqualTo(Color.Red)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `lerp brush with a specified, b not specified and t is larger than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = SpanStyle(brush = brush)
        val style2 = SpanStyle()

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.brush).isNull()
        assertThat(newStyle.color).isEqualTo(Color.Unspecified)
    }
}
