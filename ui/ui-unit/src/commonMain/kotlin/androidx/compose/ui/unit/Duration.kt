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

@file:kotlin.jvm.JvmName("Durations")
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/*
 * The following unit conversion factors are required to be public due to their use
 * from inline functions.
 */

const val NanosecondsPerMicrosecond = 1000L
const val MicrosecondsPerMillisecond = 1000L
const val MillisecondsPerSecond = 1000L
const val SecondsPerMinute = 60L
const val MinutesPerHour = 60L
const val HoursPerDay = 24L
const val NanosecondsPerMillisecond =
    NanosecondsPerMicrosecond * MicrosecondsPerMillisecond
const val NanosecondsPerSecond = NanosecondsPerMillisecond * MillisecondsPerSecond
const val NanosecondsPerMinute = NanosecondsPerSecond * SecondsPerMinute
const val NanosecondsPerHour = NanosecondsPerMinute * MinutesPerHour
const val NanosecondsPerDay = NanosecondsPerHour * HoursPerDay

/** Only used by this implementation */
private const val MicrosecondsPerSecond = MicrosecondsPerMillisecond * MillisecondsPerSecond

/**
 * Constructs a [Duration] given a series of time intervals in different units.
 */
fun Duration(
    days: Long = 0,
    hours: Long = 0,
    minutes: Long = 0,
    seconds: Long = 0,
    milliseconds: Long = 0,
    microseconds: Long = 0,
    nanoseconds: Long = 0
) = Duration(
    days * NanosecondsPerDay +
        hours * NanosecondsPerHour +
        minutes * NanosecondsPerMinute +
        seconds * NanosecondsPerSecond +
        milliseconds * NanosecondsPerMillisecond +
        microseconds * NanosecondsPerMicrosecond +
        nanoseconds
)

/* A [Duration] of this many days. */
inline val Long.days: Duration get() = Duration(this * NanosecondsPerDay)

/* A [Duration] of this many hours. */
inline val Long.hours: Duration get() = Duration(this * NanosecondsPerHour)

/* A [Duration] of this many minutes. */
inline val Long.minutes: Duration get() = Duration(this * NanosecondsPerMinute)

/* A [Duration] of this many seconds. */
inline val Long.seconds: Duration
    @Suppress("MethodNameUnits") // lint warning is confused by an inline class.
    get() = Duration(this * NanosecondsPerSecond)

/* A [Duration] of this many milliseconds. */
inline val Long.milliseconds: Duration get() = Duration(this * NanosecondsPerMillisecond)

/* A [Duration] of this many microseconds. */
inline val Long.microseconds: Duration get() = Duration(this * NanosecondsPerMicrosecond)

/* A [Duration] of this many nanoseconds. */
inline val Long.nanoseconds: Duration get() = Duration(this)

/* A [Duration] of this many days. */
inline val Int.days: Duration get() = toLong().days

/* A [Duration] of this many hours. */
inline val Int.hours: Duration get() = toLong().hours

/* A [Duration] of this many minutes. */
inline val Int.minutes: Duration get() = toLong().minutes

/* A [Duration] of this many seconds. */
inline val Int.seconds: Duration
    @Suppress("MethodNameUnits") // lint warning is confused by an inline class.
    get() = toLong().seconds

/* A [Duration] of this many milliseconds. */
inline val Int.milliseconds: Duration get() = toLong().milliseconds

/* A [Duration] of this many microseconds. */
inline val Int.microseconds: Duration get() = toLong().microseconds

/* A [Duration] of this many nanoseconds. */
inline val Int.nanoseconds: Duration get() = toLong().nanoseconds

