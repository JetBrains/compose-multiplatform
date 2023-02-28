/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Detects pointer events that result from pointer movements and feed said events to the
 * [onMove] function. When multiple pointers are being used, only the first one is tracked.
 * If the first pointer is then removed, the second pointer will take its place as the first
 * pointer and be tracked.
 *
 * @param pointerEventPass which pass to capture the pointer event from, see [PointerEventPass]
 * @param onMove function that handles the position of move events
 */
internal suspend fun PointerInputScope.detectMoves(
    pointerEventPass: PointerEventPass = PointerEventPass.Initial,
    onMove: (Offset) -> Unit
) = coroutineScope {
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
        var previousPosition: Offset? = null
        while (currentContext.isActive) {
            val event = awaitPointerEvent(pointerEventPass)
            when (event.type) {
                PointerEventType.Move, PointerEventType.Enter, PointerEventType.Exit ->
                    event.changes.first().position
                        .takeUnless { it == previousPosition }
                        ?.let { position ->
                            previousPosition = position
                            onMove(position)
                        }
            }
        }
    }
}
