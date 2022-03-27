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

package androidx.compose.foundation.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextLayoutHelperTest {

    lateinit var fontFamilyResolver: FontFamily.Resolver

    lateinit var referenceResult: TextLayoutResult

    @Before
    fun setUp() {
        fontFamilyResolver = mock()

        val intrinsics = mock<MultiParagraphIntrinsics>()
        val multiParagraph = mock<MultiParagraph>()
        whenever(multiParagraph.intrinsics).thenReturn(intrinsics)
        whenever(intrinsics.hasStaleResolvedFonts).thenReturn(false)
        referenceResult = TextLayoutResult(
            TextLayoutInput(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = listOf(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = Constraints.fixedWidth(100)
            ),
            multiParagraph = multiParagraph,
            size = IntSize(50, 50)
        )
    }

    @Test
    fun testCanReuse_same() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isTrue()
    }

    @Test
    fun testCanReuse_different_text() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, Android").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_style() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(fontSize = 1.5.em),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_maxLines() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_softWrap() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_overflow() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Clip,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_density() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(2.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_layoutDirection() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Rtl,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_resourceLoader() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = mock(),
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_constraints() {
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = Constraints.fixedWidth(200)
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_placeholders() {
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = listOf(
                    AnnotatedString.Range(
                        item = Placeholder(10.sp, 20.sp, PlaceholderVerticalAlign.AboveBaseline),
                        start = 0,
                        end = 5
                    )
                ),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = Constraints.fixedWidth(200)
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_notLatestTypefaces_isFalse() {
        val constraints = Constraints.fixedWidth(100)
        whenever(referenceResult.multiParagraph.intrinsics.hasStaleResolvedFonts)
            .thenReturn(true)
        assertThat(referenceResult.canReuse(
            text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
            style = TextStyle(),
            placeholders = emptyList(),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints
        )).isFalse()
    }

    @Test
    fun canReuseLayout_returns_true_for_the_same_instance() {
        val style = TextStyle(lineHeight = 1.em)
        assertThat(
            style.canReuseLayout(style)
        ).isTrue()
    }

    @Test
    fun canReuseLayout_returns_true_for_the_equal_instance() {
        val style = TextStyle(lineHeight = 1.em)
        assertThat(
            style.canReuseLayout(style.copy())
        ).isTrue()
    }

    @Test
    fun canReuseLayout_returns_true_for_color_change() {
        val style = TextStyle(color = Color.Red)
        assertThat(
            style.canReuseLayout(TextStyle(color = Color.Green))
        ).isTrue()
    }

    @Test
    fun canReuseLayout_returns_true_for_shadow_change() {
        val style = TextStyle(shadow = Shadow(color = Color.Red))
        assertThat(
            style.canReuseLayout(TextStyle(shadow = Shadow(color = Color.Green)))
        ).isTrue()
    }

    @Test
    fun canReuseLayout_returns_true_for_textDecoration_change() {
        val style = TextStyle(textDecoration = TextDecoration.LineThrough)
        assertThat(
            style.canReuseLayout(TextStyle(textDecoration = TextDecoration.Underline))
        ).isTrue()
    }

    @Test
    fun canReuseLayout_returns_false_for_background_change() {
        // even though background does not change metrics, without recreating layout background
        // color animations doesn't work, do not remove.
        val style = TextStyle(background = Color.Red)
        assertThat(
            style.canReuseLayout(TextStyle(background = Color.Green))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_fontSize_change() {
        val style = TextStyle(fontSize = 10.sp)
        assertThat(
            style.canReuseLayout(TextStyle(fontSize = 11.sp))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_fontStyle_change() {
        val style = TextStyle(fontStyle = FontStyle.Italic)
        assertThat(
            style.canReuseLayout(TextStyle(fontStyle = FontStyle.Normal))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_fontSynthesis_change() {
        val style = TextStyle(fontSynthesis = FontSynthesis.Style)
        assertThat(
            style.canReuseLayout(TextStyle(fontSynthesis = FontSynthesis.Weight))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_fontFamily_change() {
        val style = TextStyle(fontFamily = FontFamily.SansSerif)
        assertThat(
            style.canReuseLayout(TextStyle(fontFamily = FontFamily.Serif))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_fontFeatureSettings_change() {
        val style = TextStyle(fontFeatureSettings = "abc")
        assertThat(
            style.canReuseLayout(TextStyle(fontFeatureSettings = "def"))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_letterSpacing_change() {
        val style = TextStyle(letterSpacing = 0.2.sp)
        assertThat(
            style.canReuseLayout(TextStyle(letterSpacing = 0.3.sp))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_baselineShift_change() {
        val style = TextStyle(baselineShift = BaselineShift.Superscript)
        assertThat(
            style.canReuseLayout(TextStyle(baselineShift = BaselineShift.Subscript))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_textGeometricTransform_change() {
        val style = TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = 1f))
        assertThat(
            style.canReuseLayout(
                TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = 2f))
            )
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_localeList_change() {
        val style = TextStyle(localeList = LocaleList("en-US"))
        assertThat(
            style.canReuseLayout(TextStyle(localeList = LocaleList("en-CA")))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_textAlign_change() {
        val style = TextStyle(textAlign = TextAlign.Start)
        assertThat(
            style.canReuseLayout(TextStyle(textAlign = TextAlign.End))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_textDirection_change() {
        val style = TextStyle(textDirection = TextDirection.Ltr)
        assertThat(
            style.canReuseLayout(TextStyle(textDirection = TextDirection.Rtl))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_lineHeight_change() {
        val style = TextStyle(lineHeight = 1.em)
        assertThat(
            style.canReuseLayout(TextStyle(lineHeight = 1.1.em))
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_textIndent_change() {
        val style = TextStyle(textIndent = TextIndent(firstLine = 0.sp))
        assertThat(
            style.canReuseLayout(TextStyle(textIndent = TextIndent(firstLine = 1.sp)))
        ).isFalse()
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalTextApi::class)
    @Test
    fun canReuseLayout_returns_false_for_platformStyle_change() {
        val style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        assertThat(
            style.canReuseLayout(
                TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true))
            )
        ).isFalse()
    }

    @Test
    fun canReuseLayout_returns_false_for_color_and_textAlign_change() {
        val style = TextStyle(color = Color.Red, textAlign = TextAlign.Start)
        assertThat(
            style.canReuseLayout(
                TextStyle(color = Color.Blue, textAlign = TextAlign.End)
            )
        ).isFalse()
    }

    @Test
    fun canReuseLayout_should_be_updated_when_a_new_attribute_is_added_to_TextStyle() {
        // TextLayoutHelper TextStyle.caReuseLayout is very easy to forget to update when TextStyle
        // changes. Adding this test to fail so that when a new attribute is added to TextStyle
        // it will remind us that we need to update the function.
        val knownProperties = listOf(
            getProperty("color"),
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
            getProperty("spanStyle")
        )

        val textStyleProperties = TextStyle::class.memberProperties.map { Property(it) }

        assertWithMessage(
            "New property is added to TextStyle, TextStyle.canReuseLayout should be " +
            "updated accordingly"
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
