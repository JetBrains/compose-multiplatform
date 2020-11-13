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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

/**
 * Return selection information for TextField.
 *
 * @param textLayoutResult a result of the text layout.
 * @param rawStartOffset unprocessed start offset calculated directly from input position
 * @param rawEndOffset unprocessed end offset calculated directly from input position
 * @param previousSelection previous selection result
 * @param previousHandlesCrossed true if the previous selection's handles are crossed
 * @param isStartHandle true if the start handle is being dragged
 * @param wordBasedSelection This flag is ignored if the selection handles are being dragged. If
 * the selection is modified by long press and drag gesture, the result selection will be
 * adjusted to word based selection. Otherwise, the selection will be adjusted to character based
 * selection.
 *
 * @return selected text range.
 */
internal fun getTextFieldSelection(
    textLayoutResult: TextLayoutResult?,
    rawStartOffset: Int,
    rawEndOffset: Int,
    previousSelection: TextRange?,
    previousHandlesCrossed: Boolean,
    isStartHandle: Boolean,
    wordBasedSelection: Boolean
): TextRange {
    textLayoutResult?.let {
        val lastOffset = it.layoutInput.text.text.length

        var (startOffset, endOffset, handlesCrossed) =
            processAsSingleComposable(
                rawStartOffset = rawStartOffset,
                rawEndOffset = rawEndOffset,
                previousSelection = previousSelection,
                isStartHandle = isStartHandle,
                lastOffset = lastOffset,
                handlesCrossed = previousHandlesCrossed
            )
        if (wordBasedSelection) {
            val (start, end) = updateWordBasedSelection(
                textLayoutResult = it,
                startOffset = startOffset,
                endOffset = endOffset,
                handlesCrossed = handlesCrossed
            )
            startOffset = start
            endOffset = end
        }

        return TextRange(startOffset, endOffset)
    }
    return TextRange(0, 0)
}
