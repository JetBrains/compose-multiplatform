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

package androidx.compose.material3

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode

internal actual val KeyEvent.isDirectionUp: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_UP

internal actual val KeyEvent.isDirectionDown: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_DOWN

internal actual val KeyEvent.isDirectionRight: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_RIGHT

internal actual val KeyEvent.isDirectionLeft: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_LEFT

internal actual val KeyEvent.isHome: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_HOME

internal actual val KeyEvent.isMoveEnd: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_END

internal actual val KeyEvent.isPgUp: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_PAGE_UP

internal actual val KeyEvent.isPgDn: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_PAGE_DOWN

internal actual val KeyEvent.isEsc: Boolean
    get() = key.nativeKeyCode == java.awt.event.KeyEvent.VK_ESCAPE
