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

import org.jetbrains.skiko.SkikoPointerEvent

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
     * Original raw native event from AWT
     */
    val mouseEvent: SkikoPointerEvent?
) {
    internal actual constructor(
        changes: List<PointerInputChange>,
        internalPointerEvent: InternalPointerEvent?
    ) : this(changes, internalPointerEvent?.mouseEvent)

    /**
     * @param changes The changes.
     */
    actual constructor(changes: List<PointerInputChange>) : this(changes, mouseEvent = null)

    actual var type: PointerEventType = PointerEventType.Unknown
        internal set

    actual val buttons: PointerButtons = PointerButtons(mouseEvent?.buttons?.value ?: 0)

    actual val keyboardModifiers: PointerKeyboardModifiers
        get() = TODO("implement native pointer event")

}


actual val PointerButtons.isPrimaryPressed: Boolean
    get() = TODO("implement native events")

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = TODO("implement native events")

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = TODO("implement native events")


actual val PointerButtons.isBackPressed: Boolean
    get() = TODO("implement native events")

actual val PointerButtons.isForwardPressed: Boolean
    get() = TODO("implement native events")

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    TODO("implement native events")

// TODO: all this file should go away when we move to Skiko events for skikoCommon.
actual val PointerButtons.areAnyPressed: Boolean
    get() = packedValue != 0

actual fun PointerButtons.indexOfFirstPressed(): Int = TODO("implement native events")

actual fun PointerButtons.indexOfLastPressed(): Int = TODO("implement native events")

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = TODO("implement native events")

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = TODO("implement native events")
