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

package androidx.compose.animation.core

import android.os.Build
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

@RunWith(Parameterized::class)
class SpringEstimationTest(private val m: Double, private val k: Double) {
    companion object {
        private const val TwoFrames60fpsMillis = 33L

        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Array<out Any>> {
            return mutableListOf<Array<out Any>>().apply {
                (1..100 step 2).map { it * it }.forEach { m ->
                    (1..1000 step 20).map { it * it }.forEach { k ->
                        add(arrayOf(m.toDouble(), k.toDouble()))
                    }
                }
                // Additional edge cases to test for
                add(arrayOf(10_000.0, 1.0))
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N) // parallelStream() requires API level 24
    fun runTestCases() {
        val failedTestCaseResults = mutableListOf<TestCaseResult>()

        val testCases = generateTestCases()
        println("Generated ${testCases.size} test cases")

        testCases.parallelStream().forEach {
            val res = runTestCase(it)
            if (!res.pass) {
                synchronized(failedTestCaseResults) {
                    failedTestCaseResults.add(res)
                }
            }
        }

        if (failedTestCaseResults.isNotEmpty()) {
            println("Failed ${failedTestCaseResults.size} test cases")
            failedTestCaseResults.forEach {
                println(it.testCase)
                if (it.reason.isNotBlank()) {
                    println("\treason:${it.reason}")
                }
            }
        }

        assertTrue(failedTestCaseResults.isEmpty())
    }

    private fun runTestCase(testCase: TestCase): TestCaseResult {
        val springSimulation = SpringSimulation(0.0f)
        springSimulation.dampingRatio = testCase.dampingRatio.toFloat()
        springSimulation.stiffness = testCase.stiffness.toFloat()

        val endTime = estimateAnimationDurationMillis(
            mass = testCase.mass,
            springConstant = testCase.springConstant,
            dampingCoefficient = testCase.dampingCoefficient,
            initialDisplacement = testCase.initialDisplacement,
            initialVelocity = testCase.initialVelocity,
            delta = 1.0
        )

        val alternateEndTime = estimateAnimationDurationMillis(
            stiffness = testCase.stiffness,
            dampingRatio = testCase.dampingRatio,
            initialDisplacement = testCase.initialDisplacement,
            initialVelocity = testCase.initialVelocity,
            delta = 1.0
        )

        // Test that the alternate implementation gives the same answer within 1ms.
        if (abs(endTime - alternateEndTime) > 1) {
            return TestCaseResult(
                pass = false,
                testCase = testCase,
                reason = "stiffness/dampingRatio implementation discrepancy"
            )
        }

        if (endTime == Long.MAX_VALUE)
            return TestCaseResult(false, testCase, "End time +infinity")

        val simTwoFramesAfter = springSimulation.updateValues(
            lastDisplacement = testCase.initialDisplacement.toFloat(),
            lastVelocity = testCase.initialVelocity.toFloat(),
            timeElapsed = endTime + TwoFrames60fpsMillis
        )
        val simTwoFramesBefore = springSimulation.updateValues(
            lastDisplacement = testCase.initialDisplacement.toFloat(),
            lastVelocity = testCase.initialVelocity.toFloat(),
            timeElapsed = max(endTime - TwoFrames60fpsMillis, 0L)
        )
        val simAtTime = springSimulation.updateValues(
            lastDisplacement = testCase.initialDisplacement.toFloat(),
            lastVelocity = testCase.initialVelocity.toFloat(),
            timeElapsed = endTime
        )

        val pass = if (testCase.dampingRatio >= 1.0) {
            // The primary success criterion is that two frames before the settling time, the
            // function x(t) is greater than the threshold and two frames after.

            // A secondary criterion is added to account for scenarios where the settling time is
            // close to the inflection point in over/critically-damped cases, and therefore the
            // before and after times are both below the threshold.
            (
                (
                    abs(simTwoFramesBefore.value) >= 0.999 &&
                        abs(simTwoFramesAfter.value) <= 1.001
                    ) || (
                    abs(simAtTime.value) >= 0.999 &&
                        abs(simTwoFramesBefore.value) < abs(simAtTime.value) &&
                        abs(simTwoFramesAfter.value) < abs(simAtTime.value)
                    )
                )
        } else {
            // In the under-damped scenario, x(t) varies heavily due to oscillations, therefore
            // the over/critically damped conditions may fail erroneously.
            abs(simTwoFramesAfter.value) < 1.00
        }

        return TestCaseResult(pass, testCase)
    }

    private fun generateTestCases(): List<TestCase> {
        val testCases = mutableListOf<TestCase>()

        // Generate general test cases that broadly cover the over and under damped test cases
        for (c in 1..10_000 step 500) {
            for (v0 in -200_000..200_000 step 100_000) {
                for (p0 in -10_000..10_000 step 100) {
                    if (!(v0 == 0 && p0 == 0)) {
                        val testCase = TestCase(
                            mass = m,
                            springConstant = k,
                            dampingCoefficient = c.toDouble(),
                            initialVelocity = v0.toDouble(),
                            initialDisplacement = p0.toDouble()
                        )
                        synchronized(testCases) {
                            testCases.add(
                                testCase
                            )
                        }
                    }
                }
            }
        }

        // Generate specific test cases that cover the critically damped test cases

        // Guarantee a damping ratio of 1.0 by fixing c such that
        // c^2 = 4mk
        val c = 2.0 * sqrt(k * m)
        for (v0 in -200_000..200_000 step 10_000) {
            for (p0 in -10_000..10_000 step 100) {
                if (!(v0 == 0 && p0 == 0)) {
                    val testCase = TestCase(
                        mass = m,
                        springConstant = k,
                        dampingCoefficient = c,
                        initialVelocity = v0.toDouble(),
                        initialDisplacement = p0.toDouble()
                    )

                    synchronized(testCases) {
                        testCases.add(
                            testCase
                        )
                    }
                }
            }
        }
        return testCases
    }

    private data class TestCase(
        val mass: Double,
        val springConstant: Double,
        val dampingCoefficient: Double,
        val initialVelocity: Double,
        val initialDisplacement: Double
    ) {
        val dampingRatio: Double
            get() {
                val criticalDamping = 2.0 * sqrt(springConstant * mass)
                return dampingCoefficient / criticalDamping
            }
        val stiffness: Double
            get() {
                return springConstant / mass
            }
    }

    private data class TestCaseResult(
        val pass: Boolean,
        val testCase: TestCase,
        val reason: String = ""
    )
}