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

package androidx.compose.ui.window

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp

/**
 * Constructs an [WindowPosition.Absolute] from [x] and [y] [Dp] values.
 */
fun WindowPosition(x: Dp, y: Dp) = WindowPosition.Absolute(x, y)

/**
 * Constructs an [WindowPosition.Aligned] from [alignment] value.
 */
fun WindowPosition(alignment: Alignment) = WindowPosition.Aligned(alignment)

/**
 * Position of the window or dialog on the screen in [Dp].
 */
@Immutable
sealed class WindowPosition {
    /**
     * The horizontal position of the window in [Dp].
     */
    @Stable
    abstract val x: Dp

    /**
     * The vertical position of the window in [Dp].
     */
    @Stable
    abstract val y: Dp

    /**
     * `true` if the window position has specific coordinates on the screen
     *
     * `false` if coordinates are not yet determined (position is [PlatformDefault] or [Aligned])
     */
    @Stable
    abstract val isSpecified: Boolean

    /**
     * Initial position of the window that depends on the platform.
     * Usually every new window will be positioned in a cascade mode.
     *
     * This value should be used only before window will be visible.
     * After window will be visible, it cannot change its position to the PlatformDefault
     */
    object PlatformDefault : WindowPosition() {
        override val x: Dp get() = Dp.Unspecified
        override val y: Dp get() = Dp.Unspecified
        override val isSpecified: Boolean get() = false

        @Stable
        override fun toString() = "PlatformDefault"
    }

    /**
     * Window will be aligned when it will be shown on the screen. [alignment] defines how the
     * window will be aligned (in the center, or in some of the corners). Window
     * will be aligned in the area that is not occupied by the screen insets (taskbar, OS menubar)
     */
    @Immutable
    class Aligned(val alignment: Alignment) : WindowPosition() {
        override val x: Dp get() = Dp.Unspecified
        override val y: Dp get() = Dp.Unspecified
        override val isSpecified: Boolean get() = false

        /**
         * Returns a copy of this [Aligned] instance optionally overriding the
         * [alignment].
         */
        fun copy(alignment: Alignment = this.alignment) = Aligned(alignment)

        @Stable
        override fun toString() = "Aligned($alignment)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Aligned

            if (alignment != other.alignment) return false

            return true
        }

        override fun hashCode(): Int {
            return alignment.hashCode()
        }
    }

    /**
     * Absolute position of the window on the current window screen
     */
    @Immutable
    class Absolute(override val x: Dp, override val y: Dp) : WindowPosition() {
        override val isSpecified: Boolean get() = true

        @Stable
        operator fun component1(): Dp = x

        @Stable
        operator fun component2(): Dp = y

        /**
         * Returns a copy of this [Absolute] instance optionally overriding the
         * [x] or [y] parameter.
         */
        fun copy(x: Dp = this.x, y: Dp = this.y) = Absolute(x, y)

        @Stable
        override fun toString() = "Absolute($x, $y)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Absolute

            if (x != other.x) return false
            if (y != other.y) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            return result
        }
    }
}