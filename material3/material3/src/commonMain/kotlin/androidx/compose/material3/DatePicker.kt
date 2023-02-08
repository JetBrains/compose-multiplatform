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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.lang.Integer.max
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

// TODO: External preview image.
/**
 * <a href="https://m3.material.io/components/date-pickers/overview" class="external" target="_blank">Material Design date picker</a>.
 *
 * Date pickers let people select a date and preferably should be embedded into Dialogs.
 * See [DatePickerDialog].
 *
 * By default, a date picker lets you pick a date via a calendar UI. However, it also allows
 * switching into a date input mode for a manual entry of dates using the numbers on a keyboard.
 *
 * A simple DatePicker looks like:
 * @sample androidx.compose.material3.samples.DatePickerSample
 *
 * A DatePicker with an initial UI of a date input mode looks like:
 * @sample androidx.compose.material3.samples.DateInputSample
 *
 * A DatePicker with validation that blocks certain days from being selected looks like:
 * @sample androidx.compose.material3.samples.DatePickerWithDateValidatorSample
 *
 * @param state state of the date picker. See [rememberDatePickerState].
 * @param modifier the [Modifier] to be applied to this date picker
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param dateValidator a lambda that takes a date timestamp and return true if the date is a valid
 * one for selection. Invalid dates will appear disabled in the UI.
 * @param title the title to be displayed in the date picker
 * @param headline the headline to be displayed in the date picker
 * @param showModeToggle indicates if this DatePicker should show a mode toggle action that
 * transforms it into a date input
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date
 * picker in different states. See [DatePickerDefaults.colors].
 */
@ExperimentalMaterial3Api
@Composable
fun DatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerFormatter() },
    dateValidator: (Long) -> Boolean = { true },
    title: (@Composable () -> Unit)? = { DatePickerDefaults.DatePickerTitle(state) },
    headline: @Composable () -> Unit = {
        DatePickerDefaults.DatePickerHeadline(
            state,
            dateFormatter
        )
    },
    showModeToggle: Boolean = true,
    colors: DatePickerColors = DatePickerDefaults.colors()
) {
    DateEntryContainer(
        modifier = modifier,
        title = title,
        headline = headline,
        modeToggleButton = if (showModeToggle) {
            { DateEntryModeToggleButton(stateData = state.stateData) }
        } else {
            null
        },
        headlineTextStyle = MaterialTheme.typography.fromToken(
            DatePickerModalTokens.HeaderHeadlineFont
        ),
        headerMinHeight = DatePickerModalTokens.HeaderContainerHeight,
        headerContentPadding = DatePickerHeaderPadding,
        colors = colors
    ) {
        SwitchableDateEntryContent(
            state = state,
            dateFormatter = dateFormatter,
            dateValidator = dateValidator,
            colors = colors
        )
    }
}

/**
 * Creates a [DatePickerState] for a [DatePicker] that is remembered across compositions.
 *
 * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a month to be displayed to the user. By default, in case an
 * `initialSelectedDateMillis` is provided, the initial displayed month would be the month of the
 * selected date. Otherwise, in case `null` is provided, the displayed month would be the
 * current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 */
@Composable
@ExperimentalMaterial3Api
fun rememberDatePickerState(
    @Suppress("AutoBoxing") initialSelectedDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker
): DatePickerState = rememberSaveable(
    saver = DatePickerState.Saver()
) {
    DatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode
    )
}

/**
 * A state object that can be hoisted to observe the date picker state. See
 * [rememberDatePickerState].
 *
 * The state's [selectedDateMillis] will provide a timestamp that represents the _start_ of the day.
 */
@ExperimentalMaterial3Api
@Stable
class DatePickerState private constructor(internal val stateData: StateData) {

    /**
     * Constructs a DatePickerState.
     *
     * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of a date. Provide a `null` to indicate no selection. Note
     * that the state's
     * [selectedDateMillis] will provide a timestamp that represents the _start_ of the day, which
     * may be different than the provided initialSelectedDateMillis.
     * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of a month to be displayed to the user. In case `null` is
     * provided, the displayed month would be the current one.
     * @param yearRange an [IntRange] that holds the year range that the date picker will be limited
     * to
     * @param initialDisplayMode an initial [DisplayMode] that this state will hold
     * @see rememberDatePickerState
     * @throws [IllegalArgumentException] if the initial selected date or displayed month represent
     * a year that is out of the year range.
     */
    constructor(
        @Suppress("AutoBoxing") initialSelectedDateMillis: Long?,
        @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
        yearRange: IntRange,
        initialDisplayMode: DisplayMode
    ) : this(
        StateData(
            initialSelectedStartDateMillis = initialSelectedDateMillis,
            initialSelectedEndDateMillis = null,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            initialDisplayMode = initialDisplayMode,
        )
    )

    /**
     * A timestamp that represents the _start_ of the day of the selected date in _UTC_ milliseconds
     * from the epoch.
     *
     * In case no date was selected or provided, the state will hold a `null` value.
     */
    @get:Suppress("AutoBoxing")
    val selectedDateMillis by derivedStateOf {
        stateData.selectedStartDate?.utcTimeMillis
    }

    /**
     * A mutable state of [DisplayMode] that represents the current display mode of the UI
     * (i.e. picker or input).
     */
    var displayMode by stateData.displayMode

    companion object {
        /**
         * The default [Saver] implementation for [DatePickerState].
         */
        fun Saver(): Saver<DatePickerState, *> = Saver(
            save = { with(StateData.Saver()) { save(it.stateData) } },
            restore = { value -> DatePickerState(with(StateData.Saver()) { restore(value)!! }) }
        )
    }
}

/**
 * Contains default values used by the date pickers.
 */
@ExperimentalMaterial3Api
@Stable
object DatePickerDefaults {

