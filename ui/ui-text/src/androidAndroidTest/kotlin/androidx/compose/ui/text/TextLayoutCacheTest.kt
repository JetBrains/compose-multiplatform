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
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextLayoutCacheTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)
    private val defaultDensity = Density(density = 1f)

    @Test(expected = IllegalArgumentException::class)
    fun capacity_cannot_be_zero() {
        TextLayoutCache(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun capacity_cannot_be_negative() {
        TextLayoutCache(-2)
    }

    @Test
    fun exactInput_shouldReturnTheSameResult() {
        val textLayoutCache = TextLayoutCache(16)
        val textLayoutInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red)
        )

        val textLayoutResult = layoutText(textLayoutInput)
        textLayoutCache.put(textLayoutInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(textLayoutInput)).isEqualTo(textLayoutResult)
    }

    @Test
    fun colorChange_shouldReturnFromCache() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red)
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Blue)
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isEqualTo(textLayoutResult)
    }

    @Test
    fun brushChange_shouldReturnFromCache() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red)
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(brush = Brush.linearGradient(listOf(Color.Blue, Color.Red)))
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isEqualTo(textLayoutResult)
    }

    @Test
    fun shadowChange_shouldReturnFromCache() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(shadow = Shadow(color = Color.Red))
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(shadow = Shadow(color = Color.Blue))
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isEqualTo(textLayoutResult)
    }

    @Test
    fun textDecorationChange_shouldReturnFromCache() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(textDecoration = TextDecoration.LineThrough)
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(textDecoration = TextDecoration.Underline)
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isEqualTo(textLayoutResult)
    }

    @Test
    fun constraintsMinChanges_shouldReturnFromCache() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red),
            constraints = Constraints(minWidth = 20, maxWidth = 200)
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red),
            constraints = Constraints(minWidth = 60, maxWidth = 200)
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isEqualTo(textLayoutResult)
    }

    @Test
    fun textChanges_shouldReturnNull() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(text = AnnotatedString("Hello World"))

        val secondInput = textLayoutInput(text = AnnotatedString("Hello World!"))

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isNull()
    }

    @Test
    fun fontSizeChange_shouldReturnNull() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red, fontSize = 14.sp)
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red, fontSize = 18.sp)
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isNull()
    }

    @Test
    fun densityChange_shouldReturnNull() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello")
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            density = Density(2f)
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isNull()
    }

    @Test
    fun layoutDirectionChange_shouldReturnNull() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            layoutDirection = LayoutDirection.Ltr
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            layoutDirection = LayoutDirection.Rtl
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isNull()
    }

    @Test
    fun constraintsMaxChanges_shouldReturnNull() {
        val textLayoutCache = TextLayoutCache(16)
        val firstInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red),
            constraints = Constraints(minWidth = 20, maxWidth = 200)
        )

        val secondInput = textLayoutInput(
            text = AnnotatedString("Hello"),
            style = TextStyle(color = Color.Red),
            constraints = Constraints(minWidth = 20, maxWidth = 250)
        )

        val textLayoutResult = layoutText(firstInput)
        textLayoutCache.put(firstInput, textLayoutResult)

        Truth.assertThat(textLayoutCache.get(secondInput)).isNull()
    }

    @Test
    fun cacheShouldEvict_leastRecentlyUsedLayout() {
        val textLayoutCache = TextLayoutCache(2)
        val firstInput = textLayoutInput(text = AnnotatedString("1"))
        val secondInput = textLayoutInput(text = AnnotatedString("2"))
        val thirdInput = textLayoutInput(text = AnnotatedString("3"))

        val firstLayout = layoutText(firstInput)
        val secondLayout = layoutText(secondInput)
        val thirdLayout = layoutText(thirdInput)

        textLayoutCache.put(firstInput, firstLayout)
        textLayoutCache.put(secondInput, secondLayout)
        textLayoutCache.get(firstInput)
        textLayoutCache.put(thirdInput, thirdLayout)

        Truth.assertThat(textLayoutCache.get(firstInput)).isNotNull()
        Truth.assertThat(textLayoutCache.get(secondInput)).isNull()
        Truth.assertThat(textLayoutCache.get(thirdInput)).isNotNull()
    }

    private fun textLayoutInput(
        text: AnnotatedString,
        style: TextStyle = TextStyle.Default,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        maxLines: Int = Int.MAX_VALUE,
        softWrap: Boolean = true,
        overflow: TextOverflow = TextOverflow.Clip,
        density: Density = this.defaultDensity,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
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

    private fun layoutText(textLayoutInput: TextLayoutInput) = with(textLayoutInput) {
        val measurer = TextMeasurer(
            fontFamilyResolver,
            density,
            layoutDirection,
            0
        )
        measurer.measure(
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