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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.packInts
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectionAdjustmentTest {
    @Test
    fun adjustment_None_noAdjustment() {
        val textLayoutResult = mockTextLayoutResult(text = "hello world")
        val rawSelection = TextRange(0, 5)
        val adjustedSelection = SelectionAdjustment.None.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = true,
            previousSelectionRange = null
        )

        assertThat(adjustedSelection).isEqualTo(rawSelection)
    }

    @Test
    fun adjustment_Character_notCollapsed_noAdjustment() {
        val textLayoutResult = mockTextLayoutResult(text = "hello world")
        val rawSelection = TextRange(0, 3)
        val adjustedSelection = SelectionAdjustment.Character.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = true,
            previousSelectionRange = null
        )

        assertThat(adjustedSelection).isEqualTo(rawSelection)
    }

    @Test
    fun adjustment_Character_collapsedNotReversed_returnOneCharSelectionNotReversed() {
        val textLayoutResult = mockTextLayoutResult(text = "hello")
        // The end offset is moving towards the start offset, which makes the new raw text range
        // collapsed.
        // After the adjustment, at least one character should be selected.
        // Since the previousTextRange is not reversed, the adjusted TextRange should
        // also be not reversed.
        // Based the above rules, adjusted text range should be [1, 2)
        val rawSelection = TextRange(1, 1)
        val previousSelection = TextRange(1, 2)
        val isStartHandle = false

        val adjustedSelection = SelectionAdjustment.Character.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedSelection).isEqualTo(TextRange(1, 2))
    }

    @Test
    fun adjustment_Character_collapsedReversed_returnOneCharSelectionReversed() {
        val textLayoutResult = mockTextLayoutResult(text = "hello")
        val rawSelection = TextRange(2, 2)
        val previousTextRange = TextRange(2, 1)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Character.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousTextRange
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(2, 1))
    }

    @Test
    fun adjustment_Word_collapsed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(1, 1)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun adjustment_Word_collapsed_onStartBoundary() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(6, 6)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 11))
    }

    @Test
    fun adjustment_Word_collapsed_onEndBoundary() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(5, 5)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun adjustment_Word_collapsed_zero() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(0, 0)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun adjustment_Word_collapsed_lastIndex() {
        val text = "hello world"
        val textLayoutResult = mockTextLayoutResult(
            text = text,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(text.lastIndex, text.lastIndex)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 11))
    }

    @Test
    fun adjustment_Word_collapsed_textLength() {
        val text = "hello world"
        val textLayoutResult = mockTextLayoutResult(
            text = text,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(text.length, text.length)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 11))
    }

    @Test
    fun adjustment_Word_collapsed_emptyString() {
        val textLayoutResult = mockTextLayoutResult(
            text = "",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        val rawSelection = TextRange(0, 0)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 0))
    }

    @Test
    fun adjustment_Word_notReversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        // The adjusted selection should cover the word "hello" and is not reversed.
        val rawSelection = TextRange(1, 2)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        // The raw selection
        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun adjustment_Word_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world",
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )
        // The raw selection is reversed, so the adjusted selection should cover the word "hello"
        // and is also reversed.
        val rawSelection = TextRange(2, 1)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(5, 0))
    }

    @Test
    fun adjustment_Word_crossWords() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(4, 7)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun adjustment_Word_crossWords_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(7, 4)

        val adjustedTextRange = SelectionAdjustment.Word.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = false,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 0))
    }

    @Test
    fun adjustment_Paragraph_collapsed() {
        val textLayoutResult = mockTextLayoutResult(text = "hello world\nhello world")

        val rawSelection = TextRange(14, 14)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(12, 23))
    }

    @Test
    fun adjustment_Paragraph_collapsed_zero() {

        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world\nhello world\nhello world"
        )
        val rawSelection = TextRange(0, 0)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun adjustment_Paragraph_collapsed_lastIndex() {
        val text = "hello world\nhello world"
        val textLayoutResult = mockTextLayoutResult(text = text)
        val rawSelection = TextRange(text.lastIndex, text.lastIndex)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(12, 23))
    }

    @Test
    fun adjustment_Paragraph_collapsed_textLength() {
        val text = "hello world\nhello world"
        val textLayoutResult = mockTextLayoutResult(text = text)
        val rawSelection = TextRange(text.length, text.length)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(12, 23))
    }

    @Test
    fun adjustment_Paragraph_emptyString() {
        val textLayoutResult = mockTextLayoutResult(text = "")
        val rawSelection = TextRange(0, 0)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 0))
    }

    @Test
    fun adjustment_Paragraph_notReversed() {
        val textLayoutResult = mockTextLayoutResult(text = "hello world\nhello world")
        // The raw selection is not reversed, so the adjusted selection should cover the word
        // "hello" and is not reversed either.
        val rawSelection = TextRange(1, 2)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun adjustment_Paragraph_reversed() {
        val textLayoutResult = mockTextLayoutResult(text = "hello world\nhello world")
        // The raw selection is reversed, so the adjusted selection should cover the word "hello"
        // and is also reversed.
        val rawSelection = TextRange(2, 1)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 0))
    }

    @Test
    fun adjustment_Paragraph_crossParagraph() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world\nhello world\nhello world"
        )
        val rawSelection = TextRange(13, 26)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.Paragraph.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(12, 35))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_initialSelection() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The and previous selection is null, it should use word based
        // selection in this case.
        val rawSelection = TextRange(3, 3)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = null
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndWithinWord() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [6, 7) and new selection expand the end to 8. This is
        // considered in-word selection. And it will use character-wise selection
        val rawSelection = TextRange(6, 8)
        val previousSelection = TextRange(6, 7)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 8))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartWithinWord_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(8, 6)
        val previousSelection = TextRange(7, 6)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = -1,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(8, 6))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartWithinWord() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [7, 11) and new selection expand the start to 8. This is
        // considered in-word selection. And it will use character-wise selection
        val rawSelection = TextRange(8, 11)
        val previousSelection = TextRange(7, 11)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(8, 11))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndWithinWord_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world\nhello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(11, 8)
        val previousSelection = TextRange(11, 7)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 8))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndOutOfWord_notExceedThreshold() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [6, 11) and the new selection expand the end to 13.
        // Because the previous selection end is at word boundary, it will use word selection mode.
        // However, the end is didn't exceed the middle of the next word(offset = 14), the adjusted
        // selection end will be 12, which is the start of the next word.
        val rawSelection = TextRange(6, 13)
        val previousSelection = TextRange(6, 11)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 12))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartOutOfWord_notExceedThreshold_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(13, 6)
        val previousSelection = TextRange(11, 6)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(12, 6))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartOutOfWord_notExceedThreshold() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [6, 11) and the new selection expand the start to 5.
        // Because the previous selection start is at word boundary, it will use word selection
        // mode.
        // However, the start is didn't exceed the middle of the previous word(offset = 2), the
        // adjusted selection end will be 5, which is the end of the previous word.
        val rawSelection = TextRange(5, 11)
        val previousSelection = TextRange(6, 11)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(5, 11))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndOutOfWord_notExceedThreshold_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(11, 5)
        val previousSelection = TextRange(11, 6)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 5))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndOutOfWord_exceedThreshold() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [6, 11) and the new selection expand the end to 15.
        // Because the previous selection end is at word boundary, it will use word based selection
        // strategy.
        // Since the 15 exceed the middle of the next word(offset: 14), the adjusted selection end
        // will be 17.
        val rawSelection = TextRange(6, 15)
        val previousSelection = TextRange(6, 11)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 17))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartOutOfWord_exceedThreshold_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(15, 6)
        val previousSelection = TextRange(11, 6)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(17, 6))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartOutOfWord_exceedThreshold() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [6, 11) and the new selection expand the end to 2.
        // Because the previous selection end is at word boundary, it will use word based selection
        // strategy.
        // Since the 2 exceed the middle of the previous word(offset: 2), the adjusted selection
        // start will be 0.
        val rawSelection = TextRange(2, 11)
        val previousSelection = TextRange(6, 11)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndOutOfWord_exceedThreshold_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(11, 2)
        val previousSelection = TextRange(11, 6)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 0))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndToNextLine() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [3, 4) and new selection expand the end to 8. Because offset
        // 8 is at the next line, it will use word based selection strategy. And since 8 exceeds
        // the middle of the next word(offset: 8), the end will be adjusted to word end: 11.
        val rawSelection = TextRange(3, 8)
        val previousSelection = TextRange(3, 4)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(3, 11))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartToNextLine_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )

        val rawSelection = TextRange(8, 3)
        val previousSelection = TextRange(4, 3)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 3))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartToNextLine() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [6, 8) and new selection expand the start to 3. Because offset
        // 3 is at the previous line, it will use word based selection strategy. And because 3
        // doesn't exceed the middle of the previous word(offset: 2), the end will be adjusted to
        // word end: 5.
        val rawSelection = TextRange(3, 8)
        val previousSelection = TextRange(7, 8)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(5, 8))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndToNextLine_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )

        val rawSelection = TextRange(8, 3)
        val previousSelection = TextRange(8, 7)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(8, 5))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndToNextLine_withinWord() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 8
        )
        // The text line break is as shown:
        //   hello wo
        //   rld hell
        //   o world
        // The previous selection is [3, 7) and the end is expanded to 9, which is the next line.
        // Because end offset is moving between lines, it will use word based selection. In this
        // case the word "world" crosses 2 lines, so the candidate values for the adjusted end
        // offset are 8(first character of the line) and 11(word end). Since 9 is closer to
        // 11(word end), the end offset will be adjusted to 11.
        val rawSelection = TextRange(3, 9)
        val previousSelection = TextRange(3, 7)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(3, 11))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartToNextLine_withinWord_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 8
        )

        val rawSelection = TextRange(9, 3)
        val previousSelection = TextRange(7, 3)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(11, 3))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandStartToNextLine_withinWord() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 8
        )
        // The text line break is as shown:
        //   hello wo
        //   rld hell
        //   o world
        // The previous selection is [16, 17) and the start is expanded to 15, which is at the
        // previous line.
        // Because start offset is moving between lines, it will use word based selection. In this
        // case the word "hello" crosses 2 lines, so the candidate values for the adjusted start
        // offset are 12(word start) and 16(last character of the line). Since 15 is closer to
        // 16(word end), the end offset will be adjusted to 16.
        val rawSelection = TextRange(15, 17)
        val previousSelection = TextRange(16, 17)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(16, 17))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_expandEndToNextLine_withinWord_reverse() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 8
        )

        val rawSelection = TextRange(17, 15)
        val previousSelection = TextRange(17, 16)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(17, 16))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkEnd() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [0, 11) and new selection shrink the end to 8. In this case
        // it will use character based selection strategy.
        val rawSelection = TextRange(0, 8)
        val previousSelection = TextRange(0, 11)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(0, 8))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkStart_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(8, 0)
        val previousSelection = TextRange(11, 0)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(8, 0))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkStart() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )
        // The previous selection is [0, 8) and new selection shrink the start to 2. In this case
        // it will use character based selection strategy.
        val rawSelection = TextRange(2, 8)
        val previousSelection = TextRange(0, 8)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(2, 8))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkEnd_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val rawSelection = TextRange(8, 2)
        val previousSelection = TextRange(8, 0)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(8, 2))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkEndToPrevLine() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [2, 8) and new selection shrink the end to 4. Because offset
        // 4 is at the previous line, it will use word based selection strategy. And the end will
        // be snap to 5.
        val rawSelection = TextRange(2, 4)
        val previousSelection = TextRange(2, 8)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(2, 5))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkStartToPrevLine_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )

        val rawSelection = TextRange(4, 2)
        val previousSelection = TextRange(8, 2)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(5, 2))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkStartToNextLine() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [2, 8) and new selection shrink the end to 7. Because offset
        // 7 is at the next line, it will use word based selection strategy. And the start will
        // be snap to 6.
        val rawSelection = TextRange(7, 8)
        val previousSelection = TextRange(2, 8)
        val isStartHandle = true

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.start,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 8))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_shrinkEndToNextLine_reversed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )

        val rawSelection = TextRange(8, 7)
        val previousSelection = TextRange(8, 2)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(8, 6))
    }

    @Test
    fun adjustment_characterWithWordAccelerate_crossLineSelection_notCollapsed() {
        val textLayoutResult = mockTextLayoutResult(
            text = "hello world hello world",
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineLength = 6
        )
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [6, 15) and new selection move the end to 7. Because offset
        // 7 is at the previous line, it will use word based selection strategy.
        // Normally, the new end will snap to the closest word boundary,
        // which is 6(the word "world"'s boundaries are 6 and 11).
        // However, in this specific case the selection start offset is already 6,
        // adjusting the end to 6 will result in a collapsed selection [6, 6). So, it should
        // move the end offset to the other word boundary which is 11 instead.
        val rawSelection = TextRange(6, 7)
        val previousSelection = TextRange(6, 15)
        val isStartHandle = false

        val adjustedTextRange = SelectionAdjustment.CharacterWithWordAccelerate.adjust(
            textLayoutResult = textLayoutResult,
            newRawSelectionRange = rawSelection,
            previousHandleOffset = previousSelection.end,
            isStartHandle = isStartHandle,
            previousSelectionRange = previousSelection
        )

        assertThat(adjustedTextRange).isEqualTo(TextRange(6, 11))
    }

    private fun mockTextLayoutResult(
        text: String,
        wordBoundaries: List<TextRange> = listOf(),
        lineLength: Int = text.length
    ): TextLayoutResult {
        val multiParagraph = mock<MultiParagraph> {
            on { getWordBoundary(any()) }.thenAnswer { invocation ->
                val offset = invocation.arguments[0] as Int
                val wordBoundary = wordBoundaries.find { offset in it.start..it.end }
                // Workaround: Mockito doesn't work with inline class now. The packed Long is
                // equal to TextRange(start, end).
                packInts(wordBoundary!!.start, wordBoundary.end)
            }

            on { getLineForOffset(any()) }.thenAnswer { invocation ->
                val offset = invocation.arguments[0] as Int
                offset / lineLength
            }

            on { getLineStart(any()) }.thenAnswer { invocation ->
                val offset = invocation.arguments[0] as Int
                offset * lineLength
            }

            on { getLineEnd(any(), any()) }.thenAnswer { invocation ->
                val offset = invocation.arguments[0] as Int
                (offset + 1) * lineLength
            }
        }

        return TextLayoutResult(
            layoutInput = TextLayoutInput(
                text = AnnotatedString(text = text),
                style = TextStyle.Default,
                placeholders = emptyList(),
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Clip,
                density = Density(1f, 1f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = mock(),
                constraints = mock()
            ),
            multiParagraph = multiParagraph,
            size = IntSize.Zero
        )
    }
}