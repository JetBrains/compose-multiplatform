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
import androidx.compose.ui.graphics.colorspace.ColorModel
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.Rgb
import androidx.compose.ui.graphics.colorspace.connect
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min

/**
 * The `Color` class contains color information to be used while painting
 * in [Canvas]. `Color` supports [ColorSpace]s with 3 [components][ColorSpace.componentCount],
 * plus one for [alpha].
 *
 * ### Creating
 *
 * `Color` can be created with one of these methods:
 *
 *     // from 4 separate [Float] components. Alpha and ColorSpace are optional
 *     val rgbaWhiteFloat = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f,
 *         ColorSpace.get(ColorSpaces.Srgb))
 *
 *     // from a 32-bit SRGB color integer
 *     val fromIntWhite = Color(android.graphics.Color.WHITE)
 *     val fromLongBlue = Color(0xFF0000FF)
 *
 *     // from SRGB integer component values. Alpha is optional
 *     val rgbaWhiteInt = Color(red = 0xFF, green = 0xFF, blue = 0xFF, alpha = 0xFF)
 *
 * ### Representation
 *
 * A `Color` always defines a color using 4 components packed in a single
 * 64 bit long value. One of these components is always alpha while the other
 * three components depend on the color space's [color model][ColorModel].
 * The most common color model is the [RGB][ColorModel.Rgb] model in
 * which the components represent red, green, and blue values.
 *
 * **Component ranges:** the ranges defined in the tables
 * below indicate the ranges that can be encoded in a color long. They do not
 * represent the actual ranges as they may differ per color space. For instance,
 * the RGB components of a color in the [Display P3][ColorSpaces.DisplayP3]
 * color space use the `[0..1]` range. Please refer to the documentation of the
 * various [color spaces][ColorSpaces] to find their respective ranges.
 *
 * **Alpha range:** while alpha is encoded in a color long using
 * a 10 bit integer (thus using a range of `[0..1023]`), it is converted to and
 * from `[0..1]` float values when decoding and encoding color longs.
 *
 * **sRGB color space:** for compatibility reasons and ease of
 * use, `Color` encoded [sRGB][ColorSpaces.Srgb] colors do not
 * use the same encoding as other color longs.
 * ```
 * | Component | Name        | Size    | Range                 |
 * |-----------|-------------|---------|-----------------------|
 * | [RGB][ColorSpace.Model.Rgb] color model |
 * | R         | Red         | 16 bits | `[-65504.0, 65504.0]` |
 * | G         | Green       | 16 bits | `[-65504.0, 65504.0]` |
 * | B         | Blue        | 16 bits | `[-65504.0, 65504.0]` |
 * | A         | Alpha       | 10 bits | `[0..1023]`           |
 * |           | Color space | 6 bits  | `[0..63]`             |
 * | [SRGB][ColorSpaces.Srgb] color space |
 * | R         | Red         | 8 bits  | `[0..255]`            |
 * | G         | Green       | 8 bits  | `[0..255]`            |
 * | B         | Blue        | 8 bits  | `[0..255]`            |
 * | A         | Alpha       | 8 bits  | `[0..255]`            |
 * | X         | Unused      | 32 bits | `[0]`                 |
 * | [XYZ][ColorSpace.Model.Xyz] color model |
 * | X         | X           | 16 bits | `[-65504.0, 65504.0]` |
 * | Y         | Y           | 16 bits | `[-65504.0, 65504.0]` |
 * | Z         | Z           | 16 bits | `[-65504.0, 65504.0]` |
 * | A         | Alpha       | 10 bits | `[0..1023]`           |
 * |           | Color space | 6 bits  | `[0..63]`             |
 * | [Lab][ColorSpace.Model.Lab] color model |
 * | L         | L           | 16 bits | `[-65504.0, 65504.0]` |
 * | a         | a           | 16 bits | `[-65504.0, 65504.0]` |
 * | b         | b           | 16 bits | `[-65504.0, 65504.0]` |
 * | A         | Alpha       | 10 bits | `[0..1023]`           |
 * |           | Color space | 6 bits  | `[0..63]`             |
 * ```
 * The components in this table are listed in encoding order (see below),
 * which is why color longs in the RGB model are called RGBA colors (even if
 * this doesn't quite hold for the special case of sRGB colors).
 *
 * The color encoding relies on half-precision float values (fp16). If you
 * wish to know more about the limitations of half-precision float values, please
 * refer to the documentation of the [Float16] class.
 *
 * The values returned by these methods depend on the color space encoded
 * in the color long. The values are however typically in the `[0..1]` range
 * for RGB colors. Please refer to the documentation of the various
 * [color spaces][ColorSpaces] for the exact ranges.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Color(val value: ULong) {
    /**
     * Returns this color's color space.
     *
     * @return A non-null instance of [ColorSpace]
     */
    @Stable
    val colorSpace: ColorSpace
        get() = ColorSpaces.getColorSpace((value and 0x3fUL).toInt())

    /**
     * Converts this color from its color space to the specified color space.
     * The conversion is done using the default rendering intent as specified
     * by [ColorSpace.connect].
     *
     * @param colorSpace The destination color space, cannot be null
     *
     * @return A non-null color instance in the specified color space
     */
    fun convert(colorSpace: ColorSpace): Color {
        if (colorSpace == this.colorSpace) {
            return this // nothing to convert
        }
        val connector = this.colorSpace.connect(colorSpace)
        val color = getComponents()
        connector.transform(color)
        return Color(
            red = color[0],
            green = color[1],
            blue = color[2],
            alpha = color[3],
            colorSpace = colorSpace
        )
    }

    /**
     * Returns the value of the red component in the range defined by this
     * color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorModel.Rgb],
     * calling this is the first component of the ColorSpace.
     *
     * @see alpha
     * @see blue
     * @see green
     */
    @Stable
    val red: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 48) and 0xffUL).toFloat() / 255.0f
            } else {
                Float16(((value shr 48) and 0xffffUL).toShort())
                    .toFloat()
            }
        }

    /**
     * Returns the value of the green component in the range defined by this
     * color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorModel.Rgb],
     * calling this is the second component of the ColorSpace.
     *
     * @see alpha
     * @see red
     * @see blue
     */
    @Stable
    val green: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 40) and 0xffUL).toFloat() / 255.0f
            } else {
                Float16(((value shr 32) and 0xffffUL).toShort())
                    .toFloat()
            }
        }

    /**
     * Returns the value of the blue component in the range defined by this
     * color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorModel.Rgb],
     * calling this is the third component of the ColorSpace.
     *
     * @see alpha
     * @see red
     * @see green
     */
    @Stable
    val blue: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 32) and 0xffUL).toFloat() / 255.0f
            } else {
                Float16(((value shr 16) and 0xffffUL).toShort())
                    .toFloat()
            }
        }

    /**
     * Returns the value of the alpha component in the range `[0..1]`.
     *
     * @see red
     * @see green
     * @see blue
     */
    @Stable
    val alpha: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 56) and 0xffUL).toFloat() / 255.0f
            } else {
                ((value shr 6) and 0x3ffUL).toFloat() / 1023.0f
            }
        }

    @Stable
    operator fun component1(): Float = red

    @Stable
    operator fun component2(): Float = green

    @Stable
    operator fun component3(): Float = blue

    @Stable
    operator fun component4(): Float = alpha

    @Stable
    operator fun component5(): ColorSpace = colorSpace

    /**
     * Copies the existing color, changing only the provided values. The [ColorSpace][colorSpace]
     * of the returned [Color] is the same as this [colorSpace].
     */
    @Stable
    fun copy(
        alpha: Float = this.alpha,
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue
    ): Color = Color(
        red = red,
        green = green,
        blue = blue,
        alpha = alpha,
        colorSpace = this.colorSpace
    )

    /**
     * Returns a string representation of the object. This method returns
     * a string equal to the value of:
     *
     *     "Color($r, $g, $b, $a, ${colorSpace.name})"
     *
     * For instance, the string representation of opaque black in the sRGB
     * color space is equal to the following value:
     *
     *     Color(0.0, 0.0, 0.0, 1.0, sRGB IEC61966-2.1)
     *
     * @return A non-null string representation of the object
     */
    override fun toString(): String {
        return "Color($red, $green, $blue, $alpha, ${colorSpace.name})"
    }

    companion object {
        @Stable
        val Black = Color(0xFF000000)
        @Stable
        val DarkGray = Color(0xFF444444)
        @Stable
        val Gray = Color(0xFF888888)
        @Stable
        val LightGray = Color(0xFFCCCCCC)
        @Stable
        val White = Color(0xFFFFFFFF)
        @Stable
        val Red = Color(0xFFFF0000)
        @Stable
        val Green = Color(0xFF00FF00)
        @Stable
        val Blue = Color(0xFF0000FF)
        @Stable
        val Yellow = Color(0xFFFFFF00)
        @Stable
        val Cyan = Color(0xFF00FFFF)
        @Stable
        val Magenta = Color(0xFFFF00FF)
        @Stable
        val Transparent = Color(0x00000000)

        /**
         * Because Color is an inline class, this represents an unset value
         * without having to box the Color. It will be treated as [Transparent]
         * when drawn. A Color can compare with [Unspecified] for equality or use
         * [isUnspecified] to check for the unset value or [isSpecified] for any color that isn't
         * [Unspecified].
         */
        @Stable
        val Unspecified = Color(0f, 0f, 0f, 0f, ColorSpaces.Unspecified)

        /**
         * Return a [Color] from [hue], [saturation], and [value] (HSV representation).
         *
         * @param hue The color value in the range (0..360), where 0 is red, 120 is green, and
         * 240 is blue
         * @param saturation The amount of [hue] represented in the color in the range (0..1),
         * where 0 has no color and 1 is fully saturated.
         * @param value The strength of the color, where 0 is black.
         * @param colorSpace The RGB color space used to calculate the Color from the HSV values.
         */
        fun hsv(
            hue: Float,
            saturation: Float,
            value: Float,
            alpha: Float = 1f,
            colorSpace: Rgb = ColorSpaces.Srgb
        ): Color {
            require(hue in 0f..360f && saturation in 0f..1f && value in 0f..1f) {
                "HSV ($hue, $saturation, $value) must be in range (0..360, 0..1, 0..1)"
            }
            val red = hsvToRgbComponent(5, hue, saturation, value)
            val green = hsvToRgbComponent(3, hue, saturation, value)
            val blue = hsvToRgbComponent(1, hue, saturation, value)
            return Color(red, green, blue, alpha, colorSpace)
        }

        private fun hsvToRgbComponent(n: Int, h: Float, s: Float, v: Float): Float {
            val k = (n.toFloat() + h / 60f) % 6f
            return v - (v * s * max(0f, minOf(k, 4 - k, 1f)))
        }

        /**
         * Return a [Color] from [hue], [saturation], and [lightness] (HSL representation).
         *
         * @param hue The color value in the range (0..360), where 0 is red, 120 is green, and
         * 240 is blue
         * @param saturation The amount of [hue] represented in the color in the range (0..1),
         * where 0 has no color and 1 is fully saturated.
         * @param lightness A range of (0..1) where 0 is black, 0.5 is fully colored, and 1 is
         * white.
         * @param colorSpace The RGB color space used to calculate the Color from the HSL values.
         */
        fun hsl(
            hue: Float,
            saturation: Float,
            lightness: Float,
            alpha: Float = 1f,
            colorSpace: Rgb = ColorSpaces.Srgb
        ): Color {
            require(hue in 0f..360f && saturation in 0f..1f && lightness in 0f..1f) {
                "HSL ($hue, $saturation, $lightness) must be in range (0..360, 0..1, 0..1)"
            }
            val red = hslToRgbComponent(0, hue, saturation, lightness)
            val green = hslToRgbComponent(8, hue, saturation, lightness)
            val blue = hslToRgbComponent(4, hue, saturation, lightness)
            return Color(red, green, blue, alpha, colorSpace)
        }

        private fun hslToRgbComponent(n: Int, h: Float, s: Float, l: Float): Float {
            val k = (n.toFloat() + h / 30f) % 12f
            val a = s * min(l, 1f - l)
            return l - a * max(-1f, minOf(k - 3, 9 - k, 1f))
        }
    }
}

