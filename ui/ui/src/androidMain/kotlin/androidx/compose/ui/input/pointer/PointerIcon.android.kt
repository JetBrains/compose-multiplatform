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

package androidx.compose.ui.input.pointer

import android.view.PointerIcon.TYPE_CROSSHAIR
import android.view.PointerIcon.TYPE_HAND
import android.view.PointerIcon.TYPE_DEFAULT
import android.view.PointerIcon.TYPE_TEXT

internal class AndroidPointerIconType(val type: Int) : PointerIcon {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndroidPointerIconType

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type
    }

    override fun toString(): String {
        return "AndroidPointerIcon(type=$type)"
    }
}

internal class AndroidPointerIcon(val pointerIcon: android.view.PointerIcon) : PointerIcon {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndroidPointerIcon

        return pointerIcon == other.pointerIcon
    }

    override fun hashCode(): Int {
        return pointerIcon.hashCode()
    }

    override fun toString(): String {
        return "AndroidPointerIcon(pointerIcon=$pointerIcon)"
    }
}

/**
 * Creates [PointerIcon] from [android.view.PointerIcon]
 */
fun PointerIcon(pointerIcon: android.view.PointerIcon): PointerIcon =
    AndroidPointerIcon(pointerIcon)

/**
 * Creates [PointerIcon] from pointer icon type (see [android.view.PointerIcon.getSystemIcon]
 */
fun PointerIcon(pointerIconType: Int): PointerIcon =
    AndroidPointerIconType(pointerIconType)

internal actual val pointerIconDefault: PointerIcon = AndroidPointerIconType(TYPE_DEFAULT)
internal actual val pointerIconCrosshair: PointerIcon = AndroidPointerIconType(TYPE_CROSSHAIR)
internal actual val pointerIconText: PointerIcon = AndroidPointerIconType(TYPE_TEXT)
internal actual val pointerIconHand: PointerIcon = AndroidPointerIconType(TYPE_HAND)