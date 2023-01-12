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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.foundation.newtext.text.TEST_FONT_FAMILY
import androidx.compose.foundation.newtext.text.toIntPx
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphLayoutCacheTest {

    private val fontFamily = TEST_FONT_FAMILY
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun minIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = MultiParagraphLayoutCache(
                text = annotatedString,
                style = TextStyle.Default,
                fontFamilyResolver = fontFamilyResolver,
            ).also {
                it.density = this
            }

            textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)

            assertThat(textDelegate.minIntrinsicWidth)
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = MultiParagraphLayoutCache(
                text = annotatedString,
                style = TextStyle.Default,
                fontFamilyResolver = fontFamilyResolver,
            ).also {
                it.density = this
            }

            textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)

            assertThat(textDelegate.maxIntrinsicWidth)
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() {
        val textDelegate = MultiParagraphLayoutCache(
            text = AnnotatedString("Hello World"),
            style = TextStyle.Default,
            fontFamilyResolver = fontFamilyResolver,
        ).also {
            it.density = density
        }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layout
        assertThat(resultFirstLayout.layoutInput.constraints).isEqualTo(constraintsFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(
            constraintsSecondLayout,
            LayoutDirection.Ltr
        )
        val resultSecondLayout = textDelegate.layout
        assertThat(resultSecondLayout.layoutInput.constraints).isEqualTo(constraintsSecondLayout)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() {
        val textDelegate = MultiParagraphLayoutCache(
            text = AnnotatedString("Hello World"),
            style = TextStyle.Default,
            fontFamilyResolver = fontFamilyResolver,
        ).also {
            it.density = density
        }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layout
        assertThat(resultFirstLayout.size.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(
            constraintsSecondLayout,
            LayoutDirection.Ltr
        )
        val resultSecondLayout = textDelegate.layout
        assertThat(resultSecondLayout.size.height).isEqualTo(heightSecondLayout)
    }

    @Test
    fun TextLayoutResult_layout_withEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")
        val textDelegate = MultiParagraphLayoutCache(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            fontFamilyResolver = fontFamilyResolver,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        ).also {
            it.density = density
        }

        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        // Makes width smaller than needed.
        val width = textDelegate.maxIntrinsicWidth / 2
        val constraints = Constraints(maxWidth = width)
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.layout

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }

    @Test
    fun TextLayoutResult_layoutWithLimitedHeight_withEllipsis() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")

        val textDelegate = MultiParagraphLayoutCache(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Ellipsis,
        ).also {
            it.density = density
        }
        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        val constraints = Constraints(
            maxWidth = textDelegate.maxIntrinsicWidth / 4,
            maxHeight = (fontSize * 2.7).roundToInt() // fully fits at most 2 lines
        )
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.layout

        assertThat(layoutResult.lineCount).isEqualTo(2)
        assertThat(layoutResult.isLineEllipsized(1)).isTrue()
    }

    @Test
    fun TextLayoutResult_sameWidth_inRtlAndLtr_withLetterSpacing() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World")

        val textDelegate = MultiParagraphLayoutCache(
            text = text,
            style = TextStyle(fontSize = fontSize.sp, letterSpacing = 0.5.sp),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Ellipsis,
        ).also {
            it.density = density
        }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val layoutResultLtr = textDelegate.layout
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Rtl)
        val layoutResultRtl = textDelegate.layout

        assertThat(layoutResultLtr.size.width).isEqualTo(layoutResultRtl.size.width)
    }
}
