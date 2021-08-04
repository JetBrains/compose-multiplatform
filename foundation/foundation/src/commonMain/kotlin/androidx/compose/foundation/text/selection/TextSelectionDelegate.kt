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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.getParagraphBoundary
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.max

/**
 * This method takes unprocessed selection information as input, and calculates the selection
 * range for current composable, and check if the selection handles are crossed, for selection with
 * the start and end are in different composables.
 *
 * @param startPosition graphical position of the start of the selection, in composable's
 * coordinates.
 * @param endPosition graphical position of the end of the selection, in composable's coordinates.
 * @param rawStartOffset unprocessed start offset calculated directly from input position
 * @param rawEndOffset unprocessed end offset calculated directly from input position
 * @param lastOffset the last offset of the text in current composable
 * @param bounds the bounds of the composable
 * @param containsWholeSelectionStart flag to check if the current composable contains the start of
 * the selection
 * @param containsWholeSelectionEnd flag to check if the current composable contains the end of the
 * selection
 *
 * @return the final textRange which contains the startOffset, endOffset of the selection, and
 * if the start and end handles are crossed each other.
 */
internal fun processCrossComposable(
    startPosition: Offset,
    endPosition: Offset,
    rawStartOffset: Int,
    rawEndOffset: Int,
    lastOffset: Int,
    bounds: Rect,
    containsWholeSelectionStart: Boolean,
    containsWholeSelectionEnd: Boolean
): TextRange? {
    val handlesCrossed = SelectionMode.Vertical.areHandlesCrossed(
        bounds = bounds,
        start = startPosition,
        end = endPosition
    )
    val isSelected = SelectionMode.Vertical.isSelected(
        bounds = bounds,
        start = if (handlesCrossed) endPosition else startPosition,
        end = if (handlesCrossed) startPosition else endPosition
    )
    val startOffset = if (isSelected && !containsWholeSelectionStart) {
        // If the composable is selected but the start is not in the composable, bound to the border
        // of the text in the composable.
        if (handlesCrossed) max(lastOffset, 0) else 0
    } else {
        // This else branch means (isSelected && containsWholeSelectionStart || !isSelected). If the
        // composable is not selected, the final offset will still be -1, if the composable contains
        // the start, the final offset has already been calculated earlier.
        rawStartOffset
    }
    val endOffset = if (isSelected && !containsWholeSelectionEnd) {
        // If the composable is selected but the end is not in the composable, bound to the border
        // of the text in the composable.
        if (handlesCrossed) 0 else max(lastOffset, 0)
    } else {
        // The same as startOffset.
        rawEndOffset
    }
    if (startOffset == -1 || endOffset == -1) return null
    return TextRange(startOffset, endOffset)
}

/**
 * This method returns the adjusted start and end offset of the selection according to [adjustment].
 *
 * @param textLayoutResult a result of the text layout.
 * @param textRange the initial selected text range which needs to be adjusted.
 * @param adjustment how to adjust selection
 *
 * @return the adjusted text selection range.
 */
internal fun adjustSelection(
    textLayoutResult: TextLayoutResult,
    textRange: TextRange,
    isStartHandle: Boolean,
    previousHandlesCrossed: Boolean,
    adjustment: SelectionAdjustment
): TextRange {
    val textLength = textLayoutResult.layoutInput.text.text.length
    if (adjustment == SelectionAdjustment.None || textLength == 0) {
        return textRange
    }

    if (adjustment == SelectionAdjustment.Character) {
        return if (!textRange.collapsed) {
            textRange
        } else {
            ensureAtLeastOneChar(
                offset = textRange.start,
                lastOffset = textLayoutResult.layoutInput.text.lastIndex,
                isStartHandle = isStartHandle,
                previousHandlesCrossed = previousHandlesCrossed
            )
        }
    }

    val boundaryFun = if (adjustment == SelectionAdjustment.Word) {
        textLayoutResult::getWordBoundary
    } else {
        textLayoutResult.layoutInput.text.text::getParagraphBoundary
    }

    val maxOffset = textLength - 1
    val startBoundary = boundaryFun(textRange.start.coerceIn(0, maxOffset))
    val endBoundary = boundaryFun(textRange.end.coerceIn(0, maxOffset))

    // If handles are not crossed, start should be snapped to the start of the word containing the
    // start offset, and end should be snapped to the end of the word containing the end offset.
    // If handles are crossed, start should be snapped to the end of the word containing the start
    // offset, and end should be snapped to the start of the word containing the end offset.
    val start = if (textRange.reversed) startBoundary.end else startBoundary.start
    val end = if (textRange.reversed) endBoundary.start else endBoundary.end

    return TextRange(start, end)
}

/**
 * This method returns the graphical position where the selection handle should be based on the
 * offset and other information.
 *
 * @param textLayoutResult a result of the text layout.
 * @param offset character offset to be calculated
 * @param isStart true if called for selection start handle
 * @param areHandlesCrossed true if the selection handles are crossed
 *
 * @return the graphical position where the selection handle should be.
 */
internal fun getSelectionHandleCoordinates(
    textLayoutResult: TextLayoutResult,
    offset: Int,
    isStart: Boolean,
    areHandlesCrossed: Boolean
): Offset {
    val line = textLayoutResult.getLineForOffset(offset)
    val offsetToCheck =
        if (isStart && !areHandlesCrossed || !isStart && areHandlesCrossed) offset
        else max(offset - 1, 0)
    val bidiRunDirection = textLayoutResult.getBidiRunDirection(offsetToCheck)
    val paragraphDirection = textLayoutResult.getParagraphDirection(offset)

    val x = textLayoutResult.getHorizontalPosition(
        offset = offset,
        usePrimaryDirection = bidiRunDirection == paragraphDirection
    )
    val y = textLayoutResult.getLineBottom(line)

    return Offset(x, y)
}
