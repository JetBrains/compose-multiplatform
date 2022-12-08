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
package androidx.compose.material3.samples

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DatePickerSample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Here we just update a Text with the selected timestamp on every selection.
        // In a real app, you may consider composing this date picker in a dialog and read the value
        // when the confirm button is clicked.
        val datePickerState = rememberDatePickerState()
        DatePicker(datePickerState = datePickerState, modifier = Modifier.padding(16.dp))
        Text("Selected date timestamp: ${datePickerState.selectedDateMillis ?: "no selection"}")
    }
}

@SuppressLint("ClassVerificationFailure")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DatePickerWithDateValidatorSample() {
    val datePickerState = rememberDatePickerState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DatePicker(
            datePickerState = datePickerState,
            // Blocks Sunday and Saturday from being selected.
            dateValidator = { utcDateInMills ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val dayOfWeek = Instant.ofEpochMilli(utcDateInMills).atZone(ZoneId.of("UTC"))
                        .toLocalDate().dayOfWeek
                    dayOfWeek != DayOfWeek.SUNDAY && dayOfWeek != DayOfWeek.SATURDAY
                } else {
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = utcDateInMills
                    calendar[Calendar.DAY_OF_WEEK] != Calendar.SUNDAY &&
                        calendar[Calendar.DAY_OF_WEEK] != Calendar.SATURDAY
                }
            }
        )
        Text("Selected date timestamp: ${datePickerState.selectedDateMillis ?: "no selection"}")
    }
}
