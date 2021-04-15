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

package androidx.compose.foundation.benchmark

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SimpleComponentImplementationBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

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

class ComponentWithModifiersTestCase : SimpleComponentImplementationTestCase() {

    @Composable
    override fun MeasuredContent() {
        val innerSize = getInnerSize()
        Box(
            Modifier.size(48.dp)
                .background(color = Color.Cyan)
                .padding(innerSize.value)
                .border(
                    color = Color.Cyan,
                    width = 1.dp,
                    shape = CircleShape
                )
        )
    }
}

class ComponentWithRedrawTestCase : SimpleComponentImplementationTestCase() {

    @Composable
    override fun MeasuredContent() {
        val innerSize = getInnerSize()
        val stroke = Stroke()
        Canvas(Modifier.size(48.dp)) {
            drawCircle(Color.Black, size.minDimension, style = stroke)
            drawCircle(Color.Black, innerSize.value.value / 2f, center)
        }
    }
}

class ComponentWithTwoLayoutNodesTestCase : SimpleComponentImplementationTestCase() {
    @Composable
    override fun MeasuredContent() {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(BorderStroke(1.dp, Color.Cyan), CircleShape)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            val innerSize = getInnerSize().value
            Canvas(Modifier.size(innerSize)) {
                drawOutline(
                    CircleShape.createOutline(size, layoutDirection, this),
                    Color.Cyan
                )
            }
        }
    }
}

abstract class SimpleComponentImplementationTestCase :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var state: MutableState<Dp>? = null

    @Composable
    fun getInnerSize(): MutableState<Dp> {
        val innerSize = remember { mutableStateOf(10.dp) }
        state = innerSize
        return innerSize
    }

    override fun toggleState() {
        with(state!!) {
            value = if (value == 10.dp) {
                20.dp
            } else {
                10.dp
            }
        }
    }
}