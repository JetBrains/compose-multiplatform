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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset

/**
 * Class that represents the corresponding Shader implementation on a platform. This maps
 * to Gradients or ImageShaders
 */
expect class Shader

/**
 * Creates a linear gradient from `from` to `to`.
 *
 * If `colorStops` is provided, each value is a number from 0.0 to 1.0
 * that specifies where the color at the corresponding index in [colors]
 * begins in the gradient. If `colorStops` is not provided, then the colors are dispersed evenly
 *
 * The behavior before [from] and after [to] is described by the `tileMode`
 * argument. For details, see the [TileMode] enum. If no [TileMode] is provided
 * the default value of [TileMode.Clamp] is used
 */
fun LinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>? = null,
    tileMode: TileMode = TileMode.Clamp
): Shader = ActualLinearGradientShader(
    from,
    to,
    colors,
    colorStops,
    tileMode
)

internal expect fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader

/**
 * Creates a radial gradient centered at `center` that ends at `radius`
 * distance from the center.
 *
 * If `colorStops` is provided, each value is a number from 0.0 to 1.0
 * that specifies where the color at the corresponding index in [colors]
 * begins in the gradient. If `colorStops` is not provided, then the colors are dispersed evenly
 *
 * The behavior before and after the radius is described by the `tileMode`
 * argument. For details, see the [TileMode] enum.
 *
 * The behavior outside of the bounds of [center] +/- [radius] is described by the `tileMode`
 * argument. For details, see the [TileMode] enum. If no [TileMode] is provided
 * the default value of [TileMode.Clamp] is used
 */
fun RadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>? = null,
    tileMode: TileMode = TileMode.Clamp
): Shader = ActualRadialGradientShader(center, radius, colors, colorStops, tileMode)

internal expect fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader

/**
 * Creates a circular gradient that sweeps around a provided center point. The sweep begins
 * relative to 3 o'clock and continues clockwise until it reaches the starting position again.
 *
 * If `colorStops` is provided, each value is a number from 0.0 to 1.0
 * that specifies where the color at the corresponding index in [colors]
 * begins in the gradient. If `colorStops` is not provided, then the colors are dispersed evenly
 *
 * @param center Position for the gradient to sweep around
 * @param colors Colors to be rendered as part of the gradient
 * @param colorStops Placement of the colors along the sweep about the center position
 */
fun SweepGradientShader(
    center: Offset,
    colors: List<Color>,
    colorStops: List<Float>? = null
): Shader = ActualSweepGradientShader(center, colors, colorStops)

internal expect fun ActualSweepGradientShader(
    center: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
): Shader

/**
 * Creates a Shader using the given [ImageBitmap] as an input texture. If the shader is
 * to be drawn in an area larger than the size of the [ImageBitmap], the region is filled
 * in the horizontal and vertical directions based on the [tileModeX] and [tileModeY] parameters.
 */
fun ImageShader(
    image: ImageBitmap,
    tileModeX: TileMode = TileMode.Clamp,
    tileModeY: TileMode = TileMode.Clamp
): Shader = ActualImageShader(image, tileModeX, tileModeY)

internal expect fun ActualImageShader(
    image: ImageBitmap,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader