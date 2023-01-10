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
        Strings.DatePickerSwitchToYearSelection -> "Switch to selecting a year"
        Strings.DatePickerSwitchToDaySelection -> "Switch to selecting a day"
        Strings.DatePickerSwitchToNextMonth -> "Change to next month"
        Strings.DatePickerSwitchToPreviousMonth -> "Change to previous month"
        else -> ""
    }
}
