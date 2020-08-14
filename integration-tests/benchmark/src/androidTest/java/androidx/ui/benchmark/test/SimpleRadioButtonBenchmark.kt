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
import androidx.ui.integration.test.core.SimpleRadioButton1TestCase
import androidx.ui.integration.test.core.SimpleRadioButton2TestCase
import androidx.ui.integration.test.core.SimpleRadioButton3TestCase
import androidx.ui.integration.test.core.SimpleRadioButton4TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class SimpleRadioButtonBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(enableTransitions = true)

    private val button1CaseFactory = { SimpleRadioButton1TestCase() }
    private val button2CaseFactory = { SimpleRadioButton2TestCase() }
    private val button3CaseFactory = { SimpleRadioButton3TestCase() }
    private val button4CaseFactory = { SimpleRadioButton4TestCase() }

    @Test
    fun radio_button_1_first_compose() {
        benchmarkRule.benchmarkFirstCompose(button1CaseFactory)
    }

    @Test
    fun radio_button_1_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(button1CaseFactory)
    }

    @Test
    fun radio_button_1_first_layout() {
        benchmarkRule.benchmarkFirstLayout(button1CaseFactory)
    }

    @Test
    fun radio_button_1_first_draw() {
        benchmarkRule.benchmarkFirstDraw(button1CaseFactory)
    }

    @Test
    fun radio_button_1_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(button1CaseFactory)
    }

    @Test
    fun radio_button_1_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(button1CaseFactory)
    }

    @Test
    fun radio_button_1_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(button1CaseFactory)
    }

    @Test
    fun radio_button_1_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(button1CaseFactory)
    }

    @Test
    fun radio_button_1_layout() {
        benchmarkRule.benchmarkLayoutPerf(button1CaseFactory)
    }

    @Test
    fun radio_button_1_draw() {
        benchmarkRule.benchmarkDrawPerf(button1CaseFactory)
    }

    @Test
    fun radio_button_2_first_compose() {
        benchmarkRule.benchmarkFirstCompose(button2CaseFactory)
    }

    @Test
    fun radio_button_2_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(button2CaseFactory)
    }

    @Test
    fun radio_button_2_first_layout() {
        benchmarkRule.benchmarkFirstLayout(button2CaseFactory)
    }

    @Test
    fun radio_button_2_first_draw() {
        benchmarkRule.benchmarkFirstDraw(button2CaseFactory)
    }

    @Test
    fun radio_button_2_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(button2CaseFactory)
    }

    @Test
    fun radio_button_2_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(button2CaseFactory)
    }

    @Test
    fun radio_button_2_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(button2CaseFactory)
    }

    @Test
    fun radio_button_2_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(button2CaseFactory)
    }

    @Test
    fun radio_button_2_layout() {
        benchmarkRule.benchmarkLayoutPerf(button2CaseFactory)
    }

    @Test
    fun radio_button_2_draw() {
        benchmarkRule.benchmarkDrawPerf(button2CaseFactory)
    }

    @Test
    fun radio_button_3_first_compose() {
        benchmarkRule.benchmarkFirstCompose(button3CaseFactory)
    }

    @Test
    fun radio_button_3_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(button3CaseFactory)
    }

    @Test
    fun radio_button_3_first_layout() {
        benchmarkRule.benchmarkFirstLayout(button3CaseFactory)
    }

    @Test
    fun radio_button_3_first_draw() {
        benchmarkRule.benchmarkFirstDraw(button3CaseFactory)
    }

    @Test
    fun radio_button_3_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(button3CaseFactory, toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(button3CaseFactory, toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(button3CaseFactory, toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_layout() {
        benchmarkRule.benchmarkLayoutPerf(button3CaseFactory)
    }

    @Test
    fun radio_button_3_draw() {
        benchmarkRule.benchmarkDrawPerf(button3CaseFactory)
    }

    @Test
    fun radio_button_4_first_compose() {
        benchmarkRule.benchmarkFirstCompose(button4CaseFactory)
    }

    @Test
    fun radio_button_4_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(button4CaseFactory)
    }

    @Test
    fun radio_button_4_first_layout() {
        benchmarkRule.benchmarkFirstLayout(button4CaseFactory)
    }

    @Test
    fun radio_button_4_first_draw() {
        benchmarkRule.benchmarkFirstDraw(button4CaseFactory)
    }

    @Test
    fun radio_button_4_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(button4CaseFactory)
    }

    @Test
    fun radio_button_4_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(button4CaseFactory, toggleCausesRecompose = true)
    }

    @Test
    fun radio_button_4_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(button4CaseFactory, toggleCausesRecompose = true)
    }

    @Test
    fun radio_button_4_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(button4CaseFactory, toggleCausesRecompose = true)
    }

    @Test
    fun radio_button_4_layout() {
        benchmarkRule.benchmarkLayoutPerf(button4CaseFactory)
    }

    @Test
    fun radio_button_4_draw() {
        benchmarkRule.benchmarkDrawPerf(button4CaseFactory)
    }
}
