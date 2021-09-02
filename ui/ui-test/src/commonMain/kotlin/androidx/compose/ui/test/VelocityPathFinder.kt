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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
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
     * `b = [velocity]` and `T = [durationMillis]`.
     *
     * See the graphs in https://www.desmos.com/calculator/nfk9urzq2h and play around with the
     * different inputs. In those graphs, x = t, y(x) = f(t), p_0 = start, p_n = end,
     * and a_fixed is a when d = T.
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
        val d = if (start == end) {
            T.toDouble()
        } else {
            min(T.toDouble(), 2 / velocity * (end - start))
        }
        val a = (start + velocity * d - end) / (d * d)

        check(d >= min(T, 100)) {
            val requestedDistance = (endPosition - startPosition).getDistance()
            // 1) Decrease duration to d
            val suggestedDuration = d
            // 2) Decrease velocity to (end - start) / 50 -> should work for vectors too
            val suggestedVelocity = (requestedDistance / 50) * 1000
            // 3) Increase distance to 50 * velocity
            val suggestedDistance = 50 * endVelocity / 1000
            "Unable to generate a swipe gesture between $startPosition and $endPosition with " +
                "duration $durationMillis that ends with velocity of $endVelocity, without going " +
                "outside of the range [start..end]. " +
                "Suggested fixes: " +
                "1. set duration to $suggestedDuration or lower; " +
                "2. set velocity to $suggestedVelocity or lower; or " +
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
}
