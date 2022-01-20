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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.max

internal class MultiWidgetSelectionDelegate(
    override val selectableId: Long,
    private val coordinatesCallback: () -> LayoutCoordinates?,
    private val layoutResultCallback: () -> TextLayoutResult?
) : Selectable {

    override fun updateSelection(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset?,
        isStartHandle: Boolean,
        containerLayoutCoordinates: LayoutCoordinates,
        adjustment: SelectionAdjustment,
        previousSelection: Selection?
    ): Pair<Selection?, Boolean> {
        require(
            previousSelection == null || (
                selectableId == previousSelection.start.selectableId &&
                    selectableId == previousSelection.end.selectableId
                )
        ) {
            "The given previousSelection doesn't belong to this selectable."
        }
        val layoutCoordinates = getLayoutCoordinates() ?: return Pair(null, false)
        val textLayoutResult = layoutResultCallback() ?: return Pair(null, false)

        val relativePosition = containerLayoutCoordinates.localPositionOf(
            layoutCoordinates, Offset.Zero
        )
        val localStartPosition = startHandlePosition - relativePosition
        val localEndPosition = endHandlePosition - relativePosition
        val localPreviousHandlePosition = previousHandlePosition?.let { it - relativePosition }

        return getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = localStartPosition,
            endHandlePosition = localEndPosition,
            previousHandlePosition = localPreviousHandlePosition,
            selectableId = selectableId,
            adjustment = adjustment,
            previousSelection = previousSelection,
            isStartHandle = isStartHandle
        )
    }

    override fun getSelectAllSelection(): Selection? {
        val textLayoutResult = layoutResultCallback() ?: return null
        val newSelectionRange = TextRange(0, textLayoutResult.layoutInput.text.length)

        return getAssembledSelectionInfo(
            newSelectionRange = newSelectionRange,
            handlesCrossed = false,
            selectableId = selectableId,
            textLayoutResult = textLayoutResult
        )
    }

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset {
        // Check if the selection handle's selectable is the current selectable.
        if (isStartHandle && selection.start.selectableId != this.selectableId ||
            !isStartHandle && selection.end.selectableId != this.selectableId
        ) {
            return Offset.Zero
        }

        if (getLayoutCoordinates() == null) return Offset.Zero

        val textLayoutResult = layoutResultCallback() ?: return Offset.Zero
        return getSelectionHandleCoordinates(
            textLayoutResult = textLayoutResult,
            offset = if (isStartHandle) selection.start.offset else selection.end.offset,
            isStart = isStartHandle,
            areHandlesCrossed = selection.handlesCrossed
        )
    }

    override fun getLayoutCoordinates(): LayoutCoordinates? {
        val layoutCoordinates = coordinatesCallback()
        if (layoutCoordinates == null || !layoutCoordinates.isAttached) return null
        return layoutCoordinates
    }

    override fun getText(): AnnotatedString {
        val textLayoutResult = layoutResultCallback() ?: return AnnotatedString("")
        return textLayoutResult.layoutInput.text
    }

    override fun getBoundingBox(offset: Int): Rect {
        val textLayoutResult = layoutResultCallback() ?: return Rect.Zero
        val textLength = textLayoutResult.layoutInput.text.length
        if (textLength < 1) return Rect.Zero
        return textLayoutResult.getBoundingBox(
            offset.coerceIn(0, textLength - 1)
        )
    }

    override fun getRangeOfLineContaining(offset: Int): TextRange {
        val textLayoutResult = layoutResultCallback() ?: return TextRange.Zero
        val textLength = textLayoutResult.layoutInput.text.length
        if (textLength < 1) return TextRange.Zero
        val line = textLayoutResult.getLineForOffset(offset.coerceIn(0, textLength - 1))
        return TextRange(
            start = textLayoutResult.getLineStart(line),
            end = textLayoutResult.getLineEnd(line, visibleEnd = true)
        )
    }
}

/**
 * Return information about the current selection in the Text.
 *
 * @param textLayoutResult a result of the text layout.
 * @param startHandlePosition The new positions of the moving selection handle.
 * @param previousHandlePosition The old position of the moving selection handle since the last update.
 * @param endHandlePosition the position of the selection handle that is not moving.
 * @param selectableId the id of this [Selectable].
 * @param adjustment the [SelectionAdjustment] used to process the raw selection range.
 * @param previousSelection the previous text selection.
 * @param isStartHandle whether the moving selection is the start selection handle.
 *
 * @return a pair consistent of updated [Selection] and a boolean representing whether the
 * movement is consumed.
 */
