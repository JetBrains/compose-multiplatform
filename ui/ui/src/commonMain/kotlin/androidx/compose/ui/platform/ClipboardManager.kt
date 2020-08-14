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

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString

/**
 * Interface for managing the Clipboard.
 */
interface ClipboardManager {
    /**
     * This method put the text into the Clipboard.
     *
     * @param annotatedString The [AnnotatedString] to be put into Clipboard.
     */
    fun setText(annotatedString: AnnotatedString)

    /**
     * This method get the text from the Clipboard.
     *
     * @return The text in the Clipboard.
     * It could be null due to 2 reasons: 1. Clipboard is empty; 2. Cannot convert the
     * [CharSequence] text in Clipboard to [AnnotatedString].
     */
    fun getText(): AnnotatedString?
}