/**
 * Create a [Color] by passing individual [red], [green], [blue], [alpha], and [colorSpace]
 * components. The default [color space][ColorSpace] is [SRGB][ColorSpaces.Srgb] and
 * the default [alpha] is `1.0` (opaque). [colorSpace] must have a [ColorSpace.componentCount] of
 * 3.
 */
@Stable
fun Color(
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float = 1f,
    colorSpace: ColorSpace = ColorSpaces.Srgb
): Color {
    require(
        red in colorSpace.getMinValue(0)..colorSpace.getMaxValue(0) &&
            green in colorSpace.getMinValue(1)..colorSpace.getMaxValue(1) &&
            blue in colorSpace.getMinValue(2)..colorSpace.getMaxValue(2) &&
            alpha in 0f..1f
    ) {
        "red = $red, green = $green, blue = $blue, alpha = $alpha outside the range for $colorSpace"
    }

    if (colorSpace.isSrgb) {
        val argb = (
            ((alpha * 255.0f + 0.5f).toInt() shl 24) or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt()
            )
        return Color(value = (argb.toULong() and 0xffffffffUL) shl 32)
    }

    require(colorSpace.componentCount == 3) {
        "Color only works with ColorSpaces with 3 components"
    }

    val id = colorSpace.id
    require(id != ColorSpace.MinId) {
        "Unknown color space, please use a color space in ColorSpaces"
    }

    val r = Float16(red)
    val g = Float16(green)
    val b = Float16(blue)

    val a = (max(0.0f, min(alpha, 1.0f)) * 1023.0f + 0.5f).toInt()

    // Suppress sign extension
    return Color(
        value = (
            ((r.halfValue.toULong() and 0xffffUL) shl 48) or (
                (g.halfValue.toULong() and 0xffffUL) shl 32
                ) or (
                (b.halfValue.toULong() and 0xffffUL) shl 16
                ) or (
                (a.toULong() and 0x3ffUL) shl 6
                ) or (
                id.toULong() and 0x3fUL
                )
            )
    )
}

