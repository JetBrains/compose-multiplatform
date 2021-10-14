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

import androidx.compose.ui.platform.DesktopPlatform
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.Toolkit

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

    /**
     * Original raw native event from AWT.
     *
     * Note, that its type can be different from [type], which is sent by Compose.
     * For example, Compose can send synthetic Move event on relayout,
     * but [mouseEvent] will tell that it is Up event
     */
    val mouseEvent: MouseEvent?
) {
    internal actual constructor(
        changes: List<PointerInputChange>,
        internalPointerEvent: InternalPointerEvent?
    ) : this(changes, internalPointerEvent?.mouseEvent) {
        this.type = internalPointerEvent?.type ?: PointerEventType.Unknown
    }

    actual val buttons = PointerButtons(mouseEvent?.modifiersEx ?: 0)

    actual val keyboardModifiers = createPointerKeyboardModifiers(mouseEvent?.modifiersEx ?: 0)

    /**
     * @param changes The changes.
     */
    actual constructor(changes: List<PointerInputChange>) : this(changes, mouseEvent = null)

    actual var type: PointerEventType = PointerEventType.Unknown
        internal set

    private fun createPointerKeyboardModifiers(modifiersEx: Int): PointerKeyboardModifiers {
        val toolkit = Toolkit.getDefaultToolkit()
        val capsLockBits = toolkit.getMaskForLockingKeyState(KeyEvent.VK_CAPS_LOCK, CapsLockMask)
        val numLockBits = toolkit.getMaskForLockingKeyState(KeyEvent.VK_NUM_LOCK, NumLockMask)
        val scrollLockBits =
            toolkit.getMaskForLockingKeyState(KeyEvent.VK_SCROLL_LOCK, ScrollLockMask)

        val packed = (modifiersEx and ClearMask) or capsLockBits or numLockBits or scrollLockBits
        return PointerKeyboardModifiers(packed)
    }
}

private fun Toolkit.getMaskForLockingKeyState(event: Int, mask: Int): Int {
    return try {
        if (getLockingKeyState(event)) {
            mask
        } else {
            0
        }
    } catch (_: Exception) {
        0
    }
}

/**
 * Coopt the BUTTON1_DOWN_MASK for caps lock state in PointerKeyboardModifiers
 */
private const val CapsLockMask = InputEvent.BUTTON1_DOWN_MASK
/**
 * Coopt the BUTTON2_DOWN_MASK for scroll lock state in PointerKeyboardModifiers
 */
private const val ScrollLockMask = InputEvent.BUTTON2_DOWN_MASK
/**
 * Coopt the BUTTON3_DOWN_MASK for num lock state in PointerKeyboardModifiers
 */
private const val NumLockMask = InputEvent.BUTTON3_DOWN_MASK

/**
 * Clear mask for all coopted values. We don't want button state to interfere with keyboard
 * state.
 */
private const val ClearMask = (
    InputEvent.BUTTON1_DOWN_MASK or
        InputEvent.BUTTON2_DOWN_MASK or
        InputEvent.BUTTON3_DOWN_MASK
    ).inv()

private val PointerButtons.isMacOsCtrlClick
    get() = (
        DesktopPlatform.Current == DesktopPlatform.MacOS &&
            ((packedValue and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
            ((packedValue and InputEvent.CTRL_DOWN_MASK) != 0)
        )

actual val PointerButtons.isPrimaryPressed
    get() = (packedValue and InputEvent.BUTTON1_DOWN_MASK) != 0 && !isMacOsCtrlClick

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = ((packedValue and InputEvent.BUTTON3_DOWN_MASK) != 0) || isMacOsCtrlClick

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = (packedValue and InputEvent.BUTTON2_DOWN_MASK) != 0

/**
 * Bit mask for back button.
 */
private const val BackMask = 1 shl 14

/**
 * Bit mask for forward button.
 */
private const val ForwardMask = 1 shl 15

actual val PointerButtons.isBackPressed: Boolean
    get() = packedValue and BackMask != 0

actual val PointerButtons.isForwardPressed: Boolean
    get() = packedValue and ForwardMask != 0

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    when (buttonIndex) {
        0 -> isPrimaryPressed
        1 -> isSecondaryPressed
        2 -> isTertiaryPressed
        3 -> isBackPressed
        4 -> isForwardPressed
        else -> false
    }

private const val AnyButtonMask =
    InputEvent.BUTTON1_DOWN_MASK or InputEvent.BUTTON2_DOWN_MASK or InputEvent.BUTTON3_DOWN_MASK

actual val PointerButtons.areAnyPressed: Boolean
    get() = (packedValue and AnyButtonMask) != 0

actual fun PointerButtons.indexOfFirstPressed(): Int = when {
    isPrimaryPressed -> 0
    isSecondaryPressed -> 1
    isTertiaryPressed -> 2
    isBackPressed -> 3
    isForwardPressed -> 4
    else -> -1
}

actual fun PointerButtons.indexOfLastPressed(): Int = when {
    isForwardPressed -> 4
    isBackPressed -> 3
    isTertiaryPressed -> 2
    isSecondaryPressed -> 1
    isPrimaryPressed -> 0
    else -> -1
}

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = (packedValue and InputEvent.CTRL_DOWN_MASK) != 0

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = (packedValue and InputEvent.META_DOWN_MASK) != 0

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = (packedValue and InputEvent.ALT_DOWN_MASK) != 0

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = (packedValue and InputEvent.ALT_GRAPH_DOWN_MASK) != 0

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = false

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = (packedValue and InputEvent.SHIFT_DOWN_MASK) != 0

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = false

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = (packedValue and CapsLockMask) != 0

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = (packedValue and ScrollLockMask) != 0

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = (packedValue and NumLockMask) != 0
