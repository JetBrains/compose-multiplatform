/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import java.util.Locale

/**
 * Creates a [CalendarModel] to be used by the date picker.
 */
@ExperimentalMaterial3Api
internal expect fun CalendarModel(): CalendarModel

/**
 * Formats a UTC timestamp into a string with a given date format skeleton.
 *
 * A skeleton is similar to, and uses the same format characters as described in
 * [Unicode Technical Standard #35](https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table)
 *
 * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
 * en_US locale, but "d. MMMM" in the de_CH locale.
 *
 * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
 * @param skeleton a date format skeleton
 * @param locale the [Locale] to use when formatting the given timestamp
 */
@ExperimentalMaterial3Api
internal expect fun formatWithSkeleton(
    utcTimeMillis: Long,
    skeleton: String,
    locale: Locale = Locale.getDefault()
): String

/**
 * A composable function that returns the default [Locale].
 *
 * When running on an Android platform, it will be recomposed when the `Configuration` gets updated.
 */
@Composable
@ReadOnlyComposable
@ExperimentalMaterial3Api
internal expect fun defaultLocale(): Locale

@ExperimentalMaterial3Api
internal interface CalendarModel {

    /**
     * A [CalendarDate] representing the current day.
     */
    val today: CalendarDate

    /**
     * Hold the first day of the week at the current `Locale` as an integer. The integer value
     * follows the ISO-8601 standard and refer to Monday as 1, and Sunday as 7.
     */
    val firstDayOfWeek: Int

    /**
     * Holds a list of weekday names, starting from Monday as the first day in the list.
     *
     * Each item in this list is a [Pair] that holds the full name of the day, and its short
     * abbreviation letter(s).
     *
     * Newer APIs (i.e. API 26+), a [Pair] will hold a full name and the first letter of the
     * day.
     * Older APIs that predate API 26 will hold a full name and the first three letters of the day.
     */
    val weekdayNames: List<Pair<String, String>>

    /**
     * Returns a [DateInputFormat] for the given [Locale].
     *
     * The input format represents the date with two digits for the day and the month, and
     * four digits for the year.
     *
     * For example, the input format patterns, including delimiters, will hold 10-characters strings
     * in one of the following variations:
     *  - yyyy/MM/dd
     *  - yyyy-MM-dd
     *  - yyyy.MM.dd
     *  - dd/MM/yyyy
     *  - dd-MM-yyyy
     *  - dd.MM.yyyy
     *  - MM/dd/yyyy
     */
    fun getDateInputFormat(locale: Locale = Locale.getDefault()): DateInputFormat

    /**
     * Returns a [CalendarDate] from a given _UTC_ time in milliseconds.
     *
     * The returned date will hold milliseconds value that represent the start of the day, which may
     * be different than the one provided to this function.
     *
     * @param timeInMillis UTC milliseconds from the epoch
     */
    fun getCanonicalDate(timeInMillis: Long): CalendarDate

    /**
     * Returns a [CalendarMonth] from a given _UTC_ time in milliseconds.
     *
     * @param timeInMillis UTC milliseconds from the epoch for the first day the month
     */
    fun getMonth(timeInMillis: Long): CalendarMonth

    /**
     * Returns a [CalendarMonth] from a given [CalendarDate].
     *
     * Note: This function ignores the [CalendarDate.dayOfMonth] value and just uses the date's
     * year and month to resolve a [CalendarMonth].
     *
     * @param date a [CalendarDate] to resolve into a month
     */
    fun getMonth(date: CalendarDate): CalendarMonth

    /**
     * Returns a [CalendarMonth] from a given [year] and [month].
     *
     * @param year the month's year
     * @param month an integer representing a month (e.g. JANUARY as 1, December as 12)
     */
    fun getMonth(year: Int, /* @IntRange(from = 1, to = 12) */ month: Int): CalendarMonth

    /**
     * Returns a day of week from a given [CalendarDate].
     *
     * @param date a [CalendarDate] to resolve
     */
    fun getDayOfWeek(date: CalendarDate): Int

    /**
     * Returns a [CalendarMonth] that is computed by adding a number of months, given as
     * [addedMonthsCount], to a given month.
     *
     * @param from the [CalendarMonth] to add to
     * @param addedMonthsCount the number of months to add
     */
    fun plusMonths(from: CalendarMonth, addedMonthsCount: Int): CalendarMonth

    /**
     * Returns a [CalendarMonth] that is computed by subtracting a number of months, given as
     * [subtractedMonthsCount], from a given month.
     *
     * @param from the [CalendarMonth] to subtract from
     * @param subtractedMonthsCount the number of months to subtract
     */
    fun minusMonths(from: CalendarMonth, subtractedMonthsCount: Int): CalendarMonth

    /**
     * Formats a [CalendarMonth] into a string with a given date format skeleton.
     *
     * @param month a [CalendarMonth] to format
     * @param skeleton a date format skeleton
     * @param locale the [Locale] to use when formatting the given month
     */
    fun formatWithSkeleton(
        month: CalendarMonth,
        skeleton: String,
        locale: Locale = Locale.getDefault()
    ): String =
        formatWithSkeleton(month.startUtcTimeMillis, skeleton, locale)

