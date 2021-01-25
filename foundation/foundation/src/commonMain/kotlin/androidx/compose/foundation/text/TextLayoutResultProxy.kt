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
    fun getOffsetForPosition(position: Offset): Int {
        val shiftedOffset = shiftedOffset(position)
        return value.getOffsetForPosition(shiftedOffset)
    }

    fun getLineForVerticalPosition(vertical: Float): Int {
        val shiftedVertical = shiftedOffset(Offset(0f, vertical)).y
        return value.getLineForVerticalPosition(shiftedVertical)
    }

    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int =
        value.getLineEnd(lineIndex, visibleEnd)

    /** Returns true if the screen coordinates position (x,y) corresponds to a character displayed
     * in the view. Returns false when the position is in the empty space of left/right of text.
     */
    fun isPositionOnText(offset: Offset): Boolean {
        val shiftedOffset = shiftedOffset(offset)
        val line = value.getLineForVerticalPosition(shiftedOffset.y)
        return shiftedOffset.x >= value.getLineLeft(line) &&
            shiftedOffset.x <= value.getLineRight(line)
    }

    // Shift offset
    /** Measured bounds of the decoration box and inner text field. Together used to
     * calculate the relative touch offset. Because touches are applied on the decoration box, we
     * need to translate it to the inner text field coordinates.
     */
    var innerTextFieldCoordinates: LayoutCoordinates? = null
    var decorationBoxCoordinates: LayoutCoordinates? = null

    private fun shiftedOffset(offset: Offset): Offset {
        // If offset is outside visible bounds of the inner text field, use visible bounds edges
        val visibleInnerTextFieldRect = innerTextFieldCoordinates?.let { inner ->
            decorationBoxCoordinates?.localBoundingBoxOf(inner)
        } ?: Rect.Zero
        val coercedOffset = offset.coerceIn(visibleInnerTextFieldRect)

        // Translates touch to the inner text field coordinates
        return innerTextFieldCoordinates?.let { innerTextFieldCoordinates ->
            decorationBoxCoordinates?.let { decorationBoxCoordinates ->
                innerTextFieldCoordinates.localPositionOf(decorationBoxCoordinates, coercedOffset)
            }
        } ?: coercedOffset
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