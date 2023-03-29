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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionTest {
    @Test
    fun anchorInfo_constructor() {
        val direction = ResolvedTextDirection.Ltr
        val offset = 0

        val anchor = Selection.AnchorInfo(
            direction = direction,
            offset = offset,
            selectableId = 1L
        )

        assertThat(anchor.direction).isEqualTo(direction)
        assertThat(anchor.offset).isEqualTo(offset)
        assertThat(anchor.selectableId).isEqualTo(1L)
    }

    @Test
    fun selection_constructor() {
        val startOffset = 0
        val endOffset = 6
        val startAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset,
            selectableId = 1L
        )
        val endAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset,
            selectableId = 1L
        )
        val handleCrossed = false

        val selection = Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = handleCrossed
        )

        assertThat(selection.start).isEqualTo(startAnchor)
        assertThat(selection.end).isEqualTo(endAnchor)
        assertThat(selection.handlesCrossed).isEqualTo(handleCrossed)
    }

    @Test
    fun selection_merge_handles_not_cross() {
        val selection1 = makeSelection(9, 20, 1L, false)
        val selection2 = makeSelection(0, 30, 2L, false)
        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(9)
        assertThat(selection.end.offset).isEqualTo(30)
        assertThat(selection.start.selectableId).isEqualTo(1L)
        assertThat(selection.end.selectableId).isEqualTo(2L)
        assertThat(selection.handlesCrossed).isFalse()
    }

    @Test
    fun selection_merge_handles_cross() {
        val selection1 = makeSelection(20, 9, 1L, true)
        val selection2 = makeSelection(30, 0, 2L, true)
        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(30)
        assertThat(selection.end.offset).isEqualTo(9)
        assertThat(selection.start.selectableId).isEqualTo(2L)
        assertThat(selection.end.selectableId).isEqualTo(1L)
        assertThat(selection.handlesCrossed).isTrue()
    }

    @Test
    fun selection_merge_both_empty() {
        val selection1 = makeSelection(10, 10, 1L, false)
        val selection2 = makeSelection(0, 0, 2L, false)
        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(10)
        assertThat(selection.end.offset).isEqualTo(0)
        assertThat(selection.start.selectableId).isEqualTo(1L)
        assertThat(selection.end.selectableId).isEqualTo(2L)
        assertThat(selection.handlesCrossed).isFalse()
    }

    @Test
    fun selection_merge_empty_with_not_cross() {
        val selection1 = makeSelection(10, 10, 1L)
        val selection2 = makeSelection(0, 20, 2L, false)
        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(10)
        assertThat(selection.end.offset).isEqualTo(20)
        assertThat(selection.start.selectableId).isEqualTo(1L)
        assertThat(selection.end.selectableId).isEqualTo(2L)
        assertThat(selection.handlesCrossed).isFalse()
    }

    @Test
    fun selection_merge_empty_with_cross() {
        val selection1 = makeSelection(10, 10, 1L)
        val selection2 = makeSelection(20, 0, 2L, true)
        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(20)
        assertThat(selection.end.offset).isEqualTo(10)
        assertThat(selection.start.selectableId).isEqualTo(2L)
        assertThat(selection.end.selectableId).isEqualTo(1L)
        assertThat(selection.handlesCrossed).isTrue()
    }

    @Test
    fun selection_merge_not_cross_with_cross() {
        val selection1 = makeSelection(0, 10, 1L, false)
        val selection2 = makeSelection(20, 0, 2L, true)
        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(20)
        assertThat(selection.end.offset).isEqualTo(0)
        assertThat(selection.start.selectableId).isEqualTo(2L)
        assertThat(selection.end.selectableId).isEqualTo(1L)
        assertThat(selection.handlesCrossed).isTrue()
    }

    @Test
    fun selection_toTextRange_handles_not_cross() {
        val selection = makeSelection(0, 6)
        val textRange = selection.toTextRange()

        assertThat(textRange).isEqualTo(TextRange(0, 6))
    }

    @Test
    fun selection_toTextRange_handles_cross() {
        val selection = makeSelection(6, 0)
        val textRange = selection.toTextRange()

        assertThat(textRange).isEqualTo(TextRange(6, 0))
    }

    private fun makeSelection(
        startOffset: Int,
        endOffset: Int,
        selectableId: Long = 1L,
        handlesCrossed: Boolean = false
    ): Selection {
        val startAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset,
            selectableId = selectableId
        )
        val endAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset,
            selectableId = selectableId
        )
        return Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = handlesCrossed
        )
    }
}
