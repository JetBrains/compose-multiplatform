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

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.withSign

/**
 * A [ColorSpace] is used to identify a specific organization of colors.
 * Each color space is characterized by a [color model][ColorModel] that defines
 * how a color value is represented (for instance the [RGB][ColorModel.Rgb] color
 * model defines a color value as a triplet of numbers).
 *
 * Each component of a color must fall within a valid range, specific to each
 * color space, defined by [getMinValue] and [getMaxValue]
 * This range is commonly `[0..1]`. While it is recommended to use values in the
 * valid range, a color space always clamps input and output values when performing
 * operations such as converting to a different color space.
 *
 * ### Using color spaces
 *
 * This implementation provides a pre-defined set of common color spaces
 * described in the [ColorSpaces] object.
 *
 * The documentation of [ColorSpaces] provides a detailed description of the
 * various characteristics of each available color space.
 *
 * ### Color space conversions
 *
 * To allow conversion between color spaces, this implementation uses the CIE
 * XYZ profile connection space (PCS). Color values can be converted to and from
 * this PCS using [toXyz] and [fromXyz].
 *
 * For color space with a non-RGB color model, the white point of the PCS
 * *must be* the CIE standard illuminant D50. RGB color spaces use their
 * native white point (D65 for [sRGB][ColorSpaces.Srgb] for instance and must
 * undergo [chromatic adaptation][Adaptation] as necessary.
 *
 * Since the white point of the PCS is not defined for RGB color space, it is
 * highly recommended to use the [connect] method to perform conversions
 * between color spaces. A color space can be
 * manually adapted to a specific white point using [adapt].
 * Please refer to the documentation of [RGB color spaces][Rgb] for more
 * information. Several common CIE standard illuminants are provided in this
 * class as reference (see [Illuminant.D65] or [Illuminant.D50]
 * for instance).
 *
 * Here is an example of how to convert from a color space to another:
 *
 *     // Convert from DCI-P3 to Rec.2020
 *     val connector = ColorSpaces.DciP3.connect(ColorSpaces.BT2020)
 *
 *     val bt2020Values = connector.transform(p3r, p3g, p3b);
 *
 * You can easily convert to [sRGB][ColorSpaces.Srgb] by omitting the color space
 * parameter:
 *
 *     // Convert from DCI-P3 to sRGB
 *     val connector = ColorSpaces.DciP3.connect()
 *
 *     val sRGBValues = connector.transform(p3r, p3g, p3b);
 *
 * Conversions also work between color spaces with different color models:
 *
 *     // Convert from CIE L*a*b* (color model Lab) to Rec.709 (color model RGB)
 *     val connector = ColorSpaces.CieLab.connect(ColorSpaces.Bt709)
 *
 * ### Color spaces and multi-threading
 *
 * Color spaces and other related classes ([Connector] for instance)
 * are immutable and stateless. They can be safely used from multiple concurrent
 * threads.
 *
 * @see ColorSpaces
 * @see ColorModel
 * @see Connector
 * @see Adaptation
 */
