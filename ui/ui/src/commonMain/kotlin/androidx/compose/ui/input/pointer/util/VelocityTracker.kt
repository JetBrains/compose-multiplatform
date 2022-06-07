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

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

private const val AssumePointerMoveStoppedMilliseconds: Int = 40
private const val HistorySize: Int = 20
// TODO(b/204895043): Keep value in sync with VelocityPathFinder.HorizonMilliSeconds
private const val HorizonMilliseconds: Int = 100
private const val MinSampleSize: Int = 3

/**
 * Calculate the total impulse provided to the screen and the resulting velocity.
 *
 * The touchscreen is modeled as a physical object.
 * Initial condition is discussed below, but for now suppose that v(t=0) = 0
 *
 * The kinetic energy of the object at the release is E=0.5*m*v^2
 * Then vfinal = sqrt(2E/m). The goal is to calculate E.
 *
 * The kinetic energy at the release is equal to the total work done on the object by the finger.
 * The total work W is the sum of all dW along the path.
 *
 * dW = F*dx, where dx is the piece of path traveled.
 * Force is change of momentum over time, F = dp/dt = m dv/dt.
 * Then substituting:
 * dW = m (dv/dt) * dx = m * v * dv
 *
 * Summing along the path, we get:
 * W = sum(dW) = sum(m * v * dv) = m * sum(v * dv)
 * Since the mass stays constant, the equation for final velocity is:
 * vfinal = sqrt(2*sum(v * dv))
 *
 * Here,
 * dv : change of velocity = (v[i+1]-v[i])
 * dx : change of distance = (x[i+1]-x[i])
 * dt : change of time = (t[i+1]-t[i])
 * v : instantaneous velocity = dx/dt
 *
 * The final formula is:
 * vfinal = sqrt(2) * sqrt(sum((v[i]-v[i-1])*|v[i]|)) for all i
 * The absolute value is needed to properly account for the sign. If the velocity over a
 * particular segment descreases, then this indicates braking, which means that negative
 * work was done. So for two positive, but decreasing, velocities, this contribution would be
 * negative and will cause a smaller final velocity.
 *
 * Initial condition
 * There are two ways to deal with initial condition:
 * 1) Assume that v(0) = 0, which would mean that the screen is initially at rest.
 * This is not entirely accurate. We are only taking the past X ms of touch data, where X is
 * currently equal to 100. However, a touch event that created a fling probably lasted for longer
 * than that, which would mean that the user has already been interacting with the touchscreen
 * and it has probably already been moving.
 * 2) Assume that the touchscreen has already been moving at a certain velocity, calculate this
 * initial velocity and the equivalent energy, and start with this initial energy.
 * Consider an example where we have the following data, consisting of 3 points:
 *                 time: t0, t1, t2
 *                 x   : x0, x1, x2
 *                 v   :  0, v1, v2
 * Here is what will happen in each of these scenarios:
 * 1) By directly applying the formula above with the v(0) = 0 boundary condition, we will get
 * vfinal = sqrt(2*(|v1|*(v1-v0) + |v2|*(v2-v1))). This can be simplified since v0=0
 * vfinal = sqrt(2*(|v1|*v1 + |v2|*(v2-v1))) = sqrt(2*(v1^2 + |v2|*(v2 - v1)))
 * since velocity is a real number
 * 2) If we treat the screen as already moving, then it must already have an energy (per mass)
 * equal to 1/2*v1^2. Then the initial energy should be 1/2*v1*2, and only the second segment
 * will contribute to the total kinetic energy (since we can effectively consider that v0=v1).
 * This will give the following expression for the final velocity:
 * vfinal = sqrt(2*(1/2*v1^2 + |v2|*(v2-v1)))
 * This analysis can be generalized to an arbitrary number of samples.
 *
 *
 * Comparing the two equations above, we see that the only mathematical difference
 * is the factor of 1/2 in front of the first velocity term.
 * This boundary condition would allow for the "proper" calculation of the case when all of the
 * samples are equally spaced in time and distance, which should suggest a constant velocity.
 *
 * Note that approach 2) is sensitive to the proper ordering of the data in time, since
 * the boundary condition must be applied to the oldest sample to be accurate.
 */
