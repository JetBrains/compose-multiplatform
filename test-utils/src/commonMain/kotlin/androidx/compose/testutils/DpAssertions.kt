/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.testutils

import androidx.compose.ui.unit.Dp
import kotlin.math.absoluteValue

/**
 * Asserts that this value is equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 *
 * @throws AssertionError if comparison fails.
 */
fun Dp.assertIsEqualTo(expected: Dp, subject: String = "", tolerance: Dp = Dp(.5f)) {
    val diff = (this - expected).value.absoluteValue
    if (diff > tolerance.value) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, expected $expected (tolerance: $tolerance)"
        )
    }
}

/**
 * Asserts that this value is not equal to the given [unexpected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param unexpected The value to which this one should not be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance that is expected to be greater than the difference between the
 * given values to treat them as non-equal.
 *
 * @throws AssertionError if comparison fails.
 */
fun Dp.assertIsNotEqualTo(unexpected: Dp, subject: String = "", tolerance: Dp = Dp(.5f)) {
    val diff = (this - unexpected).value.absoluteValue
    if (diff <= tolerance.value) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, not expected to be equal to $unexpected within a " +
                "tolerance of $tolerance"
        )
    }
}