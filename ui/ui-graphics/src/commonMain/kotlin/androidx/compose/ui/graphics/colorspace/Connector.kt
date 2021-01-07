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

/**
 * A connector transforms colors from a source color space to a destination
 * color space.
 *
 * A source color space is connected to a destination color space using the
 * color transform `C` computed from their respective transforms noted
 * `Tsrc` and `Tdst` in the following equation:
 *
 * [See equation](https://developer.android.com/reference/android/graphics/ColorSpace.Connector)
 *
 * The transform `C` shown above is only valid when the source and
 * destination color spaces have the same profile connection space (PCS).
 * We know that instances of [ColorSpace] always use CIE XYZ as their
 * PCS but their white points might differ. When they do, we must perform
 * a chromatic adaptation of the color spaces' transforms. To do so, we
 * use the von Kries method described in the documentation of [Adaptation],
 * using the CIE standard illuminant [D50][Illuminant.D50]
 * as the target white point.
 *
 * Example of conversion from [sRGB][ColorSpaces.Srgb] to
 * [DCI-P3][ColorSpaces.DciP3]:
 *
 *     val connector = ColorSpaces.Srgb.connect(ColorSpaces.DciP3);
 *     val p3 = connector.transform(1.0f, 0.0f, 0.0f);
 *     // p3 contains { 0.9473, 0.2740, 0.2076 }
 *
 * @see Adaptation
 * @see ColorSpace.adapt
 * @see ColorSpace.connect
 */
open class Connector
/**
 * To connect between color spaces, we might need to use adapted transforms.
 * This should be transparent to the user so this constructor takes the
 * original source and destinations (returned by the getters), as well as
 * possibly adapted color spaces used by transform().
 */
