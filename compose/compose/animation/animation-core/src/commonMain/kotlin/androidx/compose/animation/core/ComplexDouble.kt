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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.animation.core

import kotlin.math.abs
import kotlin.math.sqrt

internal data class ComplexDouble(private var _real: Double, private var _imaginary: Double) {
    val real: Double
        get() {
            return _real
        }
    val imaginary: Double
        get() {
            return _imaginary
        }

    inline operator fun plus(other: Double): ComplexDouble {
        _real += other
        return this
    }

    inline operator fun plus(other: ComplexDouble): ComplexDouble {
        _real += other.real
        _imaginary += other.imaginary
        return this
    }

    inline operator fun minus(other: Double): ComplexDouble {
        return this + -other
    }

    inline operator fun minus(other: ComplexDouble): ComplexDouble {
        return this + -other
    }

    inline operator fun times(other: Double): ComplexDouble {
        _real *= other
        _imaginary *= other
        return this
    }

    inline operator fun times(other: ComplexDouble): ComplexDouble {
        _real = real * other.real - imaginary * other.imaginary
        _imaginary = real * other.imaginary + other.real * imaginary
        return this
    }

    inline operator fun unaryMinus(): ComplexDouble {
        _real *= -1
        _imaginary *= -1
        return this
    }

    inline operator fun div(other: Double): ComplexDouble {
        _real /= other
        _imaginary /= other
        return this
    }
}

/**
 * Returns the roots of the polynomial [a]x^2+[b]x+[c]=0 which may be complex.
 */
internal fun complexQuadraticFormula(
    a: Double,
    b: Double,
    c: Double
): Pair<ComplexDouble, ComplexDouble> {
    val firstRoot = (-b + complexSqrt(b * b - 4.0 * a * c)) / (2.0 * a)
    val secondRoot = (-b - complexSqrt(b * b - 4.0 * a * c)) / (2.0 * a)
    return firstRoot to secondRoot
}

/**
 * Returns the square root of [num] which may be imaginary.
 */
internal fun complexSqrt(num: Double): ComplexDouble {
    return if (num < 0.0) {
        ComplexDouble(0.0, sqrt(abs(num)))
    } else {
        ComplexDouble(sqrt(num), 0.0)
    }
}

internal inline operator fun Double.plus(other: ComplexDouble): ComplexDouble {
    return other + this
}

internal inline operator fun Double.minus(other: ComplexDouble): ComplexDouble {
    return this + -other
}

internal inline operator fun Double.times(other: ComplexDouble): ComplexDouble {
    return other * this
}