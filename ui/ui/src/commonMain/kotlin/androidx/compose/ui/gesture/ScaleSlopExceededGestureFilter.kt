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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue

// TODO(b/143877464): Implement a "can scale in / can scale out" check so that scale slop is only
//  surpassed in the appropriate direction?
/**
 * This gesture detector detects when a user's pointer input is intended to include scaling.
 *
 * This gesture detector is very similar to [rawScaleGestureFilter] except that instead of
 * providing callbacks for scaling, it instead provides one callback for when a user is intending
 * to scale.  It does so using the same semantics as [rawScaleGestureFilter], and simply waits
 * until the user has scaled just enough to suggest the user is truly intended to scale.
 *
 * The gesture is considered to include scaling when the absolute cumulative average change in
 * distance of all pointers from the average pointer over time surpasses a particular value
 * (currently [ScaleSlop]).
 *
 * For example, if the [ScaleSlop] is 5 pixels and 2 pointers were 1 pixel away from each
 * other and now are 11.00001 pixels away from each other, the slop will have been surpassed and
 * [onScaleSlopExceeded] will be called (both pointers are slightly more than 5 pixels away from
 * the average of the pointers than they were).
 */
fun Modifier.scaleSlopExceededGestureFilter(
    onScaleSlopExceeded: () -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scaleSlopExceededGestureFilter"
        properties["onScaleSlopExceeded"] = onScaleSlopExceeded
    }
) {
    val scaleSlop = with(AmbientDensity.current) { ScaleSlop.toPx() }
    val filter = remember { ScaleSlopExceededGestureFilter(scaleSlop) }
    // TODO(b/129784010): Consider also allowing onStart, onScale, and onEnd to be set individually.
    filter.onScaleSlopExceeded = onScaleSlopExceeded
    PointerInputModifierImpl(filter)
}

/**
 * @param scaleSlop The absolute cumulative average change in distance of all pointers from the
 * average pointer over time that must be surpassed to indicate the user is trying to scale.
 *
 * @see scaleSlopExceededGestureFilter
 */
internal class ScaleSlopExceededGestureFilter(private val scaleSlop: Float) : PointerInputFilter
() {
    lateinit var onScaleSlopExceeded: () -> Unit

    var passedSlop = false
    var scaleDiffTotal = 0f

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {

        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main) {

            if (!passedSlop) {

                val currentlyDownChanges =
                    changes.filter { it.current.down && it.previous.down }

                if (currentlyDownChanges.isNotEmpty()) {
                    val dimensionInformation =
                        currentlyDownChanges.calculateAllDimensionInformation()
                    val scaleDifference = dimensionInformation.calculateScaleDifference()

                    scaleDiffTotal += scaleDifference

                    if (scaleDiffTotal.absoluteValue > scaleSlop) {
                        passedSlop = true
                        onScaleSlopExceeded.invoke()
                    }
                }
            }
        }

        if (passedSlop &&
            pass == PointerEventPass.Final &&
            changes.all { it.changedToUpIgnoreConsumed() }
        ) {
            passedSlop = false
            scaleDiffTotal = 0f
        }
    }

    override fun onCancel() {
        passedSlop = false
        scaleDiffTotal = 0f
    }
}
