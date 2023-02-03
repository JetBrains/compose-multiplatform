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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.material3.tokens.TimePickerTokens
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialColor
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialContainerSize
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialLabelTextFont
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorCenterContainerSize
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorHandleContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorHandleContainerSize
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorTrackContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialUnselectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.ContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorContainerShape
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorOutlineColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorSelectedContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorSelectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorUnselectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorVerticalContainerHeight
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorVerticalContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorContainerHeight
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorContainerShape
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorLabelTextFont
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorSelectedContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorSelectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorUnselectedContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorUnselectedLabelTextColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Time pickers help users select and set a specific time.
 *
 * Shows a picker that allows the user to select time.
 * Subscribe to updates through [TimePickerState]
 *
 * @sample androidx.compose.material3.samples.TimePickerSample
 *
 * [state] state for this timepicker, allows to subscribe to changes to [TimePickerState.hour] and
 * [TimePickerState.minute], and set the initial time for this picker.
 *
 */
@Composable
@ExperimentalMaterial3Api
fun TimePicker(
    state: TimePickerState,
    colors: TimePickerColors = TimePickerDefaults.colors()
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ClockDisplay(state, colors)
        Spacer(modifier = Modifier.height(ClockDisplayBottomMargin))
        ClockFace(state, colors)
        Spacer(modifier = Modifier.height(ClockFaceBottomMargin))
    }
}

/**
 * Contains the default values used by [TimePicker]
 */
@ExperimentalMaterial3Api
@Stable
object TimePickerDefaults {

    /**
     * Default colors used by a [TimePicker] in different states
     *
     * @param clockDialColor The color of the clock dial.
     * @param clockDialSelectedContentColor the color of the numbers of the clock dial when they
     * are selected or overlapping with the selector
     * @param clockDialUnselectedContentColor the color of the numbers of the clock dial when they
     * are unselected
     * @param selectorColor The color of the clock dial selector.
     * @param containerColor The container color of the time picker.
     * @param periodSelectorBorderColor the color used for the border of the AM/PM toggle.
     * @param periodSelectorSelectedContainerColor the color used for the selected container of
     * the AM/PM toggle
     * @param periodSelectorUnselectedContainerColor the color used for the unselected container
     * of the AM/PM toggle
     * @param periodSelectorSelectedContentColor color used for the selected content of
     * the AM/PM toggle
     * @param periodSelectorUnselectedContentColor color used for the unselected content
     * of the AM/PM toggle
     * @param timeSelectorSelectedContainerColor color used for the selected container of the
     * display buttons to switch between hour and minutes
     * @param timeSelectorUnselectedContainerColor color used for the unselected container of the
     * display buttons to switch between hour and minutes
     * @param timeSelectorSelectedContentColor color used for the selected content of the display
     * buttons to switch between hour and minutes
     * @param timeSelectorUnselectedContentColor color used for the unselected content of the
     * display buttons to switch between hour and minutes
     */
    @Composable
    fun colors(
        clockDialColor: Color = ClockDialColor.toColor(),
        clockDialSelectedContentColor: Color = ClockDialSelectedLabelTextColor.toColor(),
        clockDialUnselectedContentColor: Color = ClockDialUnselectedLabelTextColor.toColor(),
        selectorColor: Color = ClockDialSelectorHandleContainerColor.toColor(),
        containerColor: Color = ContainerColor.toColor(),
        periodSelectorBorderColor: Color = PeriodSelectorOutlineColor.toColor(),
        periodSelectorSelectedContainerColor: Color =
            PeriodSelectorSelectedContainerColor.toColor(),
        periodSelectorUnselectedContainerColor: Color = Color.Transparent,
        periodSelectorSelectedContentColor: Color =
            PeriodSelectorSelectedLabelTextColor.toColor(),
        periodSelectorUnselectedContentColor: Color =
            PeriodSelectorUnselectedLabelTextColor.toColor(),
        timeSelectorSelectedContainerColor: Color =
            TimeSelectorSelectedContainerColor.toColor(),
        timeSelectorUnselectedContainerColor: Color =
            TimeSelectorUnselectedContainerColor.toColor(),
        timeSelectorSelectedContentColor: Color =
            TimeSelectorSelectedLabelTextColor.toColor(),
        timeSelectorUnselectedContentColor: Color =
            TimeSelectorUnselectedLabelTextColor.toColor(),
    ) = TimePickerColors(
        clockDialColor = clockDialColor,
        clockDialSelectedContentColor = clockDialSelectedContentColor,
        clockDialUnselectedContentColor = clockDialUnselectedContentColor,
        selectorColor = selectorColor,
        containerColor = containerColor,
        periodSelectorBorderColor = periodSelectorBorderColor,
        periodSelectorSelectedContainerColor = periodSelectorSelectedContainerColor,
        periodSelectorUnselectedContainerColor = periodSelectorUnselectedContainerColor,
        periodSelectorSelectedContentColor = periodSelectorSelectedContentColor,
        periodSelectorUnselectedContentColor = periodSelectorUnselectedContentColor,
        timeSelectorSelectedContainerColor = timeSelectorSelectedContainerColor,
        timeSelectorUnselectedContainerColor = timeSelectorUnselectedContainerColor,
        timeSelectorSelectedContentColor = timeSelectorSelectedContentColor,
        timeSelectorUnselectedContentColor = timeSelectorUnselectedContentColor
    )
}

