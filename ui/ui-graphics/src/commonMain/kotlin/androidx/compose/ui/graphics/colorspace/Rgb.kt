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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.packFloats
import kotlin.math.abs
import kotlin.math.pow

/**
 * An RGB color space is an additive color space using the
 * [RGB][ColorModel.Rgb] color model (a color is therefore represented
 * by a tuple of 3 numbers).
 *
 * A specific RGB color space is defined by the following properties:
 *
 *  * Three chromaticities of the red, green and blue primaries, which
 * define the gamut of the color space.
 *  * A white point chromaticity that defines the stimulus to which
 * color space values are normalized (also just called "white").
 *  * An opto-electronic transfer function, also called opto-electronic
 * conversion function or often, and approximately, gamma function.
 *  * An electro-optical transfer function, also called electo-optical
 * conversion function or often, and approximately, gamma function.
 *  * A range of valid RGB values (most commonly `[0..1]`).
 *
 * The most commonly used RGB color space is [sRGB][ColorSpaces.Srgb].
 *
 * ### Primaries and white point chromaticities
 *
 * In this implementation, the chromaticity of the primaries and the white
 * point of an RGB color space is defined in the CIE xyY color space. This
 * color space separates the chromaticity of a color, the x and y components,
 * and its luminance, the Y component. Since the primaries and the white
 * point have full brightness, the Y component is assumed to be 1 and only
 * the x and y components are needed to encode them.
 *
 * For convenience, this implementation also allows to define the
 * primaries and white point in the CIE XYZ space. The tristimulus XYZ values
 * are internally converted to xyY.
 *
 * [sRGB primaries and white point](https://developer.android.com/reference/android/images/graphics/colorspace_srgb.png)
 *
 * ### Transfer functions
 *
 * A transfer function is a color component conversion function, defined as
 * a single variable, monotonic mathematical function. It is applied to each
 * individual component of a color. They are used to perform the mapping
 * between linear tristimulus values and non-linear electronic signal value.
 *
 * The *opto-electronic transfer function* (OETF or OECF) encodes
 * tristimulus values in a scene to a non-linear electronic signal value.
 * An OETF is often expressed as a power function with an exponent between
 * 0.38 and 0.55 (the reciprocal of 1.8 to 2.6).
 *
 * The *electro-optical transfer function* (EOTF or EOCF) decodes
 * a non-linear electronic signal value to a tristimulus value at the display.
 * An EOTF is often expressed as a power function with an exponent between
 * 1.8 and 2.6.
 *
 * Transfer functions are used as a compression scheme. For instance,
 * linear sRGB values would normally require 11 to 12 bits of precision to
 * store all values that can be perceived by the human eye. When encoding
 * sRGB values using the appropriate OETF (see [sRGB][ColorSpaces.Srgb] for
 * an exact mathematical description of that OETF), the values can be
 * compressed to only 8 bits precision.
 *
 * When manipulating RGB values, particularly sRGB values, it is safe
 * to assume that these values have been encoded with the appropriate
 * OETF (unless noted otherwise). Encoded values are often said to be in
 * "gamma space". They are therefore defined in a non-linear space. This
 * in turns means that any linear operation applied to these values is
 * going to yield mathematically incorrect results (any linear interpolation
 * such as gradient generation for instance, most image processing functions
 * such as blurs, etc.).
 *
 * To properly process encoded RGB values you must first apply the
 * EOTF to decode the value into linear space. After processing, the RGB
 * value must be encoded back to non-linear ("gamma") space. Here is a
 * formal description of the process, where `f` is the processing
 * function to apply:
 *
 * [See RGB equation](https://developer.android.com/reference/android/graphics/ColorSpace.Rgb)
 *
 * If the transfer functions of the color space can be expressed as an
 * ICC parametric curve as defined in ICC.1:2004-10, the numeric parameters
 * can be retrieved from [transferParameters]. This can
 * be useful to match color spaces for instance.
 *
 * Some RGB color spaces, such as [ColorSpaces.Aces] and
 * [scRGB][ColorSpaces.LinearExtendedSrgb], are said to be linear because
 * their transfer functions are the identity function: `f(x) = x`.
 * If the source and/or destination are known to be linear, it is not
 * necessary to invoke the transfer functions.
 *
 * ### Range
 *
 * Most RGB color spaces allow RGB values in the range `[0..1]`. There
 * are however a few RGB color spaces that allow much larger ranges. For
 * instance, [scRGB][ColorSpaces.ExtendedSrgb] is used to manipulate the
 * range `[-0.5..7.5]` while [ACES][ColorSpaces.Aces] can be used throughout
 * the range `[-65504, 65504]`.
 *
 * [Extended sRGB and its large range](https://developer.android.com/reference/android/images/graphics/colorspace_scrgb.png)
 *
 * ### Converting between RGB color spaces
 *
 * Conversion between two color spaces is achieved by using an intermediate
 * color space called the profile connection space (PCS). The PCS used by
 * this implementation is CIE XYZ. The conversion operation is defined
 * as such:
 *
 * [See RGB equation](https://developer.android.com/reference/android/graphics/ColorSpace.Rgb)
 *
 * Where `Tsrc` is the [RGB to XYZ transform][getTransform]
 * of the source color space and `Tdst^-1` the
 * [XYZ to RGB transform][getInverseTransform] of the destination color space.
 *
 * Many RGB color spaces commonly used with electronic devices use the
 * standard illuminant [D65][Illuminant.D65]. Care must be take however
 * when converting between two RGB color spaces if their white points do not
 * match. This can be achieved by either calling
 * [adapt] to adapt one or both color spaces to
 * a single common white point. This can be achieved automatically by calling
 * [ColorSpace.connect], which also handles
 * non-RGB color spaces.
 *
 * To learn more about the white point adaptation process, refer to the
 * documentation of [Adaptation].
 */
