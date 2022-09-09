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

package androidx.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateChangedFlagsTest {

    @Test
    fun testUpdateSingleChangedFlag() {
        val flag = 0b010_0
        val result = updateChangedFlags(flag)
        assertEquals(0b001_0, result)
    }

    @Test
    fun testUpdateMultipleChangedFlags() {
        val flags = 0b010_010_010_010_010__010_010_010_010_010_0
        val result = updateChangedFlags(flags)
        assertEquals(0b001_001_001_001_001__001_001_001_001_001_0, result)
    }

    @Test
    fun testUpdateNonChangedValuesUnmodified() {
        val flags = 0b011_000_011_000_011__000_011_000_011_000_0
        val result = updateChangedFlags(flags)
        assertEquals(flags, result)
    }

    @Test
    fun testUpdateSingleChangedFlag_forced_stable() {
        val flag = 0b110_0
        val result = updateChangedFlags(flag)
        assertEquals(0b101_0, result)
    }

    @Test
    fun testUpdateMultipleChangedFlags_forced_stable() {
        val flags = 0b110_110_110_110_110__110_110_110_110_110_0
        val result = updateChangedFlags(flags)
        assertEquals(0b101_101_101_101_101__101_101_101_101_101_0, result)
    }

    @Test
    fun testUpdateNonChangedValuesUnmodified_forced_stable() {
        val flags = 0b111_100_111_100_111__100_111_100_111_100_0
        val result = updateChangedFlags(flags)
        assertEquals(flags, result)
    }
}