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

import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = 29)
@RunWith(Parameterized::class) // Parameterized to work around timeouts (b/174175784)
class StartupDemosMacrobenchmark(
    @Suppress("unused") private val ignored: Boolean
) {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun compiledColdStartup() = benchmarkRule.measureStartup(
        profileCompiled = true,
        coldLaunch = true
    )

    @Test
    fun uncompiledColdStartup() = benchmarkRule.measureStartup(
        profileCompiled = false,
        coldLaunch = true
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun startupDemosParameters(): List<Array<Any>> {
            return listOf(arrayOf(false))
        }
    }
}