/**
 * Represents the colors used by a [TimePicker] in different states
 *
 * See [TimePickerDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@Immutable
@ExperimentalMaterial3Api
class TimePickerColors internal constructor(
    internal val clockDialColor: Color,
    internal val selectorColor: Color,
    internal val containerColor: Color,
    internal val periodSelectorBorderColor: Color,
    private val clockDialSelectedContentColor: Color,
    private val clockDialUnselectedContentColor: Color,
    private val periodSelectorSelectedContainerColor: Color,
    private val periodSelectorUnselectedContainerColor: Color,
    private val periodSelectorSelectedContentColor: Color,
    private val periodSelectorUnselectedContentColor: Color,
    private val timeSelectorSelectedContainerColor: Color,
    private val timeSelectorUnselectedContainerColor: Color,
    private val timeSelectorSelectedContentColor: Color,
    private val timeSelectorUnselectedContentColor: Color,
) {
    internal fun periodSelectorContainerColor(selected: Boolean) =
        if (selected) {
            periodSelectorSelectedContainerColor
        } else {
            periodSelectorUnselectedContainerColor
        }

    internal fun periodSelectorContentColor(selected: Boolean) =
        if (selected) {
            periodSelectorSelectedContentColor
        } else {
            periodSelectorUnselectedContentColor
        }

    internal fun timeSelectorContainerColor(selected: Boolean) =
        if (selected) {
            timeSelectorSelectedContainerColor
        } else {
            timeSelectorUnselectedContainerColor
        }

    internal fun timeSelectorContentColor(selected: Boolean) =
        if (selected) {
            timeSelectorSelectedContentColor
        } else {
            timeSelectorUnselectedContentColor
        }

    internal fun clockDialContentColor(selected: Boolean) =
        if (selected) {
            clockDialSelectedContentColor
        } else {
            clockDialUnselectedContentColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimePickerColors

        if (clockDialColor != other.clockDialColor) return false
        if (selectorColor != other.selectorColor) return false
        if (containerColor != other.containerColor) return false
        if (periodSelectorBorderColor != other.periodSelectorBorderColor) return false
        if (periodSelectorSelectedContainerColor != other.periodSelectorSelectedContainerColor)
            return false
        if (periodSelectorUnselectedContainerColor != other.periodSelectorUnselectedContainerColor)
            return false
        if (periodSelectorSelectedContentColor != other.periodSelectorSelectedContentColor)
            return false
        if (periodSelectorUnselectedContentColor != other.periodSelectorUnselectedContentColor)
            return false
        if (timeSelectorSelectedContainerColor != other.timeSelectorSelectedContainerColor)
            return false
        if (timeSelectorUnselectedContainerColor != other.timeSelectorUnselectedContainerColor)
            return false
        if (timeSelectorSelectedContentColor != other.timeSelectorSelectedContentColor)
            return false
        if (timeSelectorUnselectedContentColor != other.timeSelectorUnselectedContentColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = clockDialColor.hashCode()
        result = 31 * result + selectorColor.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + periodSelectorBorderColor.hashCode()
        result = 31 * result + periodSelectorSelectedContainerColor.hashCode()
        result = 31 * result + periodSelectorUnselectedContainerColor.hashCode()
        result = 31 * result + periodSelectorSelectedContentColor.hashCode()
        result = 31 * result + periodSelectorUnselectedContentColor.hashCode()
        result = 31 * result + timeSelectorSelectedContainerColor.hashCode()
        result = 31 * result + timeSelectorUnselectedContainerColor.hashCode()
        result = 31 * result + timeSelectorSelectedContentColor.hashCode()
        result = 31 * result + timeSelectorUnselectedContentColor.hashCode()
        return result
    }
}

/**
 * Creates a [TimePickerState] for a time picker that is remembered across compositions
 * and configuration changes.
 *
 * @param initialHour starting hour for this state, will be displayed in the time picker when launched
 * Ranges from 0 to 23
 * @param initialMinute starting minute for this state, will be displayed in the time picker when
 * launched. Ranges from 0 to 59
 * @param is24Hour The format for this time picker `false` for 12 hour format with an AM/PM toggle
 * or `true` for 24 hour format without toggle.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberTimePickerState(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24Hour: Boolean = false,
): TimePickerState = rememberSaveable(
    saver = TimePickerState.Saver()
) {
    TimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
    )
}

/**
 * A class to handle state changes in a [TimePicker]
 *
 * @sample androidx.compose.material3.samples.TimePickerSample
 *
 * @param initialHour
 *  starting hour for this state, will be displayed in the time picker when launched
 *  Ranges from 0 to 23
 * @param initialMinute
 *  starting minute for this state, will be displayed in the time picker when launched.
 *  Ranges from 0 to 59
 * @param is24Hour The format for this time picker `false` for 12 hour format with an AM/PM toggle
 *  or `true` for 24 hour format without toggle.
 */
