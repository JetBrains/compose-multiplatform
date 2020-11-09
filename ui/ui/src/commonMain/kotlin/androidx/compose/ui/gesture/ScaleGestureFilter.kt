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

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Observes various events sent by [scaleGestureFilter].  Implement and pass into
 * [scaleGestureFilter] so that [scaleGestureFilter] may call the functions when events occur.
 */
interface ScaleObserver {
    /**
     * Override to be notified when scaling has started.
     *
     * This will be called when scaling occurs and a single pointer moves far enough to suggest
     * the user intends to perform scaling (the required drag distance is  defined by
     * [TouchSlop]). Always called just before [onScale] and isn't called again until
     * after [onStop].
     *
     * @see scaleGestureFilter
     * @see onScale
     * @see onStop
     */
    fun onStart() {}

    /**
     * Override to be notified when scaling has occurred.
     *
     * Always called just after [onStart] (and for every subsequent scale).
     *
     * @param scaleFactor The ratio of newSize / oldSize that the scaling gesture has expressed
     * between pointers last position and current position (this value is not cumulative over the
     * lifetime of of the gesture). For example, if 2 fingers are 10 pixel apart, and then move
     * such that they are 20 pixels apart, the scaleFactor will be 2.  If 2 fingers that are 20
     * pixels apart move such that they are 10 pixels apart, the scaleFactor will be .5.
     *
     * @see scaleGestureFilter
     * @see onStart
     * @see onStop
     *
     */
    fun onScale(scaleFactor: Float)

    /**
     * Override to be notified when scaling has stopped.
     *
     * This is called once less than 2 pointers remain.
     *
     * Only called after [onStart] and one or more calls to [onScale]
     *
     * @see scaleGestureFilter
     * @see onStart
     * @see onScale
     */
    fun onStop() {}

    /**
     * Override to be notified when the scale has been cancelled.
     *
     * This is called if [onStart] has ben called and then a cancellation event has occurs
     * (for example, due to the gesture detector being removed from the tree) before [onStop] is
     * called.
     */
    fun onCancel() {}
}

/**
 * This gesture detector detects scaling.
 *
 * Scaling is when the average distance between a set of pointers changes over time.  It is
 * also known as pinch, or pinch to zoom.
 *
 * Scaling begins when the average distance between a set of pointers changes and at least 1 pointer
 * moves beyond the touch slop distance (currently defined by [TouchSlop]).  When scaling begins,
 * [ScaleObserver.onStart] is called followed immediately by a call to [ScaleObserver.onScale].
 * [ScaleObserver.onScale] is then continuously called whenever the movement of pointers denotes
 * scaling. The gesture stops with either a call to [RawScaleObserver.onStop] or
 * [RawScaleObserver.onCancel], either of which will only be called if [RawScaleObserver.onStart]
 * was previously called. [RawScaleObserver.onStop] is called when no pointers remain.
 * [RawScaleObserver.onCancel] is called due to a system cancellation event.
 *
 * This gesture detector is similar to [rawScaleGestureFilter] except that it is made for more
 * standard use cases where touch slop should likely be respected and no "nested scaling" is
 * needed.
 *
 * @param scaleObserver The callback interface to report all events related to scaling.
 */
fun Modifier.scaleGestureFilter(
    scaleObserver: ScaleObserver
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scaleGestureFilter"
        properties["scaleObserver"] = scaleObserver
    }
) {
    val glue = remember { TouchSlopScaleGestureDetectorGlue() }
    glue.scaleObserver = scaleObserver

    rawScaleGestureFilter(glue.rawScaleObserver, glue::scaleEnabled)
        .scaleSlopExceededGestureFilter(glue::enableScale)
}

/**
 * Glues together the logic of RawScaleGestureDetector and TouchSlopExceededGestureDetector.
 */
private class TouchSlopScaleGestureDetectorGlue {

    lateinit var scaleObserver: ScaleObserver
    var scaleEnabled = false

    fun enableScale() {
        scaleEnabled = true
    }

    val rawScaleObserver: RawScaleObserver =
        object : RawScaleObserver {
            override fun onStart() {
                scaleObserver.onStart()
            }

            override fun onScale(scaleFactor: Float): Float {
                scaleObserver.onScale(scaleFactor)
                return scaleFactor
            }

            override fun onStop() {
                scaleEnabled = false
                scaleObserver.onStop()
            }

            override fun onCancel() {
                scaleEnabled = false
                scaleObserver.onCancel()
            }
        }
}