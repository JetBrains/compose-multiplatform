/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.text.selection

import androidx.compose.foundation.benchmark.text.filterForCi
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * we had some issues where SelectionContainer will dramatically slow down the compose & draw
 * time(mainly the compose time). Now it's mostly fixed.
 *
 * This benchmark is to observe for regressions
 */
@SmallTest
@RunWith(Parameterized::class)
class SelectionContainerBenchmark(private val childrenCount: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "childrenCount={0}"
        )
        fun initParameters() = arrayOf(1, 10, 20).filterForCi { min() }
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()
    private val caseFactory = { SelectionContainerTestCase(childrenCount) }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(caseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(caseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(caseFactory)
    }
}