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
import androidx.compose.runtime.ReadOnlyComposable
import java.util.Locale

/**
 * Returns a [CalendarModel] to be used by the date picker.
 */
@ExperimentalMaterial3Api
internal actual fun CalendarModel(): CalendarModel = LegacyCalendarModelImpl()

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
    // Note: there is no equivalent in Java for Android's DateFormat.getBestDateTimePattern.
    // The JDK SimpleDateFormat expects a pattern, so the results will be "2023Jan7",
    // "2023January", etc. in case a skeleton holds an actual ICU skeleton and not a pattern.
    return LegacyCalendarModelImpl.formatWithPattern(
        utcTimeMillis = utcTimeMillis,
        pattern = skeleton,
        locale = locale
    )
}

/**
 * A composable function that returns the default [Locale].
 */
@Composable
@ReadOnlyComposable
@ExperimentalMaterial3Api
internal actual fun defaultLocale(): Locale = Locale.getDefault()
