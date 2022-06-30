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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextMeasurerTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)
    private val defaultDensity = Density(density = 1f)
    private val layoutDirection = LayoutDirection.Ltr

    private val longText = AnnotatedString(
        "Lorem ipsum dolor sit amet, consectetur " +
            "adipiscing elit. Curabitur augue leo, finibus vitae felis ac, pretium condimentum " +
            "augue. Nullam non libero sed lectus aliquet venenatis non at purus. Fusce id arcu " +
            "eu mauris pulvinar laoreet."
    )

    private val multiLineText = AnnotatedString("Lorem\nipsum\ndolor\nsit\namet")

    @Test
    fun width_shouldMatter_ifSoftwrapIsEnabled() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = longText,
                softWrap = true,
                constraints = Constraints(maxWidth = 200)
            )
        )

        assertThat(textLayoutResult.multiParagraph.width).isEqualTo(200)
    }

    @Test
    fun width_shouldMatter_ifSoftwrapIsDisabled_butOverflowIsEllipsis() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = longText,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = 200)
            )
        )

        assertThat(textLayoutResult.multiParagraph.width).isEqualTo(200)
    }

    @Test
    fun width_shouldBeMaxIntrinsicWidth_ifSoftwrapIsDisabled_andOverflowIsClip() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = longText,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = 200)
            )
        )

        val intrinsics = multiParagraphIntrinsics(text = longText)

        assertThat(textLayoutResult.multiParagraph.width).isEqualTo(intrinsics.maxIntrinsicWidth)
    }

    @Test
    fun width_shouldBeMaxIntrinsicWidth_ifSoftwrapIsDisabled_andOverflowIsVisible() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = longText,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = 200)
            )
        )

        val intrinsics = multiParagraphIntrinsics(text = longText)

        assertThat(textLayoutResult.multiParagraph.width).isEqualTo(intrinsics.maxIntrinsicWidth)
    }

    @Test
    fun overwriteMaxLines_ifSoftwrapIsDisabled_andTextOverflowIsEllipsis() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = multiLineText,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        )

        assertThat(textLayoutResult.multiParagraph.lineCount).isEqualTo(1)
    }

    @Test
    fun dontOverwriteMaxLines_ifSoftwrapIsEnabled() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = multiLineText,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        )

        assertThat(textLayoutResult.multiParagraph.lineCount).isEqualTo(5)
    }

    @Test
    fun disabledSoftwrap_andOverflowClip_shouldConstrainLayoutSize() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = longText,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = 200)
            )
        )

        assertThat(textLayoutResult.multiParagraph.width).isNotEqualTo(200f)
        assertThat(textLayoutResult.size.width).isEqualTo(200)
    }

    @Test
    fun disabledSoftwrap_andOverflowVisible_shouldConstrainLayoutSize() {
        val textLayoutResult = layoutText(
            textLayoutInput(
                text = longText,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = 200)
            )
        )

        assertThat(textLayoutResult.multiParagraph.width).isNotEqualTo(200f)
        assertThat(textLayoutResult.size.width).isEqualTo(200)
    }

    @Test
    fun colorShouldChangeInResult_whenCacheIsActive() {
        val textMeasurer = textMeasurer(cacheSize = 8)
        val firstTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(color = Color.Red)
            ), textMeasurer
        )

        val secondTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(color = Color.Blue)
            ), textMeasurer
        )

        assertThat(firstTextLayout.multiParagraph).isSameInstanceAs(secondTextLayout.multiParagraph)
        assertThat(firstTextLayout.layoutInput.style.color).isEqualTo(Color.Red)
        assertThat(secondTextLayout.layoutInput.style.color).isEqualTo(Color.Blue)
    }

    @Test
    fun brushShouldChangeInResult_whenCacheIsActive() {
        val textMeasurer = textMeasurer(cacheSize = 8)
        val firstTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(brush = Brush.linearGradient(listOf(Color.Red, Color.Blue)))
            ), textMeasurer
        )

        val secondTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(brush = Brush.linearGradient(listOf(Color.Green, Color.Yellow)))
            ), textMeasurer
        )

        assertThat(firstTextLayout.multiParagraph).isSameInstanceAs(secondTextLayout.multiParagraph)
        assertThat(firstTextLayout.layoutInput.style.brush)
            .isEqualTo(Brush.linearGradient(listOf(Color.Red, Color.Blue)))
        assertThat(secondTextLayout.layoutInput.style.brush)
            .isEqualTo(Brush.linearGradient(listOf(Color.Green, Color.Yellow)))
    }

    @Test
    fun shadowShouldChangeInResult_whenCacheIsActive() {
        val textMeasurer = textMeasurer(cacheSize = 8)
        val firstTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(shadow = Shadow(Color.Red))
            ), textMeasurer
        )

        val secondTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(shadow = Shadow(Color.Blue))
            ), textMeasurer
        )

        assertThat(firstTextLayout.multiParagraph).isSameInstanceAs(secondTextLayout.multiParagraph)
        assertThat(firstTextLayout.layoutInput.style.shadow).isEqualTo(Shadow(Color.Red))
        assertThat(secondTextLayout.layoutInput.style.shadow).isEqualTo(Shadow(Color.Blue))
    }

    @Test
    fun textDecorationShouldChangeInResult_whenCacheIsActive() {
        val textMeasurer = textMeasurer(cacheSize = 8)
        val firstTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(textDecoration = TextDecoration.Underline)
            ), textMeasurer
        )

        val secondTextLayout = layoutText(
            textLayoutInput(
                text = longText,
                style = TextStyle(textDecoration = TextDecoration.LineThrough)
            ), textMeasurer
        )

        assertThat(firstTextLayout.multiParagraph).isSameInstanceAs(secondTextLayout.multiParagraph)
        assertThat(firstTextLayout.layoutInput.style.textDecoration)
            .isEqualTo(TextDecoration.Underline)
        assertThat(secondTextLayout.layoutInput.style.textDecoration)
            .isEqualTo(TextDecoration.LineThrough)
    }

    private fun textLayoutInput(
        text: AnnotatedString = AnnotatedString("Hello"),
        style: TextStyle = TextStyle.Default,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        maxLines: Int = Int.MAX_VALUE,
        softWrap: Boolean = true,
        overflow: TextOverflow = TextOverflow.Clip,
        density: Density = this.defaultDensity,
        layoutDirection: LayoutDirection = this.layoutDirection,
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        constraints: Constraints = Constraints()
    ): TextLayoutInput {
        return TextLayoutInput(
            text = text,
            style = style.merge(TextStyle(fontFamily = fontFamilyMeasureFont)),
            placeholders = placeholders,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow,
            density = density,
            layoutDirection = layoutDirection,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints
        )
    }

    private fun multiParagraphIntrinsics(
        text: AnnotatedString = AnnotatedString("Hello"),
        style: TextStyle = TextStyle.Default,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        density: Density = this.defaultDensity,
        layoutDirection: LayoutDirection = this.layoutDirection,
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver
    ): MultiParagraphIntrinsics {
        return MultiParagraphIntrinsics(
            annotatedString = text,
            style = resolveDefaults(
                style.merge(TextStyle(fontFamily = fontFamilyMeasureFont)),
                layoutDirection
            ),
            placeholders = placeholders,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )
    }

    private fun textMeasurer(
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        density: Density = this.defaultDensity,
        layoutDirection: LayoutDirection = this.layoutDirection,
        cacheSize: Int = 0
    ): TextMeasurer = TextMeasurer(
        fontFamilyResolver,
        density,
        layoutDirection,
        cacheSize
    )

    private fun layoutText(
        textLayoutInput: TextLayoutInput,
        textMeasurer: TextMeasurer? = null
    ) = with(textLayoutInput) {
        (textMeasurer ?: textMeasurer()).measure(
            text = text,
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            placeholders = placeholders,
            size = IntSize(constraints.maxWidth, constraints.maxHeight),
        )
    }
}