@Stable
class TimePickerState(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
) {
    init {
        require(initialHour in 0..23) { "initialHour should in [0..23] range" }
        require(initialHour in 0..59) { "initialMinute should be in [0..59] range" }
    }

    val minute: Int get() = minuteAngle.toMinute()
    val hour: Int get() = hourAngle.toHour() + if (isAfternoon) 12 else 0
    val is24hour: Boolean = is24Hour

    internal val hourForDisplay: Int get() = hourForDisplay(hour)
    internal val selectorPos by derivedStateOf(structuralEqualityPolicy()) {
        val inInnerCircle = isInnerCircle
        val handleRadiusPx = ClockDialSelectorHandleContainerSize / 2
        val selectorLength = if (is24Hour && inInnerCircle && selection == Selection.Hour) {
            InnerCircleRadius
        } else {
            OuterCircleSizeRadius
        }.minus(handleRadiusPx)

        val length = selectorLength + handleRadiusPx
        val offsetX = length * cos(currentAngle.value) + ClockDialContainerSize / 2
        val offsetY = length * sin(currentAngle.value) + ClockDialContainerSize / 2

        DpOffset(offsetX, offsetY)
    }

    internal val values by derivedStateOf {
        if (selection == Selection.Minute) Minutes else Hours
    }

    internal var selection by mutableStateOf(Selection.Hour)
    internal var isAfternoonToggle by mutableStateOf(initialHour > 12 && !is24Hour)
    internal var isInnerCircle by mutableStateOf(initialHour > 12 || initialHour == 0)

    private var hourAngle by mutableStateOf(RadiansPerHour * initialHour % 12 - FullCircle / 4)
    private var minuteAngle by mutableStateOf(RadiansPerMinute * initialMinute - FullCircle / 4)

    private val mutex = MutatorMutex()
    private val isAfternoon by derivedStateOf {
        (is24hour && isInnerCircle && hourAngle.toHour() != 0) || isAfternoonToggle
    }

    internal val currentAngle = Animatable(hourAngle)

    internal fun isSelected(value: Int): Boolean =
        if (selection == Selection.Minute) {
            value == minute
        } else {
            hour == (value + if (isAfternoon) 12 else 0)
        }

    internal suspend fun update(value: Float, fromTap: Boolean = false) {
        mutex.mutate(MutatePriority.UserInput) {
            if (selection == Selection.Hour) {
                hourAngle = value.toHour() % 12 * RadiansPerHour
            } else if (fromTap) {
                minuteAngle = (value.toMinute() - value.toMinute() % 5) * RadiansPerMinute
            } else {
                minuteAngle = value.toMinute() * RadiansPerMinute
            }

            if (fromTap) {
                currentAngle.snapTo(minuteAngle)
            } else {
                currentAngle.snapTo(offsetHour(value))
            }
        }
    }

    internal suspend fun animateToCurrent() {
        val (start, end) = if (selection == Selection.Hour) {
            valuesForAnimation(minuteAngle, hourAngle)
        } else {
            valuesForAnimation(hourAngle, minuteAngle)
        }

        currentAngle.snapTo(start)
        currentAngle.animateTo(end, tween(200))
    }

    private fun hourForDisplay(hour: Int): Int = when {
        is24hour && isInnerCircle && hour == 0 -> 12
        is24hour -> hour % 24
        hour % 12 == 0 -> 12
        isAfternoon -> hour - 12
        else -> hour
    }

    private fun offsetHour(angle: Float): Float {
        val ret = angle + QuarterCircle.toFloat()
        return if (ret < 0) ret + FullCircle else ret
    }

    private fun Float.toHour(): Int {
        val hourOffset: Float = RadiansPerHour / 2
        val totalOffset = hourOffset + QuarterCircle
        return ((this + totalOffset) / RadiansPerHour).toInt() % 12
    }

    private fun Float.toMinute(): Int {
        val hourOffset: Float = RadiansPerMinute / 2
        val totalOffset = hourOffset + QuarterCircle
        return ((this + totalOffset) / RadiansPerMinute).toInt() % 60
    }

    suspend fun settle() {
        val targetValue = valuesForAnimation(currentAngle.value, minuteAngle)
        currentAngle.snapTo(targetValue.first)
        currentAngle.animateTo(targetValue.second, tween(200))
    }

    companion object {
        /**
         * The default [Saver] implementation for [TimePickerState].
         */
        fun Saver(): Saver<TimePickerState, *> = Saver(
            save = {
                listOf(
                    it.minute,
                    it.hour,
                    it.is24hour
                )
            },
            restore = { value ->
                TimePickerState(
                    initialHour = value[0] as Int,
                    initialMinute = value[1] as Int,
                    is24Hour = value[2] as Boolean
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockDisplay(state: TimePickerState, colors: TimePickerColors) {
    Row(horizontalArrangement = Arrangement.Center) {
        TimeSelector(state.hourForDisplay, state, Selection.Hour, colors = colors)
        DisplaySeparator()
        TimeSelector(state.minute, state, Selection.Minute, colors = colors)
        if (!state.is24hour) {
            Spacer(modifier = Modifier.width(PeriodToggleTopMargin))
            PeriodToggle(state, colors)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodToggle(state: TimePickerState, colors: TimePickerColors) {
    val borderStroke = BorderStroke(
        TimePickerTokens.PeriodSelectorOutlineWidth,
        colors.periodSelectorBorderColor
    )

    val shape = PeriodSelectorContainerShape.toShape() as CornerBasedShape
    val contentDescription = getString(Strings.TimePickerPeriodToggle)
    Column(
        Modifier
            .semantics { this.contentDescription = contentDescription }
            .selectableGroup()
            .size(PeriodSelectorVerticalContainerWidth, PeriodSelectorVerticalContainerHeight)
            .border(border = borderStroke, shape = shape)
    ) {
        ToggleItem(
            checked = !state.isAfternoonToggle,
            shape = shape.top(),
            onClick = {
                state.isAfternoonToggle = false
            },
            colors = colors,
        ) { Text(text = getString(string = Strings.TimePickerAM)) }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(TimePickerTokens.PeriodSelectorOutlineWidth)
                .background(color = PeriodSelectorOutlineColor.toColor())
        )
        ToggleItem(
            checked =
            state.isAfternoonToggle,
            shape = shape.bottom(),
            onClick = {
                state.isAfternoonToggle = true
            },
            colors = colors,
        ) { Text(getString(string = Strings.TimePickerPM)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ToggleItem(
    checked: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    colors: TimePickerColors,
    content: @Composable RowScope.() -> Unit,
) {
    val contentColor = colors.periodSelectorContentColor(checked)
    val containerColor = colors.periodSelectorContainerColor(checked)

    TextButton(
        modifier = Modifier
            .weight(1f)
            .semantics { selected = checked },
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        onClick = onClick,
        content = content,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            containerColor = containerColor
        )
    )
}

@Composable
private fun DisplaySeparator() {
    val style = copyAndSetFontPadding(
        style = MaterialTheme.typography.fromToken(TimeSelectorLabelTextFont).copy(
            textAlign = TextAlign.Center,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both
            )
        ), includeFontPadding = false
    )

    Box(
        modifier = Modifier.size(
            DisplaySeparatorWidth,
            PeriodSelectorVerticalContainerHeight
        ), contentAlignment = Alignment.Center
    ) { Text(text = ":", style = style) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimeSelector(
    value: Int,
    state: TimePickerState,
    selection: Selection,
    colors: TimePickerColors,
) {
    val selected = state.selection == selection
    val selectorContentDescription = getString(
        if (selection == Selection.Hour) {
            Strings.TimePickerHourSelection
        } else {
            Strings.TimePickerMinuteSelection
        }
    )

    val containerColor = colors.timeSelectorContainerColor(selected)
    val contentColor = colors.timeSelectorContentColor(selected)
    val scope = rememberCoroutineScope()
    Surface(
        modifier = Modifier
            .size(TimeSelectorContainerWidth, TimeSelectorContainerHeight)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.contentDescription = selectorContentDescription
            },
        onClick = {
            if (selection != state.selection) {
                state.selection = selection
                scope.launch {
                    state.animateToCurrent()
                }
            }
        },
        selected = selected,
        shape = TimeSelectorContainerShape.toShape(),
        color = containerColor,
    ) {
        val valueContentDescription = getString(
            numberContentDescription(
                selection = selection,
                is24Hour = state.is24hour
            ),
            value
        )
        Box(contentAlignment = Alignment.Center) {
            val textStyle = MaterialTheme.typography.fromToken(TimeSelectorLabelTextFont)
            Text(
                modifier = Modifier.semantics { contentDescription = valueContentDescription },
                text = value.toLocalString(minDigits = 2),
                color = contentColor,
                style = textStyle,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockFace(state: TimePickerState, colors: TimePickerColors) {
    Crossfade(
        modifier = Modifier
            .background(shape = CircleShape, color = colors.clockDialColor)
            .size(ClockDialContainerSize)
            .semantics {
                selectableGroup()
            },
        targetState = state.values,
        animationSpec = tween(durationMillis = MotionTokens.DurationMedium3.toInt())
    ) { screen ->
        CircularLayout(
            modifier = Modifier
                .clockDial(state)
                .size(ClockDialContainerSize)
                .drawSelector(state, colors),
            radius = OuterCircleSizeRadius,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.clockDialContentColor(false)
            ) {
                repeat(screen.size) {
                    val outerValue = if (!state.is24hour) screen[it] else screen[it] % 12
                    ClockText(
                        is24Hour = state.is24hour,
                        selection = state.selection,
                        value = outerValue,
                        selected = state.isSelected(it)
                    )
                }

                if (state.selection == Selection.Hour && state.is24hour) {
                    CircularLayout(
                        modifier = Modifier
                            .layoutId(LayoutId.InnerCircle)
                            .size(ClockDialContainerSize)
                            .background(shape = CircleShape, color = Color.Transparent),
                        radius = InnerCircleRadius
                    ) {
                        repeat(ExtraHours.size) {
                            val innerValue = ExtraHours[it]
                            ClockText(
                                is24Hour = true,
                                selection = state.selection,
                                value = innerValue,
                                selected = state.isSelected(it % 11)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.drawSelector(
    state: TimePickerState,
    colors: TimePickerColors,
): Modifier = this.drawWithContent {
    val selectorOffsetPx = Offset(state.selectorPos.x.toPx(), state.selectorPos.y.toPx())

    val selectorRadius = ClockDialSelectorHandleContainerSize.toPx() / 2
    val selectorColor = colors.selectorColor

    // clear out the selector section
    drawCircle(
        radius = selectorRadius,
        center = selectorOffsetPx,
        color = Color.Black,
        blendMode = BlendMode.Clear,
    )

    // draw the text composables
    drawContent()

    // draw the selector and clear out the numbers overlapping
    drawCircle(
        radius = selectorRadius,
        center = selectorOffsetPx,
        color = selectorColor,
        blendMode = BlendMode.Xor
    )

    val strokeWidth = ClockDialSelectorTrackContainerWidth.toPx()
    val lineLength = selectorOffsetPx.minus(
        Offset(
            (selectorRadius * cos(state.currentAngle.value)),
            (selectorRadius * sin(state.currentAngle.value))
        )
    )

    // draw the selector line
    drawLine(
        start = size.center,
        strokeWidth = strokeWidth,
        end = lineLength,
        color = selectorColor,
        blendMode = BlendMode.SrcOver
    )

    // draw the selector small dot
    drawCircle(
        radius = ClockDialSelectorCenterContainerSize.toPx() / 2,
        center = size.center,
        color = selectorColor,
    )

    // draw the portion of the number that was overlapping
    drawCircle(
        radius = selectorRadius,
        center = selectorOffsetPx,
        color = colors.clockDialContentColor(selected = true),
        blendMode = BlendMode.DstOver
    )
}

private fun Modifier.clockDial(state: TimePickerState): Modifier = composed(debugInspectorInfo {
    name = "clockDial"
    properties["state"] = state
}) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(IntOffset.Zero) }
    val scope = rememberCoroutineScope()
    val maxDist = with(LocalDensity.current) { MaxDistance.toPx() }
    fun moveSelector(x: Float, y: Float) {
        if (state.selection == Selection.Hour && state.is24hour) {
            state.isInnerCircle = dist(x, y, center.x, center.y) < maxDist
        }
    }
    Modifier
        .onSizeChanged { center = it.center }
        .pointerInput(state, maxDist, center) {
            detectTapGestures(
                onPress = {
                    offsetX = it.x
                    offsetY = it.y
                },
                onTap = {
                    scope.launch {
                        state.update(atan(it.y - center.y, it.x - center.x), true)
                        moveSelector(it.x, it.y)

                        if (state.selection == Selection.Hour) {
                            state.selection = Selection.Minute
                        } else {
                            state.settle()
                        }
                    }
                },
            )
        }
        .pointerInput(state, maxDist, center) {
            detectDragGestures(onDragEnd = {
                scope.launch {
                    if (state.selection == Selection.Hour) {
                        state.selection = Selection.Minute
                        state.animateToCurrent()
                    } else {
                        state.settle()
                    }
                }
            }) { _, dragAmount ->
                scope.launch {
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    state.update(atan(offsetY - center.y, offsetX - center.x))
                }
                moveSelector(offsetX, offsetY)
            }
        }
}

@Composable
private fun ClockText(
    is24Hour: Boolean,
    selected: Boolean,
    selection: Selection,
    value: Int
) {
    val style = MaterialTheme.typography.fromToken(ClockDialLabelTextFont).let {
        remember(it) {
            copyAndSetFontPadding(style = it, false)
        }
    }

    val contentDescription = getString(
        numberContentDescription(
            selection = selection,
            is24Hour = is24Hour
        ),
        value
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(MinimumInteractiveSize)
            .focusable()
            .semantics {
                this.selected = selected
                this.contentDescription = contentDescription
            }
    ) {
        Text(
            text = value.toLocalString(minDigits = 1),
            style = style,
        )
    }
}

/** Distribute elements evenly on a circle of [radius] */
@Composable
private fun CircularLayout(
    modifier: Modifier = Modifier,
    radius: Dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier, content = content
    ) { measurables, constraints ->
        val radiusPx = radius.toPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.filter {
            it.layoutId != LayoutId.Selector && it.layoutId != LayoutId.InnerCircle
        }.map { measurable -> measurable.measure(itemConstraints) }
        val selectorMeasurable = measurables.find { it.layoutId == LayoutId.Selector }
        val innerMeasurable = measurables.find { it.layoutId == LayoutId.InnerCircle }
        val theta = FullCircle / (placeables.count())
        val selectorPlaceable = selectorMeasurable?.measure(itemConstraints)
        val innerCirclePlaceable = innerMeasurable?.measure(itemConstraints)

        layout(
            width = constraints.minWidth,
            height = constraints.minHeight,
        ) {
            selectorPlaceable?.place(0, 0)

            placeables.forEachIndexed { i, it ->
                val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                val offsetX = radiusPx * cos(theta * i - QuarterCircle) + centerOffsetX
                val offsetY = radiusPx * sin(theta * i - QuarterCircle) + centerOffsetY
                it.place(
                    x = offsetX.roundToInt(), y = offsetY.roundToInt()
                )
            }

            innerCirclePlaceable?.place(
                (constraints.minWidth - innerCirclePlaceable.width) / 2,
                (constraints.minHeight - innerCirclePlaceable.height) / 2
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun numberContentDescription(selection: Selection, is24Hour: Boolean): Strings {
    if (selection == Selection.Minute) {
        return Strings.TimePickerMinuteSuffix
    }

    if (is24Hour) {
        return Strings.TimePicker24HourSuffix
    }

    return Strings.TimePickerHourSuffix
}

private fun valuesForAnimation(current: Float, new: Float): Pair<Float, Float> {
    var start = current
    var end = new
    if (abs(start - end) <= PI) {
        return Pair(start, end)
    }

    if (start > PI && end < PI) {
        end += FullCircle
    } else if (current < PI && new > PI) {
        start += FullCircle
    }

    return Pair(start, end)
}

private fun dist(x1: Float, y1: Float, x2: Int, y2: Int): Float {
    val x = x2 - x1
    val y = y2 - y1
    return hypot(x.toDouble(), y.toDouble()).toFloat()
}

private fun atan(y: Float, x: Float): Float {
    val ret = atan2(y, x) - QuarterCircle.toFloat()
    return if (ret < 0) ret + FullCircle else ret
}

private enum class LayoutId {
    Selector, InnerCircle,
}

internal enum class Selection {
    Hour, Minute
}

private const val FullCircle: Float = (PI * 2).toFloat()
private const val QuarterCircle = PI / 2
private const val RadiansPerMinute: Float = FullCircle / 60
private const val RadiansPerHour: Float = FullCircle / 12f

private val OuterCircleSizeRadius = 101.dp
private val InnerCircleRadius = 69.dp
private val ClockDisplayBottomMargin = 36.dp
private val ClockFaceBottomMargin = 24.dp
private val PeriodToggleTopMargin = 12.dp
private val DisplaySeparatorWidth = 24.dp

private val MaxDistance = 74.dp
private val MinimumInteractiveSize = 48.dp
private val Minutes = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
private val Hours = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
private val ExtraHours = Hours.map { (it % 12 + 12) }

private fun Int.toLocalString(minDigits: Int): String {
    val formatter = NumberFormat.getIntegerInstance()
    // Eliminate any use of delimiters when formatting the integer.
    formatter.isGroupingUsed = false
    formatter.minimumIntegerDigits = minDigits
    return formatter.format(this)
}