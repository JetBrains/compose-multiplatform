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
 * Illuminant contains standard CIE [white points][WhitePoint].
 */
object Illuminant {
    /**
     * Standard CIE 1931 2° illuminant A, encoded in xyY.
     * This illuminant has a color temperature of 2856K.
     */
    val A = WhitePoint(0.44757f, 0.40745f)

    /**
     * Standard CIE 1931 2° illuminant B, encoded in xyY.
     * This illuminant has a color temperature of 4874K.
     */
    val B = WhitePoint(0.34842f, 0.35161f)

    /**
     * Standard CIE 1931 2° illuminant C, encoded in xyY.
     * This illuminant has a color temperature of 6774K.
     */
    val C = WhitePoint(0.31006f, 0.31616f)

    /**
     * Standard CIE 1931 2° illuminant D50, encoded in xyY.
     * This illuminant has a color temperature of 5003K. This illuminant
     * is used by the profile connection space in ICC profiles.
     */
    val D50 = WhitePoint(0.34567f, 0.35850f)

    /**
     * Standard CIE 1931 2° illuminant D55, encoded in xyY.
     * This illuminant has a color temperature of 5503K.
     */
    val D55 = WhitePoint(0.33242f, 0.34743f)

    /**
     * Standard CIE 1931 2° illuminant D60, encoded in xyY.
     * This illuminant has a color temperature of 6004K.
     */
    val D60 = WhitePoint(0.32168f, 0.33767f)

    /**
     * Standard CIE 1931 2° illuminant D65, encoded in xyY.
     * This illuminant has a color temperature of 6504K. This illuminant
     * is commonly used in RGB color spaces such as sRGB, BT.209, etc.
     */
    val D65 = WhitePoint(0.31271f, 0.32902f)

    /**
     * Standard CIE 1931 2° illuminant D75, encoded in xyY.
     * This illuminant has a color temperature of 7504K.
     */
    val D75 = WhitePoint(0.29902f, 0.31485f)

    /**
     * Standard CIE 1931 2° illuminant E, encoded in xyY.
     * This illuminant has a color temperature of 5454K.
     */
    val E = WhitePoint(0.33333f, 0.33333f)

    internal val D50Xyz = floatArrayOf(0.964212f, 1.0f, 0.825188f)
}