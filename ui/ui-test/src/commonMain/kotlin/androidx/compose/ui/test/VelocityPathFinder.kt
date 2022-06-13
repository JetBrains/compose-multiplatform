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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

internal class VelocityPathFinder(
    val startPosition: Offset,
    val endPosition: Offset,
    val endVelocity: Float,
    val durationMillis: Long
) {
    private val vx: Double
    private val vy: Double

    init {
        // Decompose v into its x and y components
        val delta = endPosition - startPosition
        val theta = atan2(delta.y.toDouble(), delta.x.toDouble())
        // Note: it would be more precise to do `theta = atan2(-y, x)`, because atan2 expects a
        // coordinate system where positive y goes up and in our coordinate system positive y goes
        // down. However, in that case we would also have to inverse `vy` to convert the velocity
        // back to our own coordinate system. But then it's just a double negation, so we can skip
        // both conversions entirely.

        // VelocityTracker internally calculates px/ms, not px/s
        vx = cos(theta) * endVelocity / 1000
        vy = sin(theta) * endVelocity / 1000
    }

    fun generateFunction(): (Long) -> Offset {
        val fx = createFunctionForVelocity(vx, Offset::x)
        val fy = createFunctionForVelocity(vy, Offset::y)
        return { t -> Offset(fx(t), fy(t)) }
    }

    /**
     * Generates a function f(t) where `f(0) = start`, `f(T) = end`, and the polynomial fit over
     * the last 100ms is of the form `f(t) = a*(t-T)^2 + b*(t-T) + c`, with
     * `start = [value].invoke([startPosition])`, `end = [value].invoke([endPosition])`,
     * `b = [velocity]` and `T = [durationMillis]`. Note that this implies `f'(T) = [velocity]`.
     *
     * There are three different shapes that the function can take: a flat line, a flat line
     * followed by a parabola that starts with `f'(t) = 0`, or a parabola that starts with
     * `f'(0) > 0`.
     *
     * 1. Flat line:
     * This happens when start == end and requires that the requested velocity is 0.
     *
     * 2. Flat line followed by a parabola:
     * This happens when there is a parabola that satisfies `f(t_d) = start`, `f'(t_d) = 0`,
     * `f'(T) = velocity` and `t_d >= 0`. The gesture will wait at the start location until t_d
     * and then follow that parabola till `f(T) = end`.
     *
     * 3. Parabola that starts with `f'(0) > 0`:
     * If there is a parabola that satisfies `f(t_d) = start`, `f'(t_d) = 0`, `f'(T) = velocity`,
     * but `t_d < 0`; or if `velocity = 0` (in which case the previously mentioned parabola
     * doesn't exist); we can't follow that parabola because we'd have to start following it in
     * the past (`t_d < 0`). Instead, it can be shown that in this case we can always create a
     * parabola that satisfies `f(0) = start`, `f(T) = end` and `f'(T) = velocity`. This parabola
     * will have `f'(0) > 0`.
     *
     * In the calculations below, instead of calculating t_d, we calculate `d = T - t_d`, and
     * immediately cap it to T.
     *
     * @param velocity The desired velocity in the x or y direction at the end position
     */
    private fun createFunctionForVelocity(
        velocity: Double,
        value: Offset.() -> Float
    ): (Long) -> Float {
        val T = durationMillis
        val start = value.invoke(startPosition)
        val end = value.invoke(endPosition)
        // `d = T - t_d` in scenario 2 (see documentation above)
        // `d = T` in scenario 1 and 3 (see documentation above)
        val d = if (start == end) {
            T.toDouble()
        } else {
            min(T.toDouble(), 2 / velocity * (end - start))
        }
        val a = (start + velocity * d - end) / (d * d)

        require(d >= min(T, HorizonMilliseconds)) {
            val requestedDistance = (endPosition - startPosition).getDistance()
            // 1) Decrease duration to d
            val suggestedDuration = d
            // 2) Decrease velocity to 2/100 * (end - start) -> should work for vectors too
            val suggestedVelocity = (2f / min(T, HorizonMilliseconds)) * requestedDistance * 1000
            // 3) Increase distance to 100/2 * velocity
            val suggestedDistance = (min(T, HorizonMilliseconds) / 2f) * endVelocity / 1000
            "Unable to generate a swipe gesture between $startPosition and $endPosition with " +
                "duration $durationMillis that ends with velocity of $endVelocity px/s, without " +
                "going outside of the range [start..end]. " +
                "Suggested fixes: " +
                "1. set duration to $suggestedDuration or lower; " +
                "2. set velocity to $suggestedVelocity px/s or lower; or " +
                "3. increase the distance between the start and end to $suggestedDistance or " +
                "higher"
        }

        val threshold = T - d
        return { t: Long ->
            when {
                t < threshold -> start
                // `f(t) = a*(t-T)^2 + b*(t-T) + c`
                else -> a * (t - T) * (t - T) + velocity * (t - T) + end
            }.toFloat()
        }
    }

    companion object {
        // TODO(b/204895043): Taken from VelocityTrackerKt.HorizonMilliseconds. Must stay the same.
        private const val HorizonMilliseconds: Long = 100
        private const val DefaultDurationMilliseconds: Long = 200

        /**
         * Calculates a duration for a gesture such that a valid swipe can be generated for that
         * gesture that starts at [start] and ends at [end] with the given [endVelocity].
         *
         * In most cases the duration is going to be 200ms, except for a few edge cases where it
         * would not be possible to generate a valid swipe for the given requirements. If no
         * duration exist for which it would be possible to generate a valid swipe that meets the
         * requirements, and [IllegalArgumentException] is thrown.
         */
        fun calculateDefaultDuration(start: Offset, end: Offset, endVelocity: Float): Long {
            require(endVelocity >= 0f) {
                "Velocity cannot be $endVelocity, it must be positive"
            }
            require(start != end || endVelocity == 0f) {
                "When start == end; velocity cannot be $endVelocity, it must be 0f"
            }

            val distance = (end - start).getDistance()
            /** For an explanation of `d`, see [createFunctionForVelocity]. */
            // Times 1000 because velocity is in px/s and our time unit is ms.
            val d = 2 / endVelocity * distance * 1000

            // Referring to the graphs mentioned in the kdoc of createFunctionForVelocity;
            // d = 0: start == end and velocity > 0           not possible (already checked for)
            // d = NaN: start == end and velocity == 0        T=200 (scenario 1)
            // d = Infinity: start != end and velocity == 0   T=200 (scenario 3)
            // d > 200: start != end and velocity > 0         T=200 (scenario 3)
            // d > HorizonMs: start != end and velocity > 0   T=200 (scenario 2)
            // d <= HorizonMs: start != end and velocity > 0  T=d   (scenario 3)

            if (d.isNaN() || d > HorizonMilliseconds) {
                return DefaultDurationMilliseconds
            }

            // d <= HorizonMilliseconds, so we have to pick `T = d`. But, when d is very small,
            // this leads to a duration too short to even get a velocity.
            // Check and throw if this is the case.
            val minimumDuration = ceil(2.5f * eventPeriodMillis).roundToLong()
            require(floor(d).roundToLong() >= minimumDuration) {
                // Nope. This won't work.
                val suggestedVelocity = (2f / minimumDuration) * distance * 1000
                val suggestedDistance = .5f * minimumDuration * endVelocity / 1000
                "Unable to generate a swipe gesture between $start and $end that ends with " +
                    "velocity of $endVelocity px/s, without going outside of the range " +
                    "[start..end]. " +
                    "Suggested fixes: " +
                    "1. set velocity to $suggestedVelocity px/s or lower; or " +
                    "2. increase the distance between the start and end to " +
                    "$suggestedDistance or higher"
            }
            return floor(d).roundToLong()
        }
    }
}
