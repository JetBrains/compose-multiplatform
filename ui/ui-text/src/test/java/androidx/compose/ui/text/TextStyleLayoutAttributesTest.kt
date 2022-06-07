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

package androidx.compose.ui.text

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleLayoutAttributesTest {
    @Test
    fun returns_true_for_the_same_instance() {
        val style = TextStyle(lineHeight = 1.em)
        assertThat(
            style.hasSameLayoutAffectingAttributes(style)
        ).isTrue()
    }

    @Test
    fun returns_true_for_the_equal_instance() {
        val style = TextStyle(lineHeight = 1.em)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                style.copy()
            )
        ).isTrue()
    }

    @Test
    fun returns_true_for_color_change() {
        val style = TextStyle(color = Color.Red)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(color = Color.Green)
            )
        ).isTrue()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun returns_true_for_color_to_brush_change() {
        val style = TextStyle(color = Color.Red)
        assertThat(
            style.hasSameLayoutAffectingAttributes(TextStyle(brush = SolidColor(Color.Green)))
        ).isTrue()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun returns_true_for_brush_to_color_change() {
        val style = TextStyle(brush = SolidColor(Color.Green))
        assertThat(
            style.hasSameLayoutAffectingAttributes(TextStyle(color = Color.Red))
        ).isTrue()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun returns_true_for_brush_solid_color_change() {
        val style = TextStyle(brush = SolidColor(Color.Red))
        style.copy()
        assertThat(
            style.hasSameLayoutAffectingAttributes(TextStyle(brush = SolidColor(Color.Green)))
        ).isTrue()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun returns_true_for_brush_shader_change() {
        val style = TextStyle(brush = Brush.linearGradient(listOf(Color.Black, Color.White)))
        assertThat(
            style.hasSameLayoutAffectingAttributes(TextStyle(
                brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
            ))
        ).isTrue()
    }

    @Test
    fun returns_true_for_shadow_change() {
        val style = TextStyle(shadow = Shadow(color = Color.Red))
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(shadow = Shadow(color = Color.Green))
            )
        ).isTrue()
    }

    @Test
    fun returns_true_for_textDecoration_change() {
        val style = TextStyle(textDecoration = TextDecoration.LineThrough)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(textDecoration = TextDecoration.Underline)
            )
        ).isTrue()
    }

    @Test
    fun returns_false_for_background_change() {
        // even though background does not change metrics, without recreating layout background
        // color animations doesn't work, do not remove.
        val style = TextStyle(background = Color.Red)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(background = Color.Green)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_fontSize_change() {
        val style = TextStyle(fontSize = 10.sp)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(fontSize = 11.sp)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_fontStyle_change() {
        val style = TextStyle(fontStyle = FontStyle.Italic)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(fontStyle = FontStyle.Normal)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_fontSynthesis_change() {
        val style = TextStyle(fontSynthesis = FontSynthesis.Style)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(fontSynthesis = FontSynthesis.Weight)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_fontFamily_change() {
        val style = TextStyle(fontFamily = FontFamily.SansSerif)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(fontFamily = FontFamily.Serif)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_fontFeatureSettings_change() {
        val style = TextStyle(fontFeatureSettings = "abc")
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(fontFeatureSettings = "def")
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_letterSpacing_change() {
        val style = TextStyle(letterSpacing = 0.2.sp)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(letterSpacing = 0.3.sp)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_baselineShift_change() {
        val style = TextStyle(baselineShift = BaselineShift.Superscript)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(baselineShift = BaselineShift.Subscript)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_textGeometricTransform_change() {
        val style = TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = 1f))
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = 2f))
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_localeList_change() {
        val style = TextStyle(localeList = LocaleList("en-US"))
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(localeList = LocaleList("en-CA"))
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_textAlign_change() {
        val style = TextStyle(textAlign = TextAlign.Start)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(textAlign = TextAlign.End)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_textDirection_change() {
        val style = TextStyle(textDirection = TextDirection.Ltr)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(textDirection = TextDirection.Rtl)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_lineHeight_change() {
        val style = TextStyle(lineHeight = 1.em)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(lineHeight = 1.1.em)
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_textIndent_change() {
        val style = TextStyle(textIndent = TextIndent(firstLine = 0.sp))
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(textIndent = TextIndent(firstLine = 1.sp))
            )
        ).isFalse()
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalTextApi::class)
    @Test
    fun returns_false_for_platformStyle_change() {
        val style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true))
            )
        ).isFalse()
    }

    @Test
    fun returns_false_for_color_and_textAlign_change() {
        val style = TextStyle(color = Color.Red, textAlign = TextAlign.Start)
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(color = Color.Blue, textAlign = TextAlign.End)
            )
        ).isFalse()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun returns_false_for_lineHeightStyle_change() {
        val style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.None
            )
        )
        assertThat(
            style.hasSameLayoutAffectingAttributes(
                TextStyle(
                    lineHeightStyle = LineHeightStyle(
                        alignment = Alignment.Bottom,
                        trim = Trim.Both
                    )
                )
            )
        ).isFalse()
    }

    @Test
    fun should_be_updated_when_a_new_attribute_is_added_to_TextStyle() {
        // TextLayoutHelper TextStyle.hasSameLayoutAffectingAttributes is very easy to forget
        // to update when TextStyle changes. Adding this test to fail so that when a new attribute
        // is added to TextStyle it will remind us that we need to update the function.
        val knownProperties = listOf(
            getProperty("color"),
            getProperty("brush"),
            getProperty("shadow"),
            getProperty("textDecoration"),
            getProperty("fontSize"),
            getProperty("fontWeight"),
            getProperty("fontStyle"),
            getProperty("fontSynthesis"),
            getProperty("fontFamily"),
            getProperty("fontFeatureSettings"),
            getProperty("letterSpacing"),
            getProperty("baselineShift"),
            getProperty("textGeometricTransform"),
            getProperty("localeList"),
            getProperty("background"),
            getProperty("textAlign"),
            getProperty("textDirection"),
            getProperty("lineHeight"),
            getProperty("textIndent"),
            getProperty("platformStyle"),
            // ParagraphStyle and SpanStyle properties are already compared, TextStyle should have
            // paragraph style attributes is tested in:
            // ui-text/../androidx/compose/ui/text/TextSpanParagraphStyleTest.kt
            getProperty("paragraphStyle"),
            getProperty("spanStyle"),
            getProperty("lineHeightStyle")
        )

        val textStyleProperties = TextStyle::class.memberProperties.map { Property(it) }

        assertWithMessage(
            "New property is added to TextStyle, TextStyle.hasSameLayoutAffectingAttributes " +
                "should be updated accordingly"
        ).that(knownProperties).containsAtLeastElementsIn(textStyleProperties)
    }

    private fun getProperty(name: String): Property {
        return TextStyle::class.memberProperties.first { it.name == name }.let { Property(it) }
    }

    private data class Property(
        val name: String?,
        val type: KType
    ) {
        constructor(parameter: KProperty1<*, *>) : this(parameter.name, parameter.returnType)
    }
}