    /**
     * Creates a [DatePickerColors] that will potentially animate between the provided colors
     * according to the Material specification.
     *
     * @param containerColor the color used for the date picker's background
     * @param titleContentColor the color used for the date picker's title
     * @param headlineContentColor the color used for the date picker's headline
     * @param weekdayContentColor the color used for the weekday letters
     * @param subheadContentColor the color used for the month and year subhead labels that appear
     * when the date picker is scrolling calendar months vertically
     * @param yearContentColor the color used for the year item when selecting a year
     * @param currentYearContentColor the color used for the current year content when selecting a
     * year
     * @param selectedYearContentColor the color used for the selected year content when selecting a
     * year
     * @param selectedYearContainerColor the color used for the selected year container when
     * selecting a year
     * @param dayContentColor the color used for days content
     * @param disabledDayContentColor the color used for disabled days content
     * @param selectedDayContentColor the color used for selected days content
     * @param disabledSelectedDayContentColor the color used for disabled selected days content
     * @param selectedDayContainerColor the color used for a selected day container
     * @param disabledSelectedDayContainerColor the color used for a disabled selected day container
     * @param todayContentColor the color used for the day that marks the current date
     * @param todayDateBorderColor the color used for the border of the day that marks the current
     * date
     * @param dayInSelectionRangeContentColor the content color used for days that are within a date
     * range selection
     * @param dayInSelectionRangeContainerColor the container color used for days that are within a
     * date range selection
     */
    @Composable
    fun colors(
        containerColor: Color = DatePickerModalTokens.ContainerColor.toColor(),
        titleContentColor: Color = DatePickerModalTokens.HeaderSupportingTextColor.toColor(),
        headlineContentColor: Color = DatePickerModalTokens.HeaderHeadlineColor.toColor(),
        weekdayContentColor: Color = DatePickerModalTokens.WeekdaysLabelTextColor.toColor(),
        subheadContentColor: Color =
            DatePickerModalTokens.RangeSelectionMonthSubheadColor.toColor(),
        yearContentColor: Color =
            DatePickerModalTokens.SelectionYearUnselectedLabelTextColor.toColor(),
        currentYearContentColor: Color = DatePickerModalTokens.DateTodayLabelTextColor.toColor(),
        selectedYearContentColor: Color =
            DatePickerModalTokens.SelectionYearSelectedLabelTextColor.toColor(),
        selectedYearContainerColor: Color =
            DatePickerModalTokens.SelectionYearSelectedContainerColor.toColor(),
        dayContentColor: Color = DatePickerModalTokens.DateUnselectedLabelTextColor.toColor(),
        // TODO: Missing token values for the disabled colors.
        disabledDayContentColor: Color = dayContentColor.copy(alpha = 0.38f),
        selectedDayContentColor: Color = DatePickerModalTokens.DateSelectedLabelTextColor.toColor(),
        // TODO: Missing token values for the disabled colors.
        disabledSelectedDayContentColor: Color = selectedDayContentColor.copy(alpha = 0.38f),
        selectedDayContainerColor: Color =
            DatePickerModalTokens.DateSelectedContainerColor.toColor(),
        // TODO: Missing token values for the disabled colors.
        disabledSelectedDayContainerColor: Color = selectedDayContainerColor.copy(alpha = 0.38f),
        todayContentColor: Color = DatePickerModalTokens.DateTodayLabelTextColor.toColor(),
        todayDateBorderColor: Color =
            DatePickerModalTokens.DateTodayContainerOutlineColor.toColor(),
        dayInSelectionRangeContentColor: Color =
            DatePickerModalTokens.SelectionDateInRangeLabelTextColor.toColor(),
        dayInSelectionRangeContainerColor: Color =
            DatePickerModalTokens.RangeSelectionActiveIndicatorContainerColor.toColor()
    ): DatePickerColors =
        DatePickerColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor,
            headlineContentColor = headlineContentColor,
            weekdayContentColor = weekdayContentColor,
            subheadContentColor = subheadContentColor,
            yearContentColor = yearContentColor,
            currentYearContentColor = currentYearContentColor,
            selectedYearContentColor = selectedYearContentColor,
            selectedYearContainerColor = selectedYearContainerColor,
            dayContentColor = dayContentColor,
            disabledDayContentColor = disabledDayContentColor,
            selectedDayContentColor = selectedDayContentColor,
            disabledSelectedDayContentColor = disabledSelectedDayContentColor,
            selectedDayContainerColor = selectedDayContainerColor,
            disabledSelectedDayContainerColor = disabledSelectedDayContainerColor,
            todayContentColor = todayContentColor,
            todayDateBorderColor = todayDateBorderColor,
            dayInSelectionRangeContentColor = dayInSelectionRangeContentColor,
            dayInSelectionRangeContainerColor = dayInSelectionRangeContainerColor
        )

    /**
     * A default date picker title composable.
     *
     * @param state a [DatePickerState] that will help determine the title's content
     * @param modifier a [Modifier] to be applied for the title
     */
    @Composable
    fun DatePickerTitle(state: DatePickerState, modifier: Modifier = Modifier) {
        when (state.displayMode) {
            DisplayMode.Picker -> Text(
                text = getString(string = Strings.DatePickerTitle),
                modifier = modifier
            )

            DisplayMode.Input -> Text(
                text = getString(string = Strings.DateInputTitle),
                modifier = modifier
            )
        }
    }

    /**
     * A default date picker headline composable that displays a default headline text when there is
     * no date selection, and an actual date string when there is.
     *
     * @param state a [DatePickerState] that will help determine the title's headline
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     */
    @Composable
    fun DatePickerHeadline(
        state: DatePickerState,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier = Modifier
    ) {
        with(state.stateData) {
            val defaultLocale = defaultLocale()
            val formattedDate = dateFormatter.formatDate(
                date = selectedStartDate,
                calendarModel = calendarModel,
                locale = defaultLocale
            )
            val verboseDateDescription = dateFormatter.formatDate(
                date = selectedStartDate,
                calendarModel = calendarModel,
                locale = defaultLocale,
                forContentDescription = true
            ) ?: when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                else -> ""
            }

            val headlineText = formattedDate ?: when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerHeadline)
                DisplayMode.Input -> getString(Strings.DateInputHeadline)
                else -> ""
            }

            val headlineDescription = when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerHeadlineDescription)
                DisplayMode.Input -> getString(Strings.DateInputHeadlineDescription)
                else -> ""
            }.format(verboseDateDescription)

            Text(
                text = headlineText,
                modifier = modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = headlineDescription
                },
                maxLines = 1
            )
        }
    }

    /**
     * Creates and remembers a [FlingBehavior] that will represent natural fling curve with snap to
     * the most visible month in the months list.
     *
     * @param lazyListState a [LazyListState]
     * @param decayAnimationSpec the decay to use
     */
    @Composable
    internal fun rememberSnapFlingBehavior(
        lazyListState: LazyListState,
        decayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay()
    ): FlingBehavior {
        val density = LocalDensity.current
        return remember(density) {
            SnapFlingBehavior(
                lazyListState = lazyListState,
                decayAnimationSpec = decayAnimationSpec,
                snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                density = density
            )
        }
    }

    /** The range of years for the date picker dialogs. */
    val YearRange: IntRange = IntRange(1900, 2100)

    /** The default tonal elevation used for [DatePickerDialog]. */
    val TonalElevation: Dp = DatePickerModalTokens.ContainerElevation

    /** The default shape for date picker dialogs. */
    val shape: Shape @Composable get() = DatePickerModalTokens.ContainerShape.toShape()

    /**
     * A date format skeleton used to format the date picker's year selection menu button (e.g.
     * "March 2021")
     */
    const val YearMonthSkeleton: String = "yMMMM"

    /**
     * A date format skeleton used to format a selected date (e.g. "Mar 27, 2021")
     */
    const val YearAbbrMonthDaySkeleton: String = "yMMMd"

    /**
     * A date format skeleton used to format a selected date to be used as content description for
     * screen readers (e.g. "Saturday, March 27, 2021")
     */
    const val YearMonthWeekdayDaySkeleton: String = "yMMMMEEEEd"
}