abstract class ColorSpace internal constructor(
    /**
     * Returns the name of this color space. The name is never null
     * and contains always at least 1 character.
     *
     * Color space names are recommended to be unique but are not
     * guaranteed to be. There is no defined format but the name usually
     * falls in one of the following categories:
     *
     *  * Generic names used to identify color spaces in non-RGB
     * color models. For instance: [Generic L*a*b*][ColorSpaces.CieLab].
     *  * Names tied to a particular specification. For instance:
     * [sRGB IEC61966-2.1][ColorSpaces.Srgb] or
     * [SMPTE ST 2065-1:2012 ACES][ColorSpaces.Aces].
     *  * Ad-hoc names, often generated procedurally or by the user
     * during a calibration workflow. These names often contain the
     * make and model of the display.
     *
     * Because the format of color space names is not defined, it is
     * not recommended to programmatically identify a color space by its
     * name alone. Names can be used as a first approximation.
     *
     * It is however perfectly acceptable to display color space names to
     * users in a UI, or in debuggers and logs. When displaying a color space
     * name to the user, it is recommended to add extra information to avoid
     * ambiguities: color model, a representation of the color space's gamut,
     * white point, etc.
     *
     * @return A non-null String of length >= 1
     */
    val name: String,

    /**
     * The color model of this color space.
     *
     * @see ColorModel
     * @see componentCount
     */
    val model: ColorModel,

    /**
     * The ID of this color space. Positive IDs match the color
     * spaces enumerated in [ColorSpaces]. A negative ID indicates a
     * color space created by calling one of the public constructors.
     */
    internal val id: Int
) {
    constructor(name: String, model: ColorModel) : this(name, model, MinId)
    /**
     * Returns the number of components that form a color value according
     * to this color space's color model.
     *
     * @return An integer between 1 and 4
     *
     * @see ColorModel
     * @see model
     */
    val componentCount: Int
        /*@IntRange(from = 1, to = 4)*/
        get() = model.componentCount

    /**
     * Returns whether this color space is a wide-gamut color space.
     * An RGB color space is wide-gamut if its gamut entirely contains
     * the [sRGB][ColorSpaces.Srgb] gamut and if the area of its gamut is
     * 90% of greater than the area of the [NTSC][ColorSpaces.Ntsc1953]
     * gamut.
     *
     * @return True if this color space is a wide-gamut color space,
     * false otherwise
     */
    abstract val isWideGamut: Boolean

    /**
     *
     * Indicates whether this color space is the sRGB color space or
     * equivalent to the sRGB color space.
     *
     * A color space is considered sRGB if it meets all the following
     * conditions:
     *
     *  * Its color model is [ColorModel.Rgb].
     *  *
     * Its primaries are within 1e-3 of the true
     * [sRGB][ColorSpaces.Srgb] primaries.
     *
     *  *
     * Its white point is within 1e-3 of the CIE standard
     * illuminant [D65][Illuminant.D65].
     *
     *  * Its opto-electronic transfer function is not linear.
     *  * Its electro-optical transfer function is not linear.
     *  * Its transfer functions yield values within 1e-3 of [ColorSpaces.Srgb].
     *  * Its range is `[0..1]`.
     *
     *
     * This method always returns true for [ColorSpaces.Srgb].
     *
     * @return True if this color space is the sRGB color space (or a
     * close approximation), false otherwise
     */
    open val isSrgb: Boolean
        get() = false

    init { // ColorSpace init
        if (name.isEmpty()) {
            throw IllegalArgumentException(
                "The name of a color space cannot be null and " +
                    "must contain at least 1 character"
            )
        }

        if (id < MinId || id > MaxId) {
            throw IllegalArgumentException("The id must be between $MinId and $MaxId")
        }
    }

    /**
     * Returns the minimum valid value for the specified component of this
     * color space's color model.
     *
     * @param component The index of the component, from `0` to `3`, inclusive.
     * @return A floating point value less than [getMaxValue]
     *
     * @see getMaxValue
     * @see ColorModel.componentCount
     */
    abstract fun getMinValue(/*@IntRange(from = 0, to = 3)*/ component: Int): Float

    /**
     * Returns the maximum valid value for the specified component of this
     * color space's color model.
     *
     * @param component The index of the component, from `0` to `3`, inclusive
     * @return A floating point value greater than [getMinValue]
     *
     * @see getMinValue
     * @see ColorModel.componentCount
     */
    abstract fun getMaxValue(/*@IntRange(from = 0, to = 3)*/ component: Int): Float

    /**
     * Converts a color value from this color space's model to
     * tristimulus CIE XYZ values. If the color model of this color
     * space is not [RGB][ColorModel.Rgb], it is assumed that the
     * target CIE XYZ space uses a [D50][Illuminant.D50]
     * standard illuminant.
     *
     * This method is a convenience for color spaces with a model
     * of 3 components ([RGB][ColorModel.Rgb] or [ColorModel.Lab]
     * for instance). With color spaces using fewer or more components,
     * use [toXyz] instead.
     *
     * @param r The first component of the value to convert from (typically R in RGB)
     * @param g The second component of the value to convert from (typically G in RGB)
     * @param b The third component of the value to convert from (typically B in RGB)
     * @return A new array of 3 floats, containing tristimulus XYZ values
     *
     * @see toXyz
     * @see fromXyz
     */
    /*@Size(3)*/
    fun toXyz(r: Float, g: Float, b: Float): FloatArray {
        return toXyz(floatArrayOf(r, g, b))
    }

    /**
     * Converts a color value from this color space's model to
     * tristimulus CIE XYZ values. If the color model of this color
     * space is not [RGB][ColorModel.Rgb], it is assumed that the
     * target CIE XYZ space uses a [D50][Illuminant.D50]
     * standard illuminant.
     *
     * The specified array's length  must be at least
     * equal to to the number of color components as returned by
     * [ColorModel.componentCount].
     *
     * @param v An array of color components containing the color space's
     * color value to convert to XYZ, and large enough to hold
     * the resulting tristimulus XYZ values, at least 3 values.
     * @return The array passed in parameter [v].
     *
     * @see toXyz
     * @see fromXyz
     */
    /*@Size(min = 3)*/
    abstract fun toXyz(/*@Size(min = 3)*/ v: FloatArray): FloatArray

    /**
     * Converts tristimulus values from the CIE XYZ space to this
     * color space's color model.
     *
     * @param x The X component of the color value
     * @param y The Y component of the color value
     * @param z The Z component of the color value
     * @return A new array whose size is equal to the number of color
     * components as returned by [ColorModel.componentCount].
     *
     * @see fromXyz
     * @see toXyz
     */
    /*@Size(min = 3)*/
    fun fromXyz(x: Float, y: Float, z: Float): FloatArray {
        val xyz = FloatArray(model.componentCount)
        xyz[0] = x
        xyz[1] = y
        xyz[2] = z
        return fromXyz(xyz)
    }

    /**
     * Converts tristimulus values from the CIE XYZ space to this color
     * space's color model. The resulting value is passed back in the specified
     * array.
     *
     * The specified array's length  must be at least equal to
     * to the number of color components as returned by
     * [ColorModel.componentCount], and its first 3 values must
     * be the XYZ components to convert from.
     *
     * @param v An array of color components containing the XYZ values
     * to convert from, and large enough to hold the number
     * of components of this color space's model. The minimum size is 3, but
     * most color spaces have 4 components.
     * @return The array passed in parameter [v].
     *
     * @see fromXyz
     * @see toXyz
     */
    /*@Size(min = 3)*/
    abstract fun fromXyz(/*@Size(min = 3)*/ v: FloatArray): FloatArray

    /**
     * Returns a string representation of the object. This method returns
     * a string equal to the value of:
     *
     *     "$name "(id=$id, model=$model)"
     *
     * For instance, the string representation of the [sRGB][ColorSpaces.Srgb]
     * color space is equal to the following value:
     *
     *     sRGB IEC61966-2.1 (id=0, model=RGB)
     *
     * @return A string representation of the object
     */
    override fun toString(): String {
        return "$name (id=$id, model=$model)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || this::class != other::class) {
            return false
        }

        val that = other as ColorSpace

        if (id != that.id) return false

        return if (name != that.name) false else model == that.model
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + id
        return result
    }

    internal companion object { // ColorSpace companion object

        /**
         * The minimum ID value a color space can have.
         *
         * @see id
         */
        internal const val MinId = -1 // Do not change

        /**
         * The maximum ID value a color space can have.
         *
         * @see id
         */
        internal const val MaxId = 63 // Do not change, used to encode in longs
    }
}

