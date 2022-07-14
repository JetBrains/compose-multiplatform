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

package androidx.compose.animation.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.math.absoluteValue

/**
 * Easing is a way to adjust an animation’s fraction. Easing allows transitioning
 * elements to speed up and slow down, rather than moving at a constant rate.
 *
 * Fraction is a value between 0 and 1.0 indicating our current point in
 * the animation where 0 represents the start and 1.0 represents the end.
 *
 * An [Easing] must map fraction=0.0 to 0.0 and fraction=1.0 to 1.0.
 */
@Stable
fun interface Easing {
    fun transform(fraction: Float): Float
}

/**
 * Elements that begin and end at rest use this standard easing. They speed up quickly
 * and slow down gradually, in order to emphasize the end of the transition.
 *
 * Standard easing puts subtle attention at the end of an animation, by giving more
 * time to deceleration than acceleration. It is the most common form of easing.
 *
 * This is equivalent to the Android `FastOutSlowInInterpolator`
 */
val FastOutSlowInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

/**
 * Incoming elements are animated using deceleration easing, which starts a transition
 * at peak velocity (the fastest point of an element’s movement) and ends at rest.
 *
 * This is equivalent to the Android `LinearOutSlowInInterpolator`
 */
val LinearOutSlowInEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/**
 * Elements exiting a screen use acceleration easing, where they start at rest and
 * end at peak velocity.
 *
 * This is equivalent to the Android `FastOutLinearInInterpolator`
 */
val FastOutLinearInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

/**
 * It returns fraction unmodified. This is useful as a default value for
 * cases where a [Easing] is required but no actual easing is desired.
 */
val LinearEasing: Easing = Easing { fraction -> fraction }

/**
 * A cubic polynomial easing.
 *
 * The [CubicBezierEasing] class implements third-order Bézier curves.
 *
 * This is equivalent to the Android `PathInterpolator`
 *
 * Rather than creating a new instance, consider using one of the common
 * cubic [Easing]s:
 *
 * @see FastOutSlowInEasing
 * @see LinearOutSlowInEasing
 * @see FastOutLinearInEasing
 *
 * @param a The x coordinate of the first control point.
 *          The line through the point (0, 0) and the first control point is tangent
 *          to the easing at the point (0, 0).
 * @param b The y coordinate of the first control point.
 *          The line through the point (0, 0) and the first control point is tangent
 *          to the easing at the point (0, 0).
 * @param c The x coordinate of the second control point.
 *          The line through the point (1, 1) and the second control point is tangent
 *          to the easing at the point (1, 1).
 * @param d The y coordinate of the second control point.
 *          The line through the point (1, 1) and the second control point is tangent
 *          to the easing at the point (1, 1).
 */
@Immutable
class CubicBezierEasing(
    private val a: Float,
    private val b: Float,
    private val c: Float,
    private val d: Float
) : Easing {

    init {
        require(!a.isNaN() && !b.isNaN() && !c.isNaN() && !d.isNaN()) {
            "Parameters to CubicBezierEasing cannot be NaN. Actual parameters are: $a, $b, $c, $d."
        }
    }

    private fun evaluateCubic(a: Float, b: Float, m: Float): Float {
        return 3 * a * (1 - m) * (1 - m) * m +
            3 * b * (1 - m) * /*    */ m * m +
            /*                      */ m * m * m
    }

    override fun transform(fraction: Float): Float {
        if (fraction > 0f && fraction < 1f) {
            var start = 0.0f
            var end = 1.0f
            while (true) {
                val midpoint = (start + end) / 2
                val estimate = evaluateCubic(a, c, midpoint)
                if ((fraction - estimate).absoluteValue < CubicErrorBound)
                    return evaluateCubic(b, d, midpoint)
                if (estimate < fraction)
                    start = midpoint
                else
                    end = midpoint
            }
        } else {
            return fraction
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is CubicBezierEasing && a == other.a && b == other.b && c == other.c &&
            d == other.d
    }

    override fun hashCode(): Int {
        return ((a.hashCode() * 31 + b.hashCode()) * 31 + c.hashCode()) * 31 + d.hashCode()
    }
}

private const val CubicErrorBound: Float = 0.001f
