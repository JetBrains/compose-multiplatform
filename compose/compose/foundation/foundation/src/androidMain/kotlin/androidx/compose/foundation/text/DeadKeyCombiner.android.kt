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

import android.view.KeyCharacterMap
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint

internal actual class DeadKeyCombiner {

    private var deadKeyCode: Int? = null

    actual fun consume(event: KeyEvent): Int? {
        val codePoint = event.utf16CodePoint
        if (codePoint and KeyCharacterMap.COMBINING_ACCENT != 0) {
            deadKeyCode = codePoint and KeyCharacterMap.COMBINING_ACCENT_MASK
            return null
        }

        val localDeadKeyCode = deadKeyCode
        if (localDeadKeyCode != null) {
            deadKeyCode = null
            return KeyCharacterMap.getDeadChar(localDeadKeyCode, codePoint)
                // if the combo doesn't exist, fall back to the current key press
                .takeUnless { it == 0 } ?: codePoint
        }

        return codePoint
    }
}
