/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.platform.extensions.flattenFontStylesAndApply
import androidx.compose.ui.text.platform.extensions.setSpanStyles
import androidx.compose.ui.text.platform.style.ShaderBrushSpan
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt

@RunWith(AndroidJUnit4::class)
@SmallTest
class SpannableExtensionsTest {
    @Test
    fun flattenStylesAndApply_emptyList() {
        val spanStyles = listOf<AnnotatedString.Range<SpanStyle>>()
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )

        verify(block, never()).invoke(any(), anyInt(), anyInt())
    }

    @Test
    fun flattenStylesAndApply_oneStyle() {
        val spanStyle = SpanStyle(fontWeight = FontWeight(123))
        val start = 4
        val end = 10
        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle, start, end)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        verify(block, times(1)).invoke(spanStyle, start, end)
    }

    @Test
    fun flattenStylesAndApply_containedByOldStyle() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 10),
            AnnotatedString.Range(spanStyle2, 4, 6)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1, 3, 4)
            verify(block).invoke(spanStyle1.merge(spanStyle2), 4, 6)
            verify(block).invoke(spanStyle1, 6, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_containedByOldStyle_sharedStart() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 10),
            AnnotatedString.Range(spanStyle2, 3, 6)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1.merge(spanStyle2), 3, 6)
            verify(block).invoke(spanStyle1, 6, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_containedByOldStyle_sharedEnd() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 10),
            AnnotatedString.Range(spanStyle2, 5, 10)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1, 3, 5)
            verify(block).invoke(spanStyle1.merge(spanStyle2), 5, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_sameRange() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 10),
            AnnotatedString.Range(spanStyle2, 3, 10)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1.merge(spanStyle2), 3, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_overlappingStyles() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 10),
            AnnotatedString.Range(spanStyle2, 6, 19)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1, 3, 6)
            verify(block).invoke(spanStyle1.merge(spanStyle2), 6, 10)
            verify(block).invoke(spanStyle2, 10, 19)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_notIntersectedStyles() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 4),
            AnnotatedString.Range(spanStyle2, 8, 10)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1, 3, 4)
            verify(block).invoke(spanStyle2, 8, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_containedByOldStyle_appliedInOrder() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontWeight = FontWeight(200))

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 3, 10),
            AnnotatedString.Range(spanStyle2, 5, 9)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle1, 3, 5)
            // spanStyle2 will overwrite spanStyle1 in [5, 9).
            verify(block).invoke(spanStyle2, 5, 9)
            verify(block).invoke(spanStyle1, 9, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_containsOldStyle_appliedInOrder() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(123))
        val spanStyle2 = SpanStyle(fontWeight = FontWeight(200))

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 5, 7),
            AnnotatedString.Range(spanStyle2, 3, 10)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            // Ideally we can only have 1 spanStyle, but it will overcomplicate the code.
            verify(block).invoke(spanStyle2, 3, 5)
            // spanStyle2 will overwrite spanStyle1 in [5, 7).
            verify(block).invoke(spanStyle2, 5, 7)
            verify(block).invoke(spanStyle2, 7, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_notIntersected_appliedInIndexOrder() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(100))
        val spanStyle2 = SpanStyle(fontWeight = FontWeight(200))
        val spanStyle3 = SpanStyle(fontWeight = FontWeight(300))

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle3, 7, 8),
            AnnotatedString.Range(spanStyle2, 3, 4),
            AnnotatedString.Range(spanStyle1, 1, 2)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        // Despite that spanStyle3 is applied first, the spanStyles are applied in the index order.
        inOrder(block) {
            verify(block).invoke(spanStyle1, 1, 2)
            verify(block).invoke(spanStyle2, 3, 4)
            verify(block).invoke(spanStyle3, 7, 8)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_intersected_appliedInIndexOrder() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(100))
        val spanStyle2 = SpanStyle(fontWeight = FontWeight(200))

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 5, 9),
            AnnotatedString.Range(spanStyle2, 3, 6)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle2, 3, 5)
            // SpanStyles are applied in index order, but since spanStyle2 is applied later, it
            // will overwrite spanStyle1's fontWeight.
            verify(block).invoke(spanStyle2, 5, 6)
            verify(block).invoke(spanStyle1, 6, 9)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_allEmptyRanges_notApplied() {
        val contextSpanStyle = SpanStyle(fontWeight = FontWeight(400))
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(100))
        val spanStyle2 = SpanStyle(fontWeight = FontWeight(200))
        val spanStyle3 = SpanStyle(fontWeight = FontWeight(300))

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 2, 2),
            AnnotatedString.Range(spanStyle2, 4, 4),
            AnnotatedString.Range(spanStyle3, 0, 0),
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = contextSpanStyle,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(contextSpanStyle, 0, 2)
            verify(block).invoke(contextSpanStyle, 2, 4)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_emptySpanRange_shouldNotApply() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(100))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)
        val spanStyle3 = SpanStyle(fontWeight = FontWeight(200))

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle3, 4, 10),
            AnnotatedString.Range(spanStyle2, 1, 7),
            AnnotatedString.Range(spanStyle1, 3, 3)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle2, 1, 3)
            verify(block).invoke(spanStyle2, 3, 4)
            verify(block).invoke(spanStyle3.merge(spanStyle2), 4, 7)
            verify(block).invoke(spanStyle3, 7, 10)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_emptySpanRangeBeginning_shouldNotApply() {
        val spanStyle1 = SpanStyle(fontWeight = FontWeight(100))
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 0, 0),
            AnnotatedString.Range(spanStyle2, 0, 7)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = null,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(spanStyle2, 0, 7)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_withContextSpanStyle_inheritContext() {
        val color = Color.Red
        val fontStyle = FontStyle.Italic
        val fontWeight = FontWeight(200)
        val contextSpanStyle = SpanStyle(color = color, fontStyle = fontStyle)
        val spanStyle = SpanStyle(fontWeight = fontWeight)

        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle, 3, 6)
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = contextSpanStyle,
            spanStyles = spanStyles,
            block = block
        )
        inOrder(block) {
            verify(block).invoke(
                argThat {
                    this == SpanStyle(
                        color = color,
                        fontStyle = fontStyle,
                        fontWeight = fontWeight
                    )
                },
                eq(3),
                eq(6)
            )
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun flattenStylesAndApply_withContextSpanStyle_multipleSpanStyles_inheritContext() {
        val contextColor = Color.Red
        val contextFontWeight = FontWeight.Light
        val contextFontStyle = FontStyle.Normal
        val contextFontSize = 18.sp

        val fontWeight = FontWeight.Bold
        val fontStyle = FontStyle.Italic
        val fontSize = 24.sp
        val contextSpanStyle = SpanStyle(
            color = contextColor,
            fontWeight = contextFontWeight,
            fontStyle = contextFontStyle,
            fontSize = contextFontSize
        )
        val spanStyle1 = SpanStyle(fontWeight = fontWeight)
        val spanStyle2 = SpanStyle(fontStyle = fontStyle)
        val spanStyle3 = SpanStyle(fontSize = fontSize)

        // There will be 5 ranges:
        //   [2, 4)   contextColor, fontWeight,        contextFontStyle, contextFontSize
        //   [4, 6)   contextColor, fontWeight,        fontStyle,        contextFontSize
        //   [6, 8)   contextColor, fontWeight,        fontStyle,        fontSize
        //   [8, 10)  contextColor, contextFontWeight, fontStyle,        fontSize
        //   [10, 12) contextColor, contextFontWeight, contextFontStyle, fontSize
        val spanStyles = listOf(
            AnnotatedString.Range(spanStyle1, 2, 8),
            AnnotatedString.Range(spanStyle2, 4, 10),
            AnnotatedString.Range(spanStyle3, 6, 12),
        )
        val block = mock<(SpanStyle, Int, Int) -> Unit>()
        flattenFontStylesAndApply(
            contextFontSpanStyle = contextSpanStyle,
            spanStyles = spanStyles,
            block = block
        )

        inOrder(block) {
            verify(block).invoke(
                argThat {
                    this == contextSpanStyle.copy(fontWeight = fontWeight)
                },
                eq(2),
                eq(4)
            )
            verify(block).invoke(
                argThat {
                    this == contextSpanStyle.copy(fontWeight = fontWeight, fontStyle = fontStyle)
                },
                eq(4),
                eq(6)
            )
            verify(block).invoke(
                argThat {
                    this == contextSpanStyle.copy(
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        fontSize = fontSize
                    )
                },
                eq(6),
                eq(8)
            )
            verify(block).invoke(
                argThat {
                    this == contextSpanStyle.copy(
                        fontStyle = fontStyle,
                        fontSize = fontSize
                    )
                },
                eq(8),
                eq(10)
            )
            verify(block).invoke(
                argThat {
                    this == contextSpanStyle.copy(fontSize = fontSize)
                },
                eq(10),
                eq(12)
            )
            verifyNoMoreInteractions()
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun shaderBrush_shouldAdd_shaderBrushSpan_whenApplied() {
        val text = "abcde abcde"
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val spanStyle = SpanStyle(brush = brush)
        val spannable = SpannableStringBuilder().apply { append(text) }
        spannable.setSpanStyles(
            contextTextStyle = TextStyle(),
            spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
            density = Density(1f, 1f),
            resolveTypeface = { _, _, _, _ -> Typeface.DEFAULT }
        )

        assertThat(spannable).hasSpan(ShaderBrushSpan::class, 0, text.length) {
            it.shaderBrush == brush && it.alpha.isNaN()
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun shaderBrush_shouldAdd_shaderBrushSpan_whenApplied_withSpecifiedAlpha() {
        val text = "abcde abcde"
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val spanStyle = SpanStyle(brush = brush, alpha = 0.6f)
        val spannable = SpannableStringBuilder().apply { append(text) }
        spannable.setSpanStyles(
            contextTextStyle = TextStyle(),
            spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
            density = Density(1f, 1f),
            resolveTypeface = { _, _, _, _ -> Typeface.DEFAULT }
        )

        assertThat(spannable).hasSpan(ShaderBrushSpan::class, 0, text.length) {
            it.shaderBrush == brush && it.alpha == 0.6f
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun solidColorBrush_shouldAdd_ForegroundColorSpan_whenApplied() {
        val text = "abcde abcde"
        val spanStyle = SpanStyle(brush = SolidColor(Color.Red))
        val spannable = SpannableStringBuilder().apply { append(text) }
        spannable.setSpanStyles(
            contextTextStyle = TextStyle(),
            spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
            density = Density(1f, 1f),
            resolveTypeface = { _, _, _, _ -> Typeface.DEFAULT }
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun whenColorAndShaderBrushSpansCollide_bothShouldApply() {
        val text = "abcde abcde"
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val brushStyle = SpanStyle(brush = brush)
        val colorStyle = SpanStyle(color = Color.Red)
        val spannable = SpannableStringBuilder().apply { append(text) }
        spannable.setSpanStyles(
            contextTextStyle = TextStyle(),
            spanStyles = listOf(
                AnnotatedString.Range(brushStyle, 0, text.length),
                AnnotatedString.Range(colorStyle, 0, text.length)
            ),
            density = Density(1f, 1f),
            resolveTypeface = { _, _, _, _ -> Typeface.DEFAULT }
        )

        assertThat(spannable).hasSpan(ShaderBrushSpan::class, 0, text.length) {
            it.shaderBrush == brush
        }
        assertThat(spannable).hasSpan(ForegroundColorSpan::class, 0, text.length)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun whenColorAndSolidColorBrushSpansCollide_bothShouldApply() {
        val text = "abcde abcde"
        val brush = SolidColor(Color.Blue)
        val brushStyle = SpanStyle(brush = brush)
        val colorStyle = SpanStyle(color = Color.Red)
        val spannable = SpannableStringBuilder().apply { append(text) }
        spannable.setSpanStyles(
            contextTextStyle = TextStyle(),
            spanStyles = listOf(
                AnnotatedString.Range(brushStyle, 0, text.length),
                AnnotatedString.Range(colorStyle, 0, text.length)
            ),
            density = Density(1f, 1f),
            resolveTypeface = { _, _, _, _ -> Typeface.DEFAULT }
        )

        assertThat(spannable).hasSpan(ForegroundColorSpan::class, 0, text.length) {
            it.foregroundColor == Color.Blue.toArgb()
        }
        assertThat(spannable).hasSpan(ForegroundColorSpan::class, 0, text.length) {
            it.foregroundColor == Color.Red.toArgb()
        }
    }
}