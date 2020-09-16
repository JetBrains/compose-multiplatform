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

// Quality levels for image filters.
//
// See [Paint.filterQuality].
enum class FilterQuality {

    /**
     * Fastest possible filtering, albeit also the lowest quality
     * Typically this implies nearest-neighbour filtering.
     */
    None,

    /**
     * Better quality than [None], faster than [Medium].
     * Typically this implies bilinear interpolation.
     */
    Low,

    /**
     * Better quality than [Low], faster than [High].
     *
     * Typically this implies a combination of bilinear interpolation and
     * pyramidal parametric prefiltering (mipmaps).
     */
    Medium,

    /**
     * Best possible quality filtering, albeit also the slowest.
     * Typically this implies bicubic interpolation or better.
     */
    High,
}