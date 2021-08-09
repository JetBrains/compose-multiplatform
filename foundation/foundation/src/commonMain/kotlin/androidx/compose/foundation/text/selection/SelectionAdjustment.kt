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

import androidx.compose.foundation.text.getParagraphBoundary
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

/**
 * Selection can be adjusted depends on context. For example, in touch mode dragging after a long
 * press adjusts selection by word. But selection by dragging handles is character precise
 * without adjustments. With a mouse, double-click selects by words and triple-clicks by paragraph.
 * @see [SelectionRegistrar.notifySelectionUpdate]
 */
internal interface SelectionAdjustment {

    /**
     * The callback function that is called once a new selection arrives, the return value of
     * this function will be the final selection range on the corresponding [Selectable].
     *
     * @param textLayoutResult the [TextLayoutResult] of the involved [Selectable].
     * @param newRawSelectionRange the new selection range computed from the selection handle
     * position on screen.
     * @param previousRawSelectionRange the previous raw selection range.
     * @param previousAdjustedSelection the previous adjusted selection range, or the selection
     * range to be updated.
     */
    fun adjust(
        textLayoutResult: TextLayoutResult,
        newRawSelectionRange: TextRange,
        previousRawSelectionRange: TextRange?,
        previousAdjustedSelection: TextRange?,
        isStartHandle: Boolean
    ): TextRange

