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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class DateRangeInputTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dateRangeInput() {
        lateinit var state: DateRangePickerState
        lateinit var pickerStartDateHeadline: String
        lateinit var pickerEndDateHeadline: String
        rule.setMaterialContent(lightColorScheme()) {
            pickerStartDateHeadline = getString(string = Strings.DateRangePickerStartHeadline)
            pickerEndDateHeadline = getString(string = Strings.DateRangePickerEndHeadline)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            state = rememberDateRangePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis,
                initialDisplayMode = DisplayMode.Input
            )
            DateRangePicker(state = state)
        }

        // Expecting 2 nodes with the text "Start date", and 2 with "End date".
        rule.onAllNodesWithText(pickerStartDateHeadline, useUnmergedTree = true)
            .assertCountEquals(2)
        rule.onAllNodesWithText(pickerEndDateHeadline, useUnmergedTree = true)
            .assertCountEquals(2)

        // Enter dates.
        rule.onNodeWithText(pickerStartDateHeadline).performClick().performTextInput("01272019")
        rule.onNodeWithText(pickerEndDateHeadline).performClick().performTextInput("05102020")

        rule.runOnIdle {
            assertThat(state.selectedStartDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 1,
                    dayOfMonth = 27
                )
            )
            assertThat(state.selectedEndDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2020,
                    month = 5,
                    dayOfMonth = 10
                )
            )
        }

        // Now expecting only one node with "Start date", and one with "End date".
        rule.onAllNodesWithText(pickerStartDateHeadline, useUnmergedTree = true)
            .assertCountEquals(1)
        rule.onAllNodesWithText(pickerEndDateHeadline, useUnmergedTree = true)
            .assertCountEquals(1)
        rule.onNodeWithText("Jan 27, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("May 10, 2020", useUnmergedTree = true).assertExists()
    }

    @Test
    fun dateRangeInputWithInitialDates() {
        lateinit var state: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialStartDateMillis =
                dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            val initialEndDateMillis =
                dayInUtcMilliseconds(year = 2020, month = 10, dayOfMonth = 20)
            state = rememberDateRangePickerState(
                initialSelectedStartDateMillis = initialStartDateMillis,
                initialSelectedEndDateMillis = initialEndDateMillis,
                initialDisplayMode = DisplayMode.Input
            )
            DateRangePicker(state = state)
        }

        rule.onNodeWithText("05/11/2010").assertExists()
        rule.onNodeWithText("10/20/2020").assertExists()
        rule.onNodeWithText("May 11, 2010", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Oct 20, 2020", useUnmergedTree = true).assertExists()
    }

    @Test
    fun inputDateNotAllowed() {
        lateinit var startDateRangeInputLabel: String
        lateinit var endDateRangeInputLabel: String
        lateinit var errorMessage: String
        lateinit var state: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            startDateRangeInputLabel = getString(string = Strings.DateRangePickerStartHeadline)
            endDateRangeInputLabel = getString(string = Strings.DateRangePickerEndHeadline)
            errorMessage = getString(string = Strings.DateInputInvalidNotAllowed)
            state = rememberDateRangePickerState(initialDisplayMode = DisplayMode.Input)
            DateRangePicker(state = state,
                // All dates are invalid for the sake of this test.
                dateValidator = { false }
            )
        }

        // Enter dates.
        rule.onNodeWithText(startDateRangeInputLabel).performClick().performTextInput("01272019")
        rule.onNodeWithText(endDateRangeInputLabel).performClick().performTextInput("05102020")

        rule.runOnIdle {
            assertThat(state.selectedStartDateMillis).isNull()
            assertThat(state.selectedEndDateMillis).isNull()
        }
        rule.onNodeWithText("01/27/2019")
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(
                expectValue(
                    SemanticsProperties.Error,
                    errorMessage.format("Jan 27, 2019")
                )
            )
        rule.onNodeWithText("05/10/2020")
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(
                expectValue(
                    SemanticsProperties.Error,
                    errorMessage.format("May 10, 2020")
                )
            )
    }

    @Test
    fun outOfOrderDateRange() {
        lateinit var startDateRangeInputLabel: String
        lateinit var endDateRangeInputLabel: String
        lateinit var errorMessage: String
        lateinit var state: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            startDateRangeInputLabel = getString(string = Strings.DateRangePickerStartHeadline)
            endDateRangeInputLabel = getString(string = Strings.DateRangePickerEndHeadline)
            errorMessage = getString(string = Strings.DateRangeInputInvalidRangeInput)
            state = rememberDateRangePickerState(
                // Limit the years selection to 2018-2023
                yearRange = IntRange(2018, 2023),
                initialDisplayMode = DisplayMode.Input
            )
            DateRangePicker(state = state)
        }

        // Enter dates where the start date is later than the end date.
        rule.onNodeWithText(startDateRangeInputLabel).performClick().performTextInput("01272020")
        rule.onNodeWithText(endDateRangeInputLabel).performClick().performTextInput("05102019")

        rule.runOnIdle {
            // Expecting the first stored date to still be valid, and the second one to be null.
            assertThat(state.selectedStartDateMillis).isNotNull()
            assertThat(state.selectedEndDateMillis).isNull()
        }
        rule.onNodeWithText("05/10/2019", useUnmergedTree = true)
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(expectValue(SemanticsProperties.Error, errorMessage))
    }

    @Test
    fun switchToDateRangePicker() {
        lateinit var switchToPickerDescription: String
        lateinit var startDateRangeInputLabel: String
        lateinit var endDateRangeInputLabel: String
        lateinit var pickerStartDateHeadline: String
        lateinit var pickerEndDateHeadline: String
        rule.setMaterialContent(lightColorScheme()) {
            switchToPickerDescription = getString(string = Strings.DatePickerSwitchToCalendarMode)
            startDateRangeInputLabel = getString(string = Strings.DateRangePickerStartHeadline)
            endDateRangeInputLabel = getString(string = Strings.DateRangePickerEndHeadline)
            pickerStartDateHeadline = getString(string = Strings.DateRangePickerStartHeadline)
            pickerEndDateHeadline = getString(string = Strings.DateRangePickerEndHeadline)
            DateRangePicker(
                state = rememberDateRangePickerState(initialDisplayMode = DisplayMode.Input)
            )
        }

        // Click to switch to DateRangePicker.
        rule.onNodeWithContentDescription(label = switchToPickerDescription).performClick()

        rule.waitForIdle()
        rule.onNodeWithText(pickerStartDateHeadline, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(pickerEndDateHeadline, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(startDateRangeInputLabel).assertDoesNotExist()
        rule.onNodeWithText(endDateRangeInputLabel).assertDoesNotExist()
    }

    @Test
    fun defaultSemantics() {
        val startDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
        val endDateMillis = dayInUtcMilliseconds(year = 2010, month = 6, dayOfMonth = 12)
        lateinit var pickerStartDateHeadline: String
        lateinit var pickerEndDateHeadline: String
        rule.setMaterialContent(lightColorScheme()) {
            pickerStartDateHeadline = getString(string = Strings.DateRangePickerStartHeadline)
            pickerEndDateHeadline = getString(string = Strings.DateRangePickerEndHeadline)
            DateRangePicker(
                state = rememberDateRangePickerState(
                    initialSelectedStartDateMillis = startDateMillis,
                    initialSelectedEndDateMillis = endDateMillis,
                    initialDisplayMode = DisplayMode.Input
                )
            )
        }

        val fullStartDateDescription = formatWithSkeleton(
            startDateMillis,
            DatePickerDefaults.YearMonthWeekdayDaySkeleton,
            Locale.US
        )
        val fullEndDateDescription = formatWithSkeleton(
            endDateMillis,
            DatePickerDefaults.YearMonthWeekdayDaySkeleton,
            Locale.US
        )

        val startHeadlineDescription = "$pickerStartDateHeadline: $fullStartDateDescription"
        val endHeadlineDescription = "$pickerEndDateHeadline: $fullEndDateDescription"
        rule.onNodeWithContentDescription("$startHeadlineDescription, $endHeadlineDescription")
            .assertExists()
    }

    // Returns the given date's day as milliseconds from epoch. The returned value is for the day's
    // start on midnight.
    private fun dayInUtcMilliseconds(year: Int, month: Int, dayOfMonth: Int): Long {
        val firstDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        firstDayCalendar.clear()
        firstDayCalendar[Calendar.YEAR] = year
        firstDayCalendar[Calendar.MONTH] = month - 1
        firstDayCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
        return firstDayCalendar.timeInMillis
    }
}
