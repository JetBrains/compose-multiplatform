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
import androidx.compose.runtime.Stable

/**
 * Creates a color filter that applies the blend mode given as the second
 * argument. The source color is the one given as the first argument, and the
 * destination color is the one from the layer being composited.
 *
 * The output of this filter is then composited into the background according
 * to the [Paint.blendMode], using the output of this filter as the source
 * and the background as the destination.
 */
@Immutable
data class ColorFilter(
    @Stable
    val color: Color,
    @Stable
    val blendMode: BlendMode
) {
    companion object {
        /**
         * Helper method to create a [ColorFilter] that tints contents to the specified color
         */
        @Stable
        fun tint(color: Color): ColorFilter = ColorFilter(color, BlendMode.SrcIn)
    }
}