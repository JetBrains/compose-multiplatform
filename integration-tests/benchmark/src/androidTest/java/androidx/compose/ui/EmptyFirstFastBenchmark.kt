/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstComposeFast
import androidx.compose.testutils.benchmark.benchmarkFirstDrawFast
import androidx.compose.testutils.benchmark.benchmarkFirstLayoutFast
import androidx.compose.testutils.benchmark.benchmarkFirstMeasureFast
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmptyFirstFastBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { EmptyLayeredTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstComposeFast(testCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasureFast(testCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayoutFast(testCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDrawFast(testCaseFactory)
    }
}

class EmptyLayeredTestCase : LayeredComposeTestCase {
    @Composable
    override fun emitMeasuredContent() {}

    @Composable
    override fun emitContentWrappers(content: @Composable () -> Unit) {
        Box {
            content()
        }
    }
}