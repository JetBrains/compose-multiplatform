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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleResolveDefaultsTest {
    private val DefaultFontSize = 14.sp
    private val DefaultLetterSpacing = 0.sp
    private val DefaultBackgroundColor = Color.Transparent
    private val DefaultLineHeight = TextUnit.Unspecified
    private val DefaultColor = Color.Black

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_default_values() {
        // We explicitly expect the default values since we do not want to change these values.
        resolveDefaults(TextStyle(), LayoutDirection.Ltr).also {
            assertThat(it.brush).isNull()
            assertThat(it.color).isEqualTo(DefaultColor)
            assertThat(it.fontSize).isEqualTo(DefaultFontSize)
            assertThat(it.fontWeight).isEqualTo(FontWeight.Normal)
            assertThat(it.fontStyle).isEqualTo(FontStyle.Normal)
            assertThat(it.fontSynthesis).isEqualTo(FontSynthesis.All)
            assertThat(it.fontFamily).isEqualTo(FontFamily.Default)
            assertThat(it.fontFeatureSettings).isEqualTo("")
            assertThat(it.letterSpacing).isEqualTo(DefaultLetterSpacing)
            assertThat(it.baselineShift).isEqualTo(BaselineShift.None)
            assertThat(it.textGeometricTransform).isEqualTo(TextGeometricTransform.None)
            assertThat(it.localeList).isEqualTo(LocaleList.current)
            assertThat(it.background).isEqualTo(DefaultBackgroundColor)
            assertThat(it.textDecoration).isEqualTo(TextDecoration.None)
            assertThat(it.shadow).isEqualTo(Shadow.None)
            assertThat(it.textAlign).isEqualTo(TextAlign.Start)
            assertThat(it.textDirection).isEqualTo(TextDirection.Ltr)
            assertThat(it.lineHeight).isEqualTo(DefaultLineHeight)
            assertThat(it.textIndent).isEqualTo(TextIndent.None)
            assertThat(it.platformStyle).isNull()
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_use_provided_values_brush() {
        val brush = Brush.linearGradient(listOf(Color.White, Color.Black))

        assertThat(
            resolveDefaults(
                TextStyle(brush = brush),
                direction = LayoutDirection.Ltr
            ).brush
        ).isEqualTo(brush)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_use_provided_values_shader_brush_color_unspecified() {
        val brush = Brush.linearGradient(listOf(Color.White, Color.Black))

        assertThat(
            resolveDefaults(
                TextStyle(brush = brush),
                direction = LayoutDirection.Ltr
            ).color
        ).isEqualTo(Color.Unspecified)
    }

    @Test
    fun test_use_provided_values_color() {
        assertThat(
            resolveDefaults(
                TextStyle(color = Color.Red),
                direction = LayoutDirection.Ltr
            ).color
        ).isEqualTo(Color.Red)
    }

    @Test
    fun test_use_provided_values_fontSize() {
        assertThat(
            resolveDefaults(
                TextStyle(fontSize = DefaultFontSize * 2),
                direction = LayoutDirection.Ltr
            ).fontSize
        ).isEqualTo(DefaultFontSize * 2)
    }

    @Test
    fun test_use_provided_values_fontWeight() {
        assertThat(
            resolveDefaults(
                TextStyle(fontWeight = FontWeight.W900),
                direction = LayoutDirection.Ltr
            ).fontWeight
        ).isEqualTo(FontWeight.W900)
    }

    @Test
    fun test_use_provided_values_fontStyle() {
        assertThat(
            resolveDefaults(
                TextStyle(fontStyle = FontStyle.Italic),
                direction = LayoutDirection.Ltr
            ).fontStyle
        ).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun test_use_provided_values_fontSynthesis() {
        assertThat(
            resolveDefaults(
                TextStyle(fontSynthesis = FontSynthesis.Weight),
                direction = LayoutDirection.Ltr
            ).fontSynthesis
        ).isEqualTo(FontSynthesis.Weight)
    }

    @Test
    fun test_use_provided_values_fontFamily() {
        assertThat(
            resolveDefaults(
                TextStyle(fontFamily = FontFamily.Cursive),
                direction = LayoutDirection.Ltr
            ).fontFamily
        ).isEqualTo(FontFamily.Cursive)
    }

    @Test
    fun test_use_provided_values_fontFeatureSettings() {
        assertThat(
            resolveDefaults(
                TextStyle(fontFeatureSettings = "'liga': off"),
                direction = LayoutDirection.Ltr
            ).fontFeatureSettings
        ).isEqualTo("'liga': off")
    }

    @Test
    fun test_use_provided_values_letterSpacing() {
        assertThat(
            resolveDefaults(
                TextStyle(letterSpacing = 1.2.em),
                direction = LayoutDirection.Ltr
            ).letterSpacing
        ).isEqualTo(1.2.em)
    }

    @Test
    fun test_use_provided_values_baselineShift() {
        assertThat(
            resolveDefaults(
                TextStyle(baselineShift = BaselineShift.Superscript),
                direction = LayoutDirection.Ltr
            ).baselineShift
        ).isEqualTo(BaselineShift.Superscript)
    }

    @Test
    fun test_use_provided_values_textGeometricTransform() {
        assertThat(
            resolveDefaults(
                TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = 10.0f)),
                direction = LayoutDirection.Ltr
            ).textGeometricTransform
        ).isEqualTo(TextGeometricTransform(scaleX = 10.0f))
    }

    @Test
    fun test_use_provided_values_localeList() {
        assertThat(
            resolveDefaults(
                TextStyle(localeList = LocaleList("fr-FR")),
                direction = LayoutDirection.Ltr
            ).localeList
        ).isEqualTo(LocaleList("fr-FR"))
    }

    @Test
    fun test_use_provided_values_background() {
        assertThat(
            resolveDefaults(
                TextStyle(background = Color.Blue),
                direction = LayoutDirection.Ltr
            ).background
        ).isEqualTo(Color.Blue)
    }

    @Test
    fun test_use_provided_values_textDecoration() {
        assertThat(
            resolveDefaults(
                TextStyle(textDecoration = TextDecoration.LineThrough),
                direction = LayoutDirection.Ltr
            ).textDecoration
        ).isEqualTo(TextDecoration.LineThrough)
    }

    @Test
    fun test_use_provided_values_shadow() {
        assertThat(
            resolveDefaults(
                TextStyle(shadow = Shadow(color = Color.Yellow)),
                direction = LayoutDirection.Ltr
            ).shadow
        ).isEqualTo(Shadow(color = Color.Yellow))
    }

    @Test
    fun test_use_provided_values_textAlign() {
        assertThat(
            resolveDefaults(
                TextStyle(textAlign = TextAlign.Right),
                direction = LayoutDirection.Ltr
            ).textAlign
        ).isEqualTo(TextAlign.Right)
    }

    @Test
    fun test_use_provided_values_lineHeight() {
        assertThat(
            resolveDefaults(
                TextStyle(lineHeight = 12.sp),
                direction = LayoutDirection.Ltr
            ).lineHeight
        ).isEqualTo(12.sp)
    }

    @Test
    fun test_use_provided_values_textIndent() {
        assertThat(
            resolveDefaults(
                TextStyle(textIndent = TextIndent(12.sp, 13.sp)),
                direction = LayoutDirection.Ltr
            ).textIndent
        ).isEqualTo(TextIndent(12.sp, 13.sp))
    }

    @Test
    fun test_use_provided_values_textDirection_with_LTR_layoutDirection() {
        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.Content),
                direction = LayoutDirection.Ltr
            ).textDirection
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.Ltr),
                direction = LayoutDirection.Ltr
            ).textDirection
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.Rtl),
                direction = LayoutDirection.Ltr
            ).textDirection
        ).isEqualTo(TextDirection.Rtl)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.ContentOrLtr),
                direction = LayoutDirection.Ltr
            ).textDirection
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.ContentOrRtl),
                direction = LayoutDirection.Ltr
            ).textDirection
        ).isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun test_use_provided_values_textDirection_with_RTL_layoutDirection() {
        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.Content),
                direction = LayoutDirection.Rtl
            ).textDirection
        ).isEqualTo(TextDirection.ContentOrRtl)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.Ltr),
                direction = LayoutDirection.Rtl
            ).textDirection
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.Rtl),
                direction = LayoutDirection.Rtl
            ).textDirection
        ).isEqualTo(TextDirection.Rtl)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.ContentOrLtr),
                direction = LayoutDirection.Rtl
            ).textDirection
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveDefaults(
                TextStyle(textDirection = TextDirection.ContentOrRtl),
                direction = LayoutDirection.Rtl
            ).textDirection
        ).isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun test_default_direction_algorithm_with_provided_layoutDirection() {
        assertThat(
            resolveDefaults(
                TextStyle(),
                direction = LayoutDirection.Ltr
            ).textDirection
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveDefaults(
                TextStyle(),
                direction = LayoutDirection.Rtl
            ).textDirection
        ).isEqualTo(TextDirection.Rtl)
    }
}
