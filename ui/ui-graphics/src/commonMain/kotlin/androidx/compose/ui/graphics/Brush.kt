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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.nativeClass

@Immutable
sealed class Brush {
    abstract fun applyTo(p: Paint, alpha: Float)
}

@Immutable
class SolidColor(val value: Color) : Brush() {
    override fun applyTo(p: Paint, alpha: Float) {
        p.alpha = DefaultAlpha
        p.color = if (alpha != DefaultAlpha) {
            value.copy(alpha = value.alpha * alpha)
        } else {
            value
        }
        if (p.shader != null) p.shader = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (nativeClass() != other?.nativeClass()) return false

        other as SolidColor

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "SolidColor(value=$value)"
    }
}

typealias ColorStop = Pair<Float, Color>

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates.
 * The colors are
 *
 * ```
 *  LinearGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      startX = 0.0f,
 *      startY = 50.0f,
 *      endY = 0.0f,
 *      endY = 100.0f
 * )
 * ```
 */
@Stable
fun LinearGradient(
    colors: List<Color>,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    tileMode: TileMode = TileMode.Clamp
) = LinearGradient(
    colors,
    null,
    startX,
    startY,
    endX,
    endY,
    tileMode
)

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates.
 * The colors are dispersed at the provided offset defined in the [ColorStop]
 *
 * ```
 *  LinearGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      startX = 0.0f,
 *      startY = 50.0f,
 *      endY = 0.0f,
 *      endY = 100.0f
 * )
 * ```
 */
@Stable
fun LinearGradient(
    vararg colorStops: ColorStop,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    tileMode: TileMode = TileMode.Clamp
) = LinearGradient(
    List<Color>(colorStops.size) { i -> colorStops[i].second },
    List<Float>(colorStops.size) { i -> colorStops[i].first },
    startX,
    startY,
    endX,
    endY,
    tileMode
)

/**
 * Creates a radial gradient with the given colors at the provided offset defined in the [ColorStop]
 * ```
 * RadialGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.Repeated
 * )
 * ```
 */
@Stable
fun RadialGradient(
    vararg colorStops: ColorStop,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.Clamp
) = RadialGradient(
    List<Color>(colorStops.size) { i -> colorStops[i].second },
    List<Float>(colorStops.size) { i -> colorStops[i].first },
    centerX,
    centerY,
    radius,
    tileMode
)

/**
 * Creates a radial gradient with the given colors evenly dispersed within the gradient
 * ```
 * RadialGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.Repeated
 * )
 * ```
 */
@Stable
fun RadialGradient(
    colors: List<Color>,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.Clamp
) = RadialGradient(colors, null, centerX, centerY, radius, tileMode)

/**
 * Creates a vertical gradient with the given colors evenly dispersed within the gradient
 * Ex:
 * ```
 *  VerticalGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      startY = 0.0f,
 *      endY = 100.0f
 * )
 *
 * ```
 */
@Stable
fun VerticalGradient(
    colors: List<Color>,
    startY: Float,
    endY: Float,
    tileMode: TileMode = TileMode.Clamp
) = LinearGradient(
    colors,
    null,
    startX = 0.0f,
    startY = startY,
    endX = 0.0f,
    endY = endY,
    tileMode = tileMode
)

/**
 * Creates a vertical gradient with the given colors at the provided offset defined in the [ColorStop]
 * Ex:
 * ```
 *  VerticalGradient(
 *      0.1f to Color.Red,
 *      0.3f to Color.Green,
 *      0.5f to Color.Blue,
 *      startY = 0.0f,
 *      endY = 100.0f
 * )
 * ```
 */
@Stable
fun VerticalGradient(
    vararg colorStops: ColorStop,
    startY: Float,
    endY: Float,
    tileMode: TileMode = TileMode.Clamp
) = LinearGradient(
    List<Color>(colorStops.size) { i -> colorStops[i].second },
    List<Float>(colorStops.size) { i -> colorStops[i].first },
    startX = 0.0f,
    startY = startY,
    endX = 0.0f,
    endY = endY,
    tileMode = tileMode
)

/**
 * Creates a horizontal gradient with the given colors evenly dispersed within the gradient
 *
 * Ex:
 * ```
 *  HorizontalGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      startX = 10.0f,
 *      endX = 20.0f
 * )
 * ```
 */
@Stable
fun HorizontalGradient(
    colors: List<Color>,
    startX: Float,
    endX: Float,
    tileMode: TileMode = TileMode.Clamp
) = LinearGradient(
    colors,
    null,
    startX = startX,
    startY = 0.0f,
    endX = endX,
    endY = 0.0f,
    tileMode = tileMode
)