class Rgb
/**
 * Creates a new RGB color space using a specified set of primaries
 * and a specified white point.
 *
 * The primaries and white point can be specified in the CIE xyY space
 * or in CIE XYZ. The length of the arrays depends on the chosen space:
 *
 * ```
 * | Spaces | Primaries length | White point length |
 * |--------|------------------|--------------------|
 * | xyY    | 6                | 2                  |
 * | XYZ    | 9                | 3                  |
 * ```
 *
 * When the primaries and/or white point are specified in xyY, the Y component
 * does not need to be specified and is assumed to be 1.0. Only the xy components
 * are required.
 *
 * @param name Name of the color space, cannot be null, its length must be >= 1
 * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
 * @param whitePoint Reference white as a [WhitePoint]
 * @param transform Computed transform matrix that converts from RGB to XYZ, or
 * `null` to compute it from `primaries` and `whitePoint`.
 * @param oetf Opto-electronic transfer function, cannot be null
 * @param eotf Electro-optical transfer function, cannot be null
 * @param min The minimum valid value in this color space's RGB range
 * @param max The maximum valid value in this color space's RGB range
 * @param transferParameters Parameters for the transfer functions
 * @param id ID of this color space as an integer between [ColorSpace.MinId] and
 * [ColorSpace.MaxId]
 *
 * @throws IllegalArgumentException If any of the following conditions is met:
 *  * The name is null or has a length of 0.
 *  * The primaries array is null or has a length that is neither 6 or 9.
 *  * The white point array is null or has a length that is neither 2 or 3.
 *  * The OETF is null or the EOTF is null.
 *  * The minimum valid value is >= the maximum valid value.
 *  * The ID is not between [ColorSpace.MinId] and [ColorSpace.MaxId].
 */