/**
 * Connects two color spaces to allow conversion from the source color
 * space to the destination color space. If the source and destination
 * color spaces do not have the same profile connection space (CIE XYZ
 * with the same white point), they are chromatically adapted to use the
 * CIE standard illuminant [D50][Illuminant.D50] as needed.
 *
 * If the source and destination are the same, an optimized connector
 * is returned to avoid unnecessary computations and loss of precision.
 *
 * @param destination The color space to convert colors to
 * @param intent The render intent to map colors from the source to the destination
 * @return A non-null connector between the two specified color spaces
 */
fun ColorSpace.connect(
    destination: ColorSpace = ColorSpaces.Srgb,
    intent: RenderIntent = RenderIntent.Perceptual
): Connector {
    if (this === destination) {
        return Connector.identity(this)
    }

    return if (this.model == ColorModel.Rgb && destination.model == ColorModel.Rgb) {
        Connector.RgbConnector(
            this as Rgb,
            destination as Rgb,
            intent
        )
    } else {
        Connector(this, destination, intent)
    }
}

/**
 * Performs the chromatic adaptation of a color space from its native
 * white point to the specified white point. If the specified color space
 * does not have an [RGB][ColorModel.Rgb] color model, or if the color
 * space already has the target white point, the color space is returned
 * unmodified.
 *
 * The chromatic adaptation is performed using the von Kries method
 * described in the documentation of [Adaptation].
 *
 * @param whitePoint The new white point
 * @param adaptation The adaptation matrix
 * @return A new color space if the specified color space has an RGB
 * model and a white point different from the specified white
 * point; the specified color space otherwise
 * @see Adaptation
 */
