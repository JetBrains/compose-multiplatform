/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import org.jetbrains.skija.FilterTileMode
import org.jetbrains.skija.GradientStyle

actual typealias Shader = org.jetbrains.skija.Shader

internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    return Shader.makeLinearGradient(
        from.x, from.y, to.x, to.y, colors.toIntArray(), colorStops?.toFloatArray(),
        GradientStyle(tileMode.toSkijaRect(), true, identityMatrix33())
    )
}

internal actual fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    return Shader.makeRadialGradient(
        center.x,
        center.y,
        radius,
        colors.toIntArray(),
        colorStops?.toFloatArray(),
        GradientStyle(tileMode.toSkijaRect(), true, identityMatrix33())
    )
}

internal actual fun ActualImageShader(
    image: ImageBitmap,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader {
    return image.asDesktopBitmap().makeShader(
        tileModeX.toSkijaTileMode(),
        tileModeY.toSkijaTileMode()
    )
}

private fun TileMode.toSkijaTileMode() = when (this) {
    TileMode.Clamp -> FilterTileMode.CLAMP
    TileMode.Repeated -> FilterTileMode.REPEAT
    TileMode.Mirror -> FilterTileMode.MIRROR
}

private fun List<Color>.toIntArray(): IntArray =
    IntArray(size) { i -> this[i].toArgb() }

private fun validateColorStops(colors: List<Color>, colorStops: List<Float>?) {
    if (colorStops == null) {
        if (colors.size < 2) {
            throw IllegalArgumentException(
                "colors must have length of at least 2 if colorStops " +
                    "is omitted."
            )
        }
    } else if (colors.size != colorStops.size) {
        throw IllegalArgumentException(
            "colors and colorStops arguments must have" +
                " equal length."
        )
    }
}

private fun TileMode.toSkijaRect() = when (this) {
    TileMode.Clamp -> FilterTileMode.CLAMP
    TileMode.Repeated -> FilterTileMode.REPEAT
    TileMode.Mirror -> FilterTileMode.MIRROR
}