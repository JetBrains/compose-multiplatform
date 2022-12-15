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

package androidx.compose.foundation.lazy.staggeredgrid

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
import org.junit.Test

class LazyStaggeredGridLaneInfoTest {
    private val laneInfo = LazyStaggeredGridLaneInfo()

    @Test
    fun emptySpan_unset() {
        assertEquals(LazyStaggeredGridLaneInfo.Unset, laneInfo.getLane(0))
    }

    @Test
    fun setLane() {
        laneInfo.setLane(0, 42)
        laneInfo.setLane(1, 0)

        assertEquals(42, laneInfo.getLane(0))
        assertEquals(0, laneInfo.getLane(1))
    }

    @Test
    fun setLane_beyondBound() {
        val bound = laneInfo.upperBound()
        laneInfo.setLane(bound - 1, 42)
        laneInfo.setLane(bound, 42)

        assertEquals(42, laneInfo.getLane(bound - 1))
        assertEquals(42, laneInfo.getLane(bound))
    }

    @Test
    fun setLane_largeNumber() {
        laneInfo.setLane(Int.MAX_VALUE / 2, 42)

        assertEquals(42, laneInfo.getLane(Int.MAX_VALUE / 2))
    }

    @Test
    fun setLane_decreaseBound() {
        laneInfo.setLane(Int.MAX_VALUE / 2, 42)
        laneInfo.setLane(0, 42)

        assertEquals(-1, laneInfo.getLane(Int.MAX_VALUE / 2))
        assertEquals(42, laneInfo.getLane(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun setLane_negative() {
        laneInfo.setLane(-1, 0)
    }

    @Test
    fun setLaneGaps() {
        laneInfo.setLane(0, 0)
        laneInfo.setLane(1, 1)
        laneInfo.setGaps(0, intArrayOf(42, 24))
        laneInfo.setGaps(1, intArrayOf(12, 21))

        assertThat(laneInfo.getGaps(0)).asList().isEqualTo(listOf(42, 24))
        assertThat(laneInfo.getGaps(1)).asList().isEqualTo(listOf(12, 21))
    }

    @Test
    fun missingLaneGaps() {
        laneInfo.setLane(42, 0)
        laneInfo.setGaps(0, intArrayOf(42, 24))

        assertThat(laneInfo.getGaps(42)).isNull()
    }

    @Test
    fun clearLaneGaps() {
        laneInfo.setLane(42, 0)
        laneInfo.setGaps(42, intArrayOf(42, 24))

        assertThat(laneInfo.getGaps(42)).isNotNull()

        laneInfo.setGaps(42, null)
        assertThat(laneInfo.getGaps(42)).isNull()
    }

    @Test
    fun resetOnLaneInfoContentMove() {
        laneInfo.setLane(0, 0)
        laneInfo.setGaps(0, intArrayOf(42, 24))

        laneInfo.setLane(Int.MAX_VALUE / 2, 1)

        laneInfo.setGaps(0, null)
        assertThat(laneInfo.getGaps(0)).isNull()
    }
}