/**
 * Represents the colors used by the date picker.
 *
 * See [DatePickerDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@ExperimentalMaterial3Api
@Immutable
class DatePickerColors internal constructor(
    internal val containerColor: Color,
    internal val titleContentColor: Color,
    internal val headlineContentColor: Color,
    internal val weekdayContentColor: Color,
    internal val subheadContentColor: Color,
    private val yearContentColor: Color,
    private val currentYearContentColor: Color,
    private val selectedYearContentColor: Color,
    private val selectedYearContainerColor: Color,
    private val dayContentColor: Color,
    private val disabledDayContentColor: Color,
    private val selectedDayContentColor: Color,
    private val disabledSelectedDayContentColor: Color,
    private val selectedDayContainerColor: Color,
    private val disabledSelectedDayContainerColor: Color,
    private val todayContentColor: Color,
    internal val todayDateBorderColor: Color,
    internal val dayInSelectionRangeContainerColor: Color,
    private val dayInSelectionRangeContentColor: Color,
) {
    /**
     * Represents the content color for a calendar day.
     *
     * @param isToday indicates that the color is for a date that represents today
     * @param selected indicates that the color is for a selected day
     * @param inRange indicates that the day is part of a selection range of days
     * @param enabled indicates that the day is enabled for selection
     */
    @Composable
    internal fun dayContentColor(
        isToday: Boolean,
        selected: Boolean,
        inRange: Boolean,
        enabled: Boolean
    ): State<Color> {
        val target = when {
            selected && enabled -> selectedDayContentColor
            selected && !enabled -> disabledSelectedDayContentColor
            inRange && enabled -> dayInSelectionRangeContentColor
            inRange && !enabled -> disabledDayContentColor
            isToday -> todayContentColor
            enabled -> dayContentColor
            else -> disabledDayContentColor
        }

        return if (inRange) {
            rememberUpdatedState(target)
        } else {
            // Animate the content color only when the day is not in a range.
            animateColorAsState(
                target,
                tween(durationMillis = MotionTokens.DurationShort2.toInt())
            )
        }
    }

    /**
     * Represents the container color for a calendar day.
     *
     * @param selected indicates that the color is for a selected day
     * @param enabled indicates that the day is enabled for selection
     * @param animate whether or not to animate a container color change
     */
    @Composable
    internal fun dayContainerColor(
        selected: Boolean,
        enabled: Boolean,
        animate: Boolean
    ): State<Color> {
        val target = if (selected) {
            if (enabled) selectedDayContainerColor else disabledSelectedDayContainerColor
        } else {
            Color.Transparent
        }
        return if (animate) {
            animateColorAsState(
                target,
                tween(durationMillis = MotionTokens.DurationShort2.toInt())
            )
        } else {
            rememberUpdatedState(target)
        }
    }

    /**
     * Represents the content color for a calendar year.
     *
     * @param currentYear indicates that the color is for a year that represents the current year
     * @param selected indicates that the color is for a selected year
     */
    @Composable
    internal fun yearContentColor(currentYear: Boolean, selected: Boolean): State<Color> {
        val target = if (selected) {
            selectedYearContentColor
        } else if (currentYear) {
            currentYearContentColor
        } else {
            yearContentColor
        }

        return animateColorAsState(
            target,
            tween(durationMillis = MotionTokens.DurationShort2.toInt())
        )
    }

    /**
     * Represents the container color for a calendar year.
     *
     * @param selected indicates that the color is for a selected day
     */
    @Composable
    internal fun yearContainerColor(selected: Boolean): State<Color> {
        val target = if (selected) selectedYearContainerColor else Color.Transparent
        return animateColorAsState(
            target,
            tween(durationMillis = MotionTokens.DurationShort2.toInt())
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DatePickerColors) return false
        if (containerColor != other.containerColor) return false
        if (titleContentColor != other.titleContentColor) return false
        if (headlineContentColor != other.headlineContentColor) return false
        if (weekdayContentColor != other.weekdayContentColor) return false
        if (subheadContentColor != other.subheadContentColor) return false
        if (yearContentColor != other.yearContentColor) return false
        if (currentYearContentColor != other.currentYearContentColor) return false
        if (selectedYearContentColor != other.selectedYearContentColor) return false
        if (selectedYearContainerColor != other.selectedYearContainerColor) return false
        if (dayContentColor != other.dayContentColor) return false
        if (disabledDayContentColor != other.disabledDayContentColor) return false
        if (selectedDayContentColor != other.selectedDayContentColor) return false
        if (disabledSelectedDayContentColor != other.disabledSelectedDayContentColor) return false
        if (selectedDayContainerColor != other.selectedDayContainerColor) return false
        if (disabledSelectedDayContainerColor != other.disabledSelectedDayContainerColor) {
            return false
        }
        if (todayContentColor != other.todayContentColor) return false
        if (todayDateBorderColor != other.todayDateBorderColor) return false
        if (dayInSelectionRangeContainerColor != other.dayInSelectionRangeContainerColor) {
            return false
        }
        if (dayInSelectionRangeContentColor != other.dayInSelectionRangeContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + titleContentColor.hashCode()
        result = 31 * result + headlineContentColor.hashCode()
        result = 31 * result + weekdayContentColor.hashCode()
        result = 31 * result + subheadContentColor.hashCode()
        result = 31 * result + yearContentColor.hashCode()
        result = 31 * result + currentYearContentColor.hashCode()
        result = 31 * result + selectedYearContentColor.hashCode()
        result = 31 * result + selectedYearContainerColor.hashCode()
        result = 31 * result + dayContentColor.hashCode()
        result = 31 * result + disabledDayContentColor.hashCode()
        result = 31 * result + selectedDayContentColor.hashCode()
        result = 31 * result + disabledSelectedDayContentColor.hashCode()
        result = 31 * result + selectedDayContainerColor.hashCode()
        result = 31 * result + disabledSelectedDayContainerColor.hashCode()
        result = 31 * result + todayContentColor.hashCode()
        result = 31 * result + todayDateBorderColor.hashCode()
        result = 31 * result + dayInSelectionRangeContainerColor.hashCode()
        result = 31 * result + dayInSelectionRangeContentColor.hashCode()
        return result
    }
}

