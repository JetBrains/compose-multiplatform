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

import org.jetbrains.skia.ColorFilter as SkiaColorFilter

actual typealias NativeColorFilter = SkiaColorFilter

/**
 * Obtain a [org.jetbrains.skia.ColorFilter] instance from this [ColorFilter]
 */
fun ColorFilter.asSkiaColorFilter(): SkiaColorFilter = nativeColorFilter

/**
 * Create a [ColorFilter] from the given [org.jetbrains.skia.ColorFilter] instance
 */
fun org.jetbrains.skia.ColorFilter.asComposeColorFilter(): ColorFilter = ColorFilter(this)

internal actual fun actualTintColorFilter(color: Color, blendMode: BlendMode): ColorFilter =
    ColorFilter(SkiaColorFilter.makeBlend(color.toArgb(), blendMode.toSkia()))

internal actual fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): ColorFilter =
    ColorFilter(
        SkiaColorFilter.makeMatrix(
            org.jetbrains.skia.ColorMatrix(*colorMatrix.values)
        )
    )

internal actual fun actualLightingColorFilter(multiply: Color, add: Color): ColorFilter =
    ColorFilter(SkiaColorFilter.makeLighting(multiply.toArgb(), add.toArgb()))