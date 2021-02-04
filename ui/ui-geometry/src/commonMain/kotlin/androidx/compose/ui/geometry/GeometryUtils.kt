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

package androidx.compose.ui.geometry

import kotlin.math.pow

// File of internal utility methods used for the geometry library
internal fun Float.toStringAsFixed(digits: Int): String {
    val clampedDigits: Int = kotlin.math.max(digits, 0) // Accept positive numbers and 0 only
    val pow = 10f.pow(clampedDigits)
    val shifted = this * pow // shift the given value by the corresponding power of 10
    val decimal = shifted - shifted.toInt() // obtain the decimal of the shifted value
    // Manually round up if the decimal value is greater than or equal to 0.5f.
    // because kotlin.math.round(0.5f) rounds down
    val roundedShifted = if (decimal >= 0.5f) {
        shifted.toInt() + 1
    } else {
        shifted.toInt()
    }

    val rounded = roundedShifted / pow // divide off the corresponding power of 10 to shift back
    return if (clampedDigits > 0) {
        // If we have any decimal points, convert the float to a string
        rounded.toString()
    } else {
        // If we do not have any decimal points, return the int
        // based string representation
        rounded.toInt().toString()
    }
}
