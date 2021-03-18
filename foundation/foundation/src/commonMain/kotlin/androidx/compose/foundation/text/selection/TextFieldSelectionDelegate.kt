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
 * @param adjustment selection is adjusted according to this param
 * @param ensureAtLeastOneChar should selection contain at least one character
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
    adjustment: SelectionAdjustment,
    ensureAtLeastOneChar: Boolean
): TextRange {
    textLayoutResult?.let {
        val lastOffset = it.layoutInput.text.text.length

        val (startOffset, endOffset, handlesCrossed) =
            processAsSingleComposable(
                rawStartOffset = rawStartOffset,
                rawEndOffset = rawEndOffset,
                previousSelection = previousSelection,
                isStartHandle = isStartHandle,
                lastOffset = lastOffset,
                handlesCrossed = previousHandlesCrossed,
                ensureAtLeastOneChar = ensureAtLeastOneChar
            )

        val (start, end) = adjustSelection(
            textLayoutResult = textLayoutResult,
            startOffset = startOffset,
            endOffset = endOffset,
            handlesCrossed = handlesCrossed,
            adjustment = adjustment
        )

        return TextRange(start, end)
    }
    return TextRange(0, 0)
}
