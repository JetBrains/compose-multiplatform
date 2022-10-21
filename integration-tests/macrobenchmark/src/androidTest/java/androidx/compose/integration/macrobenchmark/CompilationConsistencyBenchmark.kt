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

import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.COMPILATION_MODES
import androidx.testutils.measureStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This benchmark is used to verify that compilation mode performance is consistent across runs,
 * and that compilation state doesn't leak across benchmarks.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
@RunWith(Parameterized::class)
class CompilationConsistencyBenchmark(
    @Suppress("UNUSED_PARAMETER") iteration: Int,
    private val compilationMode: CompilationMode
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureStartup(
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        packageName = packageName
    ) {
        action = "androidx.compose.integration.macrobenchmark.target.LAZY_COLUMN_ACTIVITY"
        putExtra("ITEM_COUNT", 5)
    }

    companion object {
        val packageName = "androidx.compose.integration.macrobenchmark.target"

        @Parameterized.Parameters(name = "iter={0},compilation={1}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = mutableListOf<Array<Any>>().apply {
            for (iter in 1..4) {
                for (compilationMode in COMPILATION_MODES) {
                    add(arrayOf(iter, compilationMode))
                }
            }
        }
    }
}