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

package androidx.compose.material.benchmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for RadioGroup-like layout (column of rows of text and radio buttons).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RadioGroupBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val radioCaseFactory = { RadioGroupTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(radioCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(radioCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(radioCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(radioCaseFactory)
    }

    @Test
    fun toggleRadio_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            radioCaseFactory,
            assertOneRecomposition = false
        )
    }

    @Test
    fun toggleRadio_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(radioCaseFactory, assertOneRecomposition = false)
    }

    @Test
    fun toggleRadio_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(radioCaseFactory, assertOneRecomposition = false)
    }

    @Test
    fun toggleRadio_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(radioCaseFactory, assertOneRecomposition = false)
    }
}

internal class RadioGroupTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private val radiosCount = 10
    private val options = (0 until radiosCount).toList()
    private val select = mutableStateOf(0)

    override fun toggleState() {
        select.value = (select.value + 1) % radiosCount
    }

    @Composable
    override fun MeasuredContent() {
        Column {
            options.forEach { item ->
                Row(
                    modifier = Modifier.selectable(
                        selected = (select.value == item),
                        onClick = { select.value = item }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.toString())
                    RadioButton(
                        selected = (select.value == item),
                        onClick = { select.value = item }
                    )
                }
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}