/**
 * A date formatter used by [DatePicker].
 *
 * The date formatter will apply the best possible localized form of the given skeleton and Locale.
 * A skeleton is similar to, and uses the same format characters as, a Unicode
 * <a href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a> pattern.
 *
 * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
 * `en_US` locale, but "d. MMMM" in the `de_CH` locale.
 *
 * @param yearSelectionSkeleton a date format skeleton used to format the date picker's year
 * selection menu button (e.g. "March 2021").
 * @param selectedDateSkeleton a date format skeleton used to format a selected date (e.g.
 * "Mar 27, 2021")
 * @param selectedDateDescriptionSkeleton a date format skeleton used to format a selected date to
 * be used as content description for screen readers (e.g. "Saturday, March 27, 2021")
 */
@ExperimentalMaterial3Api
@Immutable
class DatePickerFormatter constructor(
    internal val yearSelectionSkeleton: String = DatePickerDefaults.YearMonthSkeleton,
    internal val selectedDateSkeleton: String = DatePickerDefaults.YearAbbrMonthDaySkeleton,
    internal val selectedDateDescriptionSkeleton: String =
        DatePickerDefaults.YearMonthWeekdayDaySkeleton
) {

    internal fun formatMonthYear(
        month: CalendarMonth?,
        calendarModel: CalendarModel,
        locale: Locale
    ): String? {
        if (month == null) return null
        return calendarModel.formatWithSkeleton(month, yearSelectionSkeleton, locale)
    }

    internal fun formatDate(
        date: CalendarDate?,
        calendarModel: CalendarModel,
        locale: Locale,
        forContentDescription: Boolean = false
    ): String? {
        if (date == null) return null
        return calendarModel.formatWithSkeleton(
            date, if (forContentDescription) {
                selectedDateDescriptionSkeleton
            } else {
                selectedDateSkeleton
            },
            locale
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DatePickerFormatter) return false

        if (yearSelectionSkeleton != other.yearSelectionSkeleton) return false
        if (selectedDateSkeleton != other.selectedDateSkeleton) return false
        if (selectedDateDescriptionSkeleton != other.selectedDateDescriptionSkeleton) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yearSelectionSkeleton.hashCode()
        result = 31 * result + selectedDateSkeleton.hashCode()
        result = 31 * result + selectedDateDescriptionSkeleton.hashCode()
        return result
    }
}

/**
 * Represents the different modes that a date picker can be at.
 */
@Immutable
@JvmInline
@ExperimentalMaterial3Api
value class DisplayMode internal constructor(internal val value: Int) {

    companion object {
        /** Date picker mode */
        val Picker = DisplayMode(0)

        /** Date text input mode */
        val Input = DisplayMode(1)
    }

    override fun toString() = when (this) {
        Picker -> "Picker"
        Input -> "Input"
        else -> "Unknown"
    }
}

