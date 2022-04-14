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

/**
 * Algorithms to use when painting on the canvas.
 *
 * When drawing a shape or image onto a canvas, different algorithms can be
 * used to blend the pixels. The different values of [BlendMode] specify
 * different such algorithms.
 *
 * Each algorithm has two inputs, the _source_, which is the image being drawn,
 * and the _destination_, which is the image into which the source image is
 * being composited. The destination is often thought of as the _background_.
 * The source and destination both have four color channels, the red, green,
 * blue, and alpha channels. These are typically represented as numbers in the
 * range 0.0 to 1.0. The output of the algorithm also has these same four
 * channels, with values computed from the source and destination.
 *
 * The horizontal and vertical bars in these images show the red, green, and
 * blue channels with varying opacity levels, then all three color channels
 * together with those same varying opacity levels, then all three color
 * channels set to zero with those varying opacity levels, then two bars showing
 * a red/green/blue repeating gradient, the first with full opacity and the
 * second with partial opacity, and finally a bar with the three color channels
 * set to zero but the opacity varying in a repeating gradient.
 *
 * ## Application to the [Canvas] API
 *
 * When using [Canvas.saveLayer] and [Canvas.restore], the blend mode of the
 * [Paint] given to the [Canvas.saveLayer] will be applied when
 * [Canvas.restore] is called. Each call to [Canvas.saveLayer] introduces a new
 * layer onto which shapes and images are painted; when [Canvas.restore] is
 * called, that layer is then composited onto the parent layer, with the source
 * being the most-recently-drawn shapes and images, and the destination being
 * the parent layer. (For the first [Canvas.saveLayer] call, the parent layer
 * is the canvas itself.)
 *
 * See also:
 *
 *  * [Paint.blendMode], which uses [BlendMode] to define the compositing
 *    strategy.
 */
