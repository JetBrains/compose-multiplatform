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

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class DatePickerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dateSelectionWithInitialDate() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 1)
            datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialDateMillis,
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DatePicker(state = datePickerState)
        }

        // Select the 11th day of the displayed month is selected.
        rule.onNodeWithText("11").assertIsSelected()
        rule.onNodeWithText("May 11, 2010").assertExists()
    }

    @Test
    fun dateSelection() {
        lateinit var defaultHeadline: String
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DatePickerHeadline)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState = rememberDatePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DatePicker(state = datePickerState)
        }

        rule.onNodeWithText(defaultHeadline).assertExists()

        // Select the 27th day of the displayed month.
        rule.onNodeWithText("27").assertIsNotSelected()
        rule.onNodeWithText("27").performClick()

        rule.runOnIdle {
            assertThat(datePickerState.selectedDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 1,
                    dayOfMonth = 27
                )
            )
        }

        rule.onNodeWithText(defaultHeadline).assertDoesNotExist()
        rule.onNodeWithText("Jan 27, 2019").assertExists()
        rule.onNodeWithText("27").assertIsSelected()
    }

    @Test
    fun invalidDateSelection() {
        lateinit var defaultHeadline: String
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DatePickerHeadline)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState = rememberDatePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DatePicker(state = datePickerState,
                // All dates are invalid for the sake of this test.
                dateValidator = { false }
            )
        }

        rule.onNodeWithText(defaultHeadline).assertExists()

        // Select the 27th day of the displayed month.
        rule.onNodeWithText("27").performClick()

        rule.runOnIdle {
            assertThat(datePickerState.selectedDateMillis).isNull()
        }

        rule.onNodeWithText(defaultHeadline).assertExists()
    }

    @Test
    fun yearSelection() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState = rememberDatePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DatePicker(state = datePickerState)
        }

        rule.onNodeWithText("January 2019").performClick()
        rule.onNodeWithText("2019").assertIsSelected()
        rule.onNodeWithText("2020").performClick()
        // Select the 15th day of the displayed month in 2020.
        rule.onAllNodesWithText("15").onFirst().performClick()

        rule.runOnIdle {
            assertThat(datePickerState.selectedDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2020,
                    month = 1,
                    dayOfMonth = 15
                )
            )
        }

        // Check that if the years are opened again, the last selected year is still marked as such
        rule.onNodeWithText("January 2020").performClick()
        rule.onNodeWithText("2019").assertIsNotSelected()
        rule.onNodeWithText("2020").assertIsSelected()
    }

    @Test
    fun yearRange() {
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            DatePicker(
                state = rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis,
                    // Limit the years selection to 2018-2023
                    yearRange = IntRange(2018, 2023)
                )
            )
        }

        rule.onNodeWithText("January 2019").performClick()
        (2018..2023).forEach { year ->
            rule.onNodeWithText(year.toString()).assertExists()
        }
        rule.onNodeWithText("2017").assertDoesNotExist()
        rule.onNodeWithText("2024").assertDoesNotExist()
    }

    @Test
    fun monthsTraversal() {
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2018, month = 1, dayOfMonth = 1)
            DatePicker(
                state = rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis
                )
            )
        }

        rule.onNodeWithText("January 2018").assertExists()
        // Click the next month arrow button
        rule.onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()

        // Check that the current month's menu button content was changed.
        rule.onNodeWithText("February 2018").assertExists()
        rule.onNodeWithText("January 2018").assertDoesNotExist()

        // Click the previous month arrow button
        rule.onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()

        // Check that we are back to the original month
        rule.onNodeWithText("January 2018").assertExists()
        rule.onNodeWithText("February 2018").assertDoesNotExist()
    }

    @Test
    fun monthsTraversalAtRangeEdges() {
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2018, month = 1, dayOfMonth = 1)
            DatePicker(
                state = rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis,
                    // Limit the years to just 2018
                    yearRange = IntRange(2018, 2018)
                )
            )
        }

        // Assert that we can only click next at the initial state.
        val nextMonthButton =
            rule.onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
        nextMonthButton.assertIsEnabled()
        val previousMonthButton = rule.onNodeWithContentDescription(
            label = "previous",
            substring = true,
            ignoreCase = true
        )
        previousMonthButton.assertIsNotEnabled()

        // Click 11 times next and assert that we can only click previous.
        repeat(11) {
            nextMonthButton.performClick()
            previousMonthButton.assertIsEnabled()
        }
        nextMonthButton.assertIsNotEnabled()
    }

    @Test
    fun switchToDateInput() {
        lateinit var switchToInputDescription: String
        lateinit var dateInputLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            switchToInputDescription = getString(string = Strings.DatePickerSwitchToInputMode)
            dateInputLabel = getString(string = Strings.DateInputLabel)
            DatePicker(state = rememberDatePickerState())
        }

        // Click to switch to DateInput.
        rule.onNodeWithContentDescription(label = switchToInputDescription).performClick()

        rule.waitForIdle()
        rule.onNodeWithText(dateInputLabel).assertIsDisplayed()
        rule.onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        rule.onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun state_initWithSelectedDate() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState = rememberDatePickerState(initialSelectedDateMillis = 1649721600000L)
        }
        with(datePickerState) {
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            assertThat(stateData.displayedMonth).isEqualTo(
                stateData.calendarModel.getMonth(year = 2022, month = 4)
            )
        }
    }

    @Test
    fun state_initWithSelectedDate_roundingToStartDay() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = 1649721600000L + 10000L)
        }
        with(datePickerState) {
            // Assert that the actual selectedDateMillis was rounded down to the start of day
            // timestamp
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            assertThat(stateData.displayedMonth).isEqualTo(
                stateData.calendarModel.getMonth(year = 2022, month = 4)
            )
        }
    }

    @Test
    fun state_initWithSelectedDateAndNullMonth() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = 1649721600000L,
                initialDisplayedMonthMillis = null
            )
        }

        with(datePickerState) {
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            // Assert that the displayed month is the current month as of today.
            assertThat(stateData.displayedMonth).isEqualTo(
                stateData.calendarModel.getMonth(stateData.calendarModel.today.utcTimeMillis)
            )
        }
    }

    @Test
    fun state_initWithNulls() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = null,
                initialDisplayedMonthMillis = null
            )
        }

        with(datePickerState) {
            assertThat(selectedDateMillis).isNull()
            // Assert that the displayed month is the current month as of today.
            assertThat(stateData.displayedMonth).isEqualTo(
                stateData.calendarModel.getMonth(stateData.calendarModel.today.utcTimeMillis)
            )
        }
    }

    @Test
    fun state_restoresDatePickerState() {
        val restorationTester = StateRestorationTester(rule)
        var datePickerState: DatePickerState? = null
        restorationTester.setContent {
            datePickerState = rememberDatePickerState()
        }

        with(datePickerState!!) {
            val date =
                stateData.calendarModel.getCanonicalDate(1649721600000L) // 04/12/2022
            val displayedMonth = stateData.calendarModel.getMonth(date)
            rule.runOnIdle {
                stateData.selectedStartDate.value = date
                stateData.displayedMonth = displayedMonth
            }

            datePickerState = null

            restorationTester.emulateSavedInstanceStateRestore()

            rule.runOnIdle {
                assertThat(stateData.selectedStartDate.value).isEqualTo(date)
                assertThat(stateData.displayedMonth).isEqualTo(displayedMonth)
                assertThat(datePickerState!!.selectedDateMillis).isEqualTo(1649721600000L)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialDateOutOfBounds() {
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2051, month = 5, dayOfMonth = 11)
            rememberDatePickerState(
                initialSelectedDateMillis = initialDateMillis,
                yearRange = IntRange(2000, 2050)
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialDisplayedMonthOutObBounds() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 1999, month = 1, dayOfMonth = 1)
            datePickerState = rememberDatePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis,
                yearRange = IntRange(2000, 2050)
            )
            DatePicker(state = datePickerState)
        }
    }

    @Test
    fun defaultSemantics() {
        val selectedDateInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
        val monthInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 1)
        lateinit var expectedHeadlineStringFormat: String
        rule.setMaterialContent(lightColorScheme()) {
            // e.g. "Current selection: %1$s"
            expectedHeadlineStringFormat = getString(Strings.DatePickerHeadlineDescription)
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDateInUtcMillis,
                    initialDisplayedMonthMillis = monthInUtcMillis
                )
            )
        }

        val fullDateDescription = formatWithSkeleton(
            selectedDateInUtcMillis,
            DatePickerDefaults.YearMonthWeekdayDaySkeleton,
            Locale.US
        )

        rule.onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithText("May 2010")
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithText("11")
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
            .assertContentDescriptionEquals(fullDateDescription)
        rule.onNodeWithText("May 11, 2010")
            .assertContentDescriptionEquals(
                expectedHeadlineStringFormat.format(fullDateDescription)
            )
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