/**
 * Holds the state's data for the date picker.
 *
 * Note that the internal representation is capable of holding a start and end date. However, the
 * the [DatePickerState] and the [DateRangePickerState] that use this class will only expose
 * publicly the relevant functionality for their purpose.
 *
 * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of a start date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of an end date. Provide a `null` to indicate no selection. This
 * value will be ignored in case it's smaller or equals to the initial start value.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a month to be displayed to the user. In case `null` is provided, the
 * displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @see rememberDatePickerState
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal class StateData constructor(
    initialSelectedStartDateMillis: Long?,
    initialSelectedEndDateMillis: Long?,
    initialDisplayedMonthMillis: Long?,
    val yearRange: IntRange,
    initialDisplayMode: DisplayMode,
) {

    val calendarModel: CalendarModel = CalendarModel()

    /**
     * A mutable state of [CalendarDate] that represents the start date for a selection.
     */
    var selectedStartDate by mutableStateOf(
        if (initialSelectedStartDateMillis != null) {
            val date = calendarModel.getCanonicalDate(
                initialSelectedStartDateMillis
            )
            require(yearRange.contains(date.year)) {
                "The initial selected start date's year (${date.year}) is out of the years range " +
                    "of $yearRange."
            }
            date
        } else {
            null
        }
    )

    /**
     * A mutable state of [CalendarDate] that represents the end date for a selection.
     *
     * Single date selection states that use this [StateData] should always have this as `null`.
     */
    var selectedEndDate by mutableStateOf(
        // Set to null in case the provided value is "undefined" or <= than the start date.
        if (initialSelectedEndDateMillis != null &&
            initialSelectedStartDateMillis != null &&
            initialSelectedEndDateMillis > initialSelectedStartDateMillis
        ) {
            val date = calendarModel.getCanonicalDate(
                initialSelectedEndDateMillis
            )
            require(yearRange.contains(date.year)) {
                "The initial selected end date's year (${date.year}) is out of the years range " +
                    "of $yearRange."
            }
            date
        } else {
            null
        }
    )

    /**
     * A mutable state for the month that is displayed to the user. In case an initial month was not
     * provided, the current month will be the one to be displayed.
     */
    var displayedMonth by mutableStateOf(
        if (initialDisplayedMonthMillis != null) {
            val month = calendarModel.getMonth(initialDisplayedMonthMillis)
            require(yearRange.contains(month.year)) {
                "The initial display month's year (${month.year}) is out of the years range of " +
                    "$yearRange."
            }
            month
        } else {
            currentMonth
        }
    )

    /**
     * The current [CalendarMonth] that represents the present's day month.
     */
    val currentMonth: CalendarMonth
        get() = calendarModel.getMonth(calendarModel.today)

    /**
     * A mutable state of [DisplayMode] that represents the current display mode of the UI
     * (i.e. picker or input).
     */
    var displayMode = mutableStateOf(initialDisplayMode)

    /**
     * The displayed month index within the total months at the defined years range.
     *
     * @see [displayedMonth]
     * @see [yearRange]
     */
    val displayedMonthIndex: Int
        get() = displayedMonth.indexIn(yearRange)

    /**
     * The total month count for the defined years range.
     *
     * @see [yearRange]
     */
    val totalMonthsInRange: Int
        get() = (yearRange.last - yearRange.first + 1) * 12

    fun isInRange(date: Long): Boolean {
        return date >= (selectedStartDate?.utcTimeMillis ?: Long.MAX_VALUE) &&
            date <= (selectedEndDate?.utcTimeMillis ?: Long.MIN_VALUE)
    }

    companion object {
        /**
         * A [Saver] implementation for [StateData].
         */
        fun Saver(): Saver<StateData, Any> = listSaver(
            save = {
                listOf(
                    it.selectedStartDate?.utcTimeMillis,
                    it.selectedEndDate?.utcTimeMillis,
                    it.displayedMonth.startUtcTimeMillis,
                    it.yearRange.first,
                    it.yearRange.last,
                    it.displayMode.value.value
                )
            },
            restore = { value ->
                StateData(
                    initialSelectedStartDateMillis = value[0] as Long?,
                    initialSelectedEndDateMillis = value[1] as Long?,
                    initialDisplayedMonthMillis = value[2] as Long?,
                    yearRange = IntRange(value[3] as Int, value[4] as Int),
                    initialDisplayMode = DisplayMode(value[5] as Int)
                )
            }
        )
    }
}

