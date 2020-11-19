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

package androidx.compose.runtime.benchmark.collection

import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.benchmark.ComposeBenchmarkBase
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Benchmark that tests the performance of ArrayList and MutableVector.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MutableVectorBenchmark : ComposeBenchmarkBase() {
    private val arraySize = 100

    val content: Array<Int> = Array(arraySize) { it }

    @Before
    fun setUp() {
        repeat(arraySize) {
            content[it] = it
        }
    }

    @Test
    fun createArrayList() {
        benchmarkRule.measureRepeated {
            mutableListOf(*content)
        }
    }

    @Test
    fun createVector() {
        benchmarkRule.measureRepeated {
            mutableVectorOf(*content)
        }
    }

    @Test
    fun indexOfArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.indexOf(-1)
        }
    }

    @Test
    fun indexOfVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.indexOf(-1)
        }
    }

    @Test
    fun iterateArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            list.fastForEach { sum += it }
        }
    }

    @Test
    fun iterateVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            list.forEach { sum += it }
        }
    }

    @Test
    fun addArrayList() {
        val list = ArrayList<Int>(arraySize)
        benchmarkRule.measureRepeated {
            repeat(arraySize) {
                list.add(it)
            }
            list.clear()
        }
    }

    @Test
    fun addVector() {
        val list = MutableVector<Int>(arraySize)
        benchmarkRule.measureRepeated {
            repeat(arraySize) {
                list.add(it)
            }
            list.clear()
        }
    }

    @Test
    fun removeArrayList() {
        val list = ArrayList<Int>(arraySize)
        benchmarkRule.measureRepeated {
            list.addAll(content)
            while (list.isNotEmpty()) {
                list.removeAt(list.lastIndex)
            }
        }
    }

    @Test
    fun removeVector() {
        val list = MutableVector<Int>(arraySize)
        benchmarkRule.measureRepeated {
            list.addAll(content)
            while (list.isNotEmpty()) {
                list.removeAt(list.lastIndex)
            }
        }
    }

    @Test
    fun removeStartArrayList() {
        val list = ArrayList<Int>(arraySize)
        benchmarkRule.measureRepeated {
            list.addAll(content)
            while (list.isNotEmpty()) {
                list.removeAt(0)
            }
        }
    }

    @Test
    fun removeStartVector() {
        val list = MutableVector<Int>(arraySize)
        benchmarkRule.measureRepeated {
            list.addAll(content)
            while (list.isNotEmpty()) {
                list.removeAt(0)
            }
        }
    }

    @Test
    fun mapArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            val mapped = list.map { it }
            sum += mapped.firstOrNull() ?: 0
        }
    }

    @Test
    fun mapVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            val mapped = list.map { it }
            sum += mapped.firstOrNull() ?: 0
        }
    }

    @Test
    fun indexOfLastArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.indexOfLast { it == 0 }
        }
    }

    @Test
    fun indexOfLastVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.indexOfLast { it == 0 }
        }
    }

    @Test
    fun lastIndexOfArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.lastIndexOf(0)
        }
    }

    @Test
    fun lastIndexOfVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.lastIndexOf(0)
        }
    }

    @Test
    fun anyArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += if (list.fastAny { it == -1 }) 1 else 0
        }
    }

    @Test
    fun anyVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += if (list.any { it == -1 }) 1 else 0
        }
    }

    @Test
    fun sumByArrayList() {
        val list = mutableListOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.fastSumBy { it }
        }
    }

    @Test
    fun sumByVector() {
        val list = mutableVectorOf(*content)
        var sum = 0
        benchmarkRule.measureRepeated {
            sum += list.sumBy { it }
        }
    }

    @Test
    fun addAllArrayList() {
        val list = mutableListOf(*content)
        benchmarkRule.measureRepeated {
            val list2 = mutableListOf<Int>()
            list2.addAll(list)
            list2.addAll(list)
            list2.addAll(list)
            list2.addAll(list)
        }
    }

    @Test
    fun addAllVector() {
        val list = mutableVectorOf(*content)
        benchmarkRule.measureRepeated {
            val list2 = mutableVectorOf<Int>()
            list2.addAll(list)
            list2.addAll(list)
            list2.addAll(list)
            list2.addAll(list)
        }
    }

    @Test
    fun insertAllArrayList() {
        val list = mutableListOf(*content)
        benchmarkRule.measureRepeated {
            val list2 = mutableListOf<Int>()
            list2.addAll(0, list)
            list2.addAll(0, list)
            list2.addAll(0, list)
            list2.addAll(0, list)
        }
    }

    @Test
    fun insertAllVector() {
        val list = mutableVectorOf(*content)
        benchmarkRule.measureRepeated {
            val list2 = mutableVectorOf<Int>()
            list2.addAll(0, list)
            list2.addAll(0, list)
            list2.addAll(0, list)
            list2.addAll(0, list)
        }
    }

    @Test
    fun setArrayList() {
        val list = mutableListOf<Int>()
        list.addAll(content)
        benchmarkRule.measureRepeated {
            repeat(arraySize) {
                list[it] = list[it] + 1
            }
        }
    }

    @Test
    fun setVector() {
        val list = mutableVectorOf<Int>()
        list.addAll(content)
        benchmarkRule.measureRepeated {
            repeat(arraySize) {
                list[it] = list[it] + 1
            }
        }
    }
}