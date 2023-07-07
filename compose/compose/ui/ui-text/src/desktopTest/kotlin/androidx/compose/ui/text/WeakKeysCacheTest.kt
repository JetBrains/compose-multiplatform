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

import kotlin.test.assertEquals
import org.junit.Test

class WeakKeysCacheTest {
    data class MyKey(val v: Int)

    @Test
    fun clearOnGC() {
        val cache = WeakKeysCache<MyKey, Int>()
        var created = 0
        assertEquals(1, cache.get(MyKey(42)) { ++created })
        assertEquals(1, cache.get(MyKey(42)) { ++created })
        System.gc()
        assertEquals(2, cache.get(MyKey(42)) { ++created })
        assertEquals(2, cache.get(MyKey(42)) { ++created })
    }
}