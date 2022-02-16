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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.findFollowingBreak
import androidx.compose.foundation.text.findPrecedingBreak

import kotlin.test.Test
import kotlin.test.assertEquals

class StringHelpersTest {
    val complexString = "\uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0"

    @Test
    fun StringHelpersTest_findFollowingBreak() {
        val result = complexString.findFollowingBreak(0)
        assertEquals(result, 7)
    }

    @Test
    fun StringHelpersTest_findPrecedingBreak() {
        val result = complexString.findPrecedingBreak(7)
        assertEquals(result, 0)
    }
}