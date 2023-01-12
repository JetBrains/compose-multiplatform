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

import android.os.Build
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Creates a [CalendarModel] to be used by the date picker.
 */
@ExperimentalMaterial3Api
internal actual fun createCalendarModel(): CalendarModel {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        CalendarModelImpl()
    } else {
        LegacyCalendarModelImpl()
    }
}

/**
 * Formats a UTC timestamp into a string with a given date format skeleton.
 *
 * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
 * @param skeleton a date format skeleton
 * @param locale the [Locale] to use when formatting the given timestamp
 */
@ExperimentalMaterial3Api
internal actual fun formatWithSkeleton(
    utcTimeMillis: Long,
    skeleton: String,
    locale: Locale
): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        CalendarModelImpl.format(utcTimeMillis, skeleton, locale)
    } else {
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        val dateFormat = SimpleDateFormat(pattern, locale)
        dateFormat.timeZone = LegacyCalendarModelImpl.utcTimeZone
        val calendar = Calendar.getInstance(LegacyCalendarModelImpl.utcTimeZone)
        calendar.timeInMillis = utcTimeMillis
        dateFormat.format(calendar.timeInMillis)
    }
}
