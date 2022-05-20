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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class MutableIntervalListTest {

    private val intervalList = MutableIntervalList<Int>()

    @Test
    fun addOneItem_searchInterval() {
        intervalList.addInterval(1, 10)

        val foundInterval = intervalList[0]

        assertThat(foundInterval.value).isEqualTo(10)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(0)
    }

    @Test
    fun addOneItem_searchIndexOutOfBounds() {
        intervalList.addInterval(1, 10)

        val wasException: Boolean = try {
            intervalList[2]
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }

        assertThat(wasException).isTrue()
    }

    @Test
    fun addSingleItems_searchFirstInterval() {
        addFiveSingleIntervals()

        val foundInterval = intervalList[0]

        assertThat(foundInterval.value).isEqualTo(10)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(0)
    }

    @Test
    fun addSingleItems_searchLastInterval() {
        addFiveSingleIntervals()

        val foundInterval = intervalList[4]

        assertThat(foundInterval.value).isEqualTo(50)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(4)
    }

    @Test
    fun addSingleItems_searchMidInterval() {
        addFiveSingleIntervals()

        val foundInterval = intervalList[2]

        assertThat(foundInterval.value).isEqualTo(30)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(2)
    }

    @Test
    fun addVariableItems_searchFirstInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList[0]

        assertThat(foundInterval.value).isEqualTo(10)
        assertThat(foundInterval.size).isEqualTo(3)
        assertThat(foundInterval.startIndex).isEqualTo(0)
    }

    @Test
    fun addVariableItems_searchLastInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList[22]

        assertThat(foundInterval.value).isEqualTo(50)
        assertThat(foundInterval.size).isEqualTo(11)
        assertThat(foundInterval.startIndex).isEqualTo(12)
    }

    @Test
    fun addVariableItems_searchMidInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList[6]

        assertThat(foundInterval.value).isEqualTo(30)
        assertThat(foundInterval.size).isEqualTo(7)
        assertThat(foundInterval.startIndex).isEqualTo(4)
    }

    @Test
    fun addVariableItems_searchSecondInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList[3]

        assertThat(foundInterval.value).isEqualTo(20)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(3)
    }

    @Test
    fun addVariableItems_searchIndexOutOfBounds() {
        addFiveVariableIntervals()

        val wasException: Boolean = try {
            intervalList[23]
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }

        assertThat(wasException).isTrue()
    }

    @Test
    fun addOneItem_searchIndexOutOfBoundsWithNegativeValue() {
        intervalList.addInterval(1, 10)

        val wasException: Boolean = try {
            intervalList[-1]
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }

        assertThat(wasException).isTrue()
    }

    @Test
    fun forEach_withoutParamsIterateThroughAll() {
        addFiveSingleIntervals()
        val intervals = mutableListOf<IntervalList.Interval<Int>>()

        intervalList.forEach {
            intervals.add(it)
        }

        assertThat(intervals.map { it.startIndex }).isEqualTo(listOf(0, 1, 2, 3, 4))
        assertThat(intervals.map { it.size }).isEqualTo(listOf(1, 1, 1, 1, 1))
        assertThat(intervals.map { it.value }).isEqualTo(listOf(10, 20, 30, 40, 50))
    }

    @Test
    fun forEach_withStartInterval() {
        addFiveSingleIntervals()
        val intervals = mutableListOf<IntervalList.Interval<Int>>()

        intervalList.forEach(fromIndex = 2) {
            intervals.add(it)
        }

        assertThat(intervals.map { it.startIndex }).isEqualTo(listOf(2, 3, 4))
    }

    @Test
    fun forEach_withStartAndEndInterval() {
        addFiveSingleIntervals()
        val intervals = mutableListOf<IntervalList.Interval<Int>>()

        intervalList.forEach(fromIndex = 2, toIndex = 3) {
            intervals.add(it)
        }

        assertThat(intervals.map { it.startIndex }).isEqualTo(listOf(2, 3))
    }

    @Test
    fun forEach_withTheSameStartAndEndInterval() {
        addFiveSingleIntervals()
        val intervals = mutableListOf<IntervalList.Interval<Int>>()

        intervalList.forEach(fromIndex = 2, toIndex = 2) {
            intervals.add(it)
        }

        assertThat(intervals.map { it.startIndex }).isEqualTo(listOf(2))
    }

    @Test
    fun forEach_startLargerThanEndThrows() {
        addFiveSingleIntervals()

        val wasException: Boolean = try {
            intervalList.forEach(fromIndex = 3, toIndex = 2) {}
            false
        } catch (e: IllegalArgumentException) {
            true
        }

        assertThat(wasException).isTrue()
    }

    @Test
    fun forEach_outOfBounds() {
        addFiveSingleIntervals()

        val wasException1: Boolean = try {
            intervalList.forEach(fromIndex = -1) {}
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }
        assertThat(wasException1).isTrue()

        val wasException2: Boolean = try {
            intervalList.forEach(toIndex = -1) {}
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }
        assertThat(wasException2).isTrue()

        val wasException3: Boolean = try {
            intervalList.forEach(fromIndex = 6) {}
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }
        assertThat(wasException3).isTrue()

        val wasException4: Boolean = try {
            intervalList.forEach(toIndex = 6) {}
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }
        assertThat(wasException4).isTrue()
    }

    private fun addFiveSingleIntervals() {
        intervalList.addInterval(1, 10)
        intervalList.addInterval(1, 20)
        intervalList.addInterval(1, 30)
        intervalList.addInterval(1, 40)
        intervalList.addInterval(1, 50)
    }

    private fun addFiveVariableIntervals() {
        intervalList.addInterval(3, 10)
        intervalList.addInterval(1, 20)
        intervalList.addInterval(7, 30)
        intervalList.addInterval(1, 40)
        intervalList.addInterval(11, 50)
    }
}