/**
 * A base container for the date picker and the date input. This container composes the top common
 * area of the UI, and accepts [content] for the actual calendar picker or text field input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateEntryContainer(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    headline: @Composable () -> Unit,
    modeToggleButton: (@Composable () -> Unit)?,
    colors: DatePickerColors,
    headlineTextStyle: TextStyle,
    headerMinHeight: Dp,
    headerContentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .sizeIn(minWidth = DatePickerModalTokens.ContainerWidth)
            .padding(DatePickerHorizontalPadding)
    ) {
        DatePickerHeader(
            modifier = Modifier,
            title = title,
            titleContentColor = colors.titleContentColor,
            headlineContentColor = colors.headlineContentColor,
            minHeight = headerMinHeight,
            contentPadding = headerContentPadding
        ) {
            ProvideTextStyle(value = headlineTextStyle, content = headline)
            modeToggleButton?.invoke()
        }
        Divider()
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateEntryModeToggleButton(stateData: StateData) {
    with(stateData) {
        if (displayMode.value == DisplayMode.Picker) {
            IconButton(onClick = {
                displayMode.value = DisplayMode.Input
            }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = getString(Strings.DatePickerSwitchToInputMode)
                )
            }
        } else {
            IconButton(
                onClick = {
                    // Update the displayed month, if needed, and change the mode to a
                    // date-picker.
                    selectedStartDate?.let { displayedMonth = calendarModel.getMonth(it) }
                    displayMode.value = DisplayMode.Picker
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = getString(Strings.DatePickerSwitchToCalendarMode)
                )
            }
        }
    }
}

/**
 * Date entry content that displays a [DatePickerContent] or a [DateInputContent] according to the
 * state's display mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchableDateEntryContent(
    state: DatePickerState,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean,
    colors: DatePickerColors
) {
    // TODO(b/266480386): Apply the motion spec for this once we have it. Consider replacing this
    //  with AnimatedContent when it's out of experimental.
    Crossfade(targetState = state.displayMode, animationSpec = spring()) { mode ->
        when (mode) {
            DisplayMode.Picker -> DatePickerContent(
                stateData = state.stateData,
                dateFormatter = dateFormatter,
                dateValidator = dateValidator,
                colors = colors
            )

            DisplayMode.Input -> DateInputContent(
                stateData = state.stateData,
                dateFormatter = dateFormatter,
                dateValidator = dateValidator,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerContent(
    stateData: StateData,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean,
    colors: DatePickerColors
) {
    val monthsListState =
        rememberLazyListState(initialFirstVisibleItemIndex = stateData.displayedMonthIndex)
    val coroutineScope = rememberCoroutineScope()

    val onDateSelected = { dateInMillis: Long ->
        stateData.selectedStartDate =
            stateData.calendarModel.getCanonicalDate(dateInMillis)
    }

    var yearPickerVisible by rememberSaveable { mutableStateOf(false) }
    val defaultLocale = defaultLocale()
    Column {
        MonthsNavigation(
            nextAvailable = monthsListState.canScrollForward,
            previousAvailable = monthsListState.canScrollBackward,
            yearPickerVisible = yearPickerVisible,
            yearPickerText = dateFormatter.formatMonthYear(
                month = stateData.displayedMonth,
                calendarModel = stateData.calendarModel,
                locale = defaultLocale
            ) ?: "-",
            onNextClicked = {
                coroutineScope.launch {
                    monthsListState.animateScrollToItem(
                        monthsListState.firstVisibleItemIndex + 1
                    )
                }
            },
            onPreviousClicked = {
                coroutineScope.launch {
                    monthsListState.animateScrollToItem(
                        monthsListState.firstVisibleItemIndex - 1
                    )
                }
            },
            onYearPickerButtonClicked = { yearPickerVisible = !yearPickerVisible }
        )

        Box {
            Column {
                WeekDays(colors, stateData.calendarModel)
                HorizontalMonthsList(
                    onDateSelected = onDateSelected,
                    stateData = stateData,
                    lazyListState = monthsListState,
                    dateFormatter = dateFormatter,
                    dateValidator = dateValidator,
                    colors = colors
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = yearPickerVisible,
                modifier = Modifier.clipToBounds(),
                enter = expandVertically() + fadeIn(initialAlpha = 0.6f),
                exit = shrinkVertically() + fadeOut()
            ) {
                // Apply a paneTitle to make the screen reader focus on a relevant node after this
                // column is hidden and disposed.
                // TODO(b/186443263): Have the screen reader focus on a year in the list when the
                //  list is revealed.
                val yearsPaneTitle = getString(Strings.DatePickerYearPickerPaneTitle)
                Column(modifier = Modifier.semantics { paneTitle = yearsPaneTitle }) {
                    YearPicker(
                        // Keep the height the same as the monthly calendar + weekdays height, and
                        // take into account the thickness of the divider that will be composed
                        // below it.
                        modifier = Modifier.requiredHeight(
                            RecommendedSizeForAccessibility * (MaxCalendarRows + 1) -
                                DividerDefaults.Thickness
                        ),
                        onYearSelected = { year ->
                            // Switch back to the monthly calendar and scroll to the selected year.
                            yearPickerVisible = !yearPickerVisible
                            coroutineScope.launch {
                                // Scroll to the selected year (maintaining the month of year).
                                // A LaunchEffect at the MonthsList will take care of rest and will
                                // update the state's displayedMonth to the month we scrolled to.
                                with(stateData) {
                                    monthsListState.scrollToItem(
                                        (year - yearRange.first) * 12 + displayedMonth.month - 1
                                    )
                                }
                            }
                        },
                        colors = colors,
                        stateData = stateData
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
internal fun DatePickerHeader(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    titleContentColor: Color,
    headlineContentColor: Color,
    minHeight: Dp,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit
) {
    // Apply a defaultMinSize only when the title is not null.
    val heightModifier =
        if (title != null) {
            Modifier.defaultMinSize(minHeight = minHeight)
        } else {
            Modifier
        }
    Column(
        modifier
            .fillMaxWidth()
            .then(heightModifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (title != null) {
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                val textStyle =
                    MaterialTheme.typography.fromToken(
                        DatePickerModalTokens.HeaderSupportingTextFont
                    )
                ProvideTextStyle(textStyle) {
                    Box(contentAlignment = Alignment.BottomStart) {
                        title()
                    }
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides headlineContentColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Composes a horizontal pageable list of months.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorizontalMonthsList(
    onDateSelected: (dateInMillis: Long) -> Unit,
    stateData: StateData,
    lazyListState: LazyListState,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean,
    colors: DatePickerColors,
) {
    val today = stateData.calendarModel.today
    val firstMonth = remember(stateData.yearRange) {
        stateData.calendarModel.getMonth(
            year = stateData.yearRange.first,
            month = 1 // January
        )
    }
    LazyRow(
        // Apply this to prevent the screen reader from scrolling to the next or previous month, and
        // instead, traverse outside the Month composable when swiping from a focused first or last
        // day of the month.
        modifier = Modifier.semantics {
            horizontalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
        },
        state = lazyListState,
        // TODO(b/264687693): replace with the framework's rememberSnapFlingBehavior(lazyListState)
        //  when promoted to stable
        flingBehavior = DatePickerDefaults.rememberSnapFlingBehavior(lazyListState)
    ) {
        items(stateData.totalMonthsInRange) {
            val month =
                stateData.calendarModel.plusMonths(
                    from = firstMonth,
                    addedMonthsCount = it
                )
            Box(
                modifier = Modifier.fillParentMaxWidth()
            ) {
                Month(
                    month = month,
                    onDateSelected = onDateSelected,
                    today = today,
                    stateData = stateData,
                    rangeSelectionEnabled = false,
                    dateValidator = dateValidator,
                    dateFormatter = dateFormatter,
                    colors = colors
                )
            }
        }
    }

    LaunchedEffect(lazyListState) {
        updateDisplayedMonth(lazyListState, stateData)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal suspend fun updateDisplayedMonth(
    lazyListState: LazyListState,
    stateData: StateData
) {
    snapshotFlow { lazyListState.firstVisibleItemIndex }.collect {
        val yearOffset = lazyListState.firstVisibleItemIndex / 12
        val month = lazyListState.firstVisibleItemIndex % 12 + 1
        with(stateData) {
            if (displayedMonth.month != month ||
                displayedMonth.year != yearRange.first + yearOffset
            ) {
                displayedMonth = calendarModel.getMonth(
                    year = yearRange.first + yearOffset,
                    month = month
                )
            }
        }
    }
}

/**
 * Composes the weekdays letters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeekDays(colors: DatePickerColors, calendarModel: CalendarModel) {
    val firstDayOfWeek = calendarModel.firstDayOfWeek
    val weekdays = calendarModel.weekdayNames
    val dayNames = arrayListOf<Pair<String, String>>()
    // Start with firstDayOfWeek - 1 as the days are 1-based.
    for (i in firstDayOfWeek - 1 until weekdays.size) {
        dayNames.add(weekdays[i])
    }
    for (i in 0 until firstDayOfWeek - 1) {
        dayNames.add(weekdays[i])
    }
    CompositionLocalProvider(LocalContentColor provides colors.weekdayContentColor) {
        val textStyle =
            MaterialTheme.typography.fromToken(DatePickerModalTokens.WeekdaysLabelTextFont)
        ProvideTextStyle(value = textStyle) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minHeight = RecommendedSizeForAccessibility
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayNames.forEach {
                    Box(
                        modifier = Modifier
                            .clearAndSetSemantics { contentDescription = it.first }
                            .size(
                                width = RecommendedSizeForAccessibility,
                                height = RecommendedSizeForAccessibility
                            ),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text = it.second,
                            modifier = Modifier.wrapContentSize(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable that renders a calendar month and displays a date selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Month(
    month: CalendarMonth,
    onDateSelected: (dateInMillis: Long) -> Unit,
    today: CalendarDate,
    stateData: StateData,
    rangeSelectionEnabled: Boolean,
    dateValidator: (Long) -> Boolean,
    dateFormatter: DatePickerFormatter,
    colors: DatePickerColors
) {
    fun isInRange(date: Long): Boolean {
        return rangeSelectionEnabled && stateData.isInRange(date)
    }

    val rangeSelectionInfo: State<SelectedRangeInfo?> = remember(rangeSelectionEnabled) {
        derivedStateOf {
            if (rangeSelectionEnabled) {
                SelectedRangeInfo.calculateRangeInfo(
                    month,
                    stateData.selectedStartDate,
                    stateData.selectedEndDate
                )
            } else {
                null
            }
        }
    }

    val rangeSelectionDrawModifier = if (rangeSelectionEnabled) {
        Modifier.drawWithContent {
            rangeSelectionInfo.value?.let {
                drawRangeBackground(it, colors.dayInSelectionRangeContainerColor)
            }
            drawContent()
        }
    } else {
        Modifier
    }

    val startSelection = stateData.selectedStartDate
    val endSelection = stateData.selectedEndDate
    ProvideTextStyle(
        MaterialTheme.typography.fromToken(DatePickerModalTokens.DateLabelTextFont)
    ) {
        var cellIndex = 0
        Column(
            modifier = Modifier
                .requiredHeight(RecommendedSizeForAccessibility * MaxCalendarRows)
                .then(rangeSelectionDrawModifier),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(MaxCalendarRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(DaysInWeek) {
                        if (cellIndex < month.daysFromStartOfWeekToFirstOfMonth ||
                            cellIndex >=
                            (month.daysFromStartOfWeekToFirstOfMonth + month.numberOfDays)
                        ) {
                            // Empty cell
                            Spacer(
                                modifier = Modifier.requiredSize(
                                    width = RecommendedSizeForAccessibility,
                                    height = RecommendedSizeForAccessibility
                                )
                            )
                        } else {
                            val dayNumber = cellIndex - month.daysFromStartOfWeekToFirstOfMonth
                            val dateInMillis = month.startUtcTimeMillis +
                                (dayNumber * MillisecondsIn24Hours)
                            val isToday = dateInMillis == today.utcTimeMillis
                            val startDateSelected = dateInMillis == startSelection?.utcTimeMillis
                            val endDateSelected = dateInMillis == endSelection?.utcTimeMillis
                            val dayContentDescription = dayContentDescription(
                                rangeSelectionEnabled = rangeSelectionEnabled,
                                isToday = isToday,
                                isStartDate = startDateSelected,
                                isEndDate = endDateSelected
                            )
                            Day(
                                modifier = Modifier.semantics {
                                    role = Role.Button
                                    dayContentDescription?.let { contentDescription = it }
                                },
                                selected = startDateSelected || endDateSelected,
                                onClick = { onDateSelected(dateInMillis) },
                                // Only animate on the first selected day. This is important to
                                // disable when drawing a range marker behind the days on an
                                // end-date selection.
                                animateChecked = startDateSelected,
                                enabled = remember(dateInMillis) {
                                    dateValidator.invoke(dateInMillis)
                                },
                                today = isToday,
                                inRange = remember(dateInMillis, startSelection, endSelection) {
                                    isInRange(dateInMillis)
                                },
                                colors = colors
                            ) {
                                val defaultLocale = defaultLocale()
                                Text(
                                    text = (dayNumber + 1).toLocalString(),
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            formatWithSkeleton(
                                                dateInMillis,
                                                dateFormatter.selectedDateDescriptionSkeleton,
                                                defaultLocale
                                            )
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        cellIndex++
                    }
                }
            }
        }
    }
}

@Composable
private fun dayContentDescription(
    rangeSelectionEnabled: Boolean,
    isToday: Boolean,
    isStartDate: Boolean,
    isEndDate: Boolean
): String? {
    val descriptionBuilder = StringBuilder()
    if (rangeSelectionEnabled) {
        if (isStartDate) {
            descriptionBuilder.append(getString(string = Strings.DateRangePickerStartHeadline))
        } else if (isEndDate) {
            descriptionBuilder.append(getString(string = Strings.DateRangePickerEndHeadline))
        }
    }
    if (isToday) {
        if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append(", ")
        descriptionBuilder.append(getString(string = Strings.DatePickerTodayDescription))
    }
    return if (descriptionBuilder.isEmpty()) null else descriptionBuilder.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day(
    modifier: Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    animateChecked: Boolean,
    enabled: Boolean,
    today: Boolean,
    inRange: Boolean,
    colors: DatePickerColors,
    content: @Composable () -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        // Semantic role is intentionally not set here and left to be set by the caller
        // In the `Month` function above, the implementation checks whether the day is today and
        // sets the content description differently.
        modifier = modifier
        .minimumInteractiveComponentSize()
            .requiredSize(
                DatePickerModalTokens.DateStateLayerWidth,
                DatePickerModalTokens.DateStateLayerHeight
            ),
        enabled = enabled,
        shape = DatePickerModalTokens.DateContainerShape.toShape(),
        color = colors.dayContainerColor(
            selected = selected,
            enabled = enabled,
            animate = animateChecked
        ).value,
        contentColor = colors.dayContentColor(
            isToday = today,
            selected = selected,
            inRange = inRange,
            enabled = enabled,
        ).value,
        border = if (today && !selected) {
            BorderStroke(
                DatePickerModalTokens.DateTodayContainerOutlineWidth,
                colors.todayDateBorderColor
            )
        } else {
            null
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearPicker(
    modifier: Modifier,
    onYearSelected: (year: Int) -> Unit,
    colors: DatePickerColors,
    stateData: StateData
) {
    ProvideTextStyle(
        value = MaterialTheme.typography.fromToken(DatePickerModalTokens.SelectionYearLabelTextFont)
    ) {
        val currentYear = stateData.currentMonth.year
        val displayedYear = stateData.displayedMonth.year
        val lazyGridState =
            rememberLazyGridState(
                // Set the initial index to a few years before the current year to allow quicker
                // selection of previous years.
                initialFirstVisibleItemIndex = max(
                    0, displayedYear - stateData.yearRange.first - YearsInRow
                )
            )
        // Match the years container color to any elevated surface color that is composed under it.
        val containerColor = if (colors.containerColor == MaterialTheme.colorScheme.surface) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)
        } else {
            colors.containerColor
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(YearsInRow),
            modifier = modifier
                .background(containerColor)
                // Apply this to have the screen reader traverse outside the visible list of years
                // and not scroll them by default.
                .semantics {
                    verticalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
                },
            state = lazyGridState,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(YearsVerticalPadding)
        ) {
            items(stateData.yearRange.count()) {
                val selectedYear = it + stateData.yearRange.first
                Year(
                    modifier = Modifier
                        .requiredSize(
                            width = DatePickerModalTokens.SelectionYearContainerWidth,
                            height = DatePickerModalTokens.SelectionYearContainerHeight
                        ),
                    selected = selectedYear == displayedYear,
                    currentYear = selectedYear == currentYear,
                    onClick = { onYearSelected(selectedYear) },
                    colors = colors
                ) {
                    val localizedYear = selectedYear.toLocalString()
                    val description =
                        getString(Strings.DatePickerNavigateToYearDescription).format(localizedYear)
                    Text(
                        text = localizedYear,
                        modifier = Modifier.semantics {
                            contentDescription = description
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Year(
    modifier: Modifier,
    selected: Boolean,
    currentYear: Boolean,
    onClick: () -> Unit,
    colors: DatePickerColors,
    content: @Composable () -> Unit
) {
    val border = remember(currentYear, selected) {
        if (currentYear && !selected) {
            // Use the day's spec to draw a border around the current year.
            BorderStroke(
                DatePickerModalTokens.DateTodayContainerOutlineWidth,
                colors.todayDateBorderColor
            )
        } else {
            null
        }
    }
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = DatePickerModalTokens.SelectionYearStateLayerShape.toShape(),
        color = colors.yearContainerColor(selected = selected).value,
        contentColor = colors.yearContentColor(
            currentYear = currentYear,
            selected = selected
        ).value,
        border = border,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/**
 * A composable that shows a year menu button and a couple of buttons that enable navigation between
 * displayed months.
 */
