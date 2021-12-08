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
package androidx.compose.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerType
import org.jetbrains.skiko.SkikoPointerEvent

internal actual fun ComposeScene.onPlatformInputMethodEvent(event: Any) {
    TODO("implement native onPlatformInputMethodEvent")
}

internal actual fun pointerInputEvent(
    eventType: PointerEventType,
    position: Offset,
    timeMillis: Long,
    nativeEvent: Any?,
    type: PointerType,
    isMousePressed: Boolean,
    pointerId: Long,
    scrollDelta: Offset
): PointerInputEvent {
    return PointerInputEvent(
        eventType,
        timeMillis,
        listOf(
            PointerInputEventData(
                PointerId(pointerId),
                timeMillis,
                position,
                position,
                isMousePressed,
                type,
                scrollDelta = scrollDelta
            )
        ),
        nativeEvent as? SkikoPointerEvent
    )
}

internal actual fun getTimeMilliseconds(): Long = kotlin.system.getTimeNanos() / 1_000_000L
