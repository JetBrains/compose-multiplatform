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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Creates a [DateRangePickerState] for a [DateRangePicker] that is remembered across compositions.
 *
 * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of a start date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of an end date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a month to be displayed to the user. By default, in case an
 * `initialSelectedStartDateMillis` is provided, the initial displayed month would be the month of
 * the selected date. Otherwise, in case `null` is provided, the displayed month would be the
 * current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 */
@Composable
@ExperimentalMaterial3Api
internal fun rememberDateRangePickerState(
    @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? =
        initialSelectedStartDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker
): DateRangePickerState = rememberSaveable(
    saver = DateRangePickerState.Saver()
) {
    DateRangePickerState(
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode
    )
}

/**
 * A state object that can be hoisted to observe the date picker state. See
 * [rememberDateRangePickerState].
 *
 * The state's [selectedStartDateMillis] and [selectedEndDateMillis] will provide timestamps for the
 * _beginning_ of the selected days (i.e. midnight in _UTC_ milliseconds from the epoch).
 */
@ExperimentalMaterial3Api
@Stable
internal class DateRangePickerState private constructor(internal val stateData: StateData) {

    /**
     * Constructs a DateRangePickerState.
     *
     * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of a start date. Provide a `null` to indicate no selection.
     * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of an end date. Provide a `null` to indicate no selection.
     * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of a month to be displayed to the user. By default, in case
     * an `initialSelectedStartDateMillis` is provided, the initial displayed month would be the
     * month of the selected date. Otherwise, in case `null` is provided, the displayed month would
     * be the current one.
     * @param yearRange an [IntRange] that holds the year range that the date picker will be limited
     * to
     * @param initialDisplayMode an initial [DisplayMode] that this state will hold
     * @see rememberDatePickerState
     * @throws [IllegalArgumentException] if the initial selected date or displayed month represent
     * a year that is out of the year range.
     */
    constructor(
        @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long?,
        @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long?,
        @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
        yearRange: IntRange,
        initialDisplayMode: DisplayMode
    ) : this(
        StateData(
            initialSelectedStartDateMillis = initialSelectedStartDateMillis,
            initialSelectedEndDateMillis = initialSelectedEndDateMillis,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            initialDisplayMode = initialDisplayMode,
        )
    )

    /**
     * A timestamp that represents the selected range start date.
     *
     * The timestamp would hold a value for the _start_ of the day in _UTC_ milliseconds from the
     * epoch.
     *
     * In case a start date was not selected or provided, the state will hold a `null` value.
     */
    @get:Suppress("AutoBoxing")
    val selectedStartDateMillis by derivedStateOf {
        stateData.selectedStartDate?.utcTimeMillis
    }

    /**
     * A timestamp that represents the selected range end date.
     *
     * The timestamp would hold a value for the _start_ of the day in _UTC_ milliseconds from the
     * epoch.
     *
     * In case an end date was not selected or provided, the state will hold a `null` value.
     */
    @get:Suppress("AutoBoxing")
    val selectedEndDateMillis by derivedStateOf {
        stateData.selectedEndDate?.utcTimeMillis
    }

    /**
     * A mutable state of [DisplayMode] that represents the current display mode of the UI
     * (i.e. picker or input).
     */
    var displayMode by stateData.displayMode

    companion object {
        /**
         * The default [Saver] implementation for [DateRangePickerState].
         */
        fun Saver(): Saver<DateRangePickerState, *> = Saver(
            save = { with(StateData.Saver()) { save(it.stateData) } },
            restore = { value ->
                DateRangePickerState(with(StateData.Saver()) { restore(value)!! })
            }
        )
    }
}
