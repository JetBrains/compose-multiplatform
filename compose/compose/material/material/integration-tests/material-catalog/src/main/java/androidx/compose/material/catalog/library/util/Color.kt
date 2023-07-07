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

package androidx.compose.material.catalog.library.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * Calculates an 'on' color for this color.
 *
 * @return [Color.Black] or [Color.White], depending on [isLightColor].
 */
fun Color.onColor(): Color {
    return if (isLightColor()) Color.Black else Color.White
}

/**
 * Calculates if this color is considered light.
 *
 * @return true or false, depending on the higher contrast between [Color.Black] and [Color.White].
 *
 */
fun Color.isLightColor(): Boolean {
    val contrastForBlack = calculateContrast(foreground = Color.Black)
    val contrastForWhite = calculateContrast(foreground = Color.White)
    return contrastForBlack > contrastForWhite
}

/**
 * Calculates a rudimentary darkened variant color for this color.
 *
 * @return this linear interpolated with [Color.Black] at 30% fraction.
 */
fun Color.variantColor(): Color {
    return lerp(this, Color.Black, 0.3f)
}

private fun Color.calculateContrast(foreground: Color): Double {
    return ColorUtils.calculateContrast(foreground.toArgb(), toArgb())
}
