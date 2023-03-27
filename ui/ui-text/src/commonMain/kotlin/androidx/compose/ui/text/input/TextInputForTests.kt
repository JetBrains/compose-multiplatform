/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.text.ExperimentalTextApi

/**
 * Defines additional operations that can be performed on text editors by UI tests that aren't
 * available as semantics actions. Tests call these methods indirectly, by the various `perform*`
 * extension functions on `SemanticsNodeInteraction`. [PlatformTextInputAdapter]s implement support
 * for these operations by returning instances of this interface from
 * [PlatformTextInputAdapter.inputForTests].
 *
 * Implementations of this interface should perform the requested operations at the lowest level
 * as possible (as close to the system calls as possible) to exercise as much of the production code
 * as possible. E.g. they should not operate directly on the text buffer, but emulate the calls
 * the platform would make if the user were performing these operations via the IME.
 */
// If new methods need to be added to this interface to support additional testing APIs, they should
// be given default implementations that throw UnsupportedOperationExceptions. This is not a concern
// for backwards compatibility because it simply means that tests may not use new perform* methods
// on older implementations that haven't linked against the newer version of Compose.
@ExperimentalTextApi
interface TextInputForTests {

    /**
     * Sends the given text to this node in similar way to IME. The text should be inserted at the
     * current cursor position.
     *
     * @param text Text to send.
     */
    fun inputTextForTest(text: String)

    /**
     * Performs the submit action configured on the current node, if any.
     *
     * On Android, this is the IME action.
     */
    // TODO(b/269633168, b/269633506) Remove and implement using semantics instead.
    @ExperimentalTextApi
    fun submitTextForTest()
}