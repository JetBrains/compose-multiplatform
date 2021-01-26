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

package androidx.compose.ui.graphics

import org.jetbrains.skija.ColorFilter as SkijaColorFilter

actual typealias NativeColorFilter = SkijaColorFilter

/**
 * Obtain a reference to the desktop ColorFilter type
 */
fun ColorFilter.asDesktopColorFilter(): SkijaColorFilter = nativeColorFilter

fun org.jetbrains.skija.ColorFilter.toComposeColorFilter(): ColorFilter = ColorFilter(this)

internal actual fun actualTintColorFilter(color: Color, blendMode: BlendMode): ColorFilter =
    ColorFilter(SkijaColorFilter.makeBlend(color.toArgb(), blendMode.toSkija()))

internal actual fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): ColorFilter =
    ColorFilter(
        SkijaColorFilter.makeMatrix(
            org.jetbrains.skija.ColorMatrix(*colorMatrix.values)
        )
    )

internal actual fun actualLightingColorFilter(multiply: Color, add: Color): ColorFilter =
    ColorFilter(SkijaColorFilter.makeLighting(multiply.toArgb(), add.toArgb()))