private fun kineticEnergyToVelocity(work: Float): Float {
    return sign(work) * sqrt(2 * abs(work))
}

private class ImpulseCalculator {
    private var work = 0f
    private var previousT: Long = Long.MAX_VALUE
    private var previousX: Float = Float.NaN
    private var initialCondition = true

    /**
     * Return the velocity, in pixels/second.
     * Even though the input time is in milliseconds, we convert to second inside this function
     * because it provides a more stable numerical behaviour.
     */
    fun getVelocity(): Float {
        return kineticEnergyToVelocity(work)
    }

    fun addPosition(timeMillis: Long, x: Float) {
        // t[i] is in milliseconds, but due to FP arithmetic, convert to seconds
        val SecondsPerMs = 0.001f
        if (previousT == Long.MAX_VALUE || previousX.isNaN()) {
            previousT = timeMillis
            previousX = x
            // This is a first data point, nothing to compute here
            return
        }
        if (timeMillis == previousT) {
            previousX = x
            // Should never happen, but for stability, skip this sample
            return
        }
        val vprev = kineticEnergyToVelocity(work) // v[i-1]
        val vcurr = (x - previousX) / (SecondsPerMs * (timeMillis - previousT)) // v[i]
        work += (vcurr - vprev) * abs(vcurr)
        if (initialCondition) {
            work *= 0.5f
            initialCondition = false
        }
        previousT = timeMillis
        previousX = x
    }

    fun reset() {
        work = 0f
        previousT = Long.MAX_VALUE
        previousX = Float.NaN
        initialCondition = true
    }
}

/**
 * Computes a pointer's velocity.
 *
 * The input data is provided by calling [addPosition]. Adding data is cheap. Ensure that all data
 * for the gesture of interest is added.
 *
 * To obtain a velocity, call [calculateVelocity]. This will
 * compute the velocity based on the data added so far. Only call this when
 * you need to use the velocity, since the computation is relatively expensive.
 */
class VelocityTracker {
    // Circular buffer; current sample at index.
    private val samples: Array<PointAtTime?> = Array(HistorySize) { null }
    private var index: Int = 0
    private val useImpulse = true

    /**
     * Adds a position as the given time to the tracker.
     *
     * Call resetTracking to remove added Offsets.
     *
     * @see resetTracking
     */
    fun addPosition(timeMillis: Long, position: Offset) {
        index = (index + 1) % HistorySize
        samples[index] = PointAtTime(position, timeMillis)
    }

    /**
     * Computes the estimated velocity of the pointer at the time of the last provided data point.
     *
     * This can be expensive. Only call this when you need the velocity.
     */
    fun calculateVelocity(): Velocity {
        if (useImpulse) {
            return getImpulseVelocity()
        }
        val lsq2estimate = getLsq2VelocityEstimate().pixelsPerSecond
        return Velocity(lsq2estimate.x, lsq2estimate.y)
    }

