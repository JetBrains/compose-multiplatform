/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.benchmark

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class TrivialCompositionBenchmarkValidator : LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleState = mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        Layout(modifier = modifier, measurePolicy = measurePolicy)
    }

    override fun toggleState() {
        toggleState.value = !toggleState.value
    }
}

private val measurePolicy = MeasurePolicy { _, _ ->
    layout(300, 300) {}
}

private val modifier = Modifier.fillMaxSize()

@LargeTest
@RunWith(Parameterized::class)
class TrivialCompositionBenchmarkValidatorTest(private val testIteration: Int) {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactory = {
        TrivialCompositionBenchmarkValidator()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testIteration={0}")
        fun initParameters(): Array<Any> = Array(20) { it + 1 }
    }

    @Test
    fun recomposeIsConstantOverhead() {
        benchmarkRule.toggleStateBenchmarkRecompose(caseFactory, requireRecomposition = false)
    }
}
