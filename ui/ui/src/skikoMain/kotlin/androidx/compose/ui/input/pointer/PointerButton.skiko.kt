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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import kotlin.jvm.JvmInline

@JvmInline
@ExperimentalComposeUiApi
/**
 * Represents the index of a pointer button.
 * See [PointerEvent.button], where [PointerButton] is used.
 */
value class PointerButton(val index: Int) {
    companion object {
        val Primary = PointerButton(0)
        val Secondary = PointerButton(1)
        val Tertiary = PointerButton(2)
        val Back = PointerButton(3)
        val Forward = PointerButton(4)
    }
}

@ExperimentalComposeUiApi
val PointerButton?.isPrimary: Boolean
    get() { return this == PointerButton.Primary }

@ExperimentalComposeUiApi
val PointerButton?.isSecondary: Boolean
    get() { return this == PointerButton.Secondary }

@ExperimentalComposeUiApi
val PointerButton?.isTertiary: Boolean
    get() { return this == PointerButton.Tertiary }

@ExperimentalComposeUiApi
val PointerButton?.isBack: Boolean
    get() { return this == PointerButton.Back }

@ExperimentalComposeUiApi
val PointerButton?.isForward: Boolean
    get() { return this == PointerButton.Forward }
