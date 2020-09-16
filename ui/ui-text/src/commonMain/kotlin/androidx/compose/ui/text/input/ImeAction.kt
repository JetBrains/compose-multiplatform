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

/**
 * Enums used for indicating IME action.
 *
 * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_MASK_ACTION>
 */
enum class ImeAction {
    /**
     * An IME action used to represent that any IME action is associated.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_UNSPECIFIED>
     */
    Unspecified,

    /**
     * An IME action used to represent that no IME action is available in editor.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_NONE>
     */
    NoAction,

    /**
     * An IME action used to represent that the "enter" key works as "go" action.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_GO>
     */
    Go,

    /**
     * An IME action used to represent that the "enter" key works as "search" action.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_SEARCH>
     */
    Search,

    /**
     * An IME action used to represent that the "enter" key works as "send" action.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_SEND>
     */
    Send,

    /**
     * An IME action used to represent that the "enter" key works as "previous" action.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_PREVIOUS>
     */
    Previous,

    /**
     * An IME action used to represent that the "enter" key works as "next" action.
     *
     * https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_NEXT
     */
    Next,

    /**
     * An IME action used to represent that the "enter" key works as "done" action.
     *
     * @see <https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html#IME_ACTION_DONE>
     */
    Done
}