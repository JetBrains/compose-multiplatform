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

package androidx.compose.ui.text.benchmark.intl

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.intl.LocaleList
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LocaleListBenchmark(val languageTag: String) {
    companion object {
        private val langTagPresets = arrayOf("en-US", "ja-JP", "zh-CH", "zh-TW", "sr-Latn-SR")

        @JvmStatic
        @Parameterized.Parameters(name = "languageTags={0}")
        fun initParameters(): Array<Any> =
            // Generates the comma separated language tags from the sliced langTagPresets array.
            // e.g. this generates, "en-US", "en-US,ja-JP", "en-US,ja-JP,zh-CH", ...
            langTagPresets.mapIndexed { index, _ -> langTagPresets.sliceArray(0..index) }
                .map { it.joinToString(",") }
                .toTypedArray()
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun create() {
        benchmarkRule.measureRepeated { LocaleList(languageTag) }
    }
}