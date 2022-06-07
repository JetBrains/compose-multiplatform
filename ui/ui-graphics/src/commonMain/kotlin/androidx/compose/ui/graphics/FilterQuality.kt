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
 * Quality levels for image filters.
 * See [Paint.filterQuality].
 */
@Immutable
@kotlin.jvm.JvmInline
value class FilterQuality internal constructor(val value: Int) {

    companion object {
        /**
         * Fastest possible filtering, albeit also the lowest quality
         * Typically this implies nearest-neighbour filtering.
         */
        val None = FilterQuality(0)

        /**
         * Better quality than [None], faster than [Medium].
         * Typically this implies bilinear interpolation.
         */
        val Low = FilterQuality(1)

        /**
         * Better quality than [Low], faster than [High].
         *
         * Typically this implies a combination of bilinear interpolation and
         * pyramidal parametric prefiltering (mipmaps).
         */
        val Medium = FilterQuality(2)

        /**
         * Best possible quality filtering, albeit also the slowest.
         * Typically this implies bicubic interpolation or better.
         */
        val High = FilterQuality(3)
    }

    override fun toString() = when (this) {
        None -> "None"
        Low -> "Low"
        Medium -> "Medium"
        High -> "High"
        else -> "Unknown"
    }
}