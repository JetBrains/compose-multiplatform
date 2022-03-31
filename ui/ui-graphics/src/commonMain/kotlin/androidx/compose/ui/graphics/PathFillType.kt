/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable

/**
 * Determines the winding rule that decides how the interior of a [Path] is
 * calculated.
 *
 * This enum is used by the [Path.fillType] property.
 */
@Immutable
@kotlin.jvm.JvmInline
value class PathFillType internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * The interior is defined by a non-zero sum of signed edge crossings.
         *
         * For a given point, the point is considered to be on the inside of the path
         * if a line drawn from the point to infinity crosses lines going clockwise
         * around the point a different number of times than it crosses lines going
         * counter-clockwise around that point.
         *
         * See: <https://en.wikipedia.org/wiki/Nonzero-rule>
         */
        val NonZero = PathFillType(0)

        /**
         * The interior is defined by an odd number of edge crossings.
         *
         * For a given point, the point is considered to be on the inside of the path
         * if a line drawn from the point to infinity crosses an odd number of lines.
         *
         * See: <https://en.wikipedia.org/wiki/Even-odd_rule>
         */
        val EvenOdd = PathFillType(1)
    }

    override fun toString() = when (this) {
        NonZero -> "NonZero"
        EvenOdd -> "EvenOdd"
        else -> "Unknown"
    }
}