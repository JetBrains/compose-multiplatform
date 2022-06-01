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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * End-to-end test for compose-runtime-tracing verifying that names of Composables show up in
 * a Perfetto trace.
 */
class TrivialTracingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun test_composable_names_present_in_trace() {
        val metrics = COMPOSABLE_NAMES.map { composableName ->
            TraceSectionMetric("%$PACKAGE_NAME.$composableName %$FILE_NAME:% key=%")
        }
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            iterations = 1, // we are only verifying the presence of entries (not the timing data)
            setupBlock = {
                PerfettoCapture().enableAndroidxTracingPerfetto(
                    PACKAGE_NAME,
                    provideBinariesIfMissing = true
                )
            }
        ) {
            startActivityAndWait(Intent(ACTION))
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.compose.integration.macrobenchmark.target"

        private const val ACTION =
            "androidx.compose.integration.macrobenchmark.target.TRIVIAL_TRACING_ACTIVITY"

        private const val FILE_NAME = "TrivialTracingActivity.kt"

        private val COMPOSABLE_NAMES = listOf(
            "Foo_BBC27C8E_13A7_4A5F_A735_AFDC433F54C3",
            "Bar_4888EA32_ABC5_4550_BA78_1247FEC1AAC9",
            "Baz_609801AB_F5A9_47C3_9405_2E82542F21B8"
        )
    }
}
