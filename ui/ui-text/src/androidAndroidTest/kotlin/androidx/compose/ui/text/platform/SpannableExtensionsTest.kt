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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.extensions.flattenStylesAndApply
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.any
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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

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
        flattenStylesAndApply(spanStyles, block)

        inOrder(block) {
            verify(block).invoke(spanStyle2, 3, 5)
            // SpanStyles are applied in index order, but since spanStyle2 is applied later, it
            // will overwrite spanStyle1's fontWeight.
            verify(block).invoke(spanStyle2, 5, 6)
            verify(block).invoke(spanStyle1, 6, 9)
            verifyNoMoreInteractions()
        }
    }
}