/**
 * Creates a new [Color] instance from an ARGB color int.
 * The resulting color is in the [sRGB][ColorSpaces.Srgb]
 * color space.
 *
 * @param color The ARGB color int to create a <code>Color</code> from.
 * @return A non-null instance of {@link Color}
 */
@Stable
fun Color(/*@ColorInt*/ color: Int): Color {
    return Color(value = color.toULong() shl 32)
}

/**
 * Creates a new [Color] instance from an ARGB color int.
 * The resulting color is in the [sRGB][ColorSpaces.Srgb]
 * color space. This is useful for specifying colors with alpha
 * greater than 0x80 in numeric form without using [Long.toInt]:
 *
 *     val color = Color(0xFF000080)
 *
 * @param color The 32-bit ARGB color int to create a <code>Color</code>
 * from
 * @return A non-null instance of {@link Color}
 */
@Stable
fun Color(color: Long): Color {
    return Color(value = (color.toULong() and 0xffffffffUL) shl 32)
}

/**
 * Creates a new [Color] instance from an ARGB color components.
 * The resulting color is in the [sRGB][ColorSpaces.Srgb]
 * color space. The default alpha value is `0xFF` (opaque).
 *
 * @param red The red component of the color, between 0 and 255.
 * @param green The green component of the color, between 0 and 255.
 * @param blue The blue component of the color, between 0 and 255.
 * @param alpha The alpha component of the color, between 0 and 255.
 *
 * @return A non-null instance of {@link Color}
 */
