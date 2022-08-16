/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PackingTests {
    @Test
    fun testShorts() {
        val a = 123
        val b = 345
        val c = 789
        val d = 999
        val packed = packShorts(a.toShort(), b.toShort(), c.toShort(), d.toShort())
        assertEquals(a, unpackShort1(packed))
        assertEquals(b, unpackShort2(packed))
        assertEquals(c, unpackShort3(packed))
        assertEquals(d, unpackShort4(packed))
    }

    @Test
    fun testShortsWithBool() {
        val a = 123
        val b = 345
        val c = 789
        val d = 999
        val bool = true
        val packed = packShortsAndBool(a.toShort(), b.toShort(), c.toShort(), d.toShort(), bool)
        assertEquals(a, unpackShort1(packed))
        assertEquals(b, unpackShort2(packed))
        assertEquals(c, unpackShort3(packed))
        assertEquals(d, unpackShort4(packed))
        assertEquals(bool, unpackHighestBit(packed) == 1)
    }

    @Test
    fun testShortsWithBoolFalse() {
        val a = 123
        val b = 345
        val c = 789
        val d = 999
        val bool = false
        val packed = packShortsAndBool(a.toShort(), b.toShort(), c.toShort(), d.toShort(), bool)
        assertEquals(a, unpackShort1(packed))
        assertEquals(b, unpackShort2(packed))
        assertEquals(c, unpackShort3(packed))
        assertEquals(d, unpackShort4(packed))
        assertEquals(bool, unpackHighestBit(packed) == 1)
    }
}

fun ULong.asBinaryString(): String = buildString {
    var value = this@asBinaryString
    while (value > 0u) {
        append(if (value and 0b1u > 0u) '1' else '0')
        value = value shr 1
    }
}