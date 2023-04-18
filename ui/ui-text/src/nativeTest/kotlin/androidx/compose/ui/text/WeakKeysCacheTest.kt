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

package androidx.compose.ui.text

import kotlin.native.internal.GC
import kotlin.native.ref.WeakReference
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeakKeysCacheTest {
    data class MyKey(val v: Int)

    @Test
    fun clearOnGC() {
        val cache = WeakKeysCache<MyKey, Int>()
        var created = 0

        // Accessing to weak reference creates strong one in stack now.
        // So to make GC work, wrap it into separate function.
        fun checkCache(expected: Int) {
            assertEquals(expected, cache.get(MyKey(42)) { ++created })
            assertEquals(expected, cache.get(MyKey(42)) { ++created })
        }
        checkCache(1)
        GC.collect()
        checkCache(2)
    }

}