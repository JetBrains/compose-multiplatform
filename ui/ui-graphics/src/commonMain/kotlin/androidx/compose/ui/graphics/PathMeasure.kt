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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.internal.JvmDefaultWithCompatibility

/**
 * Create an empty [PathMeasure] object. To uses this to measure the length of a path, and/or to
 * find the position and tangent along it, call [PathMeasure.setPath]. Note that once a path is
 * associated with the measure object, it is undefined if the path is subsequently modified and
 * the measure object is used. If the path is modified, you must call [PathMeasure.setPath] with
 * the path.
 */
expect fun PathMeasure(): PathMeasure

@JvmDefaultWithCompatibility
interface PathMeasure {

    /**
     * The total length of the current contour, or 0 if no path is associated with this measure
     * object.
     */
    val length: Float

    /**
     * Given a start and stop distance, return in dst the intervening segment(s). If the segment
     * is zero-length, return false, else return true. startD and stopD are pinned to legal
     * values (0..getLength()). If startD >= stopD then return false (and leave dst untouched).
     * Begin the segment with a moveTo if startWithMoveTo is true.
     */
    fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean = true
    ): Boolean

    /**
     * Assign a new path, or null to have none.
     */
    fun setPath(path: Path?, forceClosed: Boolean)
}
