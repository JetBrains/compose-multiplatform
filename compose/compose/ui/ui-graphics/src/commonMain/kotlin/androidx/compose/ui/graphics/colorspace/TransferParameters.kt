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
 * Defines the parameters for the ICC parametric curve type 4, as
 * defined in ICC.1:2004-10, section 10.15.
 *
 * [The EOTF is of the form linked here](https://d.android.com/reference/android/graphics/ColorSpace.Rgb.TransferParameters)
 *
 * The corresponding OETF is simply the inverse function.
 *
 * The parameters defined by this class form a valid transfer
 * function only if all the following conditions are met:
 *
 *  * No parameter is a [Not-a-Number][Double.isNaN]
 *  * `d` is in the range `[0..1]`
 *  * The function is not constant
 *  * The function is positive and increasing
 */
data class TransferParameters(
    /** Value g in the equation of the EOTF described above.  */
    val gamma: Double,
    /** Value a in the equation of the EOTF described above.  */
    val a: Double,
    /** Value b in the equation of the EOTF described above.  */
    val b: Double,
    /** Value c in the equation of the EOTF described above.  */
    val c: Double,
    /** Value d in the equation of the EOTF described above.  */
    val d: Double,
    /** Value e in the equation of the EOTF described above.  */
    val e: Double = 0.0,
    /** Value f in the equation of the EOTF described above.  */
    val f: Double = 0.0
) {
    init {
        if (a.isNaN() || b.isNaN() || c.isNaN() || d.isNaN() || e.isNaN() || f.isNaN() ||
            gamma.isNaN()
        ) {
            throw IllegalArgumentException("Parameters cannot be NaN")
        }

        // Next representable float after 1.0
        // We use doubles here but the representation inside our native code is often floats
        if (!(d >= 0.0 && d <= 1.0)) {
            throw IllegalArgumentException(
                "Parameter d must be in the range [0..1], was " +
                    "$d"
            )
        }

        if (d == 0.0 && (a == 0.0 || gamma == 0.0)) {
            throw IllegalArgumentException(
                "Parameter a or g is zero, the transfer function is constant"
            )
        }

        if (d >= 1.0 && c == 0.0) {
            throw IllegalArgumentException(
                "Parameter c is zero, the transfer function is constant"
            )
        }

        if ((a == 0.0 || gamma == 0.0) && c == 0.0) {
            throw IllegalArgumentException(
                "Parameter a or g is zero," +
                    " and c is zero, the transfer function is constant"
            )
        }

        if (c < 0.0) {
            throw IllegalArgumentException("The transfer function must be increasing")
        }

        if (a < 0.0 || gamma < 0.0) {
            throw IllegalArgumentException(
                (
                    "The transfer function must be " +
                        "positive or increasing"
                    )
            )
        }
    }
}