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

package androidx.compose.ui.input.pointer

internal actual class InternalPointerEvent constructor(
    val type: PointerEventType,
    actual val changes: Map<PointerId, PointerInputChange>,
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers,
    val nativeEvent: Any?
) {
    actual constructor(
        changes: Map<PointerId, PointerInputChange>,
        pointerInputEvent: PointerInputEvent
    ) : this(
        pointerInputEvent.eventType,
        changes,
        pointerInputEvent.buttons,
        pointerInputEvent.keyboardModifiers,
        pointerInputEvent.nativeEvent
    )

    actual var suppressMovementConsumption: Boolean = false

    // Assume that all changes are from mouse events for now
    actual fun issuesEnterExitEvent(pointerId: PointerId): Boolean = true
}