    private fun getImpulseVelocity(): Velocity {
        var sampleCount = 0

        // The sample at index is our newest sample.  If it is null, we have no samples so return.
        val newestSample: PointAtTime = samples[index] ?: return Velocity(0f, 0f)

        var previousSample: PointAtTime = newestSample

        // Starting with the most recent PointAtTime sample, iterate backwards while
        // the samples represent continuous motion.
        val xCalculator = ImpulseCalculator()
        val yCalculator = ImpulseCalculator()
        var i: Int = index
        do {
            i = (i + 1) % HistorySize
            val sample: PointAtTime = samples[i] ?: continue

            val age = newestSample.time - sample.time
            val delta = abs(sample.time - previousSample.time)
            previousSample = newestSample
            if (age > HorizonMilliseconds) {
                continue // skip the old samples
            }

            if (delta > AssumePointerMoveStoppedMilliseconds) {
                xCalculator.reset()
                yCalculator.reset()
            }

            xCalculator.addPosition(-age, sample.point.x)
            yCalculator.addPosition(-age, sample.point.y)

            sampleCount += 1
        } while (i != index && sampleCount < HistorySize)

        if (sampleCount < MinSampleSize) {
            // Compatibility behaviour: if we only have 2 points, return 0 velocity.
            // Some tests needs this.
            return Velocity(0f, 0f)
        }

        val xVelocity = xCalculator.getVelocity()
        val yVelocity = yCalculator.getVelocity()

        return Velocity(xVelocity, yVelocity)
    }

    /**
     * Clears the tracked positions added by [addPosition].
     */
    fun resetTracking() {
        samples.fill(element = null)
    }

    /**
     * Returns an estimate of the velocity of the object being tracked by the
     * tracker given the current information available to the tracker.
     *
     * Information is added using [addPosition].
     *
     * Returns null if there is no data on which to base an estimate.
     */
    private fun getLsq2VelocityEstimate(): VelocityEstimate {
        val x: MutableList<Float> = mutableListOf()
        val y: MutableList<Float> = mutableListOf()
        val time: MutableList<Float> = mutableListOf()
        var sampleCount = 0
        var index: Int = index

        // The sample at index is our newest sample.  If it is null, we have no samples so return.
        val newestSample: PointAtTime = samples[index] ?: return VelocityEstimate.None

        var previousSample: PointAtTime = newestSample
        var oldestSample: PointAtTime = newestSample

        // Starting with the most recent PointAtTime sample, iterate backwards while
        // the samples represent continuous motion.
        do {
            val sample: PointAtTime = samples[index] ?: break

            val age: Float = (newestSample.time - sample.time).toFloat()
            val delta: Float =
                abs(sample.time - previousSample.time).toFloat()
            previousSample = sample
            if (age > HorizonMilliseconds || delta > AssumePointerMoveStoppedMilliseconds) {
                break
            }

            oldestSample = sample
            val position: Offset = sample.point
            x.add(position.x)
            y.add(position.y)
            time.add(-age)
            index = (if (index == 0) HistorySize else index) - 1

            sampleCount += 1
        } while (sampleCount < HistorySize)

        if (sampleCount >= MinSampleSize) {
            try {
                val xFit: PolynomialFit = polyFitLeastSquares(time, x, 2)
                val yFit: PolynomialFit = polyFitLeastSquares(time, y, 2)

                // The 2nd coefficient is the derivative of the quadratic polynomial at
                // x = 0, and that happens to be the last timestamp that we end up
                // passing to polyFitLeastSquares for both x and y.
                val xSlope = xFit.coefficients[1]
                val ySlope = yFit.coefficients[1]
                return VelocityEstimate(
                    pixelsPerSecond = Offset(
                        // Convert from pixels/ms to pixels/s
                        (xSlope * 1000),
                        (ySlope * 1000)
                    ),
                    confidence = xFit.confidence * yFit.confidence,
                    durationMillis = newestSample.time - oldestSample.time,
                    offset = newestSample.point - oldestSample.point
                )
            } catch (exception: IllegalArgumentException) {
                // TODO(b/129494918): Is catching an exception here something we really want to do?
                return VelocityEstimate.None
            }
        }

        // We're unable to make a velocity estimate but we did have at least one
        // valid pointer position.
        return VelocityEstimate(
            pixelsPerSecond = Offset.Zero,
            confidence = 1.0f,
            durationMillis = newestSample.time - oldestSample.time,
            offset = newestSample.point - oldestSample.point
        )
    }
}