internal constructor(
    name: String,
    primaries: FloatArray,
    val whitePoint: WhitePoint,
    transform: FloatArray?,
    oetf: DoubleFunction,
    eotf: DoubleFunction,
    private val min: Float,
    private val max: Float,
    /**
     * Returns the parameters used by the [electro-optical][eotf]
     * and [opto-electronic][oetf] transfer functions. If the transfer
     * functions do not match the ICC parametric curves defined in ICC.1:2004-10
     * (section 10.15), this method returns null.
     *
     * See [TransferParameters] for a full description of the transfer
     * functions.
     *
     * @return An instance of [TransferParameters] or null if this color
     * space's transfer functions do not match the equation defined in
     * [TransferParameters]
     */
    val transferParameters: TransferParameters?,
    id: Int
) : ColorSpace(name, ColorModel.Rgb, id) {

    internal val primaries: FloatArray
    internal val transform: FloatArray
    internal val inverseTransform: FloatArray
    internal val oetfOrig = oetf

    /**
     * Returns the opto-electronic transfer function (OETF) of this color space.
     * The inverse function is the electro-optical transfer function (EOTF) returned
     * by [eotf]. These functions are defined to satisfy the following
     * equality for x ∈ `[0..1]`:
     *
     *     OETF(EOTF(x) = EOTF(OETF(x)) = x
     *
     * For RGB colors, this function can be used to convert from linear space
     * to "gamma space" (gamma encoded). The terms gamma space and gamma encoded
     * are frequently used because many OETFs can be closely approximated using
     * a simple power function of the form x^γ (the
     * approximation of the [sRGB][ColorSpaces.Srgb] OETF uses γ = 2.2
     * for instance).
     *
     * @return A transfer function that converts from linear space to "gamma space"
     *
     * @see eotf
     * @see Rgb.transferParameters
     */
    val oetf: (Double) -> Double = { x ->
        oetfOrig(x).coerceIn(min.toDouble(), max.toDouble())
    }

    internal val oetfFunc: DoubleFunction = DoubleFunction { x ->
        oetfOrig(x).coerceIn(min.toDouble(), max.toDouble())
    }

    internal val eotfOrig = eotf

    /**
     * Returns the electro-optical transfer function (EOTF) of this color space.
     * The inverse function is the opto-electronic transfer function (OETF)
     * returned by [oetf]. These functions are defined to satisfy the
     * following equality for x in `[0..1]`:
     *
     *     OETF(EOTF(x) = EOTF(OETF(x)) = x
     *
     * For RGB colors, this function can be used to convert from "gamma space"
     * (gamma encoded) to linear space. The terms gamma space and gamma encoded
     * are frequently used because many EOTFs can be closely approximated using
     * a simple power function of the form x^γ (the approximation of the
     * [sRGB][ColorSpaces.Srgb] EOTF uses γ = 2.2 for instance).
     *
     * @return A transfer function that converts from "gamma space" to linear space
     *
     * @see oetf
     * @see Rgb.transferParameters
     */
    val eotf: (Double) -> Double = { x ->
        eotfOrig(x.coerceIn(min.toDouble(), max.toDouble()))
    }

    internal val eotfFunc = DoubleFunction { x ->
        eotfOrig(x.coerceIn(min.toDouble(), max.toDouble()))
    }

    override val isWideGamut: Boolean
    override val isSrgb: Boolean

    init {
        if (primaries.size != 6 && primaries.size != 9) {
            throw IllegalArgumentException(
                (
                    "The color space's primaries must be " +
                        "defined as an array of 6 floats in xyY or 9 floats in XYZ"
                    )
            )
        }

        if (min >= max) {
            throw IllegalArgumentException(
                "Invalid range: min=$min, max=$max; min must " +
                    "be strictly < max"
            )
        }
        this.primaries = xyPrimaries(primaries)

        if (transform == null) {
            this.transform = computeXYZMatrix(this.primaries, this.whitePoint)
        } else {
            if (transform.size != 9) {
                throw IllegalArgumentException(
                    (
                        "Transform must have 9 entries! Has " +
                            "${transform.size}"
                        )
                )
            }
            this.transform = transform
        }
        inverseTransform = inverse3x3(this.transform)

        // A color space is wide-gamut if its area is >90% of NTSC 1953 and
        // if it entirely contains the Color space definition in xyY
        isWideGamut = isWideGamut(this.primaries, min, max)
        isSrgb = isSrgb(this.primaries, this.whitePoint, oetf, eotf, min, max, id)
    }

    /**
     * Returns the primaries of this color space as a new array of 6 floats.
     * The Y component is assumed to be 1 and is therefore not copied into
     * the destination. The x and y components of the first primary are
     * written in the array at positions 0 and 1 respectively.
     *
     * @return A new non-null array of 2 floats
     *
     * @see whitePoint
     */
    /*@Size(6)*/
    fun getPrimaries(): FloatArray = primaries.copyOf()

    /**
     * Returns the transform of this color space as a new array. The
     * transform is used to convert from RGB to XYZ (with the same white
     * point as this color space). To connect color spaces, you must first
     * [adapt][ColorSpace.adapt] them to the
     * same white point.
     *
     * It is recommended to use [ColorSpace.connect]
     * to convert between color spaces.
     *
     * @return A new array of 9 floats
     *
     * @see getInverseTransform
     */
    /*@Size(9)*/
    fun getTransform(): FloatArray = transform.copyOf()

    /**
     * Returns the inverse transform of this color space as a new array.
     * The inverse transform is used to convert from XYZ to RGB (with the
     * same white point as this color space). To connect color spaces, you
     * must first [adapt][ColorSpace.adapt] them
     * to the same white point.
     *
     * It is recommended to use [ColorSpace.connect]
     * to convert between color spaces.
     *
     * @return A new array of 9 floats
     *
     * @see getTransform
     */
    /*@Size(9)*/
    fun getInverseTransform(): FloatArray = inverseTransform.copyOf()

    /**
     * Creates a new RGB color space using a 3x3 column-major transform matrix.
     * The transform matrix must convert from the RGB space to the profile connection
     * space CIE XYZ.
     *
     * The range of the color space is imposed to be `[0..1]`.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param toXYZ 3x3 column-major transform matrix from RGB to the profile
     * connection space CIE XYZ as an array of 9 floats, cannot be null
     * @param oetf Opto-electronic transfer function, cannot be null
     * @param eotf Electro-optical transfer function, cannot be null
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The OETF is null or the EOTF is null.
     *  * The minimum valid value is >= the maximum valid value.
     */
    constructor(
        /*@Size(min = 1)*/
        name: String,
        /*@Size(9)*/
        toXYZ: FloatArray,
        oetf: (Double) -> Double,
        eotf: (Double) -> Double
    ) : this(
        name,
        computePrimaries(toXYZ),
        computeWhitePoint(toXYZ),
        null,
        DoubleFunction { x -> oetf(x) },
        DoubleFunction { x -> eotf(x) },
        0.0f,
        1.0f,
        null,
        MinId
    )

    /**
     * Creates a new RGB color space using a specified set of primaries
     * and a specified white point.
     *
     * The primaries and white point can be specified in the CIE xyY space
     * or in CIE XYZ. The length of the arrays depends on the chosen space:
     *
     * ```
     * | Spaces | Primaries length | White point length |
     * |--------|------------------|--------------------|
     * | xyY    | 6                | 2                  |
     * | XYZ    | 9                | 3                  |
     * ```
     *
     * When the primaries and/or white point are specified in xyY, the Y component
     * does not need to be specified and is assumed to be 1.0. Only the xy components
     * are required.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
     * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
     * @param oetf Opto-electronic transfer function, cannot be null
     * @param eotf Electro-optical transfer function, cannot be null
     * @param min The minimum valid value in this color space's RGB range
     * @param max The maximum valid value in this color space's RGB range
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The primaries array is null or has a length that is neither 6 or 9.
     *  * The white point array is null or has a length that is neither 2 or 3.
     *  * The OETF is null or the EOTF is null.
     *  * The minimum valid value is >= the maximum valid value.
     */
    constructor(
        /*@Size(min = 1)*/
        name: String,
        /*@Size(min = 6, max = 9)*/
        primaries: FloatArray,
        whitePoint: WhitePoint,
        oetf: (Double) -> Double,
        eotf: (Double) -> Double,
        min: Float,
        max: Float
    ) : this(
        name,
        primaries,
        whitePoint,
        null,
        DoubleFunction { x -> oetf(x) },
        DoubleFunction { x -> eotf(x) },
        min,
        max,
        null,
        MinId
    )

    /**
     * Creates a new RGB color space using a 3x3 column-major transform matrix.
     * The transform matrix must convert from the RGB space to the profile connection
     * space CIE XYZ.
     *
     * The range of the color space is imposed to be `[0..1]`.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param toXYZ 3x3 column-major transform matrix from RGB to the profile
     * connection space CIE XYZ as an array of 9 floats, cannot be null
     * @param function Parameters for the transfer functions
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * Gamma is negative.
     */
    constructor(
        /*@Size(min = 1)*/
        name: String,
        /*@Size(9)*/
        toXYZ: FloatArray,
        function: TransferParameters
    ) : this(name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), function, MinId)

    /**
     * Creates a new RGB color space using a specified set of primaries
     * and a specified white point.
     *
     * The primaries and white point can be specified in the CIE xyY space
     * or in CIE XYZ. The length of the arrays depends on the chosen space:
     *
     * ```
     * | Spaces | Primaries length | White point length |
     * |--------|------------------|--------------------|
     * | xyY    | 6                | 2                  |
     * | XYZ    | 9                | 3                  |
     * ```
     *
     * When the primaries and/or white point are specified in xyY, the Y component
     * does not need to be specified and is assumed to be 1.0. Only the xy components
     * are required.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
     * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
     * @param function Parameters for the transfer functions
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The primaries array is null or has a length that is neither 6 or 9.
     *  * The white point array is null or has a length that is neither 2 or 3.
     *  * The transfer parameters are invalid.
     */
    constructor(
        /*@Size(min = 1)*/
        name: String,
        /*@Size(min = 6, max = 9)*/
        primaries: FloatArray,
        whitePoint: WhitePoint,
        function: TransferParameters
    ) : this(name, primaries, whitePoint, function, MinId)

    /**
     * Creates a new RGB color space using a specified set of primaries
     * and a specified white point.
     *
     * The primaries and white point can be specified in the CIE xyY space
     * or in CIE XYZ. The length of the arrays depends on the chosen space:
     *
     * ```
     * | Spaces | Primaries length | White point length |
     * |--------|------------------|--------------------|
     * | xyY    | 6                | 2                  |
     * | XYZ    | 9                | 3                  |
     * ```
     *
     * When the primaries and/or white point are specified in xyY, the Y component
     * does not need to be specified and is assumed to be 1.0. Only the xy components
     * are required.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
     * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
     * @param function Parameters for the transfer functions
     * @param id ID of this color space as an integer between [ColorSpace.MinId] and
     * [ColorSpace.MaxId]
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The primaries array is null or has a length that is neither 6 or 9.
     *  * The white point array is null or has a length that is neither 2 or 3.
     *  * The ID is not between [ColorSpace.MinId] and [ColorSpace.MaxId].
     *  * The transfer parameters are invalid.
     *
     * @see get
     */
    internal constructor(
        name: String,
        primaries: FloatArray,
        whitePoint: WhitePoint,
        function: TransferParameters,
        id: Int
    ) : this(
        name, primaries, whitePoint, null,
        if (function.e == 0.0 && function.f == 0.0) DoubleFunction { x ->
            rcpResponse(
                x,
                function.a,
                function.b,
                function.c,
                function.d,
                function.gamma
            )
        } else DoubleFunction { x ->
            rcpResponse(
                x, function.a, function.b, function.c, function.d, function.e,
                function.f, function.gamma
            )
        },
        if (function.e == 0.0 && function.f == 0.0) DoubleFunction { x ->
            response(
                x,
                function.a,
                function.b,
                function.c,
                function.d,
                function.gamma
            )
        } else DoubleFunction { x ->
            response(
                x, function.a, function.b, function.c, function.d, function.e,
                function.f, function.gamma
            )
        },
        0.0f, 1.0f, function, id
    )

    /**
     * Creates a new RGB color space using a 3x3 column-major transform matrix.
     * The transform matrix must convert from the RGB space to the profile connection
     * space CIE XYZ.
     *
     * The range of the color space is imposed to be `[0..1]`.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param toXYZ 3x3 column-major transform matrix from RGB to the profile
     * connection space CIE XYZ as an array of 9 floats, cannot be null
     * @param gamma Gamma to use as the transfer function
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * Gamma is negative.
     *
     * @see get
     */
    constructor(
        /*@Size(min = 1)*/
        name: String,
        /*@Size(9)*/
        toXYZ: FloatArray,
        gamma: Double
    ) : this(
        name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), gamma, 0.0f, 1.0f,
        MinId
    )

    /**
     * Creates a new RGB color space using a specified set of primaries
     * and a specified white point.
     *
     * The primaries and white point can be specified in the CIE xyY space
     * or in CIE XYZ. The length of the arrays depends on the chosen space:
     *
     * ```
     * | Spaces | Primaries length | White point length |
     * |--------|------------------|--------------------|
     * | xyY    | 6                | 2                  |
     * | XYZ    | 9                | 3                  |
     * ```
     *
     * When the primaries and/or white point are specified in xyY, the Y component
     * does not need to be specified and is assumed to be 1.0. Only the xy components
     * are required.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
     * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
     * @param gamma Gamma to use as the transfer function
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The primaries array is null or has a length that is neither 6 or 9.
     *  * The white point array is null or has a length that is neither 2 or 3.
     *  * Gamma is negative.
     *
     * @see get
     */
    constructor(
        /*@Size(min = 1)*/
        name: String,
        /*@Size(min = 6, max = 9)*/
        primaries: FloatArray,
        whitePoint: WhitePoint,
        gamma: Double
    ) : this(name, primaries, whitePoint, gamma, 0.0f, 1.0f, MinId)

    /**
     * Creates a new RGB color space using a specified set of primaries
     * and a specified white point.
     *
     * The primaries and white point can be specified in the CIE xyY space
     * or in CIE XYZ. The length of the arrays depends on the chosen space:
     *
     * ```
     * | Spaces | Primaries length | White point length |
     * |--------|------------------|--------------------|
     * | xyY    | 6                | 2                  |
     * | XYZ    | 9                | 3                  |
     * ```
     *
     * When the primaries and/or white point are specified in xyY, the Y component
     * does not need to be specified and is assumed to be 1.0. Only the xy components
     * are required.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
     * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
     * @param gamma Gamma to use as the transfer function
     * @param min The minimum valid value in this color space's RGB range
     * @param max The maximum valid value in this color space's RGB range
     * @param id ID of this color space as an integer between [ColorSpace.MinId] and
     * [ColorSpace.MaxId]
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The primaries array is null or has a length that is neither 6 or 9.
     *  * The white point array is null or has a length that is neither 2 or 3.
     *  * The minimum valid value is >= the maximum valid value.
     *  * The ID is not between [ColorSpace.MinId] and [ColorSpace.MaxId].
     *  * Gamma is negative.
     *
     * @see get
     */
    internal constructor(
        name: String,
        primaries: FloatArray,
        whitePoint: WhitePoint,
        gamma: Double,
        min: Float,
        max: Float,
        id: Int
    ) : this(
        name, primaries, whitePoint, null,
        if (gamma == 1.0) DoubleIdentity
        else DoubleFunction { x -> (if (x < 0.0) 0.0 else x).pow(1.0 / gamma) },
        if (gamma == 1.0) DoubleIdentity
        else DoubleFunction { x -> (if (x < 0.0) 0.0 else x).pow(gamma) },
        min,
        max,
        TransferParameters(gamma, 1.0, 0.0, 0.0, 0.0),
        id
    )

    /**
     * Creates a copy of the specified color space with a new transform.
     *
     * @param colorSpace The color space to create a copy of
     */
    internal constructor(
        colorSpace: Rgb,
        transform: FloatArray,
        whitePoint: WhitePoint
    ) : this(
        colorSpace.name, colorSpace.primaries, whitePoint, transform,
        colorSpace.oetfOrig, colorSpace.eotfOrig, colorSpace.min, colorSpace.max,
        colorSpace.transferParameters,
        MinId
    )

    /**
     * Copies the primaries of this color space in specified array. The Y
     * component is assumed to be 1 and is therefore not copied into the
     * destination. The x and y components of the first primary are written
     * in the array at positions 0 and 1 respectively.
     *
     * @param primaries The destination array, cannot be null, its length
     * must be >= 6
     *
     * @return [primaries] array, modified to contain the primaries of this color space.
     *
     * @see getPrimaries
     */
    /*@Size(min = 6)*/
    fun getPrimaries(/*@Size(min = 6)*/ primaries: FloatArray): FloatArray {
        return this.primaries.copyInto(primaries)
    }

    /**
     * Copies the transform of this color space in specified array. The
     * transform is used to convert from RGB to XYZ (with the same white
     * point as this color space). To connect color spaces, you must first
     * [adapt][ColorSpace.adapt] them to the
     * same white point.
     *
     * It is recommended to use [ColorSpace.connect]
     * to convert between color spaces.
     *
     * @param transform The destination array, cannot be null, its length
     * must be >= 9
     *
     * @return [transform], modified to contain the transform for this color space.
     *
     * @see getInverseTransform
     */
    /*@Size(min = 9)*/
    fun getTransform(/*@Size(min = 9)*/ transform: FloatArray): FloatArray {
        return this.transform.copyInto(transform)
    }

    /**
     * Copies the inverse transform of this color space in specified array.
     * The inverse transform is used to convert from XYZ to RGB (with the
     * same white point as this color space). To connect color spaces, you
     * must first [adapt][ColorSpace.adapt] them
     * to the same white point.
     *
     * It is recommended to use [ColorSpace.connect]
     * to convert between color spaces.
     *
     * @param inverseTransform The destination array, cannot be null, its length
     * must be >= 9
     *
     * @return The [inverseTransform] array passed as a parameter, modified to contain the
     * inverse transform of this color space.
     *
     * @see getTransform
     */
    /*@Size(min = 9)*/
    fun getInverseTransform(/*@Size(min = 9)*/ inverseTransform: FloatArray): FloatArray {
        return this.inverseTransform.copyInto(inverseTransform)
    }

    override fun getMinValue(component: Int): Float {
        return min
    }

    override fun getMaxValue(component: Int): Float {
        return max
    }

    /**
     * Decodes an RGB value to linear space. This is achieved by
     * applying this color space's electro-optical transfer function
     * to the supplied values.
     *
     * Refer to the documentation of [Rgb] for
     * more information about transfer functions and their use for
     * encoding and decoding RGB values.
     *
     * @param r The red component to decode to linear space
     * @param g The green component to decode to linear space
     * @param b The blue component to decode to linear space
     * @return A new array of 3 floats containing linear RGB values
     *
     * @see toLinear
     * @see fromLinear
     */
    /*@Size(3)*/
    fun toLinear(r: Float, g: Float, b: Float): FloatArray {
        return toLinear(floatArrayOf(r, g, b))
    }

    /**
     * Decodes an RGB value to linear space. This is achieved by
     * applying this color space's electro-optical transfer function
     * to the first 3 values of the supplied array. The result is
     * stored back in the input array.
     *
     * Refer to the documentation of [Rgb] for
     * more information about transfer functions and their use for
     * encoding and decoding RGB values.
     *
     * @param v A non-null array of non-linear RGB values, its length
     * must be at least 3
     * @return [v], containing linear RGB values
     *
     * @see toLinear
     * @see fromLinear
     */
    /*@Size(min = 3)*/
    fun toLinear(/*@Size(min = 3)*/ v: FloatArray): FloatArray {
        v[0] = eotfFunc(v[0].toDouble()).toFloat()
        v[1] = eotfFunc(v[1].toDouble()).toFloat()
        v[2] = eotfFunc(v[2].toDouble()).toFloat()
        return v
    }

    /**
     * Encodes an RGB value from linear space to this color space's
     * "gamma space". This is achieved by applying this color space's
     * opto-electronic transfer function to the supplied values.
     *
     * Refer to the documentation of [Rgb] for
     * more information about transfer functions and their use for
     * encoding and decoding RGB values.
     *
     * @param r The red component to encode from linear space
     * @param g The green component to encode from linear space
     * @param b The blue component to encode from linear space
     * @return A new array of 3 floats containing non-linear RGB values
     *
     * @see fromLinear
     * @see toLinear
     */
    /*@Size(3)*/
    fun fromLinear(r: Float, g: Float, b: Float): FloatArray {
        return fromLinear(floatArrayOf(r, g, b))
    }

    /**
     * Encodes an RGB value from linear space to this color space's
     * "gamma space". This is achieved by applying this color space's
     * opto-electronic transfer function to the first 3 values of the
     * supplied array. The result is stored back in the input array.
     *
     * Refer to the documentation of [Rgb] for
     * more information about transfer functions and their use for
     * encoding and decoding RGB values.
     *
     * @param v A non-null array of linear RGB values, its length
     * must be at least 3
     * @return [v], containing non-linear RGB values
     *
     * @see fromLinear
     * @see toLinear
     */
    /*@Size(min = 3)*/
    fun fromLinear(/*@Size(min = 3) */v: FloatArray): FloatArray {
        v[0] = oetfFunc(v[0].toDouble()).toFloat()
        v[1] = oetfFunc(v[1].toDouble()).toFloat()
        v[2] = oetfFunc(v[2].toDouble()).toFloat()
        return v
    }

    override fun toXyz(v: FloatArray): FloatArray {
        v[0] = eotfFunc(v[0].toDouble()).toFloat()
        v[1] = eotfFunc(v[1].toDouble()).toFloat()
        v[2] = eotfFunc(v[2].toDouble()).toFloat()
        return mul3x3Float3(transform, v)
    }

    override fun toXy(v0: Float, v1: Float, v2: Float): Long {
        val v00 = eotfFunc(v0.toDouble()).toFloat()
        val v10 = eotfFunc(v1.toDouble()).toFloat()
        val v20 = eotfFunc(v2.toDouble()).toFloat()

        val x = mul3x3Float3_0(transform, v00, v10, v20)
        val y = mul3x3Float3_1(transform, v00, v10, v20)

        return packFloats(x, y)
    }

    override fun toZ(v0: Float, v1: Float, v2: Float): Float {
        val v00 = eotfFunc(v0.toDouble()).toFloat()
        val v10 = eotfFunc(v1.toDouble()).toFloat()
        val v20 = eotfFunc(v2.toDouble()).toFloat()

        val z = mul3x3Float3_2(transform, v00, v10, v20)

        return z
    }

    override fun xyzaToColor(
        x: Float,
        y: Float,
        z: Float,
        a: Float,
        colorSpace: ColorSpace
    ): Color {
        var v0 = mul3x3Float3_0(inverseTransform, x, y, z)
        var v1 = mul3x3Float3_1(inverseTransform, x, y, z)
        var v2 = mul3x3Float3_2(inverseTransform, x, y, z)

        v0 = oetfFunc(v0.toDouble()).toFloat()
        v1 = oetfFunc(v1.toDouble()).toFloat()
        v2 = oetfFunc(v2.toDouble()).toFloat()

        return Color(v0, v1, v2, a, colorSpace)
    }

    override fun fromXyz(v: FloatArray): FloatArray {
        mul3x3Float3(inverseTransform, v)
        v[0] = oetfFunc(v[0].toDouble()).toFloat()
        v[1] = oetfFunc(v[1].toDouble()).toFloat()
        v[2] = oetfFunc(v[2].toDouble()).toFloat()
        return v
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        val rgb = other as Rgb

        if (rgb.min.compareTo(min) != 0) return false
        if (rgb.max.compareTo(max) != 0) return false
        if (whitePoint != rgb.whitePoint) return false
        if (!(primaries contentEquals rgb.primaries)) return false
        if (transferParameters != null) {
            return transferParameters == rgb.transferParameters
        } else if (rgb.transferParameters == null) {
            return true
        }

        return if (oetfOrig != rgb.oetfOrig) false else eotfOrig == rgb.eotfOrig
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + whitePoint.hashCode()
        result = 31 * result + primaries.contentHashCode()
        result = 31 * result + (if (min != +0.0f) min.toBits() else 0)
        result = 31 * result + (if (max != +0.0f) max.toBits() else 0)
        result = (
            31 * result +
                if (transferParameters != null) transferParameters.hashCode() else 0
            )
        if (transferParameters == null) {
            result = 31 * result + oetfOrig.hashCode()
            result = 31 * result + eotfOrig.hashCode()
        }
        return result
    }

    // internal so that current.txt doesn't expose it: a 'private' companion object
    // is marked deprecated
    internal companion object {
        private val DoubleIdentity = DoubleFunction { d -> d }

        /**
         * Computes whether a color space is the sRGB color space or at least
         * a close approximation.
         *
         * @param primaries The set of RGB primaries in xyY as an array of 6 floats
         * @param whitePoint The white point in xyY as an array of 2 floats
         * @param OETF The opto-electronic transfer function
         * @param EOTF The electro-optical transfer function
         * @param min The minimum value of the color space's range
         * @param max The minimum value of the color space's range
         * @param id The ID of the color space
         * @return True if the color space can be considered as the sRGB color space
         *
         * @see isSrgb
         */
        private fun isSrgb(
            primaries: FloatArray,
            whitePoint: WhitePoint,
            OETF: DoubleFunction,
            EOTF: DoubleFunction,
            min: Float,
            max: Float,
            id: Int
        ): Boolean {
            if (id == 0) return true
            if (!compare(primaries, ColorSpaces.SrgbPrimaries)) {
                return false
            }
            if (!compare(whitePoint, Illuminant.D65)) {
                return false
            }

            if (min != 0.0f) return false
            if (max != 1.0f) return false

            // We would have already returned true if this was SRGB itself, so
            // it is safe to reference it here.
            val srgb = ColorSpaces.Srgb

            var x = 0.0
            while (x <= 1.0) {
                if (!compare(
                        x,
                        OETF,
                        srgb.oetfOrig
                    )
                ) return false
                if (!compare(
                        x,
                        EOTF,
                        srgb.eotfOrig
                    )
                ) return false
                x += 1 / 255.0
            }

            return true
        }

        private fun compare(
            point: Double,
            a: DoubleFunction,
            b: DoubleFunction
        ): Boolean {
            val rA = a(point)
            val rB = b(point)
            return abs(rA - rB) <= 1e-3
        }

        /**
         * Computes whether the specified CIE xyY or XYZ primaries (with Y set to 1) form
         * a wide color gamut. A color gamut is considered wide if its area is &gt; 90%
         * of the area of NTSC 1953 and if it contains the sRGB color gamut entirely.
         * If the conditions above are not met, the color space is considered as having
         * a wide color gamut if its range is larger than [0..1].
         *
         * @param primaries RGB primaries in CIE xyY as an array of 6 floats
         * @param min The minimum value of the color space's range
         * @param max The minimum value of the color space's range
         * @return True if the color space has a wide gamut, false otherwise
         *
         * @see isWideGamut
         * @see area
         */
        private fun isWideGamut(
            primaries: FloatArray,
            min: Float,
            max: Float
        ): Boolean {
            return (
                (
                    (
                        area(primaries) / area(
                            ColorSpaces.Ntsc1953Primaries
                        ) > 0.9f && contains(
                            primaries,
                            ColorSpaces.SrgbPrimaries
                        )
                        )
                    ) || (min < 0.0f && max > 1.0f)
                )
        }

        /**
         * Computes the area of the triangle represented by a set of RGB primaries
         * in the CIE xyY space.
         *
         * @param primaries The triangle's vertices, as RGB primaries in an array of 6 floats
         * @return The area of the triangle
         *
         * @see isWideGamut
         */
        private fun area(primaries: FloatArray): Float {
            val rx = primaries[0]
            val ry = primaries[1]
            val gx = primaries[2]
            val gy = primaries[3]
            val bx = primaries[4]
            val by = primaries[5]
            val det = rx * gy + ry * bx + gx * by - gy * bx - ry * gx - rx * by
            val r = 0.5f * det
            return if (r < 0.0f) -r else r
        }

        /**
         * Computes the cross product of two 2D vectors.
         *
         * @param ax The x coordinate of the first vector
         * @param ay The y coordinate of the first vector
         * @param bx The x coordinate of the second vector
         * @param by The y coordinate of the second vector
         * @return The result of a x b
         */
        private fun cross(ax: Float, ay: Float, bx: Float, by: Float): Float {
            return ax * by - ay * bx
        }

        /**
         * Decides whether a 2D triangle, identified by the 6 coordinates of its
         * 3 vertices, is contained within another 2D triangle, also identified
         * by the 6 coordinates of its 3 vertices.
         *
         * In the illustration below, we want to test whether the RGB triangle
         * is contained within the triangle XYZ formed by the 3 vertices at
         * the "+" locations.
         *
         *
         *                                     Y     .
         *                                 .   +    .
         *                                  .     ..
         *                                   .   .
         *                                    . .
         *                                     .  G
         *                                     *
         *                                    * *
         *                                  **   *
         *                                 *      **
         *                                *         *
         *                              **           *
         *                             *              *
         *                            *                *
         *                          **                  *
         *                         *                     *
         *                        *                       **
         *                      **                          *   R    ...
         *                     *                             *  .....
         *                    *                         ***** ..
         *                  **              ************       .   +
         *              B  *    ************                    .   X
         *           ......*****                                 .
         *     ......    .                                        .
         *             ..
         *        +   .
         *      Z    .
         *
         * RGB is contained within XYZ if all the following conditions are true
         * (with "x" the cross product operator):
         *
         *   -->  -->
         *   GR x RX >= 0
         *   -->  -->
         *   RX x BR >= 0
         *   -->  -->
         *   RG x GY >= 0
         *   -->  -->
         *   GY x RG >= 0
         *   -->  -->
         *   RB x BZ >= 0
         *   -->  -->
         *   BZ x GB >= 0
         *
         * @param p1 The enclosing triangle as 6 floats
         * @param p2 The enclosed triangle as 6 floats
         * @return True if the triangle p1 contains the triangle p2
         *
         * @see isWideGamut
         */
        private fun contains(p1: FloatArray, p2: FloatArray): Boolean {
            // Translate the vertices p1 in the coordinates system
            // with the vertices p2 as the origin
            val p0 = floatArrayOf(
                p1[0] - p2[0], p1[1] - p2[1],
                p1[2] - p2[2], p1[3] - p2[3],
                p1[4] - p2[4], p1[5] - p2[5]
            )
            // Check the first vertex of p1
            if ((
                cross(
                        p0[0],
                        p0[1],
                        p2[0] - p2[4],
                        p2[1] - p2[5]
                    ) < 0 ||
                    cross(
                        p2[0] - p2[2],
                        p2[1] - p2[3],
                        p0[0],
                        p0[1]
                    ) < 0
                )
            ) {
                return false
            }
            // Check the second vertex of p1
            if ((
                cross(
                        p0[2],
                        p0[3],
                        p2[2] - p2[0],
                        p2[3] - p2[1]
                    ) < 0 ||
                    cross(
                        p2[2] - p2[4],
                        p2[3] - p2[5],
                        p0[2],
                        p0[3]
                    ) < 0
                )
            ) {
                return false
            }
            // Check the third vertex of p1
            return !(
                cross(
                    p0[4],
                    p0[5],
                    p2[4] - p2[2],
                    p2[5] - p2[3]
                ) < 0 ||
                    cross(
                    p2[4] - p2[0],
                    p2[5] - p2[1],
                    p0[4],
                    p0[5]
                ) < 0
                )
        }

        /**
         * Computes the primaries  of a color space identified only by
         * its RGB->XYZ transform matrix. This method assumes that the
         * range of the color space is [0..1].
         *
         * @param toXYZ The color space's 3x3 transform matrix to XYZ
         * @return A new array of 6 floats containing the color space's
         * primaries in CIE xyY
         */
        internal fun computePrimaries(toXYZ: FloatArray): FloatArray {
            val r = mul3x3Float3(
                toXYZ,
                floatArrayOf(1.0f, 0.0f, 0.0f)
            )
            val g = mul3x3Float3(
                toXYZ,
                floatArrayOf(0.0f, 1.0f, 0.0f)
            )
            val b = mul3x3Float3(
                toXYZ,
                floatArrayOf(0.0f, 0.0f, 1.0f)
            )

            val rSum = r[0] + r[1] + r[2]
            val gSum = g[0] + g[1] + g[2]
            val bSum = b[0] + b[1] + b[2]

            return floatArrayOf(
                r[0] / rSum, r[1] / rSum,
                g[0] / gSum, g[1] / gSum,
                b[0] / bSum, b[1] / bSum
            )
        }

        /**
         * Computes the white point of a color space identified only by
         * its RGB->XYZ transform matrix. This method assumes that the
         * range of the color space is [0..1].
         *
         * @param toXYZ The color space's 3x3 transform matrix to XYZ
         * @return A new array of 2 floats containing the color space's
         * white point in CIE xyY
         */
        private fun computeWhitePoint(toXYZ: FloatArray): WhitePoint {
            val w = mul3x3Float3(
                toXYZ,
                floatArrayOf(1.0f, 1.0f, 1.0f)
            )
            val sum = w[0] + w[1] + w[2]
            return WhitePoint(w[0] / sum, w[1] / sum)
        }

        /**
         * Converts the specified RGB primaries point to xyY if needed. The primaries
         * can be specified as an array of 6 floats (in CIE xyY) or 9 floats
         * (in CIE XYZ). If no conversion is needed, the input array is copied.
         *
         * @param primaries The primaries in xyY or XYZ, in an array of 6 floats.
         * @return A new array of 6 floats containing the primaries in xyY
         */
        private fun xyPrimaries(primaries: FloatArray): FloatArray {
            val xyPrimaries = FloatArray(6)

            // XYZ to xyY
            if (primaries.size == 9) {
                var sum: Float = primaries[0] + primaries[1] + primaries[2]
                xyPrimaries[0] = primaries[0] / sum
                xyPrimaries[1] = primaries[1] / sum

                sum = primaries[3] + primaries[4] + primaries[5]
                xyPrimaries[2] = primaries[3] / sum
                xyPrimaries[3] = primaries[4] / sum

                sum = primaries[6] + primaries[7] + primaries[8]
                xyPrimaries[4] = primaries[6] / sum
                xyPrimaries[5] = primaries[7] / sum
            } else {
                primaries.copyInto(xyPrimaries, endIndex = 6)
            }

            return xyPrimaries
        }

        /**
         * Computes the matrix that converts from RGB to XYZ based on RGB
         * primaries and a white point, both specified in the CIE xyY space.
         * The Y component of the primaries and white point is implied to be 1.
         *
         * @param primaries The RGB primaries in xyY, as an array of 6 floats
         * @param whitePoint The white point in xyY, as an array of 2 floats
         * @return A 3x3 matrix as a new array of 9 floats
         */
        private fun computeXYZMatrix(
            primaries: FloatArray,
            whitePoint: WhitePoint
        ): FloatArray {
            val rx = primaries[0]
            val ry = primaries[1]
            val gx = primaries[2]
            val gy = primaries[3]
            val bx = primaries[4]
            val by = primaries[5]
            val wx = whitePoint.x
            val wy = whitePoint.y

            val oneRxRy = (1 - rx) / ry
            val oneGxGy = (1 - gx) / gy
            val oneBxBy = (1 - bx) / by
            val oneWxWy = (1 - wx) / wy

            val rxRy = rx / ry
            val gxGy = gx / gy
            val bxBy = bx / by
            val wxWy = wx / wy

            val byNumerator =
                (oneWxWy - oneRxRy) * (gxGy - rxRy) - (wxWy - rxRy) * (oneGxGy - oneRxRy)
            val byDenominator =
                (oneBxBy - oneRxRy) * (gxGy - rxRy) - (bxBy - rxRy) * (oneGxGy - oneRxRy)
            val bY = byNumerator / byDenominator
            val gY = (wxWy - rxRy - bY * (bxBy - rxRy)) / (gxGy - rxRy)
            val rY = 1f - gY - bY

            val rYRy = rY / ry
            val gYGy = gY / gy
            val bYBy = bY / by

            return floatArrayOf(
                rYRy * rx, rY, rYRy * (1f - rx - ry),
                gYGy * gx, gY, gYGy * (1f - gx - gy),
                bYBy * bx, bY, bYBy * (1f - bx - by)
            )
        }
    }
}

/**
 * Java's DoubleUnaryOperator isn't available until API 24, so we'll use a substitute.
 * When we bump minimum SDK versions, this should be removed and we should use Java's version.
 */
internal fun interface DoubleFunction {
    operator fun invoke(double: Double): Double
}