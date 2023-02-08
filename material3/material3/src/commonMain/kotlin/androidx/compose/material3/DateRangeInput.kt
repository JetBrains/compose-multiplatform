/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangeInputContent(
    stateData: StateData,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean,
) {
    // Obtain the DateInputFormat for the default Locale.
    val defaultLocale = defaultLocale()
    val dateInputFormat = remember(defaultLocale) {
        stateData.calendarModel.getDateInputFormat(defaultLocale)
    }
    val errorDatePattern = getString(Strings.DateInputInvalidForPattern)
    val errorDateOutOfYearRange = getString(Strings.DateInputInvalidYearRange)
    val errorInvalidNotAllowed = getString(Strings.DateInputInvalidNotAllowed)
    val errorInvalidRange = getString(Strings.DateRangeInputInvalidRangeInput)
    val dateInputValidator = remember(dateInputFormat, dateFormatter) {
        DateInputValidator(
            stateData = stateData,
            dateInputFormat = dateInputFormat,
            dateFormatter = dateFormatter,
            dateValidator = dateValidator,
            errorDatePattern = errorDatePattern,
            errorDateOutOfYearRange = errorDateOutOfYearRange,
            errorInvalidNotAllowed = errorInvalidNotAllowed,
            errorInvalidRangeInput = errorInvalidRange
        )
    }
    Row(
        modifier = Modifier.padding(paddingValues = InputTextFieldPadding),
        horizontalArrangement = Arrangement.spacedBy(TextFieldSpacing)
    ) {
        DateInputTextField(
            modifier = Modifier.weight(0.5f),
            stateData = stateData,
            initialDate = stateData.selectedStartDate.value,
            onDateChanged = { date -> stateData.selectedStartDate.value = date },
            inputIdentifier = InputIdentifier.StartDateInput,
            dateInputValidator = dateInputValidator,
            dateInputFormat = dateInputFormat,
            locale = defaultLocale
        )
        DateInputTextField(
            modifier = Modifier.weight(0.5f),
            stateData = stateData,
            initialDate = stateData.selectedEndDate.value,
            onDateChanged = { date -> stateData.selectedEndDate.value = date },
            inputIdentifier = InputIdentifier.EndDateInput,
            dateInputValidator = dateInputValidator,
            dateInputFormat = dateInputFormat,
            locale = defaultLocale
        )
    }
}

private val TextFieldSpacing = 8.dp
