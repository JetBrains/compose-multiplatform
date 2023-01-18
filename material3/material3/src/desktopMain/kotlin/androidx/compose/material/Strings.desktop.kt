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

@Composable
internal actual fun getString(string: Strings): String {
    return when (string) {
        Strings.NavigationMenu -> "Navigation menu"
        Strings.CloseDrawer -> "Close navigation menu"
        Strings.CloseSheet -> "Close sheet"
        Strings.DefaultErrorMessage -> "Invalid input"
        Strings.SliderRangeStart -> "Range Start"
        Strings.SliderRangeEnd -> "Range End"
        Strings.Dialog -> "Dialog"
        Strings.MenuExpanded -> "Expanded"
        Strings.MenuCollapsed -> "Collapsed"
        Strings.SnackbarDismiss -> "Dismiss"
        Strings.SearchBarSearch -> "Search"
        Strings.SuggestionsAvailable -> "Suggestions below"
        Strings.DatePickerTitle -> "Select date"
        Strings.DatePickerHeadline -> "Selected date"
        Strings.DatePickerYearPickerPaneTitle -> "Year picker visible"
        Strings.DatePickerSwitchToYearSelection -> "Switch to selecting a year"
        Strings.DatePickerSwitchToDaySelection -> "Swipe to select a year, or tap to switch " +
            "back to selecting a day"
        Strings.DatePickerSwitchToNextMonth -> "Change to next month"
        Strings.DatePickerSwitchToPreviousMonth -> "Change to previous month"
        Strings.DatePickerNavigateToYearDescription -> "Navigate to year %1$"
        Strings.DatePickerHeadlineDescription -> "Current selection: %1$"
        Strings.DatePickerNoSelectionDescription -> "None"
        Strings.DateInputTitle -> "Select date"
        Strings.DateInputHeadline -> "Entered date"
        Strings.DateInputLabel -> "Date"
        Strings.DateInputHeadlineDescription -> "Entered date: %1$"
        Strings.DateInputNoInputHeadlineDescription -> "None"
        Strings.DateInputInvalidNotAllowed -> "Date not allowed: %1$"
        Strings.DateInputInvalidForPattern -> "Date does not match expected pattern: %1$"
        Strings.DateInputInvalidYearRange -> "Date out of expected year range %1$ - %2$"
        Strings.TooltipLongPressLabel -> "Show tooltip"
        else -> ""
    }
}
