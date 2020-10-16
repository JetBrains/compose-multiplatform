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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * The keyboard configuration options for text field. It is not guaranteed if a software keyboard
 * will comply with the options provided here.
 *
 * @param singleLine signals the keyboard that the text field is single line and keyboard should
 * not show enter action.
 * @param capitalization signals the keyboard whether to automatically capitalize characters,
 * words or sentences.
 */
@ExperimentalTextApi
@Immutable
data class KeyboardOptions(
    val singleLine: Boolean = false,
    val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    companion object {
        val Default = KeyboardOptions()
    }
}