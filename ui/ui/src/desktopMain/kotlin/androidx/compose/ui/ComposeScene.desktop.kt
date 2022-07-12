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
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.AccessibilityController
import androidx.compose.ui.platform.AccessibilityControllerImpl
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.SkiaBasedOwner
import java.awt.event.InputMethodEvent
import java.awt.event.MouseEvent

internal actual fun ComposeScene.onPlatformInputMethodEvent(event: Any) {
    require(event is InputMethodEvent)
    if (!event.isConsumed) {
        when (event.id) {
            InputMethodEvent.INPUT_METHOD_TEXT_CHANGED -> {
                platformInputService.replaceInputMethodText(event)
                event.consume()
            }
            InputMethodEvent.CARET_POSITION_CHANGED -> {
                platformInputService.inputMethodCaretPositionChanged(event)
                event.consume()
            }
        }
    }
}

internal actual fun pointerInputEvent(
    eventType: PointerEventType,
    position: Offset,
    timeMillis: Long,
    nativeEvent: Any?,
    type: PointerType,
    isMousePressed: Boolean,
    pointerId: Long,
    scrollDelta: Offset,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers,
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
                pressure = 1.0f,
                type,
                scrollDelta = scrollDelta
            )
        ),
        buttons,
        keyboardModifiers,
        nativeEvent as MouseEvent?
    )
}

@OptIn(ExperimentalComposeUiApi::class)
internal actual val DefaultPointerButtons: PointerButtons = PointerButtons()

@OptIn(ExperimentalComposeUiApi::class)
internal actual val DefaultPointerKeyboardModifiers: PointerKeyboardModifiers =
    PointerKeyboardModifiers()

@OptIn(ExperimentalComposeUiApi::class)
internal actual val PrimaryPressedPointerButtons: PointerButtons =
    PointerButtons(isPrimaryPressed = true)
internal actual fun makeAccessibilityController(
    skiaBasedOwner: SkiaBasedOwner,
    component: PlatformComponent
): AccessibilityController = AccessibilityControllerImpl(skiaBasedOwner, component)

internal actual fun currentMillis(): Long = System.currentTimeMillis()