    /**
     * Formats a [CalendarDate] into a string with a given date format skeleton.
     *
     * @param date a [CalendarDate] to format
     * @param skeleton a date format skeleton
     * @param locale the [Locale] to use when formatting the given date
     */
    fun formatWithSkeleton(
        date: CalendarDate,
        skeleton: String,
        locale: Locale = Locale.getDefault()
    ): String = formatWithSkeleton(date.utcTimeMillis, skeleton, locale)

    /**
     * Formats a UTC timestamp into a string with a given date format pattern.
     *
     * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
     * @param pattern a date format pattern
     * @param locale the [Locale] to use when formatting the given timestamp
     */
    fun formatWithPattern(utcTimeMillis: Long, pattern: String, locale: Locale): String

    /**
     * Parses a date string into a [CalendarDate].
     *
     * @param date a date string
     * @param pattern the expected date pattern to be used for parsing the date string
     * @return a [CalendarDate], or a `null` in case the parsing failed
     */
    fun parse(date: String, pattern: String): CalendarDate?
}

/**
 * Represents a calendar date.
 *
 * @param year the date's year
 * @param month the date's month
 * @param dayOfMonth the date's day of month
 * @param utcTimeMillis the date representation in _UTC_ milliseconds from the epoch
 */
@ExperimentalMaterial3Api
internal data class CalendarDate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val utcTimeMillis: Long
) : Comparable<CalendarDate> {
    override operator fun compareTo(other: CalendarDate): Int =
        this.utcTimeMillis.compareTo(other.utcTimeMillis)

    /**
     * Formats the date into a string with the given skeleton format and a [Locale].
     */
    fun format(
        calendarModel: CalendarModel,
        skeleton: String,
        locale: Locale = Locale.getDefault()
    ): String =
        calendarModel.formatWithSkeleton(this, skeleton, locale)
}

/**
 * Represents a calendar month.
 *
 * @param year the month's year
 * @param month the calendar month as an integer (e.g. JANUARY as 1, December as 12)
 * @param numberOfDays the number of days in the month
 * @param daysFromStartOfWeekToFirstOfMonth the number of days from the start of the week to the
 * first day of the month
 * @param startUtcTimeMillis the first day of the month in _UTC_ milliseconds from the epoch
 */
@ExperimentalMaterial3Api
internal data class CalendarMonth(
    val year: Int,
    val month: Int,
    val numberOfDays: Int,
    val daysFromStartOfWeekToFirstOfMonth: Int,
    val startUtcTimeMillis: Long
) {

    /**
     * The last _UTC_ milliseconds from the epoch of the month (i.e. the last millisecond of the
     * last day of the month)
     */
    val endUtcTimeMillis: Long = startUtcTimeMillis + (numberOfDays * MillisecondsIn24Hours) - 1

    /**
     * Returns the position of a [CalendarMonth] within given years range.
     */
    fun indexIn(years: IntRange): Int {
        return (year - years.first) * 12 + month - 1
    }

    /**
     * Formats the month into a string with the given skeleton format and a [Locale].
     */
    fun format(
        calendarModel: CalendarModel,
        skeleton: String,
        locale: Locale = Locale.getDefault()
    ): String =
        calendarModel.formatWithSkeleton(this, skeleton, locale)
}

/**
 * Holds the date input format pattern information.
 *
 * This data class hold the delimiter that is used by the current [Locale] when representing dates
 * in a short format, as well as a date pattern with and without a delimiter.
 */
@ExperimentalMaterial3Api
@Immutable
internal data class DateInputFormat(
    val patternWithDelimiters: String,
    val delimiter: Char
) {
    val patternWithoutDelimiters: String = patternWithDelimiters.replace(delimiter.toString(), "")
}

/**
 * Receives a given local date format string and returns a string that can be displayed to the user
 * and parsed by the date parser.
 *
 * This function:
 *  - Removes all characters that don't match `d`, `M` and `y`, or any of the date format delimiters
 *    `.`, `/` and `-`.
 *  - Ensures that the format is for two digits day and month, and four digits year.
 *
 * The output of this cleanup is always a 10 characters string in one of the following variations:
 *  - yyyy/MM/dd
 *  - yyyy-MM-dd
 *  - yyyy.MM.dd
 *  - dd/MM/yyyy
 *  - dd-MM-yyyy
 *  - dd.MM.yyyy
 *  - MM/dd/yyyy
 */
@ExperimentalMaterial3Api
internal fun datePatternAsInputFormat(localeFormat: String): DateInputFormat {
    val patternWithDelimiters = localeFormat.replace(Regex("[^dMy/\\-.]"), "")
        .replace(Regex("d{1,2}"), "dd")
        .replace(Regex("M{1,2}"), "MM")
        .replace(Regex("y{1,4}"), "yyyy")
        .replace("My", "M/y") // Edge case for the Kako locale
        .removeSuffix(".") // Removes a dot suffix that appears in some formats

    val delimiterRegex = Regex("[/\\-.]")
    val delimiterMatchResult = delimiterRegex.find(patternWithDelimiters)
    val delimiterIndex = delimiterMatchResult!!.groups[0]!!.range.first
    val delimiter = patternWithDelimiters.substring(delimiterIndex, delimiterIndex + 1)
    return DateInputFormat(
        patternWithDelimiters = patternWithDelimiters,
        delimiter = delimiter[0]
    )
}

internal const val DaysInWeek: Int = 7
internal const val MillisecondsIn24Hours = 86400000L
