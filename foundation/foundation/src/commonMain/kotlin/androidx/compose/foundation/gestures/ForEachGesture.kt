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

import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

/**
 * A gesture was canceled and cannot continue, likely because another gesture has taken
 * over the pointer input stream.
 */
class GestureCancellationException(message: String? = null) : CancellationException(message)

/**
 * Repeatedly calls [block] to handle gestures. If there is a [CancellationException],
 * it will wait until all pointers are raised before another gesture is detected, or it
 * exits if [isActive] is `false`.
 */
suspend fun PointerInputScope.forEachGesture(block: suspend PointerInputScope.() -> Unit) {
    val currentContext = currentCoroutineContext()
    while (currentContext.isActive) {
        try {
            block()

            // Wait for all pointers to be up. Gestures start when a finger goes down.
            awaitAllPointersUp()
        } catch (e: CancellationException) {
            if (currentContext.isActive) {
                // The current gesture was canceled. Wait for all fingers to be "up" before looping
                // again.
                awaitAllPointersUp()
            } else {
                // forEachGesture was cancelled externally. Rethrow the cancellation exception to
                // propagate it upwards.
                throw e
            }
        }
    }
}

/**
 * Returns `true` if the current state of the pointer events has all pointers up and `false`
 * if any of the pointers are down.
 */
internal fun AwaitPointerEventScope.allPointersUp(): Boolean =
    !currentEvent.changes.fastAny { it.pressed }

/**
 * Waits for all pointers to be up before returning.
 */
internal suspend fun PointerInputScope.awaitAllPointersUp() {
    awaitPointerEventScope { awaitAllPointersUp() }
}

/**
 * Waits for all pointers to be up before returning.
 */
internal suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    if (!allPointersUp()) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.fastAny { it.pressed })
    }
}