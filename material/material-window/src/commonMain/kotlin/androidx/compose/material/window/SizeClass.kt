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

package androidx.compose.material.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Window size classes are a set of opinionated viewport breakpoints to design, develop, and test resizable application layouts against.
 * For more details check <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes" class="external" target="_blank">Support different screen sizes</a> documentation.
 *
 * @param width width-based size class of the window
 * @param height height-based size class of the window
 */
@Immutable
class SizeClass private constructor(
    val width: WidthSizeClass,
    val height: HeightSizeClass
) {
    companion object {
        /**
         * Calculates [SizeClass] for a given [size]. Should be used for testing purposes only
         *
         * @param size of the window
         * @return size class corresponding to the given width and height
         */
        @ExperimentalMaterialWindowApi
        @TestOnly
        fun calculateFromSize(size: DpSize): SizeClass {
            val widthSizeClass = WidthSizeClass.fromWidth(size.width)
            val heightSizeClass = HeightSizeClass.fromHeight(size.height)
            return SizeClass(widthSizeClass, heightSizeClass)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SizeClass

        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        return result
    }

    override fun toString() = "SizeClass($width, $height)"
}

/**
 * Width-based size class of the window.
 *
 * A size class represents a breakpoint that can be used to build responsive layouts. Each size
 * class breakpoint represents a majority case for typical device scenarios so your layouts will
 * work well on most devices and configurations.
 *
 * For more details check <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
@Immutable
inline class WidthSizeClass private constructor(private val value: Int) {
    companion object {
        /** Represents the majority of phones in portrait. */
        val Compact = WidthSizeClass(0)

        /**
         * Represents the majority of tablets in portrait and large unfolded inner displays in portrait.
         */
        val Medium = WidthSizeClass(1)

        /**
         * Represents the majority of tablets in landscape and large unfolded inner displays in
         * landscape.
         */
        val Expanded = WidthSizeClass(2)

        /** Calculates [WidthSizeClass] size class for given [width] */
        internal fun fromWidth(width: Dp): WidthSizeClass {
            require(width >= 0.dp) { "Width must not be negative" }
            return when {
                width < 600.dp -> Compact
                width < 840.dp -> Medium
                else -> Expanded
            }
        }
    }
}

/**
 * Height-based size class of the window.
 *
 * A size class represents a breakpoint that can be used to build responsive layouts. Each size
 * class breakpoint represents a majority case for typical device scenarios so your layouts will
 * work well on most devices and configurations.
 *
 * For more details check <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
@Immutable
inline class HeightSizeClass private constructor(private val value: Int) {
    companion object {
        /** Represents the majority of phones in landscape */
        val Compact = HeightSizeClass(0)

        /** Represents the majority of tablets in landscape and majority of phones in portrait */
        val Medium = HeightSizeClass(1)

        /** Represents the majority of tablets in portrait */
        val Expanded = HeightSizeClass(2)

        /** Calculates [HeightSizeClass] size class for given [height] */
        internal fun fromHeight(height: Dp): HeightSizeClass {
            require(height >= 0.dp) { "Height must not be negative" }
            return when {
                height < 480.dp -> Compact
                height < 900.dp -> Medium
                else -> Expanded
            }
        }
    }
}