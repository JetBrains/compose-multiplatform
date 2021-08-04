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

import android.view.KeyEvent
import android.view.MotionEvent

internal actual typealias NativePointerButtons = Int
internal actual typealias NativePointerKeyboardModifiers = Int

/**
 * Describes a pointer input change event that has occurred at a particular point in time.
 */
actual data class PointerEvent internal constructor(
    /**
     * The changes.
     */
    actual val changes: List<PointerInputChange>,
    internal val motionEvent: MotionEvent?
) {
    internal actual constructor(
        changes: List<PointerInputChange>,
        internalPointerEvent: InternalPointerEvent?
    ) : this(changes, internalPointerEvent?.motionEvent)

    /**
     * @param changes The changes.
     */
    actual constructor(changes: List<PointerInputChange>) : this(changes, motionEvent = null)

    actual val buttons = PointerButtons(motionEvent?.buttonState ?: 0)

    actual val keyboardModifiers = PointerKeyboardModifiers(motionEvent?.metaState ?: 0)
}

actual val PointerButtons.isPrimaryPressed: Boolean
    get() = packedValue and (MotionEvent.BUTTON_PRIMARY or MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = packedValue and
        (MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_STYLUS_SECONDARY) != 0

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = packedValue and MotionEvent.BUTTON_TERTIARY != 0

actual val PointerButtons.isBackPressed: Boolean
    get() = packedValue and MotionEvent.BUTTON_BACK != 0

actual val PointerButtons.isForwardPressed: Boolean
    get() = packedValue and MotionEvent.BUTTON_FORWARD != 0

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    when (buttonIndex) {
        0 -> isPrimaryPressed
        1 -> isSecondaryPressed
        2, 3, 4 -> packedValue and (1 shl buttonIndex) != 0
        else -> packedValue and (1 shl (buttonIndex + 2)) != 0
    }

actual val PointerButtons.areAnyPressed: Boolean
    get() = packedValue != 0

actual fun PointerButtons.indexOfFirstPressed(): Int {
    if (packedValue == 0) {
        return -1
    }
    var index = 0
    var shifted = packedValue
    while (shifted and 1 == 0) {
        index++
        shifted = shifted ushr 1
    }
    return indexRemovingStylusPrimaryAndSecondary(index)
}

actual fun PointerButtons.indexOfLastPressed(): Int {
    var shifted = packedValue
    var index = -1
    while (shifted != 0) {
        index++
        shifted = shifted ushr 1
    }
    return indexRemovingStylusPrimaryAndSecondary(index)
}

private fun indexRemovingStylusPrimaryAndSecondary(buttonIndex: Int): Int {
    return when (buttonIndex) {
        -1, 0, 1, 2, 3, 4 -> buttonIndex
        5 -> 0 // stylus primary is just primary
        6 -> 1 // stylus secondary is just secondary
        else -> buttonIndex - 2
    }
}

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = (packedValue and KeyEvent.META_CTRL_ON) != 0

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = (packedValue and KeyEvent.META_META_ON) != 0

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = (packedValue and KeyEvent.META_ALT_ON) != 0

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = false

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = (packedValue and KeyEvent.META_SYM_ON) != 0

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = (packedValue and KeyEvent.META_SHIFT_ON) != 0

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = (packedValue and KeyEvent.META_FUNCTION_ON) != 0

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = (packedValue and KeyEvent.META_CAPS_LOCK_ON) != 0

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = (packedValue and KeyEvent.META_SCROLL_LOCK_ON) != 0

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = (packedValue and KeyEvent.META_NUM_LOCK_ON) != 0