@kotlin.jvm.JvmOverloads
fun ColorSpace.adapt(
    whitePoint: WhitePoint,
    adaptation: Adaptation = Adaptation.Bradford
): ColorSpace {
    if (this.model == ColorModel.Rgb) {
        val rgb = this as Rgb
        if (compare(rgb.whitePoint, whitePoint)) {
            return this
        }

        val xyz = whitePoint.toXyz()
        val adaptationTransform =
            chromaticAdaptation(
                adaptation.transform,
                rgb.whitePoint.toXyz(),
                xyz
            )
        val transform = mul3x3(
            adaptationTransform,
            rgb.transform
        )

        return Rgb(rgb, transform, whitePoint)
    }
    return this
}

// Reciprocal piecewise gamma response
internal fun rcpResponse(x: Double, a: Double, b: Double, c: Double, d: Double, g: Double):
    Double {
        return if (x >= d * c) (x.pow(1.0 / g) - b) / a else x / c
    }

// Piecewise gamma response
internal fun response(x: Double, a: Double, b: Double, c: Double, d: Double, g: Double):
    Double {
        return if (x >= d) (a * x + b).pow(g) else c * x
    }

// Reciprocal piecewise gamma response
internal fun rcpResponse(
    x: Double,
    a: Double,
    b: Double,
    c: Double,
    d: Double,
    e: Double,
    f: Double,
    g: Double
): Double {
    return if (x >= d * c) ((x - e).pow(1.0 / g) - b) / a else (x - f) / c
}

// Piecewise gamma response
internal fun response(
    x: Double,
    a: Double,
    b: Double,
    c: Double,
    d: Double,
    e: Double,
    f: Double,
    g: Double
): Double {
    return if (x >= d) (a * x + b).pow(g) + e else c * x + f
}

// Reciprocal piecewise gamma response, encoded as sign(x).f(abs(x)) for color
// spaces that allow negative values
internal fun absRcpResponse(
    x: Double,
    a: Double,
    b: Double,
    c: Double,
    d: Double,
    g: Double
): Double {
    return rcpResponse(if (x < 0.0) -x else x, a, b, c, d, g).withSign(x)
}

