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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable

@Immutable
@JvmInline
internal value class Strings private constructor(
    @Suppress("unused") private val value: Int = nextId()
) {
    companion object {
        private var id = 0
        private fun nextId() = id++

        val NavigationMenu = Strings()
        val CloseDrawer = Strings()
        val CloseSheet = Strings()
        val DefaultErrorMessage = Strings()
        val ExposedDropdownMenu = Strings()
        val SliderRangeStart = Strings()
        val SliderRangeEnd = Strings()
        val Dialog = Strings()
        val MenuExpanded = Strings()
        val MenuCollapsed = Strings()
        val SnackbarDismiss = Strings()
        val SearchBarSearch = Strings()
        val SuggestionsAvailable = Strings()
        val DatePickerTitle = Strings()
        val DatePickerHeadline = Strings()
        val DatePickerYearPickerPaneTitle = Strings()
        val DatePickerSwitchToYearSelection = Strings()
        val DatePickerSwitchToDaySelection = Strings()
        val DatePickerSwitchToNextMonth = Strings()
        val DatePickerSwitchToPreviousMonth = Strings()
        val DatePickerNavigateToYearDescription = Strings()
        val DatePickerHeadlineDescription = Strings()
        val DatePickerNoSelectionDescription = Strings()
        val DatePickerTodayDescription = Strings()
        val DatePickerScrollToShowLaterYears = Strings()
        val DatePickerScrollToShowEarlierYears = Strings()
        val DateInputTitle = Strings()
        val DateInputHeadline = Strings()
        val DateInputLabel = Strings()
        val DateInputHeadlineDescription = Strings()
        val DateInputNoInputDescription = Strings()
        val DateInputInvalidNotAllowed = Strings()
        val DateInputInvalidForPattern = Strings()
        val DateInputInvalidYearRange = Strings()
        val DatePickerSwitchToCalendarMode = Strings()
        val DatePickerSwitchToInputMode = Strings()
        val DateRangePickerTitle = Strings()
        val DateRangePickerStartHeadline = Strings()
        val DateRangePickerEndHeadline = Strings()
        val DateRangePickerScrollToShowNextMonth = Strings()
        val DateRangePickerScrollToShowPreviousMonth = Strings()
        val DateRangePickerDayInRange = Strings()
        val DateRangeInputTitle = Strings()
        val DateRangeInputInvalidRangeInput = Strings()
        val TooltipLongPressLabel = Strings()
        val TimePickerAM = Strings()
        val TimePickerPM = Strings()
        val TimePickerPeriodToggle = Strings()
        val TimePickerHourSelection = Strings()
        val TimePickerMinuteSelection = Strings()
        val TimePickerHourSuffix = Strings()
        val TimePicker24HourSuffix = Strings()
        val TimePickerMinuteSuffix = Strings()
        val TimePickerHour = Strings()
        val TimePickerMinute = Strings()
        val TooltipPaneDescription = Strings()
    }
}

@Composable
@ReadOnlyComposable
internal expect fun getString(string: Strings): String

@Composable
@ReadOnlyComposable
internal expect fun getString(string: Strings, vararg formatArgs: Any): String
