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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * A gesture detector for rotation, panning, and zoom. Once touch slop has been reached, the
 * user can use rotation, panning and zoom gestures. [onGesture] will be called when any of the
 * rotation, zoom or pan occurs, passing the rotation angle in degrees, zoom in scale factor and
 * pan as an offset in pixels. Each of these changes is a difference between the previous call
 * and the current gesture. This will consume all position changes after touch slop has
 * been reached. [onGesture] will also provide centroid of all the pointers that are down.
 *
 * If [panZoomLock] is `true`, rotation is allowed only if touch slop is detected for rotation
 * before pan or zoom motions. If not, pan and zoom gestures will be detected, but rotation
 * gestures will not be. If [panZoomLock] is `false`, once touch slop is reached, all three
 * gestures are detected.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.DetectTransformGestures
 */
suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false

            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop ||
                            rotationMotion > touchSlop ||
                            panMotion > touchSlop
                        ) {
                            pastTouchSlop = true
                            lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                        }
                    }

                    if (pastTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                        if (effectiveRotation != 0f ||
                            zoomChange != 1f ||
                            panChange != Offset.Zero
                        ) {
                            onGesture(centroid, panChange, zoomChange, effectiveRotation)
                        }
                        event.changes.fastForEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
        }
    }
}

/**
 * Returns the rotation, in degrees, of the pointers between the
 * [PointerInputChange.previousPosition] and [PointerInputChange.position] states. Only
 * the pointers that are down in both previous and current states are considered.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.CalculateRotation
 */
fun PointerEvent.calculateRotation(): Float {
    val pointerCount = changes.fastSumBy { if (it.previousPressed && it.pressed) 1 else 0 }
    if (pointerCount < 2) {
        return 0f
    }
    val currentCentroid = calculateCentroid(useCurrent = true)
    val previousCentroid = calculateCentroid(useCurrent = false)
    var rotation = 0f
    var rotationWeight = 0f

    // We want to weigh each pointer differently so that motions farther from the
    // centroid have more weight than pointers close to the centroid. Essentially,
    // a small distance change near the centroid could equate to a large angle
    // change and we don't want it to affect the rotation as much as pointers farther
    // from the centroid, which should be more stable.

    changes.fastForEach { change ->
        if (change.pressed && change.previousPressed) {
            val currentPosition = change.position
            val previousPosition = change.previousPosition
            val previousOffset = previousPosition - previousCentroid
            val currentOffset = currentPosition - currentCentroid

            val previousAngle = previousOffset.angle()
            val currentAngle = currentOffset.angle()
            val angleDiff = currentAngle - previousAngle
            val weight = (currentOffset + previousOffset).getDistance() / 2f

            // We weigh the rotation with the distance to the centroid. This gives
            // more weight to angle changes from pointers farther from the centroid than
            // those that are closer.
            rotation += when {
                angleDiff > 180f -> angleDiff - 360f
                angleDiff < -180f -> angleDiff + 360f
                else -> angleDiff
            } * weight

            // weight its contribution by the distance to the centroid
            rotationWeight += weight
        }
    }
    return if (rotationWeight == 0f) 0f else rotation / rotationWeight
}

/**
 * Returns the angle of the [Offset] between -180 and 180, or 0 if [Offset.Zero].
 */
private fun Offset.angle(): Float =
    if (x == 0f && y == 0f) 0f else -atan2(x, y) * 180f / PI.toFloat()

/**
 * Uses the change of the centroid size between the [PointerInputChange.previousPosition] and
 * [PointerInputChange.position] to determine how much zoom was intended.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.CalculateZoom
 */
fun PointerEvent.calculateZoom(): Float {
    val currentCentroidSize = calculateCentroidSize(useCurrent = true)
    val previousCentroidSize = calculateCentroidSize(useCurrent = false)
    if (currentCentroidSize == 0f || previousCentroidSize == 0f) {
        return 1f
    }
    return currentCentroidSize / previousCentroidSize
}

/**
 * Returns the change in the centroid location between the previous and the current pointers that
 * are down. Pointers that are newly down or raised are not considered in the centroid
 * movement.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.CalculatePan
 */
fun PointerEvent.calculatePan(): Offset {
    val currentCentroid = calculateCentroid(useCurrent = true)
    if (currentCentroid == Offset.Unspecified) {
        return Offset.Zero
    }
    val previousCentroid = calculateCentroid(useCurrent = false)
    return currentCentroid - previousCentroid
}

/**
 * Returns the average distance from the centroid for all pointers that are currently
 * and were previously down. If no pointers are down, `0` is returned.
 * If [useCurrent] is `true`, the size of the [PointerInputChange.position] is returned and
 * if `false`, the size of [PointerInputChange.previousPosition] is returned. Only pointers that
 * are down in both the previous and current state are used to calculate the centroid size.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.CalculateCentroidSize
 */
fun PointerEvent.calculateCentroidSize(useCurrent: Boolean = true): Float {
    val centroid = calculateCentroid(useCurrent)
    if (centroid == Offset.Unspecified) {
        return 0f
    }

    var distanceToCentroid = 0f
    var distanceWeight = 0
    changes.fastForEach { change ->
        if (change.pressed && change.previousPressed) {
            val position = if (useCurrent) change.position else change.previousPosition
            distanceToCentroid += (position - centroid).getDistance()
            distanceWeight++
        }
    }
    return distanceToCentroid / distanceWeight.toFloat()
}

/**
 * Returns the centroid of all pointers that are down and were previously down. If no pointers
 * are down, [Offset.Unspecified] is returned. If [useCurrent] is `true`, the centroid of the
 * [PointerInputChange.position] is returned and if `false`, the centroid of the
 * [PointerInputChange.previousPosition] is returned. Only pointers that are down in both the
 * previous and current state are used to calculate the centroid.
 *
 * Example Usage:
 * @sample androidx.compose.foundation.samples.CalculateCentroidSize
 */
fun PointerEvent.calculateCentroid(
    useCurrent: Boolean = true
): Offset {
    var centroid = Offset.Zero
    var centroidWeight = 0

    changes.fastForEach { change ->
        if (change.pressed && change.previousPressed) {
            val position = if (useCurrent) change.position else change.previousPosition
            centroid += position
            centroidWeight++
        }
    }
    return if (centroidWeight == 0) {
        Offset.Unspecified
    } else {
        centroid / centroidWeight.toFloat()
    }
}
