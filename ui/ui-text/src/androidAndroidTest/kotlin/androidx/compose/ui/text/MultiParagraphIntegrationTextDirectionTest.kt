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

import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphIntegrationTextDirectionTest {

    private lateinit var defaultLocale: Locale
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val ltrLocaleList = LocaleList("en")
    private val rtlLocaleList = LocaleList("ar")
    private val ltrLocale = Locale.ENGLISH
    private val rtlLocale = Locale("ar")

    @Before
    fun before() {
        defaultLocale = Locale.getDefault()
    }

    @After
    fun after() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun nullTextDirection_withLtrLocale_resolvesToLtr() {
        Locale.setDefault(ltrLocale)

        val paragraph = multiParagraph(
            text = AnnotatedString(""),
            textDirection = null
        )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test
    fun nullTextDirection_withRtlLocale_resolvesToRtl() {
        Locale.setDefault(rtlLocale)

        val paragraph = multiParagraph(
            text = AnnotatedString(""),
            textDirection = null
        )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Rtl)
    }

    @Test
    fun nullTextDirection_withLtrLocaleList_resolvesToLtr() {
        val paragraph = multiParagraph(
            text = AnnotatedString(""),
            textDirection = null,
            localeList = ltrLocaleList
        )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test
    fun nullTextDirection_withRtlLocaleList_resolvesToRtl() {
        val paragraph = multiParagraph(
            text = AnnotatedString(""),
            textDirection = null,
            localeList = rtlLocaleList
        )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Rtl)
    }

    @Test
    fun textDirection_content_withLtrLocale() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = multiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = multiParagraph(
                text = text,
                fontSize = fontSize,
                textDirection = TextDirection.Content,
                localeList = ltrLocaleList,
                width = width
            )

            // First paragraph should be rendered as: "a .", dot is visually after "a ".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo("a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: ". א", dot is visually before " א".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo(width - "\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: "  .", dot is visually after "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo("  ".length * fontSizeInPx)
        }
    }

    @Test
    fun textDirection_content_withRtlLocale() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = multiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth
            assertThat(width).isLessThan(Int.MAX_VALUE)
            val paragraph = multiParagraph(
                text = text,
                fontSize = fontSize,
                textDirection = TextDirection.Content,
                localeList = rtlLocaleList,
                width = width
            )

            // First paragraph should be rendered as: "a .", dot is visually after "a ".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo("a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: ". א", dot is visually before " א".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo(width - "\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: ".  ", dot is visually before "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo(width - "  ".length * fontSizeInPx)
        }
    }

    @Test
    fun textDirection_forceLtr() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = multiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = multiParagraph(
                text = text,
                fontSize = fontSize,
                textDirection = TextDirection.Ltr,
                width = width
            )

            // First paragraph should be rendered as: "a .", dot is visually after "a ".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo("a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: "א .", dot is visually after "א ".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo("\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: "  .", dot is visually after "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo("  ".length * fontSizeInPx)
        }
    }

    @Test
    fun textDirection_forceRtl() {
        with(defaultDensity) {
            val textLtr = "a ."
            val textRtl = "\u05D0 ."
            val textNeutral = "  ."
            val text = createAnnotatedString(textLtr, textRtl, textNeutral)

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val width = multiParagraphIntrinsics(text, fontSize).maxIntrinsicWidth

            val paragraph = multiParagraph(
                text = text,
                fontSize = fontSize,
                textDirection = TextDirection.Rtl,
                width = width
            )

            // First paragraph should be rendered as: ". a", dot is visually before " a".
            assertThat(paragraph.getHorizontalPosition(2, true))
                .isEqualTo(width - "a ".length * fontSizeInPx)
            // Second paragraph should be rendered as: ". א", dot is visually before " א".
            assertThat(paragraph.getHorizontalPosition(5, true))
                .isEqualTo(width - "\u05D0 ".length * fontSizeInPx)
            // Third paragraph should be rendered as: ".  ", dot is visually before "  ".
            assertThat(paragraph.getHorizontalPosition(8, true))
                .isEqualTo(width - "  ".length * fontSizeInPx)
        }
    }

    /**
     * Helper function which creates an AnnotatedString where each input string becomes a paragraph.
     */
    private fun createAnnotatedString(vararg paragraphs: String) =
        createAnnotatedString(paragraphs.toList())

    /**
     * Helper function which creates an AnnotatedString where each input string becomes a paragraph.
     */
    private fun createAnnotatedString(paragraphs: List<String>): AnnotatedString {
        return buildAnnotatedString {
            for (paragraph in paragraphs) {
                pushStyle(ParagraphStyle())
                append(paragraph)
                pop()
            }
        }
    }

    private fun multiParagraphIntrinsics(
        text: AnnotatedString,
        fontSize: TextUnit = TextUnit.Unspecified,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf()
    ): MultiParagraphIntrinsics {
        return MultiParagraphIntrinsics(
            text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize
            ),
            placeholders = placeholders,
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }

    private fun multiParagraph(
        text: AnnotatedString,
        localeList: LocaleList? = null,
        textDirection: TextDirection? = null,
        fontSize: TextUnit = TextUnit.Unspecified,
        width: Float = Float.MAX_VALUE
    ): MultiParagraph {
        return MultiParagraph(
            annotatedString = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                localeList = localeList,
                textDirection = textDirection
            ),
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context)
        )
    }
}