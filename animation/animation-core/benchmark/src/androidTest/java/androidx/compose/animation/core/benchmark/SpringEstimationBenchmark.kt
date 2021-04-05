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

package androidx.compose.animation.core.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.animation.core.estimateAnimationDurationMillis
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@LargeTest
@RunWith(AndroidJUnit4::class)
class SpringEstimationBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    @LargeTest
    fun benchmarkCriticallyDamped() {
        val dampingCoeff = 2.0 * sqrt(491.0 * 4500.0)
        benchmarkRule.measureRepeated {
            estimateAnimationDurationMillis(
                mass = 491.0,
                springConstant = 4500.0,
                dampingCoefficient = dampingCoeff,
                initialDisplacement = -3000.0,
                initialVelocity = 800.0,
                delta = 1.0
            )
        }
    }

    @Test
    @LargeTest
    fun benchmarkOverDamped() {
        benchmarkRule.measureRepeated {
            estimateAnimationDurationMillis(
                mass = 81.0,
                springConstant = 130321.0,
                dampingCoefficient = 6501.0,
                initialDisplacement = -200000.0,
                initialVelocity = 4200.0,
                delta = 1.0
            )
        }
    }

    @Test
    @LargeTest
    fun benchmarkUnderDamped() {
        benchmarkRule.measureRepeated {
            estimateAnimationDurationMillis(
                mass = 1.0,
                springConstant = 1.0,
                dampingCoefficient = 1.0,
                initialDisplacement = -200000.0,
                initialVelocity = 100.0,
                delta = 1.0
            )
        }
    }
}