/**
 * A span of time, such as 27 days, 4 hours, 12 minutes, and 3 seconds.
 *
 * A [Duration] represents a difference from one point in time to another. The
 * duration may be "negative" if the difference is from a later time to an
 * earlier.
 *
 * Durations are context independent. For example, a duration of 2 days is
 * always 48 hours, even when it is added to a `DateTime` just when the
 * time zone is about to do a daylight-savings switch.
 *
 * Despite the same name, a [Duration] object does not implement "Durations"
 * as specified by ISO 8601. In particular, a duration object does not keep
 * track of the individually provided members (such as "days" or "hours"), but
 * only uses these arguments to compute the length of the corresponding time
 * interval.
 *
 * To create a [Duration], use the unit extension functions on the primitive
 * `Int` and `Long` types:
 *
 *     val aLongWeekend = 96.hours
 *
 * To create a [Duration] from several components, use the [Duration] factory function:
 *
 *     val fastestMarathon = Duration(hours = 2, minutes = 3, seconds = 2)
 *
 * The [Duration] is the sum of all individual parts.
 * This means that individual parts can be larger than the next-bigger unit.
 * For example, [inMinutes] can be greater than 59.
 *
 *     assertEquals(123, fastestMarathon.inMinutes)
 *
 * All individual parts are allowed to be negative.
 *
 * Use one of the extensions such as [inDays] to retrieve the integer value of the Duration
 * in the specified time unit. Note that the returned value is rounded down.
 * For example,
 *
 *     val aLongWeekend = 86.hours
 *     assertEquals(3, aLongWeekend.inDays())
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class Duration(val nanoseconds: Long) : Comparable<Duration> {

    /**
     * Adds this Duration and [other] and returns the sum as a Duration.
     */
    @Stable
    operator fun plus(other: Duration) = Duration(nanoseconds + other.nanoseconds)

    /**
     * Subtracts [other] from this Duration and returns the difference.
     */
    @Stable
    operator fun minus(other: Duration) = Duration(nanoseconds - other.nanoseconds)

    /**
     * Multiplies this Duration by the given [factor] and returns the result.
     */
    @Stable
    operator fun times(factor: Int) = Duration(nanoseconds * factor)

    /**
     * Multiplies this Duration by the given [factor] and returns the result.
     */
    @Stable
    operator fun times(factor: Double) = Duration((nanoseconds * factor).toLong())

    /**
     * Divides this Duration by the given [quotient] and returns the truncated
     * result as a Duration.
     */
    @Stable
    operator fun div(quotient: Int) = Duration(nanoseconds / quotient)

    /**
     * Divides this Duration by the given [quotient] and returns the truncated
     * result as a Duration.
     */
    @Stable
    operator fun div(quotient: Double) = Duration((nanoseconds / quotient).toLong())

    /**
     * Compares this Duration to [other], returning zero if the values are equal.
     *
     * Returns a negative integer if this [Duration] is shorter than
     * [other], or a positive integer if it is longer.
     *
     * A negative [Duration] is always considered shorter than a positive one.
     *
     * It is always the case that `duration1.compareTo(duration2) < 0` if
     * `(someDate + duration1).compareTo(someDate + duration2) < 0`.
     */
    @Stable
    override fun compareTo(other: Duration): Int = when {
        nanoseconds < other.nanoseconds -> -1
        nanoseconds == other.nanoseconds -> 0
        else -> 1
    }

    /**
     * Returns a string representation of this [Duration].
     *
     * Returns a string with hours, minutes, seconds, and microseconds, in the
     * following format: `HH:MM:SS.mmmmmm`. For example,
     *
     *     val d = Duration(days = 1, hours = 1, minutes = 33, microseconds = 500)
     *     d.toString()  // "25:33:00.000500"
     */
    @Stable
    override fun toString(): String {
        if (inMicroseconds() < 0) {
            return "-${Duration(-nanoseconds)}"
        }
        val twoDigitMinutes = inMinutes().rem(MinutesPerHour).toString().padStart(2, '0')
        val twoDigitSeconds = inSeconds().rem(SecondsPerMinute).toString().padStart(2, '0')
        val sixDigitUs = inMicroseconds().rem(MicrosecondsPerSecond).toString().padStart(6, '0')
        return "${inHours()}:$twoDigitMinutes:$twoDigitSeconds.$sixDigitUs"
    }

    companion object {
        /** An empty Duration. No delay. Instant. */
        @Stable
        val Zero = Duration(0)
    }
}

/*
 * API note: The `inUnits()` functions below are not `toUnits()` because they do not
 * perform type conversion. They are also functions rather than properties to highlight
 * that they are truncating conversions of the whole value, not hours/minutes-style components
 * isolated from each other available unit.
 */

/**
 * Returns the number of whole days spanned by this Duration.
 */
inline fun Duration.inDays(): Long = nanoseconds / NanosecondsPerDay

/**
 * Returns the number of whole hours spanned by this Duration.
 *
 * The returned value can be greater than 23.
 */
inline fun Duration.inHours(): Long = nanoseconds / NanosecondsPerHour

/**
 * Returns the number of whole minutes spanned by this Duration.
 *
 * The returned value can be greater than 59.
 */
inline fun Duration.inMinutes(): Long = nanoseconds / NanosecondsPerMinute

/**
 * Returns the number of whole seconds spanned by this Duration.
 *
 * The returned value can be greater than 59.
 */
inline fun Duration.inSeconds(): Long = nanoseconds / NanosecondsPerSecond

/**
 * Returns number of whole milliseconds spanned by this Duration.
 *
 * The returned value can be greater than 999.
 */
inline fun Duration.inMilliseconds(): Long = nanoseconds / NanosecondsPerMillisecond

/**
 * Returns number of whole microseconds spanned by this Duration.
 */
inline fun Duration.inMicroseconds(): Long = nanoseconds / NanosecondsPerMicrosecond
