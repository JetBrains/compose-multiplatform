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

package androidx.compose.ui.text.benchmark.input

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.benchmark.RandomTextGenerator
import androidx.compose.ui.text.input.PartialGapBuffer
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(InternalTextApi::class)
@LargeTest
@RunWith(Parameterized::class)
class GapBufferBenchmark(val initText: InitialText) {
    companion object {

        /**
         * Helper class for describing the parameter in test result
         */
        data class InitialText(val text: String, val name: String) {
            override fun toString(): String = name
        }

        private val longText = RandomTextGenerator().nextParagraph(500)
        private val shortText = RandomTextGenerator().nextParagraph(50)

        @JvmStatic
        @Parameterized.Parameters(name = "initText={0}")
        fun initParameters(): List<InitialText> = listOf(
            InitialText(longText, "Long Text"),
            InitialText(shortText, "Short Text")
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun singleReplace() {
        benchmarkRule.measureRepeated {
            val buffer = runWithTimingDisabled {
                PartialGapBuffer(initText.text)
            }

            buffer.replace(5, 10, "Android")
        }
    }

    @Test
    fun replace10timesContinued() {
        benchmarkRule.measureRepeated {
            val buffer = runWithTimingDisabled {
                PartialGapBuffer(initText.text)
            }

            for (i in 0 until 10) {
                buffer.replace(5 + i, 10 + i, "Android")
            }
        }
    }

    @Test
    fun replace10timesDiscontinued() {
        benchmarkRule.measureRepeated {
            val buffer = runWithTimingDisabled {
                PartialGapBuffer(initText.text)
            }

            for (i in 0 until 10) {
                if (i % 2 == 0) {
                    buffer.replace(5 + i, 10 + i, "Android")
                } else {
                    buffer.replace(buffer.length - 10 - i, buffer.length - 5 - i, "Android")
                }
            }
        }
    }

    @Test
    fun toStringAfterReplace() {
        benchmarkRule.measureRepeated {
            val buffer = runWithTimingDisabled {
                PartialGapBuffer(initText.text).apply {
                    replace(5, 10, "Android")
                }
            }

            buffer.toString()
        }
    }
}