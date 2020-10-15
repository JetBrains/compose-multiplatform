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

package androidx.compose.ui.text.input

/**
 * Options to request software keyboard to capitalize the text. Applies to languages which
 * has upper-case and lower-case letters.
 */
enum class KeyboardCapitalization {
    /**
     * Do not auto-capitalize text.
     */
    None,

    /**
     * Capitalize all characters.
     */
    Characters,

    /**
     * Capitalize the first character of every word.
     */
    Words,

    /**
     * Capitalize the first character of each sentence.
     */
    Sentences
}