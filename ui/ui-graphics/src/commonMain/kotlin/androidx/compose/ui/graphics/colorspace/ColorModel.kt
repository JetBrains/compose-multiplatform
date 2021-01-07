/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics.colorspace

import androidx.compose.ui.graphics.colorspace.ColorModel.Rgb

/**
 * A color model is required by a [ColorSpace] to describe the
 * way colors can be represented as tuples of numbers. A common color
 * model is the [RGB][Rgb] color model which defines a color
 * as represented by a tuple of 3 numbers (red, green and blue).
 */
enum class ColorModel(
    /**
     * Returns the number of components for this color model.
     *
     * @return An integer between 1 and 4
     */
    /*@IntRange(from = 1, to = 4)*/
    val componentCount: Int
) {
    /**
     * The RGB model is a color model with 3 components that
     * refer to the three additive primiaries: red, green
     * and blue.
     */
    Rgb(3),

    /**
     * The XYZ model is a color model with 3 components that
     * are used to model human color vision on a basic sensory
     * level.
     */
    Xyz(3),

    /**
     * The Lab model is a color model with 3 components used
     * to describe a color space that is more perceptually
     * uniform than XYZ.
     */
    Lab(3),

    /**
     * The CMYK model is a color model with 4 components that
     * refer to four inks used in color printing: cyan, magenta,
     * yellow and black (or key). CMYK is a subtractive color
     * model.
     */
    Cmyk(4)
}