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

package androidx.ui.benchmark.test

import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.benchmarkDrawPerf
import androidx.ui.benchmark.benchmarkFirstCompose
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.benchmark.benchmarkLayoutPerf
import androidx.ui.benchmark.toggleStateBenchmarkDraw
import androidx.ui.benchmark.toggleStateBenchmarkLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasure
import androidx.ui.benchmark.toggleStateBenchmarkRecompose
import androidx.ui.integration.test.material.CheckboxesInRowsTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark that runs [CheckboxesInRowsTestCase].
 */
@LargeTest
@RunWith(Parameterized::class)
class CheckboxesInRowsBenchmark(private val numberOfCheckboxes: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val checkboxCaseFactory = { CheckboxesInRowsTestCase(numberOfCheckboxes) }

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

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(checkboxCaseFactory)
    }

    @Test
    fun toggleCheckbox_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(checkboxCaseFactory)
    }

    @Test
    fun toggleCheckbox_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(checkboxCaseFactory)
    }

    @Test
    fun toggleCheckbox_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(checkboxCaseFactory)
    }

    @Test
    fun toggleCheckbox_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(checkboxCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(checkboxCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(checkboxCaseFactory)
    }
}