    companion object {
        /**
         * The selection adjustment that does nothing and directly return the input raw
         * selection range.
         */
        val None = object : SelectionAdjustment {
            override fun adjust(
                textLayoutResult: TextLayoutResult,
                newRawSelectionRange: TextRange,
                previousRawSelectionRange: TextRange?,
                previousAdjustedSelection: TextRange?,
                isStartHandle: Boolean
            ): TextRange = newRawSelectionRange
        }

        /**
         * The character based selection. It normally won't change the raw selection range except
         * when the input raw selection range is collapsed. In this case, it will always make
         * sure at least one character is selected.
         * When the given raw selection range is collapsed:
         * a) it will always try to adjust the changing selection boundary(base on the value of
         * isStartHandle) and makes sure the other boundary remains the same after the adjustment
         * b) if the previous selection range is reversed, it will try to make the adjusted
         * selection range reversed as well, and vice versa.
         */
        val Character = object : SelectionAdjustment {
            override fun adjust(
                textLayoutResult: TextLayoutResult,
                newRawSelectionRange: TextRange,
                previousRawSelectionRange: TextRange?,
                previousAdjustedSelection: TextRange?,
                isStartHandle: Boolean
            ): TextRange {
                return if (newRawSelectionRange.collapsed) {
                    // If there isn't any selection before, we assume handles are not crossed.
                    val previousHandlesCrossed = previousAdjustedSelection?.reversed ?: false
                    ensureAtLeastOneChar(
                        offset = newRawSelectionRange.start,
                        lastOffset = textLayoutResult.layoutInput.text.lastIndex,
                        isStartHandle = isStartHandle,
                        previousHandlesCrossed = previousHandlesCrossed
                    )
                } else {
                    newRawSelectionRange
                }
            }
        }

        /**
         * The word based selection adjustment. It will adjust the raw input selection such that
         * the selection boundary snap to the word boundary. It will always expand the raw input
         * selection range to the closest word boundary. If the raw selection is reversed, it
         * will always return a reversed selection, and vice versa.
         */
        val Word = object : SelectionAdjustment {
            override fun adjust(
                textLayoutResult: TextLayoutResult,
                newRawSelectionRange: TextRange,
                previousRawSelectionRange: TextRange?,
                previousAdjustedSelection: TextRange?,
                isStartHandle: Boolean
            ): TextRange {
                return adjustByBoundary(
                    textLayoutResult = textLayoutResult,
                    newRawSelection = newRawSelectionRange,
                    boundaryFun = textLayoutResult::getWordBoundary
                )
            }
        }

        /**
         * The paragraph based selection adjustment. It will adjust the raw input selection such
         * that the selection boundary snap to the paragraph boundary. It will always expand the
         * raw input selection range to the closest paragraph boundary. If the raw selection is
         * reversed, it will always return a reversed selection, and vice versa.
         */
        val Paragraph = object : SelectionAdjustment {
            override fun adjust(
                textLayoutResult: TextLayoutResult,
                newRawSelectionRange: TextRange,
                previousRawSelectionRange: TextRange?,
                previousAdjustedSelection: TextRange?,
                isStartHandle: Boolean
            ): TextRange {
                val boundaryFun = textLayoutResult.layoutInput.text::getParagraphBoundary
                return adjustByBoundary(
                    textLayoutResult = textLayoutResult,
                    newRawSelection = newRawSelectionRange,
                    boundaryFun = boundaryFun
                )
            }
        }

        private fun adjustByBoundary(
            textLayoutResult: TextLayoutResult,
            newRawSelection: TextRange,
            boundaryFun: (Int) -> TextRange
        ): TextRange {
            if (textLayoutResult.layoutInput.text.isEmpty()) {
                return TextRange.Zero
            }
            val maxOffset = textLayoutResult.layoutInput.text.lastIndex
            val startBoundary = boundaryFun(newRawSelection.start.coerceIn(0, maxOffset))
            val endBoundary = boundaryFun(newRawSelection.end.coerceIn(0, maxOffset))

            // If handles are not crossed, start should be snapped to the start of the word
            // containing the start offset, and end should be snapped to the end of the word
            // containing the end offset. If handles are crossed, start should be snapped to the
            // end of the word containing the start offset, and end should be snapped to the start
            // of the word containing the end offset.
            val start = if (newRawSelection.reversed) startBoundary.end else startBoundary.start
            val end = if (newRawSelection.reversed) endBoundary.start else endBoundary.end
            return TextRange(start, end)
        }

        /**
         * A special version of character based selection that accelerates the selection update
         * with word based selection. In short, it expands by word and shrinks by character.
         * Here is more details of the behavior:
         * 1. When previous selection is null, it will use word based selection.
         * 2. When the start/end offset has moved to a different line, it will use word
         * based selection.
         * 3. When the selection is shrinking, it behave same as the character based selection.
         * Shrinking means that the start/end offset is moving in the direction that makes
         * selected text shorter.
         * 4. The selection boundary is expanding,
         *  a.if the previous start/end offset is not a word boundary, use character based
         * selection.
         *  b.if the previous start/end offset is a word boundary, use word based selection.
         *
         *  Notice that this selection adjustment assumes that when isStartHandle is ture, only
         *  start handle is moving(or unchanged), and vice versa.
         */
        val CharacterWithWordAccelerate = object : SelectionAdjustment {
            override fun adjust(
                textLayoutResult: TextLayoutResult,
                newRawSelectionRange: TextRange,
                previousRawSelectionRange: TextRange?,
                previousAdjustedSelection: TextRange?,
                isStartHandle: Boolean
            ): TextRange {
                // Previous selection is null. We start a word based selection.
                if (
                    previousRawSelectionRange == null ||
                    previousAdjustedSelection == null
                ) {
                    return Word.adjust(
                        textLayoutResult = textLayoutResult,
                        newRawSelectionRange = newRawSelectionRange,
                        previousRawSelectionRange = previousRawSelectionRange,
                        previousAdjustedSelection = previousAdjustedSelection,
                        isStartHandle = isStartHandle
                    )
                }

                // The new selection is collapsed, ensure at least one char is selected.
                if (newRawSelectionRange.collapsed) {
                    return ensureAtLeastOneChar(
                        offset = newRawSelectionRange.start,
                        lastOffset = textLayoutResult.layoutInput.text.lastIndex,
                        isStartHandle = isStartHandle,
                        previousHandlesCrossed = previousAdjustedSelection.reversed
                    )
                }

                // Notice that we assume only one selection boundary is really updated. So we can
                // directly pass previousAdjustedSelection.end as otherBoundOffset.
                val start = updateSelectionBoundary(
                    textLayoutResult,
                    newRawSelectionRange.start,
                    previousRawSelectionRange.start,
                    previousAdjustedSelection.start,
                    previousAdjustedSelection.end,
                    true,
                    previousRawSelectionRange.reversed
                )
                val end = updateSelectionBoundary(
                    textLayoutResult,
                    newRawSelectionRange.end,
                    previousRawSelectionRange.end,
                    previousAdjustedSelection.end,
                    previousAdjustedSelection.start,
                    false,
                    previousRawSelectionRange.reversed
                )
                return TextRange(start, end)
            }

            /**
             * Helper function that updates start or end offset of the selection. It implements the
             * "expand by word and shrink by character behavior".
             *
             * @param textLayoutResult the text layout result
             * @param newRawOffset the new raw offset of the selection boundary.
             * @param previousRawOffset the previous raw offset of the selection boundary.
             * @param previousAdjustedOffset the previous final/adjusted offset. It's the current
             * @param otherBoundaryOffset the offset of the other selection boundary. It is used
             * to avoid empty selection in word based selection mode.
             * selection boundary.
             * @param isStart whether it's updating the selection start or end boundary.
             * @param isReversed whether the selection is reversed or not. We use
             * this information to determine if the selection is expanding or shrinking.
             */
            private fun updateSelectionBoundary(
                textLayoutResult: TextLayoutResult,
                newRawOffset: Int,
                previousRawOffset: Int,
                previousAdjustedOffset: Int,
                otherBoundaryOffset: Int,
                isStart: Boolean,
                isReversed: Boolean
            ): Int {
                // The raw offset didn't change, directly return the previous adjusted start offset.
                if (newRawOffset == previousRawOffset) {
                    return previousAdjustedOffset
                }

                val currentLine = textLayoutResult.getLineForOffset(newRawOffset)
                val previousLine = textLayoutResult.getLineForOffset(previousAdjustedOffset)

                // The updating selection boundary has crossed a line, use word based selection.
                if (currentLine != previousLine) {
                    return snapToWordBoundary(
                        textLayoutResult = textLayoutResult,
                        newRawOffset = newRawOffset,
                        currentLine = currentLine,
                        otherBoundaryOffset = otherBoundaryOffset,
                        isStart = isStart,
                        isReversed = isReversed
                    )
                }

                // Check if the start or end selection boundary is expanding. If it's shrinking,
                // use character based selection.
                val isExpanding =
                    isExpanding(newRawOffset, previousRawOffset, isStart, isReversed)
                if (!isExpanding) {
                    return newRawOffset
                }

                // If the previous start/end offset is not at a word boundary, which is indicating
                // that start/end offset is updating within a word. In this case, it still uses
                // character based selection.
                if (!textLayoutResult.isAtWordBoundary(previousAdjustedOffset)) {
                    return newRawOffset
                }

                // At this point we know, the updating start/end offset is still in the same line,
                // it's expanding the selection, and it's not updating within a word. It should
                // use word based selection.
                return snapToWordBoundary(
                    textLayoutResult = textLayoutResult,
                    newRawOffset = newRawOffset,
                    currentLine = currentLine,
                    otherBoundaryOffset = otherBoundaryOffset,
                    isStart = isStart,
                    isReversed = isReversed
                )
            }

            private fun snapToWordBoundary(
                textLayoutResult: TextLayoutResult,
                newRawOffset: Int,
                currentLine: Int,
                otherBoundaryOffset: Int,
                isStart: Boolean,
                isReversed: Boolean
            ): Int {
                val wordBoundary = textLayoutResult.getWordBoundary(newRawOffset)

                // In the case where the target word crosses multiple lines due to hyphenation or
                // being too long, we use the line start/end to keep the adjusted offset at the
                // same line.
                val wordStartLine = textLayoutResult.getLineForOffset(wordBoundary.start)
                val start = if (wordStartLine == currentLine) {
                    wordBoundary.start
                } else {
                    textLayoutResult.getLineStart(currentLine)
                }

                val wordEndLine = textLayoutResult.getLineForOffset(wordBoundary.end)
                val end = if (wordEndLine == currentLine) {
                    wordBoundary.end
                } else {
                    textLayoutResult.getLineEnd(currentLine)
                }

                // If one of the word boundary is exactly same as the otherBoundaryOffset, we
                // can't snap to this word boundary since it will result in an empty selection
                // range.
                if (start == otherBoundaryOffset) {
                    return end
                }
                if (end == otherBoundaryOffset) {
                    return start
                }

                val threshold = (start + end) / 2
                return if (isStart xor isReversed) {
                    // In this branch when:
                    // 1. selection is updating the start offset, and selection is not reversed.
                    // 2. selection is updating the end offset, and selection is reversed.
                    if (newRawOffset <= threshold) {
                        start
                    } else {
                        end
                    }
                } else {
                    // In this branch when:
                    // 1. selection is updating the end offset, and selection is not reversed.
                    // 2. selection is updating the start offset, and selection is reversed.
                    if (newRawOffset >= threshold) {
                        end
                    } else {
                        start
                    }
                }
            }

            private fun TextLayoutResult.isAtWordBoundary(offset: Int): Boolean {
                val wordBoundary = getWordBoundary(offset)
                return offset == wordBoundary.start || offset == wordBoundary.end
            }

            private fun isExpanding(
                newRawOffset: Int,
                previousRawOffset: Int,
                isStart: Boolean,
                previousReversed: Boolean
            ): Boolean {
                if (newRawOffset == previousRawOffset) {
                    return false
                }
                return if (isStart xor previousReversed) {
                    newRawOffset < previousRawOffset
                } else {
                    newRawOffset > previousRawOffset
                }
            }
        }
    }
}

