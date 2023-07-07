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

/**
 * Benchmark that runs [RectsInColumnSharedModelTestCase].
 */
@LargeTest
@RunWith(Parameterized::class)
class RectsInColumnSharedModelBenchmark(private val numberOfRectangles: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val rectsInColumnCaseFactory = { RectsInColumnSharedModelTestCase(numberOfRectangles) }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(rectsInColumnCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(rectsInColumnCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(rectsInColumnCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(rectsInColumnCaseFactory)
    }

    @Test
    fun toggleRectangleColor_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(rectsInColumnCaseFactory)
    }

    @Test
    fun toggleRectangleColor_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(rectsInColumnCaseFactory)
    }

    @Test
    fun toggleRectangleColor_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(rectsInColumnCaseFactory)
    }

    @Test
    fun toggleRectangleColor_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(rectsInColumnCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(rectsInColumnCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(rectsInColumnCaseFactory)
    }
}