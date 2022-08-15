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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class WeightedColumnWithRowsBenchmark(private val numberOfBoxes: Int) {

    private val subLayouts = 5
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "numberOfBoxes={0}")
        fun initParameters(): Array<Int> = arrayOf(10, 100)
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val simpleColumnWithRowsFactory =
        { WeightedColumnWithRowsTestCase(subLayouts, numberOfBoxes) }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(simpleColumnWithRowsFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(simpleColumnWithRowsFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(simpleColumnWithRowsFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(simpleColumnWithRowsFactory)
    }

    @Test
    fun toggleLayoutQuantity_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(simpleColumnWithRowsFactory)
    }

    @Test
    fun toggleLayoutQuantity_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(simpleColumnWithRowsFactory)
    }

    @Test
    fun toggleLayoutQuantity_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(simpleColumnWithRowsFactory)
    }

    @Test
    fun toggleColor_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(simpleColumnWithRowsFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(simpleColumnWithRowsFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(simpleColumnWithRowsFactory)
    }
}