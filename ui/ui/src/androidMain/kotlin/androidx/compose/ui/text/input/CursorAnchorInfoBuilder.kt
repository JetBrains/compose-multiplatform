/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.graphics.Matrix
import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection

/**
 * Helper function to build [CursorAnchorInfo](https://developer.android.com/reference/android/view/inputmethod/CursorAnchorInfo).
 *
 * @param textFieldValue required to set text, composition and selection information into the
 * CursorAnchorInfo.
 * @param textLayoutResult TextLayoutResult for the [textFieldValue] used to enter cursor and
 * character information
 * @param matrix Matrix used to convert local coordinates to global coordinates.
 */
internal fun CursorAnchorInfo.Builder.build(
    textFieldValue: TextFieldValue,
    textLayoutResult: TextLayoutResult,
    matrix: Matrix
): CursorAnchorInfo {
    reset()

    setMatrix(matrix)

    val selectionStart = textFieldValue.selection.min
    val selectionEnd = textFieldValue.selection.max
    setSelectionRange(selectionStart, selectionEnd)

    setInsertionMarker(selectionStart, textLayoutResult)

    // set composition
    val compositionStart = textFieldValue.composition?.min ?: -1
    val compositionEnd = textFieldValue.composition?.max ?: -1

    if (compositionStart in 0 until compositionEnd) {
        setComposingText(
            compositionStart,
            textFieldValue.text.subSequence(compositionStart, compositionEnd)
        )
    }

    return build()
}

private fun CursorAnchorInfo.Builder.setInsertionMarker(
    selectionStart: Int,
    textLayoutResult: TextLayoutResult
): CursorAnchorInfo.Builder {
    if (selectionStart < 0) return this

    val cursorRect = textLayoutResult.getCursorRect(selectionStart)
    val isRtl = textLayoutResult.getBidiRunDirection(selectionStart) == ResolvedTextDirection.Rtl

    var flags = 0
    if (isRtl) flags = flags or CursorAnchorInfo.FLAG_IS_RTL

    // Sets the location of the text insertion point (zero width cursor) as a rectangle in local
    // coordinates.
    setInsertionMarkerLocation(
        cursorRect.left,
        cursorRect.top,
        cursorRect.bottom,
        cursorRect.bottom,
        flags
    )

    return this
}