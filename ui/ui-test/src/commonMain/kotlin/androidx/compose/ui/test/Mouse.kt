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

package androidx.compose.ui.test

/**
 * Representation of a mouse scroll wheel axis. Only [Horizontal] and [Vertical] are supported.
 * All methods that accept a scroll axis use [Vertical] as the default, since most mice only have
 * a vertical scroll wheel.
 */
@ExperimentalTestApi
@kotlin.jvm.JvmInline
value class ScrollWheel private constructor(val value: Int) {
    @ExperimentalTestApi
    companion object {
        val Horizontal = ScrollWheel(0)
        val Vertical = ScrollWheel(1)
    }
}

/**
 * Representation of a mouse button with its associated [ID][buttonId] for the current platform.
 */
@ExperimentalTestApi
@kotlin.jvm.JvmInline
expect value class MouseButton(val buttonId: Int) {
    @ExperimentalTestApi
    companion object {
        /**
         * The primary mouse button. Typically the left mouse button.
         */
        val Primary: MouseButton

        /**
         * The secondary mouse button. Typically the right mouse button.
         */
        val Secondary: MouseButton

        /**
         * The tertiary mouse button. Typically the middle mouse button.
         */
        val Tertiary: MouseButton
    }
}
