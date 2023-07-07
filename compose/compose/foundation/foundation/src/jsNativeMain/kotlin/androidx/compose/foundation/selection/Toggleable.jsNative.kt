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

package androidx.compose.foundation.selection

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import org.jetbrains.skiko.SkikoKey

private val SPACE_KEY_CODE = SkikoKey.KEY_SPACE.platformKeyCode.toLong()
/**
 * Whether the specified [KeyEvent] represents a user intent to perform a toggle.
 * (eg. When you press Space on a focused checkbox, it should perform a toggle).
 */
internal actual val KeyEvent.isToggle: Boolean
    get() = type == KeyEventType.KeyUp && when (key.keyCode) {
        SPACE_KEY_CODE -> true
        else -> false
    }
