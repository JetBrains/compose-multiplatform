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

package androidx.compose.foundation.animation

import kotlin.math.abs
import kotlin.math.ln

// copy-paste of
// src/androidMain/kotlin/androidx/compose/foundation/animation/AndroidFlingSpline.kt

private const val Inflection = 0.35f // Tension lines cross at (Inflection, 1)
private const val StartTension = 0.5f
private const val EndTension = 1.0f
private const val P1 = StartTension * Inflection
private const val P2 = 1.0f - EndTension * (1.0f - Inflection)

private fun computeSplineInfo(
    splinePositions: FloatArray,
    splineTimes: FloatArray,
    nbSamples: Int
) {
    var xMin = 0.0f
    var yMin = 0.0f
    for (i in 0 until nbSamples) {
        val alpha = i.toFloat() / nbSamples
        var xMax = 1.0f
        var x: Float
        var tx: Float
        var coef: Float
        while (true) {
            x = xMin + (xMax - xMin) / 2.0f
            coef = 3.0f * x * (1.0f - x)
            tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x
            if (abs(tx - alpha) < 1E-5) break
            if (tx > alpha) xMax = x else xMin = x
        }
        splinePositions[i] = coef * ((1.0f - x) * StartTension + x) + x * x * x
        var yMax = 1.0f
        var y: Float
        var dy: Float
        while (true) {
            y = yMin + (yMax - yMin) / 2.0f
            coef = 3.0f * y * (1.0f - y)
            dy = coef * ((1.0f - y) * StartTension + y) + y * y * y
            if (abs(dy - alpha) < 1E-5) break
            if (dy > alpha) yMax = y else yMin = y
        }
        splineTimes[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y
    }
    splineTimes[nbSamples] = 1.0f
    splinePositions[nbSamples] = splineTimes[nbSamples]
}

/**
 * The native Android fling scroll spline and the ability to sample it.
 *
 * Ported from `android.widget.Scroller`.
 */
internal object AndroidFlingSpline {
    private const val NbSamples = 100
    private val SplinePositions = FloatArray(NbSamples + 1)
    private val SplineTimes = FloatArray(NbSamples + 1)

    init {
        // TODO This function used to be directly implemented in this init block, but it causes a
        // crash in the IR compiler.
        computeSplineInfo(SplinePositions, SplineTimes, NbSamples)
    }

    /**
     * Compute an instantaneous fling position along the scroller spline.
     *
     * @param time progress through the fling animation from 0-1
     */
    fun flingPosition(time: Float): FlingResult {
        val index = (NbSamples * time).toInt()
        var distanceCoef = 1f
        var velocityCoef = 0f
        if (index < NbSamples) {
            val tInf = index.toFloat() / NbSamples
            val tSup = (index + 1).toFloat() / NbSamples
            val dInf = SplinePositions[index]
            val dSup = SplinePositions[index + 1]
            velocityCoef = (dSup - dInf) / (tSup - tInf)
            distanceCoef = dInf + (time - tInf) * velocityCoef
        }
        return FlingResult(
            distanceCoefficient = distanceCoef,
            velocityCoefficient = velocityCoef
        )
    }

    /**
     * The rate of deceleration along the spline motion given [velocity] and [friction].
     */
    fun deceleration(velocity: Float, friction: Float): Double =
        ln(Inflection * abs(velocity) / friction.toDouble())

    /**
     * Result coefficients of a scroll computation
     */
    // TODO: pack this into an inline class
    data class FlingResult(
        /**
         * Linear distance traveled from 0-1, from source (0) to destination (1)
         */
        val distanceCoefficient: Float,
        /**
         * Instantaneous velocity coefficient at this point in the fling expressed in
         * total distance per unit time
         */
        val velocityCoefficient: Float
    )
}