@Composable
private fun MonthsNavigation(
    nextAvailable: Boolean,
    previousAvailable: Boolean,
    yearPickerVisible: Boolean,
    yearPickerText: String,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onYearPickerButtonClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(MonthYearHeight),
        horizontalArrangement = if (yearPickerVisible) {
            Arrangement.Start
        } else {
            Arrangement.SpaceBetween
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A menu button for selecting a year.
        YearPickerMenuButton(
            onClick = onYearPickerButtonClicked,
            expanded = yearPickerVisible
        ) {
            Text(text = yearPickerText,
                modifier = Modifier.semantics {
                    // Make the screen reader read out updates to the menu button text as the user
                    // navigates the arrows or scrolls to change the displayed month.
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = yearPickerText
                })
        }
        // Show arrows for traversing months (only visible when the year selection is off)
        if (!yearPickerVisible) {
            Row {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                IconButton(onClick = onPreviousClicked, enabled = previousAvailable) {
                    Icon(
                        if (rtl) {
                            Icons.Filled.KeyboardArrowRight
                        } else {
                            Icons.Filled.KeyboardArrowLeft
                        },
                        contentDescription = getString(Strings.DatePickerSwitchToPreviousMonth)
                    )
                }
                IconButton(onClick = onNextClicked, enabled = nextAvailable) {
                    Icon(
                        if (rtl) {
                            Icons.Filled.KeyboardArrowLeft
                        } else {
                            Icons.Filled.KeyboardArrowRight
                        },
                        contentDescription = getString(Strings.DatePickerSwitchToNextMonth)
                    )
                }
            }
        }
    }
}

// TODO: Replace with the official MenuButton when implemented.
@Composable
private fun YearPickerMenuButton(
    onClick: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RectangleShape,
        colors =
        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        elevation = null,
        border = null,
    ) {
        content()
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = if (expanded) {
                getString(Strings.DatePickerSwitchToDaySelection)
            } else {
                getString(Strings.DatePickerSwitchToYearSelection)
            },
            Modifier.rotate(if (expanded) 180f else 0f)
        )
    }
}

/**
 * Returns a string representation of an integer at the current Locale.
 */
private fun Int.toLocalString(): String {
    val formatter = NumberFormat.getIntegerInstance()
    // Eliminate any use of delimiters when formatting the integer.
    formatter.isGroupingUsed = false
    return formatter.format(this)
}

internal val RecommendedSizeForAccessibility = 48.dp
internal val MonthYearHeight = 56.dp
internal val DatePickerHorizontalPadding = PaddingValues(horizontal = 12.dp)
private val DatePickerHeaderPadding = PaddingValues(
    start = 12.dp,
    top = 16.dp,
    bottom = 12.dp
)

private val YearsVerticalPadding = 16.dp

private const val MaxCalendarRows = 6
private const val YearsInRow: Int = 3
