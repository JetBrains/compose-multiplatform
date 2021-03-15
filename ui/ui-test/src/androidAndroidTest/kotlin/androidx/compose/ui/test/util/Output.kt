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

package androidx.compose.ui.test.util

import com.google.common.truth.Truth

internal fun obfuscateNodesInfo(message: String): String {
    var result = message.trim()
    // Semantics uses a static object to generate ids. This object lives between tests. So the order
    // of tests affects the IDs assigned. To prevent flakes we obfuscate the IDs.
    result = result.replace("#[0-9]+ at".toRegex(), "#X at")
    // We also obfuscate pixel values just to reduce dependency on layout changes.
    result = result.replace("[0-9]+\\.[0-9]+".toRegex(), "X")
    return result
}

internal fun expectErrorMessage(expectedErrorMessage: String, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionError) {
        val received = obfuscateNodesInfo(e.localizedMessage!!)
        Truth.assertThat(received).isEqualTo(expectedErrorMessage.trim())
        return
    }

    throw AssertionError("No AssertionError thrown!")
}

internal fun expectErrorMessageStartsWith(expectedErrorMessage: String, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionError) {
        val received = obfuscateNodesInfo(e.localizedMessage!!)
        Truth.assertThat(received).startsWith(expectedErrorMessage.trim())
        return
    }

    throw AssertionError("No AssertionError thrown!")
}