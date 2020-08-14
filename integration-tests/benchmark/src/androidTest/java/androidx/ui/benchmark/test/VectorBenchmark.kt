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
import androidx.ui.benchmark.benchmarkFirstCompose
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.integration.test.framework.ProgrammaticVectorTestCase
import androidx.ui.integration.test.framework.XmlVectorTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Benchmark to compare performance of [parsing a vector asset from XML][XmlVectorTestCase] and
 * [creating the same asset purely from code][ProgrammaticVectorTestCase].
 */
@LargeTest
@RunWith(JUnit4::class)
class VectorBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun xml_compose() {
        benchmarkRule.benchmarkFirstCompose { XmlVectorTestCase() }
    }

    @Test
    fun xml_measure() {
        benchmarkRule.benchmarkFirstMeasure { XmlVectorTestCase() }
    }

    @Test
    fun xml_layout() {
        benchmarkRule.benchmarkFirstLayout { XmlVectorTestCase() }
    }

    @Test
    fun xml_draw() {
        benchmarkRule.benchmarkFirstDraw { XmlVectorTestCase() }
    }

    @Test
    fun programmatic_compose() {
        benchmarkRule.benchmarkFirstCompose { ProgrammaticVectorTestCase() }
    }

    @Test
    fun programmatic_measure() {
        benchmarkRule.benchmarkFirstMeasure { ProgrammaticVectorTestCase() }
    }

    @Test
    fun programmatic_layout() {
        benchmarkRule.benchmarkFirstLayout { ProgrammaticVectorTestCase() }
    }

    @Test
    fun programmatic_draw() {
        benchmarkRule.benchmarkFirstDraw { ProgrammaticVectorTestCase() }
    }
}
