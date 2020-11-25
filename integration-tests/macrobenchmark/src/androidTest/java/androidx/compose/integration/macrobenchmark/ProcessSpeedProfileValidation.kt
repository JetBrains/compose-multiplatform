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
import androidx.benchmark.macro.MacrobenchmarkConfig
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Macrobenchmark used for local validation of performance numbers coming from MacrobenchmarkRule.
 */
@LargeTest
@SdkSuppress(minSdkVersion = 29)
@RunWith(Parameterized::class)
class ProcessSpeedProfileValidation(
    private val compilationMode: CompilationMode,
    private val killProcess: Boolean
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun start() {
        val config = MacrobenchmarkConfig(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            killProcessEachIteration = killProcess,
            iterations = 10
        )
        benchmarkRule.measureRepeated(config) {
            pressHome()
            launchPackageAndWait()
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.compose.integration.demos"

        @Parameterized.Parameters(name = "compilation_mode={0}, kill_process={1}")
        @JvmStatic
        fun kilProcessParameters(): List<Array<Any>> {
            val compilationModes = listOf(
                CompilationMode.None,
                CompilationMode.SpeedProfile(warmupIterations = 3)
            )
            val processKillOptions = listOf(true, false)
            return compilationModes.zip(processKillOptions).map {
                arrayOf(it.first, it.second)
            }
        }
    }
}
