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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark that runs [NestedBoxesTestCase].
 */
@LargeTest
@RunWith(Parameterized::class)
class NestedBoxesBenchmark(private val depth: Int, private val children: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "depth={0} children={1}")
        fun initParameters(): Array<Any> = arrayOf(arrayOf(7, 2), arrayOf(4, 5), arrayOf(100, 1))
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val checkboxCaseFactory = { NestedBoxesTestCase(depth, children) }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(checkboxCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(checkboxCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(checkboxCaseFactory)
    }
}