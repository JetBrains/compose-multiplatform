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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.max

/**
 * This method takes unprocessed selection information as input, and calculates the selection
 * range and check if the selection handles are crossed, for selection with both start and end
 * are in the current composable.
 *
 * @param rawStartOffset unprocessed start offset calculated directly from input position
 * @param rawEndOffset unprocessed end offset calculated directly from input position
 * different location. If the selection anchors point the same location and this is true, the
 * result selection will be adjusted to word boundary. Otherwise, the selection will be adjusted
 * to keep single character selected.
 * @param previousSelection previous selected text range.
 * @param isStartHandle true if the start handle is being dragged
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param handlesCrossed true if the selection handles are crossed
 *
 * @return the final startOffset, endOffset of the selection, and if the start and end are
 * crossed each other.
 */
internal fun processAsSingleComposable(
    rawStartOffset: Int,
    rawEndOffset: Int,
    previousSelection: TextRange?,
    isStartHandle: Boolean,
    lastOffset: Int,
    handlesCrossed: Boolean
): Triple<Int, Int, Boolean> {
    var startOffset = rawStartOffset
    var endOffset = rawEndOffset
    if (startOffset == endOffset) {

        // If the start and end offset are at the same character, and it's not the initial
        // selection, then bound to at least one character.
        val textRange = ensureAtLeastOneChar(
            offset = rawStartOffset,
            lastOffset = lastOffset,
            previousSelection = previousSelection,
            isStartHandle = isStartHandle,
            handlesCrossed = handlesCrossed
        )
        startOffset = textRange.start
        endOffset = textRange.end
    }
    // Check if the start and end handles are crossed each other.
    val areHandlesCrossed = startOffset > endOffset
    return Triple(startOffset, endOffset, areHandlesCrossed)
}

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
 * @return the final startOffset, endOffset of the selection, and if the start and end handles are
 * crossed each other.
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
): Triple<Int, Int, Boolean> {
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
    return Triple(startOffset, endOffset, handlesCrossed)
}

/**
 * This method returns the adjusted word-based start and end offset of the selection.
 *
 * @param textLayoutResult a result of the text layout.
 * @param startOffset start offset to be snapped to a word.
 * @param endOffset end offset to be snapped to a word.
 * @param handlesCrossed true if the selection handles are crossed
 *
 * @return the adjusted word-based start and end offset of the selection.
 */
internal fun updateWordBasedSelection(
    textLayoutResult: TextLayoutResult,
    startOffset: Int,
    endOffset: Int,
    handlesCrossed: Boolean
): Pair<Int, Int> {
    val maxOffset = textLayoutResult.layoutInput.text.text.length - 1
    val startWordBoundary = textLayoutResult.getWordBoundary(startOffset.coerceIn(0, maxOffset))
    val endWordBoundary = textLayoutResult.getWordBoundary(endOffset.coerceIn(0, maxOffset))

    // If handles are not crossed, start should be snapped to the start of the word containing the
    // start offset, and end should be snapped to the end of the word containing the end offset.
    // If handles are crossed, start should be snapped to the end of the word containing the start
    // offset, and end should be snapped to the start of the word containing the end offset.
    val start = if (handlesCrossed) startWordBoundary.end else startWordBoundary.start
    val end = if (handlesCrossed) endWordBoundary.start else endWordBoundary.end

    return Pair(start, end)
}

/**
 * This method adjusts the raw start and end offset and bounds the selection to one character. The
 * logic of bounding evaluates the last selection result, which handle is being dragged, and if
 * selection reaches the boundary.
 *
 * @param offset unprocessed start and end offset calculated directly from input position, in
 * this case start and offset equals to each other.
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param previousSelection previous selected text range.
 * @param isStartHandle true if the start handle is being dragged
 * @param handlesCrossed true if the selection handles are crossed
 *
 * @return the adjusted [TextRange].
 */
private fun ensureAtLeastOneChar(
    offset: Int,
    lastOffset: Int,
    previousSelection: TextRange?,
    isStartHandle: Boolean,
    handlesCrossed: Boolean
): TextRange {
    var newStartOffset = offset
    var newEndOffset = offset

    previousSelection?.let {
        if (isStartHandle) {
            newStartOffset =
                if (handlesCrossed) {
                    if (newEndOffset == 0 || it.start == newEndOffset + 1) {
                        newEndOffset + 1
                    } else {
                        newEndOffset - 1
                    }
                } else {
                    if (newEndOffset == lastOffset || it.start == newEndOffset - 1) {
                        newEndOffset - 1
                    } else {
                        newEndOffset + 1
                    }
                }
        } else {
            newEndOffset =
                if (handlesCrossed) {
                    if (
                        newStartOffset == lastOffset || it.end == newStartOffset - 1
                    ) {
                        newStartOffset - 1
                    } else {
                        newStartOffset + 1
                    }
                } else {
                    if (newStartOffset == 0 || it.end == newStartOffset + 1) {
                        newStartOffset + 1
                    } else {
                        newStartOffset - 1
                    }
                }
        }
    }
    return TextRange(newStartOffset, newEndOffset)
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
