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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/date-pickers/overview" class="external" target="_blank">Material Design date range picker</a>.
 *
 * Date range pickers let people select a range of dates and can be embedded into Dialogs.
 *
 * A simple DateRangePicker looks like:
 * @sample androidx.compose.material3.samples.DateRangePickerSample
 *
 * @param state state of the date range picker. See [rememberDateRangePickerState].
 * @param modifier the [Modifier] to be applied to this date range picker
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param dateValidator a lambda that takes a date timestamp and return true if the date is a valid
 * one for selection. Invalid dates will appear disabled in the UI.
 * @param title the title to be displayed in the date range picker
 * @param headline the headline to be displayed in the date range picker
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date
 * range picker in different states. See [DatePickerDefaults.colors].
 */
@ExperimentalMaterial3Api
@Composable
fun DateRangePicker(
    state: DateRangePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerFormatter() },
    dateValidator: (Long) -> Boolean = { true },
    title: (@Composable () -> Unit)? = {
        DateRangePickerDefaults.DateRangePickerTitle(state = state)
    },
    headline: @Composable () -> Unit = {
        DateRangePickerDefaults.DateRangePickerHeadline(
            state,
            dateFormatter
        )
    },
    colors: DatePickerColors = DatePickerDefaults.colors()
) {
    DateEntryContainer(
        modifier = modifier,
        title = title,
        headline = headline,
        // TODO(b/245821979): Add showModeToggle param and us it here for DateEntryModeToggleButton
        modeToggleButton = null,
        headlineTextStyle = MaterialTheme.typography.fromToken(
            DatePickerModalTokens.RangeSelectionHeaderHeadlineFont
        ),
        headerMinHeight = DatePickerModalTokens.RangeSelectionHeaderContainerHeight -
            HeaderHeightOffset,
        headerContentPadding = DateRangePickerHeaderPadding,
        colors = colors
    ) {
        // TODO(b/245821979): Implement using a SwitchableDateEntryContent similar to the DatePicker
        DateRangePickerContent(
            stateData = state.stateData,
            dateFormatter = dateFormatter,
            dateValidator = dateValidator,
            colors = colors
        )
    }
}

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
fun rememberDateRangePickerState(
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
class DateRangePickerState private constructor(internal val stateData: StateData) {

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

/**
 * Contains default values used by the date range pickers.
 */
@ExperimentalMaterial3Api
@Stable
object DateRangePickerDefaults {

    /**
     * A default date picker title composable.
     *
     * @param state a [DatePickerState] that will help determine the title's content
     * @param modifier a [Modifier] to be applied for the title
     * @param contentPadding [PaddingValues] to be applied for the title
     */
    @Composable
    fun DateRangePickerTitle(
        state: DateRangePickerState,
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues(start = DateRangePickerHeaderStartPadding)
    ) {
        when (state.displayMode) {
            DisplayMode.Picker -> Text(
                getString(string = Strings.DateRangePickerTitle),
                modifier = modifier.padding(paddingValues = contentPadding)
            )

            DisplayMode.Input -> Text(
                getString(string = Strings.DateRangeInputTitle),
                modifier = modifier.padding(paddingValues = contentPadding)
            )
        }
    }

    /**
     * A default date picker headline composable lambda that displays a default headline text when
     * there is no date selection, and an actual date string when there is.
     *
     * @param state a [DateRangePickerState] that will help determine the headline
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     * @param contentPadding [PaddingValues] to be applied for the headline row
     */
    @Composable
    fun DateRangePickerHeadline(
        state: DateRangePickerState,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues(start = DateRangePickerHeaderStartPadding)
    ) {
        val startDateText = getString(Strings.DateRangePickerStartHeadline)
        val endDateText = getString(Strings.DateRangePickerEndHeadline)
        DateRangePickerHeadline(
            state = state,
            dateFormatter = dateFormatter,
            modifier = modifier,
            startDateText = startDateText,
            endDateText = endDateText,
            startDatePlaceholder = { Text(text = startDateText) },
            endDatePlaceholder = { Text(text = endDateText) },
            datesDelimiter = { Text(text = "-") },
            contentPadding = contentPadding
        )
    }

    /**
     * A date picker headline composable lambda that displays a default headline text when
     * there is no date selection, and an actual date string when there is.
     *
     * @param state a [DateRangePickerState] that will help determine the headline
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     * @param startDateText a string that, by default, be used as the text content for the
     * [startDatePlaceholder], as well as a prefix for the content description for the selected
     * start date
     * @param endDateText a string that, by default, be used as the text content for the
     * [endDatePlaceholder], as well as a prefix for the content description for the selected
     * end date
     * @param startDatePlaceholder a composable to be displayed as a headline placeholder for the
     * start date (i.e. a [Text] with a "Start date" string)
     * @param endDatePlaceholder a composable to be displayed as a headline placeholder for the end
     * date (i.e a [Text] with an "End date" string)
     * @param datesDelimiter a composable to be displayed as a headline delimiter between the
     * start and the end dates
     * @param contentPadding [PaddingValues] to be applied for the headline row
     */
    @Composable
    private fun DateRangePickerHeadline(
        state: DateRangePickerState,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier,
        startDateText: String,
        endDateText: String,
        startDatePlaceholder: @Composable () -> Unit,
        endDatePlaceholder: @Composable () -> Unit,
        datesDelimiter: @Composable () -> Unit,
        contentPadding: PaddingValues
    ) {
        with(state.stateData) {
            val defaultLocale = defaultLocale()
            val formatterStartDate = dateFormatter.formatDate(
                date = selectedStartDate,
                calendarModel = calendarModel,
                locale = defaultLocale
            )

            val formatterEndDate = dateFormatter.formatDate(
                date = selectedEndDate,
                calendarModel = calendarModel,
                locale = defaultLocale
            )

            val verboseStartDateDescription = dateFormatter.formatDate(
                date = selectedStartDate,
                calendarModel = calendarModel,
                locale = defaultLocale,
                forContentDescription = true
            ) ?: when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                else -> ""
            }

            val verboseEndDateDescription = dateFormatter.formatDate(
                date = selectedEndDate,
                calendarModel = calendarModel,
                locale = defaultLocale,
                forContentDescription = true
            ) ?: when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                else -> ""
            }

            val startHeadlineDescription = "$startDateText: $verboseStartDateDescription"
            val endHeadlineDescription = "$endDateText: $verboseEndDateDescription"

            Row(
                modifier = modifier
                    .padding(paddingValues = contentPadding)
                    .clearAndSetSemantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "$startHeadlineDescription, $endHeadlineDescription"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (formatterStartDate != null) {
                    Text(text = formatterStartDate)
                } else {
                    startDatePlaceholder()
                }
                datesDelimiter()
                if (formatterEndDate != null) {
                    Text(text = formatterEndDate)
                } else {
                    endDatePlaceholder()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerContent(
    stateData: StateData,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean,
    colors: DatePickerColors
) {
    val monthsListState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = stateData.displayedMonthIndex
        )

    val onDateSelected = { dateInMillis: Long ->
        updateDateSelection(stateData, dateInMillis)
    }
    Column {
        WeekDays(colors, stateData.calendarModel)
        VerticalMonthsList(
            onDateSelected = onDateSelected,
            stateData = stateData,
            lazyListState = monthsListState,
            dateFormatter = dateFormatter,
            dateValidator = dateValidator,
            colors = colors
        )
    }
}

/**
 * Composes a continuous vertical scrollable list of calendar months. Each month will appear with a
 * header text indicating the month and the year.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalMonthsList(
    onDateSelected: (dateInMillis: Long) -> Unit,
    stateData: StateData,
    lazyListState: LazyListState,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean,
    colors: DatePickerColors
) {
    val today = stateData.calendarModel.today
    val firstMonth = remember(stateData.yearRange) {
        stateData.calendarModel.getMonth(
            year = stateData.yearRange.first,
            month = 1 // January
        )
    }
    ProvideTextStyle(
        MaterialTheme.typography.fromToken(
            DatePickerModalTokens.RangeSelectionMonthSubheadFont
        )
    ) {
        LazyColumn(state = lazyListState) {
            items(stateData.totalMonthsInRange) {
                val month =
                    stateData.calendarModel.plusMonths(
                        from = firstMonth,
                        addedMonthsCount = it
                    )
                Column(
                    modifier = Modifier.fillParentMaxWidth()
                ) {
                    Text(
                        text = dateFormatter.formatMonthYear(
                            month,
                            stateData.calendarModel,
                            defaultLocale()
                        ) ?: "-",
                        modifier = Modifier.padding(paddingValues = CalendarMonthSubheadPadding),
                        color = colors.subheadContentColor
                    )
                    Month(
                        month = month,
                        onDateSelected = onDateSelected,
                        today = today,
                        stateData = stateData,
                        rangeSelectionEnabled = true,
                        dateValidator = dateValidator,
                        dateFormatter = dateFormatter,
                        colors = colors
                    )
                }
            }
        }
    }
    LaunchedEffect(lazyListState) {
        updateDisplayedMonth(lazyListState, stateData)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun updateDateSelection(
    stateData: StateData,
    dateInMillis: Long
) {
    with(stateData) {
        val date = calendarModel.getCanonicalDate(dateInMillis)
        val currentStart = selectedStartDate
        val currentEnd = selectedEndDate
        if ((currentStart == null && currentEnd == null) ||
            (currentStart != null && currentEnd != null) ||
            (currentStart != null && date < currentStart)
        ) {
            // Reset the selection to "start" only.
            selectedStartDate = date
            selectedEndDate = null
        } else if (currentStart != null && date > currentStart) {
            selectedEndDate = date
        }
    }
}

internal val CalendarMonthSubheadPadding = PaddingValues(
    start = 12.dp,
    top = 20.dp,
    bottom = 8.dp
)

/**
 * a helper class for drawing a range selection. The class holds information about the selected
 * start and end dates as coordinates within the 7 x 6 calendar month grid, as well as information
 * regarding the first and last selected items.
 *
 * A SelectedRangeInfo is created when a [Month] is composed with an `rangeSelectionEnabled` flag.
 */
internal class SelectedRangeInfo(
    val gridCoordinates: Pair<IntOffset, IntOffset>,
    val firstIsSelectionStart: Boolean,
    val lastIsSelectionEnd: Boolean
) {
    companion object {
        /**
         * Calculates the selection coordinates within the current month's grid. The returned [Pair]
         * holds the actual item x & y coordinates within the LazyVerticalGrid, and is later used to
         * calculate the exact offset for drawing the selection rectangles when in range-selection mode.
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun calculateRangeInfo(
            month: CalendarMonth,
            startDate: CalendarDate?,
            endDate: CalendarDate?
        ): SelectedRangeInfo? {
            if (startDate != null && endDate != null) {
                if (startDate.utcTimeMillis > month.endUtcTimeMillis ||
                    endDate.utcTimeMillis < month.startUtcTimeMillis
                ) {
                    return null
                }
                val firstIsSelectionStart = startDate.utcTimeMillis >= month.startUtcTimeMillis
                val lastIsSelectionEnd = endDate.utcTimeMillis <= month.endUtcTimeMillis
                val startGridItemOffset = if (firstIsSelectionStart) {
                    month.daysFromStartOfWeekToFirstOfMonth + startDate.dayOfMonth - 1
                } else {
                    month.daysFromStartOfWeekToFirstOfMonth
                }
                val endGridItemOffset = if (lastIsSelectionEnd) {
                    month.daysFromStartOfWeekToFirstOfMonth + endDate.dayOfMonth - 1
                } else {
                    month.daysFromStartOfWeekToFirstOfMonth + month.numberOfDays - 1
                }

                // Calculate the selected coordinates within the cells grid.
                val startCoordinates = IntOffset(
                    x = startGridItemOffset % DaysInWeek,
                    y = startGridItemOffset / DaysInWeek
                )
                val endCoordinates = IntOffset(
                    x = endGridItemOffset % DaysInWeek,
                    y = endGridItemOffset / DaysInWeek
                )
                return SelectedRangeInfo(
                    Pair(startCoordinates, endCoordinates),
                    firstIsSelectionStart,
                    lastIsSelectionEnd
                )
            }
            return null
        }
    }
}

/**
 * Draws the range selection background.
 *
 * This function is called during a [Modifier.drawWithContent] call when a [Month] is composed with
 * an `rangeSelectionEnabled` flag.
 */
internal fun ContentDrawScope.drawRangeBackground(
    selectedRangeInfo: SelectedRangeInfo,
    color: Color
) {
    // The LazyVerticalGrid is defined to space the items horizontally by
    // DaysHorizontalPadding (e.g. 4.dp). However, as the grid is not limited in
    // width, the spacing can go beyond that value, so this drawing takes this into
    // account.
    // TODO: Use the date's container width and height from the tokens once b/247694457 is resolved.
    val itemContainerWidth = RecommendedSizeForAccessibility.toPx()
    val itemContainerHeight = RecommendedSizeForAccessibility.toPx()
    val itemStateLayerHeight = DatePickerModalTokens.DateStateLayerHeight.toPx()
    val stateLayerVerticalPadding = (itemContainerHeight - itemStateLayerHeight) / 2
    val horizontalSpaceBetweenItems =
        (this.size.width - DaysInWeek * itemContainerWidth) / DaysInWeek

    val (x1, y1) = selectedRangeInfo.gridCoordinates.first
    val (x2, y2) = selectedRangeInfo.gridCoordinates.second
    // The endX and startX are offset to include only half the item's width when dealing with first
    // and last items in the selection in order to keep the selection edges rounded.
    val startX = x1 * (itemContainerWidth + horizontalSpaceBetweenItems) +
        (if (selectedRangeInfo.firstIsSelectionStart) itemContainerWidth / 2 else 0f) +
        horizontalSpaceBetweenItems / 2
    val startY = y1 * itemContainerHeight + stateLayerVerticalPadding
    val endX = x2 * (itemContainerWidth + horizontalSpaceBetweenItems) +
        (if (selectedRangeInfo.lastIsSelectionEnd) itemContainerWidth / 2 else itemContainerWidth) +
        horizontalSpaceBetweenItems / 2
    val endY = y2 * itemContainerHeight + stateLayerVerticalPadding

    // Draw the first row background
    drawRect(
        color = color,
        topLeft = Offset(startX, startY),
        size = Size(
            width = if (y1 == y2) endX - startX else this.size.width - startX,
            height = itemStateLayerHeight
        )
    )

    if (y1 != y2) {
        for (y in y2 - y1 - 1 downTo 1) {
            // Draw background behind the rows in between.
            drawRect(
                color = color,
                topLeft = Offset(0f, startY + (y * itemContainerHeight)),
                size = Size(
                    width = this.size.width,
                    height = itemStateLayerHeight
                )
            )
        }
        // Draw the last row selection background
        drawRect(
            color = color,
            topLeft = Offset(0f, endY),
            size = Size(
                width = endX,
                height = itemStateLayerHeight
            )
        )
    }
}

// Base header paddings that are used for the header part (title + headline). Note that for the
// range picker default title and headline we add additional start padding. The additional paddings
// are added there to allow more flexibility when those composables are provided by a developer.
private val DateRangePickerHeaderPadding = PaddingValues(
    start = 12.dp,
    bottom = 12.dp
)

// Additional start padding for the default headline and title parts.
private val DateRangePickerHeaderStartPadding = 40.dp

// An offset that is applied to the token value for the RangeSelectionHeaderContainerHeight. The
// implementation does not render a "Save" and "X" buttons by default, so we don't take those into
// account when setting the header's max height.
private val HeaderHeightOffset = 60.dp
