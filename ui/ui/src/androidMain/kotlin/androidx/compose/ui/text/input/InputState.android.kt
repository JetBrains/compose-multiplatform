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

package androidx.compose.ui.text.input

import android.view.inputmethod.ExtractedText

internal fun TextFieldValue.toExtractedText(): ExtractedText {
    val res = ExtractedText()
    res.text = text
    res.startOffset = 0
    res.partialEndOffset = text.length
    res.partialStartOffset = -1 // -1 means full text
    res.selectionStart = selection.min
    res.selectionEnd = selection.max
    res.flags = if ('\n' in text) 0 else ExtractedText.FLAG_SINGLE_LINE
    return res
}
