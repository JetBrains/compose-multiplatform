/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.VelocityPathFinder
import androidx.compose.ui.test.util.isAlmostBetween
import androidx.compose.ui.test.util.isAlmostEqualTo
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.lerp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.IllegalArgumentException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Test to check if [VelocityPathFinder.calculateDefaultDuration] calculates a duration for
 * which it is possible to generate a swipe from start to end with a certain endVelocity.
 */
@RunWith(Parameterized::class)
class VelocityPathFinderCalculateDurationTest(private val config: TestConfig) {
    data class TestConfig(
        val end: Offset,
        val requestedVelocity: Float,
        val expectedDurationMillis: Long? = null,
        val expectSuggestions: Boolean = false,
        val expectedError: Regex? = if (expectSuggestions) errorWithSuggestions else null,
        val tolerance: Float = 0.1f,
    )

    companion object {
        private val DistanceZero = Offset.Zero
        private val Distance100 = Offset(100f, 0f)

        private val errorNegativeVelocity = Regex("Velocity cannot be .*, it must be positive")
        private val errorPositiveVelocity =
            Regex("When start == end; velocity cannot be .*, it must be 0f")
        private val errorWithSuggestions = Regex(
            "Unable to generate a swipe gesture between .* and .* that ends with " +
                "velocity of .* px/s, without going outside of the range " +
                "\\[start\\.\\.end]\\. Suggested fixes: .*"
        )
        private val suggestedFixesRegex = Regex(
            "1\\. set velocity to (.*) px/s or lower; or " +
                "2\\. increase the distance between the start and end to (.*) or higher"
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf(
            // An essential value in the VelocityPathFinder is `d`, calculated as
            // `d = 2 / velocity * (end - start)`, and represents the maximum time over which we
            // can spread out a gesture going from start to end while still following a polynomial
            // that has v=velocity at the end. (Note that this necessarily has v=0 at the start:
            // if v<0 at the start, the path would go out of the range [start, end], and if v>0 we
            // would arrive at the end faster.)
            //
            // Without going into details as to why, there are basically 3 different ranges of d
            // to test for:
            //
            // 1. d < 40
            //    The requested velocity is so high that the "slowest" polynomial to reach it
            //    still takes less than 40ms, which is the minimum duration. We expect the
            //    duration suggestion to throw an IllegalArgumentException, and suggest changes
            //    to the input that would _just_ take us to that 40ms minimum.
            // 2. d >= 100
            //    We can make a valid path with any desired duration. The suggestion picks a
            //    default of 200ms.
            // 3. 40 <= d < 100
            //    The entire gesture must follow an exact polynomial, which is only possible if
            //    the suggested duration < d. We expect the suggestion to be floor(d).
            //
            // A few other scenarios to test for are:
            // - start == end
            //   Gesture can't move at all, so velocity _must_ be 0. Expect an error otherwise.
            // - velocity < 0
            //   Simply not possible. Expect an error always.

            TestConfig(DistanceZero, -1f, expectedError = errorNegativeVelocity),
            TestConfig(DistanceZero, 0f, expectedDurationMillis = 200L),
            TestConfig(DistanceZero, 1f, expectedError = errorPositiveVelocity),
            TestConfig(Distance100, -1f, expectedError = errorNegativeVelocity),
            TestConfig(Distance100, 0f, expectedDurationMillis = 200L),
            TestConfig(Distance100, 1f, expectedDurationMillis = 200L),

            TestConfig(Distance100, 1999f, expectedDurationMillis = 200L,
                    tolerance = 1f), // d > 100
            TestConfig(Distance100, 2000f, expectedDurationMillis = 100L), // d = 100
            TestConfig(Distance100, 2480f, expectedDurationMillis = 80L,
                    tolerance = 1f), // d ≈ 80.65
            TestConfig(Distance100, 5000f, expectedError = errorWithSuggestions), // d = 40
            TestConfig(Distance100, 5001f, expectedError = errorWithSuggestions), // d < 40
        )
    }

    @Test
    fun test() {
        if (config.expectedError != null) {
            testWithExpectedError(config, testSuggestions = config.expectSuggestions)
        } else {
            testWithoutExpectedError(config)
        }
    }

    private fun testWithoutExpectedError(config: TestConfig) {
        val actualDuration = VelocityPathFinder.calculateDefaultDuration(
            start = Offset.Zero,
            end = config.end,
            endVelocity = config.requestedVelocity
        )
        assertThat(actualDuration).isEqualTo(config.expectedDurationMillis)

        val pathFinder = VelocityPathFinder(
            startPosition = Offset.Zero,
            endPosition = config.end,
            endVelocity = config.requestedVelocity,
            durationMillis = actualDuration
        )

        val f = pathFinder.generateFunction()
        val velocityTracker = simulateSwipe(f, actualDuration)
        val velocity = velocityTracker.calculateVelocity()

        assertThat(velocity.sum()).isWithin(config.tolerance).of(config.requestedVelocity)
        if (config.requestedVelocity > 0) {
            // Direction of velocity of 0 is undefined, so any direction is correct
            velocity.toOffset().normalize().isAlmostEqualTo(config.end.normalize())
        }
        // At t = 0, the function should return the start position (which is Offset.Zero here)
        f(0).isAlmostEqualTo(Offset.Zero)
        // At any time, the function should be between the start and end
        for (t in 0..actualDuration) {
            assertThat(f(t).x).isAlmostBetween(0f, config.end.x)
            assertThat(f(t).y).isAlmostBetween(0f, config.end.y)
        }
        // At t = durationMillis, the function should return the end position
        f(actualDuration).isAlmostEqualTo(config.end)
    }

    private fun testWithExpectedError(config: TestConfig, testSuggestions: Boolean = false) {
        try {
            VelocityPathFinder.calculateDefaultDuration(
                start = Offset.Zero,
                end = config.end,
                endVelocity = config.requestedVelocity
            )
            fail("Expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).matches(config.expectedError!!.toPattern())

            if (testSuggestions) {
                val suggestedFixes = e.message!!.substringAfter("Suggested fixes: ")
                val (maxVelocity, minDistance) = getSuggestions(suggestedFixes)

                // Verify that the suggestions change the current config
                assertThat(maxVelocity).isLessThan(config.requestedVelocity)
                assertThat(minDistance).isGreaterThan(config.end.getDistance())

                // Try just inside the suggested value range
                testWithoutExpectedError(
                    config.copy(
                        requestedVelocity = maxVelocity * 0.999f,
                        // suggestions are designed to hit the 40L boundary
                        expectedDurationMillis = 40L
                    )
                )
                testWithoutExpectedError(
                    config.copy(
                        end = Offset(minDistance * 1.001f, 0f),
                        // suggestions are designed to hit the 40L boundary
                        expectedDurationMillis = 40L
                    )
                )

                // Try just outside the suggested value range
                testWithExpectedError(config.copy(requestedVelocity = maxVelocity * 1.001f))
                testWithExpectedError(config.copy(end = Offset(minDistance * 0.999f, 0f)))
            }
        }
    }

    private fun getSuggestions(suggestedFixes: String): List<Float> {
        val match = suggestedFixesRegex.matchEntire(suggestedFixes)
        assertThat(match).isNotNull()
        assertThat(match!!.groups).hasSize(3)
        return match.groupValues.subList(1, 3).map { it.toFloat() }
    }

    private fun simulateSwipe(f: (Long) -> Offset, durationMillis: Long): VelocityTracker {
        val velocityTracker = VelocityTracker()
        val steps = max(1, (durationMillis / eventPeriodMillis.toFloat()).roundToInt())
        for (step in 0..steps) {
            val progress = step / steps.toFloat()
            val t = lerp(0, durationMillis, progress)
            velocityTracker.addPosition(t, f(t))
        }
        return velocityTracker
    }

    private fun Offset.normalize(): Offset =
        if (isFinite && this != Offset.Zero) this / getDistance() else this

    private fun Velocity.toOffset(): Offset = Offset(x, y)
    private fun Velocity.sum(): Float = sqrt(x * x + y * y)
}