/**
 * Creates a horizontal gradient with the given colors dispersed at the provided offset defined in the [ColorStop]
 *
 * Ex:
 * ```
 *  HorizontalGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      startX = 0.0f,
 *      endX = 100.0f
 * )
 * ```
 */
@Stable
fun HorizontalGradient(
    vararg colorStops: ColorStop,
    startX: Float,
    endX: Float,
    tileMode: TileMode = TileMode.Clamp
) = LinearGradient(
    List<Color>(colorStops.size) { i -> colorStops[i].second },
    List<Float>(colorStops.size) { i -> colorStops[i].first },
    startX = startX,
    startY = 0.0f,
    endX = endX,
    endY = 0.0f,
    tileMode = tileMode
)

/**
 * Creates a sweep gradient with the given colors dispersed around the center with
 * offsets defined in each [ColorStop]. The sweep begins relative to 3 o'clock and continues
 * clockwise until it reaches the starting position again.
 *
 * Ex:
 * ```
 *  SweepGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      center = Offset(0.0f, 100.0f)
 * )
 * ```
 */
@Stable
fun SweepGradient(
    vararg colorStops: ColorStop,
    center: Offset
) = SweepGradient(
    center,
    List<Color>(colorStops.size) { i -> colorStops[i].second },
    List<Float>(colorStops.size) { i -> colorStops[i].first },
)

/**
 * Creates a sweep gradient with the given colors dispersed evenly around the center.
 * The sweep begins relative to 3 o'clock and continues clockwise until it reaches the starting
 * position again.
 *
 * Ex:
 * ```
 *  SweepGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      center = Offset(10.0f, 20.0f)
 * )
 * ```
 */
@Stable
fun SweepGradient(
    colors: List<Color>,
    center: Offset
) = SweepGradient(center, colors, null)

/**
 * Brush implementation used to apply a linear gradient on a given [Paint]
 */
@Immutable
class LinearGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val startX: Float,
    private val startY: Float,
    private val endX: Float,
    private val endY: Float,
    private val tileMode: TileMode = TileMode.Clamp
) : ShaderBrush(
    LinearGradientShader(
        Offset(startX, startY),
        Offset(endX, endY),
        colors,
        stops,
        tileMode
    )
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (nativeClass() != other?.nativeClass()) return false

        other as LinearGradient

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (startX != other.startX) return false
        if (startY != other.startY) return false
        if (endX != other.endX) return false
        if (endY != other.endY) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + startX.hashCode()
        result = 31 * result + startY.hashCode()
        result = 31 * result + endX.hashCode()
        result = 31 * result + endY.hashCode()
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "LinearGradient(colors=$colors, " +
            "stops=$stops, " +
            "startX=$startX, " +
            "startY=$startY, " +
            "endX=$endX, " +
            "endY=$endY, " +
            "tileMode=$tileMode)"
    }
}

/**
 * Brush implementation used to apply a radial gradient on a given [Paint]
 */
@Immutable
class RadialGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val centerX: Float,
    private val centerY: Float,
    private val radius: Float,
    private val tileMode: TileMode = TileMode.Clamp
) : ShaderBrush(
    RadialGradientShader(
        Offset(centerX, centerY),
        radius,
        colors,
        stops,
        tileMode
    )
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (nativeClass() != other?.nativeClass()) return false

        other as RadialGradient

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (centerX != other.centerX) return false
        if (centerY != other.centerY) return false
        if (radius != other.radius) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + centerX.hashCode()
        result = 31 * result + centerY.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "RadialGradient(" +
            "colors=$colors, " +
            "stops=$stops, " +
            "centerX=$centerX, " +
            "centerY=$centerY, " +
            "radius=$radius, " +
            "tileMode=$tileMode)"
    }
}

/**
 * Brush implementation used to apply a sweep gradient on a given [Paint]
 */
@Immutable
class SweepGradient internal constructor(
    private val center: Offset,
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
) : ShaderBrush(
    SweepGradientShader(
        center,
        colors,
        stops
    )
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (nativeClass() != other?.nativeClass()) return false

        other as SweepGradient

        if (center != other.center) return false
        if (colors != other.colors) return false
        if (stops != other.stops) return false

        return true
    }

    override fun hashCode(): Int {
        var result = center.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SweepGradient(center=$center, colors=$colors, stops=$stops)"
    }
}

/**
 * Brush implementation that wraps and applies a the provided shader to a [Paint]
 */
@Immutable
open class ShaderBrush(val shader: Shader) : Brush() {
    final override fun applyTo(p: Paint, alpha: Float) {
        if (p.color != Color.Black) p.color = Color.Black
        if (p.shader != shader) p.shader = shader
        if (p.alpha != alpha) p.alpha = alpha
    }
}
