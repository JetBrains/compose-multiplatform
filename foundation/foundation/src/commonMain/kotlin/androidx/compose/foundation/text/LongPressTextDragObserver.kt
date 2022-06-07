/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal interface TextDragObserver {
    /**
     * Called as soon as a down event is received. If the pointer eventually moves while remaining
     * down, a drag gesture may be started. After this method:
     * - [onUp] will always be called eventually, once the pointer is released.
     * - [onStart] _may_ be called, if there is a drag that exceeds touch slop.
     *
     * This method will not be called before [onStart] in the case when a down event happens that
     * may not result in a drag, e.g. on the down before a long-press that starts a selection.
     */
    fun onDown(point: Offset)

    /**
     * Called after [onDown] if an up event is received without dragging.
     */
    fun onUp()

    /**
     * Called once a drag gesture has started, which means touch slop has been exceeded.
     * [onDown] _may_ be called before this method if the down event could not have
     * started a different gesture.
     */
    fun onStart(startPoint: Offset)

    fun onDrag(delta: Offset)

    fun onStop()

    fun onCancel()
}

internal suspend fun PointerInputScope.detectDragGesturesAfterLongPressWithObserver(
    observer: TextDragObserver
) = detectDragGesturesAfterLongPress(
    onDragEnd = { observer.onStop() },
    onDrag = { _, offset ->
        observer.onDrag(offset)
    },
    onDragStart = {
        observer.onStart(it)
    },
    onDragCancel = { observer.onCancel() }
)

/**
 * Detects gesture events for a [TextDragObserver], including both initial down events and drag
 * events.
 */
internal suspend fun PointerInputScope.detectDownAndDragGesturesWithObserver(
    observer: TextDragObserver
) {
    coroutineScope {
        launch {
            detectPreDragGesturesWithObserver(observer)
        }
        launch {
            detectDragGesturesWithObserver(observer)
        }
    }
}

/**
 * Detects initial down events and calls [TextDragObserver.onDown] and
 * [TextDragObserver.onUp].
 */
private suspend fun PointerInputScope.detectPreDragGesturesWithObserver(
    observer: TextDragObserver
) {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown()
            observer.onDown(down.position)
            // Wait for that pointer to come up.
            do {
                val event = awaitPointerEvent()
            } while (event.changes.fastAny { it.id == down.id && it.pressed })
            observer.onUp()
        }
    }
}

/**
 * Detects drag gestures for a [TextDragObserver].
 */
private suspend fun PointerInputScope.detectDragGesturesWithObserver(
    observer: TextDragObserver
) {
    detectDragGestures(
        onDragEnd = { observer.onStop() },
        onDrag = { _, offset ->
            observer.onDrag(offset)
        },
        onDragStart = {
            observer.onStart(it)
        },
        onDragCancel = { observer.onCancel() }
    )
}