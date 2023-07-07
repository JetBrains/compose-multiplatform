/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.testutils

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.ArrayList
import kotlin.math.ceil
import kotlin.math.floor

/** One [MotionEvent] approximately every 10 milliseconds. We care about this frequency because a
 * standard touchscreen operates at 100 Hz and therefore produces about one touch event every
 * 10 milliseconds.  We want to produce a similar frequency to emulate real world input events.*/
const val MOTION_EVENT_INTERVAL_MILLIS: Int = 10

/**
 * Distance and time span necessary to produce a fling.
 *
 * Distance and time necessary to produce a fling for [MotionEvent]
 *
 * @property distance Distance between [MotionEvent]s in pixels for a fling.
 * @property time Time between [MotionEvent]s in milliseconds for a fling.
 */
data class FlingData(val distance: Float, val time: Int) {

    /**
     * @property velocity Velocity of fling in pixels per millisecond.
     */
    val velocity: Float = distance / time
}

data class MotionEventData(
    val eventTimeDelta: Int,
    val action: Int,
    val x: Float,
    val y: Float,
    val metaState: Int
)

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

fun MotionEventData.toMotionEvent(downTime: Long): MotionEvent = MotionEvent.obtain(
    downTime,
    this.eventTimeDelta + downTime,
    this.action,
    this.x,
    this.y,
    this.metaState
)

/**
 * Constructs a [FlingData] from a [Context] and [velocityPixelsPerSecond].
 *
 * [velocityPixelsPerSecond] must between [ViewConfiguration.getScaledMinimumFlingVelocity] * 1.1
 * and [ViewConfiguration.getScaledMaximumFlingVelocity] * .9, inclusive.  Losses of precision do
 * not allow the simulated fling to be super precise.
 */
@JvmOverloads
fun generateFlingData(context: Context, velocityPixelsPerSecond: Float? = null): FlingData {
    val configuration = ViewConfiguration.get(context)
    val touchSlop = configuration.scaledTouchSlop
    val minimumVelocity = configuration.scaledMinimumFlingVelocity
    val maximumVelocity = configuration.scaledMaximumFlingVelocity

    val targetPixelsPerMilli =
        if (velocityPixelsPerSecond != null) {
            if (velocityPixelsPerSecond < minimumVelocity * 1.1 - .001f ||
                velocityPixelsPerSecond > maximumVelocity * .9 + .001f
            ) {
                throw IllegalArgumentException(
                    "velocityPixelsPerSecond must be between " +
                        "ViewConfiguration.scaledMinimumFlingVelocity * 1.1 and " +
                        "ViewConfiguration.scaledMinimumFlingVelocity * .9, inclusive"
                )
            }
            velocityPixelsPerSecond / 1000f
        } else {
            ((maximumVelocity + minimumVelocity) / 2) / 1000f
        }

    val targetDistancePixels = touchSlop * 2
    val targetMillisPassed = floor(targetDistancePixels / targetPixelsPerMilli).toInt()

    if (targetMillisPassed < 1) {
        throw IllegalArgumentException("Flings must require some time")
    }

    return FlingData(targetDistancePixels.toFloat(), targetMillisPassed)
}

/**
 *  Returns [value] rounded up to the closest [interval] * N, where N is a Integer.
 */
private fun ceilToInterval(value: Int, interval: Int): Int =
    ceil(value.toFloat() / interval).toInt() * interval

/**
 * Generates a [List] of [MotionEventData] starting from ([originX], [originY]) that will cause a
 * fling in the finger direction [Direction].
 */
fun FlingData.generateFlingMotionEventData(
    originX: Float,
    originY: Float,
    fingerDirection: Direction
):
    List<MotionEventData> {

        // Ceiling the time and distance to match up with motion event intervals.
        val time: Int = ceilToInterval(this.time, MOTION_EVENT_INTERVAL_MILLIS)
        val distance: Float = velocity * time

        val dx: Float = when (fingerDirection) {
            Direction.LEFT -> -distance
            Direction.RIGHT -> distance
            else -> 0f
        }
        val dy: Float = when (fingerDirection) {
            Direction.UP -> -distance
            Direction.DOWN -> distance
            else -> 0f
        }
        val toX = originX + dx
        val toY = originY + dy

        val numberOfInnerEvents = (time / MOTION_EVENT_INTERVAL_MILLIS) - 1
        val dxIncrement = dx / (numberOfInnerEvents + 1)
        val dyIncrement = dy / (numberOfInnerEvents + 1)

        val motionEventData = ArrayList<MotionEventData>()
        motionEventData.add(MotionEventData(0, MotionEvent.ACTION_DOWN, originX, originY, 0))
        for (i in 1..(numberOfInnerEvents)) {
            val timeDelta = i * MOTION_EVENT_INTERVAL_MILLIS
            val x = originX + (i * dxIncrement)
            val y = originY + (i * dyIncrement)
            motionEventData.add(MotionEventData(timeDelta, MotionEvent.ACTION_MOVE, x, y, 0))
        }
        motionEventData.add(MotionEventData(time, MotionEvent.ACTION_MOVE, toX, toY, 0))
        motionEventData.add(MotionEventData(time, MotionEvent.ACTION_UP, toX, toY, 0))

        return motionEventData
    }

/**
 * Dispatches an array of [MotionEvent] to a [View].
 *
 * The MotionEvents will start at [downTime] and will be generated from the [motionEventData].
 * The MotionEvents will be dispatched synchronously, one after the other, with no gaps of time
 * in between each [MotionEvent].
 *
 */
fun View.dispatchTouchEvents(downTime: Long, motionEventData: List<MotionEventData>) {
    for (motionEventDataItem in motionEventData) {
        dispatchTouchEvent(motionEventDataItem.toMotionEvent(downTime))
    }
}

/**
 * Simulates a fling on a [View].
 *
 * Convenience method that calls other public api.  See documentation of those functions for more
 * detail.
 *
 * @see [generateFlingData]
 * @see [generateFlingMotionEventData]
 * @see [dispatchTouchEvents]
 */
@JvmOverloads
fun View.simulateFling(
    downTime: Long,
    originX: Float,
    originY: Float,
    direction: Direction,
    velocityPixelsPerSecond: Float? = null
) {
    dispatchTouchEvents(
        downTime,
        generateFlingData(context, velocityPixelsPerSecond)
            .generateFlingMotionEventData(originX, originY, direction)
    )
}
