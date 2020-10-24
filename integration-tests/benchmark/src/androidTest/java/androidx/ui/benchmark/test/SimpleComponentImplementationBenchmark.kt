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
import androidx.ui.integration.test.core.ComponentWithTwoLayoutNodesTestCase
import androidx.ui.integration.test.core.ComponentWithRedrawTestCase
import androidx.ui.integration.test.core.ComponentWithModifiersTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class SimpleComponentImplementationBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(enableTransitions = true)

    private val twoLayoutNodesCaseFactory = { ComponentWithTwoLayoutNodesTestCase() }
    private val redrawOnlyCasefactory = { ComponentWithRedrawTestCase() }
    private val modifiersOnlyCaseFactory = { ComponentWithModifiersTestCase() }

    @Test
    fun component_twoLayoutNodes_first_compose() {
        benchmarkRule.benchmarkFirstCompose(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_first_layout() {
        benchmarkRule.benchmarkFirstLayout(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_first_draw() {
        benchmarkRule.benchmarkFirstDraw(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_layout() {
        benchmarkRule.benchmarkLayoutPerf(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_twoLayoutNodes_draw() {
        benchmarkRule.benchmarkDrawPerf(twoLayoutNodesCaseFactory)
    }

    @Test
    fun component_redrawOnly_first_compose() {
        benchmarkRule.benchmarkFirstCompose(redrawOnlyCasefactory)
    }

    @Test
    fun component_redrawOnly_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(redrawOnlyCasefactory)
    }

    @Test
    fun component_redrawOnly_first_layout() {
        benchmarkRule.benchmarkFirstLayout(redrawOnlyCasefactory)
    }

    @Test
    fun component_redrawOnly_first_draw() {
        benchmarkRule.benchmarkFirstDraw(redrawOnlyCasefactory)
    }

    @Test
    fun component_redrawOnly_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            redrawOnlyCasefactory,
            toggleCausesRecompose = false
        )
    }

    @Test
    fun component_redrawOnly_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            redrawOnlyCasefactory,
            toggleCausesRecompose = false
        )
    }

    @Test
    fun component_redrawOnly_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            redrawOnlyCasefactory,
            toggleCausesRecompose = false
        )
    }

    @Test
    fun component_redrawOnly_layout() {
        benchmarkRule.benchmarkLayoutPerf(redrawOnlyCasefactory)
    }

    @Test
    fun component_redrawOnly_draw() {
        benchmarkRule.benchmarkDrawPerf(redrawOnlyCasefactory)
    }

    @Test
    fun component_modifiersOnly_first_compose() {
        benchmarkRule.benchmarkFirstCompose(modifiersOnlyCaseFactory)
    }

    @Test
    fun component_modifiersOnly_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(modifiersOnlyCaseFactory)
    }

    @Test
    fun component_modifiersOnly_first_layout() {
        benchmarkRule.benchmarkFirstLayout(modifiersOnlyCaseFactory)
    }

    @Test
    fun component_modifiersOnly_first_draw() {
        benchmarkRule.benchmarkFirstDraw(modifiersOnlyCaseFactory)
    }

    @Test
    fun component_modifiersOnly_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(modifiersOnlyCaseFactory)
    }

    @Test
    fun component_modifiersOnly_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            modifiersOnlyCaseFactory,
            toggleCausesRecompose = true
        )
    }

    @Test
    fun component_modifiersOnly_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            modifiersOnlyCaseFactory,
            toggleCausesRecompose = true
        )
    }

    @Test
    fun component_modifiersOnly_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            modifiersOnlyCaseFactory,
            toggleCausesRecompose = true
        )
    }

    @Test
    fun component_modifiersOnly_layout() {
        benchmarkRule.benchmarkLayoutPerf(modifiersOnlyCaseFactory)
    }

    @Test
    fun component_modifiersOnly_draw() {
        benchmarkRule.benchmarkDrawPerf(modifiersOnlyCaseFactory)
    }
}