internal fun getTextSelectionInfo(
    textLayoutResult: TextLayoutResult,
    startHandlePosition: Offset,
    endHandlePosition: Offset,
    previousHandlePosition: Offset?,
    selectableId: Long,
    adjustment: SelectionAdjustment,
    previousSelection: Selection? = null,
    isStartHandle: Boolean = true
): Pair<Selection?, Boolean> {

    val bounds = Rect(
        0.0f,
        0.0f,
        textLayoutResult.size.width.toFloat(),
        textLayoutResult.size.height.toFloat()
    )

    val isSelected =
        SelectionMode.Vertical.isSelected(bounds, startHandlePosition, endHandlePosition)

    if (!isSelected) {
        return Pair(null, false)
    }

    val rawStartHandleOffset = getOffsetForPosition(textLayoutResult, bounds, startHandlePosition)
    val rawEndHandleOffset = getOffsetForPosition(textLayoutResult, bounds, endHandlePosition)
    val rawPreviousHandleOffset = previousHandlePosition?.let {
        getOffsetForPosition(textLayoutResult, bounds, it)
    } ?: -1

    val adjustedTextRange = adjustment.adjust(
        textLayoutResult = textLayoutResult,
        newRawSelectionRange = TextRange(rawStartHandleOffset, rawEndHandleOffset),
        previousHandleOffset = rawPreviousHandleOffset,
        isStartHandle = isStartHandle,
        previousSelectionRange = previousSelection?.toTextRange()
    )
    val newSelection = getAssembledSelectionInfo(
        newSelectionRange = adjustedTextRange,
        handlesCrossed = adjustedTextRange.reversed,
        selectableId = selectableId,
        textLayoutResult = textLayoutResult
    )

    // Determine whether the movement is consumed by this Selectable.
    // If the selection has  changed, the movement is consumed.
    // And there are also cases where the selection stays the same but selection handle raw
    // offset has changed.(Usually this happen because of adjustment like SelectionAdjustment.Word)
    // In this case we also consider the movement being consumed.
    val selectionUpdated = newSelection != previousSelection
    val handleUpdated = if (isStartHandle) {
        rawStartHandleOffset != rawPreviousHandleOffset
    } else {
        rawEndHandleOffset != rawPreviousHandleOffset
    }
    val consumed = handleUpdated || selectionUpdated
    return Pair(newSelection, consumed)
}

internal fun getOffsetForPosition(
    textLayoutResult: TextLayoutResult,
    bounds: Rect,
    position: Offset
): Int {
    val length = textLayoutResult.layoutInput.text.length
    return if (bounds.contains(position)) {
        textLayoutResult.getOffsetForPosition(position).coerceIn(0, length)
    } else {
        val value = SelectionMode.Vertical.compare(position, bounds)
        if (value < 0) 0 else length
    }
}

/**
 * [Selection] contains a lot of parameters. It looks more clean to assemble an object of this
 * class in a separate method.
 *
 * @param newSelectionRange the final new selection text range.
 * @param handlesCrossed true if the selection handles are crossed
 * @param selectableId the id of the current [Selectable] for which the [Selection] is being
 * calculated
 * @param textLayoutResult a result of the text layout.
 *
 * @return an assembled object of [Selection] using the offered selection info.
 */
private fun getAssembledSelectionInfo(
    newSelectionRange: TextRange,
    handlesCrossed: Boolean,
    selectableId: Long,
    textLayoutResult: TextLayoutResult
): Selection {
    return Selection(
        start = Selection.AnchorInfo(
            direction = textLayoutResult.getBidiRunDirection(newSelectionRange.start),
            offset = newSelectionRange.start,
            selectableId = selectableId
        ),
        end = Selection.AnchorInfo(
            direction = textLayoutResult.getBidiRunDirection(max(newSelectionRange.end - 1, 0)),
            offset = newSelectionRange.end,
            selectableId = selectableId
        ),
        handlesCrossed = handlesCrossed
    )
}
