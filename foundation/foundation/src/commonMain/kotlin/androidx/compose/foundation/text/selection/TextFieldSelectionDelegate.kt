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
 * @param isStartHandle true if the start handle is being dragged
 * @param adjustment selection is adjusted according to this param
 *
 * @return selected text range.
 */
internal fun getTextFieldSelection(
    textLayoutResult: TextLayoutResult?,
    rawStartOffset: Int,
    rawEndOffset: Int,
    previousSelection: TextRange?,
    isStartHandle: Boolean,
    adjustment: SelectionAdjustment
): TextRange {
    textLayoutResult?.let {
        val textRange = TextRange(rawStartOffset, rawEndOffset)

        // When the previous selection is null, it's allowed to have collapsed selection.
        // So we can ignore the SelectionAdjustment.CHARACTER.
        if (previousSelection == null && adjustment == SelectionAdjustment.CHARACTER) {
            return textRange
        }

        return adjustSelection(
            textLayoutResult = textLayoutResult,
            textRange = textRange,
            isStartHandle = isStartHandle,
            previousHandlesCrossed = previousSelection?.reversed ?: false,
            adjustment = adjustment
        )
    }
    return TextRange(0, 0)
}