@Stable
fun Color(
    /*@IntRange(from = 0, to = 0xFF)*/
    red: Int,
    /*@IntRange(from = 0, to = 0xFF)*/
    green: Int,
    /*@IntRange(from = 0, to = 0xFF)*/
    blue: Int,
    /*@IntRange(from = 0, to = 0xFF)*/
    alpha: Int = 0xFF
): Color {
    val color = ((alpha and 0xFF) shl 24) or
        ((red and 0xFF) shl 16) or
        ((green and 0xFF) shl 8) or
        (blue and 0xFF)
    return Color(color)
}

/**
 * Linear interpolate between two [Colors][Color], [start] and [stop] with [fraction] fraction
 * between the two. The [ColorSpace] of the result is always the [ColorSpace][Color.colorSpace]
 * of [stop]. [fraction] should be between 0 and 1, inclusive. Interpolation is done
 * in the [ColorSpaces.Oklab] color space.
 */
@Stable
fun lerp(start: Color, stop: Color, /*@FloatRange(from = 0.0, to = 1.0)*/ fraction: Float): Color {
    val colorSpace = ColorSpaces.Oklab
    val startColor = start.convert(colorSpace)
    val endColor = stop.convert(colorSpace)

    val startAlpha = startColor.alpha
    val startL = startColor.red
    val startA = startColor.green
    val startB = startColor.blue

    val endAlpha = endColor.alpha
    val endL = endColor.red
    val endA = endColor.green
    val endB = endColor.blue

    val interpolated = Color(
        alpha = lerp(startAlpha, endAlpha, fraction),
        red = lerp(startL, endL, fraction),
        green = lerp(startA, endA, fraction),
        blue = lerp(startB, endB, fraction),
        colorSpace = colorSpace
    )
    return interpolated.convert(stop.colorSpace)
}

