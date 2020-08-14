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

package androidx.compose.ui.gesture.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.Uptime
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.inMilliseconds
import kotlin.math.absoluteValue

private const val AssumePointerMoveStoppedMilliseconds: Int = 40
private const val HistorySize: Int = 20
private const val HorizonMilliseconds: Int = 100
private const val MinSampleSize: Int = 3

/**
 * Computes a pointer's velocity.
 *
 * The input data is provided by calling [addPosition]. Adding data is cheap.
 *
 * To obtain a velocity, call [calculateVelocity] or [getVelocityEstimate]. This will
 * compute the velocity based on the data added so far. Only call these when
 * you need to use the velocity, as they are comparatively expensive.
 *
 * The quality of the velocity estimation will be better if more data points
 * have been received.
 */
class VelocityTracker {

    // Circular buffer; current sample at index.
    private val samples: Array<PointAtTime?> = Array(HistorySize) { null }
    private var index: Int = 0

    /**
     * Adds a position as the given time to the tracker.
     *
     * Call resetTracking to remove added Offsets.
     *
     * @see resetTracking
     */
    // TODO(shepshapard): VelocityTracker needs to be updated to be passed vectors instead of
    //   positions. For velocity tracking, the only thing that is important is the change in
    //   position over time.
    fun addPosition(uptime: Uptime, position: Offset) {
        index = (index + 1) % HistorySize
        samples[index] = PointAtTime(position, uptime)
    }

    /**
     * Computes the estimated velocity of the pointer at the time of the last provided data point.
     *
     * This can be expensive. Only call this when you need the velocity.
     */
    fun calculateVelocity() = Velocity(pixelsPerSecond = getVelocityEstimate().pixelsPerSecond)

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
    private fun getVelocityEstimate(): VelocityEstimate {
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

            val age: Float = (newestSample.time - sample.time).inMilliseconds().toFloat()
            val delta: Float =
                (sample.time - previousSample.time).inMilliseconds().absoluteValue.toFloat()
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
                    duration = newestSample.time - oldestSample.time,
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
            duration = newestSample.time - oldestSample.time,
            offset = newestSample.point - oldestSample.point
        )
    }
}

private data class PointAtTime(val point: Offset, val time: Uptime)

/**
 * A two dimensional velocity estimate.
 *
 * VelocityEstimates are computed by [VelocityTracker.getVelocityEstimate]. An
 * estimate's [confidence] measures how well the velocity tracker's position
 * data fit a straight line, [duration] is the time that elapsed between the
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
    val duration: Duration,
    /**
     * The difference between the first and last position sample used
     * to compute [pixelsPerSecond].
     */
    val offset: Offset
) {
    companion object {
        val None = VelocityEstimate(Offset.Zero, 1f, Duration(0), Offset.Zero)
    }
}