// Piecewise gamma response, encoded as sign(x).f(abs(x)) for color spaces that
// allow negative values
internal fun absResponse(x: Double, a: Double, b: Double, c: Double, d: Double, g: Double):
    Double {
        return response(if (x < 0.0) -x else x, a, b, c, d, g).withSign(x)
    }

/**
 * Compares two sets of parametric transfer functions parameters with a precision of 1e-3.
 *
 * @param a The first set of parameters to compare
 * @param b The second set of parameters to compare
 * @return True if the two sets are equal, false otherwise
 */
internal fun compare(a: TransferParameters, b: TransferParameters?): Boolean {
    return (
        b != null &&
            abs(a.a - b.a) < 1e-3 &&
            abs(a.b - b.b) < 1e-3 &&
            abs(a.c - b.c) < 1e-3 &&
            abs(a.d - b.d) < 2e-3 && // Special case for variations in sRGB OETF/EOTF

            abs(a.e - b.e) < 1e-3 &&
            abs(a.f - b.f) < 1e-3 &&
            abs(a.gamma - b.gamma) < 1e-3
        )
}

/**
 * Compares two WhitePoints with a precision of 1e-3.
 *
 * @param a The first WhitePoint to compare
 * @param b The second WhitePoint to compare
 * @return True if the two WhitePoints are equal, false otherwise
 */
internal fun compare(a: WhitePoint, b: WhitePoint): Boolean {
    if (a === b) return true
    return abs(a.x - b.x) < 1e-3f && abs(a.y - b.y) < 1e-3f
}

/**
 * Compares two arrays of float with a precision of 1e-3.
 *
 * @param a The first array to compare
 * @param b The second array to compare
 * @return True if the two arrays are equal, false otherwise
 */
internal fun compare(a: FloatArray, b: FloatArray): Boolean {
    if (a === b) return true
    for (i in a.indices) {
        // TODO: do we need the compareTo() here? Isn't the abs sufficient?
        if (a[i].compareTo(b[i]) != 0 && abs(a[i] - b[i]) > 1e-3f) return false
    }
    return true
}

/**
 * Inverts a 3x3 matrix. This method assumes the matrix is invertible.
 *
 * @param m A 3x3 matrix as a non-null array of 9 floats
 * @return A new array of 9 floats containing the inverse of the input matrix
 */
internal fun inverse3x3(m: FloatArray): FloatArray {
    val a = m[0]
    val b = m[3]
    val c = m[6]
    val d = m[1]
    val e = m[4]
    val f = m[7]
    val g = m[2]
    val h = m[5]
    val i = m[8]

    val xA = e * i - f * h
    val xB = f * g - d * i
    val xC = d * h - e * g

    val det = a * xA + b * xB + c * xC

    val inverted = FloatArray(m.size)
    inverted[0] = xA / det
    inverted[1] = xB / det
    inverted[2] = xC / det
    inverted[3] = (c * h - b * i) / det
    inverted[4] = (a * i - c * g) / det
    inverted[5] = (b * g - a * h) / det
    inverted[6] = (b * f - c * e) / det
    inverted[7] = (c * d - a * f) / det
    inverted[8] = (a * e - b * d) / det
    return inverted
}

/**
 * Multiplies two 3x3 matrices, represented as non-null arrays of 9 floats.
 *
 * @param lhs 3x3 matrix, as a non-null array of 9 floats
 * @param rhs 3x3 matrix, as a non-null array of 9 floats
 * @return A new array of 9 floats containing the result of the multiplication
 * of rhs by lhs
 */