/**
 * Composites [this] color on top of [background] using the Porter-Duff 'source over' mode.
 *
 * Both [this] and [background] must not be pre-multiplied, and the resulting color will also
 * not be pre-multiplied.
 *
 * The [ColorSpace] of the result is always the [ColorSpace][Color.colorSpace] of [background].
 *
 * @return the [Color] representing [this] composited on top of [background], converted to the
 * color space of [background].
 */
@Stable
fun Color.compositeOver(background: Color): Color {
    val fg = this.convert(background.colorSpace)

    val bgA = background.alpha
    val fgA = fg.alpha
    val a = fgA + (bgA * (1f - fgA))

    val r = compositeComponent(fg.red, background.red, fgA, bgA, a)
    val g = compositeComponent(fg.green, background.green, fgA, bgA, a)
    val b = compositeComponent(fg.blue, background.blue, fgA, bgA, a)

    return Color(r, g, b, a, background.colorSpace)
}

/**
 * Composites the given [foreground component][fgC] over the [background component][bgC], with
 * foreground and background alphas of [fgA] and [bgA] respectively.
 *
 * This uses a pre-calculated composite destination alpha of [a] for efficiency.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun compositeComponent(
    fgC: Float,
    bgC: Float,
    fgA: Float,
    bgA: Float,
    a: Float
) = if (a == 0f) 0f else ((fgC * fgA) + ((bgC * bgA) * (1f - fgA))) / a

/**
 * Returns this color's components as a new array. The last element of the
 * array is always the alpha component.
 *
 * @return A new, non-null array whose size is 4
 */
/*@Size(value = 4)*/
private fun Color.getComponents(): FloatArray = floatArrayOf(red, green, blue, alpha)

/**
 * Returns the relative luminance of this color.
 *
 * Based on the formula for relative luminance defined in WCAG 2.0,
 * W3C Recommendation 11 December 2008.
 *
 * @return A value between 0 (darkest black) and 1 (lightest white)
 *
 * @throws IllegalArgumentException If the this color's color space
 * does not use the [RGB][ColorModel.Rgb] color model
 */
@Stable
fun Color.luminance(): Float {
    val colorSpace = colorSpace
    require(colorSpace.model == ColorModel.Rgb) {
        "The specified color must be encoded in an RGB color space. " +
            "The supplied color space is ${colorSpace.model}"
    }

    val eotf = (colorSpace as Rgb).eotf
    val r = eotf(red.toDouble())
    val g = eotf(green.toDouble())
    val b = eotf(blue.toDouble())

    return saturate(((0.2126 * r) + (0.7152 * g) + (0.0722 * b)).toFloat())
}

private fun saturate(v: Float): Float {
    return if (v <= 0.0f) 0.0f else (if (v >= 1.0f) 1.0f else v)
}

/**
 * Converts this color to an ARGB color int. A color int is always in
 * the [sRGB][ColorSpaces.Srgb] color space. This implies
 * a color space conversion is applied if needed.
 *
 * @return An ARGB color in the sRGB color space
 */
@Stable
/*@ColorInt*/
fun Color.toArgb(): Int {
    val colorSpace = colorSpace
    if (colorSpace.isSrgb) {
        return (this.value shr 32).toInt()
    }

    val color = getComponents()
    // The transformation saturates the output
    colorSpace.connect().transform(color)

    return (color[3] * 255.0f + 0.5f).toInt() shl 24 or
        ((color[0] * 255.0f + 0.5f).toInt() shl 16) or
        ((color[1] * 255.0f + 0.5f).toInt() shl 8) or
        (color[2] * 255.0f + 0.5f).toInt()
}

/**
 * `false` when this is [Color.Unspecified].
 */
@Stable
inline val Color.isSpecified: Boolean get() = value != Color.Unspecified.value

/**
 * `true` when this is [Color.Unspecified].
 */
@Stable
inline val Color.isUnspecified: Boolean get() = value == Color.Unspecified.value

/**
 * If this [Color] [isSpecified] then this is returned, otherwise [block] is executed and its result
 * is returned.
 */
inline fun Color.takeOrElse(block: () -> Color): Color = if (isSpecified) this else block()
