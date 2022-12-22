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

package androidx.compose.foundation.copyPasteAndroidTests.text

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.test.Ignore
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, InternalFoundationTextApi::class)
class TextDelegateIntegrationTest {

    @Test
    @Ignore // TODO: test is failing
    fun minIntrinsicWidth_getter() = with(Density(1f, 1f)) {
        val fontSize = 20.sp
        val text = "Hello"
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = FontFamily.Default)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            style = TextStyle.Default,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )

        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

        assertThat(textDelegate.minIntrinsicWidth)
            .isEqualTo((fontSize.toPx() * text.length))
    }

    @Test
    @Ignore // TODO: test is failing
    fun maxIntrinsicWidth_getter() = with(Density(1f, 1f)) {
        val fontSize = 20.sp
        val text = "Hello"
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = FontFamily.Default)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            style = TextStyle.Default,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )

        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

        assertThat(textDelegate.maxIntrinsicWidth)
            .isEqualTo((fontSize.toPx() * text.length))
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() = with(Density(1f, 1f)) {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello World!"),
            style = TextStyle.Default,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        val resultFirstLayout = textDelegate.layout(constraintsFirstLayout, LayoutDirection.Ltr)
        assertThat(resultFirstLayout.layoutInput.constraints).isEqualTo(constraintsFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        val resultSecondLayout = textDelegate.layout(
            constraintsSecondLayout,
            LayoutDirection.Ltr,
            resultFirstLayout
        )
        assertThat(resultSecondLayout.layoutInput.constraints).isEqualTo(constraintsSecondLayout)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() = with(Density(1f, 1f)) {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello World!"),
            style = TextStyle.Default,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        val resultFirstLayout = textDelegate.layout(constraintsFirstLayout, LayoutDirection.Ltr)
        assertThat(resultFirstLayout.size.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        val resultSecondLayout = textDelegate.layout(
            constraintsSecondLayout,
            LayoutDirection.Ltr,
            resultFirstLayout
        )
        assertThat(resultSecondLayout.size.height).isEqualTo(heightSecondLayout)
    }

    @Test
    @Ignore // TODO: test is failing
    fun TextLayoutResult_layout_withEllipsis_withoutSoftWrap() = with(Density(1f, 1f)) {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")
        val textDelegate = TextDelegate(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )
        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)
        // Makes width smaller than needed.
        val width = textDelegate.maxIntrinsicWidth / 2
        val constraints = Constraints(maxWidth = width)
        val layoutResult = textDelegate.layout(constraints, LayoutDirection.Ltr)

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }

    @Test
    @Ignore // TODO: test is failing
    fun TextLayoutResult_layoutWithLimitedHeight_withEllipsis() = with(Density(1f, 1f)) {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")
        val textDelegate = TextDelegate(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            overflow = TextOverflow.Ellipsis,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )
        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

        val constraints = Constraints(
            maxWidth = textDelegate.maxIntrinsicWidth / 4,
            maxHeight = (fontSize * 2.7).roundToInt() // fully fits at most 2 lines
        )
        val layoutResult = textDelegate.layout(constraints, LayoutDirection.Ltr)

        assertThat(layoutResult.lineCount).isEqualTo(2)
        assertThat(layoutResult.isLineEllipsized(1)).isTrue()
    }

    @Test
    fun TextLayoutResult_sameWidth_inRtlAndLtr_withLetterSpacing() = with(Density(1f, 1f)) {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World")
        val textDelegate = TextDelegate(
            text = text,
            style = TextStyle(fontSize = fontSize.sp, letterSpacing = 0.5.sp),
            overflow = TextOverflow.Ellipsis,
            density = this,
            fontFamilyResolver = createFontFamilyResolver()
        )
        val layoutResultLtr = textDelegate.layout(Constraints(), LayoutDirection.Ltr)
        val layoutResultRtl = textDelegate.layout(Constraints(), LayoutDirection.Rtl)

        assertThat(layoutResultLtr.size.width).isEqualTo(layoutResultRtl.size.width)
    }
}
