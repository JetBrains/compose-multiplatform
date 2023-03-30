/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import org.jetbrains.skiko.SkikoInputModifiers

internal actual fun NativeKeyEvent.toPointerKeyboardModifiers(): PointerKeyboardModifiers {
    return PointerKeyboardModifiers(
        isCtrlPressed = modifiers.has(SkikoInputModifiers.CONTROL),
        isShiftPressed = modifiers.has(SkikoInputModifiers.SHIFT),
        isAltPressed = modifiers.has(SkikoInputModifiers.ALT),
        isMetaPressed = modifiers.has(SkikoInputModifiers.META),
        // TODO: add other modifiers when they are available in SkikoInputModifiers
    )
}

// TODO: For non-JVM platforms, you can revive the kotlin-reflect implementation from
//  https://android-review.googlesource.com/c/platform/frameworks/support/+/2441379
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun InspectorInfo.tryPopulateReflectively(
    element: ModifierNodeElement<*>
) {
}