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

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.input.pointer.util.VelocityTracker1D.Strategy
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// Velocities between (1-Tolerance)*RV and (1+Tolerance)*RV are accepted
// where RV is the "Real Velocity"
private const val Tolerance: Float = 0.2f

@RunWith(JUnit4::class)
class VelocityTracker1DTest {

    @Test
    fun lsq2_differentialValues_unsupported() {
        assertThrows(IllegalStateException::class.java) {
            VelocityTracker1D(isDataDifferential = true, Strategy.Lsq2)
        }
    }
    @Test
    fun twoPoints_nonDifferentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(DataPointAtTime(1, 5f), DataPointAtTime(2, 15f)),
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Impulse, 10000f),
                    // 3 is the minimum number of data points for Lsq2, so expect a 0 velocity.
                    ExpectedVelocity(Strategy.Lsq2, 0f),
                )
            )
        )
    }

    @Test
    fun twoPoints_differentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(DataPointAtTime(1, 5f), DataPointAtTime(2, 10f)),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 10000f))
            )
        )
    }

    @Test
    fun threePoints_pointerStoppedMoving_nonDifferentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(25, 25f),
                    DataPointAtTime(50, 50f),
                    DataPointAtTime(100, 100f),
                ),
                // Expect 0 velocities, as the pointer will be considered to have stopped moving,
                // due to the (100-50)=40ms gap from the last data point (i.e. it's effectively
                // a data set with only 1 data point).
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Lsq2, 0f),
                    ExpectedVelocity(Strategy.Impulse, 0f)
                ),
            )
        )
    }

    @Test
    fun threePoints_pointerStoppedMoving_differentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(25, 25f),
                    DataPointAtTime(50, 50f),
                    DataPointAtTime(100, 100f),
                ),
                // Expect 0 velocities, as the pointer will be considered to have stopped moving,
                // due to the (100-50)=40ms gap from the last data point (i.e. it's effectively
                // a data set with only 1 data point).
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Impulse, 0f)
                ),
            )
        )
    }

    /** Impulse strategy specific test cases. */
    @Test
    fun threePoints_zeroVelocity_nonDifferentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 273f),
                    DataPointAtTime(1, 273f),
                    DataPointAtTime(2, 273f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 0f))
            ),
        )
    }

    @Test
    fun threePoints_zeroVelocity_differentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(1, 0f),
                    DataPointAtTime(2, 0f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 0f))
            ),
        )
    }

    @Test
    fun resetTracking_defaultConstructor() {
        // Fixed velocity at 5 points per 10 milliseconds
        val tracker = VelocityTracker1D()
        tracker.addDataPoint(0, 0f)
        tracker.addDataPoint(10, 5f)
        tracker.addDataPoint(20, 10f)
        tracker.addDataPoint(30, 15f)
        tracker.addDataPoint(40, 30f)

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isZero()
    }

    @Test
    fun resetTracking_differentialValues_impulse() {
        // Fixed velocity at 5 points per 10 milliseconds
        val tracker = VelocityTracker1D(isDataDifferential = true, Strategy.Impulse)
        tracker.addDataPoint(0, 0f)
        tracker.addDataPoint(10, 5f)
        tracker.addDataPoint(20, 10f)
        tracker.addDataPoint(30, 15f)
        tracker.addDataPoint(40, 30f)

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isZero()
    }

    @Test
    fun resetTracking_nonDifferentialValues_impulse() {
        // Fixed velocity at 5 points per 10 milliseconds
        val tracker = VelocityTracker1D(isDataDifferential = false, Strategy.Impulse)
        tracker.addDataPoint(0, 0f)
        tracker.addDataPoint(10, 5f)
        tracker.addDataPoint(20, 10f)
        tracker.addDataPoint(30, 15f)
        tracker.addDataPoint(40, 30f)

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isZero()
    }

    @Test
    fun resetTracking_nonDifferentialValues_lsq2() {
        // Fixed velocity at 5 points per 10 milliseconds
        val tracker = VelocityTracker1D(isDataDifferential = false, Strategy.Lsq2)
        tracker.addDataPoint(0, 0f)
        tracker.addDataPoint(10, 5f)
        tracker.addDataPoint(20, 10f)
        tracker.addDataPoint(30, 15f)
        tracker.addDataPoint(40, 30f)

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isZero()
    }

    @Test
    fun linearMotion_positiveVelocity_positiveDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(10, 5f),
                    DataPointAtTime(20, 10f),
                    DataPointAtTime(30, 15f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 500f)),
            )
        )
    }

    @Test
    fun linearMotion_positiveVelocity_positiveDataPoints_differentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(10, 5f),
                    DataPointAtTime(20, 5f),
                    DataPointAtTime(30, 5f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 500f)),
            )
        )
    }

    @Test
    fun linearMotion_positiveVelocity_negativeDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, -20f),
                    DataPointAtTime(10, -15f),
                    DataPointAtTime(20, -10f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 500f)),
            )
        )
    }

    @Test
    fun linearMotion_positiveVelocity_mixedSignDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, -5f),
                    DataPointAtTime(10, 0f),
                    DataPointAtTime(20, 5f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 500f)),
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_negativeDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(10, -5f),
                    DataPointAtTime(20, -10f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, -500f)),
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_negativeDataPoints_differentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(0, 5f),
                    DataPointAtTime(10, -5f),
                    DataPointAtTime(20, -5f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, -500f)),
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_postiveDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 20f),
                    DataPointAtTime(10, 15f),
                    DataPointAtTime(20, 10f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, -500f)),
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_mixedSignDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 5f),
                    DataPointAtTime(10, 0f),
                    DataPointAtTime(20, -5f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, -500f)),
            )
        )
    }

    @Test
    fun linearHalfMotion() {
        // Stay still for 50 ms, and then move 100 points in the final 50 ms.
        // The final line is sloped at 2 units/ms.
        // This can be visualized as 2 lines: flat line (50ms), and line with slope of 2 units/ms.
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(10, 0f),
                    DataPointAtTime(20, 0f),
                    DataPointAtTime(30, 0f),
                    DataPointAtTime(40, 0f),
                    DataPointAtTime(50, 0f),
                    DataPointAtTime(60, 20f),
                    DataPointAtTime(70, 40f),
                    DataPointAtTime(80, 60f),
                    DataPointAtTime(90, 80f),
                    DataPointAtTime(100, 100f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 2000f))
            ),
        )
    }

    @Test
    fun linearHalfMotionSampled() {
        // Linear half motion, but sampled much less frequently. The resulting velocity is higher
        // than the previous test, because the path looks significantly different now if you
        // were to just plot these points.
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(30, 0f),
                    DataPointAtTime(40, 0f),
                    DataPointAtTime(70, 40f),
                    DataPointAtTime(100, 100f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 2018.2f))
            )
        )
    }

    @Test
    fun linearMotionFollowedByFlatLine() {
        // Fixed velocity at first, but flat line afterwards.
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(10, 10f),
                    DataPointAtTime(20, 20f),
                    DataPointAtTime(30, 30f),
                    DataPointAtTime(40, 40f),
                    DataPointAtTime(50, 50f),
                    DataPointAtTime(60, 50f),
                    DataPointAtTime(70, 50f),
                    DataPointAtTime(80, 50f),
                    DataPointAtTime(90, 50f),
                    DataPointAtTime(100, 50f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 1000f))
            )
        )
    }

    @Test
    fun linearMotionFollowedByFlatLineWithoutIntermediatePoints() {
        // Fixed velocity at first, but flat line afterwards
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 0f),
                    DataPointAtTime(50, 50f),
                    DataPointAtTime(100, 50f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 0f))
            ),
        )
    }

    @Test
    fun pixelWatch_rsb_scrollDown_thenUp_thenDown() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(2580534, -0.0334f),
                    DataPointAtTime(2580549, -0.1336f),
                    DataPointAtTime(2580566, -0.2505f),
                    DataPointAtTime(2580581, -0.1837f),
                    DataPointAtTime(2580597, -0.2672f),
                    DataPointAtTime(2580613, -0.5511f),
                    DataPointAtTime(2580635, -0.6012f),
                    DataPointAtTime(2580661, -0.4509f),
                    DataPointAtTime(2580683, -0.4175f),
                    DataPointAtTime(2580705, -0.1503f),
                    DataPointAtTime(2580722, -0.0167f),
                    DataPointAtTime(2580786, 0.0501f),
                    DataPointAtTime(2580801, 0.1503f),
                    DataPointAtTime(2580822, 0.3006f),
                    DataPointAtTime(2580838, 0.3006f),
                    DataPointAtTime(2580854, 0.4008f),
                    DataPointAtTime(2580869, 0.5177f),
                    DataPointAtTime(2580886, 0.501f),
                    DataPointAtTime(2580905, 0.3507f),
                    DataPointAtTime(2580921, 0.3507f),
                    DataPointAtTime(2580937, 0.0668f),
                    DataPointAtTime(2580974, 0.0167f),
                    DataPointAtTime(2581034, -0.0668f),
                    DataPointAtTime(2581049, -0.1169f),
                    DataPointAtTime(2581070, -0.3173f),
                    DataPointAtTime(2581086, -0.2004f),
                    DataPointAtTime(2581101, -0.2338f),
                    DataPointAtTime(2581118, -0.4175f),
                    DataPointAtTime(2581134, -0.4175f),
                    DataPointAtTime(2581150, -0.3674f),
                    DataPointAtTime(2581166, -0.2672f),
                    DataPointAtTime(2581181, -0.1503f),
                    DataPointAtTime(2581199, -0.0668f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, -9.73f))
            )
        )
    }

    /** Device generated test cases. */
    @Test
    fun pixelWatch_rsb_scrollDown() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(224598, -0.0501f),
                    DataPointAtTime(224621, -0.1336f),
                    DataPointAtTime(224645, -0.5511f),
                    DataPointAtTime(224669, -0.8016f),
                    DataPointAtTime(224687, -1.0354f),
                    DataPointAtTime(224706, -0.4843f),
                    DataPointAtTime(224738, -0.334f),
                    DataPointAtTime(224754, -0.0835f)
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, -27.86f))
            )
        )
    }

    @Test
    fun pxielWatch_rsb_scrollUp() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = true,
                dataPoints = listOf(
                    DataPointAtTime(269606, 0.0501f),
                    DataPointAtTime(269626, 0.2171f),
                    DataPointAtTime(269641, 0.2672f),
                    DataPointAtTime(269658, 0.2672f),
                    DataPointAtTime(269674, 0.2672f),
                    DataPointAtTime(269690, 0.3674f),
                    DataPointAtTime(269706, 0.5511f),
                    DataPointAtTime(269722, 0.501f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 31.92f))
            )
        )
    }

    @Test
    fun swordfishFlingDown_xValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 271f),
                    DataPointAtTime(16, 269.786346f),
                    DataPointAtTime(35, 267.983063f),
                    DataPointAtTime(52, 262.638397f),
                    DataPointAtTime(68, 266.138824f),
                    DataPointAtTime(85, 274.79245f),
                    DataPointAtTime(96, 274.79245f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 623.57f))
            )
        )
    }

    @Test
    fun swordfishFlingDown_yValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(0, 96f),
                    DataPointAtTime(16, 106.922775f),
                    DataPointAtTime(35, 156.660034f),
                    DataPointAtTime(52, 220.339081f),
                    DataPointAtTime(68, 331.581116f),
                    DataPointAtTime(85, 428.113159f),
                    DataPointAtTime(96, 428.113159f),
                ),
                expectedVelocities = listOf(ExpectedVelocity(Strategy.Impulse, 5970.73f))
            )
        )
    }

    @Test
    fun sailfishFlingUpSlow_xValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(235089067, 528.0f),
                    DataPointAtTime(235089084, 527.0f),
                    DataPointAtTime(235089093, 527.0f),
                    DataPointAtTime(235089095, 527.0f),
                    DataPointAtTime(235089101, 527.0f),
                    DataPointAtTime(235089110, 528.0f),
                    DataPointAtTime(235089112, 528.25f),
                    DataPointAtTime(235089118, 531.0f),
                    DataPointAtTime(235089126, 535.0f),
                    DataPointAtTime(235089129, 536.33f),
                    DataPointAtTime(235089135, 540.0f),
                    DataPointAtTime(235089144, 546.0f),
                    DataPointAtTime(235089146, 547.21f),
                    DataPointAtTime(235089152, 553.0f),
                    DataPointAtTime(235089160, 559.0f),
                    DataPointAtTime(235089162, 560.66f),
                ),
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Impulse, 764.34f),
                    ExpectedVelocity(Strategy.Lsq2, 951.69f),
                )
            )
        )
    }

    @Test
    fun sailfishFlingUpSlow_yValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(235089067, 983.0f),
                    DataPointAtTime(235089084, 981.0f),
                    DataPointAtTime(235089093, 977.0f),
                    DataPointAtTime(235089095, 975.93f),
                    DataPointAtTime(235089101, 970.0f),
                    DataPointAtTime(235089110, 960.0f),
                    DataPointAtTime(235089112, 957.51f),
                    DataPointAtTime(235089118, 946.0f),
                    DataPointAtTime(235089126, 931.0f),
                    DataPointAtTime(235089129, 926.02f),
                    DataPointAtTime(235089135, 914.0f),
                    DataPointAtTime(235089144, 896.0f),
                    DataPointAtTime(235089146, 892.36f),
                    DataPointAtTime(235089152, 877.0f),
                    DataPointAtTime(235089160, 851.0f),
                    DataPointAtTime(235089162, 843.82f),
                ),
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Impulse, -3604.82f),
                    ExpectedVelocity(Strategy.Lsq2, -3044.96f),
                )
            )
        )
    }

    @Test
    fun sailfishFlingUpFast_xValues() {
        // Some "repeated" data points are removed, since the conversion from ns to ms made some
        // data ponits "repeated"
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(920922, 561.0f),
                    DataPointAtTime(920930, 559.0f),
                    DataPointAtTime(920938, 559.0f),
                    DataPointAtTime(920947, 562.91f),
                    DataPointAtTime(920955, 577.0f),
                    DataPointAtTime(920963, 596.87f),
                    DataPointAtTime(920972, 631.0f),
                    DataPointAtTime(920980, 671.31f),
                    DataPointAtTime(920989, 715.0f),
                ),
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Impulse, 5670.32f),
                    ExpectedVelocity(Strategy.Lsq2, 5991.87f),
                )
            )
        )
    }

    @Test
    fun sailfishFlingUpFast_yValues() {
        // Some "repeated" data points are removed, since the conversion from ns to ms made some
        // data ponits "repeated"
        checkTestCase(
            VelocityTrackingTestCase(
                differentialDataPoints = false,
                dataPoints = listOf(
                    DataPointAtTime(920922, 1412.0f),
                    DataPointAtTime(920930, 1377.0f),
                    DataPointAtTime(920938, 1371.0f),
                    DataPointAtTime(920947, 1342.68f),
                    DataPointAtTime(920955, 1272.0f),
                    DataPointAtTime(920963, 1190.54f),
                    DataPointAtTime(920972, 1093.0f),
                    DataPointAtTime(920980, 994.68f),
                    DataPointAtTime(920989, 903.0f),
                ),
                expectedVelocities = listOf(
                    ExpectedVelocity(Strategy.Impulse, -13021.10f),
                    ExpectedVelocity(Strategy.Lsq2, -15094f),
                )
            )
        )
    }

    /** Test cases derived from [VelocityTrackerTest]. */
    @Test
    fun testsFromThe2DVelocityTrackerTest() {
        var xDataPoints: MutableList<DataPointAtTime> = mutableListOf()
        var yDataPoints: MutableList<DataPointAtTime> = mutableListOf()

        var i = 0
        velocityEventData.forEach {
            if (it.down) {
                xDataPoints.add(DataPointAtTime(it.uptime, it.position.x))
                yDataPoints.add(DataPointAtTime(it.uptime, it.position.y))
            } else {
                // Check velocity along the X axis
                checkTestCase(
                    VelocityTrackingTestCase(
                        differentialDataPoints = false,
                        dataPoints = xDataPoints,
                        expectedVelocities = listOf(
                            ExpectedVelocity(
                                Strategy.Lsq2, expected2DVelocities[i].first
                            )
                        )
                    ),
                )
                // Check velocity along the Y axis
                checkTestCase(
                    VelocityTrackingTestCase(
                        differentialDataPoints = false,
                        dataPoints = yDataPoints,
                        expectedVelocities = listOf(
                            ExpectedVelocity(
                                Strategy.Lsq2, expected2DVelocities[i].second
                            )
                        )
                    ),
                )
                xDataPoints = mutableListOf()
                yDataPoints = mutableListOf()
                i += 1
            }
        }
    }

    private fun checkTestCase(testCase: VelocityTrackingTestCase) {
        testCase.expectedVelocities.forEach { expectedVelocity ->
            val tracker = VelocityTracker1D(
                testCase.differentialDataPoints,
                expectedVelocity.strategy
            )
            testCase.dataPoints.forEach {
                tracker.addDataPoint(it.time, it.dataPoint)
            }

            assertWithMessage("Wrong velocity for data points: ${testCase.dataPoints}" +
                "\nExpected velocity: {$expectedVelocity}")
                .that(tracker.calculateVelocity())
                .isWithin(abs(expectedVelocity.velocity) * Tolerance)
                .of(expectedVelocity.velocity)
        }
    }
}

/** Represents an expected [velocity] for a velocity tracking [strategy]. */
private data class ExpectedVelocity(val strategy: Strategy, val velocity: Float)

/** Holds configs for a velocity tracking test case, for convenience. */
private data class VelocityTrackingTestCase(
    val differentialDataPoints: Boolean,
    val dataPoints: List<DataPointAtTime>,
    val expectedVelocities: List<ExpectedVelocity>
)