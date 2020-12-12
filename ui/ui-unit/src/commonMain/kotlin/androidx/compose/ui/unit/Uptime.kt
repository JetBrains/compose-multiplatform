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

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.isSpecified

/**
 * A single point in time with a time base of the system's uptime [nanoseconds]. Compare to
 * [Duration].
 *
 * Arithmetic involving [Uptime] is limited and always involves [Duration].  For example, it is
 * nonsensical to add two [Uptime]s, an [Uptime] + a [Duration] yields an [Uptime], and an
 * [Uptime] - an [Uptime] yields a [Duration].
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class Uptime(val nanoseconds: Long) : Comparable<Uptime> {

    /**
     * Adds a [Duration] to this [Uptime] and returns the result.
     */
    operator fun plus(duration: Duration) = Uptime(nanoseconds + duration.nanoseconds)

    /**
     * Subtracts a [Duration] from this [Uptime] and returns the result.
     */
    operator fun minus(duration: Duration) = Uptime(nanoseconds - duration.nanoseconds)

    /**
     * Returns the [Duration] between this [Uptime] and another.
     */
    operator fun minus(other: Uptime) = Duration(nanoseconds - other.nanoseconds)

    /**
     * Compares this [Uptime] to [other], returning zero if the values are equal.
     *
     * Returns a negative integer if this [Uptime] is shorter than
     * [other], or a positive integer if it is longer.
     */
    override fun compareTo(other: Uptime): Int = when {
        nanoseconds < other.nanoseconds -> -1
        nanoseconds == other.nanoseconds -> 0
        else -> 1
    }

    companion object {
        /** The time at which the device booted (0 nanoseconds). */
        val Boot = Uptime(0)

        /** Constant for an unspecified time. */
        val Unspecified = Uptime(Long.MIN_VALUE)
    }
}

/**
 * `false` when this is [Uptime.Unspecified].
 */
@Stable
inline val Uptime.isSpecified: Boolean
    get() = nanoseconds != Uptime.Unspecified.nanoseconds

/**
 * `true` when this is [Uptime.Unspecified].
 */
@Stable
inline val Uptime.isUnspecified: Boolean
    get() = nanoseconds == Uptime.Unspecified.nanoseconds

/**
 * If this [Uptime] [isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun Uptime.useOrElse(block: () -> Uptime): Uptime =
    if (isSpecified) this else block()

/**
 * Add a Duration to a [Uptime] and returns the result as a [Uptime].
 */
operator fun Duration.plus(uptime: Uptime) = Uptime(nanoseconds + uptime.nanoseconds)