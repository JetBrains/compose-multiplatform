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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Tests of [VelocityPathFinder] creates paths that will lead to the desired velocity.
 */
@RunWith(Parameterized::class)
class VelocityPathFinderTest(private val config: TestConfig) {
    data class TestConfig(
        val end: Offset,
        val requestedVelocity: Float,
        val durationMillis: Long,
        val expectedError: Boolean
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<TestConfig>().apply {
            for (direction in Direction.values()) {
                // An essential value in the VelocityPathFinder is `d`, calculated as
                // `d = min(T.toDouble(), 2 / velocity * (end - start))`, and represents the
                // duration of the polynomial that will result in the correct velocity, from the
                // start to the end position. T is the duration of the swipe.

                // Different scenarios to test are:
                // - d < 100 (because VelocityTracker uses the last 100ms of the polynomial)
                // - 100 < d < T (swipe must wait at start until d ms left)
                // - d > T (swipe can start immediately)
                // - v == 0

                add(TestConfig(direction.offset, 0f, 100L, false)) // v == 0, small T
                add(TestConfig(direction.offset, 0f, 500L, false)) // v == 0, medium T
                add(TestConfig(direction.offset, 0f, 1500L, false)) // v == 0, large T
                add(TestConfig(direction.offset, 500f, 500L, false)) // T < d
                add(TestConfig(direction.offset, 1500f, 500L, false)) // 100 < d < T
                add(TestConfig(direction.offset, 6000f, 500L, false)) // d < 100 && T > d
                add(TestConfig(direction.offset, 6000f, 66L, false)) // d < 100 && T < d
            }
            // Regression for b/182477143
            add(TestConfig(Offset(424.8f, 0f) - Offset(295.2f, 0f), 2000f, 3000L, false))
            // Same as above, but for T = 100
            add(TestConfig(Offset(129.6f, 0f), 2000f, 100L, false))
        }
    }

    @Test
    fun test() {
        if (config.expectedError) {
            testWithExpectedError(config, testSuggestions = true)
        } else {
            testWithoutExpectedError(config)
        }
    }

    private fun testWithoutExpectedError(config: TestConfig) {
        val pathFinder = VelocityPathFinder(
            startPosition = Offset.Zero,
            endPosition = config.end,
            endVelocity = config.requestedVelocity,
            durationMillis = config.durationMillis
        )

        val f = pathFinder.generateFunction()
        val velocityTracker = simulateSwipe(config, f)
        val velocity = velocityTracker.calculateVelocity()

        val velocityTolerance = .1f * config.requestedVelocity // 10% of the expected value
        assertThat(velocity.sum()).isWithin(velocityTolerance).of(config.requestedVelocity)
        if (config.requestedVelocity > 0) {
            // Direction of velocity of 0 is undefined, so any direction is correct
            velocity.toOffset().normalize().isAlmostEqualTo(config.end.normalize(), 0.03f)
        }
        // At t = 0, the function should return the start position (which is Offset.Zero here)
        f(0).isAlmostEqualTo(Offset.Zero)
        // At any time, the function should be between the start and end
        for (t in 0..config.durationMillis) {
            assertThat(f(t).x).isAlmostBetween(0f, config.end.x)
            assertThat(f(t).y).isAlmostBetween(0f, config.end.y)
        }
        // At t = durationMillis, the function should return the end position
        f(config.durationMillis).isAlmostEqualTo(config.end)
    }

    private fun testWithExpectedError(config: TestConfig, testSuggestions: Boolean = false) {
        try {
            VelocityPathFinder(
                startPosition = Offset.Zero,
                endPosition = config.end,
                endVelocity = config.requestedVelocity,
                durationMillis = config.durationMillis
            ).generateFunction()
            fail("Expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).startsWith(
                "Unable to generate a swipe gesture between ${Offset.Zero} and ${config.end} " +
                    "with duration ${config.durationMillis} that ends with velocity of " +
                    "${config.requestedVelocity} px/s, without going outside of the range " +
                    "[start..end]. Suggested fixes: "
            )

            val suggestedFixes = e.message!!.substringAfter("Suggested fixes: ")
            val (maxDuration, maxVelocity, minDistance) = getSuggestions(suggestedFixes)

            // Verify that the suggestions change the current config
            assertThat(maxDuration).isLessThan(config.durationMillis.toFloat())
            assertThat(maxVelocity).isLessThan(config.requestedVelocity)
            assertThat(minDistance).isGreaterThan(config.end.getDistance())

            if (testSuggestions) {
                // Try just inside the suggested value range
                testWithoutExpectedError(config.copy(durationMillis = floor(maxDuration).toLong()))
                testWithoutExpectedError(config.copy(requestedVelocity = maxVelocity * 0.999f))
                val goodDistance = config.end * (minDistance * 1.001f / config.end.getDistance())
                testWithoutExpectedError(config.copy(end = goodDistance))

                // Try just outside the suggested value range
                testWithExpectedError(config.copy(durationMillis = floor(maxDuration).toLong() + 1))
                testWithExpectedError(config.copy(requestedVelocity = maxVelocity * 1.001f))
                val badDistance = config.end * (minDistance * 0.999f / config.end.getDistance())
                testWithExpectedError(config.copy(end = badDistance))
            }
        }
    }

    private val suggestedFixesRegex = Regex(
        "1\\. set duration to (.*) or lower; " +
            "2\\. set velocity to (.*) px/s or lower; or " +
            "3\\. increase the distance between the start and end to (.*) or higher"
    )

    private fun getSuggestions(suggestedFixes: String): List<Float> {
        val match = suggestedFixesRegex.matchEntire(suggestedFixes)
        assertThat(match).isNotNull()
        assertThat(match!!.groups).hasSize(4)
        return match.groupValues.subList(1, 4).map { it.toFloat() }
    }

    private fun simulateSwipe(config: TestConfig, f: (Long) -> Offset): VelocityTracker {
        val velocityTracker = VelocityTracker()
        val steps = max(1, (config.durationMillis / eventPeriodMillis.toFloat()).roundToInt())
        for (step in 0..steps) {
            val progress = step / steps.toFloat()
            val t = lerp(0, config.durationMillis, progress)
            velocityTracker.addPosition(t, f(t))
        }
        return velocityTracker
    }

    private fun Offset.normalize(): Offset =
        if (isFinite && this != Offset.Zero) this / getDistance() else this

    private fun Velocity.toOffset(): Offset = Offset(x, y)
    private fun Velocity.sum(): Float = sqrt(x * x + y * y)

    /**
     * Direction of the swipe, when starting from [Offset.Zero].
     * N/W/S/E are straight lines, NW/SW/SE/NE are at a 60º angle.
     */
    enum class Direction(val offset: Offset) {
        N(Offset(0f, -200f)),
        NW(Offset(-100f, -173.2f)),
        W(Offset(-200f, 0f)),
        SW(Offset(-173.2f, 100f)),
        S(Offset(0f, 200f)),
        SE(Offset(100f, 173.2f)),
        E(Offset(200f, 0f)),
        NE(Offset(173.2f, -100f))
    }
}
