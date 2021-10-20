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

package androidx.compose.foundation

import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun isComposeRootInScrollableContainer(): () -> Boolean {
    val view = LocalView.current
    return {
        view.isInScrollableViewGroup()
    }
}

// Copied from View#isInScrollingContainer() which is @hide
private fun View.isInScrollableViewGroup(): Boolean {
    var p = parent
    while (p != null && p is ViewGroup) {
        if (p.shouldDelayChildPressedState()) {
            return true
        }
        p = p.parent
    }
    return false
}

internal actual val TapIndicationDelay: Long = ViewConfiguration.getTapTimeout().toLong()

/**
 * Whether the specified [KeyEvent] represents a user intent to perform a click.
 * (eg. When you press Enter on a focused button, it should perform a click).
 */
internal actual val KeyEvent.isClick: Boolean
    get() = type == KeyUp && when (key.nativeKeyCode) {
            KEYCODE_DPAD_CENTER,
            KEYCODE_ENTER,
            KEYCODE_NUMPAD_ENTER -> true
            else -> false
    }