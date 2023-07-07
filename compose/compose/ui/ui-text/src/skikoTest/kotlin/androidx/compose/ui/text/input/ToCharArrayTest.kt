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

import kotlin.test.Test
import kotlin.test.assertEquals

class ToCharArrayTest {
    @Test
    fun start_from_0() {
        val charArray = CharArray(2)
        "ab".toCharArray(charArray, 0, 0, 1)
        assertEquals('a', charArray[0])
    }

    @Test
    fun start_from_1() {
        val charArray = CharArray(2)
        "ab".toCharArray(charArray, 1, 1, 2)
        assertEquals('b', charArray[1])
    }

}