@Immutable
@kotlin.jvm.JvmInline
value class BlendMode internal constructor(@Suppress("unused") private val value: Int) {

    companion object {

        /**
         * Drop both the source and destination images, leaving nothing.
         */
        val Clear = BlendMode(0)

        /**
         * Drop the destination image, only paint the source image.
         *
         * Conceptually, the destination is first cleared, then the source image is
         * painted.
         */
        val Src = BlendMode(1)

        /**
         * Drop the source image, only paint the destination image.
         *
         * Conceptually, the source image is discarded, leaving the destination
         * untouched.
         */
        val Dst = BlendMode(2)

        /**
         * Composite the source image over the destination image.
         *
         * This is the default value. It represents the most intuitive case, where
         * shapes are painted on top of what is below, with transparent areas showing
         * the destination layer.
         */
        val SrcOver = BlendMode(3)

        /**
         * Composite the source image under the destination image.
         *
         * This is the opposite of [SrcOver].
         *
         * This is useful when the source image should have been painted before the
         * destination image, but could not be.
         */
        val DstOver = BlendMode(4)

        /**
         * Show the source image, but only where the two images overlap. The
         * destination image is not rendered, it is treated merely as a mask. The
         * color channels of the destination are ignored, only the opacity has an
         * effect.
         *
         * To show the destination image instead, consider [DstIn].
         *
         * To reverse the semantic of the mask (only showing the source where the
         * destination is absent, rather than where it is present), consider
         * [SrcOut].
         */
        val SrcIn = BlendMode(5)

        /**
         * Show the destination image, but only where the two images overlap. The
         * source image is not rendered, it is treated merely as a mask. The color
         * channels of the source are ignored, only the opacity has an effect.
         *
         * To show the source image instead, consider [SrcIn].
         *
         * To reverse the semantic of the mask (only showing the source where the
         * destination is present, rather than where it is absent), consider [DstOut].
         */
        val DstIn = BlendMode(6)

        /**
         * Show the source image, but only where the two images do not overlap. The
         * destination image is not rendered, it is treated merely as a mask. The color
         * channels of the destination are ignored, only the opacity has an effect.
         *
         * To show the destination image instead, consider [DstOut].
         *
         * To reverse the semantic of the mask (only showing the source where the
         * destination is present, rather than where it is absent), consider [SrcIn].
         *
         * This corresponds to the "Source out Destination" Porter-Duff operator.
         */
        val SrcOut = BlendMode(7)

        /**
         * Show the destination image, but only where the two images do not overlap. The
         * source image is not rendered, it is treated merely as a mask. The color
         * channels of the source are ignored, only the opacity has an effect.
         *
         * To show the source image instead, consider [SrcOut].
         *
         * To reverse the semantic of the mask (only showing the destination where the
         * source is present, rather than where it is absent), consider [DstIn].
         *
         * This corresponds to the "Destination out Source" Porter-Duff operator.
         */
        val DstOut = BlendMode(8)

        /**
         * Composite the source image over the destination image, but only where it
         * overlaps the destination.
         *
         * This is essentially the [SrcOver] operator, but with the output's opacity
         * channel being set to that of the destination image instead of being a
         * combination of both image's opacity channels.
         *
         * For a variant with the destination on top instead of the source, see
         * [DstAtop].
         */
        val SrcAtop = BlendMode(9)

        /**
         * Composite the destination image over the source image, but only where it
         * overlaps the source.
         *
         * This is essentially the [DstOver] operator, but with the output's opacity
         * channel being set to that of the source image instead of being a
         * combination of both image's opacity channels.
         *
         * For a variant with the source on top instead of the destination, see
         * [SrcAtop].
         */
        val DstAtop = BlendMode(10)

        /**
         * Apply a bitwise `xor` operator to the source and destination images. This
         * leaves transparency where they would overlap.
         */
        val Xor = BlendMode(11)

        /**
         * Sum the components of the source and destination images.
         *
         * Transparency in a pixel of one of the images reduces the contribution of
         * that image to the corresponding output pixel, as if the color of that
         * pixel in that image was darker.
         *
         */
        val Plus = BlendMode(12)

        /**
         * Multiply the color components of the source and destination images.
         *
         * This can only result in the same or darker colors (multiplying by white,
         * 1.0, results in no change; multiplying by black, 0.0, results in black).
         *
         * When compositing two opaque images, this has similar effect to overlapping
         * two transparencies on a projector.
         *
         * For a variant that also multiplies the alpha channel, consider [Multiply].
         *
         * See also:
         *
         *  * [Screen], which does a similar computation but inverted.
         *  * [Overlay], which combines [Modulate] and [Screen] to favor the
         *    destination image.
         *  * [Hardlight], which combines [Modulate] and [Screen] to favor the
         *    source image.
         */
        val Modulate = BlendMode(13)

        /**
         * Multiply the inverse of the components of the source and destination
         * images, and inverse the result.
         *
         * Inverting the components means that a fully saturated channel (opaque
         * white) is treated as the value 0.0, and values normally treated as 0.0
         * (black, transparent) are treated as 1.0.
         *
         * This is essentially the same as [Modulate] blend mode, but with the values
         * of the colors inverted before the multiplication and the result being
         * inverted back before rendering.
         *
         * This can only result in the same or lighter colors (multiplying by black,
         * 1.0, results in no change; multiplying by white, 0.0, results in white).
         * Similarly, in the alpha channel, it can only result in more opaque colors.
         *
         * This has similar effect to two projectors displaying their images on the
         * same screen simultaneously.
         *
         * See also:
         *
         *  * [Modulate], which does a similar computation but without inverting the
         *    values.
         *  * [Overlay], which combines [Modulate] and [Screen] to favor the
         *    destination image.
         *  * [Hardlight], which combines [Modulate] and [Screen] to favor the
         *    source image.
         */
        val Screen = BlendMode(14) // The last coeff mode.

        /**
         * Multiply the components of the source and destination images after
         * adjusting them to favor the destination.
         *
         * Specifically, if the destination value is smaller, this multiplies it with
         * the source value, whereas is the source value is smaller, it multiplies
         * the inverse of the source value with the inverse of the destination value,
         * then inverts the result.
         *
         * Inverting the components means that a fully saturated channel (opaque
         * white) is treated as the value 0.0, and values normally treated as 0.0
         * (black, transparent) are treated as 1.0.
         *
         * See also:
         *
         *  * [Modulate], which always multiplies the values.
         *  * [Screen], which always multiplies the inverses of the values.
         *  * [Hardlight], which is similar to [Overlay] but favors the source image
         *    instead of the destination image.
         */
        val Overlay = BlendMode(15)

        /**
         * Composite the source and destination image by choosing the lowest value
         * from each color channel.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver].
         */
        val Darken = BlendMode(16)

        /**
         * Composite the source and destination image by choosing the highest value
         * from each color channel.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver].
         */
        val Lighten = BlendMode(17)

        /**
         * Divide the destination by the inverse of the source.
         *
         * Inverting the components means that a fully saturated channel (opaque
         * white) is treated as the value 0.0, and values normally treated as 0.0
         * (black, transparent) are treated as 1.0.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         */
        val ColorDodge = BlendMode(18)

        /**
         * Divide the inverse of the destination by the source, and inverse the result.
         *
         * Inverting the components means that a fully saturated channel (opaque
         * white) is treated as the value 0.0, and values normally treated as 0.0
         * (black, transparent) are treated as 1.0.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         */
        val ColorBurn = BlendMode(19)

        /**
         * Multiply the components of the source and destination images after
         * adjusting them to favor the source.
         *
         * Specifically, if the source value is smaller, this multiplies it with the
         * destination value, whereas is the destination value is smaller, it
         * multiplies the inverse of the destination value with the inverse of the
         * source value, then inverts the result.
         *
         * Inverting the components means that a fully saturated channel (opaque
         * white) is treated as the value 0.0, and values normally treated as 0.0
         * (black, transparent) are treated as 1.0.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * See also:
         *
         *  * [Modulate], which always multiplies the values.
         *  * [Screen], which always multiplies the inverses of the values.
         *  * [Overlay], which is similar to [Hardlight] but favors the destination
         *    image instead of the source image.
         */
        val Hardlight = BlendMode(20)

        /**
         * Use [ColorDodge] for source values below 0.5 and [ColorBurn] for source
         * values above 0.5.
         *
         * This results in a similar but softer effect than [Overlay].
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * See also:
         *
         *  * [Color], which is a more subtle tinting effect.
         */
        val Softlight = BlendMode(21)

        /**
         * Subtract the smaller value from the bigger value for each channel.
         *
         * Compositing black has no effect; compositing white inverts the colors of
         * the other image.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver].
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * The effect is similar to [Exclusion] but harsher.
         */
        val Difference = BlendMode(22)

        /**
         * Subtract double the product of the two images from the sum of the two
         * images.
         *
         * Compositing black has no effect; compositing white inverts the colors of
         * the other image.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver].
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * The effect is similar to [Difference] but softer.
         */
        val Exclusion = BlendMode(23)

        /**
         * Multiply the components of the source and destination images, including
         * the alpha channel.
         *
         * This can only result in the same or darker colors (multiplying by white,
         * 1.0, results in no change; multiplying by black, 0.0, results in black).
         *
         * Since the alpha channel is also multiplied, a fully-transparent pixel
         * (opacity 0.0) in one image results in a fully transparent pixel in the
         * output. This is similar to [DstIn], but with the colors combined.
         *
         * For a variant that multiplies the colors but does not multiply the alpha
         * channel, consider [Modulate].
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         */
        val Multiply = BlendMode(24) // The last separable mode.

        /**
         * Take the hue of the source image, and the saturation and luminosity of the
         * destination image.
         *
         * The effect is to tint the destination image with the source image.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver]. Regions that are entirely transparent in the source image take
         * their hue from the destination.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         */
        val Hue = BlendMode(25)

        /**
         * Take the saturation of the source image, and the hue and luminosity of the
         * destination image.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver]. Regions that are entirely transparent in the source image take
         * their saturation from the destination.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * See also:
         *
         *  * [Color], which also applies the hue of the source image.
         *  * [Luminosity], which applies the luminosity of the source image to the
         *    destination.
         */
        val Saturation = BlendMode(26)

        /**
         * Take the hue and saturation of the source image, and the luminosity of the
         * destination image.
         *
         * The effect is to tint the destination image with the source image.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver]. Regions that are entirely transparent in the source image take
         * their hue and saturation from the destination.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * See also:
         *
         *  * [Hue], which is a similar but weaker effect.
         *  * [Softlight], which is a similar tinting effect but also tints white.
         *  * [Saturation], which only applies the saturation of the source image.
         */
        val Color = BlendMode(27)

        /**
         * Take the luminosity of the source image, and the hue and saturation of the
         * destination image.
         *
         * The opacity of the output image is computed in the same way as for
         * [SrcOver]. Regions that are entirely transparent in the source image take
         * their luminosity from the destination.
         *
         * **NOTE** This [BlendMode] can only be used on Android API level 29 and above
         *
         * See also:
         *
         *  * [Saturation], which applies the saturation of the source image to the
         *    destination.
         */
        val Luminosity = BlendMode(28)
    }

    override fun toString() = when (this) {
        Clear -> "Clear"
        Src -> "Src"
        Dst -> "Dst"
        SrcOver -> "SrcOver"
        DstOver -> "DstOver"
        SrcIn -> "SrcIn"
        DstIn -> "DstIn"
        SrcOut -> "SrcOut"
        DstOut -> "DstOut"
        SrcAtop -> "SrcAtop"
        DstAtop -> "DstAtop"
        Xor -> "Xor"
        Plus -> "Plus"
        Modulate -> "Modulate"
        Screen -> "Screen"
        Overlay -> "Overlay"
        Darken -> "Darken"
        Lighten -> "Lighten"
        ColorDodge -> "ColorDodge"
        ColorBurn -> "ColorBurn"
        Hardlight -> "HardLight"
        Softlight -> "Softlight"
        Difference -> "Difference"
        Exclusion -> "Exclusion"
        Multiply -> "Multiply"
        Hue -> "Hue"
        Saturation -> "Saturation"
        Color -> "Color"
        Luminosity -> "Luminosity"
        else -> "Unknown" // Should not get here since we have an internal constructor
    }
}

/**
 * Capability query to determine if the particular platform supports the [BlendMode]. Not
 * all platforms support all blend mode algorithms, however, [BlendMode.SrcOver] is guaranteed
 * to be supported as it is the default drawing algorithm. If a [BlendMode] that is not supported
 * is used, the default of SrcOver is consumed instead.
 */
expect fun BlendMode.isSupported(): Boolean