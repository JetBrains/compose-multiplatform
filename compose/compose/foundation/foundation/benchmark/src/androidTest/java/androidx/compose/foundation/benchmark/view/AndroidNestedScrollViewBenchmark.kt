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

package androidx.compose.foundation.benchmark.view

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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark that runs [AndroidNestedScrollViewTestCase].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidNestedScrollViewBenchmark {

    @get:Rule
    val benchmarkRule = AndroidBenchmarkRule()

    private val nestedScrollCaseFactory = { AndroidNestedScrollViewTestCase() }

    @Test
    fun first_setContent() {
        benchmarkRule.benchmarkFirstSetContent(nestedScrollCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(nestedScrollCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(nestedScrollCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(nestedScrollCaseFactory)
    }

    @Test
    fun changeScroll_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(nestedScrollCaseFactory)
    }

    @Test
    fun changeScroll_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(nestedScrollCaseFactory)
    }

    @Test
    fun changeScroll_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(nestedScrollCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(nestedScrollCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(nestedScrollCaseFactory)
    }
}