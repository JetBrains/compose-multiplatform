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

package androidx.compose.ui.platform

import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon

internal actual fun sendKeyEvent(
    platformInputService: PlatformInput,
    keyInputModifier: KeyInputModifier,
    keyEvent: KeyEvent
): Boolean {
    TODO("implement native sendKeyEvent")
/*
    when {
        keyEvent.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_TYPED ->
            platformInputService.charKeyPressed = true
        keyEvent.type == KeyEventType.KeyUp ->
            platformInputService.charKeyPressed = false
    }

    return keyInputModifier.processKeyInput(keyEvent)
*/
}

internal actual fun setPointerIcon(
    containerCursor: PlatformComponentWithCursor?,
    icon: PointerIcon?
) {
    println("TODO: implement setPointerIcon for Native")
}
