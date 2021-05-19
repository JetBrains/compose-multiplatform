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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

internal interface TextDragObserver {
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

internal suspend fun PointerInputScope.detectDragGesturesWithObserver(
    observer: TextDragObserver
) = detectDragGestures(
    onDragEnd = { observer.onStop() },
    onDrag = { _, offset ->
        observer.onDrag(offset)
    },
    onDragStart = {
        observer.onStart(it)
    },
    onDragCancel = { observer.onCancel() }
)