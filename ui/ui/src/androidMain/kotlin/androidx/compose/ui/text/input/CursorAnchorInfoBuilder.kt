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

import android.view.inputmethod.CursorAnchorInfo

/**
 * Helper function to build [CursorAnchorInfo](https://developer.android.com/reference/android/view/inputmethod/CursorAnchorInfo).
 *
 * @param textFieldValue required to set text, composition and selection information into the
 * CursorAnchorInfo.
 */
internal fun CursorAnchorInfo.Builder.build(
    textFieldValue: TextFieldValue
): CursorAnchorInfo {
    reset()

    val selectionStart = textFieldValue.selection.min
    val selectionEnd = textFieldValue.selection.max
    setSelectionRange(selectionStart, selectionEnd)

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
