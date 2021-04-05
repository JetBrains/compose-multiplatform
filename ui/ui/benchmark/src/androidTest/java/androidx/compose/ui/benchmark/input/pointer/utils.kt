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

package androidx.compose.ui.benchmark.input.pointer

import android.view.MotionEvent
import android.view.View

/**
 * Creates a simple [MotionEvent].
 *
 * @param dispatchTarget The [View] that the [MotionEvent] is going to be dispatched to. This
 * guarantees that the MotionEvent is created correctly for both Compose (which relies on raw
 * coordinates being correct) and Android (which requires that local coordinates are correct).
 */
internal fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    dispatchTarget: View
): MotionEvent {

    val locationOnScreen = IntArray(2) { 0 }
    dispatchTarget.getLocationOnScreen(locationOnScreen)

    pointerCoords.forEach {
        it.x += locationOnScreen[0]
        it.y += locationOnScreen[1]
    }

    val motionEvent = MotionEvent.obtain(
        0,
        eventTime.toLong(),
        action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
        numPointers,
        pointerProperties,
        pointerCoords,
        0,
        0,
        0f,
        0f,
        0,
        0,
        0,
        0
    ).apply {
        offsetLocation(-locationOnScreen[0].toFloat(), -locationOnScreen[1].toFloat())
    }

    pointerCoords.forEach {
        it.x -= locationOnScreen[0]
        it.y -= locationOnScreen[1]
    }

    return motionEvent
}

@Suppress("RemoveRedundantQualifierName")
internal fun PointerProperties(id: Int) =
    MotionEvent.PointerProperties().apply { this.id = id }

@Suppress("RemoveRedundantQualifierName")
internal fun PointerCoords(x: Float, y: Float) =
    MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
    }