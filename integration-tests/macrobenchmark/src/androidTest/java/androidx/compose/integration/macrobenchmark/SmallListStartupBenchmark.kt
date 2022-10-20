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

package androidx.compose.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.testutils.createStartupCompilationParams
import androidx.testutils.getStartupMetrics
import androidx.testutils.measureStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class SmallListStartupBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Temporary, tracking for b/231455742
     *
     * Note that this tracing only exists on more recent API levels
     */
    private val metrics = getStartupMetrics() + if (startupMode == StartupMode.COLD) {
        listOf(
            TraceSectionMetric("cache_hit", TraceSectionMetric.Mode.Sum),
            TraceSectionMetric("cache_miss", TraceSectionMetric.Mode.Sum)
        )
    } else {
        emptyList()
    }

    @Test
    fun startup() = benchmarkRule.measureStartup(
        compilationMode = compilationMode,
        startupMode = startupMode,
        metrics = metrics,
        packageName = "androidx.compose.integration.macrobenchmark.target"
    ) {
        action = "androidx.compose.integration.macrobenchmark.target.LAZY_COLUMN_ACTIVITY"
        putExtra("ITEM_COUNT", 5)
    }

    companion object {
        @Parameterized.Parameters(name = "startup={0},compilation={1}")
        @JvmStatic
        fun parameters() = createStartupCompilationParams()
    }
}