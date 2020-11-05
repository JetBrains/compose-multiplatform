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

package androidx.compose.ui.selection

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionTest {
    @Test
    fun anchorInfo_constructor() {
        val direction = ResolvedTextDirection.Ltr
        val offset = 0
        val selectable: Selectable = mock()

        val anchor = Selection.AnchorInfo(
            direction = direction,
            offset = offset,
            selectable = selectable
        )

        assertThat(anchor.direction).isEqualTo(direction)
        assertThat(anchor.offset).isEqualTo(offset)
        assertThat(anchor.selectable).isEqualTo(selectable)
    }

    @Test
    fun selection_constructor() {
        val startOffset = 0
        val endOffset = 6
        val selectable: Selectable = mock()
        val startAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset,
            selectable = selectable
        )
        val endAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset,
            selectable = selectable
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
        val startOffset1 = 9
        val endOffset1 = 20
        val selectable1: Selectable = mock()
        val startAnchor1 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset1,
            selectable = selectable1
        )
        val endAnchor1 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset1,
            selectable = selectable1
        )
        val selection1 = Selection(
            start = startAnchor1,
            end = endAnchor1,
            handlesCrossed = false
        )
        val startOffset2 = 0
        val endOffset2 = 30
        val selectable2: Selectable = mock()
        val startAnchor2 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset2,
            selectable = selectable2
        )
        val endAnchor2 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset2,
            selectable = selectable2
        )
        val selection2 = Selection(
            start = startAnchor2,
            end = endAnchor2,
            handlesCrossed = false
        )

        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(startOffset1)
        assertThat(selection.end.offset).isEqualTo(endOffset2)
        assertThat(selection.start.selectable).isEqualTo(selectable1)
        assertThat(selection.end.selectable).isEqualTo(selectable2)
        assertThat(selection.handlesCrossed).isFalse()
    }

    @Test
    fun selection_merge_handles_cross() {
        val startOffset1 = 20
        val endOffset1 = 9
        val selectable1: Selectable = mock()
        val startAnchor1 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset1,
            selectable = selectable1
        )
        val endAnchor1 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset1,
            selectable = selectable1
        )
        val selection1 = Selection(
            start = startAnchor1,
            end = endAnchor1,
            handlesCrossed = true
        )
        val startOffset2 = 30
        val endOffset2 = 0
        val selectable2: Selectable = mock()
        val startAnchor2 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset2,
            selectable = selectable2
        )
        val endAnchor2 = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset2,
            selectable = selectable2
        )
        val selection2 = Selection(
            start = startAnchor2,
            end = endAnchor2,
            handlesCrossed = true
        )

        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(startOffset2)
        assertThat(selection.end.offset).isEqualTo(endOffset1)
        assertThat(selection.start.selectable).isEqualTo(selectable2)
        assertThat(selection.end.selectable).isEqualTo(selectable1)
        assertThat(selection.handlesCrossed).isTrue()
    }

    @Test
    fun selection_toTextRange_handles_not_cross() {
        val startOffset = 0
        val endOffset = 6
        val startAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset,
            selectable = mock()
        )
        val endAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset,
            selectable = mock()
        )
        val selection = Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = false
        )

        val textRange = selection.toTextRange()

        assertThat(textRange).isEqualTo(TextRange(startOffset, endOffset))
    }

    @Test
    fun selection_toTextRange_handles_cross() {
        val startOffset = 6
        val endOffset = 0
        val startAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = startOffset,
            selectable = mock()
        )
        val endAnchor = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = endOffset,
            selectable = mock()
        )
        val selection = Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = false
        )

        val textRange = selection.toTextRange()

        assertThat(textRange).isEqualTo(TextRange(startOffset, endOffset))
    }
}
