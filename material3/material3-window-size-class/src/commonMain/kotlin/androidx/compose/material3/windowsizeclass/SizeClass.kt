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

package androidx.compose.material3.windowsizeclass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Window size classes are a set of opinionated viewport breakpoints to design, develop, and test
 * responsive application layouts against.
 * For more details check <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes" class="external" target="_blank">Support different screen sizes</a> documentation.
 *
 * SizeClass contains a [WidthSizeClass] and [HeightSizeClass], representing the size classes for
 * this window's width and height respectively.
 *
 * See [calculateSizeClass] to calculate the size class for the Activity's current window
 *
 * @param width width-based window size class
 * @param height height-based window size class
 */
@Immutable
class SizeClass private constructor(
    val width: WidthSizeClass,
    val height: HeightSizeClass
) {
    companion object {
        /**
         * Calculates [SizeClass] for a given [size]. Should be used for testing purposes only - to
         * calculate a [SizeClass] for the Activity's current window see [calculateSizeClass].
         *
         * @param size of the window
         * @return size class corresponding to the given width and height
         */
        @ExperimentalMaterial3WindowSizeClassApi
        @TestOnly
        fun calculateFromSize(size: DpSize): SizeClass {
            val widthSizeClass = WidthSizeClass.fromWidth(size.width)
            val heightSizeClass = HeightSizeClass.fromHeight(size.height)
            return SizeClass(widthSizeClass, heightSizeClass)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

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
 * Width-based window size class.
 *
 * A size class represents a breakpoint that can be used to build responsive layouts. Each size
 * class breakpoint represents a majority case for typical device scenarios so your layouts will
 * work well on most devices and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
@Immutable
@kotlin.jvm.JvmInline
value class WidthSizeClass private constructor(private val value: String) {
    companion object {
        /** Represents the majority of phones in portrait. */
        val Compact = WidthSizeClass("Compact")

        /**
         * Represents the majority of tablets in portrait and large unfolded inner displays in
         * portrait.
         */
        val Medium = WidthSizeClass("Medium")

        /**
         * Represents the majority of tablets in landscape and large unfolded inner displays in
         * landscape.
         */
        val Expanded = WidthSizeClass("Expanded")

        /** Calculates the [WidthSizeClass] for a given [width] */
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
 * Height-based window size class.
 *
 * A size class represents a breakpoint that can be used to build responsive layouts. Each size
 * class breakpoint represents a majority case for typical device scenarios so your layouts will
 * work well on most devices and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
@Immutable
@kotlin.jvm.JvmInline
value class HeightSizeClass private constructor(private val value: String) {
    companion object {
        /** Represents the majority of phones in landscape */
        val Compact = HeightSizeClass("Compact")

        /** Represents the majority of tablets in landscape and majority of phones in portrait */
        val Medium = HeightSizeClass("Medium")

        /** Represents the majority of tablets in portrait */
        val Expanded = HeightSizeClass("Expanded")

        /** Calculates the [HeightSizeClass] for a given [height] */
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
