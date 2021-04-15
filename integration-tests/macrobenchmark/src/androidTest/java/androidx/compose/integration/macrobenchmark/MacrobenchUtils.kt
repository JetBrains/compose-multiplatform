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

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.isSupportedWithVmSettings
import androidx.benchmark.macro.junit4.MacrobenchmarkRule

const val TARGET_PACKAGE = "androidx.compose.integration.macrobenchmark.target"

fun MacrobenchmarkRule.measureStartup(
    compilationMode: CompilationMode,
    startupMode: StartupMode,
    iterations: Int = 3,
    setupIntent: Intent.() -> Unit = {}
) = measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(StartupTimingMetric()),
    compilationMode = compilationMode,
    iterations = iterations,
    startupMode = startupMode
) {
    pressHome()
    val intent = Intent()
    intent.setPackage(TARGET_PACKAGE)
    setupIntent(intent)
    startActivityAndWait(intent)
}

fun createStartupCompilationParams(
    startupModes: List<StartupMode> = listOf(StartupMode.HOT, StartupMode.WARM, StartupMode.COLD),
    compilationModes: List<CompilationMode> = listOf(
        CompilationMode.None,
        CompilationMode.Interpreted,
        CompilationMode.SpeedProfile()
    )
): List<Array<Any>> = mutableListOf<Array<Any>>().apply {
    for (startupMode in startupModes) {
        for (compilationMode in compilationModes) {
            // Skip configs that can't run, so they don't clutter Studio benchmark
            // output with AssumptionViolatedException dumps
            if (compilationMode.isSupportedWithVmSettings()) {
                add(arrayOf(startupMode, compilationMode))
            }
        }
    }
}

fun createCompilationParams(
    compilationModes: List<CompilationMode> = listOf(
        CompilationMode.None,
        CompilationMode.Interpreted,
        CompilationMode.SpeedProfile()
    )
): List<Array<Any>> = mutableListOf<Array<Any>>().apply {
    for (compilationMode in compilationModes) {
        // Skip configs that can't run, so they don't clutter Studio benchmark
        // output with AssumptionViolatedException dumps
        if (compilationMode.isSupportedWithVmSettings()) {
            add(arrayOf(compilationMode))
        }
    }
}