internal constructor(
    /**
     * Returns the source color space this connector will convert from.
     *
     * @return A non-null instance of [ColorSpace]
     *
     * @see destination
     */
    val source: ColorSpace,
    /**
     * Returns the destination color space this connector will convert to.
     *
     * @return A non-null instance of [ColorSpace]
     *
     * @see source
     */
    val destination: ColorSpace,
    private val transformSource: ColorSpace,
    private val transformDestination: ColorSpace,
    /**
     * Returns the render intent this connector will use when mapping the
     * source color space to the destination color space.
     *
     * @return A non-null [RenderIntent]
     *
     * @see RenderIntent
     */
    val renderIntent: RenderIntent,
    private val transform: FloatArray?
) {
    /**
     * Creates a new connector between a source and a destination color space.
     *
     * @param source The source color space, cannot be null
     * @param destination The destination color space, cannot be null
     * @param intent The render intent to use when compressing gamuts
     */
    internal constructor(
        source: ColorSpace,
        destination: ColorSpace,
        intent: RenderIntent
    ) : this(
        source, destination,
        if (source.model == ColorModel.Rgb) source.adapt(Illuminant.D50) else source,
        if (destination.model == ColorModel.Rgb) {
            destination.adapt(Illuminant.D50)
        } else {
            destination
        },
        intent,
        computeTransform(
            source,
            destination,
            intent
        )
    )

    /**
     * Transforms the specified color from the source color space
     * to a color in the destination color space. This convenience
     * method assumes a source color model with 3 components
     * (typically RGB). To transform from color models with more than
     * 3 components, such as [CMYK][ColorModel.Cmyk], use
     * [transform] instead.
     *
     * @param r The red component of the color to transform
     * @param g The green component of the color to transform
     * @param b The blue component of the color to transform
     * @return A new array of 3 floats containing the specified color
     * transformed from the source space to the destination space
     *
     * @see transform
     */
    /*@Size(3)*/
    fun transform(r: Float, g: Float, b: Float): FloatArray {
        return transform(floatArrayOf(r, g, b))
    }

    /**
     * Transforms the specified color from the source color space
     * to a color in the destination color space.
     *
     * @param v A non-null array of 3 floats containing the value to transform
     * and that will hold the result of the transform
     * @return The [v] array passed as a parameter, containing the specified color
     * transformed from the source space to the destination space
     *
     * @see transform
     */
    /*@Size(min = 3)*/
    open fun transform(/*@Size(min = 3)*/ v: FloatArray): FloatArray {
        val xyz = transformSource.toXyz(v)
        if (transform != null) {
            xyz[0] *= transform[0]
            xyz[1] *= transform[1]
            xyz[2] *= transform[2]
        }
        return transformDestination.fromXyz(xyz)
    }

    /**
     * Optimized connector for RGB->RGB conversions.
     */
    internal class RgbConnector internal constructor(
        private val mSource: Rgb,
        private val mDestination: Rgb,
        intent: RenderIntent
    ) : Connector(mSource, mDestination, mSource, mDestination, intent, null) {
        private val mTransform: FloatArray

        init {
            mTransform = computeTransform(mSource, mDestination, intent)
        }

        override fun transform(v: FloatArray): FloatArray {
            v[0] = mSource.eotf(v[0].toDouble()).toFloat()
            v[1] = mSource.eotf(v[1].toDouble()).toFloat()
            v[2] = mSource.eotf(v[2].toDouble()).toFloat()
            mul3x3Float3(mTransform, v)
            v[0] = mDestination.oetf(v[0].toDouble()).toFloat()
            v[1] = mDestination.oetf(v[1].toDouble()).toFloat()
            v[2] = mDestination.oetf(v[2].toDouble()).toFloat()
            return v
        }

        /**
         * Computes the color transform that connects two RGB color spaces.
         *
         * We can only connect color spaces if they use the same profile
         * connection space. We assume the connection space is always
         * CIE XYZ but we maye need to perform a chromatic adaptation to
         * match the white points. If an adaptation is needed, we use the
         * CIE standard illuminant D50. The unmatched color space is adapted
         * using the von Kries transform and the [Adaptation.Bradford]
         * matrix.
         *
         * @param source The source color space, cannot be null
         * @param destination The destination color space, cannot be null
         * @param intent The render intent to use when compressing gamuts
         * @return An array of 9 floats containing the 3x3 matrix transform
         */
        private fun computeTransform(
            source: Rgb,
            destination: Rgb,
            intent: RenderIntent
        ): FloatArray {
            if (compare(source.whitePoint, destination.whitePoint)) {
                // RGB->RGB using the PCS of both color spaces since they have the same
                return mul3x3(destination.inverseTransform, source.transform)
            } else {
                // RGB->RGB using CIE XYZ D50 as the PCS
                var transform = source.transform
                var inverseTransform = destination.inverseTransform

                val srcXYZ = source.whitePoint.toXyz()
                val dstXYZ = destination.whitePoint.toXyz()

                if (!compare(source.whitePoint, Illuminant.D50)) {
                    val srcAdaptation = chromaticAdaptation(
                        Adaptation.Bradford.transform,
                        srcXYZ,
                        Illuminant.D50Xyz.copyOf()
                    )
                    transform = mul3x3(srcAdaptation, source.transform)
                }

                if (!compare(destination.whitePoint, Illuminant.D50)) {
                    val dstAdaptation = chromaticAdaptation(
                        Adaptation.Bradford.transform,
                        dstXYZ,
                        Illuminant.D50Xyz.copyOf()
                    )
                    inverseTransform = inverse3x3(
                        mul3x3(
                            dstAdaptation,
                            destination.transform
                        )
                    )
                }

                if (intent == RenderIntent.Absolute) {
                    transform = mul3x3Diag(
                        floatArrayOf(
                            srcXYZ[0] / dstXYZ[0],
                            srcXYZ[1] / dstXYZ[1],
                            srcXYZ[2] / dstXYZ[2]
                        ),
                        transform
                    )
                }

                return mul3x3(inverseTransform, transform)
            }
        }
    }

    internal companion object {
        /**
         * Computes an extra transform to apply in XYZ space depending on the
         * selected rendering intent.
         */
        private fun computeTransform(
            source: ColorSpace,
            destination: ColorSpace,
            intent: RenderIntent
        ): FloatArray? {
            if (intent != RenderIntent.Absolute) return null

            val srcRGB = source.model == ColorModel.Rgb
            val dstRGB = destination.model == ColorModel.Rgb

            if (srcRGB && dstRGB) return null

            if (srcRGB || dstRGB) {
                val rgb = (if (srcRGB) source else destination) as Rgb
                val srcXYZ = if (srcRGB) rgb.whitePoint.toXyz() else Illuminant.D50Xyz
                val dstXYZ = if (dstRGB) rgb.whitePoint.toXyz() else Illuminant.D50Xyz
                return floatArrayOf(
                    srcXYZ[0] / dstXYZ[0],
                    srcXYZ[1] / dstXYZ[1],
                    srcXYZ[2] / dstXYZ[2]
                )
            }

            return null
        }

        /**
         * Returns the identity connector for a given color space.
         *
         * @param source The source and destination color space
         * @return A non-null connector that does not perform any transform
         *
         * @see ColorSpace.connect
         */
        internal fun identity(source: ColorSpace): Connector {
            return object : Connector(source, source, RenderIntent.Relative) {
                override fun transform(v: FloatArray): FloatArray {
                    return v
                }
            }
        }
    }
}