/**
 * Track the positions and timestamps inside this event change.
 *
 * For optimal tracking, this should be called for the DOWN event and all MOVE
 * events, including any touch-slop-captured MOVE event.
 *
 * @param event Pointer change to track.
 */
fun VelocityTracker.addPointerInputChange(event: PointerInputChange) {
    @OptIn(ExperimentalComposeUiApi::class)
    event.historical.fastForEach {
        addPosition(it.uptimeMillis, it.position)
    }
    addPosition(event.uptimeMillis, event.position)
}

private data class PointAtTime(val point: Offset, val time: Long)

/**
 * A two dimensional velocity estimate.
 *
 * VelocityEstimates are computed by [VelocityTracker.getVelocityEstimate]. An
 * estimate's [confidence] measures how well the velocity tracker's position
 * data fit a straight line, [durationMillis] is the time that elapsed between the
 * first and last position sample used to compute the velocity, and [offset]
 * is similarly the difference between the first and last positions.
 *
 * See also:
 *
 *  * VelocityTracker, which computes [VelocityEstimate]s.
 *  * Velocity, which encapsulates (just) a velocity vector and provides some
 *    useful velocity operations.
 */
private data class VelocityEstimate(
    /** The number of pixels per second of velocity in the x and y directions. */
    val pixelsPerSecond: Offset,
    /**
     * A value between 0.0 and 1.0 that indicates how well [VelocityTracker]
     * was able to fit a straight line to its position data.
     *
     * The value of this property is 1.0 for a perfect fit, 0.0 for a poor fit.
     */
    val confidence: Float,
    /**
     * The time that elapsed between the first and last position sample used
     * to compute [pixelsPerSecond].
     */
    val durationMillis: Long,
    /**
     * The difference between the first and last position sample used
     * to compute [pixelsPerSecond].
     */
    val offset: Offset
) {
    companion object {
        val None = VelocityEstimate(Offset.Zero, 1f, 0, Offset.Zero)
    }
}

/**
 *  TODO (shepshapard): If we want to support varying weights for each position, we could accept a
 *  3rd FloatArray of weights for each point and use them instead of the [DefaultWeight].
 */
private const val DefaultWeight = 1f

/**
 * Fits a polynomial of the given degree to the data points.
 *
 * If the [degree] is larger than or equal to the number of points, a polynomial will be returned
 * with coefficients of the value 0 for all degrees larger than or equal to the number of points.
 * For example, if 2 data points are provided and a quadratic polynomial (degree of 2) is requested,
 * the resulting polynomial ax^2 + bx + c is guaranteed to have a = 0;
 *
 * Throws an IllegalArgumentException if:
 * <ul>
 *   <li>[degree] is not a positive integer.
 *   <li>[x] and [y] are not the same size.
 *   <li>[x] or [y] are empty.
 *   <li>(some other reason that
 * </ul>
 *
 */