internal fun mul3x3(lhs: FloatArray, rhs: FloatArray):
    FloatArray {
        val r = FloatArray(9)
        r[0] = lhs[0] * rhs[0] + lhs[3] * rhs[1] + lhs[6] * rhs[2]
        r[1] = lhs[1] * rhs[0] + lhs[4] * rhs[1] + lhs[7] * rhs[2]
        r[2] = lhs[2] * rhs[0] + lhs[5] * rhs[1] + lhs[8] * rhs[2]
        r[3] = lhs[0] * rhs[3] + lhs[3] * rhs[4] + lhs[6] * rhs[5]
        r[4] = lhs[1] * rhs[3] + lhs[4] * rhs[4] + lhs[7] * rhs[5]
        r[5] = lhs[2] * rhs[3] + lhs[5] * rhs[4] + lhs[8] * rhs[5]
        r[6] = lhs[0] * rhs[6] + lhs[3] * rhs[7] + lhs[6] * rhs[8]
        r[7] = lhs[1] * rhs[6] + lhs[4] * rhs[7] + lhs[7] * rhs[8]
        r[8] = lhs[2] * rhs[6] + lhs[5] * rhs[7] + lhs[8] * rhs[8]
        return r
    }

/**
 * Multiplies a vector of 3 components by a 3x3 matrix and stores the
 * result in the input vector.
 *
 * @param lhs 3x3 matrix, as a non-null array of 9 floats
 * @param rhs Vector of 3 components, as a non-null array of 3 floats
 * @return The array of 3 passed as the [rhs] parameter
 */
internal fun mul3x3Float3(
    lhs: FloatArray,
    rhs: FloatArray
): FloatArray {
    val r0 = rhs[0]
    val r1 = rhs[1]
    val r2 = rhs[2]
    rhs[0] = lhs[0] * r0 + lhs[3] * r1 + lhs[6] * r2
    rhs[1] = lhs[1] * r0 + lhs[4] * r1 + lhs[7] * r2
    rhs[2] = lhs[2] * r0 + lhs[5] * r1 + lhs[8] * r2
    return rhs
}

/**
 * Multiplies a diagonal 3x3 matrix lhs, represented as an array of 3 floats,
 * by a 3x3 matrix represented as an array of 9 floats.
 *
 * @param lhs Diagonal 3x3 matrix, as a non-null array of 3 floats
 * @param rhs 3x3 matrix, as a non-null array of 9 floats
 * @return A new array of 9 floats containing the result of the multiplication
 * of [rhs] by [lhs].
 */
internal fun mul3x3Diag(
    lhs: FloatArray,
    rhs: FloatArray
): FloatArray {
    return floatArrayOf(
        lhs[0] * rhs[0], lhs[1] * rhs[1], lhs[2] * rhs[2],
        lhs[0] * rhs[3], lhs[1] * rhs[4], lhs[2] * rhs[5],
        lhs[0] * rhs[6], lhs[1] * rhs[7], lhs[2] * rhs[8]
    )
}

/**
 * Computes the chromatic adaptation transform from the specified
 * source white point to the specified destination white point.
 *
 * The transform is computed using the von Kries method, described
 * in more details in the documentation of [Adaptation]. The
 * [Adaptation] enum provides different matrices that can be
 * used to perform the adaptation.
 *
 * @param matrix The adaptation matrix
 * @param srcWhitePoint The white point to adapt from, *will be modified*
 * @param dstWhitePoint The white point to adapt to, *will be modified*
 * @return A 3x3 matrix as a non-null array of 9 floats
 */
internal fun chromaticAdaptation(
    matrix: FloatArray,
    srcWhitePoint: FloatArray,
    dstWhitePoint: FloatArray
): FloatArray {
    val srcLMS = mul3x3Float3(matrix, srcWhitePoint)
    val dstLMS = mul3x3Float3(matrix, dstWhitePoint)
    // LMS is a diagonal matrix stored as a float[3]
    val LMS =
        floatArrayOf(dstLMS[0] / srcLMS[0], dstLMS[1] / srcLMS[1], dstLMS[2] / srcLMS[2])
    return mul3x3(inverse3x3(matrix), mul3x3Diag(LMS, matrix))
}