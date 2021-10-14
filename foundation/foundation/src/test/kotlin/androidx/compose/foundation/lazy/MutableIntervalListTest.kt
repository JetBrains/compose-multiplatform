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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MutableIntervalListTest {

    private val intervalList = MutableIntervalList<Int>()

    @Test
    fun addOneItem_searchInterval() {
        intervalList.add(1, 10)

        val foundInterval = intervalList.intervalForIndex(0)

        assertThat(foundInterval.content).isEqualTo(10)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(0)
    }

    @Test
    fun addOneItem_searchIndexOutOfBounds() {
        intervalList.add(1, 10)

        val wasException: Boolean = try {
            intervalList.intervalForIndex(2)
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }

        assertThat(wasException).isTrue()
    }

    @Test
    fun addSingleItems_searchFirstInterval() {
        addFiveSingleIntervals()

        val foundInterval = intervalList.intervalForIndex(0)

        assertThat(foundInterval.content).isEqualTo(10)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(0)
    }

    @Test
    fun addSingleItems_searchLastInterval() {
        addFiveSingleIntervals()

        val foundInterval = intervalList.intervalForIndex(4)

        assertThat(foundInterval.content).isEqualTo(50)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(4)
    }

    @Test
    fun addSingleItems_searchMidInterval() {
        addFiveSingleIntervals()

        val foundInterval = intervalList.intervalForIndex(2)

        assertThat(foundInterval.content).isEqualTo(30)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(2)
    }

    @Test
    fun addVariableItems_searchFirstInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList.intervalForIndex(0)

        assertThat(foundInterval.content).isEqualTo(10)
        assertThat(foundInterval.size).isEqualTo(3)
        assertThat(foundInterval.startIndex).isEqualTo(0)
    }

    @Test
    fun addVariableItems_searchLastInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList.intervalForIndex(22)

        assertThat(foundInterval.content).isEqualTo(50)
        assertThat(foundInterval.size).isEqualTo(11)
        assertThat(foundInterval.startIndex).isEqualTo(12)
    }

    @Test
    fun addVariableItems_searchMidInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList.intervalForIndex(6)

        assertThat(foundInterval.content).isEqualTo(30)
        assertThat(foundInterval.size).isEqualTo(7)
        assertThat(foundInterval.startIndex).isEqualTo(4)
    }

    @Test
    fun addVariableItems_searchSecondInterval() {
        addFiveVariableIntervals()

        val foundInterval = intervalList.intervalForIndex(3)

        assertThat(foundInterval.content).isEqualTo(20)
        assertThat(foundInterval.size).isEqualTo(1)
        assertThat(foundInterval.startIndex).isEqualTo(3)
    }

    @Test
    fun addVariableItems_searchIndexOutOfBounds() {
        addFiveVariableIntervals()

        val wasException: Boolean = try {
            intervalList.intervalForIndex(23)
            false
        } catch (e: IndexOutOfBoundsException) {
            true
        }

        assertThat(wasException).isTrue()
    }

    private fun addFiveSingleIntervals() {
        intervalList.add(1, 10)
        intervalList.add(1, 20)
        intervalList.add(1, 30)
        intervalList.add(1, 40)
        intervalList.add(1, 50)
    }

    private fun addFiveVariableIntervals() {
        intervalList.add(3, 10)
        intervalList.add(1, 20)
        intervalList.add(7, 30)
        intervalList.add(1, 40)
        intervalList.add(11, 50)
    }
}
