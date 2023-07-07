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

package androidx.compose.foundation.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult

internal class TextLayoutResultProxy(val value: TextLayoutResult) {
    // TextLayoutResult methods
    /**
     * Translates the position of the touch on the screen to the position in text. Because touch
     * is relative to the decoration box, we need to translate it to the inner text field's
     * coordinates first before calculating position of the symbol in text.
     *
     * @param position original position of the gesture relative to the decoration box
     * @param coerceInVisibleBounds if true and original [position] is outside visible bounds
     * of the inner text field, the [position] will be shifted to the closest edge of the inner
     * text field's visible bounds. This is useful when you have a decoration box
     * bigger than the inner text field, so when user touches to the decoration box area, the cursor
     * goes to the beginning or the end of the visible inner text field; otherwise if we put the
     * cursor under the touch in the invisible part of the inner text field, it would scroll to
     * make the cursor visible. This behavior is not needed, and therefore
     * [coerceInVisibleBounds] should be set to false, when the user drags outside visible bounds
     * to make a selection.
     */
    fun getOffsetForPosition(position: Offset, coerceInVisibleBounds: Boolean = true): Int {
        val relativePosition = position
            .let { if (coerceInVisibleBounds) it.coercedInVisibleBoundsOfInputText() else it }
            .relativeToInputText()
        return value.getOffsetForPosition(relativePosition)
    }

    fun getLineForVerticalPosition(vertical: Float): Int {
        val relativeVertical = Offset(0f, vertical)
            .coercedInVisibleBoundsOfInputText()
            .relativeToInputText().y
        return value.getLineForVerticalPosition(relativeVertical)
    }

    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int =
        value.getLineEnd(lineIndex, visibleEnd)

    /** Returns true if the screen coordinates position (x,y) corresponds to a character displayed
     * in the view. Returns false when the position is in the empty space of left/right of text.
     */
    fun isPositionOnText(offset: Offset): Boolean {
        val relativeOffset = offset.coercedInVisibleBoundsOfInputText().relativeToInputText()
        val line = value.getLineForVerticalPosition(relativeOffset.y)
        return relativeOffset.x >= value.getLineLeft(line) &&
            relativeOffset.x <= value.getLineRight(line)
    }

    // Shift offset
    /** Measured bounds of the decoration box and inner text field. Together used to
     * calculate the relative touch offset. Because touches are applied on the decoration box, we
     * need to translate it to the inner text field coordinates.
     */
    var innerTextFieldCoordinates: LayoutCoordinates? = null
    var decorationBoxCoordinates: LayoutCoordinates? = null

    /**
     * Translates the click happened on the decoration box to the position in the inner text
     * field coordinates. This relative position is then used to determine symbol position in
     * text using TextLayoutResult object.
     */
    private fun Offset.relativeToInputText(): Offset {
        // Translates touch to the inner text field coordinates
        return innerTextFieldCoordinates?.let { innerTextFieldCoordinates ->
            decorationBoxCoordinates?.let { decorationBoxCoordinates ->
                if (innerTextFieldCoordinates.isAttached && decorationBoxCoordinates.isAttached) {
                    innerTextFieldCoordinates.localPositionOf(decorationBoxCoordinates, this)
                } else {
                    this
                }
            }
        } ?: this
    }

    /**
     * If click on the decoration box happens outside visible inner text field, coerce the click
     * position to the visible edges of the inner text field.
     */
    private fun Offset.coercedInVisibleBoundsOfInputText(): Offset {
        // If offset is outside visible bounds of the inner text field, use visible bounds edges
        val visibleInnerTextFieldRect =
            innerTextFieldCoordinates?.let { innerTextFieldCoordinates ->
                if (innerTextFieldCoordinates.isAttached) {
                    decorationBoxCoordinates?.localBoundingBoxOf(innerTextFieldCoordinates)
                } else {
                    Rect.Zero
                }
            } ?: Rect.Zero
        return this.coerceIn(visibleInnerTextFieldRect)
    }
}

private fun Offset.coerceIn(rect: Rect): Offset {
    val xOffset = when {
        x < rect.left -> rect.left
        x > rect.right -> rect.right
        else -> x
    }
    val yOffset = when {
        y < rect.top -> rect.top
        y > rect.bottom -> rect.bottom
        else -> y
    }
    return Offset(xOffset, yOffset)
}