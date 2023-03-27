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
package androidx.compose.foundation.layout.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ColorLerpBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun lerpSrgb() {
        benchmarkRule.measureRepeated {
            repeat(1001) {
                lerp(Color.Blue, Color.White, it / 1000f)
            }
        }
    }

    @Test
    fun lerpOklab() {
        val blue = Color.Blue.convert(ColorSpaces.Oklab)
        val green = Color.Green.convert(ColorSpaces.Oklab)
        benchmarkRule.measureRepeated {
            repeat(1001) {
                lerp(blue, green, it / 1000f)
            }
        }
    }

    @Test
    fun lerpCieLab() {
        val blue = Color.Blue.convert(ColorSpaces.CieLab)
        val green = Color.Green.convert(ColorSpaces.CieLab)
        benchmarkRule.measureRepeated {
            repeat(1001) {
                lerp(blue, green, it / 1000f)
            }
        }
    }
}