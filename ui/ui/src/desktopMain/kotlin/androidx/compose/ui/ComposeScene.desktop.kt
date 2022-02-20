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

import androidx.compose.ui.platform.AccessibilityController
import androidx.compose.ui.platform.AccessibilityControllerImpl
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.SkiaBasedOwner
import java.awt.event.InputMethodEvent

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

internal actual fun makeAccessibilityController(
    skiaBasedOwner: SkiaBasedOwner,
    component: PlatformComponent
): AccessibilityController = AccessibilityControllerImpl(skiaBasedOwner, component)

internal actual fun currentMillis(): Long = System.currentTimeMillis()
