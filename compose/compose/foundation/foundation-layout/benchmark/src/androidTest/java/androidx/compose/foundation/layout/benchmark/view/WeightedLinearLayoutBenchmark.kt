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

package androidx.compose.foundation.layout.benchmark.view

import androidx.compose.testutils.benchmark.AndroidBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkFirstSetContent
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class WeightedLinearLayoutBenchmark(private val numberOfBoxes: Int) {

    private val subLayouts = 5
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "numberOfBoxes={0}")
        fun initParameters(): Array<Int> = arrayOf(10, 100)
    }

    @get:Rule
    val benchmarkRule = AndroidBenchmarkRule()

    private val linearLayoutCaseFactory = {
        WeightedLinearLayoutTestCase(subLayouts, numberOfBoxes)
    }

    @Test
    fun first_setContent() {
        benchmarkRule.benchmarkFirstSetContent(linearLayoutCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(linearLayoutCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(linearLayoutCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(linearLayoutCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(linearLayoutCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(linearLayoutCaseFactory)
    }

    @Test
    fun changeLayoutContents_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(linearLayoutCaseFactory)
    }

    @Test
    fun changeLayoutContents_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(linearLayoutCaseFactory)
    }

    @Test
    fun changeLayoutContents_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(linearLayoutCaseFactory)
    }
}