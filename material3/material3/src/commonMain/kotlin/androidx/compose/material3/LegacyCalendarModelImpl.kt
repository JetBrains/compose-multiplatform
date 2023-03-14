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

import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * A [CalendarModel] implementation for API < 26.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class LegacyCalendarModelImpl : CalendarModel {

    override val today
        get(): CalendarDate {
            val systemCalendar = Calendar.getInstance()
            systemCalendar[Calendar.HOUR_OF_DAY] = 0
            systemCalendar[Calendar.MINUTE] = 0
            systemCalendar[Calendar.SECOND] = 0
            systemCalendar[Calendar.MILLISECOND] = 0
            val utcOffset =
                systemCalendar.get(Calendar.ZONE_OFFSET) + systemCalendar.get(Calendar.DST_OFFSET)
            return CalendarDate(
                year = systemCalendar[Calendar.YEAR],
                month = systemCalendar[Calendar.MONTH] + 1,
                dayOfMonth = systemCalendar[Calendar.DAY_OF_MONTH],
                utcTimeMillis = systemCalendar.timeInMillis + utcOffset
            )
        }

    override val firstDayOfWeek: Int = dayInISO8601(Calendar.getInstance().firstDayOfWeek)

    override val weekdayNames: List<Pair<String, String>> = buildList {
        val weekdays = DateFormatSymbols(Locale.getDefault()).weekdays
        val shortWeekdays = DateFormatSymbols(Locale.getDefault()).shortWeekdays
        // Skip the first item, as it's empty, and the second item, as it represents Sunday while it
        // should be last according to ISO-8601.
        weekdays.drop(2).forEachIndexed { index, day ->
            add(Pair(day, shortWeekdays[index + 2]))
        }
        // Add Sunday to the end.
        add(Pair(weekdays[1], shortWeekdays[1]))
    }

    override fun getDateInputFormat(locale: Locale): DateInputFormat {
        return datePatternAsInputFormat(
            (DateFormat.getDateInstance(
                DateFormat.SHORT,
                locale
            ) as SimpleDateFormat).toPattern()
        )
    }

    override fun getCanonicalDate(timeInMillis: Long): CalendarDate {
        val calendar = Calendar.getInstance(utcTimeZone)
        calendar.timeInMillis = timeInMillis
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return CalendarDate(
            year = calendar[Calendar.YEAR],
            month = calendar[Calendar.MONTH] + 1,
            dayOfMonth = calendar[Calendar.DAY_OF_MONTH],
            utcTimeMillis = calendar.timeInMillis
        )
    }

    override fun getMonth(timeInMillis: Long): CalendarMonth {
        val firstDayCalendar = Calendar.getInstance(utcTimeZone)
        firstDayCalendar.timeInMillis = timeInMillis
        firstDayCalendar[Calendar.DAY_OF_MONTH] = 1
        firstDayCalendar[Calendar.HOUR_OF_DAY] = 0
        firstDayCalendar[Calendar.MINUTE] = 0
        firstDayCalendar[Calendar.SECOND] = 0
        firstDayCalendar[Calendar.MILLISECOND] = 0
        return getMonth(firstDayCalendar)
    }

    override fun getMonth(date: CalendarDate): CalendarMonth {
        return getMonth(date.year, date.month)
    }

    override fun getMonth(year: Int, month: Int): CalendarMonth {
        val firstDayCalendar = Calendar.getInstance(utcTimeZone)
        firstDayCalendar.clear()
        firstDayCalendar[Calendar.YEAR] = year
        firstDayCalendar[Calendar.MONTH] = month - 1
        firstDayCalendar[Calendar.DAY_OF_MONTH] = 1
        return getMonth(firstDayCalendar)
    }

    override fun getDayOfWeek(date: CalendarDate): Int {
        return dayInISO8601(date.toCalendar(TimeZone.getDefault())[Calendar.DAY_OF_WEEK])
    }

    override fun plusMonths(from: CalendarMonth, addedMonthsCount: Int): CalendarMonth {
        if (addedMonthsCount <= 0) return from

        val laterMonth = from.toCalendar()
        laterMonth.add(Calendar.MONTH, addedMonthsCount)
        return getMonth(laterMonth)
    }

    override fun minusMonths(from: CalendarMonth, subtractedMonthsCount: Int): CalendarMonth {
        if (subtractedMonthsCount <= 0) return from

        val earlierMonth = from.toCalendar()
        earlierMonth.add(Calendar.MONTH, -subtractedMonthsCount)
        return getMonth(earlierMonth)
    }

    override fun formatWithPattern(utcTimeMillis: Long, pattern: String, locale: Locale): String =
        LegacyCalendarModelImpl.formatWithPattern(utcTimeMillis, pattern, locale)

    override fun parse(date: String, pattern: String): CalendarDate? {
        val dateFormat = SimpleDateFormat(pattern)
        dateFormat.timeZone = utcTimeZone
        dateFormat.isLenient = false
        return try {
            val parsedDate = dateFormat.parse(date) ?: return null
            val calendar = Calendar.getInstance(utcTimeZone)
            calendar.time = parsedDate
            CalendarDate(
                year = calendar[Calendar.YEAR],
                month = calendar[Calendar.MONTH] + 1,
                dayOfMonth = calendar[Calendar.DAY_OF_MONTH],
                utcTimeMillis = calendar.timeInMillis
            )
        } catch (pe: ParseException) {
            null
        }
    }

    companion object {

        /**
         * Formats a UTC timestamp into a string with a given date format pattern.
         *
         * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
         * @param pattern a date format pattern
         * @param locale the [Locale] to use when formatting the given timestamp
         */
        fun formatWithPattern(utcTimeMillis: Long, pattern: String, locale: Locale): String {
            val dateFormat = SimpleDateFormat(pattern, locale)
            dateFormat.timeZone = utcTimeZone
            val calendar = Calendar.getInstance(utcTimeZone)
            calendar.timeInMillis = utcTimeMillis
            return dateFormat.format(calendar.timeInMillis)
        }

        /**
         * Holds a UTC [TimeZone].
         */
        internal val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Returns a given [Calendar] day number as a day representation under ISO-8601, where the first
     * day is defined as Monday.
     */
    private fun dayInISO8601(day: Int): Int {
        val shiftedDay = (day + 6) % 7
        return if (shiftedDay == 0) return /* Sunday */ 7 else shiftedDay
    }

    private fun getMonth(firstDayCalendar: Calendar): CalendarMonth {
        val difference = dayInISO8601(firstDayCalendar[Calendar.DAY_OF_WEEK]) - firstDayOfWeek
        val daysFromStartOfWeekToFirstOfMonth = if (difference < 0) {
            difference + DaysInWeek
        } else {
            difference
        }
        return CalendarMonth(
            year = firstDayCalendar[Calendar.YEAR],
            month = firstDayCalendar[Calendar.MONTH] + 1,
            numberOfDays = firstDayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH),
            daysFromStartOfWeekToFirstOfMonth = daysFromStartOfWeekToFirstOfMonth,
            startUtcTimeMillis = firstDayCalendar.timeInMillis
        )
    }

    private fun CalendarMonth.toCalendar(): Calendar {
        val calendar = Calendar.getInstance(utcTimeZone)
        calendar.timeInMillis = this.startUtcTimeMillis
        return calendar
    }

    private fun CalendarDate.toCalendar(timeZone: TimeZone): Calendar {
        val calendar = Calendar.getInstance(timeZone)
        calendar.clear()
        calendar[Calendar.YEAR] = this.year
        calendar[Calendar.MONTH] = this.month - 1
        calendar[Calendar.DAY_OF_MONTH] = this.dayOfMonth
        return calendar
    }
}