/**
 * This method adjusts the raw start and end offset and bounds the selection to one character. The
 * logic of bounding evaluates the last selection result, which handle is being dragged, and if
 * selection reaches the boundary.
 *
 * @param offset unprocessed start and end offset calculated directly from input position, in
 * this case start and offset equals to each other.
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param isStartHandle true if the start handle is being dragged
 * @param previousHandlesCrossed true if the selection handles are crossed in the previous
 * selection. This function will try to maintain the handle cross state. This can help make
 * selection stable.
 *
 * @return the adjusted [TextRange].
 */
internal fun ensureAtLeastOneChar(
    offset: Int,
    lastOffset: Int,
    isStartHandle: Boolean,
    previousHandlesCrossed: Boolean
): TextRange {
    // When lastOffset is 0, it can only return an empty TextRange.
    // When previousSelection is null, it won't start a selection and return an empty TextRange.
    if (lastOffset == 0) return TextRange(offset, offset)

    // When offset is at the boundary, the handle that is not dragged should be at [offset]. Here
    // the other handle's position is computed accordingly.
    if (offset == 0) {
        return if (isStartHandle) {
            TextRange(1, 0)
        } else {
            TextRange(0, 1)
        }
    }

    if (offset == lastOffset) {
        return if (isStartHandle) {
            TextRange(lastOffset - 1, lastOffset)
        } else {
            TextRange(lastOffset, lastOffset - 1)
        }
    }

    // In other cases, this function will try to maintain the current cross handle states.
    // Only in this way the selection can be stable.
    return if (isStartHandle) {
        if (!previousHandlesCrossed) {
            // Handle is NOT crossed, and the start handle is dragged.
            TextRange(offset - 1, offset)
        } else {
            // Handle is crossed, and the start handle is dragged.
            TextRange(offset + 1, offset)
        }
    } else {
        if (!previousHandlesCrossed) {
            // Handle is NOT crossed, and the end handle is dragged.
            TextRange(offset, offset + 1)
        } else {
            // Handle is crossed, and the end handle is dragged.
            TextRange(offset, offset - 1)
        }
    }
}