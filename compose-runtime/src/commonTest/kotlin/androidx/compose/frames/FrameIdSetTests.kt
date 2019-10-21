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

package androidx.compose.frames

import androidx.compose.BitSet
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameIdSetTests {

    @Test
    fun emptySetShouldBeEmpty() {
        val empty = FrameIdSet.EMPTY

        repeat(1000) {
            empty.shouldBe(it, false)
        }
    }

    @Test
    fun shouldBeAbleToSetItems() {
        val times = 10000
        val set = (0..times).fold(FrameIdSet.EMPTY) { prev, index ->
            prev.set(index)
        }

        repeat(times) {
            set.shouldBe(it, true)
        }
    }

    @Test
    fun shouldBeAbleToSetOnlyEven() {
        val times = 10000
        val set = (0..times).fold(FrameIdSet.EMPTY) { prev, index ->
            if (index % 2 == 0) prev.set(index) else prev
        }

        repeat(times) {
            set.shouldBe(it, it % 2 == 0)
        }
    }

    @Test
    fun shouldBeAbleToSetOnlyOdds() {
        val times = 10000
        val set = (0..times).fold(FrameIdSet.EMPTY) { prev, index ->
            if (index % 2 == 1) prev.set(index) else prev
        }

        repeat(times) {
            set.shouldBe(it, it % 2 == 1)
        }
    }

    @Test
    fun shouldBeAbleToClearEvens() {
        val times = 10000
        val allSet = (0..times).fold(FrameIdSet.EMPTY) { prev, index ->
            prev.set(index)
        }

        val set = (0..times).fold(allSet) { prev, index ->
            if (index % 2 == 0) prev.clear(index) else prev
        }

        repeat(times - 1) {
            set.shouldBe(it, it % 2 == 1)
        }
    }

    @Test
    fun shouldBeAbleToCrawlSet() {
        val times = 10000
        val set = (0..times).fold(FrameIdSet.EMPTY) { prev, index ->
            prev.clear(index - 1).set(index)
        }

        set.shouldBe(times, true)
        repeat(times - 1) {
            set.shouldBe(it, false)
        }
    }

    @Test
    fun shouldBeAbleToCrawlAndClear() {
        val times = 10000
        val set = (0..times).fold(FrameIdSet.EMPTY) { prev, index ->
            prev.let {
                if ((index - 1) % 33 != 0) it.clear(index - 1) else it
            }.set(index)
        }

        set.shouldBe(times, true)

        // The multiples of 33 items should now be set
        repeat(times - 1) {
            set.shouldBe(it, it % 33 == 0)
        }

        val newSet = (0..times - 1).fold(set) { prev, index ->
            prev.clear(index)
        }

        newSet.shouldBe(times, true)

        repeat(times - 1) {
            newSet.shouldBe(it, false)
        }
    }

    @Test
    fun shouldBeAbleToInsertAndRemoveOutOfOptimalRange() {
        FrameIdSet.EMPTY
            .set(1000)
            .set(1)
            .shouldBe(1000, true)
            .shouldBe(1, true)
            .set(10)
            .shouldBe(10, true)
            .set(4)
            .shouldBe(4, true)
            .clear(1)
            .shouldBe(1, false)
            .clear(4)
            .shouldBe(4, false)
            .clear(10)
            .shouldBe(1, false)
            .shouldBe(4, false)
            .shouldBe(10, false)
            .shouldBe(1000, true)
    }

    @Test
    fun shouldMatchBitSet() {
        val random = Random(10)
        val bitSet = BitSet()
        val set = (0..100).fold(FrameIdSet.EMPTY) { prev, _ ->
            val value = random.nextInt(0, 1000)
            bitSet.set(value)
            prev.set(value)
        }

        val clear = (0..100).fold(set) { prev, _ ->
            val value = random.nextInt(0, 1000)
            bitSet.clear(value)
            prev.clear(value)
        }

        repeat(1000) {
            clear.shouldBe(it, bitSet[it])
        }
    }
}

private fun FrameIdSet.shouldBe(index: Int, value: Boolean): FrameIdSet {
    assertEquals(value, get(index), "Bit $index should be $value")
    return this
}
