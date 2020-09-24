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

package androidx.compose.ui

import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.test.filters.LargeTest
import androidx.ui.integration.test.core.EmptyTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmptyBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val textCaseFactory = { EmptyTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(textCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(textCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(textCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(textCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(textCaseFactory)
    }
}