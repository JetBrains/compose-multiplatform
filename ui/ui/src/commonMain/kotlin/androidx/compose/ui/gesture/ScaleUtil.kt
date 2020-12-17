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

package androidx.compose.ui.gesture

import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Gets the difference in direction and magnitude of the distance between the given pointer
 * (represented by [i]) along a given dimension (represented by [previous] and [current]). For
 * example, if a given pointer's x value was previously 5 pixels greater than the average x values
 * and is currently 7 pixels greater than the average X value, this will return a value of 2.
 */
internal fun getVectorToAverageChange(
    previous: DimensionData,
    current: DimensionData,
    i: Int
): Float {
    val currentVectorToAverage = current.vectorsToAverage[i]
    val previousVectorToAverage = previous.vectorsToAverage[i]
    val absDistanceChanged = abs(abs(currentVectorToAverage) - abs(previousVectorToAverage))
    return if (currentVectorToAverage - previousVectorToAverage < 0) {
        -absDistanceChanged
    } else {
        absDistanceChanged
    }
}

/**
 * Calculates the [DimensionData] for a set of values that represent pointer positions on one
 * dimension.
 */
internal fun List<Float>.calculateDimensionInformation(): DimensionData {
    val average = average().toFloat()
    val vectorsToAverage = map {
        it - average
    }
    return DimensionData(average, vectorsToAverage)
}

/**
 * Calculates [AllDimensionData] for the given list of [PointerInputChange]s.
 */
internal fun List<PointerInputChange>.calculateAllDimensionInformation() =
    AllDimensionData(
        map {
            it.previousPosition.x
        }.calculateDimensionInformation(),
        map {
            it.previousPosition.y
        }.calculateDimensionInformation(),
        map {
            it.previousPosition.x + it.positionChange().x
        }.calculateDimensionInformation(),
        map {
            it.previousPosition.y + it.positionChange().y
        }.calculateDimensionInformation()
    )

/**
 * Calculates the scale factor from the [AllDimensionData].
 *
 * A scale factor of .5 means that the average distance to the center of all pointers has been
 * cut in half, while a scale factor of 2 means that average has doubled.  A scale factor of 1 means
 * no scaling has occurred.
 */
internal fun AllDimensionData.calculateScaleFactor() =
    averageDistanceToCenter(currentX, currentY) / averageDistanceToCenter(previousX, previousY)

/**
 * Calculates the average distance change of all pointers from the average pointer using
 * [AllDimensionData].
 *
 * If 2 pointers were 10 pixel away from each other, and then move such that they were 20
 * pixels away from each other, this function would return 10.
 *
 * If 2 pointers were 10 pixels away from each other, and then moved such that they were 5 pixels
 * away from each other, this function would return 5
 */
internal fun AllDimensionData.calculateScaleDifference(): Float =
    (averageDistanceToCenter(currentX, currentY) - averageDistanceToCenter(previousX, previousY))

/**
 * Data about a particular dimension (x or y).
 *
 * @param average The average value for all of the points along the given dimension.
 * @param vectorsToAverage The list of vectors from each point on the given dimension to the
 * average.  Negative means to the left or up, and positive means to the right or down.
 */
internal data class DimensionData(
    val average: Float,
    val vectorsToAverage: List<Float>
)

/**
 * The collection of all [DimensionData] for the previous and current pointer locations.
 */
internal data class AllDimensionData(
    val previousX: DimensionData,
    val previousY: DimensionData,
    val currentX: DimensionData,
    val currentY: DimensionData
)

/**
 * Calculates the average distance to the center of all pointers represented by [x] and [y].
 */
private fun averageDistanceToCenter(x: DimensionData, y: DimensionData): Float {
    var totalDistanceToCenter = 0f
    val count = x.vectorsToAverage.size
    for (i in 0 until count) {
        totalDistanceToCenter += hypot(x.vectorsToAverage[i], y.vectorsToAverage[i])
    }
    return totalDistanceToCenter / count
}