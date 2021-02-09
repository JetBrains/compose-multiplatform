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

// TODO mark internal once https://youtrack.jetbrains.com/issue/KT-36695 is fixed
/* internal */ expect class NativeColorFilter

/**
 * Effect used to modify the color of each pixel drawn on a [Paint] that it is installed on
 */
@Immutable
class ColorFilter internal constructor(internal val nativeColorFilter: NativeColorFilter) {
    companion object {
        /**
         * Creates a color filter that applies the blend mode given as the second
         * argument. The source color is the one given as the first argument, and the
         * destination color is the one from the layer being composited.
         *
         * The output of this filter is then composited into the background according
         * to the [Paint.blendMode], using the output of this filter as the source
         * and the background as the destination.
         *
         * @param color Color used to blend source content
         * @param blendMode BlendMode used when compositing the tint color to the destination
         */
        @Stable
        fun tint(color: Color, blendMode: BlendMode = BlendMode.SrcIn): ColorFilter =
            actualTintColorFilter(color, blendMode)

        /**
         * Create a [ColorFilter] that transforms colors through a 4x5 color matrix. This filter can
         * be used to change the saturation of pixels, convert from YUV to RGB, etc.
         *
         * @param colorMatrix ColorMatrix used to transform pixel values when drawn
         */
        @Stable
        fun colorMatrix(colorMatrix: ColorMatrix): ColorFilter =
            actualColorMatrixColorFilter(colorMatrix)

        /**
         * Create a [ColorFilter] that can be used to simulate simple lighting effects.
         * A lighting ColorFilter is defined by two parameters, one used to multiply the source
         * color and one used to add to the source color
         *
         * @param multiply Color that will be added to the source color when the color
         *          filter is applied
         * @param add Color used to multiply the source color when the color filter is applied.
         */
        @Stable
        fun lighting(multiply: Color, add: Color): ColorFilter =
            actualLightingColorFilter(multiply, add)
    }
}

internal expect fun actualTintColorFilter(color: Color, blendMode: BlendMode): ColorFilter

internal expect fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): ColorFilter

internal expect fun actualLightingColorFilter(multiply: Color, add: Color): ColorFilter