internal fun polyFitLeastSquares(
    /** The x-coordinates of each data point. */
    x: List<Float>,
    /** The y-coordinates of each data point. */
    y: List<Float>,
    degree: Int
): PolynomialFit {
    if (degree < 1) {
        throw IllegalArgumentException("The degree must be at positive integer")
    }
    if (x.size != y.size) {
        throw IllegalArgumentException("x and y must be the same length")
    }
    if (x.isEmpty()) {
        throw IllegalArgumentException("At least one point must be provided")
    }

    val truncatedDegree =
        if (degree >= x.size) {
            x.size - 1
        } else {
            degree
        }

    val coefficients = MutableList(degree + 1) { 0.0f }

    // Shorthands for the purpose of notation equivalence to original C++ code.
    val m: Int = x.size
    val n: Int = truncatedDegree + 1

    // Expand the X vector to a matrix A, pre-multiplied by the weights.
    val a = Matrix(n, m)
    for (h in 0 until m) {
        a.set(0, h, DefaultWeight)
        for (i in 1 until n) {
            a.set(i, h, a.get(i - 1, h) * x[h])
        }
    }

    // Apply the Gram-Schmidt process to A to obtain its QR decomposition.

    // Orthonormal basis, column-major ordVectorer.
    val q = Matrix(n, m)
    // Upper triangular matrix, row-major order.
    val r = Matrix(n, n)
    for (j in 0 until n) {
        for (h in 0 until m) {
            q.set(j, h, a.get(j, h))
        }
        for (i in 0 until j) {
            val dot: Float = q.getRow(j) * q.getRow(i)
            for (h in 0 until m) {
                q.set(j, h, q.get(j, h) - dot * q.get(i, h))
            }
        }

        val norm: Float = q.getRow(j).norm()
        if (norm < 0.000001) {
            // TODO(b/129494471): Determine what this actually means and see if there are
            // alternatives to throwing an Exception here.

            // Vectors are linearly dependent or zero so no solution.
            throw IllegalArgumentException(
                "Vectors are linearly dependent or zero so no " +
                    "solution. TODO(shepshapard), actually determine what this means"
            )
        }

        val inverseNorm: Float = 1.0f / norm
        for (h in 0 until m) {
            q.set(j, h, q.get(j, h) * inverseNorm)
        }
        for (i in 0 until n) {
            r.set(j, i, if (i < j) 0.0f else q.getRow(j) * a.getRow(i))
        }
    }

    // Solve R B = Qt W Y to find B. This is easy because R is upper triangular.
    // We just work from bottom-right to top-left calculating B's coefficients.
    val wy = Vector(m)
    for (h in 0 until m) {
        wy[h] = y[h] * DefaultWeight
    }
    for (i in n - 1 downTo 0) {
        coefficients[i] = q.getRow(i) * wy
        for (j in n - 1 downTo i + 1) {
            coefficients[i] -= r.get(i, j) * coefficients[j]
        }
        coefficients[i] /= r.get(i, i)
    }

    // Calculate the coefficient of determination (confidence) as:
    //   1 - (sumSquaredError / sumSquaredTotal)
    // ...where sumSquaredError is the residual sum of squares (variance of the
    // error), and sumSquaredTotal is the total sum of squares (variance of the
    // data) where each has been weighted.
    var yMean = 0.0f
    for (h in 0 until m) {
        yMean += y[h]
    }
    yMean /= m

    var sumSquaredError = 0.0f
    var sumSquaredTotal = 0.0f
    for (h in 0 until m) {
        var term = 1.0f
        var err: Float = y[h] - coefficients[0]
        for (i in 1 until n) {
            term *= x[h]
            err -= term * coefficients[i]
        }
        sumSquaredError += DefaultWeight * DefaultWeight * err * err
        val v = y[h] - yMean
        sumSquaredTotal += DefaultWeight * DefaultWeight * v * v
    }

    val confidence =
        if (sumSquaredTotal <= 0.000001f) 1f else 1f - (sumSquaredError / sumSquaredTotal)

    return PolynomialFit(coefficients, confidence)
}

internal data class PolynomialFit(
    val coefficients: List<Float>,
    val confidence: Float
)

private class Vector(
    val length: Int
) {
    val elements: Array<Float> = Array(length) { 0.0f }

    operator fun get(i: Int) = elements[i]

    operator fun set(i: Int, value: Float) {
        elements[i] = value
    }

    operator fun times(a: Vector): Float {
        var result = 0.0f
        for (i in 0 until length) {
            result += this[i] * a[i]
        }
        return result
    }

    fun norm(): Float = sqrt(this * this)
}

private class Matrix(rows: Int, cols: Int) {
    private val elements: Array<Vector> = Array(rows) { Vector(cols) }

    fun get(row: Int, col: Int): Float {
        return elements[row][col]
    }

    fun set(row: Int, col: Int, value: Float) {
        elements[row][col] = value
    }

    fun getRow(row: Int): Vector {
        return elements[row]
    }
}